package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.FileNameMap;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.dochandler.DocumentTokenizer;
import org.bbaw.wsp.cms.dochandler.parser.document.IDocument;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.DocumentParser;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.transform.XslResourceTransformer;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.Token;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizer;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

public class Harvester {
  private static Logger LOGGER = Logger.getLogger(Harvester.class);
  private static Harvester harvester;
  private CollectionReader collectionReader;
  private MetadataHandler metadataHandler;
  private String harvestDir;
  private StringBuilder harvestXmlStrBuilder;
  
  public static void main(String[] args) throws ApplicationException {
    try {
      LOGGER.info("Start of harvesting project resources");
      Harvester harvester = Harvester.getInstance();
      if (args != null && args.length != 0) {
        String operation = args[0];
        String projectId1 = args[1];
        String projectId2 = null;
        if (args.length > 2)
          projectId2 = args[2];
      } else {
        harvester.harvestCollections();
      }
      harvester.end();
      LOGGER.info("End of harvesting project resources");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static Harvester getInstance() throws ApplicationException {
    if(harvester == null) {
      harvester = new Harvester();
      harvester.init();
    }
    return harvester;
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    metadataHandler = MetadataHandler.getInstance();
    harvestDir = Constants.getInstance().getHarvestDir();
  }
  
  private void end() throws ApplicationException {

  }

  public void harvestCollections() throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      harvestCollection(collection);
    }
  }

