package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.Hashtable;

import org.bbaw.wsp.cms.document.XQuery;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Database {
  private String rdfId; // start url (crawl, eXist or oai), e.g. http://telota.bbaw.de:8085/exist/rest/db/AvHBriefedition
  private String collectionRdfId; // rdfId of collection which this database is part of
  private String type;
  private String name; // unique database name (converted from rdfId); e.g. telota.bbaw.de:8085##exist##rest##db##AvHBriefedition
  private String xmlDumpUrl;
  private JdbcConnection jdbcConnection;
  private String sql;
  private String oaiSet; // if type is oai: a set could be defined 
  private String mainResourcesTable;
  private String mainResourcesTableId;
  private String webIdPreStr;
  private String webIdAfterStr;
  private ArrayList<String> excludes; // exclude urls e.g. for eXist directories or crawls
  private ArrayList<String> includes; // include urls e.g. for crawls
  private ArrayList<String> contentCssSelectors; // css selectors to find the content element in html pages e.g.: "#content"
  private int depth; // if crawl db: crawl depth
  private Hashtable<String, XQuery> xQueries;
  private Hashtable<String, String> dcField2dbField = new Hashtable<String, String>();
  private Hashtable<String, String> edocInstituteName2collectionRdfId = new Hashtable<String, String>();

  public String getRdfId() {
    return rdfId;
  }

  public void setRdfId(String rdfId) {
    this.rdfId = rdfId;
    this.name = buildUniqueName(rdfId);
  }

  public String getCollectionRdfId() {
    return collectionRdfId;
  }

  public void setCollectionRdfId(String collectionRdfId) {
    this.collectionRdfId = collectionRdfId;
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
  
  public String getXmlDumpUrl() {
    return xmlDumpUrl;
  }

  public void setXmlDumpUrl(String xmlDumpUrl) {
    this.xmlDumpUrl = xmlDumpUrl;
  }

  public String getLanguage() throws ApplicationException {
    String retLang = null;
    String collMainLang = ProjectReader.getInstance().getCollectionMainLanguage(collectionRdfId);
    if (collMainLang != null)
      retLang = collMainLang;
    return retLang;
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

  public ArrayList<String> getIncludes() {
    return includes;
  }

  public void setIncludes(ArrayList<String> includes) {
    this.includes = includes;
  }

  public ArrayList<String> getContentCssSelectors() {
    return contentCssSelectors;
  }

  public void setContentCssSelectors(ArrayList<String> contentCssSelectors) {
    this.contentCssSelectors = contentCssSelectors;
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
  
  public String getEdocCollectionRdfId(String instituteName) throws ApplicationException {
    String collectionRdfId = edocInstituteName2collectionRdfId.get(instituteName);
    return collectionRdfId;
  }
  
  public boolean isEdocDB() {
    boolean isEdoc = false;
    if (edocInstituteName2collectionRdfId != null && ! edocInstituteName2collectionRdfId.isEmpty())
      isEdoc = true;
    return isEdoc;
  }
  
  public static Database fromDatabaseElement(Element databaseElem) {
    Database database = new Database();
    String databaseRdfId = databaseElem.select("db").attr("id");
    database.setRdfId(databaseRdfId);
    String type = databaseElem.select("db > type").text();  // e.g. "crawl", "eXist", "oai", "mysql", "postgres" or "xmldump"
    database.setType(type);
    String url = databaseElem.select("db > url").text(); // redundant field to id attribute
    if (databaseRdfId == null || databaseRdfId.isEmpty())
      database.setRdfId(url);
    String xmlDumpUrl = databaseElem.select("db > xmlDump > url").text();
    database.setXmlDumpUrl(xmlDumpUrl);
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
      String sql = databaseElem.select("db > xmlDump > sql").text();
      database.setSql(sql);
    }
    String oaiSet = databaseElem.select("db > set").text();
    if (oaiSet != null && ! oaiSet.isEmpty()) {
      database.setOaiSet(oaiSet);
      String databaseName = database.buildUniqueName(database.rdfId); // rebuild it so that the oai set name is appended to the db name
      database.setName(databaseName);
    }
    String mainResourcesTable = databaseElem.select("db > mainResourcesTable > name").text();
    if (mainResourcesTable != null && !mainResourcesTable.isEmpty())
      database.setMainResourcesTable(mainResourcesTable);
    String mainResourcesTableId = databaseElem.select("db > mainResourcesTable > idField").text();
    if (mainResourcesTableId != null && ! mainResourcesTableId.isEmpty())
      database.setMainResourcesTableId(mainResourcesTableId);
    Elements mainResourcesTableFieldsElems = databaseElem.select("db > mainResourcesTable > field");
    for (int i=0; i< mainResourcesTableFieldsElems.size(); i++) {
      Element mainResourcesTableFieldsElem = mainResourcesTableFieldsElems.get(i);  // e.g. <field><dc>title</dc><db>title</db></field>
      String dcField = mainResourcesTableFieldsElem.select("field > dc").text();
      String dbField = mainResourcesTableFieldsElem.select("field > db").text();
      if (dcField != null && dbField != null && !dcField.isEmpty() && !dbField.isEmpty())
        database.addField(dcField, dbField);
    }
    String webIdPreStr = databaseElem.select("db > webId > preStr").text();
    if (webIdPreStr != null && ! webIdPreStr.isEmpty())
      database.setWebIdPreStr(webIdPreStr);
    String webIdAfterStr = databaseElem.select("db > webId > afterStr").text();
    if (webIdAfterStr != null && ! webIdAfterStr.isEmpty())
      database.setWebIdAfterStr(webIdAfterStr);
    // read excludes (e.g. used in crawl or eXist databases, to eliminate these sub directories)
    Elements excludeElems = databaseElem.select("db > excludes > exclude");
    if (excludeElems != null && ! excludeElems.isEmpty()) {
      ArrayList<String> excludes = new ArrayList<String>();
      for (int i=0; i< excludeElems.size(); i++) {
        String excludeStr = excludeElems.get(i).text();
        if (excludeStr != null && ! excludeStr.isEmpty())
          excludes.add(excludeStr);
      }
      database.setExcludes(excludes);
    }
    // read includes (e.g. used in crawl databases, to include these urls)
    Elements includeElems = databaseElem.select("db > includes > include");
    if (includeElems != null && ! includeElems.isEmpty()) {
      ArrayList<String> includes = new ArrayList<String>();
      for (int i=0; i< includeElems.size(); i++) {
        String includeStr = includeElems.get(i).text();
        if (includeStr != null && ! includeStr.isEmpty())
          includes.add(includeStr);
      }
      database.setIncludes(includes);
    }
    // read xqueries (used for building dynamically a webUri of a record; only used in xml records
    Elements xqueryElems = databaseElem.select("db > xqueries > xquery");
    if (xqueryElems != null && ! xqueryElems.isEmpty()) {
      Hashtable<String, XQuery> xqueriesHashtable = new Hashtable<String, XQuery>();
      for (int i=0; i< xqueryElems.size(); i++) {
        Element xqueryElem = xqueryElems.get(i);
        String xQueryName = xqueryElem.select("name").text();
        String xQueryCode = xqueryElem.select("code").text();
        String xQueryPreStr = xqueryElem.select("preStr").text();
        String xQueryAfterStr = xqueryElem.select("afterStr").text();
        if (xQueryName != null && ! xQueryName.isEmpty() && xQueryCode != null && ! xQueryCode.isEmpty()) {
          XQuery xQuery = new XQuery(xQueryName, xQueryCode);
          if (xQueryPreStr != null && ! xQueryPreStr.isEmpty())
            xQuery.setPreStr(xQueryPreStr);
          if (xQueryAfterStr != null && ! xQueryAfterStr.isEmpty())
            xQuery.setAfterStr(xQueryAfterStr);
          xqueriesHashtable.put(xQueryName, xQuery);
        }
      }
      database.setxQueries(xqueriesHashtable);
    }
    // read contentCssSelectors
    Elements contentCssSelectorElems = databaseElem.select("db > contentCssSelectors > contentCssSelector");
    if (contentCssSelectorElems != null && ! contentCssSelectorElems.isEmpty()) {
      ArrayList<String> contentCssSelectors = new ArrayList<String>();
      for (int i=0; i< contentCssSelectorElems.size(); i++) {
        String contentCssSelectorStr = contentCssSelectorElems.get(i).text();
        if (contentCssSelectorStr != null && ! contentCssSelectorStr.isEmpty())
          contentCssSelectors.add(contentCssSelectorStr);
      }
      database.setContentCssSelectors(contentCssSelectors);
    }
    Elements edocInstitutesElems = databaseElem.select("db > institutes > institute");
    for (int i=0; i< edocInstitutesElems.size(); i++) {
      Element edocInstitutesElem = edocInstitutesElems.get(i);
      String instituteName = edocInstitutesElem.text();
      String collectionRdfId = edocInstitutesElem.attr("collection-id");
      if (instituteName != null && collectionRdfId != null && ! collectionRdfId.isEmpty())
        database.edocInstituteName2collectionRdfId.put(instituteName, collectionRdfId);
    }
    return database;
  }

  private String buildUniqueName(String rdfId) {
    if (rdfId == null)
      return null;
    // e.g. "http://telota.bbaw.de:8085/exist/rest/db/AvHBriefedition" is converted to "telota.bbaw.de:8085##exist##rest##db##AvHBriefedition"
    String fileNameId = rdfId.replaceFirst("https*://", "");
    fileNameId = fileNameId.replaceAll("/", "##");
    if (type != null && type.equals("oai")) {
      if (oaiSet != null)
        fileNameId = fileNameId + "##" + oaiSet;
    }
    return fileNameId;
  }

  public void addExclude(String excludeStr) {
    boolean excludeIsContained = excludes.contains(excludeStr);
    if (! excludeIsContained)
      excludes.add(excludeStr);
  }
}
