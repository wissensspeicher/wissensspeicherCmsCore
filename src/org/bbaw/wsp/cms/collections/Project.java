package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Project {
  private String id;  // nick name
  private String rdfId;  // rdfId of this project
  private String parentRdfId;  // rdf id of which this project is part of 
  private String rdfPath; // path from this project to root (project or organization)
  private String type; // project or organization
  private String title;  // title of project
  private String homepageUrl; // homepage url
  private String temporal; // temporal, e.g. "http://wissensspeicher.bbaw.de/rdf/normdata/Neuzeit"
  private Date lastModified; // last modified
  private String status; // e.g. "aktiv" or "abgeschlossen" 
  private String updateCycle; // e.g. "monthly" or "halfyearly" 
  private String mainLanguage;
  private HashMap<String, ProjectCollection> collections = new HashMap<String, ProjectCollection>();  // project collections: key is collectionRdfId and value is collection
  private HashMap<String, Database> databases = new HashMap<String, Database>();  // project databases (redundant to collection databases): key is databaseRdfId and value is database

  public ProjectCollection getCollection(String collRdfId) {
    return collections.get(collRdfId);
  }
  
  public void addCollection(String collRdfId, ProjectCollection collection) {
    collections.put(collRdfId, collection);
  }
  
  public void addDatabase(String databaseRdfId, Database database) {
    databases.put(databaseRdfId, database);
  }
  
  public ArrayList<Database> getDatabases() {
    ArrayList<Database> retDBs = new ArrayList<Database>();
    if (databases != null) {
      java.util.Collection<Database> databaseValues = this.databases.values();
      for (Database db : databaseValues) {
        retDBs.add(db);
      }
    }
    return retDBs;
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
  
  public void buildRdfPathes() throws ApplicationException {
    this.rdfPath = calcRdfPath();
    java.util.Collection<ProjectCollection> collectionValues = this.collections.values();
    for (ProjectCollection collection : collectionValues) {
      collection.buildRdfPath();
    }
  }
  
  private String calcRdfPath() throws ApplicationException {
    if (parentRdfId == null)
      return rdfId;
    Project parentProject = ProjectReader.getInstance().getProjectByRdfId(parentRdfId);
    if (parentProject == null)
      return rdfId;
    String projectRdfPath = parentProject.calcRdfPath() + "###" + rdfId;
    return projectRdfPath;
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
  public String getParentRdfId() {
    return parentRdfId;
  }
  public void setParentRdfId(String parentRdfId) {
    this.parentRdfId = parentRdfId;
  }
  public String getRdfPath() {
    return rdfPath;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getTemporal() {
    return temporal;
  }
  public void setTemporal(String temporal) {
    this.temporal = temporal;
  }
  public Date getLastModified() {
    return lastModified;
  }
  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }
  public String getHomepageUrl() {
    return homepageUrl;
  }
  public void setHomepageUrl(String homepageUrl) {
    this.homepageUrl = homepageUrl;
  }
  public String getUpdateCycle() {
    return updateCycle;
  }
  public void setUpdateCycle(String updateCycle) {
    this.updateCycle = updateCycle;
  }
  public String getMainLanguage() {
    return mainLanguage;
  }
  public void setMainLanguage(String mainLanguage) {
    this.mainLanguage = mainLanguage;
  }
}
