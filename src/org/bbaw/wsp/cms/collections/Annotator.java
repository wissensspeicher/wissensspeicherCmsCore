package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.general.Constants;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Annotator {
  private static Logger LOGGER = Logger.getLogger(Annotator.class);
  private static Annotator annotator;
  private CollectionReader collectionReader;
  private MetadataHandler metadataHandler;
  private String harvestDir;
  private String annotationDir;
  private StringBuilder harvestXmlStrBuilder;
  
  public static Annotator getInstance() throws ApplicationException {
    if(annotator == null) {
      annotator = new Annotator();
      annotator.init();
    }
    return annotator;
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    metadataHandler = MetadataHandler.getInstance();
    harvestDir = Constants.getInstance().getHarvestDir();
    annotationDir = Constants.getInstance().getAnnotationsDir();
  }
  
  private void end() throws ApplicationException {

  }

  public void annotateCollections() throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      annotateCollection(collection);
    }
  }

  public void annotateCollection(Collection collection) throws ApplicationException {
    int counter = 0;
    String collId = collection.getId();
    LOGGER.info("Annotate collection: " + collId + " ...");
    String recordsFileName = harvestDir + "/" + collection.getId() + "/metadata/records/" + collection.getId() + ".xml";
    File recordsFile = new File(recordsFileName);
    ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecordsByRecordsFile(recordsFile);
    annotate(collection, null, mdRecords); 
    ArrayList<Database> collectionDBs = collection.getDatabases();
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database collectionDB = collectionDBs.get(i);
        String dbType = collectionDB.getType();
        if (dbType != null && (dbType.equals("crawl") || dbType.equals("eXist") || dbType.equals("oai"))) {
          String dbRecordsFileName = harvestDir + "/" + collection.getId() + "/metadata/records/" + collection.getId() + "-" + collectionDB.getName() + "-1.xml";
          File dbRecordsFile = new File(dbRecordsFileName);
          ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecordsByRecordsFile(dbRecordsFile);
          annotate(collection, collectionDB, dbMdRecords);
        }
      }
    }
    LOGGER.info("Collection: " + collId + " with " + counter + " records annotated");
  }
  
  private void annotate(Collection collection, Database db, ArrayList<MetadataRecord> mdRecords) throws ApplicationException {
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      // annotate(mdRecord);  // TODO
    }
  }
}
