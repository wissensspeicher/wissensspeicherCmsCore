package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class LocalIndexer {
  private static Logger LOGGER = Logger.getLogger(LocalIndexer.class);
  private static LocalIndexer indexer;
  private CollectionReader collectionReader;
  private Harvester harvester;
  
  public static LocalIndexer getInstance() throws ApplicationException {
    if(indexer == null) {
      indexer = new LocalIndexer();
      indexer.init();
    }
    return indexer;
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    harvester = Harvester.getInstance();
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
    LOGGER.info("Index collection: " + collId + " ...");
    ArrayList<MetadataRecord> mdRecords = harvester.getMetadataRecordsByRecordsFile(collection);
    counter = counter + mdRecords.size();
    index(collection, null, mdRecords); 
    ArrayList<Database> collectionDBs = collection.getDatabases();
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database collectionDB = collectionDBs.get(i);
        String dbType = collectionDB.getType();
        if (dbType != null && (dbType.equals("crawl") || dbType.equals("eXist") || dbType.equals("oai"))) {
          ArrayList<MetadataRecord> dbMdRecords = harvester.getMetadataRecordsByRecordsFile(collection, collectionDB);
          counter = counter + dbMdRecords.size();
          index(collection, collectionDB, dbMdRecords);
        } else {
          // TODO
        }
      }
    }
    LOGGER.info("Collection: " + collId + " with " + counter + " records indexed");
  }
  
  private void index(Collection collection, Database db, ArrayList<MetadataRecord> mdRecords) throws ApplicationException {
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      index(collection, db, mdRecord);
    }
  }
  
  private void index(Collection collection, Database db, MetadataRecord mdRecord) throws ApplicationException {
    // TODO
  }

  public void delete(Collection collection) throws ApplicationException {
    // TODO
  }
}
