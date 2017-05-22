package org.bbaw.wsp.cms.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.document.SubjectHandler;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.util.Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

public class MetadataHandler {
  private static Logger LOGGER = Logger.getLogger(MetadataHandler.class);
  private static MetadataHandler mdHandler;
  private static int SOCKET_TIMEOUT = 20 * 1000;
  private Hashtable<String, ArrayList<MetadataRecord>> projectMdRecords;
  private XQueryEvaluator xQueryEvaluator;
  private HttpClient httpClient; 

  public static MetadataHandler getInstance() throws ApplicationException {
    if(mdHandler == null) {
      mdHandler = new MetadataHandler();
      mdHandler.init();
    }
    return mdHandler;
  }
  
  private void init() throws ApplicationException {
    projectMdRecords = new Hashtable<String, ArrayList<MetadataRecord>>();
    xQueryEvaluator = new XQueryEvaluator();
    httpClient = new HttpClient();
    // timeout seems to work
    httpClient.getParams().setParameter("http.socket.timeout", SOCKET_TIMEOUT);
    httpClient.getParams().setParameter("http.connection.timeout", SOCKET_TIMEOUT);
    httpClient.getParams().setParameter("http.connection-manager.timeout", new Long(SOCKET_TIMEOUT));
    httpClient.getParams().setParameter("http.protocol.head-body-timeout", SOCKET_TIMEOUT);
  }
  
  public void deleteConfigurationFiles(Project project) {
    String projectId = project.getId();
    String rdfRessourcesFileStr = Constants.getInstance().getMetadataDir() + "/resources" + "/" + projectId + ".rdf";
    File rdfRessourcesFile = new File(rdfRessourcesFileStr);
    FileUtils.deleteQuietly(rdfRessourcesFile);
    LOGGER.info("Project configuration file: " + rdfRessourcesFileStr + " successfully deleted");
    String xmlConfFileStr = Constants.getInstance().getMetadataDir() + "/resources-addinfo" + "/" + projectId + ".xml";
    File xmlConfFile = new File(rdfRessourcesFileStr);
    FileUtils.deleteQuietly(xmlConfFile);
    LOGGER.info("Project configuration file: " + xmlConfFileStr + " successfully deleted");
  }
  
  public ArrayList<MetadataRecord> getMetadataRecords(Project project, boolean generateId) throws ApplicationException {
    String projectId = project.getId();
    ArrayList<MetadataRecord> mdRecords = projectMdRecords.get(projectId);
    if (generateId || mdRecords == null) {
      String metadataRdfDir = Constants.getInstance().getMetadataDir() + "/resources";
      File rdfRessourcesFile = new File(metadataRdfDir + "/" + projectId + ".rdf");
      mdRecords = getMetadataRecordsByRdfFile(project, rdfRessourcesFile, null, generateId);
    } 
    if (mdRecords.size() == 0)
      return null;
    else 
      return mdRecords;
  }

  public ArrayList<MetadataRecord> getMetadataRecords(Project project, Database db, boolean generateId) throws ApplicationException {
    ArrayList<MetadataRecord> dbMdRecords = null;
    String dbType = db.getType();
    if (dbType.equals("eXist")) {
      dbMdRecords = getMetadataRecordsEXist(project, db, generateId);
    } else if (dbType.equals("crawl")) {
      dbMdRecords = getMetadataRecordsCrawl(project, db, generateId);
    } else { 
      // nothing
    }
    return dbMdRecords;
  }
  
  private ArrayList<MetadataRecord> getMetadataRecordsEXist(Project project, Database db, boolean generateId) throws ApplicationException {
    String projectId = project.getId();
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    Integer maxIdcounter = null;
    String urlStr = db.getRdfId();
    ProjectManager pm = ProjectManager.getInstance();
    MetadataRecord mdRecord = getNewMdRecord(urlStr); 
    mdRecord.setSystem("eXist");
    String organizationRdfId = project.getOrganizationRdfId();
    if (organizationRdfId != null)
      mdRecord.setOrganizationRdfId(organizationRdfId);
    mdRecord.setProjectId(projectId);
    String projectRdfId = project.getRdfId();
    mdRecord.setProjectRdfId(projectRdfId);
    String collectionRdfId = db.getCollectionRdfId();
    ProjectCollection rootCollection = ProjectReader.getInstance().getRootCollection(collectionRdfId); // hack: resources are set to root collection
    String rootCollectionRdfId = rootCollection.getRdfId(); 
    mdRecord.setCollectionRdfId(rootCollectionRdfId);
    String databaseRdfId = db.getRdfId();
    mdRecord.setDatabaseRdfId(databaseRdfId);
    if (generateId) {
      maxIdcounter = pm.getStatusMaxId();  // find the highest value, so that each following id is a real new id
      mdRecord.setId(maxIdcounter); // projects wide id
    }
    mdRecord = createMainFieldsMetadataRecord(mdRecord, project, null);
    Date lastModified = new Date();
    mdRecord.setLastModified(lastModified);
    String language = db.getLanguage();
    if (language != null)
      mdRecord.setLanguage(language);
    String eXistUrl = mdRecord.getWebUri();
    if (eXistUrl != null) {
      ArrayList<MetadataRecord> mdRecordsEXist = new ArrayList<MetadataRecord>();
      if (eXistUrl.endsWith("/"))
        eXistUrl = eXistUrl.substring(0, eXistUrl.length() - 1);
      ArrayList<String> excludes = db.getExcludes();
      List<String> eXistUrls = extractDocumentUrls(eXistUrl, excludes);
      for (int i=0; i<eXistUrls.size(); i++) {
        String eXistSubUrlStr = eXistUrls.get(i);
        MetadataRecord mdRecordEXist = getNewMdRecord(eXistSubUrlStr); // with docId and webUri
        mdRecordEXist.setProjectId(projectId);
        if (generateId) {
          maxIdcounter++;
          mdRecordEXist.setId(maxIdcounter); // projects wide id
        }
        mdRecordEXist = createMainFieldsMetadataRecord(mdRecordEXist, project, db);
        mdRecordEXist.setLastModified(lastModified);
        mdRecordEXist.setCreator(mdRecord.getCreator());
        mdRecordEXist.setTitle(mdRecord.getTitle());
        mdRecordEXist.setPublisher(mdRecord.getPublisher());
        mdRecordEXist.setDate(mdRecord.getDate());
        mdRecordEXist.setSystem("eXist");
        if (mdRecord.getLanguage() != null)
          mdRecordEXist.setLanguage(mdRecord.getLanguage());
        mdRecordsEXist.add(mdRecordEXist);
      }
      mdRecords.addAll(mdRecordsEXist);
    }
    if (generateId) {
      pm.setStatusMaxId(maxIdcounter);
    }
    return mdRecords;
  }
  
  private ArrayList<MetadataRecord> getMetadataRecordsCrawl(Project project, Database db, boolean generateId) throws ApplicationException {
    String projectId = project.getId();
    ProjectManager pm = ProjectManager.getInstance();
    Integer maxIdcounter = null;
    if (generateId) {
      maxIdcounter = pm.getStatusMaxId();  // find the highest value, so that each following id is a real new id
    }
    Date lastModified = new Date();
    String startUrl = db.getRdfId();
    ArrayList<String> excludes = db.getExcludes();
    ArrayList<String> includes = db.getIncludes();
    Integer depth = db.getDepth();
    Crawler crawler = new Crawler(startUrl, depth, excludes, includes);
    ArrayList<MetadataRecord> crawledMdRecords = crawler.crawl();
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    for (int i=0; i<crawledMdRecords.size(); i++) {
      MetadataRecord crawledMdRecord = crawledMdRecords.get(i);
      String crawlUrlStr = crawledMdRecord.getWebUri();
      MetadataRecord newCrawledMdRecord = getNewMdRecord(crawlUrlStr); // with docId and webUri
      String organizationRdfId = project.getOrganizationRdfId();
      if (organizationRdfId != null)
        newCrawledMdRecord.setOrganizationRdfId(organizationRdfId);
      newCrawledMdRecord.setProjectId(projectId);
      String projectRdfId = project.getRdfId();
      newCrawledMdRecord.setProjectRdfId(projectRdfId);
      String collectionRdfId = db.getCollectionRdfId();
      ProjectCollection rootCollection = ProjectReader.getInstance().getRootCollection(collectionRdfId); // hack: resources are set to root collection
      String rootCollectionRdfId = rootCollection.getRdfId(); 
      newCrawledMdRecord.setCollectionRdfId(rootCollectionRdfId);
      String databaseRdfId = db.getRdfId();
      newCrawledMdRecord.setDatabaseRdfId(databaseRdfId);
      newCrawledMdRecord.setType(crawledMdRecord.getType());
      if (generateId) {
        maxIdcounter++;
        newCrawledMdRecord.setId(maxIdcounter); // projects wide id
      }
      newCrawledMdRecord = createMainFieldsMetadataRecord(newCrawledMdRecord, project, db);
      newCrawledMdRecord.setLastModified(lastModified);
      newCrawledMdRecord.setSystem("crawl");
      String dbLanguage = db.getLanguage();
      if (dbLanguage != null)
        newCrawledMdRecord.setLanguage(dbLanguage);
      String mimeType = newCrawledMdRecord.getType();
      if (mimeType != null)
        mdRecords.add(newCrawledMdRecord);
    }
    if (generateId) {
      pm.setStatusMaxId(maxIdcounter);
    }
    return mdRecords;
  }
  
