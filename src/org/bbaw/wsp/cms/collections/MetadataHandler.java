package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.lucene.IndexHandler;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

public class MetadataHandler {
  private static Logger LOGGER = Logger.getLogger(MetadataHandler.class);
  private static MetadataHandler mdHandler;
  private CollectionReader collectionReader;

  public static MetadataHandler getInstance() throws ApplicationException {
    if(mdHandler == null) {
      mdHandler = new MetadataHandler();
      mdHandler.init();
    }
    return mdHandler;
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
  }
  
  private void end() throws ApplicationException {

  }

  public ArrayList<MetadataRecord> getMetadataRecords(Collection collection) throws ApplicationException {
    String collectionId = collection.getId();
    String metadataRdfDir = Constants.getInstance().getMetadataDir() + "/resources";
    File rdfRessourcesFile = new File(metadataRdfDir + "/" + collectionId + ".rdf");
    ArrayList<MetadataRecord> mdRecords = getMetadataRecordsByRdfFile(collection, rdfRessourcesFile, null, false);
    if (mdRecords.size() == 0)
      return null;
    else 
      return mdRecords;
  }

  public ArrayList<MetadataRecord> getMetadataRecords(Collection collection, Database db, boolean generateId) throws ApplicationException {
    ArrayList<MetadataRecord> dbMdRecords = null;
    String dbType = db.getType();
    if (dbType.equals("eXist")) {
      dbMdRecords = getMetadataRecordsEXist(collection, db, generateId);
    } else if (dbType.equals("crawl")) {
      dbMdRecords = getMetadataRecordsCrawl(collection, db, generateId);
    }
    return dbMdRecords;
  }
  
  private ArrayList<MetadataRecord> getMetadataRecordsEXist(Collection collection, Database db, boolean generateId) throws ApplicationException {
    String collectionId = collection.getId();
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    String urlStr = db.getUrl();
    Integer maxIdcounter = null;
    if (generateId)
      maxIdcounter = IndexHandler.getInstance().findMaxId();  // find the highest value, so that each following id is a real new id
    MetadataRecord mdRecord = getNewMdRecord(urlStr); 
    mdRecord.setSystem(db.getType());
    mdRecord.setCollectionNames(collectionId);
    if (generateId)
      mdRecord.setId(maxIdcounter); // collections wide id
    mdRecord = createMainFieldsMetadataRecord(mdRecord, collection, null);
    Date lastModified = new Date();
    mdRecord.setLastModified(lastModified);
    String language = db.getLanguage();
    if (language != null)
      mdRecord.setLanguage(language);
    String eXistUrl = mdRecord.getWebUri();
    if (eXistUrl != null) {
      ArrayList<MetadataRecord> mdRecordsEXist = new ArrayList<MetadataRecord>();
      if (eXistUrl.endsWith("/"))
        eXistUrl = eXistUrl.substring(0, eXistUrl.length() - 1);
      ArrayList<String> excludes = db.getExcludes();
      List<String> eXistUrls = extractDocumentUrls(eXistUrl, excludes);
      for (int i=0; i<eXistUrls.size(); i++) {
        String eXistSubUrlStr = eXistUrls.get(i);
        MetadataRecord mdRecordEXist = getNewMdRecord(eXistSubUrlStr); // with docId and webUri
        mdRecordEXist.setCollectionNames(collectionId);
        if (generateId) {
          maxIdcounter++;
          mdRecordEXist.setId(maxIdcounter); // collections wide id
        }
        mdRecordEXist = createMainFieldsMetadataRecord(mdRecordEXist, collection, db);
        mdRecordEXist.setLastModified(lastModified);
        mdRecordEXist.setCreator(mdRecord.getCreator());
        mdRecordEXist.setTitle(mdRecord.getTitle());
        mdRecordEXist.setPublisher(mdRecord.getPublisher());
        mdRecordEXist.setDate(mdRecord.getDate());
        mdRecordEXist.setSystem("eXistDir");
        if (mdRecord.getLanguage() != null)
          mdRecordEXist.setLanguage(mdRecord.getLanguage());
        mdRecordsEXist.add(mdRecordEXist);
      }
      mdRecords.addAll(mdRecordsEXist);
    }
    return mdRecords;
  }
  
