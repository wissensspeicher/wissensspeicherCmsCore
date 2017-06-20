package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Indexer {
  private static Logger LOGGER = Logger.getLogger(Indexer.class);
  private static int COMMIT_INTERVAL_DB = 1000;
  private String[] options;
  private MetadataHandler metadataHandler;
  private Harvester harvester;
  private Annotator annotator;
  private IndexHandler indexHandler;
  
  public static Indexer getInstance() throws ApplicationException {
    Indexer  indexer = new Indexer();
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
  
  public void reopenIndexHandler() throws ApplicationException {
    indexHandler.end();
    indexHandler.init();
  }
  
  public void index(Project project) throws ApplicationException {
    int counter = 0;
    String projectId = project.getId();
    LOGGER.info("Index project: " + projectId + " ...");
    ArrayList<Database> projectDBs = project.getDatabases();
    if (options != null && options.length == 1 && options[0].equals("dbType:crawl")) {
      projectDBs = project.getDatabasesByType("crawl");
    } else if (options != null && options.length == 1 && options[0].startsWith("dbName:")) {
      String dbName = options[0].replaceAll("dbName:", "");
      projectDBs = project.getDatabasesByName(dbName);
    }
    if (options != null && options.length == 1 && (options[0].equals("dbType:crawl") || options[0].startsWith("dbName:"))) {
      for (int i=0; i<projectDBs.size(); i++) {
        Database db = projectDBs.get(i);
        delete(project, db); // delete the specified db from index
      }
    } else {
      delete(project); // delete the whole project from index
    }
    if (options != null && options.length == 1 && options[0].startsWith("dbName:")) {
      // if db is specified: do not index the project rdf records
    } else {
      ArrayList<MetadataRecord> mdRecords = annotator.getMetadataRecordsByRecordsFile(project);
      if (mdRecords != null) {
        counter = counter + mdRecords.size();
        index(project, null, mdRecords, false);
      }
    }
    if (projectDBs != null) {
      for (int i=0; i<projectDBs.size(); i++) {
        Database db = projectDBs.get(i);
        String dbType = db.getType();
        if (dbType != null && (dbType.equals("crawl") || dbType.equals("eXist") || dbType.equals("oai"))) {
          LOGGER.info("Index database (" + project.getId() + ", " + db.getRdfId() + ", " + db.getType() + ") ...");
          ArrayList<MetadataRecord> dbMdRecords = annotator.getMetadataRecordsByRecordsFile(project, db);
          if (dbMdRecords != null) {
            counter = counter + dbMdRecords.size();
            index(project, db, dbMdRecords, false);
          }
        } else {
          File[] rdfDbResourcesFiles = metadataHandler.getRdfDbResourcesFiles(project, db);
          if (rdfDbResourcesFiles != null && rdfDbResourcesFiles.length > 0) {
            // add the database records of each database of each database file
            LOGGER.info("Index database (" + project.getId() + ", " + db.getRdfId() + ", " + db.getType() + ") ...");
            for (int j = 0; j < rdfDbResourcesFiles.length; j++) {
              File rdfDbResourcesFile = rdfDbResourcesFiles[j];
              LOGGER.info("Index database records of: " + rdfDbResourcesFile + " ...");
              ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecordsByRdfFile(project, rdfDbResourcesFile, db, false);
              if (dbMdRecords != null) {
                counter = counter + dbMdRecords.size();
                index(project, db, dbMdRecords, true);
              }
            }
          }
        }
      }
    }
    LOGGER.info("Project: " + projectId + " with " + counter + " records indexed");
  }
  
  private void index(Project project, Database db, ArrayList<MetadataRecord> mdRecords, boolean dbRecords) throws ApplicationException {
    // performance gain for database records: a commit is only done if commitInterval (e.g. 1000) is reached 
    if (dbRecords)
      indexHandler.setCommitInterval(COMMIT_INTERVAL_DB);
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      String organizationRdfId = project.getOrganizationRdfId();
      if (organizationRdfId != null)
        mdRecord.setOrganizationRdfId(organizationRdfId);
      String projectRdfId = project.getRdfId();
      // edoc: mdRecord.projectRdfId could be another project
      String mdRecordProjectRdfId = mdRecord.getProjectRdfId();
      if (mdRecordProjectRdfId != null && ! mdRecordProjectRdfId.isEmpty() && ! mdRecordProjectRdfId.equals(projectRdfId))
        projectRdfId = mdRecordProjectRdfId;
      mdRecord.setProjectRdfId(projectRdfId);
      // set db rdf id of mdRecord
      if (db != null) {
        String collectionRdfId = db.getCollectionRdfId();
        // edoc: mdRecord.projectRdfId could be another project
        String mdRecordCollectionRdfId = mdRecord.getCollectionRdfId();
        if (mdRecordCollectionRdfId != null && ! mdRecordCollectionRdfId.isEmpty() && ! mdRecordCollectionRdfId.equals(collectionRdfId))
          collectionRdfId = mdRecordCollectionRdfId;
        ProjectCollection rootCollection = ProjectReader.getInstance().getRootCollection(collectionRdfId); // hack: resources are set to root collection
        String rootCollectionRdfId = rootCollection.getRdfId(); 
        mdRecord.setCollectionRdfId(rootCollectionRdfId);
        String databaseRdfId = db.getRdfId();
        mdRecord.setDatabaseRdfId(databaseRdfId);
      }
      // fetch fulltextFields
      String content = harvester.getFulltext("content", mdRecord, db);
      mdRecord.setContent(content); // is used in query highlighting function
      String tokensOrig = harvester.getFulltext("tokensOrig", mdRecord, db);
      mdRecord.setTokenOrig(tokensOrig);
      String tokensMorph = harvester.getFulltext("tokensMorph", mdRecord, db);
      mdRecord.setTokenMorph(tokensMorph);
      String docId = mdRecord.getDocId();
      String docUri = mdRecord.getUri();
      int count = i+1;
      LOGGER.info("Index database (" + project.getId() + ", " + db.getRdfId() + ", " + db.getType() + ") ...");
      String logCreationStr = count + ". " + "Index database (" + project.getId() + ", " + db.getRdfId() + ", " + db.getType() + ")" + ": resource: " + docUri + " (" + docId + ")";
      if (dbRecords)
        logCreationStr = count + ". " + "Index database (" + project.getId() + ", " + db.getRdfId() + ", " + db.getType() + ")" + ": record: " + docId;
      // if isDbRecord then log only after each commit interval 
      if (! dbRecords) {
        LOGGER.info(logCreationStr);
      } else {
        if (count % COMMIT_INTERVAL_DB == 0)
          LOGGER.info(logCreationStr);
      }
      indexHandler.indexDocument(mdRecord);
      // without that, there would be a memory leak (with many big documents in one project)
      // with that the main big fields (content etc.) could be garbaged
      mdRecord.setAllNull();
    }
    indexHandler.commit();  // so that the last pending mdRecords before commitInterval were also committed
  }
  
  public void delete(Project project) throws ApplicationException {
    String projectId = project.getId();
    int countDeletedDocs = indexHandler.deleteProject(projectId);
    LOGGER.info(countDeletedDocs + " records in project: \"" + projectId + "\" successfully deleted from index");
  }

  public void delete(Project project, Database db) throws ApplicationException {
    int countDeletedDocs = indexHandler.deleteProjectDBByRdfId(db.getRdfId());
    LOGGER.info(countDeletedDocs + " records in project database: \"" + project.getId() + ", " + db.getName() + "\" successfully deleted from index");
  }
}
