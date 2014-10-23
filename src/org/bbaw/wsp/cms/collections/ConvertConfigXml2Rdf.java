package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.transform.XslResourceTransformer;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class ConvertConfigXml2Rdf {
  private static Logger LOGGER = Logger.getLogger(ConvertConfigXml2Rdf.class);
  private CollectionReader collectionReader;
  private static int SOCKET_TIMEOUT = 10 * 1000;
  private HttpClient httpClient; 
  private String resourcesDirName;
  private String dbResourcesDirName;
  private String externalResourcesDirName;
  private File inputNormdataFile;
  private XQueryEvaluator xQueryEvaluator;
  public static void main(String[] args) throws ApplicationException {
    try {
      ConvertConfigXml2Rdf convertConfigXml2Rdf = new ConvertConfigXml2Rdf();
      convertConfigXml2Rdf.init();
      // convertConfigXml2Rdf.convertAll();
      // convertConfigXml2Rdf.proofRdfProjects();
      // convertConfigXml2Rdf.proofCollectionProjects();
      // Collection c = convertConfigXml2Rdf.collectionReader.getCollection("pdr");
      // Collection c = convertConfigXml2Rdf.collectionReader.getCollection("jdg");
      // Collection c = convertConfigXml2Rdf.collectionReader.getCollection("pmbz");
      // convertConfigXml2Rdf.convert(c, false);
      // convertConfigXml2Rdf.convertDbXmlFiles(c);
      // convertConfigXml2Rdf.generateDbXmlDumpFiles(c);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    xQueryEvaluator = new XQueryEvaluator();
    httpClient = new HttpClient();
    // timeout seems to work
    httpClient.getParams().setParameter("http.socket.timeout", SOCKET_TIMEOUT);
    httpClient.getParams().setParameter("http.connection.timeout", SOCKET_TIMEOUT);
    httpClient.getParams().setParameter("http.connection-manager.timeout", new Long(SOCKET_TIMEOUT));
    httpClient.getParams().setParameter("http.protocol.head-body-timeout", SOCKET_TIMEOUT);
    resourcesDirName = Constants.getInstance().getMdsystemConfDir() +  "/resources";
    dbResourcesDirName = Constants.getInstance().getExternalDocumentsDir() + "/db-resources";
    externalResourcesDirName = Constants.getInstance().getExternalDocumentsDir() + "/resources";
    String inputNormdataFileName = Constants.getInstance().getMdsystemNormdataFile();
    inputNormdataFile = new File(inputNormdataFileName);      
  }
  
  private void generateDbXmlDumpFiles(Collection collection) throws ApplicationException {
    ArrayList<Database> collectionDBs = collection.getDatabases();
    String collectionId = collection.getId();
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database db = collectionDBs.get(i);
        String dbType = db.getType();
        JdbcConnection jdbcConn = db.getJdbcConnection();
        LOGGER.info("Generate database dump files (Collection: " + collection.getId() + ") of database: \"" + db.getName() + "\"");
        if (dbType != null && (dbType.equals("oai") || dbType.equals("oai-dbrecord"))) {
          generateOaiDbXmlDumpFiles(collection, db);
        } else if (dbType != null && dbType.equals("dwb")) {
          // special case for dwb
          generateDwbFiles(collection, db);
        } else if (collectionId.equals("dtmh")) {
          // special case for dtmh
          generateDtmhFiles(collection, db);
        } else if (collectionId.equals("pmbz")) {
          // special case for dtmh
          generatePmbzFiles(collection, db);
        } else if (collectionId.equals("aaewtla")) {
          // special case for aaewtla
          generateAaewtlaFiles(collection, db);
        } else if (collectionId.equals("coranicum")) {
          // special case for coranicum: by jdbc
          generateCoranicumDbXmlDumpFile(collection, db);
        } else if (collectionId.equals("pdr")) {
          // special case for pdr: by special pdr services
          generatePdrDbXmlDumpFile(collection, db);
        } else if (dbType != null && jdbcConn != null) {
          // generate db file by jdbc
          generateDbXmlDumpFile(collection, db);
        }
      }
    }
  }
  
  private void generateDbXmlDumpFile(Collection collection, Database db) throws ApplicationException {
    File dbResourcesDir = new File(dbResourcesDirName);
    String xmlDumpFileFilter = collection.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter fileFilter = new WildcardFileFilter(xmlDumpFileFilter);
    File[] files = dbResourcesDir.listFiles(fileFilter);
    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
        File dumpFileToDelete = files[i];
        FileUtils.deleteQuietly(dumpFileToDelete);
      }
      LOGGER.info("Database dump files of database \"" + db.getName() + "\" sucessfully deleted");
    }
    StringBuilder xmlDumpStrBuilder = new StringBuilder();
    xmlDumpStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    xmlDumpStrBuilder.append("<!-- Resources of database: " + db.getName() + " (Collection: " + collection.getId() + ") -->\n");
    JdbcConnection jdbcConn = db.getJdbcConnection();
    xmlDumpStrBuilder.append("<" + jdbcConn.getDb() + ">\n");
    Properties connectionProps = new Properties();
    connectionProps.put("user", jdbcConn.getUser());
    connectionProps.put("password", jdbcConn.getPw());
    Connection conn = null;
    try {
      String dbType = db.getType();
      if (dbType.equals("mysql")) {
        Class.forName("com.mysql.jdbc.Driver");
      } else {
        return;  // TODO postgres etc.
      }
      String jdbcUrl = "jdbc:" + dbType + "://" + jdbcConn.getHost() + ":" + jdbcConn.getPort() + "/" + jdbcConn.getDb();  // + "?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8_general_ci&characterSetResults=utf8"
      conn = DriverManager.getConnection(jdbcUrl, connectionProps);
      PreparedStatement initPS = conn.prepareStatement("SET group_concat_max_len = 999999");
      initPS.execute();
      initPS.close();
      String select = db.getSql();
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
      String xmlDumpFileName = dbResourcesDirName + "/" + collection.getId() + "-" + db.getName() + "-1" + ".xml";
      File dumpFile = new File(xmlDumpFileName);
      FileUtils.writeStringToFile(dumpFile, xmlDumpStrBuilder.toString(), "utf-8");
      LOGGER.info("Database dump file \"" + xmlDumpFileName + "\" sucessfully created");
    } catch (Exception e) {
      try {
        conn.close();
      } catch (Exception e2) {
        // nothing
      };
      throw new ApplicationException(e);
    }
  }
  
  private void generateCoranicumDbXmlDumpFile(Collection collection, Database db) throws ApplicationException {
    File dbResourcesDir = new File(dbResourcesDirName);
    String xmlDumpFileFilter = collection.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter fileFilter = new WildcardFileFilter(xmlDumpFileFilter);
    File[] files = dbResourcesDir.listFiles(fileFilter);
    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
        File dumpFileToDelete = files[i];
        FileUtils.deleteQuietly(dumpFileToDelete);
      }
      LOGGER.info("Database dump files of database \"" + db.getName() + "\" sucessfully deleted");
    }
    StringBuilder xmlDumpStrBuilder = new StringBuilder();
    xmlDumpStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    xmlDumpStrBuilder.append("<!-- Resources of database: " + db.getName() + " (Collection: " + collection.getId() + ") -->\n");
    JdbcConnection jdbcConn = db.getJdbcConnection();
    xmlDumpStrBuilder.append("<" + jdbcConn.getDb() + ">\n");
    Properties connectionProps = new Properties();
    connectionProps.put("user", jdbcConn.getUser());
    connectionProps.put("password", jdbcConn.getPw());
    Connection conn = null;
    try {
      String dbType = db.getType();
      if (dbType.equals("mysql")) {
        Class.forName("com.mysql.jdbc.Driver");
      } else {
        return;  // TODO postgres etc.
      }
      String jdbcUrl = "jdbc:" + dbType + "://" + jdbcConn.getHost() + ":" + jdbcConn.getPort() + "/" + jdbcConn.getDb();  // + "?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8_general_ci&characterSetResults=utf8"
      conn = DriverManager.getConnection(jdbcUrl, connectionProps);
      PreparedStatement initPS = conn.prepareStatement("SET group_concat_max_len = 999999");
      initPS.execute();
      initPS.close();
      String selectKoranVerses = 
          "SELECT\n" +
          " k.sure, k.vers, k.wort, k.transkription, k.arab, koran.surenname\n" +
          "FROM\n" +
          " lc_kkoran k, koran\n" +
          "WHERE\n" +
          " koran.sure = k.sure\n" +
          "AND\n" +
          " koran.vers = 1\n" +
          // "AND\n" +
          // " (k.sure = 3)\n" +
          // "AND\n" +
          // " (k.vers = 120 OR k.vers = 121)\n" +
          "ORDER BY sure, vers, wort";
      Hashtable<String, String> koranVersesGerman = getCoranicumAllKoranVersesGerman(conn);
      PreparedStatement psKoranVerses = conn.prepareStatement(selectKoranVerses);
      ResultSet rsKoranVerses = psKoranVerses.executeQuery();
      String versKey = null;
      String versTitle = null;
      String versTranskriptionStr = "";
      String versArabStr = "";
      while (rsKoranVerses.next()) {
        String sure = rsKoranVerses.getString("sure");
        String vers = rsKoranVerses.getString("vers");
        String transkription = rsKoranVerses.getString("transkription");
        String arab = rsKoranVerses.getString("arab");
        String surenname = rsKoranVerses.getString("surenname");
        String versKeyTmp = "sure/" + sure + "/vers/" + vers;
        String versTitleTmp = "Sure " + sure + " (" + surenname + "), Vers " + vers;
        if (versKey == null) {
          versKey = versKeyTmp;
          versTitle = versTitleTmp;
        }
        if (versKey.equals(versKeyTmp)) {
          versTranskriptionStr = versTranskriptionStr + " " + transkription;
          versArabStr = versArabStr + " " + arab;
        } else if (versKey != null) {
          String koranVersXmlStr = getCoranicumKoranVersXmlStr(db, koranVersesGerman, versKey, versTitle, versTranskriptionStr, versArabStr);
          xmlDumpStrBuilder.append(koranVersXmlStr);
          versKey = versKeyTmp;
          versTitle = versTitleTmp;
          versTranskriptionStr = "";
          versArabStr = "";
        }
      }
      String koranVersXmlStr = getCoranicumKoranVersXmlStr(db, koranVersesGerman, versKey, versTitle, versTranskriptionStr, versArabStr);
      xmlDumpStrBuilder.append(koranVersXmlStr);
      psKoranVerses.close();
      conn.close();
      xmlDumpStrBuilder.append("</" + jdbcConn.getDb() + ">\n");
      String xmlDumpFileName = dbResourcesDirName + "/" + collection.getId() + "-" + db.getName() + "-1" + ".xml";
      File dumpFile = new File(xmlDumpFileName);
      FileUtils.writeStringToFile(dumpFile, xmlDumpStrBuilder.toString(), "utf-8");
      LOGGER.info("Database dump file \"" + xmlDumpFileName + "\" sucessfully created");
    } catch (Exception e) {
      try {
        conn.close();
      } catch (Exception e2) {
        // nothing
      };
      throw new ApplicationException(e);
    }
  }
  
  private String getCoranicumKoranVersXmlStr(Database db, Hashtable<String, String> koranVersesGerman, String id, String title, String transkription, String arab) {
    StringBuilder versXmlStrBuilder = new StringBuilder();
    String abstractStr = "Transkription: " + transkription + "\nArabische Übersetzung: " + arab;
    String versGermanStr = koranVersesGerman.get(id);
    if (versGermanStr != null) {
      versGermanStr = versGermanStr.replaceAll("[\u0000-\u001F]", ""); // remove control characters
      abstractStr = abstractStr + "\nDeutsche Übersetzung: " + versGermanStr;
    }
    abstractStr = StringUtils.deresolveXmlEntities(abstractStr);
    versXmlStrBuilder.append("  <" + db.getMainResourcesTable() + ">\n");
    versXmlStrBuilder.append("    <id>" + id + "</id>\n");
    versXmlStrBuilder.append("    <title>" + title + "</title>\n");
    versXmlStrBuilder.append("    <abstract>" + abstractStr + "</abstract>\n");
    versXmlStrBuilder.append("  </" + db.getMainResourcesTable() + ">\n");
    return versXmlStrBuilder.toString();    
  }
  
  private Hashtable<String, String> getCoranicumAllKoranVersesGerman(Connection conn) throws ApplicationException {
    Hashtable<String, String> koranVersesGerman = new Hashtable<String, String>(); // e.g. key is "sure/2/vers/102"
    String selectKoranVersesGerman = 
        "SELECT\n" +
        " g.sure, g.vers, g.text\n" +
        "FROM koran_uebersetzung g\n";
        // "WHERE\n" +
        // " (g.sure = 2 OR g.sure = 3)\n" +
        // "AND\n" +
        // " (g.vers = 77 OR g.vers = 101 OR g.vers = 102)\n";
    try {
      PreparedStatement psKoranVersesGerman = conn.prepareStatement(selectKoranVersesGerman);
      ResultSet rsKoranVerses = psKoranVersesGerman.executeQuery();
      while (rsKoranVerses.next()) {
        String sure = rsKoranVerses.getString("sure");
        String vers = rsKoranVerses.getString("vers");
        String text = rsKoranVerses.getString("text");
        String versKeyTmp = "sure/" + sure + "/vers/" + vers;
        koranVersesGerman.put(versKeyTmp, text);
      }      
    } catch (Exception e) {
      try {
        conn.close();
      } catch (Exception e2) {
        // nothing
      };
      throw new ApplicationException(e);
    }
    return koranVersesGerman;
  }
  
  private void generateDwbFiles(Collection collection, Database db) throws ApplicationException {
    // generates both dump files + rdf file
    String collectionRdfId = collection.getRdfId();
    try {
      String xmlDumpFileFilter = db.getName() + "-" + "*.xml"; 
      FileFilter fileFilter = new WildcardFileFilter(xmlDumpFileFilter);
      File dwbExternalResourcesDir = new File(externalResourcesDirName + "/dwb");
      File[] files = dwbExternalResourcesDir.listFiles(fileFilter);
      if (files != null && files.length > 0) {
        for (int i = 0; i < files.length; i++) {
          File dumpFileToDelete = files[i];
          FileUtils.deleteQuietly(dumpFileToDelete);
        }
        LOGGER.info("Database dump files of database \"" + db.getName() + "\" sucessfully deleted");
      }
      StringBuilder rdfRecordsStrBuilder = new StringBuilder();
      rdfRecordsStrBuilder.append("<!-- Resources of database: " + db.getName() + " (External directory: " + dwbExternalResourcesDir + ") -->\n");
      File dwbMainFile = new File(externalResourcesDirName + "/dwb/dwb.xml");
      URL dwbMainFileUrl = dwbMainFile.toURI().toURL();
      XdmValue xmdValueMainResources = xQueryEvaluator.evaluate(dwbMainFileUrl, "//entry");
      XdmSequenceIterator xmdValueMainResourcesIterator = xmdValueMainResources.iterator();
      if (xmdValueMainResources != null && xmdValueMainResources.size() > 0) {
        while (xmdValueMainResourcesIterator.hasNext()) {
          XdmItem xdmItemMainResource = xmdValueMainResourcesIterator.next();
          String xdmItemMainResourceStr = xdmItemMainResource.toString();
          StringBuilder entryTeiFileStrBuilder = new StringBuilder();
          String entryKey = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string(/entry/@key)");
          entryTeiFileStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
          entryTeiFileStrBuilder.append("<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\n");
          entryTeiFileStrBuilder.append("<teiHeader>\n");
          entryTeiFileStrBuilder.append("  <fileDesc>\n");
          entryTeiFileStrBuilder.append("    <titleStmt>\n");
          entryTeiFileStrBuilder.append("      <title type=\"main\">Deutsches Wörterbuch von Jacob Grimm und Wilhelm Grimm: Belege zu: " + entryKey + "</title>\n");
          entryTeiFileStrBuilder.append("      <author>\n");
          entryTeiFileStrBuilder.append("        <persName>\n");
          entryTeiFileStrBuilder.append("          <surname>Grimm</surname>\n");
          entryTeiFileStrBuilder.append("          <forename>Jacob</forename>\n");
          entryTeiFileStrBuilder.append("        </persName>\n");
          entryTeiFileStrBuilder.append("      </author>\n");
          entryTeiFileStrBuilder.append("      <author>\n");
          entryTeiFileStrBuilder.append("        <persName>\n");
          entryTeiFileStrBuilder.append("          <surname>Grimm</surname>\n");
          entryTeiFileStrBuilder.append("          <forename>Wilhelm</forename>\n");
          entryTeiFileStrBuilder.append("        </persName>\n");
          entryTeiFileStrBuilder.append("      </author>\n");
          entryTeiFileStrBuilder.append("    </titleStmt>\n");
          entryTeiFileStrBuilder.append("    <publicationStmt>\n");
          entryTeiFileStrBuilder.append("      <pubPlace>Berlin</pubPlace>\n");
          // entryTeiFileStrBuilder.append("      <date>2014-01-09T23:11:23Z</date>\n");
          entryTeiFileStrBuilder.append("      <publisher>\n");
          entryTeiFileStrBuilder.append("        <orgName>Deutsches Wörterbuch</orgName>\n");
          entryTeiFileStrBuilder.append("        <orgName>Berlin-Brandenburgische Akademie der Wissenschaften (BBAW)</orgName>\n");
          entryTeiFileStrBuilder.append("      </publisher>\n");
          entryTeiFileStrBuilder.append("      <availability>\n");
          entryTeiFileStrBuilder.append("        <licence target=\"http://creativecommons.org/licenses/by-nc/3.0/de/\">\n");
          entryTeiFileStrBuilder.append("          <p>Distributed under the Creative Commons Attribution-NonCommercial 3.0 Unported License.</p>\n");
          entryTeiFileStrBuilder.append("        </licence>\n");
          entryTeiFileStrBuilder.append("      </availability>\n");
          entryTeiFileStrBuilder.append("      <idno>\n");
          entryTeiFileStrBuilder.append("        <idno type=\"URLWeb\">" + db.getWebIdPreStr() + entryKey + db.getWebIdAfterStr() + "</idno>\n");
          entryTeiFileStrBuilder.append("      </idno>\n");
          entryTeiFileStrBuilder.append("    </publicationStmt>\n");
          entryTeiFileStrBuilder.append("  </fileDesc>\n");
          entryTeiFileStrBuilder.append("  <profileDesc>\n");
          entryTeiFileStrBuilder.append("    <langUsage>\n");
          entryTeiFileStrBuilder.append("      <language ident=\"deu\">German</language>\n");
          entryTeiFileStrBuilder.append("    </langUsage>\n");
          entryTeiFileStrBuilder.append("  </profileDesc>\n");
          entryTeiFileStrBuilder.append("</teiHeader>\n");
          entryTeiFileStrBuilder.append("<text>\n");
          entryTeiFileStrBuilder.append(xdmItemMainResourceStr + "\n");
          entryTeiFileStrBuilder.append("</text>\n");
          entryTeiFileStrBuilder.append("</TEI>\n");
          String entryTeiFileName = externalResourcesDirName + "/dwb/dwb-" + entryKey + ".xml";
          String rdfFileId = "file:" + entryTeiFileName;
          rdfRecordsStrBuilder.append("<rdf:Description rdf:about=\"" + rdfFileId + "\">\n");
          rdfRecordsStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
          rdfRecordsStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + collectionRdfId + "\"/>\n");
          rdfRecordsStrBuilder.append("  <dc:identifier rdf:resource=\"" + rdfFileId + "\"/>\n");
          rdfRecordsStrBuilder.append("</rdf:Description>\n");
          File entryTeiFile = new File(entryTeiFileName);
          FileUtils.writeStringToFile(entryTeiFile, entryTeiFileStrBuilder.toString(), "utf-8");
        }
      }
      String rdfFileName = externalResourcesDirName + "/dwb/dwb" + ".rdf";
      File rdfFile = new File(rdfFileName);
      writeRdfFile(rdfFile, rdfRecordsStrBuilder, collectionRdfId);
      LOGGER.info("Database dump files in directory \"" + dwbExternalResourcesDir + "\" and RDF file: " + rdfFileName + " sucessfully created");
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  private void generateDtmhFiles(Collection collection, Database db) throws ApplicationException {
    String dbName = db.getName();
    File dbResourcesDir = new File(dbResourcesDirName);
    String xmlDumpFileFilter = collection.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter fileFilter = new WildcardFileFilter(xmlDumpFileFilter);
    File[] files = dbResourcesDir.listFiles(fileFilter);
    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
        File dumpFileToDelete = files[i];
        FileUtils.deleteQuietly(dumpFileToDelete);
      }
      LOGGER.info("Database dump files of database \"" + db.getName() + "\" sucessfully deleted");
    }
    StringBuilder xmlDumpStrBuilder = new StringBuilder();
    xmlDumpStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    xmlDumpStrBuilder.append("<!-- Resources of database: " + db.getName() + " (Collection: " + collection.getId() + ") -->\n");
    xmlDumpStrBuilder.append("<" + dbName + ">\n");
    String collectionId = collection.getId();
    String collExternalResourcesDirName = externalResourcesDirName + "/" + collectionId;
    File dbMainFile = new File(collExternalResourcesDirName + "/" + collection.getId() + "-" + db.getName() + "-1.xml");
    try {
      URL dbMainFileUrl = dbMainFile.toURI().toURL();
      XdmValue xmdValueMainResources = xQueryEvaluator.evaluate(dbMainFileUrl, "//*:Document");
      Hashtable<String, MetadataRecord> resources = new Hashtable<String, MetadataRecord>();  // key is: url of the resource
      XdmSequenceIterator xmdValueMainResourcesIterator = xmdValueMainResources.iterator();
      if (xmdValueMainResources != null && xmdValueMainResources.size() > 0) {
        while (xmdValueMainResourcesIterator.hasNext()) {
          XdmItem xdmItemMainResource = xmdValueMainResourcesIterator.next();
          String xdmItemMainResourceStr = xdmItemMainResource.toString();
          xdmItemMainResourceStr = xdmItemMainResourceStr.replaceAll("[\u0000-\u001F]", ""); // remove control characters
          xdmItemMainResourceStr = xdmItemMainResourceStr.replaceAll("&#xD;&#xA;", ""); // remove control characters
          String id = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string(//*:Field[@Type = '8540']/@Value)");
          if (id != null && id.startsWith("http")) {
            if (id.contains(".html"))
              id = id.substring(0, id.indexOf(".html")) + ".html";  // remove double urls
            MetadataRecord mdRecord = resources.get(id);
            if (mdRecord == null) {
              mdRecord = new MetadataRecord();
              mdRecord.setUri(id);
              resources.put(id, mdRecord);
            }
            String placeVerwaltung = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string((//*:Field[@Value = 'Verwaltung']/*:Field[@Type = '4564']/@Value)[1])");
            String libraryVerwaltung = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string((//*:Field[@Value = 'Verwaltung']/*:Field[@Type = '4600']/@Value)[1])");
            String placeVorbesitz = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string((//*:Field[@Value = 'Vorbesitz']/*:Field[@Type = '4564']/@Value)[1])");
            String libraryVorbesitz = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string((//*:Field[@Value = 'Vorbesitz']/*:Field[@Type = '4600']/@Value)[1])");
            if (mdRecord.getTitle() == null) {
              String title = placeVerwaltung + ", " + libraryVerwaltung;
              if (placeVorbesitz != null && ! placeVorbesitz.isEmpty())
                title = placeVorbesitz + ", " + libraryVorbesitz + " [heute: " + title + "]";
              mdRecord.setTitle(title);
            }
            String abstractStr = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string(//*:Field[@Type = '5209']/@Value)");
            if (mdRecord.getDescription() == null)
              mdRecord.setDescription(abstractStr);
            else 
              mdRecord.setDescription(mdRecord.getDescription() + " " + abstractStr);
          }
        }
      }
      Enumeration<String> resoucesKeys = resources.keys();
      while (resoucesKeys.hasMoreElements()) {
        String uri = resoucesKeys.nextElement();
        MetadataRecord mdRecord = resources.get(uri);
        xmlDumpStrBuilder.append("  <" + db.getMainResourcesTable() + ">\n");
        xmlDumpStrBuilder.append("    <id>" + uri + "</id>\n");
        String title = mdRecord.getTitle();
        title = StringUtils.deresolveXmlEntities(title);
        xmlDumpStrBuilder.append("    <title>" + title + "</title>\n");
        xmlDumpStrBuilder.append("    <publisher>" + "BBAW: Deutsche Texte des Mittelalters: Handschriftenarchiv" + "</publisher>\n");
        xmlDumpStrBuilder.append("    <rights>" + "CC BY-NC-SA 3.0" + "</rights>\n");
        String abstractStr = mdRecord.getDescription();
        if (abstractStr != null) {
          abstractStr = StringUtils.deresolveXmlEntities(abstractStr);
          xmlDumpStrBuilder.append("    <abstract>" + abstractStr + "</abstract>\n");
        }
        xmlDumpStrBuilder.append("  </" + db.getMainResourcesTable() + ">\n");
      }
      xmlDumpStrBuilder.append("</" + db.getName() + ">\n");
      String xmlDumpFileName = dbResourcesDirName + "/" + collection.getId() + "-" + dbName + "-1" + ".xml";
      File dumpFile = new File(xmlDumpFileName);
      FileUtils.writeStringToFile(dumpFile, xmlDumpStrBuilder.toString(), "utf-8");
      LOGGER.info("Database dump file \"" + xmlDumpFileName + "\" sucessfully created");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void generatePmbzFiles(Collection collection, Database db) throws ApplicationException {
    String dbName = db.getName();
    File dbResourcesDir = new File(dbResourcesDirName);
    String xmlDumpFileFilter = collection.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter fileFilter = new WildcardFileFilter(xmlDumpFileFilter);
    File[] files = dbResourcesDir.listFiles(fileFilter);
    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
        File dumpFileToDelete = files[i];
        FileUtils.deleteQuietly(dumpFileToDelete);
      }
      LOGGER.info("Database dump files of database \"" + db.getName() + "\" sucessfully deleted");
    }
    StringBuilder xmlDumpStrBuilder = new StringBuilder();
    xmlDumpStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    xmlDumpStrBuilder.append("<!-- Resources of database: " + db.getName() + " (Collection: " + collection.getId() + ") -->\n");
    xmlDumpStrBuilder.append("<" + dbName + ">\n");
    String collectionId = collection.getId();
    String collExternalResourcesDirName = externalResourcesDirName + "/" + collectionId;
    File dbMainFile = new File(collExternalResourcesDirName + "/" + collection.getId() + "-" + db.getName() + "-1.xml");
    try {
      URL dbMainFileUrl = dbMainFile.toURI().toURL();
      XdmValue xmdValueMainResources = xQueryEvaluator.evaluate(dbMainFileUrl, "//*:entry");
      Hashtable<String, MetadataRecord> resources = new Hashtable<String, MetadataRecord>();  // key is: id of the resource
      XdmSequenceIterator xmdValueMainResourcesIterator = xmdValueMainResources.iterator();
      if (xmdValueMainResources != null && xmdValueMainResources.size() > 0) {
        while (xmdValueMainResourcesIterator.hasNext()) {
          XdmItem xdmItemMainResource = xmdValueMainResourcesIterator.next();
          String xdmItemMainResourceStr = xdmItemMainResource.toString();
          String id = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "//*:field[@type = 'Arbeitsnr.']/*:item/text()");
          if (id != null && ! id.isEmpty()) {
            MetadataRecord mdRecord = resources.get(id);
            if (mdRecord == null) {
              mdRecord = new MetadataRecord();
              mdRecord.setUri(id);
              resources.put(id, mdRecord);
            }
            String title = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "//*:field[@type = 'Name']/*:item/text()");
            if (title != null)
              mdRecord.setTitle(title);
            String quellenname = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "//*:field[@type = 'Quellenname']/*:item/text()");
            String vita = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string(//*:field[@type = 'Vita']/*:item)");
            if (vita != null && vita.length() > 400)
              vita = vita.substring(0, 400) + " ...";
            String abstractStr = "Quellename: " + quellenname + " Vita: " + vita;
            if (quellenname == null)
              abstractStr = "Vita: " + vita;
            else if (vita == null)
              abstractStr = "Quellename: " + quellenname;
            mdRecord.setDescription(abstractStr);
          }
        }
      }
      Enumeration<String> resoucesKeys = resources.keys();
      while (resoucesKeys.hasMoreElements()) {
        String id = resoucesKeys.nextElement();
        MetadataRecord mdRecord = resources.get(id);
        String title = mdRecord.getTitle();
        if (title != null) {
          xmlDumpStrBuilder.append("  <" + db.getMainResourcesTable() + ">\n");
          xmlDumpStrBuilder.append("    <id>" + id + "</id>\n");
          title = StringUtils.deresolveXmlEntities(title);
          xmlDumpStrBuilder.append("    <title>" + title + "</title>\n");
          xmlDumpStrBuilder.append("    <publisher>" + "BBAW: Prosopographie der mittelbyzantinischen Zeit" + "</publisher>\n");
          xmlDumpStrBuilder.append("    <rights>" + "CC BY-NC-SA 3.0" + "</rights>\n");
          String abstractStr = mdRecord.getDescription();
          if (abstractStr != null) {
            abstractStr = StringUtils.deresolveXmlEntities(abstractStr);
            xmlDumpStrBuilder.append("    <abstract>" + abstractStr + "</abstract>\n");
          }
          xmlDumpStrBuilder.append("  </" + db.getMainResourcesTable() + ">\n");
        }
      }
      xmlDumpStrBuilder.append("</" + db.getName() + ">\n");
      String xmlDumpFileName = dbResourcesDirName + "/" + collection.getId() + "-" + dbName + "-1" + ".xml";
      File dumpFile = new File(xmlDumpFileName);
      FileUtils.writeStringToFile(dumpFile, xmlDumpStrBuilder.toString(), "utf-8");
      LOGGER.info("Database dump file \"" + xmlDumpFileName + "\" sucessfully created");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void generateAaewtlaFiles(Collection collection, Database db) throws ApplicationException {
    String dbName = db.getName();
    File dbResourcesDir = new File(dbResourcesDirName);
    String xmlDumpFileFilter = collection.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter fileFilter = new WildcardFileFilter(xmlDumpFileFilter);
    File[] files = dbResourcesDir.listFiles(fileFilter);
    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
        File dumpFileToDelete = files[i];
        FileUtils.deleteQuietly(dumpFileToDelete);
      }
      LOGGER.info("Database dump files of database \"" + db.getName() + "\" sucessfully deleted");
    }
    StringBuilder xmlDumpStrBuilder = new StringBuilder();
    xmlDumpStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    xmlDumpStrBuilder.append("<!-- Resources of database: " + db.getName() + " (Collection: " + collection.getId() + ") -->\n");
    xmlDumpStrBuilder.append("<" + dbName + ">\n");
    String collectionId = collection.getId();
    String collExternalResourcesDirName = externalResourcesDirName + "/" + collectionId;
    File dbMainFile = new File(collExternalResourcesDirName + "/" + collection.getId() + "-" + db.getName() + "-1.xml");
    try {
      URL dbMainFileUrl = dbMainFile.toURI().toURL();
      XdmValue xmdValueMainResources = xQueryEvaluator.evaluate(dbMainFileUrl, "//*:bwllist");
      XdmSequenceIterator xmdValueMainResourcesIterator = xmdValueMainResources.iterator();
      if (xmdValueMainResources != null && xmdValueMainResources.size() > 0) {
        while (xmdValueMainResourcesIterator.hasNext()) {
          XdmItem xdmItemMainResource = xmdValueMainResourcesIterator.next();
          String xdmItemMainResourceStr = xdmItemMainResource.toString();
          xmlDumpStrBuilder.append("  <" + db.getMainResourcesTable() + ">\n");
          String id = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string(/*:bwllist/@wcn)");
          xmlDumpStrBuilder.append("    <id>" + id + "</id>\n");
          String title = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string(/*:bwllist/@lemma)");
          xmlDumpStrBuilder.append("    <title>" + "Lemma: " + title + "</title>\n");
          xmlDumpStrBuilder.append("    <publisher>" + "BBAW: Altägyptisches Wörterbuch: Thesaurus Linguae Aegyptiae (TLA)" + "</publisher>\n");
          xmlDumpStrBuilder.append("    <rights>" + "CC BY-NC-SA 3.0" + "</rights>\n");
          String label = xQueryEvaluator.evaluateAsString(xdmItemMainResourceStr, "string(/*:bwllist/@label)");
          String elabel = xQueryEvaluator.evaluateAsString(dbMainFileUrl, "//*:bwlengl[@wcn = '" + id + "']");
          String abstractStr = label + " " + elabel;
          abstractStr = StringUtils.deresolveXmlEntities(abstractStr);
          xmlDumpStrBuilder.append("    <abstract>" + abstractStr + "</abstract>\n");
          xmlDumpStrBuilder.append("  </" + db.getMainResourcesTable() + ">\n");
        }
      }
      xmlDumpStrBuilder.append("</" + db.getName() + ">\n");
      String xmlDumpFileName = dbResourcesDirName + "/" + collection.getId() + "-" + dbName + "-1" + ".xml";
      File dumpFile = new File(xmlDumpFileName);
      FileUtils.writeStringToFile(dumpFile, xmlDumpStrBuilder.toString(), "utf-8");
      LOGGER.info("Database dump file \"" + xmlDumpFileName + "\" sucessfully created");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void generateOaiDbXmlDumpFiles(Collection collection, Database db) throws ApplicationException {
    File dbResourcesDir = new File(dbResourcesDirName);
    String xmlDumpFileFilter = collection.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter fileFilter = new WildcardFileFilter(xmlDumpFileFilter);
    File[] files = dbResourcesDir.listFiles(fileFilter);
    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
        File dumpFileToDelete = files[i];
        FileUtils.deleteQuietly(dumpFileToDelete);
      }
      LOGGER.info("Database dump files of database \"" + db.getName() + "\" sucessfully deleted");
    }
    StringBuilder xmlDumpStrBuilder = new StringBuilder();
    String oaiServerUrl = db.getXmlDumpUrl();
    String oaiSet = db.getXmlDumpSet();
    String listRecordsUrl = oaiServerUrl + "?verb=ListRecords&metadataPrefix=oai_dc&set=" + oaiSet;
    if (oaiSet == null)
      listRecordsUrl = oaiServerUrl + "?verb=ListRecords&metadataPrefix=oai_dc";
    String oaiPmhResponseStr = performGetRequest(listRecordsUrl);
    String oaiRecordsStr = xQueryEvaluator.evaluateAsString(oaiPmhResponseStr, "/*:OAI-PMH/*:ListRecords/*:record");
    xmlDumpStrBuilder.append(oaiRecordsStr);
    String resumptionToken = xQueryEvaluator.evaluateAsString(oaiPmhResponseStr, "/*:OAI-PMH/*:ListRecords/*:resumptionToken/text()");
    // if end is reached before resumptionToken comes
    if (resumptionToken == null || resumptionToken.isEmpty()) {
      writeOaiDbXmlDumpFile(collection, db, xmlDumpStrBuilder, 1);
    } else {
      int resumptionTokenCounter = 1; 
      int fileCounter = 1;
      while (resumptionToken != null && ! resumptionToken.isEmpty()) {
        String listRecordsResumptionTokenUrl = oaiServerUrl + "?verb=ListRecords&resumptionToken=" + resumptionToken;
        oaiPmhResponseStr = performGetRequest(listRecordsResumptionTokenUrl);
        oaiRecordsStr = xQueryEvaluator.evaluateAsString(oaiPmhResponseStr, "/*:OAI-PMH/*:ListRecords/*:record");
        xmlDumpStrBuilder.append(oaiRecordsStr + "\n");
        resumptionToken = xQueryEvaluator.evaluateAsString(oaiPmhResponseStr, "/*:OAI-PMH/*:ListRecords/*:resumptionToken/text()");
        int writeFileInterval = resumptionTokenCounter % 100; // after each 100 resumptionTokens generate a new file
        if (writeFileInterval == 0) {
          writeOaiDbXmlDumpFile(collection, db, xmlDumpStrBuilder, fileCounter);
          xmlDumpStrBuilder = new StringBuilder();
          fileCounter++;
        }
        resumptionTokenCounter++;
      }
      // write the last resumptionTokens
      if (xmlDumpStrBuilder.length() > 0)
        writeOaiDbXmlDumpFile(collection, db, xmlDumpStrBuilder, fileCounter);
    }
  }

  private void writeOaiDbXmlDumpFile(Collection collection, Database db, StringBuilder recordsStrBuilder, int fileCounter) throws ApplicationException {
    StringBuilder xmlDumpStrBuilder = new StringBuilder();
    String oaiSet = db.getXmlDumpSet();
    String xmlDumpFileName = dbResourcesDirName + "/" + collection.getId() + "-" + db.getName() + "-" + fileCounter + ".xml";
    xmlDumpStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    if (oaiSet == null)
      xmlDumpStrBuilder.append("<!-- Resources of OAI-PMH-Server: " + db.getName() + " (Url: " + db.getXmlDumpUrl() + ", File: " + xmlDumpFileName + ") -->\n");
    else 
      xmlDumpStrBuilder.append("<!-- Resources of OAI-PMH-Server: " + db.getName() + " (Url: " + db.getXmlDumpUrl() + ", File: " + xmlDumpFileName + ", Set: " + db.getXmlDumpSet() + ") -->\n");
    xmlDumpStrBuilder.append("<OAI-PMH>\n");
    xmlDumpStrBuilder.append("<ListRecords>\n");
    xmlDumpStrBuilder.append(recordsStrBuilder.toString());
    xmlDumpStrBuilder.append("</ListRecords>\n");
    xmlDumpStrBuilder.append("</OAI-PMH>");
    try {
      File dumpFile = new File(xmlDumpFileName);
      FileUtils.writeStringToFile(dumpFile, xmlDumpStrBuilder.toString());
      LOGGER.info("Database dump file \"" + xmlDumpFileName + "\" sucessfully created");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void generatePdrDbXmlDumpFile(Collection collection, Database db) throws ApplicationException {
    String dbName = db.getName();
    XslResourceTransformer pdrIdiTransformer = new XslResourceTransformer("pdrIdiGnd.xsl");
    File dbResourcesDir = new File(dbResourcesDirName);
    String xmlDumpFileFilter = collection.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter fileFilter = new WildcardFileFilter(xmlDumpFileFilter);
    File[] files = dbResourcesDir.listFiles(fileFilter);
    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
        File dumpFileToDelete = files[i];
        FileUtils.deleteQuietly(dumpFileToDelete);
      }
      LOGGER.info("Database dump files of database \"" + db.getName() + "\" sucessfully deleted");
    }
    StringBuilder xmlDumpStrBuilder = new StringBuilder();
    xmlDumpStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    xmlDumpStrBuilder.append("<!-- Resources of database: " + db.getName() + " (Collection: " + collection.getId() + ") -->\n");
    xmlDumpStrBuilder.append("<" + dbName + ">\n");
    ArrayList<String> personIds = null; 
    if (dbName.equals("persons"))
      personIds = getPdrPersonIds();
    else if (dbName.equals("gnds"))
      personIds = getPdrGndIds();
    for (int i=0; i<personIds.size(); i++) {
      // e.g.: pdrPo.001.005.000000362
      String personId = personIds.get(i);
      String persName = null;
      String personStr = null;
      if (dbName.equals("persons")) {
        String getPersonUrl = "http://www.personendaten.org/module/getPerson.php?extern&pdrId=" + personId;
        String resultPersonHtmlStr = null;
        try {
          resultPersonHtmlStr = performGetRequest(getPersonUrl);
        } catch (Exception e) {
          LOGGER.info(getPersonUrl + " did not work"); 
        }
        if (resultPersonHtmlStr != null) {
          resultPersonHtmlStr = resultPersonHtmlStr.replaceAll("<!DOCTYPE html[\n ]+SYSTEM \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">", "");
          resultPersonHtmlStr = resultPersonHtmlStr.replaceAll("<html xmlns=\"http://www.w3.org/1999/xhtml\">", "<html>");
          resultPersonHtmlStr = resultPersonHtmlStr.replaceAll("&nbsp;", " ");
          resultPersonHtmlStr = resultPersonHtmlStr.replaceAll("&apos;", "'");
          persName = xQueryEvaluator.evaluateAsString(resultPersonHtmlStr, "/*:html/*:body//*:div[@id = 'header']/descendant::*/text()");
          personStr = xQueryEvaluator.evaluateAsString(resultPersonHtmlStr, "string-join(/*:html/*:body/descendant::*/text(), ' ')");  // all text of all subnodes separated by blank
        }
      } else if (dbName.equals("gnds")) {
        String getPersonUrl = "https://pdrprod.bbaw.de/idi/gnd/" + personId;
        String resultPersonXmlStr = null;
        try {
          resultPersonXmlStr = performGetRequest(getPersonUrl);
        } catch (Exception e) {
          LOGGER.info(getPersonUrl + " did not work"); 
        }
        if (resultPersonXmlStr != null) {
          String resultPersonHtmlStr = pdrIdiTransformer.transformStr(resultPersonXmlStr);
          resultPersonHtmlStr = resultPersonHtmlStr.replaceAll("<!DOCTYPE html[\n ]+SYSTEM \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">", "");
          resultPersonHtmlStr = resultPersonHtmlStr.replaceAll("<html xmlns=\"http://www.w3.org/1999/xhtml\">", "<html>");
          resultPersonHtmlStr = resultPersonHtmlStr.replaceAll("&nbsp;", " ");
          resultPersonHtmlStr = resultPersonHtmlStr.replaceAll("&apos;", "'");
          persName = xQueryEvaluator.evaluateAsString(resultPersonHtmlStr, "/*:html/*:body//*:div[@id = 'header']/descendant::*/text()");
          personStr = xQueryEvaluator.evaluateAsString(resultPersonHtmlStr, "string-join(/*:html/*:body/descendant::*/text(), ' ')");  // all text of all subnodes separated by blank
        }
      }      
      xmlDumpStrBuilder.append("  <" + db.getMainResourcesTable() + ">\n");
      xmlDumpStrBuilder.append("    <id>" + personId + "</id>\n");
      xmlDumpStrBuilder.append("    <title>" + persName + "</title>\n");
      xmlDumpStrBuilder.append("    <publisher>" + "BBAW: Personendaten-Repositorium (PDR)" + "</publisher>\n");
      xmlDumpStrBuilder.append("    <rights>" + "CC BY-NC-SA 3.0" + "</rights>\n");
      personStr = StringUtils.deresolveXmlEntities(personStr);
      xmlDumpStrBuilder.append("    <abstract>" + personStr + "</abstract>\n");
      xmlDumpStrBuilder.append("  </" + db.getMainResourcesTable() + ">\n");
    }
    xmlDumpStrBuilder.append("</" + db.getName() + ">\n");
    String xmlDumpFileName = dbResourcesDirName + "/" + collection.getId() + "-" + dbName + "-1" + ".xml";
    try {
      File dumpFile = new File(xmlDumpFileName);
      FileUtils.writeStringToFile(dumpFile, xmlDumpStrBuilder.toString(), "utf-8");
      LOGGER.info("Database dump file \"" + xmlDumpFileName + "\" sucessfully created");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private ArrayList<String> getPdrPersonIds() throws ApplicationException {
    ArrayList<Integer> projectIds = new ArrayList<Integer>();
    projectIds.add(2);
    projectIds.add(3);
    projectIds.add(5);
    projectIds.add(6);
    projectIds.add(7);
    projectIds.add(202);
    ArrayList<String> personIds = new ArrayList<String>();
    for (int i=0; i<projectIds.size(); i++) {
      int projId = projectIds.get(i);
      String projIdSize3Str = "00" + projId;
      if (projId == 202)
        projIdSize3Str = "202";
      String getOccupiedIDRangesUrl = "https://pdrprod.bbaw.de/axis2/services/Utilities/getOccupiedIDRanges?Type=pdrPo&Instance=1&Min=1&Max=99999999&Project=" + projId;
      String idRangesXmlStr = performGetRequest(getOccupiedIDRangesUrl);
      XdmValue xmdValueIdRanges = xQueryEvaluator.evaluate(idRangesXmlStr, "//*:Range");
      XdmSequenceIterator xmdValueIdRangesIterator = xmdValueIdRanges.iterator();
      if (xmdValueIdRanges != null && xmdValueIdRanges.size() > 0) {
        while (xmdValueIdRangesIterator.hasNext()) {
          XdmItem xdmItemField = xmdValueIdRangesIterator.next();
          String rangeXml = xdmItemField.toString();
          String rangeMinStr = xQueryEvaluator.evaluateAsString(rangeXml, "//*:Min/text()");
          String rangeMaxStr = xQueryEvaluator.evaluateAsString(rangeXml, "//*:Max/text()");
          Integer rangeMin = Integer.valueOf(rangeMinStr);
          Integer rangeMax = Integer.valueOf(rangeMaxStr);
          for (int j=rangeMin; j<=rangeMax; j++) {
            String personIdSize9Str = "" + j;
            if (j < 10)
              personIdSize9Str = "00000000" + j;
            else if (j < 100)
              personIdSize9Str = "0000000" + j;
            else if (j < 1000)
              personIdSize9Str = "000000" + j;
            else if (j < 10000)
              personIdSize9Str = "00000" + j;
            else if (j < 100000)
              personIdSize9Str = "0000" + j;
            else if (j < 1000000)
              personIdSize9Str = "000" + j;
            else if (j < 10000000)
              personIdSize9Str = "00" + j;
            else if (j < 100000000)
              personIdSize9Str = "0" + j;
            personIds.add("pdrPo.001." + projIdSize3Str + "." + personIdSize9Str);
          }
        }
      }
    }
    return personIds;
  }
  
  private ArrayList<String> getPdrGndIds()throws ApplicationException {
    ArrayList<String> gndIds = new ArrayList<String>();
    String getIdiBeaconUrl = "https://pdrprod.bbaw.de/idi/beacon";
    String idiBeaconStr = performGetRequest(getIdiBeaconUrl);
    String[] idiBeaconStrLines = idiBeaconStr.split("\n");
    for (int i=0; i<idiBeaconStrLines.length; i++) {
      String line = idiBeaconStrLines[i];
      if (! (line.startsWith("#"))) {
        String gndId = line.replaceAll("\\|.+", "");
        gndIds.add(gndId);
      }
    }
    return gndIds;
  }
  
  private void convertAll() throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (int i=0; i<collections.size(); i++) {
      Collection collection = collections.get(i);
      convert(collection, true);
    }
  }
  
  private void convert(Collection collection, boolean convertDbXmlFiles) throws ApplicationException {
    String collectionId = collection.getId();
    String collectionRdfId = collection.getRdfId();
    try {
      StringBuilder rdfRecordsStrBuilder = new StringBuilder();
      URL inputNormdataFileUrl = inputNormdataFile.toURI().toURL();
      String projectRdfStr = xQueryEvaluator.evaluateAsString(inputNormdataFileUrl, "/*:RDF/*:Description[@*:about='" + collectionRdfId + "']");
      if (projectRdfStr != null && ! projectRdfStr.trim().isEmpty()) {
        proofProjectRdfStr(projectRdfStr, collection); // compare rdf project fields with config fields
        WspUrl[] dataUrls = collection.getDataUrls();
        if (dataUrls != null) {
          for (int i=0; i<dataUrls.length; i++) {
            WspUrl dataUrl = dataUrls[i];
            String dataUrlStr = dataUrl.getUrl();
            String dataUrlStrResolved = StringUtils.deresolveXmlEntities(dataUrlStr);
            rdfRecordsStrBuilder.append("<rdf:Description rdf:about=\"" + dataUrlStrResolved + "\">\n");
            rdfRecordsStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
            rdfRecordsStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + collectionRdfId + "\"/>\n");
            rdfRecordsStrBuilder.append("  <dc:identifier rdf:resource=\"" + dataUrlStrResolved + "\"/>\n");
            if (dataUrl.isEXistDir()) {
              rdfRecordsStrBuilder.append("  <dc:type>eXistDir</dc:type>\n");
            }
            rdfRecordsStrBuilder.append("</rdf:Description>\n");
          }
        }
      } else {
        LOGGER.error("Project: \"" + collectionRdfId + "\" (configId: \"" + collectionId + "\") does not exist");
      }
      String rdfFileName = resourcesDirName + "/" + collectionId + ".rdf";
      File rdfFile = new File(rdfFileName);
      writeRdfFile(rdfFile, rdfRecordsStrBuilder, collectionRdfId);
      // if collection is a database: insert also from xml db file
      if (convertDbXmlFiles)
        convertDbXmlFiles(collection);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  private void convertDbXmlFiles(Collection collection) throws ApplicationException {
    ArrayList<Database> collectionDBs = collection.getDatabases();
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database collectionDB = collectionDBs.get(i);
        convertDbXmlFiles(collection, collectionDB);
      }
    }
  }
  
  private void convertDbXmlFiles(Collection collection, Database db) throws ApplicationException {
    File dbResourcesDir = new File(dbResourcesDirName);
    String xmlDumpFileFilterName = collection.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter xmlDumpFileFilter = new WildcardFileFilter(xmlDumpFileFilterName);
    File[] xmlDumpFiles = dbResourcesDir.listFiles(xmlDumpFileFilter);
    if (xmlDumpFiles != null && xmlDumpFiles.length > 0) {
      Arrays.sort(xmlDumpFiles, new Comparator<File>() {
        public int compare(File f1, File f2) {
          return f1.getName().compareToIgnoreCase(f2.getName());
        }
      }); 
      // delete the old db rdf files
      String rdfFileFilterName = collection.getId() + "-" + db.getName() + "*.rdf"; 
      FileFilter rdfFileFilter = new WildcardFileFilter(rdfFileFilterName);
      File[] rdfFiles = dbResourcesDir.listFiles(rdfFileFilter);
      if (rdfFiles != null && rdfFiles.length > 0) {
        for (int i = 0; i < rdfFiles.length; i++) {
          File rdfFile = rdfFiles[i];
          FileUtils.deleteQuietly(rdfFile);
        }
      }
      // generate the new db rdf files
      for (int i = 0; i < xmlDumpFiles.length; i++) {
        File dumpFile = xmlDumpFiles[i];
        convertDbXmlFile(collection, db, dumpFile);
      }
    }
  }
  
  private void convertDbXmlFile(Collection collection, Database db, File dumpFile) throws ApplicationException {
    try {
      StringBuilder rdfRecordsStrBuilder = new StringBuilder();
      String dumpFileName = dumpFile.getName();
      rdfRecordsStrBuilder.append("<!-- Resources of database: " + db.getName() + " (Database file name: " + dumpFileName + ") -->\n");
      URL xmlDumpFileUrl = dumpFile.toURI().toURL();
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
          appendRow2rdf(rdfRecordsStrBuilder, collection, db, row);
        }
      }
      String rdfFileName = dumpFileName.replaceAll(".xml", ".rdf");
      String rdfFullFileName = dbResourcesDirName + "/" + rdfFileName; 
      File rdfFile = new File(rdfFullFileName);
      writeRdfFile(rdfFile, rdfRecordsStrBuilder, collection.getRdfId());
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
          row.addField(fieldName, fieldValue);
        }
      }
    } else if (dbType.equals("oai") || dbType.equals("oai-dbrecord")) {
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
          row.addField(fieldName, fieldValue);
        }
      }
    }
    return row;  
  }

  private void appendRow2rdf(StringBuilder rdfStrBuilder, Collection collection, Database db, Row row) {
    String collectionId = collection.getId();
    String collectionRdfId = collection.getRdfId();
    String mainLanguage = collection.getMainLanguage();
    String mainResourcesTableId = db.getMainResourcesTableId();
    String webIdPreStr = db.getWebIdPreStr();
    String webIdAfterStr = db.getWebIdAfterStr();
    String dbType = db.getType();
    String id = row.getFieldValue(mainResourcesTableId);
    id = StringUtils.deresolveXmlEntities(id);
    String rdfId = "http://" + collectionId + ".bbaw.de/id/" + id;
    if (id.startsWith("http://"))
      rdfId = id;  // id is already a url
    String rdfWebId = rdfId;
    if (webIdPreStr != null) {
      rdfWebId = webIdPreStr + id;
    }
    if (webIdAfterStr != null) {
      rdfWebId = rdfWebId + webIdAfterStr;
    }
    rdfStrBuilder.append("<rdf:Description rdf:about=\"" + rdfWebId + "\">\n");
    rdfStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
    rdfStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + collectionRdfId + "\"/>\n");
    rdfStrBuilder.append("  <dc:identifier rdf:resource=\"" + rdfWebId + "\">" + id + "</dc:identifier>\n");
    if (dbType.equals("oai"))
      rdfStrBuilder.append("  <dc:type>oaiRecord</dc:type>\n");
    else
      rdfStrBuilder.append("  <dc:type>dbRecord</dc:type>\n");
    String dbFieldCreator = db.getDbField("creator");
    if (dbFieldCreator != null) {
      String creator = row.getFieldValue(dbFieldCreator);
      if (creator != null && ! creator.isEmpty()) {
        creator = StringUtils.deresolveXmlEntities(creator);
        rdfStrBuilder.append("  <dc:creator>" + creator + "</dc:creator>\n");
      }
    }
    String dbFieldTitle = db.getDbField("title");
    if (dbFieldTitle != null) {
      String title = row.getFieldValue(dbFieldTitle);
      if (title != null && ! title.isEmpty()) {
        title = StringUtils.deresolveXmlEntities(title);
        rdfStrBuilder.append("  <dc:title>" + title + "</dc:title>\n");
      }
    }
    String dbFieldPublisher = db.getDbField("publisher");
    if (dbFieldPublisher != null) {
      String publisher = row.getFieldValue(dbFieldPublisher);
      if (publisher != null && ! publisher.isEmpty()) {
        publisher = StringUtils.deresolveXmlEntities(publisher);
        rdfStrBuilder.append("  <dc:publisher>" + publisher + "</dc:publisher>\n");
      }
    }
    String dbFieldDate = db.getDbField("date");
    if (dbFieldDate != null) {
      String date = row.getFieldValue(dbFieldDate);
      if (date != null && ! date.isEmpty()) {
        date = StringUtils.deresolveXmlEntities(date);
        rdfStrBuilder.append("  <dc:date>" + date + "</dc:date>\n");
      }
    }
    String dbFieldPages = db.getDbField("extent");
    if (dbFieldPages != null) {
      String pages = row.getFieldValue(dbFieldPages);
      if (pages != null && ! pages.isEmpty()) {
        pages = StringUtils.deresolveXmlEntities(pages);
        rdfStrBuilder.append("  <dc:extent>" + pages + "</dc:extent>\n");
      }
    }
    String dbFieldLanguage = db.getDbField("language");
    String language = null;
    if (dbFieldLanguage != null) {
      language = row.getFieldValue(dbFieldLanguage);
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
    if (dbType != null && ! dbType.equals("oai"))
      rdfStrBuilder.append("  <dc:format>text/html</dc:format>\n");
    String dbFieldSubject = db.getDbField("subject");
    if (dbFieldSubject != null) {
      String subject = row.getFieldValue(dbFieldSubject);
      if (subject != null && ! subject.isEmpty()) {
        String[] subjects = subject.split("###");
        if (! subject.contains("###") && subject.contains(";"))
          subjects = subject.split(";");
        if (subjects != null) {
          for (int i=0; i<subjects.length; i++) {
            String s = subjects[i].trim();
            s = StringUtils.deresolveXmlEntities(s);
            rdfStrBuilder.append("  <dc:subject>" + s + "</dc:subject>\n");
          }
        }
      }
    }
    String dbFieldAbstract = db.getDbField("abstract");
    if (dbFieldAbstract != null) {
      String abstractt = row.getFieldValue(dbFieldAbstract);
      if (abstractt != null && ! abstractt.isEmpty()) {
        abstractt = StringUtils.deresolveXmlEntities(abstractt);
        rdfStrBuilder.append("  <dc:abstract>" + abstractt + "</dc:abstract>\n");
      }
    }
    // TODO further dc fields
    rdfStrBuilder.append("</rdf:Description>\n");
  }

  private void writeRdfFile(File rdfFile, StringBuilder rdfRecordsStrBuilder, String collectionRdfId) throws ApplicationException {
    StringBuilder rdfStrBuilder = new StringBuilder();
    rdfStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    rdfStrBuilder.append("<rdf:RDF \n");
    rdfStrBuilder.append("   xmlns=\"" + collectionRdfId + "\" \n");
    rdfStrBuilder.append("   xml:base=\"" + collectionRdfId + "\" \n");
    rdfStrBuilder.append("   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n");
    rdfStrBuilder.append("   xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" \n");
    rdfStrBuilder.append("   xmlns:dc=\"http://purl.org/dc/elements/1.1/\" \n");
    rdfStrBuilder.append("   xmlns:dcterms=\"http://purl.org/dc/terms/\" \n");
    rdfStrBuilder.append("   xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" \n");
    rdfStrBuilder.append("   xmlns:gnd=\"http://d-nb.info/standards/elementset/gnd#\" \n");
    rdfStrBuilder.append("   xmlns:ore=\"http://www.openarchives.org/ore/terms/\" \n");
    rdfStrBuilder.append(">\n");
    rdfStrBuilder.append(rdfRecordsStrBuilder.toString());
    rdfStrBuilder.append("</rdf:RDF>");
    try {
      FileUtils.writeStringToFile(rdfFile, rdfStrBuilder.toString());
      LOGGER.info("RDF file \"" + rdfFile + "\" sucessfully created");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void proofProjectRdfStr(String projectRdfStr, Collection collection) throws ApplicationException {
    String collectionId = collection.getId();
    String collectionRdfId = collection.getRdfId();
    String collectionName = collection.getName();
    String mainLanguage = collection.getMainLanguage();
    String webBaseUrl = collection.getWebBaseUrl();
    String countFoafNickName = xQueryEvaluator.evaluateAsString(projectRdfStr, "count(/*:Description/*:nick)");
    String countFoafName = xQueryEvaluator.evaluateAsString(projectRdfStr, "count(/*:Description/*:name)");
    String countDcLanguage = xQueryEvaluator.evaluateAsString(projectRdfStr, "count(/*:Description/*:language)");
    String countFoafHomepage = xQueryEvaluator.evaluateAsString(projectRdfStr, "count(/*:Description/*:homepage/@*:resource)");
    if (countFoafNickName != null && ! countFoafNickName.contains("0") && ! countFoafNickName.contains("1"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Field <foaf:nick> exists " + countFoafNickName + " times");
    if (countFoafName != null && ! countFoafName.contains("0") && ! countFoafName.contains("1"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Field <foaf:name> exists " + countFoafName + " times");
    if (countDcLanguage != null && ! countDcLanguage.contains("0") && ! countDcLanguage.contains("1"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Field <dc:language> exists " + countDcLanguage + " times");
    if (countFoafHomepage != null && ! countFoafHomepage.contains("0") && ! countFoafHomepage.contains("1"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Field <foaf:homepage> exists " + countFoafHomepage + " times");
    String foafNickName = xQueryEvaluator.evaluateAsString(projectRdfStr, "/*:Description/*:nick[1]/text()");
    String foafName = xQueryEvaluator.evaluateAsString(projectRdfStr, "/*:Description/*:name[1]/text()");
    String dcLanguage = xQueryEvaluator.evaluateAsString(projectRdfStr, "/*:Description/*:language[1]/text()");
    String foafHomepage = xQueryEvaluator.evaluateAsString(projectRdfStr, "string(/*:Description/*:homepage[1]/@*:resource)");
    String rdfType = xQueryEvaluator.evaluateAsString(projectRdfStr, "string(/*:Description/*:type[1]/@*:resource)");
    if (foafNickName == null || foafNickName.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <foaf:nick rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + collectionId + "</foaf:nick>");
    else if (! foafNickName.equals(collectionId))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change \"<foaf:nick>" + foafNickName + "</foaf:nick>\" to <foaf:nick rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + collectionId + "</foaf:nick>");
    if (foafName == null || foafName.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <foaf:name rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + collectionName + "</foaf:name>");
    else if (! foafName.equals(collectionName))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change \"<foaf:name>" + foafName + "</foaf:name>\" to <foaf:name rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + collectionName + "</foaf:name>");
    if (dcLanguage == null || dcLanguage.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <dc:language>" + mainLanguage + "</dc:language>");
    else if (! dcLanguage.equals(mainLanguage))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change \"<dc:language>" + dcLanguage + "</dc:language>\" to <dc:language>" + mainLanguage + "</dc:language>");
    if (foafHomepage == null || foafHomepage.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <foaf:homepage rdf:resource=\"" + webBaseUrl + "\"/>");
    else if (! foafHomepage.equals(webBaseUrl))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change \"<foaf:homepage rdf:resource=\"" + foafHomepage + "\"/>\" to <foaf:homepage rdf:resource=\"" + webBaseUrl + "\"/>");
    if (rdfType == null || rdfType.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Project\"/>");
    else if (! rdfType.equals("http://xmlns.com/foaf/0.1/Project"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change <rdf:type> to <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Project\"/>");
  }

  private void proofCollectionProjects() throws ApplicationException {
    try {
      URL inputNormdataFileUrl = inputNormdataFile.toURI().toURL();
      ArrayList<Collection> collections = collectionReader.getCollections();
      LOGGER.info("Proof ConfigFiles" + " (with " + collections.size() + " projects) against" + inputNormdataFileUrl);
      for (int i=0; i<collections.size(); i++) {
        Collection coll = collections.get(i);
        String id = coll.getId();
        String rdfId = coll.getRdfId();
        String projectRdfStr = xQueryEvaluator.evaluateAsString(inputNormdataFileUrl, "/*:RDF/*:Description[@*:about='" + rdfId + "']/*:nick");
        if (projectRdfStr == null || projectRdfStr.isEmpty())
          LOGGER.error("Collection with id:" + id + " has no nickName");
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void proofRdfProjects() throws ApplicationException {
    try {
      URL inputNormdataFileUrl = inputNormdataFile.toURI().toURL();
      String rdfTypeProject = "http://xmlns.com/foaf/0.1/Project"; 
      XdmValue xmdValueProjects = xQueryEvaluator.evaluate(inputNormdataFileUrl, "/*:RDF/*:Description[*:type[@*:resource='" + rdfTypeProject + "']]");
      LOGGER.info("Proof " + inputNormdataFileUrl + " (with " + xmdValueProjects.size() + " projects) against ConfigFiles (with " + collectionReader.getCollections().size() + " projects)");
      XdmSequenceIterator xmdValueProjectsIterator = xmdValueProjects.iterator();
      if (xmdValueProjects != null && xmdValueProjects.size() > 0) {
        while (xmdValueProjectsIterator.hasNext()) {
          XdmItem xdmItemProject = xmdValueProjectsIterator.next();
          String xdmItemProjectStr = xdmItemProject.toString();
          String projectRdfId = xQueryEvaluator.evaluateAsString(xdmItemProjectStr, "string(/*:Description/@*:about)");
          String foafNickName = xQueryEvaluator.evaluateAsString(xdmItemProjectStr, "/*:Description/*:nick[1]/text()");
          if (foafNickName == null || foafNickName.trim().isEmpty()) {
            LOGGER.error("Project: \"" + projectRdfId + "\" has no \"<foaf:nick> entry");
          } else {
            Collection coll = collectionReader.getCollection(foafNickName);
            if (coll == null)
              LOGGER.error("Project: \"" + projectRdfId + "\" with \"<foaf:nick>" + foafNickName + "</foaf:nick>\"" + " has no config file");
          }
          String status = xQueryEvaluator.evaluateAsString(xdmItemProjectStr, "/*:Description/*:status[1]/text()");
          if (status == null || status.trim().isEmpty())
            LOGGER.error("Project: \"" + projectRdfId + "\" has no status (abgeschlossen or aktiv)");
          String description = xQueryEvaluator.evaluateAsString(xdmItemProjectStr, "/*:Description/*:description[1]/text()");
          if (description == null || description.trim().isEmpty())
            LOGGER.error("Project: \"" + projectRdfId + "\" has no description (Vorhaben etc.)");
          String abstractStr = xQueryEvaluator.evaluateAsString(xdmItemProjectStr, "/*:Description/*:abstract[1]/text()");
          if (abstractStr == null || abstractStr.trim().isEmpty())
            LOGGER.error("Project: \"" + projectRdfId + "\" has no abstract");
        }
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private String performGetRequest(String urlStr) throws ApplicationException {
    String resultStr = null;
    int statusCode = -1;
    try {
      GetMethod method = new GetMethod(urlStr);
      statusCode = httpClient.executeMethod(method);
      if (statusCode < 400) {
        byte[] resultBytes = method.getResponseBody();
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
  
}