  private ArrayList<MetadataRecord> getMetadataRecordsCrawl(Collection collection, Database db, boolean generateId) throws ApplicationException {
    String collectionId = collection.getId();
    Integer maxIdcounter = null;
    if (generateId)
      maxIdcounter = IndexHandler.getInstance().findMaxId();  // find the highest value, so that each following id is a real new id
    Date lastModified = new Date();
    String startUrl = db.getUrl();
    ArrayList<String> excludes = db.getExcludes();
    Integer depth = db.getDepth();
    Crawler crawler = new Crawler(startUrl, depth, excludes);
    ArrayList<MetadataRecord> crawledMdRecords = crawler.crawl();
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    for (int i=0; i<crawledMdRecords.size(); i++) {
      String crawlUrlStr = crawledMdRecords.get(i).getWebUri();
      MetadataRecord crawledMdRecord = getNewMdRecord(crawlUrlStr); // with docId and webUri
      crawledMdRecord.setCollectionNames(collectionId);
      if (generateId) {
        maxIdcounter++;
        crawledMdRecord.setId(maxIdcounter); // collections wide id
      }
      crawledMdRecord = createMainFieldsMetadataRecord(crawledMdRecord, collection, db);
      crawledMdRecord.setLastModified(lastModified);
      crawledMdRecord.setSystem("crawl");
      String dbLanguage = db.getLanguage();
      if (dbLanguage != null)
        crawledMdRecord.setLanguage(dbLanguage);
      mdRecords.add(crawledMdRecord);
    }
    return mdRecords;
  }
  
