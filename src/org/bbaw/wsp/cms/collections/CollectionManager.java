package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.bbaw.wsp.cms.dochandler.DocumentHandler;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.EdocIndexMetadataFetcherTool;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

public class CollectionManager {
  private static Logger LOGGER = Logger.getLogger(CollectionManager.class);
  private static CollectionManager confManager;
  private static int COMMIT_INTERVAL_DB = 1000;
  private CollectionReader collectionReader;

  public static CollectionManager getInstance() throws ApplicationException {
    if(confManager == null) {
      confManager = new CollectionManager();
      confManager.init();
    }
    return confManager;
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
  }
  
  /**
   * Update of all collections
   * @throws ApplicationException
   */
  public void updateCollections() throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      addDocuments(collection, true);
    }
  }

  /**
   * Update all collections with these collection ids
   * @throws ApplicationException
   */
  public void updateCollections(String[] collectionIds) throws ApplicationException {
    updateCollections(collectionIds, true);
  }
  
  /**
   * Update of all collections from that startingCollection onwards till endingCollectionId alphabetically
   * @throws ApplicationException
   */
  public void updateCollections(String startingCollectionId, String endingCollectionId) throws ApplicationException {
    updateCollections(startingCollectionId, endingCollectionId, true);
  }
  
  /**
   * Update of all collections from that startingCollection onwards alphabetically
   * @throws ApplicationException
   */
  public void updateCollections(String startingCollectionId, boolean withDatabases) throws ApplicationException {
    boolean start = false;
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      String collId = collection.getId();
      if (startingCollectionId.equals(collId))
        start = true;
      if (start)
        addDocuments(collection, withDatabases);
    }
  }

  /**
   * Update of all collections from that startingCollection onwards till endingCollectionId alphabetically
   * @throws ApplicationException
   */
  public void updateCollections(String startingCollectionId, String endingCollectionId, boolean withDatabases) throws ApplicationException {
    boolean start = false;
    boolean end = false;
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      String collId = collection.getId();
      if (startingCollectionId.equals(collId))
        start = true;
      if (start && ! end)
        addDocuments(collection, withDatabases);
      if (endingCollectionId.equals(collId)) {
        end = true;
        break;
      }
    }
  }

  /**
   * Update all collections with these collection ids
   * @throws ApplicationException
   */
  public void updateCollections(String[] collectionIds, boolean withDatabases) throws ApplicationException {
    for (String collId : collectionIds) {
      Collection collection = collectionReader.getCollection(collId);
      if (collection != null)
        addDocuments(collection, withDatabases);
      else
        LOGGER.info("Update collection not possible. Collection id: " + collId + " does not exist");
    }
  }

  /**
   * Delete all collections from that startingCollection onwards alphabetically
   * @throws ApplicationException
   */
  public void deleteCollections(String startingCollectionId) throws ApplicationException {
    boolean start = false;
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      String collId = collection.getId();
      if (startingCollectionId.equals(collId))
        start = true;
      if (start)
        deleteCollection(collId);
    }
  }

  /**
   * Delete all collections from that startingCollection onwards till endingCollectionId alphabetically
   * @throws ApplicationException
   */
  public void deleteCollections(String startingCollectionId, String endingCollectionId) throws ApplicationException {
    boolean start = false;
    boolean end = false;
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      String collId = collection.getId();
      if (startingCollectionId.equals(collId))
        start = true;
      if (start && ! end)
        deleteCollection(collId);
      if (endingCollectionId.equals(collId)) {
        end = true;
        break;
      }
    }
  }

  /**
   * Update of all collections with these collection ids
   * @throws ApplicationException
   */
  public void deleteCollections(String[] collectionIds) throws ApplicationException {
    for (String collId : collectionIds) {
      Collection collection = collectionReader.getCollection(collId);
      if (collection != null)
        deleteCollection(collId);
      else
        LOGGER.info("Delete collection not possible. Collection id: " + collId + " does not exist");
    }
  }

  /**
   * Update of the collection with that collectionId
   * @param collectionId
   * @throws ApplicationException
   */
  public void updateCollection(String collectionId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(collectionId);
    addDocuments(collection, true);
  }

  public void updateCollection(String collectionId, boolean withDatabases) throws ApplicationException {
    Collection collection = collectionReader.getCollection(collectionId);
    addDocuments(collection, withDatabases);
  }

  public void deleteCollection(String collectionId) throws ApplicationException {
    DocumentHandler docHandler = new DocumentHandler();
    CmsDocOperation docOp = new CmsDocOperation("deleteCollection", null, null, null);
    docOp.setCollectionId(collectionId);
    try {
      docHandler.doOperation(docOp);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void addDocuments(Collection collection, boolean withDatabases) throws ApplicationException {
    int counter = 0;
    String collId = collection.getId();
    LOGGER.info("Create collection: " + collId + " ...");
    initDBpediaSpotlightWriter(collection);
    // first add the manually maintained mdRecords
    ArrayList<MetadataRecord> mdRecords = getMetadataRecords(collection);
    if (mdRecords != null) {
      int countDocs = addDocuments(collection, mdRecords, false);
      counter = counter + countDocs;
      ArrayList<Database> collectionDBs = collection.getDatabases();
      // add the database records 
      if (collectionDBs != null && withDatabases) {
        for (int i=0; i<collectionDBs.size(); i++) {
          Database collectionDB = collectionDBs.get(i);
          countDocs = addDocuments(collection, collectionDB);
          counter = counter + countDocs;
        }
      }
    }
    flushDBpediaSpotlightWriter(collection);
    LOGGER.info("Collection: " + collId + " with: " + counter + " records created");
  }

  private int addDocuments(Collection collection, Database db) throws ApplicationException {
    int counter = 0;
    String dbResourcesDirName = Constants.getInstance().getExternalDocumentsDir() + "/db-resources";
    File dbResourcesDir = new File(dbResourcesDirName);
    String rdfFileFilterName = collection.getId() + "-" + db.getName() + "*.rdf"; 
    FileFilter rdfFileFilter = new WildcardFileFilter(rdfFileFilterName);
    File[] rdfDbResourcesFiles = dbResourcesDir.listFiles(rdfFileFilter);
    if (rdfDbResourcesFiles != null && rdfDbResourcesFiles.length > 0) {
      Arrays.sort(rdfDbResourcesFiles, new Comparator<File>() {
        public int compare(File f1, File f2) {
          return f1.getName().compareToIgnoreCase(f2.getName());
        }
      }); 
      // add the database records of each database of each database file
      for (int i = 0; i < rdfDbResourcesFiles.length; i++) {
        File rdfDbResourcesFile = rdfDbResourcesFiles[i];
        LOGGER.info("Create database resources from file: " + rdfDbResourcesFile);
        ArrayList<MetadataRecord> dbMdRecords = getMetadataRecordsByRdfFile(collection, rdfDbResourcesFile);
        int countDocs = addDocuments(collection, dbMdRecords, true);
        counter = counter + countDocs;
      }
    }
    return counter;
  }
  
  private int addDocuments(Collection collection, ArrayList<MetadataRecord> mdRecords, boolean dbRecords) throws ApplicationException {
    DocumentHandler docHandler = new DocumentHandler();
    int counter = 0;
    // performance gain for database records: a commit is only done if commitInterval (e.g. 1000) is reached 
    if (dbRecords)
      IndexHandler.getInstance().setCommitInterval(COMMIT_INTERVAL_DB);
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      String docUrl = mdRecord.getUri();
      String docId = mdRecord.getDocId();
      String collectionId = mdRecord.getCollectionNames();
      counter++;
      String logCreationStr = counter + ". " + "Collection: " + collectionId + ": Create resource: " + docUrl + " (" + docId + ")";
      if (docUrl == null)
        logCreationStr = counter + ". " + "Collection: " + collectionId + ": Create metadata resource: " + docId;
      // if isDbColl then log only after each commit interval 
      if (! dbRecords) {
        LOGGER.info(logCreationStr);
      } else {
        if (counter % COMMIT_INTERVAL_DB == 0)
          LOGGER.info(logCreationStr);
      }
      CmsDocOperation docOp = new CmsDocOperation("create", docUrl, null, docId);
      docOp.setMdRecord(mdRecord);
      ArrayList<String> fields = collection.getFields();
      if (fields != null && ! fields.isEmpty()) {
        String[] fieldsArray = new String[fields.size()];
        for (int j=0;j<fields.size();j++) {
          String f = fields.get(j);
          fieldsArray[j] = f;
        }
        docOp.setElementNames(fieldsArray);
      }
      String mainLanguage = collection.getMainLanguage();
      docOp.setMainLanguage(mainLanguage);
      try {
        docHandler.doOperation(docOp);
      } catch (Exception e) {
        e.printStackTrace();
      }
      // without that, there would be a memory leak (with many big documents in one collection)
      // with that the main big fields (content etc.) could be garbaged
      mdRecord.setAllNull();
    }
    IndexHandler.getInstance().commit();  // so that the last pending mdRecords before commitInterval were also committed
    return counter;
  }
  
  private void initDBpediaSpotlightWriter(Collection collection) {
    StringBuilder dbPediaSpotlightCollectionRdfStrBuilder = collection.getDbPediaSpotlightRdfStrBuilder();
    if (dbPediaSpotlightCollectionRdfStrBuilder == null) {
      dbPediaSpotlightCollectionRdfStrBuilder = new StringBuilder();
      dbPediaSpotlightCollectionRdfStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      dbPediaSpotlightCollectionRdfStrBuilder.append("<rdf:RDF \n");
      dbPediaSpotlightCollectionRdfStrBuilder.append("   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n");
      dbPediaSpotlightCollectionRdfStrBuilder.append("   xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" \n");
      dbPediaSpotlightCollectionRdfStrBuilder.append("   xmlns:dc=\"http://purl.org/dc/elements/1.1/\" \n");
      dbPediaSpotlightCollectionRdfStrBuilder.append("   xmlns:gnd=\"http://d-nb.info/standards/elementset/gnd#/\" \n");
      dbPediaSpotlightCollectionRdfStrBuilder.append("   xmlns:dcterms=\"http://purl.org/dc/terms/\" \n");
      dbPediaSpotlightCollectionRdfStrBuilder.append("   xmlns:dbpedia-spotlight=\"http://spotlight.dbpedia.org/spotlight#\" \n");
      dbPediaSpotlightCollectionRdfStrBuilder.append(">\n");
      collection.setDbPediaSpotlightRdfStrBuilder(dbPediaSpotlightCollectionRdfStrBuilder);
    }
  }
  
  private void flushDBpediaSpotlightWriter(Collection collection) throws ApplicationException {
    // Write the dbPediaSpotlight entities of all collection resources to one rdf file 
    String collId = collection.getId();
    StringBuilder dbPediaSpotlightCollectionRdfStrBuilder = collection.getDbPediaSpotlightRdfStrBuilder();
    if (dbPediaSpotlightCollectionRdfStrBuilder != null) {
      dbPediaSpotlightCollectionRdfStrBuilder.append("</rdf:RDF>\n"); // 
      String dbPediaSpotlightDirName = Constants.getInstance().getExternalDocumentsDir() +  "/dbPediaSpotlightResources";
      String spotlightAnnotationRdfStr = dbPediaSpotlightCollectionRdfStrBuilder.toString();
      try {
        FileUtils.writeStringToFile(new File(dbPediaSpotlightDirName + "/" + collId + ".rdf"), spotlightAnnotationRdfStr, "utf-8");
      } catch (IOException e) {
        throw new ApplicationException(e);
      }
      collection.setDbPediaSpotlightRdfStrBuilder(null);
    }
  }
  
  public ArrayList<MetadataRecord> getMetadataRecords(Collection collection) throws ApplicationException {
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    String collectionId = collection.getId();
    if (collectionId.equals("edoc")) {
      mdRecords = getMetadataRecordsEdoc(collection);
    } else {
      String metadataRdfDir = Constants.getInstance().getMetadataDir() + "/resources";
      File rdfRessourcesFile = new File(metadataRdfDir + "/" + collectionId + ".rdf");
      mdRecords = getMetadataRecordsByRdfFile(collection, rdfRessourcesFile);
    }
    if (mdRecords.size() == 0)
      return null;
    else 
      return mdRecords;
  }

  public ArrayList<MetadataRecord> getMetadataRecordsByRdfFile(Collection collection, File rdfRessourcesFile) throws ApplicationException {
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
      int maxIdcounter = IndexHandler.getInstance().findMaxId();  // find the highest value, so that each following id is a real new id
      if (xmdValueResources != null && xmdValueResources.size() > 0) {
        while (xmdValueResourcesIterator.hasNext()) {
          maxIdcounter++;
          XdmItem xdmItemResource = xmdValueResourcesIterator.next();
          String xdmItemResourceStr = xdmItemResource.toString();
          MetadataRecord mdRecord = getMdRecord(xQueryEvaluator, xdmItemResourceStr);  // get the mdRecord out of rdf string
          if (mdRecord != null) {
            mdRecord.setCollectionNames(collectionId);
            mdRecord.setId(maxIdcounter); // collections wide id
            mdRecord = createMainFieldsMetadataRecord(mdRecord, collection);
            // if it is a record: set lastModified to the date of the harvested dump file; if it is a normal web record: set it to now (harvested now)
            Date lastModified = new Date();
            if (mdRecord.isRecord()) {
              lastModified = xmlDumpFileLastModified;
            }
            mdRecord.setLastModified(lastModified);
            // if it is an eXist directory then fetch all subUrls/mdRecords of that directory
            if (mdRecord.isEXistDir()) {
              String eXistUrl = mdRecord.getWebUri();
              if (eXistUrl != null) {
                ArrayList<MetadataRecord> mdRecordsEXist = new ArrayList<MetadataRecord>();
                if (eXistUrl.endsWith("/"))
                  eXistUrl = eXistUrl.substring(0, eXistUrl.length() - 1);
                String excludesStr = collection.getExcludesStr();
                List<String> eXistUrls = extractDocumentUrls(eXistUrl, excludesStr);
                for (int i=0; i<eXistUrls.size(); i++) {
                  maxIdcounter++;
                  String eXistSubUrlStr = eXistUrls.get(i);
                  MetadataRecord mdRecordEXist = getNewMdRecord(eXistSubUrlStr); // with docId and webUri
                  mdRecordEXist.setCollectionNames(collectionId);
                  mdRecordEXist.setId(maxIdcounter); // collections wide id
                  mdRecordEXist = createMainFieldsMetadataRecord(mdRecordEXist, collection);
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
            } else if(mdRecord.isDirectory()) {
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
                  fileDirStr = Constants.getInstance().getExternalDocumentsDir() + "/" + fileDirUrlHost + fileDirUrlPath;
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
                  mdRecordDirEntry = createMainFieldsMetadataRecord(mdRecordDirEntry, collection);
                  mdRecordDirEntry.setLastModified(lastModified);
                  mdRecordDirEntry.setUri(uri);
                  mdRecordsDir.add(mdRecordDirEntry);
                }
                mdRecords.addAll(mdRecordsDir);
              }
            }
            // do not add the eXistDir or directory itself as a record (only the normal resource mdRecord) 
            if (! mdRecord.isEXistDir() && ! mdRecord.isDirectory()) {
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
  
  private MetadataRecord createMainFieldsMetadataRecord(MetadataRecord mdRecord, Collection collection) throws ApplicationException {
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
      Hashtable<String, XQuery> xQueries = collection.getxQueries();
      mdRecord.setxQueries(xQueries);
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecord;
  }

  private ArrayList<MetadataRecord> getMetadataRecordsEdoc(Collection collection) throws ApplicationException {
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    String metadataRedundantUrlPrefix = collection.getMetadataRedundantUrlPrefix();
    String metadataUrlPrefix = collection.getMetadataUrlPrefix();
    String[] metadataUrls = collection.getMetadataUrls();
    int maxIdcounter = IndexHandler.getInstance().findMaxId();
    for (int i=0; i<metadataUrls.length; i++) {
      maxIdcounter++;
      String metadataUrl = metadataUrls[i];
      MetadataRecord mdRecord = new MetadataRecord();
      String docId = null;
      String uri = null;
      String webUri = null;
      mdRecord.setId(maxIdcounter);
      File edocDir = new File(Constants.getInstance().getExternalDocumentsDir() + "/edoc");
      Date lastModified = new Date(edocDir.lastModified());
      mdRecord.setLastModified(lastModified);
      EdocIndexMetadataFetcherTool.fetchHtmlDirectly(metadataUrl, mdRecord);
      if (mdRecord.getLanguage() != null) {
        String isoLang = Language.getInstance().getISO639Code(mdRecord.getLanguage());
        mdRecord.setLanguage(isoLang);
      }
      String httpEdocUrl = mdRecord.getRealDocUrl();
      String uriEdoc = mdRecord.getUri();  // e.g.: http://edoc.bbaw.de/volltexte/2009/1070/
      if (httpEdocUrl != null) {             
        String docIdTmp = httpEdocUrl.replaceAll(metadataUrlPrefix, "");
        docId = "/edoc" + docIdTmp;
        String fileEdocUrl = "file:" + metadataRedundantUrlPrefix + docIdTmp;
        uri = fileEdocUrl;
        if (uriEdoc == null) {
          String edocId = docIdTmp.replaceAll("pdf/.*$", "");
          webUri = metadataUrlPrefix + edocId;
        } else { 
          webUri = uriEdoc;
        }
      } else {
        LOGGER.error("Fetching metadata failed for: " + metadataUrl + " (no url in index.html found)");
      }
      if (docId != null && uri != null) {
        mdRecord.setDocId(docId);
        mdRecord.setUri(uri);
        mdRecord.setWebUri(webUri);
        if (mdRecord.getCollectionNames() == null)
          mdRecord.setCollectionNames("edoc");
        if (mdRecord.getLanguage() == null) {
          String mainLanguage = collection.getMainLanguage();
          mdRecord.setLanguage(mainLanguage);
        }
        mdRecord.setSchemaName(null);
        mdRecords.add(mdRecord);
      }
    }
    return mdRecords;
  }

  private List<String> extractDocumentUrls(String collectionDataUrl, String excludesStr) {
    List<String> documentUrls = null;
    if (! collectionDataUrl.equals("")){
      PathExtractor extractor = new PathExtractor();
      documentUrls = extractor.initExtractor(collectionDataUrl, excludesStr);
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

  private MetadataRecord getMdRecord(XQueryEvaluator xQueryEvaluator, String xmlRdfStr) throws ApplicationException {
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
    String type = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:type/text()");
    if (type != null)
      mdRecord.setSystem(type);  // e.g. "eXistDir" or "dbRecord" or "directory" or "oai"
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
  
  /*
   * wird nicht mehr verwendet: nur noch als Demo
   */
  private void setUpdate(File configFile, boolean update) throws ApplicationException {
    try {
      // flag im Konfigurations-File auf false setzen durch Serialisierung in das File
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = docFactory.newDocumentBuilder();
      Document configFileDocument = builder.parse(configFile);
      NodeList updateNodeList = configFileDocument.getElementsByTagName("update");
      Node n = updateNodeList.item(0);
      if (n != null) {
        if (update)
          n.setTextContent("true");
        else 
          n.setTextContent("false");
        FileOutputStream os = new FileOutputStream(configFile);
        DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
        DOMImplementationLS impl = (DOMImplementationLS) reg.getDOMImplementation("LS");
        LSSerializer serializer = impl.createLSSerializer();
        LSOutput lso = impl.createLSOutput();
        lso.setByteStream(os);
        serializer.write(configFileDocument, lso); //  Vorsicht: wenn update auf true ist: es wird alles neu indexiert
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  public class FileExtensionsFilter implements FileFilter {
    private String[] fileExtensions;
    
    public FileExtensionsFilter(String[] fileExtensions) {
      this.fileExtensions = fileExtensions;
    }
    
    public boolean accept(File file) {
      for (String extension : fileExtensions) {
        if (file.getName().toLowerCase().endsWith(extension)) {
          return true;
        }
      }
      return false;
    }
  }

}
