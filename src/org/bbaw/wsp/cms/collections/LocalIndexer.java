package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class LocalIndexer {
  private static Logger LOGGER = Logger.getLogger(LocalIndexer.class);
  private static int COMMIT_INTERVAL_DB = 1000;
  private String[] options;
  private MetadataHandler metadataHandler;
  private Harvester harvester;
  private Annotator annotator;
  private IndexHandler indexHandler;
  
  public static LocalIndexer getInstance() throws ApplicationException {
    LocalIndexer  indexer = new LocalIndexer();
    indexer.init();
    return indexer;
  }
  
  private void init() throws ApplicationException {
    metadataHandler = MetadataHandler.getInstance();
    harvester = Harvester.getInstance();
    annotator = Annotator.getInstance();
    indexHandler = IndexHandler.getInstance();
  }
  
  public void setOptions(String[] options) {
    this.options = options;
  }
  
  private boolean dbMatchesOptions(String dbType) {
    boolean dbMatchesOptions = true;
    if (options != null && dbType != null && options.length == 1 && options[0].equals("dbType:crawl") && ! dbType.equals("crawl"))
      dbMatchesOptions = false;
    return dbMatchesOptions;
  }
  
  public void reopenIndexHandler() throws ApplicationException {
    indexHandler.end();
    indexHandler.init();
  }
  
  public void index(Collection collection) throws ApplicationException {
    int counter = 0;
    String collId = collection.getId();
    LOGGER.info("Index project: " + collId + " ...");
    if (options != null && options.length == 1 && options[0].equals("dbType:crawl")) {
      ArrayList<Database> crawlDBs = collection.getCrawlDatabases();
      for (int i=0; i<crawlDBs.size(); i++) {
        Database crawlDB = crawlDBs.get(i);
        delete(collection, crawlDB); // delete the crawl db from index
      }
    } else {
      delete(collection); // delete the whole collection from index
    }
    ArrayList<MetadataRecord> mdRecords = annotator.getMetadataRecordsByRecordsFile(collection);
    if (mdRecords != null) {
      counter = counter + mdRecords.size();
      index(collection, null, mdRecords, false);
    }
    ArrayList<Database> collectionDBs = collection.getDatabases();
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database db = collectionDBs.get(i);
        String dbType = db.getType();
        boolean indexDB = true;
        if (! dbMatchesOptions(dbType))
          indexDB = false; // nothing when dbType != "crawl"
        if (indexDB) {
          if (dbType != null && (dbType.equals("crawl") || dbType.equals("eXist") || dbType.equals("oai"))) {
            ArrayList<MetadataRecord> dbMdRecords = annotator.getMetadataRecordsByRecordsFile(collection, db);
            if (dbMdRecords != null) {
              counter = counter + dbMdRecords.size();
              index(collection, db, dbMdRecords, false);
            }
          } else {
            File[] rdfDbResourcesFiles = metadataHandler.getRdfDbResourcesFiles(collection, db);
            if (rdfDbResourcesFiles != null && rdfDbResourcesFiles.length > 0) {
              // add the database records of each database of each database file
              for (int j = 0; j < rdfDbResourcesFiles.length; j++) {
                File rdfDbResourcesFile = rdfDbResourcesFiles[j];
                LOGGER.info("Index database records from file: " + rdfDbResourcesFile);
                ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecordsByRdfFile(collection, rdfDbResourcesFile, db, false);
                if (dbMdRecords != null) {
                  counter = counter + dbMdRecords.size();
                  index(collection, db, dbMdRecords, true);
                }
              }
            }
          }
        }
      }
    }
    LOGGER.info("Project: " + collId + " with " + counter + " records indexed");
  }
  
  private void index(Collection collection, Database db, ArrayList<MetadataRecord> mdRecords, boolean dbRecords) throws ApplicationException {
    String collId = collection.getId();
    // performance gain for database records: a commit is only done if commitInterval (e.g. 1000) is reached 
    if (dbRecords)
      indexHandler.setCommitInterval(COMMIT_INTERVAL_DB);
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      // fetch fulltextFields
      String content = harvester.getFulltext("content", mdRecord);
      mdRecord.setContent(content); // is used in query highlighting function
      String tokensOrig = harvester.getFulltext("tokensOrig", mdRecord);
      mdRecord.setTokenOrig(tokensOrig);
      String tokensMorph = harvester.getFulltext("tokensMorph", mdRecord);
      mdRecord.setTokenMorph(tokensMorph);
      String docId = mdRecord.getDocId();
      String docUri = mdRecord.getUri();
      int count = i+1;
      String logCreationStr = count + ". " + "Project: " + collId + ": Index resource: " + docUri + " (" + docId + ")";
      if (dbRecords)
        logCreationStr = count + ". " + "Project: " + collId + ": Index database record: " + docId;
      // if isDbColl then log only after each commit interval 
      if (! dbRecords) {
        LOGGER.info(logCreationStr);
      } else {
        if (count % COMMIT_INTERVAL_DB == 0)
          LOGGER.info(logCreationStr);
      }
      indexHandler.indexDocument(mdRecord);
      // without that, there would be a memory leak (with many big documents in one collection)
      // with that the main big fields (content etc.) could be garbaged
      mdRecord.setAllNull();
    }
    indexHandler.commit();  // so that the last pending mdRecords before commitInterval were also committed
  }
  
  public void delete(Collection collection) throws ApplicationException {
    String collectionId = collection.getId();
    int countDeletedDocs = indexHandler.deleteCollection(collectionId);
    LOGGER.info(countDeletedDocs + " records in project: \"" + collectionId + "\" successfully deleted from index");
  }

  public void delete(Collection collection, Database db) throws ApplicationException {
    int countDeletedDocs = indexHandler.deleteCollectionDBByType(collection.getName(), db.getType());
    LOGGER.info(countDeletedDocs + " records in project database: \"" + collection.getId() + ", " + db.getName() + "\" successfully deleted from index");
  }
}