  public ArrayList<MetadataRecord> getMetadataRecordsByRdfFile(Collection collection, File rdfRessourcesFile, Database db, boolean generateId) throws ApplicationException {
    String collectionId = collection.getId();
    String xmlDumpFileStr = rdfRessourcesFile.getPath().replaceAll("\\.rdf", ".xml");
    File xmlDumpFile = new File(xmlDumpFileStr);
    Date xmlDumpFileLastModified = new Date(xmlDumpFile.lastModified());
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    try {
      URL rdfRessourcesFileUrl = rdfRessourcesFile.toURI().toURL();
      String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
      XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
      XdmValue xmdValueResources = xQueryEvaluator.evaluate(rdfRessourcesFileUrl, namespaceDeclaration + "/rdf:RDF/rdf:Description[rdf:type/@rdf:resource = 'http://purl.org/dc/terms/BibliographicResource']");
      XdmSequenceIterator xmdValueResourcesIterator = xmdValueResources.iterator();
      Integer maxIdcounter = null;
      if (generateId)
        maxIdcounter = IndexHandler.getInstance().findMaxId();  // find the highest value, so that each following id is a real new id
      if (xmdValueResources != null && xmdValueResources.size() > 0) {
        while (xmdValueResourcesIterator.hasNext()) {
          if (generateId)
            maxIdcounter++;
          XdmItem xdmItemResource = xmdValueResourcesIterator.next();
          String xdmItemResourceStr = xdmItemResource.toString();
          MetadataRecord mdRecord = getMdRecord(collection, xQueryEvaluator, xdmItemResourceStr);  // get the mdRecord out of rdf string
          if (mdRecord != null) {
            if (mdRecord.getCollectionNames() == null)
              mdRecord.setCollectionNames(collectionId);
            if (generateId)
              mdRecord.setId(maxIdcounter); // collections wide id
            mdRecord = createMainFieldsMetadataRecord(mdRecord, collection, db);
            // if it is a record: set lastModified to the date of the harvested dump file; if it is a normal web record: set it to now (harvested now)
            Date lastModified = new Date();
            if (mdRecord.isRecord()) {
              lastModified = xmlDumpFileLastModified;
            }
            mdRecord.setLastModified(lastModified);
            if(mdRecord.isDirectory()) {
              String fileDirUrlStr = mdRecord.getWebUri();
              if (fileDirUrlStr != null) {
                ArrayList<MetadataRecord> mdRecordsDir = new ArrayList<MetadataRecord>();
                String fileExtensions = "html;htm;pdf;doc";
                if (mdRecord.getType() != null)
                  fileExtensions = mdRecord.getType();
                String fileDirStr = null;
                try {
                  URL fileDirUrl = new URL(fileDirUrlStr);
                  String fileDirUrlHost = fileDirUrl.getHost();
                  String fileDirUrlPath = fileDirUrl.getFile();
                  fileDirStr = Constants.getInstance().getExternalDataDir() + "/" + fileDirUrlHost + fileDirUrlPath;
                } catch (MalformedURLException e) {
                  throw new ApplicationException(e);
                }
                java.util.Collection<File> files = getFiles(fileDirStr, fileExtensions);
                Iterator<File> filesIter = files.iterator();
                while (filesIter.hasNext()) {
                  maxIdcounter++;
                  File file = filesIter.next();
                  String fileAbsolutePath = file.getAbsolutePath();
                  String fileRelativePath = fileAbsolutePath.replaceFirst(fileDirStr, "");
                  String webUrl = fileDirUrlStr + fileRelativePath;
                  String uri = "file:" + fileAbsolutePath;
                  MetadataRecord mdRecordDirEntry = getNewMdRecord(webUrl);
                  mdRecordDirEntry.setCollectionNames(collectionId);
                  mdRecordDirEntry.setId(maxIdcounter); // collections wide id
                  mdRecordDirEntry = createMainFieldsMetadataRecord(mdRecordDirEntry, collection, null);
                  mdRecordDirEntry.setLastModified(lastModified);
                  mdRecordDirEntry.setUri(uri);
                  mdRecordsDir.add(mdRecordDirEntry);
                }
                mdRecords.addAll(mdRecordsDir);
              }
            }
            // do not add the eXistDir or directory itself as a record (only the normal resource mdRecord) 
            if (! mdRecord.isDirectory()) {
              mdRecords.add(mdRecord);
            }
          } 
        }
      }
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecords;
  }
  
  public void writeMetadataRecords(Collection collection, ArrayList<MetadataRecord> mdRecords, File rdfFile) throws ApplicationException {
    String collectionRdfId = collection.getRdfId();
    StringBuilder rdfStrBuilder = new StringBuilder();
    rdfStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    rdfStrBuilder.append("<rdf:RDF \n");
    rdfStrBuilder.append("   xmlns=\"" + collectionRdfId + "\" \n");
    rdfStrBuilder.append("   xml:base=\"" + collectionRdfId + "\" \n");
    rdfStrBuilder.append("   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
    rdfStrBuilder.append("   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n");
    rdfStrBuilder.append("   xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" \n");
    rdfStrBuilder.append("   xmlns:dc=\"http://purl.org/dc/elements/1.1/\" \n");
    rdfStrBuilder.append("   xmlns:dcterms=\"http://purl.org/dc/terms/\" \n");
    rdfStrBuilder.append("   xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" \n");
    rdfStrBuilder.append("   xmlns:gnd=\"http://d-nb.info/standards/elementset/gnd#\" \n");
    rdfStrBuilder.append("   xmlns:ore=\"http://www.openarchives.org/ore/terms/\" \n");
    rdfStrBuilder.append(">\n");
    rdfStrBuilder.append("<!-- Resources of database: " + rdfFile.getName() + " -->\n");
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      rdfStrBuilder.append(mdRecord.toRdfStr());
    }
    rdfStrBuilder.append("</rdf:RDF>");
    try {
      FileUtils.writeStringToFile(rdfFile, rdfStrBuilder.toString());
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  public File[] getRdfDbResourcesFiles(Collection collection, Database db) throws ApplicationException {
    String dbDumpsDirName = Constants.getInstance().getExternalDataDbDumpsDir();
    File dbDumpsDir = new File(dbDumpsDirName);
    String rdfFileFilterName = collection.getId() + "-" + db.getName() + "*.rdf"; 
    FileFilter rdfFileFilter = new WildcardFileFilter(rdfFileFilterName);
    File[] rdfDbResourcesFiles = dbDumpsDir.listFiles(rdfFileFilter);
    if (rdfDbResourcesFiles != null && rdfDbResourcesFiles.length > 0) {
      Arrays.sort(rdfDbResourcesFiles, new Comparator<File>() {
        public int compare(File f1, File f2) {
          return f1.getName().compareToIgnoreCase(f2.getName());
        }
      }); 
    }
    return rdfDbResourcesFiles;
  }
  
  private MetadataRecord createMainFieldsMetadataRecord(MetadataRecord mdRecord, Collection collection, Database db) throws ApplicationException {
    try {
      Tika tika = new Tika();
      String webUriStr = mdRecord.getWebUri();
      String uriStr = mdRecord.getUri();
      URL uri = new URL(webUriStr);
      if (uriStr != null)
        uri = new URL(uriStr);
      String uriPath = uri.getPath();
      String prefix = "/exist/rest/db";
      if (uriPath.startsWith(prefix)) {
        uriPath = uriPath.substring(prefix.length());
      }
      // no file with extension (such as a.xml or b.pdf) but a directory: then a special default file-name is used in docId
      if (! uriPath.toLowerCase().matches(".*\\.csv$|.*\\.gif$|.*\\.jpg$|.*\\.jpeg$|.*\\.html$|.*\\.htm$|.*\\.log$|.*\\.mp3$|.*\\.pdf$|.*\\.txt$|.*\\.xml$|.*\\.doc$")) {
        String mimeType = null;
        try {
          boolean isDbRecord = mdRecord.isDbRecord();
          if (! isDbRecord) {
            mimeType = tika.detect(uri); // much faster with tika
          }
          /* old code which is slow, when the files behind the url's are large 
          HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
          connection.setConnectTimeout(5000);
          connection.setReadTimeout(5000);
          connection.connect();
          mimeType = connection.getContentType();
          */
        } catch (IOException e) {
          LOGGER.error("get mime type failed for: " + webUriStr);
        }
        String fileExtension = "html";
        if (mimeType != null && mimeType.contains("html"))
          fileExtension = "html";
        else if (mimeType != null && mimeType.contains("pdf"))
          fileExtension = "pdf";
        else if (mimeType != null && mimeType.contains("xml"))
          fileExtension = "xml";
        else if (mimeType != null && mimeType.contains("png"))
          fileExtension = "png";
        else if (mimeType != null && mimeType.contains("jpg"))
          fileExtension = "jpg";
        int recordId = mdRecord.getId();
        String fileName = "indexxx" + recordId + "." + fileExtension;
        if (uriPath.endsWith("/"))
          uriPath = uriPath + fileName;
        else
          uriPath = uriPath + "/" + fileName;
      }
      String collectionId = collection.getId();
      String normalizedUriPath = normalize(uriPath);
      String docId = "/" + collectionId + normalizedUriPath;
      String mimeType = getMimeType(docId);
      if (mimeType == null) {  // last chance: try it with tika
        try {
          mimeType = tika.detect(uri);
        } catch (IOException e) {
          LOGGER.error("get mime type failed for: " + webUriStr);
          e.printStackTrace();
        }
      }
      mdRecord.setDocId(docId);
      if (mdRecord.getUri() == null)
        mdRecord.setUri(webUriStr);
      if (mdRecord.getType() == null)
        mdRecord.setType(mimeType); 
      // if no language is set then take the mainLanguage of the project
      if (mdRecord.getLanguage() == null) {
        String mainLanguage = collection.getMainLanguage();
        mdRecord.setLanguage(mainLanguage);
      }
      if (db != null) {
        Hashtable<String, XQuery> xQueries = db.getxQueries();
        if (xQueries != null)
          mdRecord.setxQueries(xQueries);
      }
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecord;
  }

  private List<String> extractDocumentUrls(String collectionDataUrl, ArrayList<String> excludes) {
    List<String> documentUrls = null;
    if (! collectionDataUrl.equals("")){
      PathExtractor extractor = new PathExtractor();
      documentUrls = extractor.initExtractor(collectionDataUrl, excludes);
    }
    return documentUrls;
  }

  private java.util.Collection<File> getFiles(String fileDirStr, String fileExtensions) throws ApplicationException {
    String[] fileExts = fileExtensions.split(";");
    for (int i=0; i<fileExts.length; i++) {
      String fileExt = fileExts[i];
      fileExts[i] = "." + fileExt;
    }
    SuffixFileFilter suffixFileFilter = new SuffixFileFilter(fileExts);
    File fileDir = new File(fileDirStr);
    java.util.Collection<File> files = FileUtils.listFiles(fileDir, suffixFileFilter, TrueFileFilter.INSTANCE);
    return files;
  }
  
  private String getMimeType(String docId) {
    String mimeType = null;
    FileNameMap fileNameMap = URLConnection.getFileNameMap();  // map with 53 entries such as "application/xml"
    mimeType = fileNameMap.getContentTypeFor(docId);
    return mimeType;
  }

  private MetadataRecord getMdRecord(Collection collection, XQueryEvaluator xQueryEvaluator, String xmlRdfStr) throws ApplicationException {
    String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
    // download url of resource
    String resourceIdUrlStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "string(/rdf:Description/dc:identifier/@rdf:resource)");
    resourceIdUrlStr = StringUtils.resolveXmlEntities(resourceIdUrlStr);
    if (resourceIdUrlStr == null || resourceIdUrlStr.trim().isEmpty()) {
      resourceIdUrlStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "string(/rdf:Description/@rdf:about)");
      if (resourceIdUrlStr == null || resourceIdUrlStr.trim().isEmpty())
        LOGGER.error("No identifier given in resource: \"" + xmlRdfStr + "\"");
    }
    MetadataRecord mdRecord = getNewMdRecord(resourceIdUrlStr);
    // web url of resource
    String resourceIdentifierStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:identifier/text()");
    if (resourceIdentifierStr != null && ! resourceIdentifierStr.isEmpty()) {
      resourceIdentifierStr = StringUtils.resolveXmlEntities(resourceIdentifierStr);
      try {
        new URL(resourceIdentifierStr); // test if identifier is a url, if so then set it as the webUri
        mdRecord.setWebUri(resourceIdentifierStr);
      } catch (MalformedURLException e) {
        // nothing
      }
      mdRecord.setUri(resourceIdUrlStr);
    }
    if (collection.getId().equals("edoc")) {
      String projectRdfStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "string(/rdf:Description/dcterms:isPartOf/@rdf:resource)");
      Collection c = CollectionReader.getInstance().getCollectionByProjectRdfId(projectRdfStr);
      if (c != null) {
        String collId = c.getId();
        mdRecord.setCollectionNames(collId);
      }
    }
    String type = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:type/text()");
    if (type != null)
      mdRecord.setSystem(type);  // e.g. "eXistDir" or "dbRecord" or "oai" or "directory" 
    XdmValue xmdValueAuthors = xQueryEvaluator.evaluate(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:creator");
    if (xmdValueAuthors != null && xmdValueAuthors.size() > 0) {
      XdmSequenceIterator xmdValueAuthorsIterator = xmdValueAuthors.iterator();
      ArrayList<Person> authors = new ArrayList<Person>();
      while (xmdValueAuthorsIterator.hasNext()) {
        XdmItem xdmItemAuthor = xmdValueAuthorsIterator.next();
        String name = xdmItemAuthor.getStringValue();
        if (name != null && ! name.isEmpty()) {
          name = name.replaceAll("\n|\\s\\s+", " ").trim();
          Person author = new Person(name);
          authors.add(author);
        } 
        String xdmItemAuthorStr = xdmItemAuthor.toString();
        String resourceId = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:creator/@*:resource)");
        if (resourceId != null && ! resourceId.isEmpty()) {
          Person creator = CollectionReader.getInstance().getPerson(resourceId.trim());
          if (creator != null)
            authors.add(creator);
        }
      }
      String creator = "";
      String creatorDetails = "<persons>";
      for (int i=0; i<authors.size(); i++) {
        Person author = authors.get(i);
        creator = creator + author.getName();
        if (authors.size() != 1)
          creator = creator + ". ";
        creatorDetails = creatorDetails + "\n" + author.toXmlStr();
      }
      creator = StringUtils.deresolveXmlEntities(creator.trim());
      creatorDetails = creatorDetails + "\n</persons>";
      if (creator != null && ! creator.isEmpty())
        mdRecord.setCreator(creator);
      if (authors != null && ! authors.isEmpty())
        mdRecord.setCreatorDetails(creatorDetails);
    }
    XdmValue xmdValueTitle = xQueryEvaluator.evaluate(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:title|/rdf:Description/dcterms:alternative");
    XdmSequenceIterator xmdValueTitleIterator = xmdValueTitle.iterator();
    if (xmdValueTitle != null && xmdValueTitle.size() > 0) {
      String title = "";
      while (xmdValueTitleIterator.hasNext()) {
        XdmItem xdmItemTitle = xmdValueTitleIterator.next();
        String xdmItemTitleStr = xdmItemTitle.getStringValue().trim();
        if (xmdValueTitle.size() == 1)
          title = xdmItemTitleStr;
        else
          title = title + xdmItemTitleStr + ". ";
      }
      title = StringUtils.resolveXmlEntities(title);
      mdRecord.setTitle(title);
    }
    String publisher = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:publisher/text()");
    if (publisher != null)
      publisher = StringUtils.resolveXmlEntities(publisher.trim());
    mdRecord.setPublisher(publisher);
    String subject = xQueryEvaluator.evaluateAsStringValueJoined(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:subject", "###");
    if (subject != null)
      subject = StringUtils.resolveXmlEntities(subject.trim());
    mdRecord.setSubject(subject);
    String subjectSwd = xQueryEvaluator.evaluateAsStringValueJoined(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:subject[@xsi:type = 'SWD']", ",");
    if (subjectSwd != null && ! subjectSwd.isEmpty()) {
      subjectSwd = StringUtils.resolveXmlEntities(subjectSwd.trim());
      if (subjectSwd.endsWith(","))
        subjectSwd = subjectSwd.substring(0, subjectSwd.length() - 1); 
    }
    mdRecord.setSwd(subjectSwd);
    String subjectDdc = xQueryEvaluator.evaluateAsStringValueJoined(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:subject[@xsi:type = 'dcterms:DDC']", ",");
    if (subjectDdc != null && ! subjectDdc.isEmpty()) {
      subjectDdc = StringUtils.resolveXmlEntities(subjectDdc.trim());
      mdRecord.setDdc(subjectDdc);
    }
    XdmValue xmdValueDcTerms = xQueryEvaluator.evaluate(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dcterms:subject");
    XdmSequenceIterator xmdValueDcTermsIterator = xmdValueDcTerms.iterator();
    if (xmdValueDcTerms != null && xmdValueDcTerms.size() > 0) {
      String subjectControlledDetails = "<subjects>";
      while (xmdValueDcTermsIterator.hasNext()) {
        XdmItem xdmItemDcTerm = xmdValueDcTermsIterator.next();
        /* e.g.:
         * <dcterms:subject>
             <rdf:Description rdf:about="http://de.dbpedia.org/resource/Kategorie:Karl_Marx">
               <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#Concept"/>
               <rdfs:label>Karl Marx</rdfs:label>
             </rdf:description>
           </dcterms:subject>
         */
        String xdmItemDcTermStr = xdmItemDcTerm.toString();
        subjectControlledDetails = subjectControlledDetails + "\n" + xdmItemDcTermStr;
      }
      subjectControlledDetails = subjectControlledDetails + "\n</subjects>";
      mdRecord.setSubjectControlledDetails(subjectControlledDetails);
    }
    String dateStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:date/text()");
    Date date = null;
    if (dateStr != null && ! dateStr.equals("")) {
      dateStr = StringUtils.resolveXmlEntities(dateStr.trim());
      dateStr = new Util().toYearStr(dateStr);  // test if possible etc
      if (dateStr != null) {
        try {
          date = new Util().toDate(dateStr + "-01-01T00:00:00.000Z");
        } catch (Exception e) {
          // nothing
        }
      }
    }
    mdRecord.setDate(date);
    String language = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:language/text()");
    mdRecord.setLanguage(language);
    String mimeType = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:format/text()");
    mdRecord.setType(mimeType);
    String abstractt = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:abstract/text()");
    if (abstractt != null)
      abstractt = StringUtils.resolveXmlEntities(abstractt.trim());
    mdRecord.setDescription(abstractt);
    String pagesStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:extent/text()");
    if (pagesStr != null && ! pagesStr.isEmpty()) {
      try {
        int pages = Integer.parseInt(pagesStr);
        mdRecord.setPageCount(pages);
      } catch (NumberFormatException e) {
        // nothing
      }
    }
    return mdRecord;    
  }

  private MetadataRecord getNewMdRecord(String urlStr) {
    MetadataRecord mdRecord = new MetadataRecord();
    try {
      URL resourceIdUrl = new URL(urlStr);
      String urlFilePath = resourceIdUrl.getFile();
      urlFilePath = StringUtils.deresolveXmlEntities(urlFilePath.trim());
      if (urlFilePath.startsWith("/"))
        urlFilePath = urlFilePath.substring(1);
      String docId = normalize(urlFilePath);
      mdRecord.setDocId(docId);
      mdRecord.setWebUri(urlStr);
    } catch (MalformedURLException e) {
      LOGGER.error("Malformed Url \"" +  urlStr + "\" in resource");
      return null;
    }
    return mdRecord;
  }
  
  /**
   * Normalizes an identifier (file path elements are shortened to maximal of 255 length)
   * @param urlFilePath
   * @return the normalized string
   */
  private String normalize(String urlFilePath) {
    String retStr = urlFilePath;
    if (urlFilePath != null && urlFilePath.contains("/") && urlFilePath.length() > 255) {
      retStr = "";
      String[] pathElems = urlFilePath.split("/");
      for (int i=0; i<pathElems.length; i++) {
        String pathElem = pathElems[i];
        if (pathElem.length() > 255)
          pathElem = pathElem.substring(0, 255);
        retStr = retStr + pathElem + "/";
      }
      retStr = retStr.substring(0, retStr.length() - 1); // without last "/"
    }
    return retStr;
  }

}
