package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.Hashtable;

import org.bbaw.wsp.cms.document.XQuery;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Database {
  private String rdfId;
  private String type;
  private String name; // TODO remove this field
  private String url;  // start url (crawl, eXist or oai)
  private String language;  // language of db content
  private JdbcConnection jdbcConnection;
  private String sql;
  private String oaiSet; // if type is oai: a set could be defined 
  private String mainResourcesTable;
  private String mainResourcesTableId;
  private String webIdPreStr;
  private String webIdAfterStr;
  private ArrayList<String> excludes; // exclude urls e.g. for eXist directories or crawls
  private int depth;
  private Hashtable<String, XQuery> xQueries;
  private Hashtable<String, String> dcField2dbField = new Hashtable<String, String>();

  public String getFileNameId() {
    if (rdfId == null)
      return null;
    // e.g. "http://telota.bbaw.de:8085/exist/rest/db/AvHBriefedition" is converted to "telota.bbaw.de:8085##exist##rest##db##AvHBriefedition"
    String fileNameId = rdfId.replaceFirst("https*://", "");
    fileNameId = fileNameId.replaceAll("/", "##");
    return fileNameId;
  }
  
  public String getRdfId() {
    return rdfId;
  }

  public void setRdfId(String rdfId) {
    this.rdfId = rdfId;
  }

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

  public String getOaiSet() {
    return oaiSet;
  }

  public void setOaiSet(String oaiSet) {
    this.oaiSet = oaiSet;
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

  public ArrayList<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(ArrayList<String> excludes) {
    this.excludes = excludes;
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
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
  
  public static Database fromDatabaseElement(Element databaseElem) {
    Database database = new Database();
    String databaseRdfId = databaseElem.select("db").attr("id");
    database.setRdfId(databaseRdfId);
    String type = databaseElem.select("db > type").text();  // e.g. "crawl", "eXist", "oai", "mysql" or "postgres"
    database.setType(type);
    String url = databaseElem.select("db > url").text();
    database.setUrl(url);
    String language = databaseElem.select("db > language").text();
    if (language != null && ! language.isEmpty())
      database.setLanguage(language);
    String depthStr = databaseElem.select("db > depth").text();
    if (depthStr != null && ! depthStr.isEmpty()) {
      Integer depth = Integer.parseInt(depthStr);
      database.setDepth(depth);
    }
    String jdbcHost = databaseElem.select("db > jdbc > host").text();
    if (jdbcHost != null && ! jdbcHost.isEmpty()) {
      JdbcConnection jdbcConn = new JdbcConnection();
      jdbcConn.setType(type);
      jdbcConn.setHost(jdbcHost);
      String jdbcPort = databaseElem.select("db > jdbc > port").text();
      jdbcConn.setPort(jdbcPort);
      String jdbcDb = databaseElem.select("db > jdbc > db").text();
      jdbcConn.setDb(jdbcDb);
      String jdbcUser = databaseElem.select("db > jdbc > user").text();
      jdbcConn.setUser(jdbcUser);
      String jdbcPw = databaseElem.select("db > jdbc > pw").text();
      jdbcConn.setPw(jdbcPw);
      database.setJdbcConnection(jdbcConn);
      String sql = databaseElem.select("db > sql").text();
      database.setSql(sql);
    }
    String oaiSet = databaseElem.select("db > set").text();
    if (oaiSet != null)
      database.setOaiSet(oaiSet);
    String mainResourcesTable = databaseElem.select("db > mainResourcesTable > name").text();
    if (mainResourcesTable != null)
      database.setMainResourcesTable(mainResourcesTable);
    String mainResourcesTableId = databaseElem.select("db > mainResourcesTable > idField").text();
    if (mainResourcesTableId != null)
      database.setMainResourcesTableId(mainResourcesTableId);
    Elements mainResourcesTableFieldsElems = databaseElem.select("db > mainResourcesTable > field");
    for (int i=0; i< mainResourcesTableFieldsElems.size(); i++) {
      Element mainResourcesTableFieldsElem = mainResourcesTableFieldsElems.get(i);  // e.g. <field><dc>title</dc><db>title</db></field>
      String dcField = mainResourcesTableFieldsElem.select("field > dc").text();
      String dbField = mainResourcesTableFieldsElem.select("field > db").text();
      if (dcField != null && dbField != null)
        database.addField(dcField, dbField);
    }
    String webIdPreStr = databaseElem.select("db > webId > preStr").text();
    if (webIdPreStr != null)
      database.setWebIdPreStr(webIdPreStr);
    String webIdAfterStr = databaseElem.select("db > webId > afterStr").text();
    if (webIdAfterStr != null)
      database.setWebIdAfterStr(webIdAfterStr);
    // read excludes (e.g. used in crawl or eXist resources, to eliminate these sub directories)
    Elements excludeElems = databaseElem.select("db > excludes > exclude");
    if (excludeElems != null && ! excludeElems.isEmpty()) {
      ArrayList<String> excludes = new ArrayList<String>();
      for (int i=0; i< excludeElems.size(); i++) {
        String excludeStr = excludeElems.get(i).text();
        excludes.add(excludeStr);
      }
      database.setExcludes(excludes);
    }
    return database;
  }
}
