package org.bbaw.wsp.cms.collections;

import java.util.Date;
import java.util.HashMap;

public class Project {
  private String id;  // nick name
  private String rdfId;  // rdfId of this project
  private String parentRdfId;  // rdf id of which this project is part of 
  private String type; // project or organization
  private String title;  // title of project
  private Date lastModified; // last modified
  private String status; // e.g. "aktiv" or "abgeschlossen" 
  private String updateCycle; // e.g. "monthly" or "halfyearly" 
  private String homepageUrl; // homepage url
  private String mainLanguage;
  private HashMap<String, ProjectCollection> collections = new HashMap<String, ProjectCollection>();  // project collections: key is projectRdfId and value is collection

  public ProjectCollection getCollection(String collRdfId) {
    return collections.get(collRdfId);
  }
  public void addCollection(String collRdfId, ProjectCollection collection) {
    collections.put(collRdfId, collection);
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
