package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class LocalIndexer {
  private static Logger LOGGER = Logger.getLogger(LocalIndexer.class);
  private static LocalIndexer indexer;
  private static int COMMIT_INTERVAL_DB = 1000;
  private CollectionReader collectionReader;
  private MetadataHandler metadataHandler;
  private IndexHandler indexHandler;
  private Annotator annotator;
  
  public static LocalIndexer getInstance() throws ApplicationException {
    if(indexer == null) {
      indexer = new LocalIndexer();
      indexer.init();
    }
    return indexer;
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    metadataHandler = MetadataHandler.getInstance();
    indexHandler = IndexHandler.getInstance();
    annotator = Annotator.getInstance();
  }
  
  public void indexCollections() throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      index(collection);
    }
  }

  public void index(Collection collection) throws ApplicationException {
    int counter = 0;
    String collId = collection.getId();
    LOGGER.info("Index project: " + collId + " ...");
    ArrayList<MetadataRecord> mdRecords = annotator.getMetadataRecordsByRecordsFile(collection);
    counter = counter + mdRecords.size();
    index(collection, null, mdRecords, false); 
    ArrayList<Database> collectionDBs = collection.getDatabases();
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database db = collectionDBs.get(i);
        String dbType = db.getType();
        if (dbType != null && (dbType.equals("crawl") || dbType.equals("eXist") || dbType.equals("oai"))) {
          ArrayList<MetadataRecord> dbMdRecords = annotator.getMetadataRecordsByRecordsFile(collection, db);
          counter = counter + dbMdRecords.size();
          index(collection, db, dbMdRecords, false);
        } else {
          File[] rdfDbResourcesFiles = metadataHandler.getRdfDbResourcesFiles(collection, db);
          if (rdfDbResourcesFiles != null && rdfDbResourcesFiles.length > 0) {
            // add the database records of each database of each database file
            for (int j = 0; j < rdfDbResourcesFiles.length; j++) {
              File rdfDbResourcesFile = rdfDbResourcesFiles[j];
              LOGGER.info("Index database records from file: " + rdfDbResourcesFile);
              ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecordsByRdfFile(collection, rdfDbResourcesFile, db);
              counter = counter + dbMdRecords.size();
              index(collection, db, dbMdRecords, true);
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
}
