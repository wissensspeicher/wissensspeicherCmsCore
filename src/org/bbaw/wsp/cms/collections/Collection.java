package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

public class Collection {
  private String id;
  private String rdfId;
  private ArrayList<Database> databases;  // when collection is a database
  private String name;
  private String webBaseUrl;  // web base url 
  private String[] metadataUrls;  // metadata urls for fetching records
  private String metadataUrlPrefix;  // prefix of metadataUrl which is not relevant as id
  private String metadataRedundantUrlPrefix;  // prefix of metadataUrl which is not relevant as id 
  private String mainLanguage;
  private String coverage;  // e.g.: http://wsp.normdata.rdf/Gegenwart or http://wsp.normdata.rdf/Neuzeit
  private String status; // e.g. "aktiv" or "abgeschlossen" 
  private String updateCycle; // e.g. "monthly" or "halfyearly" 
  private Hashtable<String, Service> services;
  private String configFileName;  // configuration file name
  private Date lastModified;
  private StringBuilder dbPediaSpotlightRdfStrBuilder;
  
  public Collection() {
    id = "";
    name = "";
    mainLanguage = "";
    configFileName = "";
  }
  
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public String getRdfId() {
    return rdfId;
  }

  public void setRdfId(String rdfId) {
    this.rdfId = rdfId;
  }

  public ArrayList<Database> getDatabases() {
    return databases;
  }

  public void setDatabases(ArrayList<Database> databases) {
    this.databases = databases;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMetadataRedundantUrlPrefix() {
    return metadataRedundantUrlPrefix;
  }

  public void setMetadataRedundantUrlPrefix(String metadataRedundantUrlPrefix) {
    this.metadataRedundantUrlPrefix = metadataRedundantUrlPrefix;
  }

  public String[] getMetadataUrls() {
    return metadataUrls;
  }

  public void setMetadataUrls(String[] metadataUrls) {
    this.metadataUrls = metadataUrls;
  }

  public String getMetadataUrlPrefix() {
    return metadataUrlPrefix;
  }

  public void setMetadataUrlPrefix(String metadataUrlPrefix) {
    this.metadataUrlPrefix = metadataUrlPrefix;
  }

  public String getMainLanguage() {
    return mainLanguage;
  }

  public void setMainLanguage(String mainLanguage) {
    this.mainLanguage = mainLanguage;
  }

  public String getCoverage() {
    return coverage;
  }

  public void setCoverage(String coverage) {
    this.coverage = coverage;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getUpdateCycle() {
    return updateCycle;
  }

  public void setUpdateCycle(String updateCycle) {
    this.updateCycle = updateCycle;
  }

  public String getWebBaseUrl() {
    return webBaseUrl;
  }

  public void setWebBaseUrl(String webBaseUrl) {
    this.webBaseUrl = webBaseUrl;
  }

  public String getConfigFileName() {
    return configFileName;
  }

  public void setConfigFileName(String configFileName) {
    this.configFileName = configFileName;
  }

  public Hashtable<String, Service> getServices() {
    return services;
  }

  public void setServices(Hashtable<String, Service> services) {
    this.services = services;
  }

  public Service getService(String serviceName) {
    Service service = null;
    if (services != null) {
      service = services.get(serviceName);
    }
    return service;
  }

  public StringBuilder getDbPediaSpotlightRdfStrBuilder() {
    return dbPediaSpotlightRdfStrBuilder;
  }

  public void setDbPediaSpotlightRdfStrBuilder(StringBuilder dbPediaSpotlightRdfStrBuilder) {
    this.dbPediaSpotlightRdfStrBuilder = dbPediaSpotlightRdfStrBuilder;
  }

}
