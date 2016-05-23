package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.general.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class ProjectManager {
  private static Logger LOGGER = Logger.getLogger(ProjectManager.class);
  private static DateFormat US_DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
  private static ProjectManager projectManager;
  private CollectionReader collectionReader;
  private Harvester harvester;
  private Annotator annotator;
  private Indexer indexer;
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
      harvester = Harvester.getInstance();
      annotator = Annotator.getInstance();
      // indexer = Indexer.getInstance();  // TODO
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

  public void setModified(Date modified) throws ApplicationException {
    statusFileDoc.select("status > modified").first().text(modified.toString());
    writeStatusFile();
  }
  
  public void update(String collectionId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(collectionId);
    harvest(collection);
    annotate(collection);
    Date now = new Date();
    index(collection);
    setModified(now);
  }
  
  public void harvest(Collection collection) throws ApplicationException {
    harvester.harvest(collection);
  }
  
  public void annotate(Collection collection) throws ApplicationException {
    annotator.annotate(collection);
  }
  
  public void index(Collection collection) throws ApplicationException {
    // vorher die collection im Index löschen und dann neu erzeugen
    // indexer.indexCollection(collection); // TODO
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
}
