package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.HashMap;

public class ProjectCollection {
  private String id;
  private String rdfId;
  private String projectRdfId; // rdfId of parent project
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

  public ArrayList<Database> getDatabasesByType(String dbType) {
    ArrayList<Database> retDBs = new ArrayList<Database>();
    if (databases != null) {
      for (int i=0; i<databases.size(); i++) {
        Database db = databases.get(i);
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
      for (int i=0; i<databases.size(); i++) {
        Database db = databases.get(i);
        String name = db.getName();
        if (name != null && name.equals(dbName))
          retDBs.add(db);
      }
    }
    return retDBs;
  }
  
}