  public int fetchMetadataRecordsOai(String dirName, Project project, Database db) throws ApplicationException {
    // fetch the oai records and write them to xml files
    StringBuilder xmlOaiStrBuilder = new StringBuilder();
    String oaiServerUrl = db.getRdfId();
    String oaiSet = db.getOaiSet();
    String listRecordsUrl = oaiServerUrl + "?verb=ListRecords&metadataPrefix=oai_dc&set=" + oaiSet;
    if (oaiSet == null)
      listRecordsUrl = oaiServerUrl + "?verb=ListRecords&metadataPrefix=oai_dc";
    String oaiPmhResponseStr = performGetRequest(listRecordsUrl);
    String oaiRecordsStr = xQueryEvaluator.evaluateAsString(oaiPmhResponseStr, "/*:OAI-PMH/*:ListRecords/*:record");
    xmlOaiStrBuilder.append(oaiRecordsStr);
    String resumptionToken = xQueryEvaluator.evaluateAsString(oaiPmhResponseStr, "/*:OAI-PMH/*:ListRecords/*:resumptionToken/text()");
    // if end is reached before resumptionToken comes
    if (resumptionToken == null || resumptionToken.isEmpty()) {
      writeOaiDbXmlFile(dirName, project, db, xmlOaiStrBuilder, 1);
    } else {
      int resumptionTokenCounter = 1; 
      int fileCounter = 1;
      while (resumptionToken != null && ! resumptionToken.isEmpty()) {
        String listRecordsResumptionTokenUrl = oaiServerUrl + "?verb=ListRecords&resumptionToken=" + resumptionToken;
        oaiPmhResponseStr = performGetRequest(listRecordsResumptionTokenUrl);
        LOGGER.info("Fetch OAI records: " + listRecordsResumptionTokenUrl);
        oaiRecordsStr = xQueryEvaluator.evaluateAsString(oaiPmhResponseStr, "/*:OAI-PMH/*:ListRecords/*:record");
        xmlOaiStrBuilder.append(oaiRecordsStr + "\n");
        resumptionToken = xQueryEvaluator.evaluateAsString(oaiPmhResponseStr, "/*:OAI-PMH/*:ListRecords/*:resumptionToken/text()");
        int writeFileInterval = resumptionTokenCounter % 100; // after each 100 resumptionTokens generate a new file
        if (writeFileInterval == 0) {
          writeOaiDbXmlFile(dirName, project, db, xmlOaiStrBuilder, fileCounter);
          xmlOaiStrBuilder = new StringBuilder();
          fileCounter++;
        }
        resumptionTokenCounter++;
      }
      // write the last resumptionTokens
      if (xmlOaiStrBuilder.length() > 0)
        writeOaiDbXmlFile(dirName, project, db, xmlOaiStrBuilder, fileCounter);
    }
    // convert the xml files to rdf files
    int recordsCount = convertDbXmlFiles(dirName, project, db);
    return recordsCount;
  }

