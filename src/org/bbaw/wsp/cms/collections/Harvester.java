package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.FileNameMap;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.collections.DocumentTokenizer;
import org.bbaw.wsp.cms.dochandler.parser.document.IDocument;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.DocumentParser;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.transform.XslResourceTransformer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.Token;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizer;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

public class Harvester {
  private static Logger LOGGER = Logger.getLogger(Harvester.class);
  private String[] options;
  private MetadataHandler metadataHandler;
  private String harvestDir;
  
  public static Harvester getInstance() throws ApplicationException {
    Harvester harvester = new Harvester();
    harvester.init();
    return harvester;
  }
  
  private void init() throws ApplicationException {
    metadataHandler = MetadataHandler.getInstance();
    harvestDir = Constants.getInstance().getHarvestDir();
  }
  
  public void setOptions(String[] options) {
    this.options = options;
  }
  
  public void harvest(Project project) throws ApplicationException {
    try {
      int counter = 0;
      String projectId = project.getId();
      LOGGER.info("Harvest project: " + projectId + " ...");
      String harvestProjectDirStr = harvestDir + "/" + project.getId();
      if (options != null && options.length == 1 && options[0].equals("dbType:crawl")) {
        // nothing when only the crawl db should be updated
      } else if (options != null && options.length == 1 && options[0].startsWith("dbName:")) {
        // nothing when only one db should be updated
      } else {
        delete(project); // remove the harvest directory
      }
      if (options != null && options.length == 1 && options[0].startsWith("dbName:")) {
        // if db is specified: do not index the project rdf records
      } else {
        ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecords(project, true);
        if (mdRecords != null) {
          ArrayList<MetadataRecord> harvestedResourcesRecords = harvestResources(project, null, mdRecords); // download resources and save metadata and fulltext fields
          counter = counter + harvestedResourcesRecords.size();
        }
      }
      ArrayList<Database> projectDBs = project.getDatabases();
      if (options != null && options.length == 1 && options[0].equals("dbType:crawl")) {
        projectDBs = project.getDatabasesByType("crawl");
      } else if (options != null && options.length == 1 && options[0].startsWith("dbName:")) {
        String dbName = options[0].replaceAll("dbName:", "");
        projectDBs = project.getDatabasesByName(dbName);
      }
      if (projectDBs != null) {
        for (int i=0; i<projectDBs.size(); i++) {
          Database projectDB = projectDBs.get(i);
          int countResources = harvestDB(project, projectDB);
          counter = counter + countResources;
        }
      }
      LOGGER.info("Project: " + projectId + " with " + counter + " resources harvested into: " + harvestProjectDirStr);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  public void delete(Project project) throws ApplicationException {
    try {
      String harvestProjectDirStr = harvestDir + "/" + project.getId();
      Runtime.getRuntime().exec("rm -rf " + harvestProjectDirStr);  // fast also when the directory contains many files
      LOGGER.info("Project harvest directory: " + harvestProjectDirStr + " successfully deleted");
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  public ArrayList<MetadataRecord> getMetadataRecordsByRecordsFile(Project project) throws ApplicationException {
    File recordsFile = new File(harvestDir + "/" + project.getId() + "/metadata/records/" + project.getId() + ".xml");
    ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecordsByRecordsFile(recordsFile);
    return mdRecords;
  }

  public ArrayList<MetadataRecord> getMetadataRecordsByRecordsFile(Project project, Database db) throws ApplicationException {
    File dbRecordsFile = new File(harvestDir + "/" + project.getId() + "/metadata/records/" + project.getId() + "-" + db.getName() + "-1.xml");
    ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecordsByRecordsFile(dbRecordsFile);
    return mdRecords;
  }

  private int harvestDB(Project project, Database db) throws ApplicationException {
    int countHarvest = 0;
    String dbType = db.getType();
    if (dbType != null && (dbType.equals("eXist") || dbType.equals("crawl") || dbType.equals("oai"))) {
      // download records and save them in RDF-file as records
      LOGGER.info("Harvest metadata records (" + project.getId() + ", " + db.getRdfId() + ", " + db.getType() + ") ...");
      ArrayList<MetadataRecord> dbMdRecords = harvestDBResources(project, db);
      // download resources and save metadata and fulltext fields
      if (dbMdRecords != null) {
        ArrayList<MetadataRecord> harvestedResourcesRecords = harvestResources(project, db, dbMdRecords);
        countHarvest = countHarvest + harvestedResourcesRecords.size();
      }
    } else if (dbType != null && (dbType.equals("oai-dbrecord"))) {
      LOGGER.info("Harvest metadata records (" + project.getId() + ", " + db.getRdfId() + ", " + db.getType() + ") ...");
      int countDbResources = harvestOaiDBResources(project, db);
      countHarvest = countHarvest + countDbResources;
    } else if (dbType != null && (dbType.equals("mysql") || dbType.equals("postgres"))) {
      // nothing, data remains in /dataExtern/dbDumps/
    } else {
      // nothing
    }
    return countHarvest;
  }
  
  private ArrayList<MetadataRecord> harvestResources(Project project, Database db, ArrayList<MetadataRecord> mdRecords) throws ApplicationException {
    ArrayList<MetadataRecord> retMdRecords = new ArrayList<MetadataRecord>();
    int countHarvest = 0;
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      try {
        countHarvest++;
        MetadataRecord newMdRecord = harvestResource(countHarvest, project, mdRecord);
        retMdRecords.add(newMdRecord);
        // without that, there would be a memory leak (with many big documents in one project)
        // with that the main big fields (content etc.) could be garbaged
        newMdRecord.setAllNull();
      } catch (Exception e) {
        LOGGER.error("Harvest of metadata record failed:");
        e.printStackTrace();
      }
    }
    // write metadata file: xml
    String projectId = project.getId();
    File dbRecordsFile = new File(harvestDir + "/" + projectId + "/metadata/records/" + projectId + ".xml");
    if (db != null)
      dbRecordsFile = new File(harvestDir + "/" + projectId + "/metadata/records/" + projectId + "-" + db.getName() + "-1.xml");
    metadataHandler.writeMetadataRecords(project, retMdRecords, dbRecordsFile, "xml");
    // write metadata file: rdf (only for eXist and crawl: oai is already generated before)
    if (db != null) {
      String dbType = db.getType();
      if (dbType.equals("eXist") || dbType.equals("crawl")) {
        String rdfDbFullFileName = harvestDir + "/" + project.getId() + "/metadata/dbResources/" + project.getId() + "-" + db.getName() + "-1.rdf";
        File rdfDbFile = new File(rdfDbFullFileName);
        metadataHandler.writeMetadataRecords(project, retMdRecords, rdfDbFile, "rdf");
      }
    }
    return retMdRecords;
  }
  
  private ArrayList<MetadataRecord> harvestDBResources(Project project, Database db) throws ApplicationException {
    ArrayList<MetadataRecord> dbMdRecords = null;
    String dbType = db.getType();
    if (dbType.equals("eXist") || dbType.equals("crawl")) { 
      dbMdRecords = metadataHandler.getMetadataRecords(project, db, true);
      ArrayList<MetadataRecord> projectMdRecords = metadataHandler.getMetadataRecords(project, false);  // performance: records of project are cached
      if (projectMdRecords != null && dbMdRecords != null)
        dbMdRecords = removeDoubleRecords(projectMdRecords, dbMdRecords);
      LOGGER.info("Harvest of metadata records (" + project.getId() + ", " + db.getRdfId() + ", " + dbType + ") finished");
    } else if (dbType.equals("oai")) {
      String dbResourcesDirName = harvestDir + "/" + project.getId() + "/metadata/dbResources";
      metadataHandler.fetchMetadataRecordsOai(dbResourcesDirName, project, db);
      String rdfDbFileName = project.getId() + "/metadata/dbResources/" + project.getId() + "-" + db.getName() + "-1.rdf";
      LOGGER.info("Harvest of metadata records (" + project.getId() + ", " + db.getRdfId() + ", " + dbType + ") finished (/harvest/" + rdfDbFileName  + ")");
      String rdfDbFullFileName = harvestDir + "/" + project.getId() + "/metadata/dbResources/" + project.getId() + "-" + db.getName() + "-1.rdf";
      File rdfDbFile = new File(rdfDbFullFileName);
      dbMdRecords = metadataHandler.getMetadataRecordsByRdfFile(project, rdfDbFile, db, true);
    } else {
      // nothing
    }
    return dbMdRecords;
  }
  
  private int harvestOaiDBResources(Project project, Database db) throws ApplicationException {
    int count = -1;
    String dbType = db.getType();
    if (dbType.equals("oai-dbrecord")) {
      String dbResourcesDirName = harvestDir + "/" + project.getId() + "/metadata/dbResources";
      count = metadataHandler.fetchMetadataRecordsOai(dbResourcesDirName, project, db);
      String rdfDbFileName = project.getId() + "/metadata/dbResources/" + project.getId() + "-" + db.getName() + "-*.rdf";
      LOGGER.info("Harvest of metadata records (" + project.getId() + ", " + db.getRdfId() + ", " + db.getType() + ") finished (/harvest/" + rdfDbFileName  + ")");
    }
    return count;
  }

  public String getFulltext(String type, MetadataRecord mdRecord) throws ApplicationException {
    String fulltext = null;
    String docId = mdRecord.getDocId();
    String projectId = mdRecord.getProjectId();
    String baseDir = harvestDir + "/" + projectId + "/data";
    String docDirName = getDocDir(docId, baseDir);
    try {
      File fulltextFile = new File(docDirName + "/" + type + ".txt");
      if (fulltextFile.exists()) {
        fulltext = FileUtils.readFileToString(fulltextFile, "utf-8");
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return fulltext;
  }
  
  private MetadataRecord harvestResource(int counter, Project project, MetadataRecord mdRecord) throws ApplicationException {
    try {
      String docId = mdRecord.getDocId();
      String srcUrlStr = mdRecord.getUri();
      String baseDir = harvestDir + "/" + project.getId() + "/data";
      String docDirName = getDocDir(docId, baseDir);
      String docDestFileName = getDocFullFileName(docId, baseDir); 
      LOGGER.info(counter + ". " + "Harvest resource: " + srcUrlStr + " (/harvest/" + project.getId() + "/data" + docId + ")");
      boolean docIsXml = isDocXml(docId); 
      URL srcUrl = null;
      if (srcUrlStr != null && ! srcUrlStr.equals("empty")) {
        srcUrl = new URL(srcUrlStr);
      }
      File docDestFile = new File(docDestFileName);
      String mimeType = mdRecord.getType();
      if (mimeType == null) {
        mimeType = getMimeType(docId);
        mdRecord.setType(mimeType);
      }
      // perform operation on file system
      boolean isDbRecord = mdRecord.isDbRecord();
      boolean isText = isText(mimeType);
      if (! isDbRecord && isText) {
        boolean success = copyUrlToFile(srcUrl, docDestFile);  // several tries with delay
        if (! success)
          return mdRecord;
      }
      if (mdRecord.getLastModified() == null) {
        Date lastModified = new Date();
        mdRecord.setLastModified(lastModified);
      }
      String docType = null;
      XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
      if (docIsXml) {
        // parse validation on file
        try {
          XdmNode docNode = xQueryEvaluator.parse(srcUrl); // if it is not parseable an exception with a detail message is thrown 
          docType = getNodeType(docNode); // e.g. "TEI" or "mets" is returned
          docType = docType.trim();
        } catch (Exception e) {
          // second try with encoded blanks url
          String srcUrlFileEncodedStr = srcUrl.getFile().replaceAll(" ", "%20");
          String srcUrlEncodedStr = srcUrl.getProtocol() + "://" + srcUrl.getHost() + ":" + srcUrl.getPort() + srcUrlFileEncodedStr;
          URL srcUrlEncoded = new URL(srcUrlEncodedStr);
          try {
            XdmNode docNode = xQueryEvaluator.parse(srcUrlEncoded);
            docType = getNodeType(docNode);
            docType = docType.trim();
          } catch (Exception e2) {
            LOGGER.error(srcUrlStr + " is not xml parseable");
            e.printStackTrace();
          }
        }
      }
      if (docType == null) {
        docType = mimeType;
      }
      if (srcUrl != null && docType == null) {
        FileUtils.deleteQuietly(docDestFile);
        LOGGER.error(srcUrlStr + " is not created: mime type is not supported");
        return mdRecord;
      }
      // document is xml fulltext document: is of type XML and not mets
      String mainLanguage = project.getMainLanguage();
      String collectionRdfId = mdRecord.getCollectionRdfId();
      if (collectionRdfId != null)
        mainLanguage = ProjectReader.getInstance().getCollectionMainLanguage(collectionRdfId);
      if (docIsXml && ! docType.equals("mets")) {
        // replace anchor in echo documents and also add the number attribute to figures
        String docDestFileNameUpgrade = docDestFileName + ".upgrade";
        File docDestFileUpgrade = new File(docDestFileNameUpgrade);
        XslResourceTransformer replaceAnchorTransformer = new XslResourceTransformer("replaceAnchor.xsl");
        String docDestFileUrlStr = docDestFile.getPath();
        String result = replaceAnchorTransformer.transform(docDestFileUrlStr);
        FileUtils.writeStringToFile(docDestFileUpgrade, result, "utf-8");
        // generate toc file (toc, figure, handwritten, persons, places)
        XslResourceTransformer tocTransformer = new XslResourceTransformer("toc.xsl");
        File tocFile = new File(docDirName + "/toc.xml");
        String tocResult = tocTransformer.transform(docDestFileNameUpgrade);
        FileUtils.writeStringToFile(tocFile, tocResult, "utf-8");
        String persons = getPersons(tocResult, xQueryEvaluator); 
        String personsDetails = getPersonsDetails(tocResult, xQueryEvaluator); 
        String places = getPlaces(tocResult, xQueryEvaluator);
        mdRecord.setPersons(persons);
        mdRecord.setPersonsDetails(personsDetails);
        mdRecord.setPlaces(places);
        // Get metadata info out of the xml document
        mdRecord = metadataHandler.getMetadataRecord(docDestFileUpgrade, docType, mdRecord, xQueryEvaluator);
        String mdRecordLanguage = mdRecord.getLanguage();
        String langId = Language.getInstance().getLanguageId(mdRecordLanguage); // test if language code is supported
        if (langId == null)
          mdRecordLanguage = null;
        if (mdRecordLanguage == null && mainLanguage != null)
          mdRecord.setLanguage(mainLanguage);
      } else if (docIsXml && docType.equals("mets")) {
        mdRecord = metadataHandler.getMetadataRecord(docDestFile, docType, mdRecord, xQueryEvaluator);
        String mdRecordLanguage = mdRecord.getLanguage();
        String langId = Language.getInstance().getLanguageId(mdRecordLanguage); // test if language code is supported
        if (langId == null)
          mdRecordLanguage = null;
        if (mdRecordLanguage == null && mainLanguage != null)
          mdRecord.setLanguage(mainLanguage);
      } 
      // build the documents fulltext fields
      if (srcUrl != null && docType != null && ! docType.equals("mets") && ! isDbRecord && isText)
        buildFulltextFields(project, mdRecord, baseDir);
      String content = mdRecord.getContent();
      if (content != null) {
        File contentFile = new File(docDirName + "/content.txt");
        FileUtils.writeStringToFile(contentFile, content, "utf-8");
      }
      String tokensOrig = mdRecord.getTokenOrig();
      if (tokensOrig != null) {
        File tokensOrigFile = new File(docDirName + "/tokensOrig.txt");
        FileUtils.writeStringToFile(tokensOrigFile, tokensOrig, "utf-8");
      }
      String tokensNorm = mdRecord.getTokenNorm();
      if (tokensNorm != null) {
        File tokensNormFile = new File(docDirName + "/tokensNorm.txt");
        FileUtils.writeStringToFile(tokensNormFile, tokensNorm, "utf-8");
      }
      String tokensMorph = mdRecord.getTokenMorph();
      if (tokensMorph != null) {
        File tokensMorphFile = new File(docDirName + "/tokensMorph.txt");
        FileUtils.writeStringToFile(tokensMorphFile, tokensMorph, "utf-8");
      }
      return mdRecord;
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  public boolean isDocXml(String docId) {
    boolean isXml = false;
    String fileExt = getDocFileExtension(docId);
    if (fileExt != null) {
      fileExt = fileExt.toLowerCase();
      if (fileExt.equals("xml"))
        isXml = true;
    }
    return isXml;
  }

  public boolean isHtml(MetadataRecord mdRecord) {
    boolean isHtml = false;
    String mimeType = mdRecord.getType();
    if (mimeType != null && mimeType.contains("html"))
      return true;;
    return isHtml;
  }

  public boolean isText(String mimeType) {
    boolean isText = true;
    if (mimeType != null && (mimeType.contains("image") || mimeType.contains("audio") || mimeType.contains("video") || mimeType.contains("gzip")))
      return false;
    return isText;
  }

  public String getMimeType(String docId) {
    String mimeType = null;
    String fileName = getDocFileName(docId);
    if (fileName != null) {
      fileName = fileName.toLowerCase();
      FileNameMap fileNameMap = URLConnection.getFileNameMap();
      mimeType = fileNameMap.getContentTypeFor(fileName);
    }
    return mimeType;
  }

  public String getDocDir(String docId, String baseDir) {
    if (docId == null || docId.trim().isEmpty())
      return null;
    String harvDir = baseDir;
    String docFileName = getDocFileName(docId);
    String docFilePath = getDocFilePath(docId);
    if (docFilePath != null)
      harvDir = harvDir + docFilePath;
    if (docFileName != null)
      harvDir = harvDir + "/" + docFileName;
    else
      harvDir = harvDir + "/" + "XXXXX";
    return harvDir;
  }
  
  private String getDocFileName(String docId) {
    String docFileName = docId.trim();
    int index = docId.lastIndexOf("/");
    if (index != -1) {
      docFileName = docId.substring(index + 1);
    }
    return docFileName;
  }
  
  public String getDocFullFileName(String docId) {
    String projectName = docId.substring(1, docId.indexOf("/", 1));
    String baseDir = harvestDir + "/" + projectName + "/data";
    return getDocFullFileName(docId, baseDir);
  }
  
  private String getDocFullFileName(String docId, String baseDir) {
    if (docId == null || docId.trim().isEmpty())
      return null;
    String docDir = getDocDir(docId, baseDir);
    String docFileName = getDocFileName(docId);
    String docFullFileName = docDir + "/" + docFileName; 
    return docFullFileName;
  }
  
  private String getDocFileExtension(String docId) {
    docId = docId.trim();
    String fileExt = null;
    int index = docId.lastIndexOf(".");
    if (index != -1) {
      fileExt = docId.substring(index + 1);
    }
    return fileExt;
  }
  
  private String getDocFilePath(String docId) {
    String docFilePath = null;
    int index = docId.lastIndexOf("/");
    if (index >= 0) {
      docFilePath = docId.substring(0, index);
    }
    if (docFilePath != null && ! docFilePath.startsWith("/"))
      docFilePath = docFilePath + "/";
    return docFilePath;
  }

  private String getNodeType(XdmNode node) {
    String nodeType = null;
    XdmSequenceIterator iter = node.axisIterator(Axis.CHILD);
    if (iter != null) {
      while (iter.hasNext()) {
        XdmNode firstChild = (XdmNode) iter.next();
        if (firstChild != null) {
          XdmNodeKind nodeKind = firstChild.getNodeKind();
          if (nodeKind.ordinal() == XdmNodeKind.ELEMENT.ordinal()) {
            QName nodeQName = firstChild.getNodeName();
            nodeType = nodeQName.getLocalName();
          }
        }
      }
    }
    return nodeType;
  }
  
  private String getPersons(String tocString, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    String persons = xQueryEvaluator.evaluateAsStringValueJoined(tocString, "/list/list[@type='persons']/item/person/name[not(. = preceding::person/name)]", "###"); // [not(. = preceding::person/name)] removes duplicates
    return persons;
  }
  
  private String getPersonsDetails(String tocString, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    String personsDetails = null;
    XdmValue xmdValuePersons = xQueryEvaluator.evaluate(tocString, "/list/list[@type='persons']/item/person[not(name = preceding::person/name)]"); // [not(name = preceding::person/name)] removes duplicates
    if (xmdValuePersons != null && xmdValuePersons.size() > 0) {
      XdmSequenceIterator xmdValuePersonsIterator = xmdValuePersons.iterator();
      personsDetails = "<persons>\n";
      while (xmdValuePersonsIterator.hasNext()) {
        XdmItem xdmItemPerson = xmdValuePersonsIterator.next();
        String xdmItemPersonStr = xdmItemPerson.toString();
        personsDetails = personsDetails + xdmItemPersonStr + "\n";
      }
      personsDetails = personsDetails + "</persons>";
    }
    return personsDetails;
  }
  
  private String getPlaces(String tocString, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    String places = xQueryEvaluator.evaluateAsStringValueJoined(tocString, "/list/list[@type='places']/item[not(. = preceding::item)]", "###"); 
    return places;
  }

  private ArrayList<MetadataRecord> removeDoubleRecords(ArrayList<MetadataRecord> projectMdRecords, ArrayList<MetadataRecord> dbMdRecords) {
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    for (int i=0; i<dbMdRecords.size(); i++) {
      MetadataRecord dbMdRecord = dbMdRecords.get(i);
      if (! isContained(projectMdRecords, dbMdRecord))
        mdRecords.add(dbMdRecord);
    }
    return mdRecords;
  }
  
  private boolean isContained(ArrayList<MetadataRecord> mdRecords, MetadataRecord mdRecord) {
    boolean isContained = false;
    String webUri = mdRecord.getWebUri();
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecordTmp = mdRecords.get(i);
      String webUriTmp = mdRecordTmp.getWebUri();
      if (webUri != null && webUriTmp != null) {
        if (webUri.endsWith("/"))
          webUri = webUri.substring(0, webUri.length() - 1);
        if (webUriTmp.endsWith("/"))
          webUriTmp = webUriTmp.substring(0, webUriTmp.length() - 1);
        if (webUri.equals(webUriTmp))
          return true;
      }
    }
    return isContained;
  }
  
  private void buildFulltextFields(Project project, MetadataRecord mdRecord, String baseDir) throws ApplicationException {
    try {
      String docId = mdRecord.getDocId();
      String language = mdRecord.getLanguage();
      boolean docIsXml = isDocXml(docId);
      boolean docIsHtml = isHtml(mdRecord);
      String docTokensOrig = null;
      String docTokensNorm = null;
      String docTokensMorph = null;
      String content = null;
      String docFileName = getDocFullFileName(docId, baseDir);
      XmlTokenizer docXmlTokenizer = null;
      if (docIsXml) {
        docFileName = getDocFullFileName(docId, baseDir) + ".upgrade";
        // remove the TEI header part
        XslResourceTransformer removeElemTransformer = new XslResourceTransformer("removeElement.xsl");
        QName elemNameQName = new QName("elemName");
        XdmValue elemNameXdmValue = new XdmAtomicValue("teiHeader");  // TODO fetch name from project configuration
        removeElemTransformer.setParameter(elemNameQName, elemNameXdmValue);
        String contentXmlRemovedHeader = removeElemTransformer.transform(docFileName);
        File docFile = new File(docFileName);
        FileUtils.writeStringToFile(docFile, contentXmlRemovedHeader, "utf-8");
        InputStreamReader docFileReader = new InputStreamReader(new FileInputStream(docFileName), "utf-8");
        // to guarantee that utf-8 is used (if not done, it does not work on Tomcat which has another default charset)
        docXmlTokenizer = new XmlTokenizer(docFileReader);
        docXmlTokenizer.setDocIdentifier(docId);
        docXmlTokenizer.setLanguage(language);
        docXmlTokenizer.setOutputFormat("string");
        String[] outputOptionsWithLemmas = { "withLemmas" }; // so all tokens are fetched with lemmas (costs performance)
        docXmlTokenizer.setOutputOptions(outputOptionsWithLemmas);
        String[] normFunctionNone = { "none" };
        docXmlTokenizer.setNormFunctions(normFunctionNone);
        docXmlTokenizer.tokenize();
        // each document at least has one page
        int pageCount = docXmlTokenizer.getPageCount();
        if (pageCount <= 0)
          pageCount = 1;  
        mdRecord.setPageCount(pageCount);
        String[] outputOptionsEmpty = {};
        docXmlTokenizer.setOutputOptions(outputOptionsEmpty); 
        // must be set to null so that the normalization function works
        docTokensOrig = docXmlTokenizer.getStringResult();
        String[] normFunctionNorm = { "norm" };
        docXmlTokenizer.setNormFunctions(normFunctionNorm);
        docTokensNorm = docXmlTokenizer.getStringResult();
        docXmlTokenizer.setOutputOptions(outputOptionsWithLemmas);
        docTokensMorph = docXmlTokenizer.getStringResult();
        // fetch original content of the documents file (without xml tags)
        XslResourceTransformer charsTransformer = new XslResourceTransformer("chars.xsl");
        content = charsTransformer.transform(docFileName);  // original content with all blanks and case sensitive and punctuation marks 
        // fill mdRecord
        mdRecord.setContent(content);
        mdRecord.setTokenOrig(docTokensOrig); // original content words (in small letters, without blanks and punctuation marks)
        mdRecord.setTokenNorm(docTokensNorm);
        mdRecord.setTokenMorph(docTokensMorph);
      } else {  // mime types: html, pdf, ... 
        if (docIsHtml) {
          File docFile = new File(docFileName);
          Document doc = Jsoup.parse(docFile, "utf-8");
          mdRecord = metadataHandler.getMetadataRecordHtml(doc, mdRecord);
          String docText = "";
          String dbRdfId = mdRecord.getDatabaseRdfId();
          String collRdfId = mdRecord.getCollectionRdfId();
          ArrayList<String> contentCssSelectors = ProjectReader.getInstance().getGlobalContentCssSelectors();
          if (contentCssSelectors == null)
            contentCssSelectors = new ArrayList<String>();
          if (collRdfId != null) {
            ProjectCollection coll = ProjectReader.getInstance().getCollection(collRdfId);
            ArrayList<String> collContentCssSelectors = coll.getContentCssSelectors();
            if (collContentCssSelectors != null)
              contentCssSelectors.addAll(collContentCssSelectors);
            if (dbRdfId != null) {
              Database db = coll.getDatabase(dbRdfId);
              if (db != null) {
                ArrayList<String> dbContentCssSelectors = db.getContentCssSelectors();
                if (dbContentCssSelectors != null)
                  contentCssSelectors.addAll(dbContentCssSelectors);
              }
            }
          }
          if (contentCssSelectors.isEmpty()) {
            contentCssSelectors.add("html > body");  // default fallback selector
          }
          String contentCssSelectorsSelect = StringUtils.join(contentCssSelectors, ", ");
          Elements contentElems = doc.select(contentCssSelectorsSelect);
          if (! contentElems.isEmpty())
            docText = contentElems.text();
          else 
            docText = doc.text();
          docTokensOrig = docText;
        } else {
          DocumentParser tikaParser = new DocumentParser();
          try {
            IDocument tikaDoc = tikaParser.parse(docFileName);
            docTokensOrig = tikaDoc.getTextOrig();
            MetadataRecord tikaMDRecord = tikaDoc.getMetadata();
            if (tikaMDRecord != null) {
              int pageCount = tikaMDRecord.getPageCount();
              if (pageCount <= 0)
                pageCount = 1;  // each document at least has one page
              mdRecord.setPageCount(pageCount);
              if (mdRecord.getIdentifier() == null)
                mdRecord.setIdentifier(tikaMDRecord.getIdentifier());
              if (mdRecord.getCreator() == null) {
                String mimeType = mdRecord.getType();
                if (mimeType != null && mimeType.equals("application/pdf")) {
                  // nothing: creator is not the creator of the document in mostly all pdf-documents, so it is not considered
                } else {
                  String tikaCreator = tikaMDRecord.getCreator();
                  boolean isProper = metadataHandler.isProper("author", tikaCreator);
                  if (isProper)
                    mdRecord.setCreator(tikaCreator);
                }
              }
              if (mdRecord.getTitle() == null) {
                String mimeType = mdRecord.getType();
                if (mimeType != null && mimeType.equals("application/pdf")) {
                  // nothing: title is not the title of the document in mostly all pdf-documents, so it is not considered
                } else {
                  mdRecord.setTitle(tikaMDRecord.getTitle());
                }
              }
              if (mdRecord.getLanguage() == null) {
                String tikaLang = tikaMDRecord.getLanguage();
                if (tikaLang != null) {
                  String isoLang = Language.getInstance().getISO639Code(tikaLang);
                  mdRecord.setLanguage(isoLang);
                }
              }
              if (mdRecord.getPublisher() == null)
                mdRecord.setPublisher(tikaMDRecord.getPublisher());
              if (mdRecord.getDate() == null)
                mdRecord.setDate(tikaMDRecord.getDate());
              if (mdRecord.getDescription() == null)
                mdRecord.setDescription(tikaMDRecord.getDescription());
              if (mdRecord.getContributor() == null)
                mdRecord.setContributor(tikaMDRecord.getContributor());
              if (mdRecord.getCoverage() == null)
                mdRecord.setCoverage(tikaMDRecord.getCoverage());
              if (mdRecord.getSubject() == null)
                mdRecord.setSubject(tikaMDRecord.getSubject());
              if (mdRecord.getSwd() == null)
                mdRecord.setSwd(tikaMDRecord.getSwd());
              if (mdRecord.getDdc() == null)
                mdRecord.setDdc(tikaMDRecord.getDdc());
              if (mdRecord.getEncoding() == null)
                mdRecord.setEncoding(tikaMDRecord.getEncoding());
            }
          } catch (ApplicationException e) {
            LOGGER.error(e.getLocalizedMessage());
          }
        }
        String mdRecordLanguage = mdRecord.getLanguage();
        String mainLanguage = project.getMainLanguage();
        String collectionRdfId = mdRecord.getCollectionRdfId();
        if (collectionRdfId != null) {
          mainLanguage = ProjectReader.getInstance().getCollectionMainLanguage(collectionRdfId);
        }
        if (mdRecordLanguage == null && mainLanguage != null)
          mdRecord.setLanguage(mainLanguage);
        String lang = mdRecord.getLanguage();
        DocumentTokenizer docTokenizer = DocumentTokenizer.getInstance();
        String[] normFunctions = {"norm"};
        ArrayList<Token> normTokens = docTokenizer.getToken(docTokensOrig, lang, normFunctions);
        docTokensNorm = docTokenizer.buildStr(normTokens, lang, "norm");
        docTokensMorph = docTokenizer.buildStr(normTokens, lang, "morph");
        content = docTokensOrig;  // content is the same as docTokensOrig
        // fill mdRecord
        mdRecord.setTokenOrig(docTokensOrig);
        mdRecord.setTokenNorm(docTokensNorm);
        mdRecord.setTokenMorph(docTokensMorph);
        mdRecord.setContent(content);
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  private boolean copyUrlToFile(URL srcUrl, File docDestFile) {
    try {
      if (srcUrl != null) {
        FileUtils.copyURLToFile(srcUrl, docDestFile, 20000, 20000);
      }
    } catch (UnknownHostException e) {
      LOGGER.error("1. try: copyUrlToFile failed. UnknownHostException: " + e.getMessage() + " for Url: " + srcUrl);
      try {
        Thread.sleep(10000); // 10 secs delay
        FileUtils.copyURLToFile(srcUrl, docDestFile, 20000, 20000);
      } catch (IOException | InterruptedException e2) {
        LOGGER.error("2. try: copyUrlToFile failed. " + e2.getClass().getName() + " : " + e.getMessage() + " for Url: " + srcUrl);
        try {
          Thread.sleep(10000); // another 10 secs delay
          FileUtils.copyURLToFile(srcUrl, docDestFile, 20000, 20000);
        } catch (IOException | InterruptedException e3) {
          LOGGER.error("3. try: copyUrlToFile failed. " + e3.getClass().getName() + " : " + e.getMessage() + " for Url: " + srcUrl);
          FileUtils.deleteQuietly(docDestFile);
          return false;
        }
      }
    } catch (SocketTimeoutException e) {
      LOGGER.error("copyUrlToFile failed. Socket timeout for: " + srcUrl);
      FileUtils.deleteQuietly(docDestFile);
      return false;
    } catch (IOException e) {
      try {
        // second try with encoded blanks url
        String srcUrlFileEncodedStr = srcUrl.getFile().replaceAll(" ", "%20");
        String srcUrlEncodedStr = srcUrl.getProtocol() + "://" + srcUrl.getHost() + ":" + srcUrl.getPort() + srcUrlFileEncodedStr;
        URL srcUrlEncoded = new URL(srcUrlEncodedStr);
        FileUtils.copyURLToFile(srcUrlEncoded, docDestFile, 20000, 20000);
      } catch (IOException e2) {
        LOGGER.error("copyUrlToFile failed. IOException: " + e.getMessage() + " for Url: " + srcUrl);
        FileUtils.deleteQuietly(docDestFile);
        return false;
      }
    }
    return true;
  }

  
}
