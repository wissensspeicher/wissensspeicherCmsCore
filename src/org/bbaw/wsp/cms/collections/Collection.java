package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.bbaw.wsp.cms.document.XQuery;

public class Collection {
  private String id;
  private String rdfId;
  private ArrayList<Database> databases;  // when collection is a database
  private String name;
  private String webBaseUrl;  // web base url 
  private WspUrl[] dataUrls;  // urls of data (could also be starting url with type "eXist")
  private String[] metadataUrls;  // metadata urls for fetching records
  private String metadataUrlPrefix;  // prefix of metadataUrl which is not relevant as id
  private String metadataRedundantUrlPrefix;  // prefix of metadataUrl which is not relevant as id 
  private String excludesStr; // excludes below dataUrl separated by a blank
  private String mainLanguage;
  private ArrayList<String> fields;
  private Hashtable<String, Service> services;
  private Hashtable<String, XQuery> xQueries;
  private List<String> excludeField;
  private String configFileName;  // configuration file name
  
  public Collection() {
    id = "";
    name = "";
    dataUrls = null;
    excludesStr = "";
    mainLanguage = "";
    fields = new ArrayList<String>();
    excludeField = new ArrayList<String>();
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

  public WspUrl[] getDataUrls() {
    return dataUrls;
  }

  public void setDataUrls(WspUrl[] dataUrls) {
    this.dataUrls = dataUrls;
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

  public String getExcludesStr() {
    return excludesStr;
  }

  public void setExcludesStr(String excludesStr) {
    this.excludesStr = excludesStr;
  }

  public List<String> getExcludeField() {
    return excludeField;
  }

  public void setExcludeField(List<String> excludeField) {
    this.excludeField = excludeField;
  }
  
  public String getMainLanguage() {
    return mainLanguage;
  }

  public void setMainLanguage(String mainLanguage) {
    this.mainLanguage = mainLanguage;
  }

  public ArrayList<String> getFields() {
    return fields;
  }

  public void setFields(ArrayList<String> fields) {
    this.fields = fields;
  }

  public Hashtable<String, XQuery> getxQueries() {
    return xQueries;
  }

  public void setxQueries(Hashtable<String, XQuery> xQueries) {
    this.xQueries = xQueries;
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

}