  private void writeOaiDbXmlFile(String dirName, Project project, Database db, StringBuilder recordsStrBuilder, int fileCounter) throws ApplicationException {
    StringBuilder xmlOaiStrBuilder = new StringBuilder();
    String oaiSet = db.getOaiSet();
    String xmlOaiDbFileName = dirName + "/" + project.getId() + "-" + db.getName() + "-" + fileCounter + ".xml";
    xmlOaiStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    if (oaiSet == null)
      xmlOaiStrBuilder.append("<!-- Resources of OAI-PMH-Server: " + db.getRdfId() + " -->\n");
    else 
      xmlOaiStrBuilder.append("<!-- Resources of OAI-PMH-Server: " + db.getRdfId() + ", Set: " + oaiSet + " -->\n");
    xmlOaiStrBuilder.append("<OAI-PMH>\n");
    xmlOaiStrBuilder.append("<ListRecords>\n");
    xmlOaiStrBuilder.append(recordsStrBuilder.toString());
    xmlOaiStrBuilder.append("</ListRecords>\n");
    xmlOaiStrBuilder.append("</OAI-PMH>");
    try {
      File xmlOaiDbFile = new File(xmlOaiDbFileName);
      FileUtils.writeStringToFile(xmlOaiDbFile, xmlOaiStrBuilder.toString());
      LOGGER.info("OAI XML database file \"" + xmlOaiDbFileName + "\" sucessfully created");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  public void generateDbXmlDumpFileByJdbc(String dbFilesDirName, Project project, Database db) throws ApplicationException {
    String xmlDumpFileName = dbFilesDirName + "/" + project.getId() + "-" + db.getName() + "-1" + ".xml";
    StringBuilder xmlDumpStrBuilder = new StringBuilder();
    xmlDumpStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    xmlDumpStrBuilder.append("<!-- Resources of database: " + db.getName() + " (Database file name: " + xmlDumpFileName + ") -->\n");
    JdbcConnection jdbcConn = db.getJdbcConnection();
    xmlDumpStrBuilder.append("<" + jdbcConn.getDb() + ">\n");
    Properties connectionProps = new Properties();
    connectionProps.put("user", jdbcConn.getUser());
    connectionProps.put("password", jdbcConn.getPw());
    Connection conn = null;
    String jdbcUrl = null;
    try {
      String dbType = db.getType();
      if (dbType.equals("mysql")) {
        Class.forName("com.mysql.jdbc.Driver");
      } else {
        return;  // TODO postgres etc.
      }
      jdbcUrl = "jdbc:" + dbType + "://" + jdbcConn.getHost() + ":" + jdbcConn.getPort() + "/" + jdbcConn.getDb();  // + "?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8_general_ci&characterSetResults=utf8"
      LOGGER.info("Generate XML dump file by JDBC: Try to get a connection to: " + jdbcUrl + " ... ");
      conn = DriverManager.getConnection(jdbcUrl, connectionProps);
      LOGGER.info("Generate XML dump file by JDBC: Connection to: " + jdbcUrl + " established");
      PreparedStatement initPS = conn.prepareStatement("SET group_concat_max_len = 999999");
      initPS.execute();
      initPS.close();
      String select = db.getSql();
      LOGGER.info("Generate XML dump file by JDBC: Prepare Statement: \n" + select);
      PreparedStatement preparedStatement = conn.prepareStatement(select);
      ResultSet rs = preparedStatement.executeQuery();
      ResultSetMetaData rsMetaData = rs.getMetaData();
      while (rs.next()) {
        xmlDumpStrBuilder.append("  <" + db.getMainResourcesTable() + ">\n");
        int colCount = rsMetaData.getColumnCount();
        for (int i=1; i<=colCount; i++) {
          String colName = rsMetaData.getColumnName(i).toLowerCase();
          String colValue = rs.getString(colName);
          xmlDumpStrBuilder.append("    <" + colName + ">" + colValue + "</" + colName + ">\n");
        }
        xmlDumpStrBuilder.append("  </" + db.getMainResourcesTable() + ">\n");
      }
      rs.close();
      conn.close();
      xmlDumpStrBuilder.append("</" + jdbcConn.getDb() + ">\n");
      File dumpFile = new File(xmlDumpFileName);
      FileUtils.writeStringToFile(dumpFile, xmlDumpStrBuilder.toString(), "utf-8");
    } catch (Exception e) {
      try {
        conn.close();
      } catch (Exception e2) {
        // nothing
      };
      LOGGER.error("Generate XML dump file by JDBC (" + jdbcUrl + ") failed: " + e.getMessage());
    }
  }

  public int convertDbXmlFiles(String dbFilesDirName, Project project, Database db) throws ApplicationException {
    int counter = 0;
    File dbFilesDir = new File(dbFilesDirName);
    String xmlDbFileFilterName = project.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter xmlDbFileFilter = new WildcardFileFilter(xmlDbFileFilterName);
    File[] xmlDbFiles = dbFilesDir.listFiles(xmlDbFileFilter);
    if (xmlDbFiles != null && xmlDbFiles.length > 0) {
      Arrays.sort(xmlDbFiles, new Comparator<File>() {
        public int compare(File f1, File f2) {
          return f1.getName().compareToIgnoreCase(f2.getName());
        }
      }); 
      // delete the old db rdf files
      String rdfFileFilterName = project.getId() + "-" + db.getName() + "*.rdf"; 
      FileFilter rdfFileFilter = new WildcardFileFilter(rdfFileFilterName);
      File[] rdfFiles = dbFilesDir.listFiles(rdfFileFilter);
      if (rdfFiles != null && rdfFiles.length > 0) {
        for (int i = 0; i < rdfFiles.length; i++) {
          File rdfFile = rdfFiles[i];
          FileUtils.deleteQuietly(rdfFile);
        }
      }
      // generate the new db rdf files
      for (int i = 0; i < xmlDbFiles.length; i++) {
        File xmlDbFile = xmlDbFiles[i];
        int fileRecordsCount = convertDbXmlFile(dbFilesDirName, project, db, xmlDbFile);
        counter = counter + fileRecordsCount;
      }
    }
    return counter;
  }
  
  private int convertDbXmlFile(String dbFilesDirName, Project project, Database db, File xmlDbFile) throws ApplicationException {
    int counter = 0;
    try {
      StringBuilder rdfRecordsStrBuilder = new StringBuilder();
      String rdfHeaderStr = getRdfHeaderStr(project);
      rdfRecordsStrBuilder.append(rdfHeaderStr);
      String xmlDbFileName = xmlDbFile.getName();
      rdfRecordsStrBuilder.append("<!-- Resources of database: " + db.getName() + " (Database file name: " + xmlDbFileName + ") -->\n");
      URL xmlDumpFileUrl = xmlDbFile.toURI().toURL();
      String mainResourcesTable = db.getMainResourcesTable();  // e.g. "titles_persons";
      String dbType = db.getType();
      XdmValue xmdValueMainResources = null;
      Integer idCounter = null;
      if (dbType.equals("mysql")) {
        xmdValueMainResources = xQueryEvaluator.evaluate(xmlDumpFileUrl, "/" + "*" + "/" + mainResourcesTable);
        XdmValue xmdValueIds = xQueryEvaluator.evaluate(xmlDumpFileUrl, "/" + "*" + "/" + mainResourcesTable + "/*:id");
        // if no id field exists then insert automatically ids starting from 1
        if (xmdValueIds != null && xmdValueIds.size() == 0)
          idCounter = new Integer(0);
      } else if (dbType.equals("postgres")) {
        xmdValueMainResources = xQueryEvaluator.evaluate(xmlDumpFileUrl, "/data/records/row");
      } else if (dbType.equals("oai") || dbType.equals("oai-dbrecord")) {
        xmdValueMainResources = xQueryEvaluator.evaluate(xmlDumpFileUrl, "/*:OAI-PMH/*:ListRecords/*:record");
      }
      XdmSequenceIterator xmdValueMainResourcesIterator = xmdValueMainResources.iterator();
      if (xmdValueMainResources != null && xmdValueMainResources.size() > 0) {
        while (xmdValueMainResourcesIterator.hasNext()) {
          XdmItem xdmItemMainResource = xmdValueMainResourcesIterator.next();
          String xdmItemMainResourceStr = xdmItemMainResource.toString();
          if (idCounter != null)
            idCounter++;
          Row row = xml2row(xdmItemMainResourceStr, db, idCounter);
          if (row != null) {
            appendRow2rdf(rdfRecordsStrBuilder, project, db, row);
            counter++;
          }
        }
      }
      String rdfFileName = xmlDbFileName.replaceAll(".xml", ".rdf");
      String rdfFullFileName = dbFilesDirName + "/" + rdfFileName; 
      File rdfFile = new File(rdfFullFileName);
      rdfRecordsStrBuilder.append("</rdf:RDF>");
      FileUtils.writeStringToFile(rdfFile, rdfRecordsStrBuilder.toString());
      return counter;
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  private Row xml2row(String rowXmlStr, Database db, Integer idCounter) throws ApplicationException {
    String dbType = db.getType();
    Row row = new Row();
    if (dbType.equals("mysql")) {
      if (idCounter != null)
        row.addField("id", idCounter.toString());
      XdmValue xmdValueFields = xQueryEvaluator.evaluate(rowXmlStr, "/*/*"); // e.g. <titles_persons><title>
      XdmSequenceIterator xmdValueFieldsIterator = xmdValueFields.iterator();
      if (xmdValueFields != null && xmdValueFields.size() > 0) {
        while (xmdValueFieldsIterator.hasNext()) {
          XdmItem xdmItemField = xmdValueFieldsIterator.next();
          String fieldStr = xdmItemField.toString();
          String fieldName = xQueryEvaluator.evaluateAsString(fieldStr, "*/name()");
          String fieldValue = xdmItemField.getStringValue();
          if (! fieldValue.trim().isEmpty())
            row.addField(fieldName, fieldValue);
        }
      }
    } else if (dbType.equals("postgres")) {
      XdmValue xmdValueFields = xQueryEvaluator.evaluate(rowXmlStr, "/*/*"); // e.g. <row><column name="title">
      XdmSequenceIterator xmdValueFieldsIterator = xmdValueFields.iterator();
      if (xmdValueFields != null && xmdValueFields.size() > 0) {
        while (xmdValueFieldsIterator.hasNext()) {
          XdmItem xdmItemField = xmdValueFieldsIterator.next();
          String fieldStr = xdmItemField.toString();
          String fieldName = xQueryEvaluator.evaluateAsString(fieldStr, "string(*/@name)");
          String fieldValue = xdmItemField.getStringValue();
          if (! fieldValue.trim().isEmpty())
            row.addField(fieldName, fieldValue);
        }
      }
    } else if (dbType.equals("oai") || dbType.equals("oai-dbrecord")) {
      String recordHeaderStatus = xQueryEvaluator.evaluateAsString(rowXmlStr, "string(/*:record/*:header/@status)");
      if (recordHeaderStatus != null && recordHeaderStatus.equals("deleted"))
        return null;
      String recordId = xQueryEvaluator.evaluateAsString(rowXmlStr, "/*:record/*:header/*:identifier/text()");
      if (recordId != null && ! recordId.isEmpty())
        row.addField("identifier", recordId);
      XdmValue xmdValueFields = xQueryEvaluator.evaluate(rowXmlStr, "/*:record/*:metadata/*:dc/*");
      XdmSequenceIterator xmdValueFieldsIterator = xmdValueFields.iterator();
      if (xmdValueFields != null && xmdValueFields.size() > 0) {
        while (xmdValueFieldsIterator.hasNext()) {
          XdmItem xdmItemField = xmdValueFieldsIterator.next();
          String fieldStr = xdmItemField.toString();
          String fieldName = xQueryEvaluator.evaluateAsString(fieldStr, "*/name()");
          String fieldValue = xdmItemField.getStringValue();
          if (! fieldValue.trim().isEmpty())
            row.addField(fieldName, fieldValue);
        }
      }
    }
    return row;  
  }

  private void appendRow2rdf(StringBuilder rdfStrBuilder, Project project, Database db, Row row) throws ApplicationException {
    String projectId = project.getId();
    String projectRdfId = project.getRdfId();
    String dbRdfId = db.getRdfId();
    String collectionRdfId = null; // special: for edoc
    String mainLanguage = db.getLanguage();
    String mainResourcesTableId = db.getMainResourcesTableId();
    String webIdPreStr = db.getWebIdPreStr();
    String webIdAfterStr = db.getWebIdAfterStr();
    String dbType = db.getType();
    String id = row.getFirstFieldValue(mainResourcesTableId);
    if (id == null)
      id = "";
    id = StringUtils.deresolveXmlEntities(id);
    String rdfId = "http://" + projectId + ".bbaw.de/id/" + id;
    if (id.startsWith("http://"))
      rdfId = id;  // id is already a url
    String rdfWebId = rdfId;
    if (webIdPreStr != null) {
      rdfWebId = webIdPreStr + id;
    }
    if (webIdAfterStr != null) {
      rdfWebId = rdfWebId + webIdAfterStr;
    }
    // special handling of edoc oai records
    if (dbRdfId.endsWith("edoc.bbaw.de/oai")) {
      String dbFieldIdentifier = db.getDbField("identifier");
      if (dbFieldIdentifier != null) {
        ArrayList<String> identifiers = row.getFieldValues(dbFieldIdentifier);
        if (identifiers != null) {
          String edocMetadataUrl = identifiers.get(0); // first one is web page with metadata, e.g. https://edoc.bbaw.de/frontdoor/index/index/docId/1
          id = edocMetadataUrl; 
          rdfWebId = identifiers.get(identifiers.size() - 1);  // last one is the fulltext document, e.g. https://edoc.bbaw.de/files/1/vortrag2309_akademieUnion.pdf
          // no fulltext document given in record
          if (! rdfWebId.startsWith("http") || rdfWebId.endsWith(".gz") || rdfWebId.endsWith(".zip") || rdfWebId.endsWith(".mp3")) {
            rdfWebId = edocMetadataUrl;
            id = "";
          }
          String edocMetadataHtmlStr = performGetRequest(edocMetadataUrl);
          edocMetadataHtmlStr = edocMetadataHtmlStr.replaceAll("<!DOCTYPE html .+>", "");
          Document doc = Jsoup.parse(edocMetadataHtmlStr, edocMetadataUrl);
          String institutesStr = doc.select("th:containsOwn(Institutes:) + td > a").text();
          String edocPublicationWithoutMappingCollectionRdfId = db.getEdocCollectionRdfId(""); // delivers normally "http://wissensspeicher.bbaw.de/rdf/projectdata/BBAWBibliothek/PublikationEdocServer/ohneZuordnung"
          if (institutesStr != null && ! institutesStr.isEmpty()) {
            institutesStr = institutesStr.trim().replaceAll("BBAW / ", "");
            String edocId = db.getEdocCollectionRdfId(institutesStr);
            if (edocId != null) {
              if (edocId.startsWith("http")) { // a collection rdf id is given
                ProjectCollection collection = ProjectReader.getInstance().getCollection(edocId);
                if (collection != null) {
                  collectionRdfId = edocId;
                  projectRdfId = collection.getProjectRdfId();
                } else {
                  LOGGER.error("Edoc institutes to collection mapping (see bbawbib.xml): " + edocId + " (collection) is not defined in any project file");
                  collectionRdfId = edocPublicationWithoutMappingCollectionRdfId;
                }
              } else {  // only a projectId is given
                Project p = ProjectReader.getInstance().getProject(edocId);
                if (p != null) {
                  projectRdfId = p.getRdfId();
                } else {
                  LOGGER.error("Edoc institutes to collection mapping (see bbawbib.xml): " + edocId + " (projectId) is not defined in a project file");
                  collectionRdfId = edocPublicationWithoutMappingCollectionRdfId;
                }
              }
            } else {
              LOGGER.error("Edoc institutes to collection mapping (see bbawbib.xml): " + institutesStr + " has no collection mapping");
              collectionRdfId = edocPublicationWithoutMappingCollectionRdfId; // some institutes (e.g. ALLEA) have no projectId: map it to a special edoc collection
            }
          } else {
            LOGGER.error("Edoc institutes to collection mapping (see bbawbib.xml): " + edocMetadataUrl + " (" + rdfWebId + ") has no institutes value (CSS selector: th:containsOwn(Institutes:) + td > a)");
            collectionRdfId = edocPublicationWithoutMappingCollectionRdfId; // if no institutes string is given: map it to a special edoc collection
          }
        }
      }
    }
    rdfStrBuilder.append("<rdf:Description rdf:about=\"" + rdfWebId + "\">\n");
    rdfStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
    rdfStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + projectRdfId + "\"/>\n");
    if (collectionRdfId != null)
      rdfStrBuilder.append("  <ore:isAggregatedBy rdf:resource=\"" + collectionRdfId + "\"/>\n");
    rdfStrBuilder.append("  <dc:identifier rdf:resource=\"" + rdfWebId + "\">" + id + "</dc:identifier>\n");
    if (dbType.equals("oai"))
      rdfStrBuilder.append("  <dc:type>oaiRecord</dc:type>\n");
    else
      rdfStrBuilder.append("  <dc:type>dbRecord</dc:type>\n");  // for huge oai projects such as jdg (performance gain in inserting them into lucene) 
    String dbFieldCreator = db.getDbField("creator");
    if (dbFieldCreator != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldCreator);
      if (fieldValues != null && ! fieldValues.isEmpty()) {
        for (int i=0; i<fieldValues.size(); i++) {
          String fieldValue = fieldValues.get(i);
          if (projectId.equals("jdg")) {
            int indexEqualSign = fieldValue.lastIndexOf("=");
            if (indexEqualSign != -1)
              fieldValue = fieldValue.substring(indexEqualSign + 1).trim();
          }
          fieldValue = StringUtils.deresolveXmlEntities(fieldValue);
          rdfStrBuilder.append("  <dc:creator>" + fieldValue + "</dc:creator>\n");
        }
      }
    }
    String dbFieldTitle = db.getDbField("title");
    if (dbFieldTitle != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldTitle);
      if (fieldValues != null && ! fieldValues.isEmpty()) {
        for (int i=0; i<fieldValues.size(); i++) {
          String fieldValue = fieldValues.get(i);
          fieldValue = StringUtils.deresolveXmlEntities(fieldValue);
          rdfStrBuilder.append("  <dc:title>" + fieldValue + "</dc:title>\n");
        }
      }
    }
    String dbFieldPublisher = db.getDbField("publisher");
    if (dbFieldPublisher != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldPublisher);
      if (fieldValues != null && ! fieldValues.isEmpty()) {
        for (int i=0; i<fieldValues.size(); i++) {
          String fieldValue = fieldValues.get(i);
          fieldValue = StringUtils.deresolveXmlEntities(fieldValue);
          rdfStrBuilder.append("  <dc:publisher>" + fieldValue + "</dc:publisher>\n");
        }
      }
    }
    String dbFieldDate = db.getDbField("date");
    if (dbFieldDate != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldDate);
      if (fieldValues != null && ! fieldValues.isEmpty()) {
        for (int i=0; i<fieldValues.size(); i++) {
          String fieldValue = fieldValues.get(i);
          fieldValue = StringUtils.deresolveXmlEntities(fieldValue);
          rdfStrBuilder.append("  <dc:date>" + fieldValue + "</dc:date>\n");
        }
      }
    }
    String dbFieldPages = db.getDbField("extent");
    if (dbFieldPages != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldPages);
      if (fieldValues != null && ! fieldValues.isEmpty()) {
        for (int i=0; i<fieldValues.size(); i++) {
          String fieldValue = fieldValues.get(i);
          fieldValue = StringUtils.deresolveXmlEntities(fieldValue);
          rdfStrBuilder.append("  <dc:extent>" + fieldValue + "</dc:extent>\n");
        }
      }
    }
    String dbFieldLanguage = db.getDbField("language");
    String language = null;
    if (dbFieldLanguage != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldLanguage);
      if (fieldValues != null)
        language = fieldValues.get(0);
      if (language != null && language.contains(" ")) {
        String[] langs = language.split(" ");
        language = langs[0];
      }
    }
    if (language != null)
      language = Language.getInstance().getISO639Code(language);
    if (language == null && mainLanguage != null)
      language = mainLanguage;
    else if (language == null && mainLanguage == null)
      language = "ger";
    rdfStrBuilder.append("  <dc:language>" + language + "</dc:language>\n");
    String dbFieldFormat = db.getDbField("format");
    if (dbFieldFormat != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldFormat);
      if (fieldValues != null && ! fieldValues.isEmpty()) {
        for (int i=0; i<fieldValues.size(); i++) {
          String fieldValue = fieldValues.get(i);
          fieldValue = StringUtils.deresolveXmlEntities(fieldValue);
          rdfStrBuilder.append("  <dc:format>" + fieldValue + "</dc:format>\n");
        }
      }
    }
    if (dbType != null && ! dbType.equals("oai"))
      rdfStrBuilder.append("  <dc:format>text/html</dc:format>\n");
    String dbFieldSubject = db.getDbField("subject");
    if (dbFieldSubject != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldSubject);
      if (fieldValues != null && ! fieldValues.isEmpty()) {
        for (int i=0; i<fieldValues.size(); i++) {
          String fieldValue = fieldValues.get(i);
          if (fieldValue != null && ! fieldValue.isEmpty()) {
            fieldValue = StringUtils.deresolveXmlEntities(fieldValue);
            if (fieldValue.startsWith("ddc:")) {
              SubjectHandler subjectHandler = SubjectHandler.getInstance();
              String ddcCode = fieldValue.substring(4);
              String ddcSubject = subjectHandler.getDdcSubject(ddcCode);
              if (ddcSubject != null)
                rdfStrBuilder.append("  <dc:subject xsi:type=\"dcterms:DDC\">" + ddcSubject + "</dc:subject>\n");
            } else {
              fieldValue = StringUtils.resolveXmlEntities(fieldValue);
              String[] subjects = fieldValue.split("###");
              if (! fieldValue.contains("###") && fieldValue.contains(";"))
                subjects = fieldValue.split(";");
              if (subjects != null) {
                for (int j=0; j<subjects.length; j++) {
                  String s = subjects[j].trim();
                  if (projectId.equals("jdg")) {
                    int indexLs = s.lastIndexOf("▴Ls");
                    if (indexLs != -1)
                      s = s.substring(indexLs + 3).trim();
                  }
                  s = StringUtils.deresolveXmlEntities(s);
                  if (projectId.equals("edoc"))
                    rdfStrBuilder.append("  <dc:subject xsi:type=\"SWD\">" + s + "</dc:subject>\n");
                  else 
                    rdfStrBuilder.append("  <dc:subject>" + s + "</dc:subject>\n");
                }
              }
            }
          }
        }
      }
    }
    String dbFieldAbstract = db.getDbField("abstract");
    if (dbFieldAbstract != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldAbstract);
      if (fieldValues != null && ! fieldValues.isEmpty()) {
        for (int i=0; i<fieldValues.size(); i++) {
          String fieldValue = fieldValues.get(i);
          fieldValue = StringUtils.deresolveXmlEntities(fieldValue);
          rdfStrBuilder.append("  <dc:abstract>" + fieldValue + "</dc:abstract>\n");
        }
      }
    }
    // TODO further dc fields
    rdfStrBuilder.append("</rdf:Description>\n");
  }
  
  public ArrayList<MetadataRecord> getMetadataRecordsByRecordsFile(File recordsFile) throws ApplicationException {
    ArrayList<MetadataRecord> mdRecords = null;
    if (! recordsFile.exists())
      return null;
    try {
      Document recordsDoc = Jsoup.parse(recordsFile, "utf-8");
      Elements records = recordsDoc.select("records > record");
      if (records != null && !records.isEmpty()) {
        mdRecords = new ArrayList<MetadataRecord>();
        for (int i=0; i<records.size(); i++) {
          Element record = records.get(i);
          MetadataRecord mdRecord = MetadataRecord.fromRecordElement(record);
          mdRecords.add(mdRecord);
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return mdRecords;
  }
  
  public ArrayList<MetadataRecord> getMetadataRecordsByRdfFile(Project project, File rdfRessourcesFile, Database db, boolean generateId) throws ApplicationException {
    if (! rdfRessourcesFile.exists())
      return null;
    String projectId = project.getId();
    String xmlDumpFileStr = rdfRessourcesFile.getPath().replaceAll("\\.rdf", ".xml");
    File xmlDumpFile = new File(xmlDumpFileStr);
    Date xmlDumpFileLastModified = new Date(xmlDumpFile.lastModified());
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    try {
      Document resourcesDoc = Jsoup.parse(rdfRessourcesFile, "utf-8");
      Elements resources = resourcesDoc.select("rdf|RDF > rdf|Description");
      ProjectManager pm = ProjectManager.getInstance();
      Integer maxIdcounter = pm.getStatusMaxId();  // find the highest value, so that each following id is a real new id
      if (resources != null && !resources.isEmpty()) {
        mdRecords = new ArrayList<MetadataRecord>();
        for (int i=0; i<resources.size(); i++) {
          Element resource = resources.get(i);
          String resourceType = resource.select("rdf|type").attr("rdf:resource");
          String resourceTypeName = "BibliographicResource"; // used in databases (old metadata structure)
          if (db == null)
            resourceTypeName = "DigitalResource"; // used in project rdf files (new metadata structure)
          if (resourceType.endsWith(resourceTypeName)) {
            MetadataRecord mdRecord = null;
            if (resourceTypeName.equals("BibliographicResource"))
              mdRecord = getMdRecordFromBibliographicResource(project, db, resource);
            else
              mdRecord = getMdRecordFromDigitalResource(project, resource);
            if (mdRecord != null) {
              String organizationRdfId = project.getOrganizationRdfId();
              if (organizationRdfId != null)
                mdRecord.setOrganizationRdfId(organizationRdfId);
              if (mdRecord.getProjectId() == null)
                mdRecord.setProjectId(projectId);
              String projectRdfId = project.getRdfId();
              if (mdRecord.getProjectRdfId() == null)
                mdRecord.setProjectRdfId(projectRdfId);
              maxIdcounter++;
              mdRecord.setId(maxIdcounter); // projects wide id
              mdRecord = createMainFieldsMetadataRecord(mdRecord, project, db);
              // if it is a record: set lastModified to the date of the harvested dump file; if it is a normal web record: set it to now (harvested now)
              Date lastModified = new Date();
              if (mdRecord.isRecord()) {
                lastModified = xmlDumpFileLastModified;
              }
              mdRecord.setLastModified(lastModified);
              mdRecords.add(mdRecord);
            }
          }
        }
      }
      pm.setStatusMaxId(maxIdcounter);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return mdRecords;
  }
  
  public void writeMetadataRecords(Project project, ArrayList<MetadataRecord> mdRecords, File outputFile, String type) throws ApplicationException {
    StringBuilder strBuilder = new StringBuilder();
    if (type.equals("rdf")) {
      String rdfHeaderStr = getRdfHeaderStr(project);
      strBuilder.append(rdfHeaderStr);
      for (int i=0; i<mdRecords.size(); i++) {
        MetadataRecord mdRecord = mdRecords.get(i);
        strBuilder.append(mdRecord.toRdfStr());
      }
      strBuilder.append("</rdf:RDF>");
    } else if (type.equals("xml")) {
      strBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      strBuilder.append("<records>\n");
      for (int i=0; i<mdRecords.size(); i++) {
        MetadataRecord mdRecord = mdRecords.get(i);
        strBuilder.append(mdRecord.toXmlStr());
      }
      strBuilder.append("</records>");
    }
    try {
      FileUtils.writeStringToFile(outputFile, strBuilder.toString());
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  private String getRdfHeaderStr(Project project) {
    String projectRdfId = project.getRdfId();
    StringBuilder rdfStrBuilder = new StringBuilder();
    rdfStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    rdfStrBuilder.append("<rdf:RDF \n");
    rdfStrBuilder.append("   xmlns=\"" + projectRdfId + "\" \n");
    rdfStrBuilder.append("   xml:base=\"" + projectRdfId + "\" \n");
    rdfStrBuilder.append("   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n");
    rdfStrBuilder.append("   xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" \n");
    rdfStrBuilder.append("   xmlns:dc=\"http://purl.org/dc/elements/1.1/\" \n");
    rdfStrBuilder.append("   xmlns:dcterms=\"http://purl.org/dc/terms/\" \n");
    rdfStrBuilder.append("   xmlns:gnd=\"http://d-nb.info/standards/elementset/gnd#\" \n");
    rdfStrBuilder.append("   xmlns:ore=\"http://www.openarchives.org/ore/terms/\" \n"); 
    rdfStrBuilder.append("   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"); 
    rdfStrBuilder.append(">\n");
    return rdfStrBuilder.toString();    
  }
  
  public File[] getRdfDbResourcesFiles(Project project, Database db) throws ApplicationException {
    String dbDumpsDirName = Constants.getInstance().getExternalDataDbDumpsDir();
    File dbDumpsDir = new File(dbDumpsDirName);
    String rdfFileFilterName = project.getId() + "-" + db.getName() + "*.rdf"; 
    FileFilter rdfFileFilter = new WildcardFileFilter(rdfFileFilterName);
    File[] rdfDbResourcesFiles = dbDumpsDir.listFiles(rdfFileFilter);
    if (rdfDbResourcesFiles != null && rdfDbResourcesFiles.length > 0) {
      Arrays.sort(rdfDbResourcesFiles, new Comparator<File>() {
        public int compare(File f1, File f2) {
          return f1.getName().compareToIgnoreCase(f2.getName());
        }
      }); 
    }
    return rdfDbResourcesFiles;
  }
  
  private MetadataRecord createMainFieldsMetadataRecord(MetadataRecord mdRecord, Project project, Database db) throws ApplicationException {
    try {
      boolean mimeTypeDetection = true;
      if (db != null && db.getType() != null && db.getType().equals("crawl"))
        mimeTypeDetection = false; // crawl db's have mimeTypeDetection already done
      Tika tika = new Tika();
      String webUriStr = mdRecord.getWebUri();
      String uriStr = mdRecord.getUri();
      URL uri = new URL(webUriStr);
      if (uriStr != null)
        uri = new URL(uriStr);
      String uriPath = uri.getPath();
      String prefix = "/exist/rest/db";
      if (uriPath.startsWith(prefix)) {
        uriPath = uriPath.substring(prefix.length());
      }
      // no file with extension (such as a.xml or b.pdf) but a directory: then a special default file-name is used in docId
      if (! uriPath.toLowerCase().matches(".*\\.csv$|.*\\.gif$|.*\\.jpg$|.*\\.jpeg$|.*\\.html$|.*\\.htm$|.*\\.log$|.*\\.mp3$|.*\\.pdf$|.*\\.txt$|.*\\.xml$|.*\\.doc$")) {
        String mimeType = null;
        try {
          boolean isDbRecord = mdRecord.isDbRecord();
          if (! mimeTypeDetection)
            mimeType = mdRecord.getType();
          if (! isDbRecord && mimeTypeDetection) {
            mimeType = tika.detect(uri); // much faster with tika
          }
          /* old code which is slow, when the files behind the url's are large 
          HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
          connection.setConnectTimeout(5000);
          connection.setReadTimeout(5000);
          connection.connect();
          mimeType = connection.getContentType();
          */
        } catch (IOException e) {
          LOGGER.error("get mime type failed for: " + webUriStr);
        }
        String fileExtension = "html";
        if (mimeType != null && mimeType.contains("html"))
          fileExtension = "html";
        else if (mimeType != null && mimeType.contains("pdf"))
          fileExtension = "pdf";
        else if (mimeType != null && mimeType.contains("xml"))
          fileExtension = "xml";
        else if (mimeType != null && mimeType.contains("png"))
          fileExtension = "png";
        else if (mimeType != null && mimeType.contains("jpg"))
          fileExtension = "jpg";
        int recordId = mdRecord.getId();
        String fileName = "indexxx" + recordId + "." + fileExtension;
        if (recordId == -1)
          fileName = "indexxx" + "." + fileExtension;
        if (uriPath.endsWith("/"))
          uriPath = uriPath + fileName;
        else
          uriPath = uriPath + "/" + fileName;
      }
      String projectId = project.getId();
      String normalizedUriPath = normalize(uriPath);
      String docId = "/" + projectId + normalizedUriPath;
      String mimeType = null;
      if (mimeTypeDetection) {
        mimeType = getMimeType(docId);
        if (mimeType == null) {  // last chance: try it with tika
          try {
            mimeType = tika.detect(uri);
          } catch (IOException e) {
            LOGGER.error("get mime type failed for: " + webUriStr);
            e.printStackTrace();
          }
        }
      }
      mdRecord.setDocId(docId);
      if (mdRecord.getUri() == null)
        mdRecord.setUri(webUriStr);
      if (mdRecord.getType() == null)
        mdRecord.setType(mimeType); 
      // if no language is set then take the mainLanguage of the database or project
      if (mdRecord.getLanguage() == null) {
        String mainLanguage = project.getMainLanguage();
        if (db != null)
          mainLanguage = db.getLanguage();
        mdRecord.setLanguage(mainLanguage);
      }
      if (db != null) {
        Hashtable<String, XQuery> xQueries = db.getxQueries();
        if (xQueries != null)
          mdRecord.setxQueries(xQueries);
      }
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecord;
  }

  private List<String> extractDocumentUrls(String projectDataUrl, ArrayList<String> excludes) {
    List<String> documentUrls = null;
    if (! projectDataUrl.equals("")){
      PathExtractor extractor = new PathExtractor(projectDataUrl, excludes);
      documentUrls = extractor.extract();
    }
    return documentUrls;
  }

  private String getMimeType(String docId) {
    String mimeType = null;
    FileNameMap fileNameMap = URLConnection.getFileNameMap();  // map with 53 entries such as "application/xml"
    mimeType = fileNameMap.getContentTypeFor(docId);
    return mimeType;
  }

  private MetadataRecord getMdRecordFromDigitalResource(Project project, Element resourceElem) throws ApplicationException {
    // download url of resource
    String resourceIdUrlStr = resourceElem.attr("rdf:about");
    if (resourceIdUrlStr == null) {
      LOGGER.error("No identifier given in resource: \"" + resourceElem + "\"");
      return null;
    }
    MetadataRecord mdRecord = getNewMdRecord(resourceIdUrlStr);
    // web url of resource
    String webUrlStr = resourceElem.select("dcterms|hasFormat").attr("rdf:resource");
    if (webUrlStr != null && ! webUrlStr.isEmpty()) {
      try {
        new URL(webUrlStr); // test if webUrlStr is a url, if so then set it as the webUri
        mdRecord.setWebUri(webUrlStr);
      } catch (MalformedURLException e) {
        // nothing
      }
      mdRecord.setUri(resourceIdUrlStr);
    }
    // collectionRdfId of which this resource is part of
    String collectionRdfId = resourceElem.select("ore|isAggregatedBy").attr("rdf:resource");
    if (collectionRdfId != null && ! collectionRdfId.isEmpty()) {
      collectionRdfId = collectionRdfId.trim();
      ProjectCollection rootCollection = ProjectReader.getInstance().getRootCollection(collectionRdfId); // hack: resources are set to root collection
      if (rootCollection != null) {
        String rootCollectionRdfId = rootCollection.getRdfId(); 
        mdRecord.setCollectionRdfId(rootCollectionRdfId);
      } else {
        LOGGER.error("Resource: \"" + resourceElem + "\": could not find rootCollection of: " + collectionRdfId + ". Please proof your resource.");
        return null;
      }
    }
    // creators
    Elements authorElems = resourceElem.select("dcterms|creator");
    if (! authorElems.isEmpty()) {
      ArrayList<Person> authors = new ArrayList<Person>();
      for (int i=0; i<authorElems.size(); i++) {
        Element authorElem = authorElems.get(i);
        String rdfIdPerson = authorElem.attr("rdf:resource");
        if (rdfIdPerson != null && ! rdfIdPerson.isEmpty()) {
          Person creator = ProjectReader.getInstance().getPerson(rdfIdPerson.trim());
          if (creator != null)
            authors.add(creator);
        }
      }
      String creator = "";
      String creatorDetails = "<persons>";
      for (int i=0; i<authors.size(); i++) {
        Person author = authors.get(i);
        creator = creator + author.getName();
        if (authors.size() != 1)
          creator = creator + ". ";
        creatorDetails = creatorDetails + "\n" + author.toXmlStr();
      }
      creator = creator.trim();
      creatorDetails = creatorDetails + "\n</persons>";
      if (creator != null && ! creator.isEmpty())
        mdRecord.setCreator(creator);
      if (authors != null && ! authors.isEmpty())
        mdRecord.setCreatorDetails(creatorDetails);
    }
    // title
    Elements titleElems = resourceElem.select("dcterms|title");
    if (! titleElems.isEmpty()) {
      String title = "";
      for (int i=0; i<titleElems.size(); i++) {
        String titleElemStr = titleElems.get(i).text();
        if (titleElems.size() == 1)
          title = titleElemStr;
        else
          title = title + titleElemStr + ". ";
      }
      mdRecord.setTitle(title);
    }
    // alternative title
    Elements alternativeElems = resourceElem.select("dcterms|alternative");
    if (! alternativeElems.isEmpty()) {
      String alternativeTitle = "";
      for (int i=0; i<alternativeElems.size(); i++) {
        String alternativeTitleElemStr = alternativeElems.get(i).text();
        if (titleElems.size() == 1)
          alternativeTitle = alternativeTitleElemStr;
        else
          alternativeTitle = alternativeTitle + alternativeTitleElemStr + ". ";
      }
      mdRecord.setAlternativeTitle(alternativeTitle);
    }
    // publisher
    Elements publisherElems = resourceElem.select("dcterms|publisher");
    if (! publisherElems.isEmpty()) {
      ArrayList<Organization> organizations = new ArrayList<Organization>();
      for (int i=0; i<publisherElems.size(); i++) {
        Element publisherElem = publisherElems.get(i);
        String rdfIdOrganization = publisherElem.attr("rdf:resource");
        if (rdfIdOrganization != null && ! rdfIdOrganization.isEmpty()) {
          Organization organization = ProjectReader.getInstance().getOrganization(rdfIdOrganization.trim());
          if (organization != null)
            organizations.add(organization);
        }
      }
      String publisherStr = "";
      for (int i=0; i<organizations.size(); i++) {
        Organization organization = organizations.get(i);
        publisherStr = publisherStr + organization.getName();
        if (organizations.size() != 1)
          publisherStr = publisherStr + ". ";
      }
      if (publisherStr != null && ! publisherStr.isEmpty())
        mdRecord.setPublisher(publisherStr);
    }
    // dcterms:subject
    Elements dctermSubjectElems = resourceElem.select("dcterms|subject");
    if (! dctermSubjectElems.isEmpty()) {
      String subjectStr = "";
      String subjectControlledDetails = "<subjects>";
      for (int i=0; i<dctermSubjectElems.size(); i++) {
        // e.g.: <dcterms:subject rdf:resource="http://d-nb.info/gnd/4037764-7"/>
        Element dctermSubjectElem = dctermSubjectElems.get(i);
        String rdfIdSubject = dctermSubjectElem.attr("rdf:resource");
        if (rdfIdSubject != null && ! rdfIdSubject.isEmpty()) {
          Subject subject = ProjectReader.getInstance().getSubject(rdfIdSubject.trim());
          if (subject != null) {
            subjectStr = subjectStr + subject.getName() + "###";
          }
        }
        String dctermSubjectElemStr = dctermSubjectElem.outerHtml();
        subjectControlledDetails = subjectControlledDetails + "\n" + dctermSubjectElemStr;
      }
      if (subjectStr.endsWith("###"))
        subjectStr = subjectStr.substring(0, subjectStr.length() - 3);
      subjectControlledDetails = subjectControlledDetails + "\n</subjects>";
      mdRecord.setSubjectControlledDetails(subjectControlledDetails);
      mdRecord.setSubjectControlled(subjectStr);
    }
    // date
    String dateStr = resourceElem.select("dcterms|date").text();
    if (dateStr != null && ! dateStr.isEmpty()) {
      dateStr = new Util().toYearStr(dateStr);  // test if possible etc
      if (dateStr != null) {
        try {
          Date date = new Util().toDate(dateStr + "-01-01T00:00:00.000Z");
          mdRecord.setDate(date);
        } catch (Exception e) {
          // nothing
        }
      }
    }
    // language
    String language = resourceElem.select("dcterms|language").attr("rdf:resource");
    if (language != null && ! language.isEmpty()) {
      int index = language.lastIndexOf("/");
      if (index > 0) 
        language = language.substring(index + 1);
      mdRecord.setLanguage(language);
    }
    // abstract
    String abstractt = resourceElem.select("dcterms|abstract").text();
    if (abstractt != null && ! abstractt.isEmpty()) {
      abstractt = StringUtils.resolveXmlEntities(abstractt.trim());
      mdRecord.setDescription(abstractt);
    }
    // extent
    String extent = resourceElem.select("dcterms|extent").text();
    if (extent != null && ! extent.isEmpty()) {
      mdRecord.setExtent(extent);
    }
    return mdRecord;
  }

  private MetadataRecord getMdRecordFromBibliographicResource(Project project, Database db, Element resourceElem) throws ApplicationException {
    // download url of resource
    Element identifierElem = resourceElem.select("dc|identifier").first();
    String resourceIdUrlStr = null;
    if (identifierElem != null) {
      resourceIdUrlStr = identifierElem.attr("rdf:resource"); 
      if (resourceIdUrlStr == null || resourceIdUrlStr.trim().isEmpty()) {
        resourceIdUrlStr = resourceElem.attr("rdf:about");
        if (resourceIdUrlStr == null || resourceIdUrlStr.trim().isEmpty())
          LOGGER.error("No identifier given in resource: \"" + resourceElem + "\"");
      }
    }
    MetadataRecord mdRecord = getNewMdRecord(resourceIdUrlStr);
    // web url of resource
    String resourceIdentifierStr = null;
    if (identifierElem != null) {
      resourceIdentifierStr = identifierElem.text();
      if (resourceIdentifierStr != null && ! resourceIdentifierStr.isEmpty()) {
        try {
          new URL(resourceIdentifierStr); // test if identifier is a url, if so then set it as the webUri
          mdRecord.setWebUri(resourceIdentifierStr);
        } catch (MalformedURLException e) {
          // nothing
        }
        mdRecord.setUri(resourceIdUrlStr);
      }
    }
    // edoc
    boolean isEdocDB = false;
    if (db != null) {
      String dbRdfId = db.getRdfId();
      if (dbRdfId.endsWith("edoc.bbaw.de/oai"))
        isEdocDB = true;
    }
    if (isEdocDB) {
      String projectRdfId = resourceElem.select("dcterms|isPartOf").attr("rdf:resource");
      if (projectRdfId != null) {
        Project p = ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
        if (p != null) {
          String projectId = p.getId();
          mdRecord.setProjectId(projectId);
          mdRecord.setProjectRdfId(projectRdfId);
        }
      }
      // collectionRdfId of which this resource is part of
      String collectionRdfId = resourceElem.select("ore|isAggregatedBy").attr("rdf:resource");
      if (collectionRdfId != null && ! collectionRdfId.isEmpty()) {
        ProjectCollection rootCollection = ProjectReader.getInstance().getRootCollection(collectionRdfId); // hack: resources are set to root collection
        String rootCollectionRdfId = rootCollection.getRdfId(); 
        mdRecord.setCollectionRdfId(rootCollectionRdfId);
      }
    }
    // systemType
    String type = resourceElem.select("dc|type").text();
    if (type != null && ! type.isEmpty())
      mdRecord.setSystem(type);  // e.g. "crawl", "eXist" or "dbRecord" (for dbType: mysql, postgres and for dbName jdg) or "oaiRecord" (for oai databases: dta, edoc)  
    // creators
    Elements authorElems = resourceElem.select("dc|creator");
    if (! authorElems.isEmpty()) {
      ArrayList<Person> authors = new ArrayList<Person>();
      for (int i=0; i<authorElems.size(); i++) {
        Element authorElem = authorElems.get(i);
        String name = authorElem.text();
        if (name != null && ! name.isEmpty()) {
          name = name.replaceAll("\n|\\s\\s+", " ").trim();
          Person author = new Person(name);
          authors.add(author);
        } 
        String resourceId = authorElem.attr("rdf:resource");
        if (resourceId != null && ! resourceId.isEmpty()) {
          Person creator = ProjectReader.getInstance().getPerson(resourceId.trim());
          if (creator != null)
            authors.add(creator);
        }
      }
      String creator = "";
      String creatorDetails = "<persons>";
      for (int i=0; i<authors.size(); i++) {
        Person author = authors.get(i);
        creator = creator + author.getName();
        if (authors.size() != 1)
          creator = creator + ". ";
        creatorDetails = creatorDetails + "\n" + author.toXmlStr();
      }
      creator = creator.trim();
      creatorDetails = creatorDetails + "\n</persons>";
      if (creator != null && ! creator.isEmpty())
        mdRecord.setCreator(creator);
      if (authors != null && ! authors.isEmpty())
        mdRecord.setCreatorDetails(creatorDetails);
    }
    // title
    Elements titleElems = resourceElem.select("dc|title, dcterms|alternative");
    if (! titleElems.isEmpty()) {
      String title = "";
      for (int i=0; i<titleElems.size(); i++) {
        String titleElemStr = titleElems.get(i).text();
        if (titleElems.size() == 1)
          title = titleElemStr;
        else
          title = title + titleElemStr + ". ";
      }
      mdRecord.setTitle(title);
    }
    // publisher
    String publisher = resourceElem.select("dc|publisher").text();
    if (publisher != null && ! publisher.isEmpty())
      mdRecord.setPublisher(publisher);
    // dc:subject
    Elements subjectElems = resourceElem.select("dc|subject");
    String subject = textValueJoined(subjectElems, "###");
    if (subject != null)
      mdRecord.setSubject(subject);
    // swd
    Elements swdElems = resourceElem.select("dc|subject[xsi:type=\"SWD\"]");
    String subjectSwd = textValueJoined(swdElems, ",");
    if (subjectSwd != null) {
      mdRecord.setSwd(subjectSwd);
    }
    // ddc
    Elements ddcElems = resourceElem.select("dc|subject[xsi:type=\"dcterms:DDC\"]");
    String subjectDdc = textValueJoined(ddcElems, ",");
    if (subjectDdc != null) {
      mdRecord.setDdc(subjectDdc);
    }
    // dcterms:subject
    Elements dctermSubjectElems = resourceElem.select("dcterms|subject");
    if (! dctermSubjectElems.isEmpty()) {
      String subjectControlledDetails = "<subjects>";
      for (int i=0; i<dctermSubjectElems.size(); i++) {
        /* e.g.:
         * <dcterms:subject>
             <rdf:Description rdf:about="http://de.dbpedia.org/resource/Kategorie:Karl_Marx">
               <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#Concept"/>
               <rdfs:label>Karl Marx</rdfs:label>
             </rdf:description>
           </dcterms:subject>
         */
        Element dctermSubjectElem = dctermSubjectElems.get(i);
        String dctermSubjectElemStr = dctermSubjectElem.outerHtml();
        subjectControlledDetails = subjectControlledDetails + "\n" + dctermSubjectElemStr;
      }
      subjectControlledDetails = subjectControlledDetails + "\n</subjects>";
      mdRecord.setSubjectControlledDetails(subjectControlledDetails);
    }
    // date
    String dateStr = resourceElem.select("dc|date").text();
    if (dateStr != null && ! dateStr.isEmpty()) {
      dateStr = new Util().toYearStr(dateStr);  // test if possible etc
      if (dateStr != null) {
        try {
          Date date = new Util().toDate(dateStr + "-01-01T00:00:00.000Z");
          mdRecord.setDate(date);
        } catch (Exception e) {
          // nothing
        }
      }
    }
    // language
    String language = resourceElem.select("dc|language").text();
    if (language != null && ! language.isEmpty())
      mdRecord.setLanguage(language);
    // mime type
    Elements mimeTypeElems = resourceElem.select("dc|format");
    if (mimeTypeElems != null && mimeTypeElems.size() > 0) {
      String mimeType = mimeTypeElems.first().text();
      if (mimeType != null && ! mimeType.isEmpty())
        mdRecord.setType(mimeType);
    }
    // abstract
    String abstractt = resourceElem.select("dc|abstract").text();
    if (abstractt != null && ! abstractt.isEmpty()) {
      abstractt = StringUtils.resolveXmlEntities(abstractt.trim());
      mdRecord.setDescription(abstractt);
    }
    // page count
    String pagesStr = resourceElem.select("dc|extent").text();
    if (pagesStr != null && ! pagesStr.isEmpty()) {
      try {
        int pages = Integer.parseInt(pagesStr);
        mdRecord.setPageCount(pages);
      } catch (NumberFormatException e) {
        // nothing
      }
    }
    return mdRecord;
  }
  
  private String textValueJoined(Elements elems, String splitStr) {
    String retValue = "";
    for (int i=0; i<elems.size(); i++) {
      String elemText = elems.get(i).text().trim();
      if (! elemText.isEmpty())
        retValue = retValue + elemText + splitStr;
    }
    if (retValue.length() >= splitStr.length())
      retValue = retValue.substring(0, retValue.length() - splitStr.length());
    if (retValue.isEmpty())
      return null;
    else 
      return retValue;
  }
  
  private MetadataRecord getNewMdRecord(String urlStr) {
    MetadataRecord mdRecord = new MetadataRecord();
    try {
      URL resourceIdUrl = new URL(urlStr);
      String urlFilePath = resourceIdUrl.getFile();
      urlFilePath = StringUtils.deresolveXmlEntities(urlFilePath.trim());
      if (urlFilePath.startsWith("/"))
        urlFilePath = urlFilePath.substring(1);
      String docId = normalize(urlFilePath);
      mdRecord.setDocId(docId);
      mdRecord.setWebUri(urlStr);
    } catch (MalformedURLException e) {
      LOGGER.error("Malformed Url \"" +  urlStr + "\" in resource");
      return null;
    }
    return mdRecord;
  }
  
  public MetadataRecord getMetadataRecord(File xmlFile, String schemaName, MetadataRecord mdRecord, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    if (schemaName == null)
      return mdRecord;
    try {
      URL srcUrl = xmlFile.toURI().toURL();
      if (schemaName.equals("TEI"))
        mdRecord = getMetadataRecordTei(xQueryEvaluator, srcUrl, mdRecord);
      else if (schemaName.equals("mets"))
        mdRecord = getMetadataRecordMets(xQueryEvaluator, srcUrl, mdRecord);
      else
        mdRecord.setSchemaName(schemaName); // all other cases: set docType to schemaName
      evaluateXQueries(xQueryEvaluator, srcUrl, mdRecord);
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecord;
  }

  private MetadataRecord evaluateXQueries(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) {
    Hashtable<String, XQuery> xqueriesHashtable = mdRecord.getxQueries();
    if (xqueriesHashtable != null) {
      Enumeration<String> keys = xqueriesHashtable.keys();
      while (keys != null && keys.hasMoreElements()) {
        String key = keys.nextElement();
        XQuery xQuery = xqueriesHashtable.get(key);
        String xQueryCode = xQuery.getCode();
        try {
          String xQueryResult = xQueryEvaluator.evaluateAsString(srcUrl, xQueryCode);
          if (xQueryResult != null)
            xQueryResult = xQueryResult.trim();
          xQuery.setResult(xQueryResult);
          if (key.equals("webId")) {
            handleXQueryWebId(xQuery, mdRecord);
          }
        } catch (Exception e) {
          // nothing
        }
      }
    }
    return mdRecord;
  }
  
  private void handleXQueryWebId(XQuery xQueryWebId, MetadataRecord mdRecord) {
    String mimeType = mdRecord.getType();
    if (mimeType == null || ! mimeType.contains("xml"))
      return;  // do nothing if it is not an xml document
    String webId = xQueryWebId.getResult();
    if (webId != null)
      webId = StringUtils.resolveXmlEntities(webId);
    // Hack: if xQuery code is "fileName"
    boolean codeIsFileName = false;
    if (xQueryWebId.getCode() != null && xQueryWebId.getCode().equals("xxxFileName"))
      codeIsFileName = true;
    if (codeIsFileName) {
      String docId = mdRecord.getDocId();
      int from = docId.lastIndexOf("/");
      String fileName = docId.substring(from + 1);
      webId = fileName;
    }
    if (webId != null) {
      webId = webId.trim();
      if (! webId.isEmpty()) {
        String webUri = null;
        String xQueryPreStr = xQueryWebId.getPreStr();
        if (xQueryPreStr != null)
          webUri = xQueryPreStr + webId;
        String xQueryAfterStr = xQueryWebId.getAfterStr();
        if (xQueryAfterStr != null)
          webUri = webUri + xQueryAfterStr;
        if (xQueryPreStr == null && xQueryAfterStr == null)
          webUri = webId;
        mdRecord.setWebUri(webUri);
      }
    }
  }
  
  private MetadataRecord getMetadataRecordTei(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/*:TEI/*:teiHeader");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:idno");
      if (identifier != null)
        identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      String creator = null;
      String creatorDetails = null;
      XdmValue xmdValueAuthors = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:author");
      if (xmdValueAuthors != null && xmdValueAuthors.size() > 0) {
        XdmSequenceIterator xmdValueAuthorsIterator = xmdValueAuthors.iterator();
        ArrayList<Person> authors = new ArrayList<Person>();
        while (xmdValueAuthorsIterator.hasNext()) {
          Person author = new Person();
          XdmItem xdmItemAuthor = xmdValueAuthorsIterator.next();
          String xdmItemAuthorStr = xdmItemAuthor.toString();
          String name = xdmItemAuthor.getStringValue();
          boolean isProper = isProper("author", name);
          if (! isProper)
            name = "";
          if (name != null) {
            name = name.replaceAll("\n|\\s\\s+", " ").trim();
          }
          String reference = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:author/*:persName/@ref)");
          if (reference != null && ! reference.isEmpty())
            author.setRef(reference);
          String forename = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:author/*:persName/*:forename)");
          if (forename != null && ! forename.isEmpty())
            author.setForename(forename.trim());
          String surname = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:author/*:persName/*:surname)");
          if (surname != null && ! surname.isEmpty())
            author.setSurname(surname.trim());
          if (surname != null && ! surname.isEmpty() && forename != null && ! forename.isEmpty())
            name = surname.trim() + ", " + forename.trim();
          else if (surname != null && ! surname.isEmpty() && (forename == null || forename.isEmpty()))
            name = surname.trim();
          if (! name.isEmpty()) {
            author.setName(name);
            authors.add(author);
          }
        }
        creator = "";
        creatorDetails = "<persons>";
        for (int i=0; i<authors.size(); i++) {
          Person author = authors.get(i);
          creator = creator + author.getName();
          if (authors.size() != 1)
            creator = creator + ". ";
          creatorDetails = creatorDetails + "\n" + author.toXmlStr();
        }
        creator = StringUtils.deresolveXmlEntities(creator.trim());
        creatorDetails = creatorDetails + "\n</persons>";
        if (creator.isEmpty())
          creator = null;
        if (authors.isEmpty())
          creatorDetails = null;
      }
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title");
      XdmValue xdmValueTitleIdno = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title/*:idno"); // e.g. in http://telotadev.bbaw.de:8078/exist/rest/db/WvH/Briefe/1827-02-20_RosenFriedrichAugust-WvH_481.xml
      if (xdmValueTitleIdno != null && xdmValueTitleIdno.size() > 0)
        title = xQueryEvaluator.evaluateAsString(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title/text()");
      XdmValue xdmValueTitleMain = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title[@type='main']");
      XdmValue xdmValueTitleShort = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title[@type='short']");
      if (xdmValueTitleShort != null && xdmValueTitleShort.size() > 0) {
        try {
          title = xdmValueTitleShort.getUnderlyingValue().getStringValue();
        } catch (XPathException e) {
          // nothing
        }
      } else if (xdmValueTitleMain != null && xdmValueTitleMain.size() > 0) {
        try {
          title = xdmValueTitleMain.getUnderlyingValue().getStringValue();
        } catch (XPathException e) {
          // nothing
        }
      }
      if (title != null) {
        title = StringUtils.deresolveXmlEntities(title.trim());
        if (title.isEmpty())
          title = null;
      }
      String language = null;
      String countLanguages = xQueryEvaluator.evaluateAsString(metadataXmlStr, "string(count(/*:teiHeader/*:profileDesc/*:langUsage/*:language))");
      if (countLanguages != null && countLanguages.equals("1"))
        language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/*:teiHeader/*:profileDesc/*:langUsage/*:language[1]/@ident)");
      if (language != null && language.isEmpty())
        language = null;
      if (language != null) {
        language = StringUtils.deresolveXmlEntities(language.trim());
        language = Language.getInstance().getISO639Code(language);
      }
      String publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:publisher/*:name", ", ");
      if (publisher == null || publisher.isEmpty())
        publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:publisher", ", ");
      if (publisher == null || publisher.isEmpty())
        publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:publisher", ", ");
      if (publisher == null || publisher.isEmpty())
        publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:bibl/*:publisher", ", ");
      if (publisher != null) {
        publisher = StringUtils.deresolveXmlEntities(publisher.trim());
        if (publisher.isEmpty())
          publisher = null;
      }
      String place = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:pubPlace", ", ");
      if (place == null || place.isEmpty())
        place = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:pubPlace");
      if (place != null) {
        place = StringUtils.deresolveXmlEntities(place.trim());
        if (place.isEmpty())
          place = null;
      }
      String publisherStr = null;
      boolean publisherEndsWithComma = false;
      if (publisher != null)
        publisherEndsWithComma = publisher.lastIndexOf(",") == publisher.length() - 1;
      if (publisher == null && place != null)
        publisherStr = place;
      else if (publisher != null && place == null)
        publisherStr = publisher;
      else if (publisher != null && place != null && publisherEndsWithComma)
        publisherStr = publisher + " " + place;
      else if (publisher != null && place != null && ! publisherEndsWithComma)
        publisherStr = publisher + ", " + place;
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:date");
      if (yearStr == null || yearStr.isEmpty())
        yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:date");
      if (yearStr == null || yearStr.isEmpty())
        yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:bibl/*:date", ", ");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr.trim());
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null) {
          try {
            date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          } catch (Exception e) {
            // nothing
          }
        }
      }
      String subject = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:profileDesc/*:textClass/*:keywords/*:term");
      if (subject != null) {
        subject = StringUtils.deresolveXmlEntities(subject.trim());
      }
      String rights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:availability");
      if (rights == null || rights.trim().isEmpty())
        rights = "open access";
      rights = StringUtils.deresolveXmlEntities(rights.trim());
      String license = "http://echo.mpiwg-berlin.mpg.de/policy/oa_basics/declaration";
      String accessRights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/*:teiHeader/*:fileDesc/*:publicationStmt/*:availability/@status)");
      if (accessRights == null || accessRights.trim().isEmpty()) 
        accessRights = "free";
      accessRights = StringUtils.deresolveXmlEntities(accessRights.trim());
      mdRecord.setIdentifier(identifier);
      // set language onto mdRecord if it is not null else the mdReord language beware its language 
      if (language != null)
        mdRecord.setLanguage(language);
      if (mdRecord.getCreator() == null)
        mdRecord.setCreator(creator);
      if (mdRecord.getCreatorDetails() == null)
        mdRecord.setCreatorDetails(creatorDetails);
      if (mdRecord.getTitle() == null)
        mdRecord.setTitle(title);
      if (mdRecord.getPublisher() == null)
        mdRecord.setPublisher(publisherStr);
      if (mdRecord.getRights() == null)
        mdRecord.setRights(rights);
      if (mdRecord.getDate() == null)
        mdRecord.setDate(date);
      if (mdRecord.getSubject() == null)
        mdRecord.setSubject(subject);
      if (mdRecord.getLicense() == null)
        mdRecord.setLicense(license);
      if (mdRecord.getAccessRights() == null)
        mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//*:pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("TEI");
    return mdRecord;
  }

  private MetadataRecord getMetadataRecordMets(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStrDmd = xQueryEvaluator.evaluateAsString(srcUrl, "/*:mets/*:dmdSec");
    String metadataXmlStrAmd = xQueryEvaluator.evaluateAsString(srcUrl, "/*:mets/*:amdSec");
    if (metadataXmlStrDmd != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "string(/*:dmdSec/@ID)");
      if (identifier != null)
        identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      String webUri = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:digiprovMD/*:mdWrap/*:xmlData/*:links/*:presentation");
      if (webUri != null)
        webUri = StringUtils.deresolveXmlEntities(webUri.trim());
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:name");
      if (creator != null) {
        creator = StringUtils.deresolveXmlEntities(creator.trim());
        if (creator.isEmpty())
          creator = null;
      }
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:titleInfo");
      if (title != null)
        title = StringUtils.deresolveXmlEntities(title.trim());
      String language = null;  // TODO
      // String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "string(/*:teiHeader/*:profileDesc/*:langUsage/*:language[1]/@ident)");
      // if (language != null && language.isEmpty())
      //   language = null;
      // if (language != null) {
      //   language = StringUtils.deresolveXmlEntities(language.trim());
      //   language = Language.getInstance().getISO639Code(language);
      // }
      String publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:rightsMD/*:mdWrap/*:xmlData/*:rights/*:owner", ", ");
      if (publisher != null)
        publisher = StringUtils.deresolveXmlEntities(publisher.trim());
      String place = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:originInfo/*:place/*:placeTerm", ", ");
      if (place != null)
        place = StringUtils.deresolveXmlEntities(place.trim());
      String publisherStr = null;
      boolean publisherEndsWithComma = false;
      if (publisher != null)
        publisherEndsWithComma = publisher.lastIndexOf(",") == publisher.length() - 1;
      if (publisher == null && place != null)
        publisherStr = place;
      else if (publisher != null && place == null)
        publisherStr = publisher;
      else if (publisher != null && place != null && publisherEndsWithComma)
        publisherStr = publisher + " " + place;
      else if (publisher != null && place != null && ! publisherEndsWithComma)
        publisherStr = publisher + ", " + place;
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:originInfo/*:dateIssued");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr.trim());
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null) {
          try {
            date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          } catch (Exception e) {
            // nothing
          }
        }
      }
      String subject = null;  // TODO
      // String subject = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:teiHeader/*:profileDesc/*:textClass/*:keywords/*:term");
      // if (subject != null)
      //   subject = StringUtils.deresolveXmlEntities(subject.trim());
      String rightsOwner = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:rightsMD/*:mdWrap/*:xmlData/*:rights/*:owner");
      String rightsOwnerUrl = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:rightsMD/*:mdWrap/*:xmlData/*:rights/*:ownerSiteURL");
      String rights = null;
      if (rightsOwner != null)
        rights = StringUtils.deresolveXmlEntities(rightsOwner.trim());
      if (rightsOwner != null && rightsOwnerUrl != null)
        rights = rights + ", " + StringUtils.deresolveXmlEntities(rightsOwnerUrl.trim());
      else if (rightsOwner == null && rightsOwnerUrl != null)
        rights = StringUtils.deresolveXmlEntities(rightsOwnerUrl.trim());
      mdRecord.setIdentifier(identifier);
      mdRecord.setWebUri(webUri);
      mdRecord.setLanguage(language);
      if (mdRecord.getCreator() == null)
        mdRecord.setCreator(creator);
      if (mdRecord.getTitle() == null)
        mdRecord.setTitle(title);
      if (mdRecord.getPublisher() == null)
        mdRecord.setPublisher(publisherStr);
      if (mdRecord.getRights() == null)
        mdRecord.setRights(rights);
      if (mdRecord.getDate() == null)
        mdRecord.setDate(date);
      if (mdRecord.getSubject() == null)
        mdRecord.setSubject(subject);
    }
    mdRecord.setSchemaName("mets");
    return mdRecord;
  }

  public MetadataRecord getMetadataRecordHtml(Document htmlDoc, MetadataRecord mdRecord) throws ApplicationException {
    String title = htmlDoc.select("head > title").text();
    if (! title.isEmpty())
      mdRecord.setTitle(title);
    Elements docMetaElems = htmlDoc.select("head > meta");
    if (docMetaElems != null) {
      for (int i=0; i< docMetaElems.size(); i++) {
        Element docMetaElem = docMetaElems.get(i);
        String docMetaName = docMetaElem.attr("name");
        String docMetaValue = docMetaElem.attr("content");
        if (docMetaName != null && ! docMetaName.isEmpty() && docMetaValue != null && ! docMetaValue.isEmpty()) {
          docMetaName = docMetaName.trim().toLowerCase();
          docMetaValue = docMetaValue.trim();
          if (docMetaName.equals("keywords") || docMetaName.equals("dc.subject")) {
            mdRecord.setSubject(docMetaValue);
          } else if (docMetaName.equals("description") || docMetaName.equals("dc.description")) {
            mdRecord.setDescription(docMetaValue);
          } else if (docMetaName.equals("language") || docMetaName.equals("dc.language")) {
            String isoLang = Language.getInstance().getISO639Code(docMetaValue.toLowerCase());
            if (isoLang != null && mdRecord.getLanguage() != null)
              mdRecord.setLanguage(isoLang);
          } else if (docMetaName.equals("dc.creator")) {
            String creator = docMetaValue;
            boolean isProper = isProper("author", creator);
            if (isProper)
              mdRecord.setCreator(creator);
          } else if (docMetaName.equals("dc.contributor")) {
            mdRecord.setContributor(docMetaValue);
          } else if (docMetaName.equals("dc.title")) {
            mdRecord.setTitle(docMetaValue);
          } else if (docMetaName.equals("dc.publisher")) {
            mdRecord.setPublisher(docMetaValue);
          } else if (docMetaName.equals("dc.coverage")) {
            mdRecord.setCoverage(docMetaValue);
          } else if (docMetaName.equals("dc.date") || docMetaName.equals("dc.date.created") || docMetaName.equals("dc.date.modified")) {
            try {
              Date date = new Util().toDate(docMetaValue);
              mdRecord.setDate(date);
            } catch (Exception e) {
              // nothing
            }
          } else if (docMetaName.equals("dc.rights")) {
            mdRecord.setRights(docMetaValue);
          } else if (docMetaName.equals("dc.accessRights")) {
            mdRecord.setAccessRights(docMetaValue);
          } else if (docMetaName.equals("dc.license")) {
            mdRecord.setLicense(docMetaValue);
          }
        }
      }
    }
    return mdRecord;
  }
  
  public boolean isProper(String fieldName, String fieldValue) {
    if (fieldValue == null)
      return true;
    boolean isProper = true;
    if (fieldName != null && fieldName.equals("author") && (fieldValue.equals("admin") || fieldValue.equals("user")))
      return false;
    return isProper;
  }

  /**
   * Normalizes an identifier (file path elements are shortened to maximal of 255 length)
   * @param urlFilePath
   * @return the normalized string
   */
  private String normalize(String urlFilePath) {
    String retStr = urlFilePath;
    if (urlFilePath != null && urlFilePath.contains("/") && urlFilePath.length() > 255) {
      retStr = "";
      String[] pathElems = urlFilePath.split("/");
      for (int i=0; i<pathElems.length; i++) {
        String pathElem = pathElems[i];
        if (pathElem.length() > 255)
          pathElem = pathElem.substring(0, 255);
        retStr = retStr + pathElem + "/";
      }
      retStr = retStr.substring(0, retStr.length() - 1); // without last "/"
    }
    return retStr;
  }

  private String performGetRequest(String urlStr) throws ApplicationException {
    String resultStr = null;
    int statusCode = -1;
    try {
      GetMethod method = new GetMethod(urlStr);
      statusCode = httpClient.executeMethod(method);
      if (statusCode < 400) {
        InputStream resultIS = method.getResponseBodyAsStream();
        byte[] resultBytes = getBytes(resultIS);
        resultStr = new String(resultBytes, "utf-8");
      }
      method.releaseConnection();
    } catch (HttpException e) {
      // nothing
    } catch (SocketTimeoutException e) {
      LOGGER.error("XXXErrorXXX" + "Url: \"" + urlStr + "\" has socket timeout after " + SOCKET_TIMEOUT + " ms (" + e.bytesTransferred + " bytes transferred)");
    } catch (IOException e) {
      // nothing
    }
    return resultStr;
  }

  private byte[] getBytes(InputStream is) throws IOException {
    int len;
    int size = 1024;
    byte[] buf;
    if (is instanceof ByteArrayInputStream) {
      size = is.available();
      buf = new byte[size];
      len = is.read(buf, 0, size);
    } else {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      buf = new byte[size];
      while ((len = is.read(buf, 0, size)) != -1)
        bos.write(buf, 0, len);
      buf = bos.toByteArray();
    }
    return buf;
  }

}
