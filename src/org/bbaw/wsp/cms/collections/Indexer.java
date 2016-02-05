package org.bbaw.wsp.cms.collections;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.collections.CollectionManager;
import org.bbaw.wsp.cms.lucene.IndexHandler;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Indexer {
  private static Logger LOGGER = Logger.getLogger(Indexer.class);
  private static Indexer indexer;
  private CollectionManager collectionManager;
  private IndexHandler indexHandler;

  public static void main(String[] args) throws ApplicationException {
    try {
      LOGGER.info("Start of indexing project resources");
      Indexer indexer = Indexer.getInstance();
      if (args != null && args.length != 0) {
        String operation = args[0];
        String projectId1 = args[1];
        String projectId2 = null;
        if (args.length > 2)
          projectId2 = args[2];
        if (operation.equals("createProjectResources") && projectId1 != null && projectId2 == null)
          indexer.collectionManager.updateCollection(projectId1);
        else if (operation.equals("createProjectResources") && projectId1 != null && projectId2 != null)
          indexer.collectionManager.updateCollections(projectId1, projectId2);
        else if (operation.equals("deleteProjectResources") && projectId1 != null && projectId2 == null)
          indexer.collectionManager.deleteCollection(projectId1);
        else if (operation.equals("deleteProjectResources") && projectId1 != null && projectId2 != null)
          indexer.collectionManager.deleteCollections(projectId1, projectId2);
      } else {
        indexer.deleteProjectResources();
        indexer.createProjectResources();
      }
      indexer.end();
      LOGGER.info("End of indexing project resources");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static Indexer getInstance() throws ApplicationException {
    if(indexer == null) {
      indexer = new Indexer();
      indexer.init();
    }
    return indexer;
  }
  
  private void init() throws ApplicationException {
    collectionManager =  CollectionManager.getInstance();
    indexHandler = IndexHandler.getInstance();
  }
  
  private void end() throws ApplicationException {
    indexHandler.end();
  }

  private void deleteProjectResources() throws ApplicationException {
    /** please comment out, what you want to do **/
    /** delete all resources of project "aaew" **/
    // collectionManager.deleteCollection("aaew");
    /** delete all resources of all projects alphabetically named between "aaew" and dta inclusive **/
    // collectionManager.deleteCollections("aaew", "dta");
    /** delete all resources of all projects named in this projectIds array **/
    // String[] projectIds = {"aaew" , "aaewerbkam", "aaewpreussen", "aaewtla", "aaewvor", "aaewzettel"};
    // collectionManager.deleteCollections(projectIds);
  }
  
  private void createProjectResources() throws ApplicationException {
    /** please comment out, what you want to do **/
    /** create all resources of project "aaew" **/
    // collectionManager.updateCollection("aaew");
    /** create all resources of all projects alphabetically named between "aaew" and "dspin" inclusive **/
    // collectionManager.updateCollections("aaew", "dspin"); // 1 hour
    // collectionManager.updateCollections("jdgzkz", "zwk");  // 2,5 hours
    // collectionManager.updateCollections("dtm", "jdg");  // 4 hours
    // collectionManager.updateCollection("dta"); // 4,5 hours
    /** create all resources of all projects named in this projectIds array **/
    // String[] projectIds = {"aaew" , "aaewerbkam", "aaewpreussen", "aaewtla", "aaewvor", "aaewzettel"};
    // collectionManager.updateCollections(projectIds);
  }
  
}