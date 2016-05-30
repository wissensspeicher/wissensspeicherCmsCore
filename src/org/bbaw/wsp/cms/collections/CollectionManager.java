package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.dochandler.DocumentHandler;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

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
  private MetadataHandler metadataHandler;

  public static CollectionManager getInstance() throws ApplicationException {
    if(confManager == null) {
      confManager = new CollectionManager();
      confManager.init();
    }
    return confManager;
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    metadataHandler = MetadataHandler.getInstance();
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
    try {
      CmsDocOperation docOp = new CmsDocOperation("deleteCollection", null, null, null);
      docOp.setCollectionId(collectionId);
      docHandler.doOperation(docOp);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void addDocumentsOfDatabase(String collectionId, String databaseName) throws ApplicationException {
    Collection collection = collectionReader.getCollection(collectionId);
    initDBpediaSpotlightWriter(collection);
    ArrayList<Database> collectionDBs = collection.getDatabases();
    // add the database records 
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database db = collectionDBs.get(i);
        String dbName = db.getName(); 
        if (dbName.equals(databaseName)) {
          LOGGER.info("Create database (Collection: " + collectionId + ", Database: " + db.getName() + ", Type: " + db.getType() + ") ...");
          int countDocs = addDocuments(collection, db);
          flushDBpediaSpotlightWriter(collection);
          LOGGER.info("Database: " + db.getName() + " with: " + countDocs + " records created");
        }
      }
    }
  }

  private void addDocuments(Collection collection, boolean withDatabases) throws ApplicationException {
    int counter = 0;
    String collId = collection.getId();
    LOGGER.info("Create collection: " + collId + " ...");
    initDBpediaSpotlightWriter(collection);
    // first add the manually maintained mdRecords
    ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecords(collection, true);
    if (mdRecords != null) {
      int countDocs = addDocuments(collection, mdRecords, false);
      counter = counter + countDocs;
    }
    ArrayList<Database> collectionDBs = collection.getDatabases();
    // add the database records 
    if (collectionDBs != null && withDatabases) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database collectionDB = collectionDBs.get(i);
        LOGGER.info("Create database (" + collectionDB.getName() + ", " + collectionDB.getType() + ") ...");
        int countDocs = addDocuments(collection, collectionDB);
        counter = counter + countDocs;
      }
    }
    flushDBpediaSpotlightWriter(collection);
    LOGGER.info("Collection: " + collId + " with: " + counter + " records created");
  }

  private int addDocuments(Collection collection, Database db) throws ApplicationException {
    int counter = 0;
    String dbType = db.getType();
    if (dbType.equals("eXist") || dbType.equals("crawl")) {
      ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecords(collection, db, true);
      int countDocs = addDocuments(collection, dbMdRecords, false);
      counter = counter + countDocs;
    } else {
      File[] rdfDbResourcesFiles = metadataHandler.getRdfDbResourcesFiles(collection, db);
      if (rdfDbResourcesFiles != null && rdfDbResourcesFiles.length > 0) {
        // add the database records of each database of each database file
        for (int i = 0; i < rdfDbResourcesFiles.length; i++) {
          File rdfDbResourcesFile = rdfDbResourcesFiles[i];
          LOGGER.info("Create database resources from file: " + rdfDbResourcesFile);
          if (dbType.equals("oai")) {
            ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecordsByRdfFile(collection, rdfDbResourcesFile, db, true);
            int countDocs = addDocuments(collection, dbMdRecords, false);
            counter = counter + countDocs;
          } else {
            ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecordsByRdfFile(collection, rdfDbResourcesFile, db, false);
            int countDocs = addDocuments(collection, dbMdRecords, true);
            counter = counter + countDocs;
          }
        }
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
      String dbPediaSpotlightAnnotationsDirName = Constants.getInstance().getAnnotationsDir();
      String spotlightAnnotationRdfStr = dbPediaSpotlightCollectionRdfStrBuilder.toString();
      try {
        FileUtils.writeStringToFile(new File(dbPediaSpotlightAnnotationsDirName + "/" + collId + ".rdf"), spotlightAnnotationRdfStr, "utf-8");
      } catch (IOException e) {
        throw new ApplicationException(e);
      }
      collection.setDbPediaSpotlightRdfStrBuilder(null);
    }
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
  
}
