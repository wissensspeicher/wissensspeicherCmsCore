package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class ProjectCollection {
  private static Logger LOGGER = Logger.getLogger(ProjectReader.class);
  private String id;
  private String rdfId;
  private String projectRdfId; // rdfId of project
  private String parentRdfId; // rdfId of parent collection
  private String rdfPath; // path from this collection to root (project or organization)
  private HashMap<String, Database> databases = new HashMap<String, Database>();  // // collection databases: key is databaseRdfId and value is database
  
  public ProjectCollection(String projectRdfId) {
    this.projectRdfId = projectRdfId;
  }
  
  public Database getDatabase(String databaseRdfId) {
    return databases.get(databaseRdfId);
  }
  
  public void addDatabase(String databaseRdfId, Database database) {
    databases.put(databaseRdfId, database);
  }
  
  public void buildRdfPath() throws ApplicationException {
    this.rdfPath = calcRdfPath();
  }
  
  private String calcRdfPath() throws ApplicationException {
    Project project = ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
    if (parentRdfId == null)
      return project.getRdfPath() + "###" + rdfId;
    ProjectCollection parentCollection = project.getCollection(parentRdfId);
    if (parentCollection == null) {
      Project parentProject = ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
      if (parentProject == null) {
        LOGGER.error("Collection: " + rdfId + " calcRdfPath: parent rdfId: " + parentRdfId + " not found");
        return "null";
      }
      return parentProject.getRdfPath() + "###" + rdfId;
    }
    String collectionRdfPath = parentCollection.calcRdfPath() + "###" + rdfId;
    return collectionRdfPath;
  }
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  public String getRdfId() {
    return rdfId;
  }

  public void setRdfId(String rdfId) {
    this.rdfId = rdfId;
  }

  public String getProjectRdfId() {
    return projectRdfId;
  }

  public void setProjectRdfId(String projectRdfId) {
    this.projectRdfId = projectRdfId;
  }

  public String getParentRdfId() {
    return parentRdfId;
  }
  
  public void setParentRdfId(String parentRdfId) {
    this.parentRdfId = parentRdfId;
  }
  
  public String getRdfPath() {
    return rdfPath;
  }

  public ArrayList<Database> getDatabasesByType(String dbType) {
    ArrayList<Database> retDBs = new ArrayList<Database>();
    if (databases != null) {
      java.util.Collection<Database> databaseValues = this.databases.values();
      for (Database db : databaseValues) {
        String type = db.getType();
        if (type != null && type.equals(dbType))
          retDBs.add(db);
      }
    }
    return retDBs;
  }
  
  public ArrayList<Database> getDatabasesByName(String dbName) {
    ArrayList<Database> retDBs = new ArrayList<Database>();
    if (databases != null) {
      java.util.Collection<Database> databaseValues = this.databases.values();
      for (Database db : databaseValues) {
        String name = db.getName();
        if (name != null && name.equals(dbName))
          retDBs.add(db);
      }
    }
    return retDBs;
  }
  
}
