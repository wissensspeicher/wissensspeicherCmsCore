package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.general.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class ProjectManager {
  private static Logger LOGGER = Logger.getLogger(ProjectManager.class);
  private static DateFormat US_DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
  private static ProjectManager projectManager;
  private CollectionReader collectionReader;
  private MetadataHandler metadataHandler;
  private Harvester harvester;
  private Annotator annotator;
  private LocalIndexer indexer;
  private File statusFile;
  private Document statusFileDoc;
  
  public static void main(String[] args) throws ApplicationException {
    try {
      if (args != null && args.length != 0) {
        String operation = args[0];
        String projectId1 = args[1];
        String projectId2 = null;
        if (args.length > 2)
          projectId2 = args[2];
        if (args.length > 2)
          projectId2 = args[2];
        if (operation.equals("update") && projectId1 != null && projectId2 == null) {
          LOGGER.info("Update of project (\"" + projectId1 + "\") into data directory: " + Constants.getInstance().getDataDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.update(projectId1);
          LOGGER.info("Update finished");
        } else if (operation.equals("update") && projectId1 != null && projectId2 != null) {
          LOGGER.info("Update of projects (\"" + projectId1 + "\" - \"" + projectId2 + "\") into data directory: " + Constants.getInstance().getDataDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.update(projectId1, projectId2);
          LOGGER.info("Update finished");
        } else if (operation.equals("harvest") && projectId1 != null && projectId2 == null) {
          LOGGER.info("Harvest of project (\"" + projectId1 + "\") into data directory: " + Constants.getInstance().getHarvestDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.harvest(projectId1);
          LOGGER.info("Harvest finished");
        } else if (operation.equals("harvest") && projectId1 != null && projectId2 != null) {
          LOGGER.info("Harvest of projects (\"" + projectId1 + "\" - \"" + projectId2 + "\") into data directory: " + Constants.getInstance().getHarvestDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.harvest(projectId1, projectId2);
          LOGGER.info("Harvest finished");
        } else if (operation.equals("annotate") && projectId1 != null && projectId2 == null) {
          LOGGER.info("Annotate project (\"" + projectId1 + "\") into data directory: " + Constants.getInstance().getAnnotationsDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.annotate(projectId1);
          LOGGER.info("Annotate finished");
        } else if (operation.equals("annotate") && projectId1 != null && projectId2 != null) {
          LOGGER.info("Annotate projects (\"" + projectId1 + "\" - \"" + projectId2 + "\") into data directory: " + Constants.getInstance().getAnnotationsDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.annotate(projectId1, projectId2);
          LOGGER.info("Annotate finished");
        } else if (operation.equals("index") && projectId1 != null && projectId2 == null) {
          LOGGER.info("Index project (\"" + projectId1 + "\") into data directory: " + Constants.getInstance().getIndexDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.index(projectId1);
          LOGGER.info("Index finished");
        } else if (operation.equals("index") && projectId1 != null && projectId2 != null) {
          LOGGER.info("Index projects (\"" + projectId1 + "\" - \"" + projectId2 + "\") into data directory: " + Constants.getInstance().getIndexDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.index(projectId1, projectId2);
          LOGGER.info("Index finished");
        }
      } 
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ProjectManager getInstance() throws ApplicationException {
    if(projectManager == null) {
      projectManager = new ProjectManager();
      projectManager.init();
    }
    return projectManager;
  }
  
  private void init() throws ApplicationException {
    try {
      collectionReader = CollectionReader.getInstance();
      metadataHandler = MetadataHandler.getInstance();
      harvester = Harvester.getInstance();
      annotator = Annotator.getInstance();
      indexer = LocalIndexer.getInstance();
      statusFile = new File(Constants.getInstance().getDataDir() + "/status/status.xml");
      if (! statusFile.exists()) {
        String statusFileContentXmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        statusFileContentXmlStr = statusFileContentXmlStr + "<status>\n";
        Date now = new Date();
        statusFileContentXmlStr = statusFileContentXmlStr + "<creation>" + now.toString() + "</creation>\n";
        statusFileContentXmlStr = statusFileContentXmlStr + "<modified>" + now.toString() + "</modified>\n";
        statusFileContentXmlStr = statusFileContentXmlStr + "<maxid>1</maxid>\n";
        statusFileContentXmlStr = statusFileContentXmlStr + "</status>";
        FileUtils.writeStringToFile(statusFile, statusFileContentXmlStr, "utf-8");
      }
      statusFileDoc = Jsoup.parse(statusFile, "utf-8");
      statusFileDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
      statusFileDoc.outputSettings().escapeMode(EscapeMode.xhtml);
      statusFileDoc.outputSettings().prettyPrint(true);
      statusFileDoc.outputSettings().indentAmount(2);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  /**
   * Update of all collections from that startingCollection onwards till endingCollectionId alphabetically
   * @throws ApplicationException
   * 
   * e.g. update(aaew, dspin)):  update all resources of all projects alphabetically named between 
   * "aaew" and "dspin" inclusive
   */
  public void update(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectId1, projectId2);
    update(collections);
  }
  
  private void update(ArrayList<Collection> collections) throws ApplicationException {
    harvest(collections);
    annotate(collections);
    index(collections);
  }
  
  public void update(String projectId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(projectId);
    harvest(collection);
    annotate(collection);
    index(collection);
    Date now = new Date();
    setModified(collection, now);  
  }
  
  public void harvest(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectId1, projectId2);
    harvest(collections);
  }
  
  private void harvest(ArrayList<Collection> collections) throws ApplicationException {
    for (int i=0; i<collections.size(); i++) {
      Collection collection = collections.get(i);
      harvest(collection);
    }
  }

  public void harvest(String projectId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(projectId);
    harvest(collection);
  }
  
  private void harvest(Collection collection) throws ApplicationException {
    harvester.harvest(collection);
  }
  
  public void annotate(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectId1, projectId2);
    annotate(collections);
  }
  
  private void annotate(ArrayList<Collection> collections) throws ApplicationException {
    for (int i=0; i<collections.size(); i++) {
      Collection collection = collections.get(i);
      annotate(collection);
    }
  }

  public void annotate(String projectId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(projectId);
    annotate(collection);
  }
  
  private void annotate(Collection collection) throws ApplicationException {
    annotator.annotate(collection);
  }
  
  public void index(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectId1, projectId2);
    index(collections);
  }
  
  private void index(ArrayList<Collection> collections) throws ApplicationException {
    for (int i=0; i<collections.size(); i++) {
      Collection collection = collections.get(i);
      index(collection);
    }
  }

  public void index(String projectId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(projectId);
    index(collection);
  }
  
  private void index(Collection collection) throws ApplicationException {
    indexer.delete(collection);
    indexer.index(collection);
  }
  
  public void writeOaiProviderFiles(Collection collection) throws ApplicationException {
    int counter = 0;
    String collId = collection.getId();
    LOGGER.info("Generate OaiProviderFiles of project: " + collId + " ...");
    ArrayList<MetadataRecord> mdRecords = harvester.getMetadataRecordsByRecordsFile(collection);
    XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
    if (mdRecords != null) {
      writeOaiProviderFiles(mdRecords, xQueryEvaluator);
    }
    ArrayList<Database> collectionDBs = collection.getDatabases();
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database db = collectionDBs.get(i);
        String dbType = db.getType();
        if (dbType != null && (dbType.equals("crawl") || dbType.equals("eXist") || dbType.equals("oai"))) {
          ArrayList<MetadataRecord> dbMdRecords = harvester.getMetadataRecordsByRecordsFile(collection, db);
          if (dbMdRecords != null) {
            writeOaiProviderFiles(dbMdRecords, xQueryEvaluator);
          }
        } else {
          File[] rdfDbResourcesFiles = metadataHandler.getRdfDbResourcesFiles(collection, db);
          if (rdfDbResourcesFiles != null && rdfDbResourcesFiles.length > 0) {
            for (int j = 0; j < rdfDbResourcesFiles.length; j++) {
              File rdfDbResourcesFile = rdfDbResourcesFiles[j];
              ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecordsByRdfFile(collection, rdfDbResourcesFile, db);
              if (dbMdRecords != null) {
                writeOaiProviderFiles(dbMdRecords, xQueryEvaluator);
              }
            }
          }
        }
      }
    }
    LOGGER.info("Project: " + collId + " with " + counter + " records indexed");
  }

  public Integer getMaxId() {
    String maxIdStr = statusFileDoc.select("status > maxid").text();
    return Integer.valueOf(maxIdStr);
  }

  public void setMaxId(Integer maxId) throws ApplicationException {
    statusFileDoc.select("status > maxid").first().text(maxId.toString());
    writeStatusFile();
  }
  
  public Date getModified() throws ApplicationException {
    Date modified = null;
    try {
      String modifiedStr = statusFileDoc.select("status > modified").text();
      modified = US_DATE_FORMAT.parse((String) modifiedStr);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return modified;
  }

  public void setModified(Collection collection, Date modified) throws ApplicationException {
    statusFileDoc.select("status > modified").first().text(modified.toString()); // TODO nur das Projekt auf modified setzen
    writeStatusFile();
  }
  
  private void writeStatusFile() throws ApplicationException {
    try {
      String statusFileContentXmlStr = statusFileDoc.select("body").first().html();
      statusFileContentXmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + statusFileContentXmlStr;
      FileUtils.writeStringToFile(statusFile, statusFileContentXmlStr, "utf-8");
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  private void writeOaiProviderFiles(ArrayList<MetadataRecord> mdRecords, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      writeOaiproviderFile(mdRecord, xQueryEvaluator);
    }    
  }

  private void writeOaiproviderFile(MetadataRecord mdRecord, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    StringBuilder dcStrBuilder = new StringBuilder();  // Dublin core content string builder
    dcStrBuilder.append("<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">\n");
    String projectUrl = mdRecord.getWebUri();
    if (projectUrl == null)
      projectUrl = mdRecord.getIdentifier();
    if (projectUrl == null)
      projectUrl = mdRecord.getDocId();
    if (projectUrl != null && ! projectUrl.trim().isEmpty()) {
      projectUrl = StringUtils.deresolveXmlEntities(projectUrl);
      dcStrBuilder.append("  <dc:identifier>" + projectUrl + "</dc:identifier>\n");
    }
    String creator = mdRecord.getCreator();
    if (creator != null) {
      creator = StringUtils.deresolveXmlEntities(creator);
      dcStrBuilder.append("  <dc:creator>" + creator + "</dc:creator>\n");
    }
    String title = mdRecord.getTitle();
    if (title != null) {
      title = StringUtils.deresolveXmlEntities(title);
      dcStrBuilder.append("  <dc:title>" + title + "</dc:title>\n");
    }
    String publisher = mdRecord.getPublisher();
    if (publisher != null) {
      publisher = StringUtils.deresolveXmlEntities(publisher);
      dcStrBuilder.append("  <dc:publisher>" + publisher + "</dc:publisher>\n");
    }
    String language = mdRecord.getLanguage();
    if (language != null)
      dcStrBuilder.append("  <dc:language>" + language + "</dc:language>\n");
    String subject = mdRecord.getSubject();
    if (subject != null) {
      subject = StringUtils.deresolveXmlEntities(subject);
      if (subject.contains("###")) {
        String[] subjects = subject.split("###");
        for (int i=0; i<subjects.length; i++) {
          String s = subjects[i];
          dcStrBuilder.append("  <dc:subject>" + s + "</dc:subject>\n");
        }
      } else {
        dcStrBuilder.append("  <dc:subject>" + subject + "</dc:subject>\n");
      }
    }
    String subjectControlledDetails = mdRecord.getSubjectControlledDetails();
    if (subjectControlledDetails != null) {
      String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
      String dcTermsSubjectsStr = xQueryEvaluator.evaluateAsStringValueJoined(subjectControlledDetails, namespaceDeclaration + "/subjects/dcterms:subject/rdf:Description/rdfs:label", "###");
      String[] dcTermsSubjects = dcTermsSubjectsStr.split("###");
      for (int i=0; i<dcTermsSubjects.length; i++) {
        String s = dcTermsSubjects[i].trim();
        s = StringUtils.deresolveXmlEntities(s);
        if (! s.isEmpty())
          dcStrBuilder.append("  <dc:subject>" + s + "</dc:subject>\n");
      }
    }
    Date pubDate = mdRecord.getDate();
    if (pubDate != null) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(pubDate);
      int year = cal.get(Calendar.YEAR);
      int month = cal.get(Calendar.MONTH) + 1;
      String monthStr = String.valueOf(month);
      if (month < 10)
        monthStr = "0" + monthStr;
      int day = cal.get(Calendar.DAY_OF_MONTH);
      String dayStr = String.valueOf(day);
      if (day < 10)
        dayStr = "0" + dayStr;
      String dateStr = year + "-" + monthStr + "-" + dayStr;
      int hours = cal.get(Calendar.HOUR_OF_DAY);
      String hoursStr = String.valueOf(hours);
      if (hours < 10)
        hoursStr = "0" + hoursStr;
      int minutes = cal.get(Calendar.MINUTE);
      String minutesStr = String.valueOf(minutes);
      if (minutes < 10)
        minutesStr = "0" + minutesStr;
      int seconds = cal.get(Calendar.SECOND);
      String secondsStr = String.valueOf(seconds);
      if (seconds < 10)
        secondsStr = "0" + secondsStr;
      String timeStr = hoursStr + ":" + minutesStr + ":" + secondsStr;
      dcStrBuilder.append("  <dc:date>" + dateStr + "T" + timeStr + "Z" + "</dc:date>\n"); // e.g. "2013-07-03T12:09:59Z"
    }
    String type = "Text";
    dcStrBuilder.append("  <dc:type>" + type + "</dc:type>\n");
    String rights = mdRecord.getRights();
    if (rights != null)
      dcStrBuilder.append("  <dc:rights>" + rights + "</dc:rights>\n"); 
    dcStrBuilder.append("</oai_dc:dc>\n");
    String dcStr = dcStrBuilder.toString();
    String collectionId = mdRecord.getCollectionNames();
    String oaiDirStr = Constants.getInstance().getOaiDir();
    int id = mdRecord.getId();
    String docId = mdRecord.getDocId();
    String fileIdentifier = docId;
    if (id != -1)
      fileIdentifier = "" + id;
    String oaiDcFileName = fileIdentifier + "_oai_dc.xml";
    File oaiDcFile = new File(oaiDirStr + "/" + collectionId + "/" + oaiDcFileName);
    try {
      FileUtils.writeStringToFile(oaiDcFile, dcStr, "utf-8");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
}
