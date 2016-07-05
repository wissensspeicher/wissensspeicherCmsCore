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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.document.SubjectHandler;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.general.Constants;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

public class MetadataHandler {
  private static Logger LOGGER = Logger.getLogger(MetadataHandler.class);
  private static MetadataHandler mdHandler;
  private static int SOCKET_TIMEOUT = 20 * 1000;
  private Hashtable<String, ArrayList<MetadataRecord>> collectionMdRecords;
  private XQueryEvaluator xQueryEvaluator;
  private HttpClient httpClient; 
  private Hashtable<String, String> institutes2collectionId;

  public static MetadataHandler getInstance() throws ApplicationException {
    if(mdHandler == null) {
      mdHandler = new MetadataHandler();
      mdHandler.init();
    }
    return mdHandler;
  }
  
  private void init() throws ApplicationException {
    collectionMdRecords = new Hashtable<String, ArrayList<MetadataRecord>>();
    xQueryEvaluator = new XQueryEvaluator();
    httpClient = new HttpClient();
    // timeout seems to work
    httpClient.getParams().setParameter("http.socket.timeout", SOCKET_TIMEOUT);
    httpClient.getParams().setParameter("http.connection.timeout", SOCKET_TIMEOUT);
    httpClient.getParams().setParameter("http.connection-manager.timeout", new Long(SOCKET_TIMEOUT));
    httpClient.getParams().setParameter("http.protocol.head-body-timeout", SOCKET_TIMEOUT);
    institutes2collectionId = new Hashtable<String, String>();
    fillInstitutes2collectionIds();
  }
  
  public void deleteConfigurationFiles(Collection collection) {
    String collectionId = collection.getId();
    String rdfRessourcesFileStr = Constants.getInstance().getMetadataDir() + "/resources" + "/" + collectionId + ".rdf";
    File rdfRessourcesFile = new File(rdfRessourcesFileStr);
    FileUtils.deleteQuietly(rdfRessourcesFile);
    LOGGER.info("Project configuration file: " + rdfRessourcesFileStr + " successfully deleted");
    String xmlConfFileStr = Constants.getInstance().getMetadataDir() + "/resources-addinfo" + "/" + collectionId + ".xml";
    File xmlConfFile = new File(rdfRessourcesFileStr);
    FileUtils.deleteQuietly(xmlConfFile);
    LOGGER.info("Project configuration file: " + xmlConfFileStr + " successfully deleted");
  }
  
  public ArrayList<MetadataRecord> getMetadataRecords(Collection collection, boolean generateId) throws ApplicationException {
    String collectionId = collection.getId();
    ArrayList<MetadataRecord> mdRecords = collectionMdRecords.get(collectionId);
    if (generateId || mdRecords == null) {
      String metadataRdfDir = Constants.getInstance().getMetadataDir() + "/resources";
      File rdfRessourcesFile = new File(metadataRdfDir + "/" + collectionId + ".rdf");
      mdRecords = getMetadataRecordsByRdfFile(collection, rdfRessourcesFile, null, generateId);
    } 
    if (mdRecords.size() == 0)
      return null;
    else 
      return mdRecords;
  }

  public ArrayList<MetadataRecord> getMetadataRecords(Collection collection, Database db, boolean generateId) throws ApplicationException {
    ArrayList<MetadataRecord> dbMdRecords = null;
    String dbType = db.getType();
    if (dbType.equals("eXist")) {
      dbMdRecords = getMetadataRecordsEXist(collection, db, generateId);
    } else if (dbType.equals("crawl")) {
      dbMdRecords = getMetadataRecordsCrawl(collection, db, generateId);
    } else { 
      // nothing
    }
    return dbMdRecords;
  }
  
