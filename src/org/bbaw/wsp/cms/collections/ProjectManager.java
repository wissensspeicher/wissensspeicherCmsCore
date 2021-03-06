package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class ProjectManager {
  private static Logger LOGGER = Logger.getLogger(ProjectManager.class);
  private static DateFormat US_DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
  private static ProjectManager projectManager;
  private ProjectReader projectReader;
  private MetadataHandler metadataHandler;
  private Harvester harvester;
  private Annotator annotator;
  private Indexer indexer;
  private File statusFile;
  private Document statusFileDoc;
  
  public static void main(String[] args) throws ApplicationException {
    try {
      if (args != null && args.length != 0) {
        String operation = args[0];
        String projectIds = null;
        ArrayList<Project> projects = null;
        ProjectManager pm = ProjectManager.getInstance();
        if (args.length > 1) {
          projectIds = args[1];
          projects = pm.getProjects(projectIds);
        }
        String[] options = null;
        if (args.length > 2) {
          options = new String[1];
          options[0] = args[2];
        }
        if (operation.equals("update") && projects != null) {
          pm.update(projects, options);
        } else if (operation.equals("harvest") && projects != null) {
          pm.harvest(projects, options);
        } else if (operation.equals("annotate") && projects != null) {
          pm.annotate(projects, options);
        } else if (operation.equals("index") && projects != null) {
          pm.index(projects, options);
        } else if (operation.equals("delete") && projects != null) {
          pm.delete(projects);
        } else if (operation.equals("indexProjects")) {
          pm.indexProjects();
        } else if (operation.equals("updateCycle")) {
          pm.updateCycle();
        } else if (operation.equals("createBaseProjects")) {
          pm.createBaseProjects();
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
      projectReader = ProjectReader.getInstance();
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

  public void indexProjects() throws ApplicationException {
    ArrayList<Project> projects = projectReader.getProjects();
    IndexHandler.getInstance().indexProjects(projects);  
    LOGGER.info("Index " + projects.size() + " projects successfully into data directory " + Constants.getInstance().getLuceneProjectsDir());
  }
  
  public ArrayList<Project> getProjects(String projectIdsStr) {
    ArrayList<Project> projects = null;
    if (projectIdsStr != null) {
      if (projectIdsStr.contains("-")) {
        String projectFrom = projectIdsStr.substring(0, projectIdsStr.indexOf("-"));
        String projectTo = projectIdsStr.substring(projectIdsStr.indexOf("-") + 1, projectIdsStr.length());
        projects = projectReader.getProjects(projectFrom, projectTo);
      } else if (projectIdsStr.contains(",")) {
        String[] projectIds = projectIdsStr.split(",");
        projects = projectReader.getProjects(projectIds);
      } else {
        String[] projectIds = new String[1];
        projectIds[0] = projectIdsStr;
        projects = projectReader.getProjects(projectIds);
      }
    }
    return projects;
  }
  
  public void createBaseProjects() throws ApplicationException {
    ArrayList<Project> projects = projectReader.getBaseProjects();
    update(projects, null);
  }
  
  public void updateCycle() throws ApplicationException {
    ArrayList<Project> projects = projectReader.getUpdateCycleProjects();
    update(projects, null);
  }
  
  public void update(String projectIds) throws ApplicationException {
    ArrayList<Project> projects = getProjects(projectIds);
    projectReader.updateProjectMetadata(projects);  // make project config files (rdf + xml) up to date (copy them from userMetadataDir if changed there) 
    projects = getProjects(projectIds);  // read them again from projectReader
    update(projects, null);
  }
  
  public void update(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Project> projects = projectReader.getProjects(projectId1, projectId2);
    projectReader.updateProjectMetadata(projects);  // make project config files (rdf + xml) up to date (copy them from userMetadataDir if changed there) 
    projects = projectReader.getProjects(projectId1, projectId2);  // read them again from projectReader
    update(projects, null);
  }
  
  public void update(ArrayList<Project> projects, String[] options) throws ApplicationException {
    String projectIds = getIdsStr(projects);
    LOGGER.info("Update of project(s) (\"" + projectIds + "\") into data directory: " + Constants.getInstance().getDataDir());
    harvest(projects, options);
    annotate(projects, options);
    index(projects, options);
    Date now = new Date();
    setStatusModified(now);  
    LOGGER.info("Update finished");
  }
  
  public void harvest(String projectIds) throws ApplicationException {
    ArrayList<Project> projects = getProjects(projectIds);
    harvest(projects, null);
  }
  
  public void harvest(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Project> projects = projectReader.getProjects(projectId1, projectId2);
    harvest(projects, null);
  }
  
  private void harvest(ArrayList<Project> projects, String[] options) throws ApplicationException {
    String projectIds = getIdsStr(projects);
    LOGGER.info("Harvest of project(s) (\"" + projectIds + "\") into data directory: " + Constants.getInstance().getHarvestDir());
    for (int i=0; i<projects.size(); i++) {
      Project project = projects.get(i);
      harvest(project, options);
    }
    LOGGER.info("Harvest finished");
  }

  private void harvest(Project project, String[] options) throws ApplicationException {
    harvester = Harvester.getInstance();
    harvester.setOptions(options);
    harvester.harvest(project);
    Date now = new Date();
    String projectId = project.getId();
    setStatusModified(projectId, "harvest", now);  
  }
  
  public void annotate(String projectIds) throws ApplicationException {
    ArrayList<Project> projects = getProjects(projectIds);
    annotate(projects, null);
  }
  
  public void annotate(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Project> projects = projectReader.getProjects(projectId1, projectId2);
    annotate(projects, null);
  }
  
  private void annotate(ArrayList<Project> projects, String[] options) throws ApplicationException {
    String projectIds = getIdsStr(projects);
    LOGGER.info("Annotate project(s) (\"" + projectIds + "\") into data directory: " + Constants.getInstance().getAnnotationsDir());
    for (int i=0; i<projects.size(); i++) {
      Project project = projects.get(i);
      annotate(project, options);
    }
    LOGGER.info("Annotate finished");
  }

  private void annotate(Project project, String[] options) throws ApplicationException {
    annotator = Annotator.getInstance();
    annotator.setOptions(options);
    annotator.annotate(project);
    Date now = new Date();
    String projectId = project.getId();
    setStatusModified(projectId, "annotate", now);  
  }
  
  public void index(String projectIds) throws ApplicationException {
    ArrayList<Project> projects = getProjects(projectIds);
    index(projects, null);
  }
  
  public void index(String[] projectIds) throws ApplicationException {
    ArrayList<Project> projects = projectReader.getProjects(projectIds);
    index(projects, null);
  }
  
  public void index(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Project> projects = projectReader.getProjects(projectId1, projectId2);
    index(projects, null);
  }
  
  private void index(ArrayList<Project> projects, String[] options) throws ApplicationException {
    String projectIds = getIdsStr(projects);
    LOGGER.info("Index project(s) (\"" + projectIds + "\") into data directory: " + Constants.getInstance().getIndexDir());
    for (int i=0; i<projects.size(); i++) {
      Project project = projects.get(i);
      index(project, options);
    }
    indexer.reopenIndexHandler();  // TODO done only because taxonomxReader does not work if something is changed in lucene
    LOGGER.info("Index finished");
  }

  private void index(Project project, String[] options) throws ApplicationException {
    indexer = Indexer.getInstance();
    indexer.setOptions(options);
    indexer.index(project);
    Date now = new Date();
    String projectId = project.getId();
    setStatusModified(projectId, "index", now);  
  }
  
  public void delete(String projectIds) throws ApplicationException {
    ArrayList<Project> projects = getProjects(projectIds);
    delete(projects);
  }
  
  public void delete(String[] projectIds) throws ApplicationException {
    ArrayList<Project> projects = projectReader.getProjects(projectIds);
    delete(projects);
  }
  
  public void delete(String projectId1, String projectId2) throws ApplicationException {
    ArrayList<Project> projects = projectReader.getProjects(projectId1, projectId2);
    delete(projects);
  }
  
  private void delete(ArrayList<Project> projects) throws ApplicationException {
    String projectIds = getIdsStr(projects);
    LOGGER.info("Delete project(s) (\"" + projectIds + "\"");
    for (int i=0; i<projects.size(); i++) {
      Project project = projects.get(i);
      delete(project);
    }
    LOGGER.info("Delete finished");
  }

  private void delete(Project project) throws ApplicationException {
    harvester = Harvester.getInstance();
    harvester.delete(project);
    annotator = Annotator.getInstance();
    annotator.delete(project);
    indexer = Indexer.getInstance();
    indexer.delete(project);
    deleteConfiguration(project);
  }
  
  private void deleteConfiguration(Project project) throws ApplicationException {
    metadataHandler.deleteConfigurationFiles(project);
    projectReader.remove(project);
  }
  
  private String getIdsStr(ArrayList<Project> projects) {
    String ids = null;
    if (projects != null && ! projects.isEmpty()) {
      ids = "";
      for (int i=0; i<projects.size(); i++) {
        String projectId = projects.get(i).getId();
        ids = ids + projectId + ",";
      }
      ids = ids.substring(0, ids.length()-1);
    }
    return ids;
  }
  
  public void writeOaiProviderFiles(Project project) throws ApplicationException {
    int counter = 0;
    String projectId = project.getId();
    LOGGER.info("Generate OaiProviderFiles of project: " + projectId + " ...");
    ArrayList<MetadataRecord> mdRecords = harvester.getMetadataRecordsByRecordsFile(project);
    XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
    if (mdRecords != null) {
      writeOaiProviderFiles(mdRecords, xQueryEvaluator);
    }
    ArrayList<Database> projectDBs = project.getDatabases();
    if (projectDBs != null) {
      for (int i=0; i<projectDBs.size(); i++) {
        Database db = projectDBs.get(i);
        String dbType = db.getType();
        if (dbType != null && (dbType.equals("crawl") || dbType.equals("eXist") || dbType.equals("oai"))) {
          ArrayList<MetadataRecord> dbMdRecords = harvester.getMetadataRecordsByRecordsFile(project, db);
          if (dbMdRecords != null) {
            writeOaiProviderFiles(dbMdRecords, xQueryEvaluator);
          }
        } else {
          File[] rdfDbResourcesFiles = metadataHandler.getRdfDbResourcesFiles(project, db);
          if (rdfDbResourcesFiles != null && rdfDbResourcesFiles.length > 0) {
            for (int j = 0; j < rdfDbResourcesFiles.length; j++) {
              File rdfDbResourcesFile = rdfDbResourcesFiles[j];
              ArrayList<MetadataRecord> dbMdRecords = metadataHandler.getMetadataRecordsByRdfFile(project, rdfDbResourcesFile, db, false);
              if (dbMdRecords != null) {
                writeOaiProviderFiles(dbMdRecords, xQueryEvaluator);
              }
            }
          }
        }
      }
    }
    LOGGER.info("Project: " + projectId + " with " + counter + " records indexed");
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

  private void setStatusModified(Date modified) throws ApplicationException {
    Element modifiedElem = statusFileDoc.select("status > modified").first();
    if (modifiedElem != null) {
      modifiedElem.text(modified.toString());
    } else {
      Element newModifiedElem = new Element(Tag.valueOf("modified"), "");
      newModifiedElem.text(modified.toString());
      Element statusElem = statusFileDoc.select("status").first();
      statusElem.appendChild(newModifiedElem);
    }
    writeStatusFile();
  }
  
  private void setStatusModified(String projectId, String operation, Date modified) throws ApplicationException {
    Element operationModifiedElem = statusFileDoc.select("status > project[id=" + projectId + "] > " + operation).first();
    if (operationModifiedElem != null) {
      operationModifiedElem.text(modified.toString());
    } else {
      Element newOperationModifiedElem = new Element(Tag.valueOf(operation), "");
      newOperationModifiedElem.text(modified.toString());
      Element projectElem = statusFileDoc.select("status > project[id=" + projectId + "]").first();
      if (projectElem == null) {
        projectElem = new Element(Tag.valueOf("project"), "");
        projectElem.attr("id", projectId);
        projectElem.appendChild(newOperationModifiedElem);
        Element statusElem = statusFileDoc.select("status").first();
        statusElem.appendChild(projectElem);
      } else {
        projectElem.appendChild(newOperationModifiedElem);
      }
    }
    writeStatusFile();
  }
  
  public String getStatusProjectXmlStr(String projectIds) throws ApplicationException {
    ArrayList<Project> projects = getProjects(projectIds);
    String[] projectIdsTmp = new String[projects.size()];
    for (int i=0; i<projects.size(); i++) {
      projectIdsTmp[i] = projects.get(i).getId();
    }
    return getStatusProjectXmlStr(projectIdsTmp);
  }
  
  public String getStatusProjectXmlStr() throws ApplicationException {
    readStatusFile();
    Elements projects = statusFileDoc.select("project");
    Collections.sort(projects, new Comparator<Element>() {
      @Override
      public int compare(Element e1, Element e2) {
        return e1.id().compareTo(e2.id());
      }
    });
    return projects.toString();
  }
  
  private String getStatusProjectXmlStr(String[] projectIds) throws ApplicationException {
    readStatusFile();
    String regExprProjects = "";
    for (int i=0; i<projectIds.length; i++) {
      String projectId = projectIds[i];
      regExprProjects = regExprProjects + "^" + projectId + "$|";
    }
    regExprProjects = regExprProjects.substring(0, regExprProjects.length() - 1);
    Elements projects = statusFileDoc.select("project[id~=" + regExprProjects + "]");
    Collections.sort(projects, new Comparator<Element>() {
      @Override
      public int compare(Element e1, Element e2) {
        return e1.id().compareTo(e2.id());
      }
    });
    return projects.toString();
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
    String projectId = mdRecord.getProjectId();
    String oaiDirStr = Constants.getInstance().getOaiDir();
    int id = mdRecord.getId();
    String docId = mdRecord.getDocId();
    String fileIdentifier = docId;
    if (id != -1)
      fileIdentifier = "" + id;
    String oaiDcFileName = fileIdentifier + "_oai_dc.xml";
    File oaiDcFile = new File(oaiDirStr + "/" + projectId + "/" + oaiDcFileName);
    try {
      FileUtils.writeStringToFile(oaiDcFile, dcStr, "utf-8");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
}
