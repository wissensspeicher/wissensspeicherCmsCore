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
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.parser.Tag;

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
        String projectIdsStr = args[1];
        String[] projectIds = null;
        String projectFrom = null;
        String projectTo = null;
        if (projectIdsStr != null) {
          if (projectIdsStr.contains("-")) {
            projectFrom = projectIdsStr.substring(0, projectIdsStr.indexOf("-"));
            projectTo = projectIdsStr.substring(projectIdsStr.indexOf("-") + 1, projectIdsStr.length());
          } else if (projectIdsStr.contains(",")) {
            projectIds = projectIdsStr.split(",");
          } else {
            projectIds = new String[1];
            projectIds[0] = projectIdsStr;
          }
        }
        if (operation.equals("update") && projectIds != null) {
          LOGGER.info("Update of project (\"" + projectIdsStr + "\") into data directory: " + Constants.getInstance().getDataDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.update(projectIds);
          LOGGER.info("Update finished");
        } else if (operation.equals("update") && projectFrom != null && projectTo != null) {
          LOGGER.info("Update of projects (\"" + projectIdsStr + "\") into data directory: " + Constants.getInstance().getDataDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.update(projectFrom, projectTo);
          LOGGER.info("Update finished");
        } else if (operation.equals("harvest") && projectIds != null) {
          LOGGER.info("Harvest of project (\"" + projectIdsStr + "\") into data directory: " + Constants.getInstance().getHarvestDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.harvest(projectIds);
          LOGGER.info("Harvest finished");
        } else if (operation.equals("harvest") && projectFrom != null && projectTo != null) {
          LOGGER.info("Harvest of projects (\"" + projectIdsStr + "\") into data directory: " + Constants.getInstance().getHarvestDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.harvest(projectFrom, projectTo);
          LOGGER.info("Harvest finished");
        } else if (operation.equals("annotate") && projectIds != null) {
          LOGGER.info("Annotate project (\"" + projectIdsStr + "\") into data directory: " + Constants.getInstance().getAnnotationsDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.annotate(projectIds);
          LOGGER.info("Annotate finished");
        } else if (operation.equals("annotate") && projectFrom != null && projectTo != null) {
          LOGGER.info("Annotate projects (\"" + projectIdsStr + "\") into data directory: " + Constants.getInstance().getAnnotationsDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.annotate(projectFrom, projectTo);
          LOGGER.info("Annotate finished");
        } else if (operation.equals("index") && projectIds != null) {
          LOGGER.info("Index project (\"" + projectIdsStr + "\") into data directory: " + Constants.getInstance().getIndexDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.index(projectIds);
          LOGGER.info("Index finished");
        } else if (operation.equals("index") && projectFrom != null && projectTo != null) {
          LOGGER.info("Index projects (\"" + projectIdsStr + "\") into data directory: " + Constants.getInstance().getIndexDir());
          ProjectManager pm = ProjectManager.getInstance();
          pm.index(projectFrom, projectTo);
          LOGGER.info("Index finished");
        } else if (operation.equals("delete") && projectIds != null) {
          LOGGER.info("Delete project (\"" + projectIdsStr + "\"");
          ProjectManager pm = ProjectManager.getInstance();
          pm.delete(projectIds);
          LOGGER.info("Delete finished");
        } else if (operation.equals("delete") && projectFrom != null && projectTo != null) {
          LOGGER.info("Delete projects (\"" + projectIdsStr + "\"");
          ProjectManager pm = ProjectManager.getInstance();
          pm.delete(projectFrom, projectTo);
          LOGGER.info("Delete finished");
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
      readStatusFile();
      statusFileDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
      statusFileDoc.outputSettings().escapeMode(EscapeMode.xhtml);
      statusFileDoc.outputSettings().prettyPrint(true);
      statusFileDoc.outputSettings().indentAmount(2);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  private void readStatusFile() throws ApplicationException {
    try {
      statusFileDoc = Jsoup.parse(statusFile, "utf-8");
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  public void update(String[] projectIds) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectIds);
    update(collections);
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
    setStatusModified(projectId, now);  
  }
  
  public void harvest(String[] projectIds) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectIds);
    harvest(collections);
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
    harvester = Harvester.getInstance();
    harvester.harvest(collection);
  }
  
  public void annotate(String[] projectIds) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectIds);
    annotate(collections);
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
    annotator = Annotator.getInstance();
    annotator.annotate(collection);
  }
  
  public void index(String[] projectIds) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectIds);
    index(collections);
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
    indexer = LocalIndexer.getInstance();
    indexer.delete(collection);
    indexer.index(collection);
  }
  
  public void delete(String[] projectIds) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectIds);
    delete(collections);
  }
  
  public void delete(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections(projectId1, projectId2);
    delete(collections);
  }
  
  private void delete(ArrayList<Collection> collections) throws ApplicationException {
    for (int i=0; i<collections.size(); i++) {
      Collection collection = collections.get(i);
      index(collection);
    }
  }

  public void delete(String projectId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(projectId);
    delete(collection);
  }
  
  private void delete(Collection collection) throws ApplicationException {
    harvester = Harvester.getInstance();
    harvester.delete(collection);
    annotator = Annotator.getInstance();
    annotator.delete(collection);
    indexer = LocalIndexer.getInstance();
    indexer.delete(collection);
    deleteConfiguration(collection);
  }
  
  private void deleteConfiguration(Collection collection) throws ApplicationException {
    metadataHandler.deleteConfigurationFiles(collection);
    collectionReader.remove(collection);
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
              ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecordsByRdfFile(collection, rdfDbResourcesFile, db, false);
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

  public Integer getStatusMaxId() throws ApplicationException {
    readStatusFile();
    String maxIdStr = statusFileDoc.select("status > maxid").text();
    return Integer.valueOf(maxIdStr);
  }

  public void setStatusMaxId(Integer maxId) throws ApplicationException {
    statusFileDoc.select("status > maxid").first().text(maxId.toString());
    writeStatusFile();
  }
  
  public Date getStatusModified() throws ApplicationException {
    Date modified = null;
    try {
      readStatusFile();
      String modifiedStr = statusFileDoc.select("status > modified").text();
      modified = US_DATE_FORMAT.parse((String) modifiedStr);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return modified;
  }

  public void setStatusModified(String projectId, Date modified) throws ApplicationException {
    Element modifiedElem = statusFileDoc.select("status > project[id=" + projectId + "] > modified").first();
    if (modifiedElem != null) {
      modifiedElem.text(modified.toString());
    } else {
      Element newModifiedElem = new Element(Tag.valueOf("modified"), "");
      newModifiedElem.text(modified.toString());
      Element projectElem = statusFileDoc.select("status > project[id=" + projectId + "]").first();
      if (projectElem == null) {
        projectElem = new Element(Tag.valueOf("project"), "");
        projectElem.attr("id", projectId);
        Element newCreationElem = new Element(Tag.valueOf("creation"), "");
        newCreationElem.text(modified.toString());
        projectElem.appendChild(newCreationElem);
        projectElem.appendChild(newModifiedElem);
        Element statusElem = statusFileDoc.select("status").first();
        statusElem.appendChild(projectElem);
      } else {
        projectElem.appendChild(newModifiedElem);
      }
    }
    writeStatusFile();
  }
  
  public String getStatusProjectXmlStr(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Collection> projects = collectionReader.getCollections(projectId1, projectId2);
    String[] projectIdsTmp = new String[projects.size()];
    for (int i=0; i<projects.size(); i++) {
      projectIdsTmp[i] = projects.get(i).getId();
    }
    return getStatusProjectXmlStr(projectIdsTmp);
  }
  
  public String getStatusProjectXmlStr(String[] projectIds) throws ApplicationException {
    readStatusFile();
    String projectSelector = "";
    for (int i=0; i<projectIds.length; i++) {
      String projectId = projectIds[i];
      projectSelector = projectSelector + "project[id=" + projectId + "], ";
    }
    projectSelector = projectSelector.substring(0, projectSelector.length() - 2);
    String projectStatusXmlStr = statusFileDoc.select(projectSelector).outerHtml();
    return projectStatusXmlStr;
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