  private ArrayList<MetadataRecord> getMetadataRecordsEXist(Collection collection, Database db, boolean generateId) throws ApplicationException {
    String collectionId = collection.getId();
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    Integer maxIdcounter = null;
    String urlStr = db.getUrl();
    ProjectManager pm = ProjectManager.getInstance();
    MetadataRecord mdRecord = getNewMdRecord(urlStr); 
    mdRecord.setSystem("eXist");
    mdRecord.setCollectionNames(collectionId);
    if (generateId) {
      maxIdcounter = pm.getStatusMaxId();  // find the highest value, so that each following id is a real new id
      mdRecord.setId(maxIdcounter); // collections wide id
    }
    mdRecord = createMainFieldsMetadataRecord(mdRecord, collection, null);
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
        mdRecordEXist.setCollectionNames(collectionId);
        if (generateId) {
          maxIdcounter++;
          mdRecordEXist.setId(maxIdcounter); // collections wide id
        }
        mdRecordEXist = createMainFieldsMetadataRecord(mdRecordEXist, collection, db);
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
  
  private ArrayList<MetadataRecord> getMetadataRecordsCrawl(Collection collection, Database db, boolean generateId) throws ApplicationException {
    String collectionId = collection.getId();
    ProjectManager pm = ProjectManager.getInstance();
    Integer maxIdcounter = null;
    if (generateId) {
      maxIdcounter = pm.getStatusMaxId();  // find the highest value, so that each following id is a real new id
    }
    Date lastModified = new Date();
    String startUrl = db.getUrl();
    ArrayList<String> excludes = db.getExcludes();
    Integer depth = db.getDepth();
    Crawler crawler = new Crawler(startUrl, depth, excludes);
    ArrayList<MetadataRecord> crawledMdRecords = crawler.crawl();
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    for (int i=0; i<crawledMdRecords.size(); i++) {
      String crawlUrlStr = crawledMdRecords.get(i).getWebUri();
      MetadataRecord crawledMdRecord = getNewMdRecord(crawlUrlStr); // with docId and webUri
      crawledMdRecord.setCollectionNames(collectionId);
      if (generateId) {
        maxIdcounter++;
        crawledMdRecord.setId(maxIdcounter); // collections wide id
      }
      crawledMdRecord = createMainFieldsMetadataRecord(crawledMdRecord, collection, db);
      crawledMdRecord.setLastModified(lastModified);
      crawledMdRecord.setSystem("crawl");
      String dbLanguage = db.getLanguage();
      if (dbLanguage != null)
        crawledMdRecord.setLanguage(dbLanguage);
      String mimeType = crawledMdRecord.getType();
      if (mimeType != null)
        mdRecords.add(crawledMdRecord);
    }
    if (generateId) {
      pm.setStatusMaxId(maxIdcounter);
    }
    return mdRecords;
  }
  
  public int fetchMetadataRecordsOai(String dirName, Collection collection, Database db) throws ApplicationException {
    // fetch the oai records and write them to xml files
    /*
    StringBuilder xmlOaiStrBuilder = new StringBuilder();
    String oaiServerUrl = db.getUrl();
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
      writeOaiDbXmlFile(dirName, collection, db, xmlOaiStrBuilder, 1);
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
          writeOaiDbXmlFile(dirName, collection, db, xmlOaiStrBuilder, fileCounter);
          xmlOaiStrBuilder = new StringBuilder();
          fileCounter++;
        }
        resumptionTokenCounter++;
      }
      // write the last resumptionTokens
      if (xmlOaiStrBuilder.length() > 0)
        writeOaiDbXmlFile(dirName, collection, db, xmlOaiStrBuilder, fileCounter);
    }
    */   // TODO
    // convert the xml files to rdf files
    int recordsCount = convertDbXmlFiles(dirName, collection, db);
    return recordsCount;
  }

  private void writeOaiDbXmlFile(String dirName, Collection collection, Database db, StringBuilder recordsStrBuilder, int fileCounter) throws ApplicationException {
    StringBuilder xmlOaiStrBuilder = new StringBuilder();
    String oaiSet = db.getOaiSet();
    String xmlOaiDbFileName = dirName + "/" + collection.getId() + "-" + db.getName() + "-" + fileCounter + ".xml";
    xmlOaiStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    if (oaiSet == null)
      xmlOaiStrBuilder.append("<!-- Resources of OAI-PMH-Server: " + db.getName() + " (Url: " + db.getUrl() + ") -->\n");
    else 
      xmlOaiStrBuilder.append("<!-- Resources of OAI-PMH-Server: " + db.getName() + " (Url: " + db.getUrl() + ", Set: " + oaiSet + ") -->\n");
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
  
  private int convertDbXmlFiles(String dbFilesDirName, Collection collection, Database db) throws ApplicationException {
    int counter = 0;
    File dbFilesDir = new File(dbFilesDirName);
    String xmlDbFileFilterName = collection.getId() + "-" + db.getName() + "*.xml"; 
    FileFilter xmlDbFileFilter = new WildcardFileFilter(xmlDbFileFilterName);
    File[] xmlDbFiles = dbFilesDir.listFiles(xmlDbFileFilter);
    if (xmlDbFiles != null && xmlDbFiles.length > 0) {
      Arrays.sort(xmlDbFiles, new Comparator<File>() {
        public int compare(File f1, File f2) {
          return f1.getName().compareToIgnoreCase(f2.getName());
        }
      }); 
      // delete the old db rdf files
      String rdfFileFilterName = collection.getId() + "-" + db.getName() + "*.rdf"; 
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
        int fileRecordsCount = convertDbXmlFile(dbFilesDirName, collection, db, xmlDbFile);
        counter = counter + fileRecordsCount;
      }
    }
    return counter;
  }
  
  private int convertDbXmlFile(String dbFilesDirName, Collection collection, Database db, File xmlDbFile) throws ApplicationException {
    int counter = 0;
    try {
      StringBuilder rdfRecordsStrBuilder = new StringBuilder();
      String rdfHeaderStr = getRdfHeaderStr(collection);
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
          appendRow2rdf(rdfRecordsStrBuilder, collection, db, row);
          counter++;
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

  private void appendRow2rdf(StringBuilder rdfStrBuilder, Collection collection, Database db, Row row) throws ApplicationException {
    String collectionId = collection.getId();
    String collectionRdfId = collection.getRdfId();
    String mainLanguage = collection.getMainLanguage();
    String mainResourcesTableId = db.getMainResourcesTableId();
    String webIdPreStr = db.getWebIdPreStr();
    String webIdAfterStr = db.getWebIdAfterStr();
    String dbType = db.getType();
    String id = row.getFirstFieldValue(mainResourcesTableId);
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
    // special handling of edoc oai records
    if (collectionId.equals("edoc")) {
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
          String institutesStr = xQueryEvaluator.evaluateAsString(edocMetadataHtmlStr, "//*:th[text() = 'Institutes:']/following-sibling::*:td/*:a/text()");
          if (institutesStr != null && ! institutesStr.isEmpty()) {
            institutesStr = institutesStr.trim().replaceAll("BBAW / ", "");
            String collRdfId = getCollectionRdfId(institutesStr);
            if (collRdfId != null)
              collectionRdfId = collRdfId;
          } else {
            // if no institutes string is given, then they are mapped to "akademiepublikationen1"
            Collection c = CollectionReader.getInstance().getCollection("akademiepublikationen1");
            collectionRdfId = c.getRdfId();
          }
        }
      }
    }
    rdfStrBuilder.append("<rdf:Description rdf:about=\"" + rdfWebId + "\">\n");
    rdfStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
    rdfStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + collectionRdfId + "\"/>\n");
    rdfStrBuilder.append("  <dc:identifier rdf:resource=\"" + rdfWebId + "\">" + id + "</dc:identifier>\n");
    if (dbType.equals("oai"))
      rdfStrBuilder.append("  <dc:type>oaiRecord</dc:type>\n");
    else
      rdfStrBuilder.append("  <dc:type>dbRecord</dc:type>\n");  // for huge oai collections such as jdg (performance gain in inserting them into lucene) 
    String dbFieldCreator = db.getDbField("creator");
    if (dbFieldCreator != null) {
      ArrayList<String> fieldValues = row.getFieldValues(dbFieldCreator);
      if (fieldValues != null && ! fieldValues.isEmpty()) {
        for (int i=0; i<fieldValues.size(); i++) {
          String fieldValue = fieldValues.get(i);
          if (collectionId.equals("jdg")) {
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
                  if (collectionId.equals("jdg")) {
                    int indexLs = s.lastIndexOf("▴Ls");
                    if (indexLs != -1)
                      s = s.substring(indexLs + 3).trim();
                  }
                  s = StringUtils.deresolveXmlEntities(s);
                  if (db.getName().equals("edoc"))
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
      URL rdfRessourcesFileUrl = recordsFile.toURI().toURL();
      XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
      XdmValue xmdValueResources = xQueryEvaluator.evaluate(rdfRessourcesFileUrl, "/records/record");
      XdmSequenceIterator xmdValueResourcesIterator = xmdValueResources.iterator();
      if (xmdValueResources != null && xmdValueResources.size() > 0) {
        mdRecords = new ArrayList<MetadataRecord>();
        while (xmdValueResourcesIterator.hasNext()) {
          XdmItem xdmItemResource = xmdValueResourcesIterator.next();
          String recordXmlString = xdmItemResource.toString();
          MetadataRecord mdRecord = MetadataRecord.fromRecordXmlStr(xQueryEvaluator, recordXmlString);
          mdRecords.add(mdRecord);
        }
      }
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecords;
  }
  
  public ArrayList<MetadataRecord> getMetadataRecordsByRdfFile(Collection collection, File rdfRessourcesFile, Database db, boolean generateId) throws ApplicationException {
    if (! rdfRessourcesFile.exists())
      return null;
    String collectionId = collection.getId();
    String xmlDumpFileStr = rdfRessourcesFile.getPath().replaceAll("\\.rdf", ".xml");
    File xmlDumpFile = new File(xmlDumpFileStr);
    Date xmlDumpFileLastModified = new Date(xmlDumpFile.lastModified());
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    try {
      URL rdfRessourcesFileUrl = rdfRessourcesFile.toURI().toURL();
      String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
      XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
      XdmValue xmdValueResources = xQueryEvaluator.evaluate(rdfRessourcesFileUrl, namespaceDeclaration + "/rdf:RDF/rdf:Description[rdf:type/@rdf:resource = 'http://purl.org/dc/terms/BibliographicResource']");
      XdmSequenceIterator xmdValueResourcesIterator = xmdValueResources.iterator();
      ProjectManager pm = ProjectManager.getInstance();
      Integer maxIdcounter = pm.getStatusMaxId();  // find the highest value, so that each following id is a real new id
      if (xmdValueResources != null && xmdValueResources.size() > 0) {
        while (xmdValueResourcesIterator.hasNext()) {
          maxIdcounter++;
          XdmItem xdmItemResource = xmdValueResourcesIterator.next();
          String xdmItemResourceStr = xdmItemResource.toString();
          MetadataRecord mdRecord = getMdRecord(collection, xQueryEvaluator, xdmItemResourceStr);  // get the mdRecord out of rdf string
          if (mdRecord != null) {
            if (mdRecord.getCollectionNames() == null)
              mdRecord.setCollectionNames(collectionId);
            mdRecord.setId(maxIdcounter); // collections wide id
            mdRecord = createMainFieldsMetadataRecord(mdRecord, collection, db);
            // if it is a record: set lastModified to the date of the harvested dump file; if it is a normal web record: set it to now (harvested now)
            Date lastModified = new Date();
            if (mdRecord.isRecord()) {
              lastModified = xmlDumpFileLastModified;
            }
            mdRecord.setLastModified(lastModified);
            if(mdRecord.isDirectory()) {
              String fileDirUrlStr = mdRecord.getWebUri();
              if (fileDirUrlStr != null) {
                ArrayList<MetadataRecord> mdRecordsDir = new ArrayList<MetadataRecord>();
                String fileExtensions = "html;htm;pdf;doc";
                if (mdRecord.getType() != null)
                  fileExtensions = mdRecord.getType();
                String fileDirStr = null;
                try {
                  URL fileDirUrl = new URL(fileDirUrlStr);
                  String fileDirUrlHost = fileDirUrl.getHost();
                  String fileDirUrlPath = fileDirUrl.getFile();
                  fileDirStr = Constants.getInstance().getExternalDataDir() + "/" + fileDirUrlHost + fileDirUrlPath;
                } catch (MalformedURLException e) {
                  throw new ApplicationException(e);
                }
                java.util.Collection<File> files = getFiles(fileDirStr, fileExtensions);
                Iterator<File> filesIter = files.iterator();
                while (filesIter.hasNext()) {
                  maxIdcounter++;
                  File file = filesIter.next();
                  String fileAbsolutePath = file.getAbsolutePath();
                  String fileRelativePath = fileAbsolutePath.replaceFirst(fileDirStr, "");
                  String webUrl = fileDirUrlStr + fileRelativePath;
                  String uri = "file:" + fileAbsolutePath;
                  MetadataRecord mdRecordDirEntry = getNewMdRecord(webUrl);
                  mdRecordDirEntry.setCollectionNames(collectionId);
                  mdRecordDirEntry.setId(maxIdcounter); // collections wide id
                  mdRecordDirEntry = createMainFieldsMetadataRecord(mdRecordDirEntry, collection, null);
                  mdRecordDirEntry.setLastModified(lastModified);
                  mdRecordDirEntry.setUri(uri);
                  mdRecordsDir.add(mdRecordDirEntry);
                }
                mdRecords.addAll(mdRecordsDir);
              }
            }
            // do not add the eXistDir or directory itself as a record (only the normal resource mdRecord) 
            if (! mdRecord.isDirectory()) {
              mdRecords.add(mdRecord);
            }
          } 
        }
      }
      pm.setStatusMaxId(maxIdcounter);
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecords;
  }
  
  public void writeMetadataRecords(Collection collection, ArrayList<MetadataRecord> mdRecords, File outputFile, String type) throws ApplicationException {
    StringBuilder strBuilder = new StringBuilder();
    if (type.equals("rdf")) {
      String rdfHeaderStr = getRdfHeaderStr(collection);
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

  private String getRdfHeaderStr(Collection collection) {
    String collectionRdfId = collection.getRdfId();
    StringBuilder rdfStrBuilder = new StringBuilder();
    rdfStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    rdfStrBuilder.append("<rdf:RDF \n");
    rdfStrBuilder.append("   xmlns=\"" + collectionRdfId + "\" \n");
    rdfStrBuilder.append("   xml:base=\"" + collectionRdfId + "\" \n");
    rdfStrBuilder.append("   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n");
    rdfStrBuilder.append("   xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" \n");
    rdfStrBuilder.append("   xmlns:dc=\"http://purl.org/dc/elements/1.1/\" \n");
    rdfStrBuilder.append("   xmlns:dcterms=\"http://purl.org/dc/terms/\" \n");
    rdfStrBuilder.append("   xmlns:gnd=\"http://d-nb.info/standards/elementset/gnd#\" \n");
    rdfStrBuilder.append("   xmlns:ore=\"http://www.openarchives.org/ore/terms/\" \n"); 
    rdfStrBuilder.append(">\n");
    return rdfStrBuilder.toString();    
  }
  
  public File[] getRdfDbResourcesFiles(Collection collection, Database db) throws ApplicationException {
    String dbDumpsDirName = Constants.getInstance().getExternalDataDbDumpsDir();
    File dbDumpsDir = new File(dbDumpsDirName);
    String rdfFileFilterName = collection.getId() + "-" + db.getName() + "*.rdf"; 
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
  
  private MetadataRecord createMainFieldsMetadataRecord(MetadataRecord mdRecord, Collection collection, Database db) throws ApplicationException {
    try {
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
          if (! isDbRecord) {
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
      String collectionId = collection.getId();
      String normalizedUriPath = normalize(uriPath);
      String docId = "/" + collectionId + normalizedUriPath;
      String mimeType = getMimeType(docId);
      if (mimeType == null) {  // last chance: try it with tika
        try {
          mimeType = tika.detect(uri);
        } catch (IOException e) {
          LOGGER.error("get mime type failed for: " + webUriStr);
          e.printStackTrace();
        }
      }
      mdRecord.setDocId(docId);
      if (mdRecord.getUri() == null)
        mdRecord.setUri(webUriStr);
      if (mdRecord.getType() == null)
        mdRecord.setType(mimeType); 
      // if no language is set then take the mainLanguage of the project
      if (mdRecord.getLanguage() == null) {
        String mainLanguage = collection.getMainLanguage();
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

  private List<String> extractDocumentUrls(String collectionDataUrl, ArrayList<String> excludes) {
    List<String> documentUrls = null;
    if (! collectionDataUrl.equals("")){
      PathExtractor extractor = new PathExtractor();
      documentUrls = extractor.initExtractor(collectionDataUrl, excludes);
    }
    return documentUrls;
  }

  private java.util.Collection<File> getFiles(String fileDirStr, String fileExtensions) throws ApplicationException {
    String[] fileExts = fileExtensions.split(";");
    for (int i=0; i<fileExts.length; i++) {
      String fileExt = fileExts[i];
      fileExts[i] = "." + fileExt;
    }
    SuffixFileFilter suffixFileFilter = new SuffixFileFilter(fileExts);
    File fileDir = new File(fileDirStr);
    java.util.Collection<File> files = FileUtils.listFiles(fileDir, suffixFileFilter, TrueFileFilter.INSTANCE);
    return files;
  }
  
  private String getMimeType(String docId) {
    String mimeType = null;
    FileNameMap fileNameMap = URLConnection.getFileNameMap();  // map with 53 entries such as "application/xml"
    mimeType = fileNameMap.getContentTypeFor(docId);
    return mimeType;
  }

  private MetadataRecord getMdRecord(Collection collection, XQueryEvaluator xQueryEvaluator, String xmlRdfStr) throws ApplicationException {
    String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
    // download url of resource
    String resourceIdUrlStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "string(/rdf:Description/dc:identifier/@rdf:resource)");
    resourceIdUrlStr = StringUtils.resolveXmlEntities(resourceIdUrlStr);
    if (resourceIdUrlStr == null || resourceIdUrlStr.trim().isEmpty()) {
      resourceIdUrlStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "string(/rdf:Description/@rdf:about)");
      if (resourceIdUrlStr == null || resourceIdUrlStr.trim().isEmpty())
        LOGGER.error("No identifier given in resource: \"" + xmlRdfStr + "\"");
    }
    MetadataRecord mdRecord = getNewMdRecord(resourceIdUrlStr);
    // web url of resource
    String resourceIdentifierStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:identifier/text()");
    if (resourceIdentifierStr != null && ! resourceIdentifierStr.isEmpty()) {
      resourceIdentifierStr = StringUtils.resolveXmlEntities(resourceIdentifierStr);
      try {
        new URL(resourceIdentifierStr); // test if identifier is a url, if so then set it as the webUri
        mdRecord.setWebUri(resourceIdentifierStr);
      } catch (MalformedURLException e) {
        // nothing
      }
      mdRecord.setUri(resourceIdUrlStr);
    }
    if (collection.getId().equals("edoc")) {
      String projectRdfStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "string(/rdf:Description/dcterms:isPartOf/@rdf:resource)");
      Collection c = CollectionReader.getInstance().getCollectionByProjectRdfId(projectRdfStr);
      if (c != null) {
        String collId = c.getId();
        mdRecord.setCollectionNames(collId);
      }
    }
    String type = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:type/text()");
    if (type != null)
      mdRecord.setSystem(type);  // e.g. "eXistDir" or "dbRecord" or "oaiRecord" or "crawl" 
    XdmValue xmdValueAuthors = xQueryEvaluator.evaluate(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:creator");
    if (xmdValueAuthors != null && xmdValueAuthors.size() > 0) {
      XdmSequenceIterator xmdValueAuthorsIterator = xmdValueAuthors.iterator();
      ArrayList<Person> authors = new ArrayList<Person>();
      while (xmdValueAuthorsIterator.hasNext()) {
        XdmItem xdmItemAuthor = xmdValueAuthorsIterator.next();
        String name = xdmItemAuthor.getStringValue();
        if (name != null && ! name.isEmpty()) {
          name = name.replaceAll("\n|\\s\\s+", " ").trim();
          Person author = new Person(name);
          authors.add(author);
        } 
        String xdmItemAuthorStr = xdmItemAuthor.toString();
        String resourceId = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:creator/@*:resource)");
        if (resourceId != null && ! resourceId.isEmpty()) {
          Person creator = CollectionReader.getInstance().getPerson(resourceId.trim());
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
      creator = StringUtils.deresolveXmlEntities(creator.trim());
      creatorDetails = creatorDetails + "\n</persons>";
      if (creator != null && ! creator.isEmpty())
        mdRecord.setCreator(creator);
      if (authors != null && ! authors.isEmpty())
        mdRecord.setCreatorDetails(creatorDetails);
    }
    XdmValue xmdValueTitle = xQueryEvaluator.evaluate(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:title|/rdf:Description/dcterms:alternative");
    XdmSequenceIterator xmdValueTitleIterator = xmdValueTitle.iterator();
    if (xmdValueTitle != null && xmdValueTitle.size() > 0) {
      String title = "";
      while (xmdValueTitleIterator.hasNext()) {
        XdmItem xdmItemTitle = xmdValueTitleIterator.next();
        String xdmItemTitleStr = xdmItemTitle.getStringValue().trim();
        if (xmdValueTitle.size() == 1)
          title = xdmItemTitleStr;
        else
          title = title + xdmItemTitleStr + ". ";
      }
      title = StringUtils.resolveXmlEntities(title);
      mdRecord.setTitle(title);
    }
    String publisher = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:publisher/text()");
    if (publisher != null)
      publisher = StringUtils.resolveXmlEntities(publisher.trim());
    mdRecord.setPublisher(publisher);
    String subject = xQueryEvaluator.evaluateAsStringValueJoined(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:subject", "###");
    if (subject != null)
      subject = StringUtils.resolveXmlEntities(subject.trim());
    mdRecord.setSubject(subject);
    String subjectSwd = xQueryEvaluator.evaluateAsStringValueJoined(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:subject[@xsi:type = 'SWD']", ",");
    if (subjectSwd != null && ! subjectSwd.isEmpty()) {
      subjectSwd = StringUtils.resolveXmlEntities(subjectSwd.trim());
      if (subjectSwd.endsWith(","))
        subjectSwd = subjectSwd.substring(0, subjectSwd.length() - 1); 
    }
    mdRecord.setSwd(subjectSwd);
    String subjectDdc = xQueryEvaluator.evaluateAsStringValueJoined(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:subject[@xsi:type = 'dcterms:DDC']", ",");
    if (subjectDdc != null && ! subjectDdc.isEmpty()) {
      subjectDdc = StringUtils.resolveXmlEntities(subjectDdc.trim());
      mdRecord.setDdc(subjectDdc);
    }
    XdmValue xmdValueDcTerms = xQueryEvaluator.evaluate(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dcterms:subject");
    XdmSequenceIterator xmdValueDcTermsIterator = xmdValueDcTerms.iterator();
    if (xmdValueDcTerms != null && xmdValueDcTerms.size() > 0) {
      String subjectControlledDetails = "<subjects>";
      while (xmdValueDcTermsIterator.hasNext()) {
        XdmItem xdmItemDcTerm = xmdValueDcTermsIterator.next();
        /* e.g.:
         * <dcterms:subject>
             <rdf:Description rdf:about="http://de.dbpedia.org/resource/Kategorie:Karl_Marx">
               <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#Concept"/>
               <rdfs:label>Karl Marx</rdfs:label>
             </rdf:description>
           </dcterms:subject>
         */
        String xdmItemDcTermStr = xdmItemDcTerm.toString();
        subjectControlledDetails = subjectControlledDetails + "\n" + xdmItemDcTermStr;
      }
      subjectControlledDetails = subjectControlledDetails + "\n</subjects>";
      mdRecord.setSubjectControlledDetails(subjectControlledDetails);
    }
    String dateStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:date/text()");
    Date date = null;
    if (dateStr != null && ! dateStr.equals("")) {
      dateStr = StringUtils.resolveXmlEntities(dateStr.trim());
      dateStr = new Util().toYearStr(dateStr);  // test if possible etc
      if (dateStr != null) {
        try {
          date = new Util().toDate(dateStr + "-01-01T00:00:00.000Z");
        } catch (Exception e) {
          // nothing
        }
      }
    }
    mdRecord.setDate(date);
    String language = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:language/text()");
    mdRecord.setLanguage(language);
    String mimeType = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:format/text()");
    mdRecord.setType(mimeType);
    String abstractt = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:abstract/text()");
    if (abstractt != null)
      abstractt = StringUtils.resolveXmlEntities(abstractt.trim());
    mdRecord.setDescription(abstractt);
    String pagesStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:extent/text()");
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
      else if (schemaName.equals("html"))
        mdRecord = getMetadataRecordHtml(xQueryEvaluator, srcUrl, mdRecord);
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

  private MetadataRecord getMetadataRecordHtml(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/html/head");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.identifier']/@content)");
      if (identifier != null && ! identifier.isEmpty())
        identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.creator']/@content)");
      boolean isProper = isProper("author", creator);
      if (! isProper)
        creator = "";
      if (creator != null) {
        creator = StringUtils.deresolveXmlEntities(creator.trim());
        if (creator.isEmpty())
          creator = null;
      }      
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.title']/@content)");
      if (title == null || title.isEmpty()) {
        title = xQueryEvaluator.evaluateAsString(metadataXmlStr, "string(/head/title)");
      }
      if (title != null) {
        title = StringUtils.deresolveXmlEntities(title.trim());
        if (title.isEmpty())
          title = null;
      }
      String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.language']/@content)");
      if (language != null && language.isEmpty())
        language = null;
      if (language != null && ! language.isEmpty()) {
        language = StringUtils.deresolveXmlEntities(language.trim());
        language = Language.getInstance().getISO639Code(language);
      }
      String publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.publisher']/@content)");
      if (publisher != null) {
        publisher = StringUtils.deresolveXmlEntities(publisher.trim());
        if (publisher.isEmpty())
          publisher = null;
      }
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.date']/@content)");
      Date date = null; 
      if (yearStr != null) {
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
      String subject = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.subject']/@content)");
      if (subject != null) {
        subject = StringUtils.deresolveXmlEntities(subject.trim());
        if (subject.isEmpty())
          subject = null;
      }
      String description = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.description']/@content)");
      if (description != null) {
        description = StringUtils.deresolveXmlEntities(description.trim());
        if (description.isEmpty())
          description = null;
      }
      String rights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.rights']/@content)");
      if (rights != null) {
        rights = StringUtils.deresolveXmlEntities(rights.trim());
        if (rights.isEmpty())
          rights = null;
      }
      String license = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.license']/@content)");
      if (license != null) {
        license = StringUtils.deresolveXmlEntities(license.trim());
        if (license.isEmpty())
          license = null;
      }
      String accessRights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.accessRights']/@content)");
      if (accessRights != null) {
        accessRights = StringUtils.deresolveXmlEntities(accessRights.trim());
        if (accessRights.isEmpty())
          accessRights = null;
      }
      mdRecord.setIdentifier(identifier);
      mdRecord.setLanguage(language);
      if (mdRecord.getCreator() == null)
        mdRecord.setCreator(creator);
      if (mdRecord.getTitle() == null)
        mdRecord.setTitle(title);
      if (mdRecord.getPublisher() == null)
        mdRecord.setPublisher(publisher);
      if (mdRecord.getRights() == null)
        mdRecord.setRights(rights);
      if (mdRecord.getDate() == null)
        mdRecord.setDate(date);
      if (mdRecord.getSubject() == null)
        mdRecord.setSubject(subject);
      if (mdRecord.getDescription() == null)
        mdRecord.setDescription(description);
      if (mdRecord.getLicense() == null)
        mdRecord.setLicense(license);
      if (mdRecord.getAccessRights() == null)
        mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("html");
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

  private String getCollectionRdfId(String institutesStr) throws ApplicationException {
    String retStr = null;
    String collectionId = institutes2collectionId.get(institutesStr);
    if (collectionId == null)
      collectionId = "akademiepublikationen1"; // some institutes (e.g. ALLEA) have no collectionId, they are mapped to "akademiepublikationen1"
    Collection c = CollectionReader.getInstance().getCollection(collectionId);
    if (c != null)
      retStr = c.getRdfId();
    return retStr;
  }
  
  private void fillInstitutes2collectionIds() {
    institutes2collectionId.put("Berlin-Brandenburgische Akademie der Wissenschaften", "akademiepublikationen1");
    institutes2collectionId.put("Veröffentlichungen von Akademiemitgliedern", "akademiepublikationen2");
    institutes2collectionId.put("Veröffentlichungen von Akademiemitarbeitern", "akademiepublikationen3");
    institutes2collectionId.put("Veröffentlichungen der Vorgängerakademien", "akademiepublikationen4");
    institutes2collectionId.put("Veröffentlichungen externer Institutionen", "akademiepublikationen5");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Psychologisches Denken und psychologische Praxis", "pd");
    institutes2collectionId.put("Akademienvorhaben Strukturen und Transformationen des Wortschatzes der ägyptischen Sprache. Text- und Wissenskultur im alten Ägypten", "aaew");
    institutes2collectionId.put("Akademienvorhaben Altägyptisches Wörterbuch", "aaew");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Berliner Akademiegeschichte im 19. und 20. Jahrhundert", "bag");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Wissenschaftliche Politikberatung in der Demokratie", "wpd");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Die Herausforderung durch das Fremde", "fremde");
    institutes2collectionId.put("Initiative Wissen für Entscheidungsprozesse", "wfe");
    institutes2collectionId.put("Initiative Qualitätsbeurteilung in der Wissenschaft", "quali");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Gentechnologiebericht", "gen");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Wissenschaften und Wiedervereinigung", "ww");
    institutes2collectionId.put("Akademienvorhaben Leibniz-Edition Berlin", "leibber");
    institutes2collectionId.put("Akademienvorhaben Deutsches Wörterbuch von Jacob Grimm und Wilhelm Grimm", "dwb");
    institutes2collectionId.put("Akademienvorhaben Census of Antique Works of Art and Architecture Known in the Renaissance", "census");
    institutes2collectionId.put("Akademienvorhaben Protokolle des Preußischen Staatsministeriums Acta Borussica", "ab");
    institutes2collectionId.put("Akademienvorhaben Berliner Klassik", "bk");
    institutes2collectionId.put("Akademienvorhaben Alexander-von-Humboldt-Forschung", "avh");
    institutes2collectionId.put("Akademienvorhaben Leibniz-Edition Potsdam", "leibpots");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Globaler Wandel", "globw");
    institutes2collectionId.put("Akademienvorhaben Jahresberichte für deutsche Geschichte", "jdg");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Die Welt als Bild", "wab");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Optionen zukünftiger industrieller Produktionssysteme", "ops");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Frauen in Akademie und Wissenschaft", "frauen");
    institutes2collectionId.put("Akademienvorhaben Corpus Coranicum", "coranicum");
    institutes2collectionId.put("Initiative Telota", "telota");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Gemeinwohl und Gemeinsinn", "gg");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Bildkulturen", "bild");
    institutes2collectionId.put("Akademienvorhaben Monumenta Germaniae Historica", "mgh");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe LandInnovation", "li");
    institutes2collectionId.put("Akademienvorhaben Marx-Engels-Gesamtausgabe", "mega");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Strukturbildung und Innovation", "sui");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Sprache des Rechts, Vermitteln, Verstehen, Verwechseln", "srvvv");
    institutes2collectionId.put("Akademienvorhaben Die Griechischen Christlichen Schriftsteller", "gcs");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Gesundheitsstandards", "gs");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Humanprojekt", "hum");
    institutes2collectionId.put("Akademienvorhaben Turfanforschung", "turfan");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Gegenworte - Hefte für den Disput über Wissen", "gw");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Strategien zur Abfallenergieverwertung", "sza");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Exzellenzinitiative", "exzellenz");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Funktionen des Bewusstseins", "fb");
    institutes2collectionId.put("Drittmittelprojekt Ökosystemleistungen", "oeko");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Zukunft des wissenschaftlichen Kommunikationssystems", "zwk");
    institutes2collectionId.put("Akademienvorhaben Preußen als Kulturstaat", "ab");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe EUTENA - Zur Zukunft technischer und naturwissenschaftlicher Bildung in Europa", "eutena");
    institutes2collectionId.put("Akademienvorhaben Digitales Wörterbuch der Deutschen Sprache", "dwds");
    institutes2collectionId.put("Akademienvorhaben Griechisches Münzwerk", "gmw");
    institutes2collectionId.put("Interdisziplinäre Arbeitsgruppe Klinische Forschung in vulnerablen Populationen", "kf");
    institutes2collectionId.put("Akademienvorhaben Die alexandrinische und antiochenische Bibelexegese in der Spätantike", "bibel");
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
