package org.bbaw.wsp.cms.collections;

import java.util.Hashtable;

import org.bbaw.wsp.cms.document.XQuery;

public class Database {
  private String type;
  private String name;
  private String url;  // eXist URL
  private String language;  // language of db content
  private JdbcConnection jdbcConnection;
  private String sql;
  private String xmlDumpFileName;
  private String xmlDumpUrl;
  private String xmlDumpSet;
  private String mainResourcesTable;
  private String mainResourcesTableId;
  private String webIdPreStr;
  private String webIdAfterStr;
  private String excludes; // excludes urls separated by a blank e.g. for eXist directories
  private Hashtable<String, XQuery> xQueries;
  private Hashtable<String, String> dcField2dbField = new Hashtable<String, String>();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public JdbcConnection getJdbcConnection() {
    return jdbcConnection;
  }

  public void setJdbcConnection(JdbcConnection jdbcConnection) {
    this.jdbcConnection = jdbcConnection;
  }

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public String getXmlDumpFileName() {
    return xmlDumpFileName;
  }

  public void setXmlDumpFileName(String xmlDumpFileName) {
    this.xmlDumpFileName = xmlDumpFileName;
  }

  public String getXmlDumpUrl() {
    return xmlDumpUrl;
  }

  public void setXmlDumpUrl(String xmlDumpUrl) {
    this.xmlDumpUrl = xmlDumpUrl;
  }

  public String getXmlDumpSet() {
    return xmlDumpSet;
  }

  public void setXmlDumpSet(String xmlDumpSet) {
    this.xmlDumpSet = xmlDumpSet;
  }

  public String getMainResourcesTable() {
    return mainResourcesTable;
  }
  
  public void setMainResourcesTable(String mainResourcesTable) {
    this.mainResourcesTable = mainResourcesTable;
  }

  public String getMainResourcesTableId() {
    return mainResourcesTableId;
  }

  public void setMainResourcesTableId(String mainResourcesTableId) {
    this.mainResourcesTableId = mainResourcesTableId;
  }

  public String getWebIdPreStr() {
    return webIdPreStr;
  }

  public void setWebIdPreStr(String webIdPreStr) {
    this.webIdPreStr = webIdPreStr;
  }

  public String getWebIdAfterStr() {
    return webIdAfterStr;
  }

  public void setWebIdAfterStr(String webIdAfterStr) {
    this.webIdAfterStr = webIdAfterStr;
  }

  public String getExcludes() {
    return excludes;
  }

  public void setExcludes(String excludes) {
    this.excludes = excludes;
  }

  public Hashtable<String, XQuery> getxQueries() {
    return xQueries;
  }

  public void setxQueries(Hashtable<String, XQuery> xQueries) {
    this.xQueries = xQueries;
  }

  public void addField(String dcField, String dbField) {
    dcField2dbField.put(dcField, dbField);
  }
  
  public String getDbField(String dcField) {
    return dcField2dbField.get(dcField);
  }
}