  public void harvestCollection(Collection collection) throws ApplicationException {
    int counter = 0;
    String collId = collection.getId();
    LOGGER.info("Harvest collection: " + collId + " ...");
    initHarvestXmlStrBuilder(collection);
    ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecords(collection);
    if (mdRecords != null) {
      int countResources = harvestResources(collection, mdRecords); // download resources and save metadata and fulltext fields
      counter = counter + countResources;
    }
    ArrayList<Database> collectionDBs = collection.getDatabases();
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database collectionDB = collectionDBs.get(i);
        int countResources = harvestDB(collection, collectionDB);
        counter = counter + countResources;
      }
    }
    flushHarvestXmlStrBuilder(collection);
    LOGGER.info("Collection: " + collId + " with " + counter + " records harvested");
  }
  
  private int harvestDB(Collection collection, Database db) throws ApplicationException {
    int countHarvest = 0;
    String dbType = db.getType();
    if (dbType != null && (dbType.equals("eXist") || dbType.equals("crawl"))) {
      // download records and save them in RDF-file as records
      LOGGER.info("Harvest metadata records (" + collection.getId() + ", " + db.getName() + ", " + db.getType() + ") ...");
      ArrayList<MetadataRecord> dbMdRecords = harvestDBResources(collection, db);
      ArrayList<MetadataRecord> collectionMdRecords = metadataHandler.getMetadataRecords(collection);
      ArrayList<MetadataRecord> dbMdRecordsWithoutDoubleEntries = removeDoubleRecords(collectionMdRecords, dbMdRecords);
      // download resources and save metadata and fulltext fields
      if (dbMdRecords != null) {
        int countResources = harvestResources(collection, dbMdRecordsWithoutDoubleEntries);
        countHarvest = countHarvest + countResources;
      }
    } else if (dbType != null && (dbType.equals("oai"))) {
      LOGGER.info("Harvest metadata records (" + collection.getId() + ", " + db.getName() + ", " + db.getType() + ") ...");
      int countDbResources = harvestOaiDBResources(collection, db);
      countHarvest = countHarvest + countDbResources;
    } else if (dbType != null && (dbType.equals("mysql") || dbType.equals("postgres") || dbType.equals("oai-dbrecord"))) {
      // nothing, data remains in /dataExtern/dbDumps/
    } else {
      // nothing
    }
    return countHarvest;
  }
  
  private int harvestResources(Collection collection, ArrayList<MetadataRecord> mdRecords) throws ApplicationException {
    int countHarvest = 1;
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      try {
        harvestResource(countHarvest, collection, mdRecord);
        countHarvest++;
      } catch (Exception e) {
        LOGGER.error("Harvest of metadata record failed:");
        e.printStackTrace();
      }
    }
    countHarvest = countHarvest - 1;
    return countHarvest;
  }
  
  private ArrayList<MetadataRecord> harvestDBResources(Collection collection, Database db) throws ApplicationException {
    ArrayList<MetadataRecord> dbMdRecords = null;
    String dbType = db.getType();
    if (dbType.equals("eXist") || dbType.equals("crawl")) { 
      dbMdRecords = metadataHandler.getMetadataRecords(collection, db, false);
      String rdfDbFileName = collection.getId() + "/metadata/dbResources/" + collection.getId() + "-" + db.getName() + "-1.rdf";
      String rdfDbFullFileName = harvestDir + "/" + collection.getId() + "/metadata/dbResources/" + collection.getId() + "-" + db.getName() + "-1.rdf";
      File rdfDbFile = new File(rdfDbFullFileName);
      metadataHandler.writeMetadataRecords(collection, dbMdRecords, rdfDbFile);
      LOGGER.info("Harvest of metadata records (" + collection.getId() + ", " + db.getName() + ", " + db.getType() + ") finished (/harvest/" + rdfDbFileName  + ")");
    } else {
      // nothing
    }
    return dbMdRecords;
  }
  
  private int harvestOaiDBResources(Collection collection, Database db) throws ApplicationException {
    int count = -1;
    String dbType = db.getType();
    if (dbType.equals("oai") || dbType.equals("oai-dbrecord")) {
      String dbResourcesDirName = harvestDir + "/" + collection.getId() + "/metadata/dbResources";
      count = metadataHandler.fetchMetadataRecordsOai(dbResourcesDirName, collection, db);
      String rdfDbFileName = collection.getId() + "/metadata/dbResources/" + collection.getId() + "-" + db.getName() + "-*.rdf";
      LOGGER.info("Harvest of metadata records (" + collection.getId() + ", " + db.getName() + ", " + db.getType() + ") finished (/harvest/" + rdfDbFileName  + ")");
    }
    return count;
  }
  
  private void harvestResource(int counter, Collection collection, MetadataRecord mdRecord) throws ApplicationException {
    try {
      String docId = mdRecord.getDocId();
      String srcUrlStr = mdRecord.getUri();
      String baseDir = harvestDir + "/" + collection.getId() + "/data";
      String docDirName = getDocDir(docId, baseDir);
      String docDestFileName = getDocFullFileName(docId, baseDir); 
      LOGGER.info(counter + ". " + "Harvest resource: " + srcUrlStr + " (/harvest/" + collection.getId() + "/data" + docId + ")");
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
          return;
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
          docType = getNodeType(docNode);
          docType = docType.trim();
        } catch (ApplicationException e) {
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
        return;
      }
      // document is xml fulltext document: is of type XML and not mets
      String mainLanguage = collection.getMainLanguage();
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
        mdRecord = getMetadataRecord(docDestFileUpgrade, docType, mdRecord, xQueryEvaluator);
        String mdRecordLanguage = mdRecord.getLanguage();
        String langId = Language.getInstance().getLanguageId(mdRecordLanguage); // test if language code is supported
        if (langId == null)
          mdRecordLanguage = null;
        if (mdRecordLanguage == null && mainLanguage != null)
          mdRecord.setLanguage(mainLanguage);
      } else if (docIsXml && docType.equals("mets")) {
        mdRecord = getMetadataRecord(docDestFile, docType, mdRecord, xQueryEvaluator);
        String mdRecordLanguage = mdRecord.getLanguage();
        String langId = Language.getInstance().getLanguageId(mdRecordLanguage); // test if language code is supported
        if (langId == null)
          mdRecordLanguage = null;
        if (mdRecordLanguage == null && mainLanguage != null)
          mdRecord.setLanguage(mainLanguage);
      } 
      // build the documents fulltext fields
      if (srcUrl != null && docType != null && ! docType.equals("mets") && ! isDbRecord && isText)
        buildFulltextFields(collection, mdRecord, baseDir);
      harvestXmlStrBuilder.append(mdRecord.toXmlStr());
      String content = mdRecord.getContent();
      if (content != null) {
        File contentFile = new File(docDirName + "/content.txt");
        FileUtils.writeStringToFile(contentFile, content, "utf-8");
      }
      String contentXml = mdRecord.getContentXml();
      if (contentXml != null) {
        File contentXmlFile = new File(docDirName + "/content.xml");
        FileUtils.writeStringToFile(contentXmlFile, contentXml, "utf-8");
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

  private ArrayList<MetadataRecord> removeDoubleRecords(ArrayList<MetadataRecord> collectionMdRecords, ArrayList<MetadataRecord> dbMdRecords) {
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    for (int i=0; i<dbMdRecords.size(); i++) {
      MetadataRecord dbMdRecord = dbMdRecords.get(i);
      if (! isContained(collectionMdRecords, dbMdRecord))
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
  
  private MetadataRecord getMetadataRecord(File xmlFile, String schemaName, MetadataRecord mdRecord, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    if (schemaName == null)
      return mdRecord;
    try {
      URL srcUrl = xmlFile.toURI().toURL();
      if (schemaName.equals("TEI"))
        mdRecord = getMetadataRecordTei(xQueryEvaluator, srcUrl, mdRecord);
      else if (schemaName.equals("mets"))
        mdRecord = getMetadataRecordMets(xQueryEvaluator, srcUrl, mdRecord);
      else if (schemaName.equals("html"))
        mdRecord = getMetadataRecordHtml(xQueryEvaluator, srcUrl, mdRecord);
      else
        mdRecord.setSchemaName(schemaName); // all other cases: set docType to schemaName
      evaluateXQueries(xQueryEvaluator, srcUrl, mdRecord);
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecord;
  }

  private MetadataRecord evaluateXQueries(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) {
    Hashtable<String, XQuery> xqueriesHashtable = mdRecord.getxQueries();
    if (xqueriesHashtable != null) {
      Enumeration<String> keys = xqueriesHashtable.keys();
      while (keys != null && keys.hasMoreElements()) {
        String key = keys.nextElement();
        XQuery xQuery = xqueriesHashtable.get(key);
        String xQueryCode = xQuery.getCode();
        try {
          String xQueryResult = xQueryEvaluator.evaluateAsString(srcUrl, xQueryCode);
          if (xQueryResult != null)
            xQueryResult = xQueryResult.trim();
          xQuery.setResult(xQueryResult);
        } catch (Exception e) {
          // nothing
        }
      }
    }
    return mdRecord;
  }
  
  private MetadataRecord getMetadataRecordTei(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/*:TEI/*:teiHeader");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:idno");
      if (identifier != null)
        identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      String creator = null;
      String creatorDetails = null;
      XdmValue xmdValueAuthors = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:author");
      if (xmdValueAuthors != null && xmdValueAuthors.size() > 0) {
        XdmSequenceIterator xmdValueAuthorsIterator = xmdValueAuthors.iterator();
        ArrayList<Person> authors = new ArrayList<Person>();
        while (xmdValueAuthorsIterator.hasNext()) {
          Person author = new Person();
          XdmItem xdmItemAuthor = xmdValueAuthorsIterator.next();
          String xdmItemAuthorStr = xdmItemAuthor.toString();
          String name = xdmItemAuthor.getStringValue();
          boolean isProper = isProper("author", name);
          if (! isProper)
            name = "";
          if (name != null) {
            name = name.replaceAll("\n|\\s\\s+", " ").trim();
          }
          String reference = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:author/*:persName/@ref)");
          if (reference != null && ! reference.isEmpty())
            author.setRef(reference);
          String forename = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:author/*:persName/*:forename)");
          if (forename != null && ! forename.isEmpty())
            author.setForename(forename.trim());
          String surname = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:author/*:persName/*:surname)");
          if (surname != null && ! surname.isEmpty())
            author.setSurname(surname.trim());
          if (surname != null && ! surname.isEmpty() && forename != null && ! forename.isEmpty())
            name = surname.trim() + ", " + forename.trim();
          else if (surname != null && ! surname.isEmpty() && (forename == null || forename.isEmpty()))
            name = surname.trim();
          if (! name.isEmpty()) {
            author.setName(name);
            authors.add(author);
          }
        }
        creator = "";
        creatorDetails = "<persons>";
        for (int i=0; i<authors.size(); i++) {
          Person author = authors.get(i);
          creator = creator + author.getName();
          if (authors.size() != 1)
            creator = creator + ". ";
          creatorDetails = creatorDetails + "\n" + author.toXmlStr();
        }
        creator = StringUtils.deresolveXmlEntities(creator.trim());
        creatorDetails = creatorDetails + "\n</persons>";
        if (creator.isEmpty())
          creator = null;
        if (authors.isEmpty())
          creatorDetails = null;
      }
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title");
      XdmValue xdmValueTitleIdno = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title/*:idno"); // e.g. in http://telotadev.bbaw.de:8078/exist/rest/db/WvH/Briefe/1827-02-20_RosenFriedrichAugust-WvH_481.xml
      if (xdmValueTitleIdno != null && xdmValueTitleIdno.size() > 0)
        title = xQueryEvaluator.evaluateAsString(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title/text()");
      XdmValue xdmValueTitleMain = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title[@type='main']");
      XdmValue xdmValueTitleShort = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title[@type='short']");
      if (xdmValueTitleShort != null && xdmValueTitleShort.size() > 0) {
        try {
          title = xdmValueTitleShort.getUnderlyingValue().getStringValue();
        } catch (XPathException e) {
          // nothing
        }
      } else if (xdmValueTitleMain != null && xdmValueTitleMain.size() > 0) {
        try {
          title = xdmValueTitleMain.getUnderlyingValue().getStringValue();
        } catch (XPathException e) {
          // nothing
        }
      }
      if (title != null) {
        title = StringUtils.deresolveXmlEntities(title.trim());
        if (title.isEmpty())
          title = null;
      }
      String language = null;
      String countLanguages = xQueryEvaluator.evaluateAsString(metadataXmlStr, "string(count(/*:teiHeader/*:profileDesc/*:langUsage/*:language))");
      if (countLanguages != null && countLanguages.equals("1"))
        language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/*:teiHeader/*:profileDesc/*:langUsage/*:language[1]/@ident)");
      if (language != null && language.isEmpty())
        language = null;
      if (language != null) {
        language = StringUtils.deresolveXmlEntities(language.trim());
        language = Language.getInstance().getISO639Code(language);
      }
      String publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:publisher/*:name", ", ");
      if (publisher == null || publisher.isEmpty())
        publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:publisher", ", ");
      if (publisher == null || publisher.isEmpty())
        publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:publisher", ", ");
      if (publisher == null || publisher.isEmpty())
        publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:bibl/*:publisher", ", ");
      if (publisher != null) {
        publisher = StringUtils.deresolveXmlEntities(publisher.trim());
        if (publisher.isEmpty())
          publisher = null;
      }
      String place = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:pubPlace", ", ");
      if (place == null || place.isEmpty())
        place = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:pubPlace");
      if (place != null) {
        place = StringUtils.deresolveXmlEntities(place.trim());
        if (place.isEmpty())
          place = null;
      }
      String publisherStr = null;
      boolean publisherEndsWithComma = false;
      if (publisher != null)
        publisherEndsWithComma = publisher.lastIndexOf(",") == publisher.length() - 1;
      if (publisher == null && place != null)
        publisherStr = place;
      else if (publisher != null && place == null)
        publisherStr = publisher;
      else if (publisher != null && place != null && publisherEndsWithComma)
        publisherStr = publisher + " " + place;
      else if (publisher != null && place != null && ! publisherEndsWithComma)
        publisherStr = publisher + ", " + place;
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:date");
      if (yearStr == null || yearStr.isEmpty())
        yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:date");
      if (yearStr == null || yearStr.isEmpty())
        yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:bibl/*:date", ", ");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr.trim());
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null) {
          try {
            date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          } catch (Exception e) {
            // nothing
          }
        }
      }
      String subject = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:profileDesc/*:textClass/*:keywords/*:term");
      if (subject != null) {
        subject = StringUtils.deresolveXmlEntities(subject.trim());
      }
      String rights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:availability");
      if (rights == null || rights.trim().isEmpty())
        rights = "open access";
      rights = StringUtils.deresolveXmlEntities(rights.trim());
      String license = "http://echo.mpiwg-berlin.mpg.de/policy/oa_basics/declaration";
      String accessRights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/*:teiHeader/*:fileDesc/*:publicationStmt/*:availability/@status)");
      if (accessRights == null || accessRights.trim().isEmpty()) 
        accessRights = "free";
      accessRights = StringUtils.deresolveXmlEntities(accessRights.trim());
      mdRecord.setIdentifier(identifier);
      // set language onto mdRecord if it is not null else the mdReord language beware its language 
      if (language != null)
        mdRecord.setLanguage(language);
      if (mdRecord.getCreator() == null)
        mdRecord.setCreator(creator);
      if (mdRecord.getCreatorDetails() == null)
        mdRecord.setCreatorDetails(creatorDetails);
      if (mdRecord.getTitle() == null)
        mdRecord.setTitle(title);
      if (mdRecord.getPublisher() == null)
        mdRecord.setPublisher(publisherStr);
      if (mdRecord.getRights() == null)
        mdRecord.setRights(rights);
      if (mdRecord.getDate() == null)
        mdRecord.setDate(date);
      if (mdRecord.getSubject() == null)
        mdRecord.setSubject(subject);
      if (mdRecord.getLicense() == null)
        mdRecord.setLicense(license);
      if (mdRecord.getAccessRights() == null)
        mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//*:pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("TEI");
    return mdRecord;
  }

  private MetadataRecord getMetadataRecordMets(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStrDmd = xQueryEvaluator.evaluateAsString(srcUrl, "/*:mets/*:dmdSec");
    String metadataXmlStrAmd = xQueryEvaluator.evaluateAsString(srcUrl, "/*:mets/*:amdSec");
    if (metadataXmlStrDmd != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "string(/*:dmdSec/@ID)");
      if (identifier != null)
        identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      String webUri = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:digiprovMD/*:mdWrap/*:xmlData/*:links/*:presentation");
      if (webUri != null)
        webUri = StringUtils.deresolveXmlEntities(webUri.trim());
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:name");
      if (creator != null) {
        creator = StringUtils.deresolveXmlEntities(creator.trim());
        if (creator.isEmpty())
          creator = null;
      }
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:titleInfo");
      if (title != null)
        title = StringUtils.deresolveXmlEntities(title.trim());
      String language = null;  // TODO
      // String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "string(/*:teiHeader/*:profileDesc/*:langUsage/*:language[1]/@ident)");
      // if (language != null && language.isEmpty())
      //   language = null;
      // if (language != null) {
      //   language = StringUtils.deresolveXmlEntities(language.trim());
      //   language = Language.getInstance().getISO639Code(language);
      // }
      String publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:rightsMD/*:mdWrap/*:xmlData/*:rights/*:owner", ", ");
      if (publisher != null)
        publisher = StringUtils.deresolveXmlEntities(publisher.trim());
      String place = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:originInfo/*:place/*:placeTerm", ", ");
      if (place != null)
        place = StringUtils.deresolveXmlEntities(place.trim());
      String publisherStr = null;
      boolean publisherEndsWithComma = false;
      if (publisher != null)
        publisherEndsWithComma = publisher.lastIndexOf(",") == publisher.length() - 1;
      if (publisher == null && place != null)
        publisherStr = place;
      else if (publisher != null && place == null)
        publisherStr = publisher;
      else if (publisher != null && place != null && publisherEndsWithComma)
        publisherStr = publisher + " " + place;
      else if (publisher != null && place != null && ! publisherEndsWithComma)
        publisherStr = publisher + ", " + place;
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:originInfo/*:dateIssued");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr.trim());
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null) {
          try {
            date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          } catch (Exception e) {
            // nothing
          }
        }
      }
      String subject = null;  // TODO
      // String subject = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:teiHeader/*:profileDesc/*:textClass/*:keywords/*:term");
      // if (subject != null)
      //   subject = StringUtils.deresolveXmlEntities(subject.trim());
      String rightsOwner = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:rightsMD/*:mdWrap/*:xmlData/*:rights/*:owner");
      String rightsOwnerUrl = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:rightsMD/*:mdWrap/*:xmlData/*:rights/*:ownerSiteURL");
      String rights = null;
      if (rightsOwner != null)
        rights = StringUtils.deresolveXmlEntities(rightsOwner.trim());
      if (rightsOwner != null && rightsOwnerUrl != null)
        rights = rights + ", " + StringUtils.deresolveXmlEntities(rightsOwnerUrl.trim());
      else if (rightsOwner == null && rightsOwnerUrl != null)
        rights = StringUtils.deresolveXmlEntities(rightsOwnerUrl.trim());
      mdRecord.setIdentifier(identifier);
      mdRecord.setWebUri(webUri);
      mdRecord.setLanguage(language);
      if (mdRecord.getCreator() == null)
        mdRecord.setCreator(creator);
      if (mdRecord.getTitle() == null)
        mdRecord.setTitle(title);
      if (mdRecord.getPublisher() == null)
        mdRecord.setPublisher(publisherStr);
      if (mdRecord.getRights() == null)
        mdRecord.setRights(rights);
      if (mdRecord.getDate() == null)
        mdRecord.setDate(date);
      if (mdRecord.getSubject() == null)
        mdRecord.setSubject(subject);
    }
    mdRecord.setSchemaName("mets");
    return mdRecord;
  }

  private MetadataRecord getMetadataRecordHtml(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/html/head");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.identifier']/@content)");
      if (identifier != null && ! identifier.isEmpty())
        identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.creator']/@content)");
      boolean isProper = isProper("author", creator);
      if (! isProper)
        creator = "";
      if (creator != null) {
        creator = StringUtils.deresolveXmlEntities(creator.trim());
        if (creator.isEmpty())
          creator = null;
      }      
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.title']/@content)");
      if (title == null || title.isEmpty()) {
        title = xQueryEvaluator.evaluateAsString(metadataXmlStr, "string(/head/title)");
      }
      if (title != null) {
        title = StringUtils.deresolveXmlEntities(title.trim());
        if (title.isEmpty())
          title = null;
      }
      String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.language']/@content)");
      if (language != null && language.isEmpty())
        language = null;
      if (language != null && ! language.isEmpty()) {
        language = StringUtils.deresolveXmlEntities(language.trim());
        language = Language.getInstance().getISO639Code(language);
      }
      String publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.publisher']/@content)");
      if (publisher != null) {
        publisher = StringUtils.deresolveXmlEntities(publisher.trim());
        if (publisher.isEmpty())
          publisher = null;
      }
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.date']/@content)");
      Date date = null; 
      if (yearStr != null) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr.trim());
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null) {
          try {
            date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          } catch (Exception e) {
            // nothing
          }
        }
      }
      String subject = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.subject']/@content)");
      if (subject != null) {
        subject = StringUtils.deresolveXmlEntities(subject.trim());
        if (subject.isEmpty())
          subject = null;
      }
      String description = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.description']/@content)");
      if (description != null) {
        description = StringUtils.deresolveXmlEntities(description.trim());
        if (description.isEmpty())
          description = null;
      }
      String rights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.rights']/@content)");
      if (rights != null) {
        rights = StringUtils.deresolveXmlEntities(rights.trim());
        if (rights.isEmpty())
          rights = null;
      }
      String license = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.license']/@content)");
      if (license != null) {
        license = StringUtils.deresolveXmlEntities(license.trim());
        if (license.isEmpty())
          license = null;
      }
      String accessRights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.accessRights']/@content)");
      if (accessRights != null) {
        accessRights = StringUtils.deresolveXmlEntities(accessRights.trim());
        if (accessRights.isEmpty())
          accessRights = null;
      }
      mdRecord.setIdentifier(identifier);
      mdRecord.setLanguage(language);
      if (mdRecord.getCreator() == null)
        mdRecord.setCreator(creator);
      if (mdRecord.getTitle() == null)
        mdRecord.setTitle(title);
      if (mdRecord.getPublisher() == null)
        mdRecord.setPublisher(publisher);
      if (mdRecord.getRights() == null)
        mdRecord.setRights(rights);
      if (mdRecord.getDate() == null)
        mdRecord.setDate(date);
      if (mdRecord.getSubject() == null)
        mdRecord.setSubject(subject);
      if (mdRecord.getDescription() == null)
        mdRecord.setDescription(description);
      if (mdRecord.getLicense() == null)
        mdRecord.setLicense(license);
      if (mdRecord.getAccessRights() == null)
        mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("html");
    return mdRecord;
  }

  private void buildFulltextFields(Collection collection, MetadataRecord mdRecord, String baseDir) throws ApplicationException {
    try {
      String docId = mdRecord.getDocId();
      String language = mdRecord.getLanguage();
      boolean docIsXml = isDocXml(docId);
      String docTokensOrig = null;
      String docTokensNorm = null;
      String docTokensMorph = null;
      String contentXml = null;
      String content = null;
      String docFileName = getDocFullFileName(docId, baseDir);
      XmlTokenizer docXmlTokenizer = null;
      if (docIsXml) {
        docFileName = getDocFullFileName(docId, baseDir) + ".upgrade";
        // remove the TEI header part
        XslResourceTransformer removeElemTransformer = new XslResourceTransformer("removeElement.xsl");
        QName elemNameQName = new QName("elemName");
        XdmValue elemNameXdmValue = new XdmAtomicValue("teiHeader");  // TODO fetch name from collection configuration
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
        // fetch original xml content of the documents file
        contentXml = FileUtils.readFileToString(docFile, "utf-8");
        // fetch original content of the documents file (without xml tags)
        XslResourceTransformer charsTransformer = new XslResourceTransformer("chars.xsl");
        content = charsTransformer.transform(docFileName);
        // fill mdRecord
        mdRecord.setContent(content);
        mdRecord.setContentXml(contentXml);
        mdRecord.setTokenOrig(docTokensOrig);
        mdRecord.setTokenNorm(docTokensNorm);
        mdRecord.setTokenMorph(docTokensMorph);
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
                // nothing: creator is not the creator of the document in mostly all pdf-documents, so is not considered
              } else {
                String tikaCreator = tikaMDRecord.getCreator();
                boolean isProper = isProper("author", tikaCreator);
                if (isProper)
                  mdRecord.setCreator(tikaCreator);
              }
            }
            if (mdRecord.getTitle() == null) {
              String mimeType = mdRecord.getType();
              if (mimeType != null && mimeType.equals("application/pdf")) {
                // nothing: title is not the title of the document in mostly all pdf-documents, so is not considered
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
        String mdRecordLanguage = mdRecord.getLanguage();
        String mainLanguage = collection.getMainLanguage();
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
  
  private boolean isProper(String fieldName, String fieldValue) {
    if (fieldValue == null)
      return true;
    boolean isProper = true;
    if (fieldName != null && fieldName.equals("author") && (fieldValue.equals("admin") || fieldValue.equals("user")))
      return false;
    return isProper;
  }

  private void initHarvestXmlStrBuilder(Collection collection) {
    harvestXmlStrBuilder = new StringBuilder();
    harvestXmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    harvestXmlStrBuilder.append("<records>\n");
  }
  
  private void flushHarvestXmlStrBuilder(Collection collection) throws ApplicationException {
    harvestXmlStrBuilder.append("</records>\n");
    String harvestXmlStr = harvestXmlStrBuilder.toString();
    boolean containsHarvestedResources = true;
    if (harvestXmlStr.endsWith("<records>\n"))
      containsHarvestedResources = false;
    if (containsHarvestedResources) {
      String collId = collection.getId();
      try {
        FileUtils.writeStringToFile(new File(harvestDir + "/" + collId + "/metadata/records/" + collId + ".xml"), harvestXmlStr, "utf-8");
      } catch (IOException e) {
        throw new ApplicationException(e);
      }
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
