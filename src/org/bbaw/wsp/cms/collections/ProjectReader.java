package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.general.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class ProjectReader {
  private static Logger LOGGER = Logger.getLogger(ProjectReader.class);
  private static ProjectReader projectReader; 
  private ArrayList<String> globalExcludes;
  private HashMap<String, Person> normdataPersons;
  private HashMap<String, Project> projects;  // key is id string and value is project
  private HashMap<String, Project> projectsByRdfId;  // key is projectRdfId string and value is project
	
	public static ProjectReader getInstance() throws ApplicationException {
		if (projectReader == null) {
			projectReader = new ProjectReader();
			projectReader.init();
		}
		return projectReader;
	}

	private void init() throws ApplicationException {
    projects = new HashMap<String, Project>();
    projectsByRdfId = new HashMap<String, Project>();
    globalExcludes = new ArrayList<String>();
    readConfFiles();
    readNormdataFiles();
    readMdsystemXmlFile();
    LOGGER.info("Project reader initialized with " + projects.size() + " projects");
	}
	
	public ArrayList<Project> getProjects() {
		ArrayList<Project> projects = new ArrayList<Project>();
		java.util.Collection<Project> projectValues = this.projects.values();
		for (Project project : projectValues) {
			projects.add(project);
		}
    Comparator<Project> projectComparator = new Comparator<Project>() {
      public int compare(Project p1, Project p2) {
        return p1.getId().compareTo(p2.getId());
      }
    };
    Collections.sort(projects, projectComparator);
		return projects;
	}

  public ArrayList<Project> getBaseProjects() {
    ArrayList<Project> retProjects = new ArrayList<Project>();
    java.util.Collection<Project> values = projects.values();
    for (Project project : values) {
      if (isBaseProject(project))
        retProjects.add(project);
    }
    return retProjects;
  }

	/**
	 * Delivers projects from that startingCollectionId onwards till endingCollectionId alphabetically
	 * @param startingCollectionId
	 * @param endingCollectionId
	 * @return ArrayList<Project>
	 */
  public ArrayList<Project> getProjects(String startingProjectId, String endingProjectId) {
    ArrayList<Project> retProjects = new ArrayList<Project>();
    boolean start = false;
    boolean end = false;
    ArrayList<Project> projects = projectReader.getProjects();
    for (Project project : projects) {
      String projectId = project.getId();
      if (startingProjectId.equals(projectId)) {
        start = true;
      }
      if (start && ! end) {
        boolean isBaseProject = isBaseProject(project);
        if (! isBaseProject) {
          retProjects.add(project);
        }
      }
      if (endingProjectId.equals(projectId)) {
        end = true;
        break;
      }
    }
    return retProjects;
  }

  public ArrayList<Project> getProjects(String[] projectIds) {
    ArrayList<Project> retProjects = new ArrayList<Project>();
    for (int i=0; i<projectIds.length; i++) {
      String projectId = projectIds[i];
      Project p = getProject(projectId);
      retProjects.add(p);
    }
    return retProjects;
  }

  public Project getProject(String projectId) {
		Project p = projects.get(projectId);
		return p;
	}

  public Project getProjectByRdfId(String projectRdfId) {
    Project p = projectsByRdfId.get(projectRdfId);
    return p;
  }

  public ArrayList<String> getGlobalExcludes() {
    return globalExcludes;
  }
  
  public ArrayList<Project> getUpdateCycleProjects() throws ApplicationException {
    ArrayList<Project> updateCycleProjects = new ArrayList<Project>();
    ArrayList<Project> projects = getProjects();
    for (int i=0; i<projects.size(); i++) {
      Project project = projects.get(i);
      boolean isBaseProject = isBaseProject(project);
      boolean isAkademiepublikationsProject = isAkademiepublikationsProject(project);
      if (! isBaseProject && ! isAkademiepublikationsProject) {
        if (projectFileChangedByChecksum(project, "rdf") || projectFileChangedByChecksum(project, "xml")) {  
          updateCycleProjects.add(project);
        } else if (updateCycleReached(project)) { 
          updateCycleProjects.add(project);
        }
      }
    }
    if (updateCycleProjects.isEmpty())
      return null;
    else
      return updateCycleProjects;
  }
  
  public void updateProject(Project project) throws ApplicationException {
    boolean isBaseProject = isBaseProject(project);
    boolean isAkademiepublikationsProject = isAkademiepublikationsProject(project);
    if (! isBaseProject && ! isAkademiepublikationsProject) {
      boolean projectFileRdfChanged = projectFileChangedByChecksum(project, "rdf");
      boolean projectFileXmlChanged = projectFileChangedByChecksum(project, "xml");
      if (projectFileRdfChanged || projectFileXmlChanged) {
        if (projectFileRdfChanged) {
          updateUserMetadata(project, "rdf");
        }
        if (projectFileXmlChanged) {
          updateUserMetadata(project, "xml");
        }
      }
      Date now = new Date();
      projectReader.setConfigFilesLastModified(project, now);
    }
  }
  
  private void readNormdataFiles() {
    normdataPersons = new HashMap<String, Person>();
    String personNormdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.normdata.persons.rdf";
    File personNormdataFile = new File(personNormdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(personNormdataFile, "utf-8");
      Elements personElems = normdataFileDoc.select("rdf|RDF > rdf|Description > rdf|type[rdf:resource*=Person]");
      if (! personElems.isEmpty()) {
        for (int i=0; i< personElems.size(); i++) {
          Person person = new Person();
          Element personElem = personElems.get(i).parent();
          String surname = personElem.select("rdf|Description > foaf|familyName").text();
          String forename = personElem.select("rdf|Description > foaf|givenName").text();
          if (surname != null && ! surname.isEmpty()) {
            person.setSurname(surname);
            person.setName(surname);
          }
          if (forename != null && ! forename.isEmpty()) {
            person.setForename(forename);
            if (person.getSurname() != null) 
              person.setName(person.getSurname() + ", " + forename);
          }
          String aboutId = personElem.select("rdf|Description").attr("rdf:about");
          normdataPersons.put(aboutId, person);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + personNormdataFile + " failed");
      e.printStackTrace();
    }
  }

	private void readMdsystemXmlFile() {
	  URL mdsystemXmlFileUrl = null;
	  try {
      String mdsystemXmlFileName = Constants.getInstance().getMdsystemConfFile();
      File mdsystemXmlFile = new File(mdsystemXmlFileName);
      Document mdsystemXmlDoc = Jsoup.parse(mdsystemXmlFile, "utf-8");
      Elements excludes = mdsystemXmlDoc.select("mdsystem > crawler > excludes > exclude");
      if (excludes != null) {
        for (Element exclude : excludes) {
          String excludeStr = exclude.text();
          globalExcludes.add(excludeStr);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + mdsystemXmlFileUrl + " failed");
      e.printStackTrace();
    }
	}
	
  private void readConfFiles() throws ApplicationException {
    try {
      // read project/organization configuration files        
      String metadataRdfDirStr = Constants.getInstance().getMetadataDir() + "/resources";
      File metadataRdfDir = new File(metadataRdfDirStr);
      ArrayList<File> projectRdfFiles = new ArrayList<File>(FileUtils.listFiles(metadataRdfDir, null, false));
      Collections.sort(projectRdfFiles);
      Iterator<File> filesIter = projectRdfFiles.iterator();
      while (filesIter.hasNext()) {
        File projectRdfFile = filesIter.next();
        // read project/organization rdf file
        Project project = readProjectRdfFile(projectRdfFile);
        if (project != null) {
          String projectId = project.getId();
          // read additional data of this project from xml file        
          String xmlConfigFileName = Constants.getInstance().getMetadataDir() + "/resources-addinfo/" + projectId + ".xml";
          File xmlConfigFile = new File(xmlConfigFileName);
          if (xmlConfigFile.exists()) {
            readProjectXmlFile(project, xmlConfigFile);
          }
        }
      }
      ArrayList<Project> projects = getProjects();
      for (int i=0; i<projects.size(); i++) {
        Project p = projects.get(i);
        p.buildRdfPathes();
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  private Project readProjectRdfFile(File projectRdfFile) throws ApplicationException {
    Project project = null;
    try {
      Document projectRdfDoc = Jsoup.parse(projectRdfFile, "utf-8");
      project = new Project();
      Date metadataRdfFileLastModified = new Date(projectRdfFile.lastModified());
      project.setLastModified(metadataRdfFileLastModified);
      String type = "project";
      Elements projectElems = projectRdfDoc.select("rdf|RDF > rdf|Description > rdf|type[rdf:resource*=Project]");
      if (projectElems.isEmpty()) {
        projectElems = projectRdfDoc.select("rdf|RDF > rdf|Description > rdf|type[rdf:resource*=Organization]");
        if (! projectElems.isEmpty()) {
          type = "organization";
        } else {
          LOGGER.error("ProjectReader: " + projectRdfFile + " contains no project or organization");
          return null;
        }
      }
      project.setType(type);
      Element projectElem = projectElems.first().parent();
      String projectRdfId = projectElem.attr("rdf:about");
      if (projectRdfId == null || projectRdfId.isEmpty()) {
        LOGGER.error("ProjectReader: " + projectRdfFile + ": no project rdf id defined");
        return null;
      }
      project.setRdfId(projectRdfId);
      String projectId = projectElem.select("foaf|nick").text();
      if (projectId == null || projectId.isEmpty()) {
        LOGGER.error("ProjectReader: " + projectRdfFile + ": no project \"<foaf:nick>\" defined");
        return null;
      }
      project.setId(projectId);
      String title = projectElem.select("foaf|name").text();
      if (title != null && ! title.isEmpty())
        project.setTitle(title);
      String status = projectElem.select("foaf|status").text();
      if (status != null && ! status.isEmpty())
        project.setStatus(status);
      String homepageUrl = projectElem.select("foaf|homepage").attr("rdf:resource");
      if (homepageUrl != null && ! homepageUrl.isEmpty())
        project.setHomepageUrl(homepageUrl);
      String parentRdfId = projectElem.select("dcterms|isPartOf").attr("rdf:resource");
      if (parentRdfId != null && ! parentRdfId.isEmpty())
        project.setParentRdfId(parentRdfId);
      String mainLanguage = projectElem.select("dcterms|language").attr("rdf:resource");
      if (mainLanguage != null) {
        if (mainLanguage.contains("/"))
          mainLanguage = mainLanguage.substring(mainLanguage.lastIndexOf("/") + 1);
        project.setMainLanguage(mainLanguage);
      } else {
        LOGGER.error("ProjectReader: " + projectRdfFile + ": no project \"<dcterms:language>\" defined");
      }
      Elements collectionElems = projectRdfDoc.select("rdf|RDF > rdf|Description > rdf|type[rdf:resource*=Aggregation]");
      for (int i=0; i< collectionElems.size(); i++) {
        Element collectionElem = collectionElems.get(i).parent();
        String collRdfId = collectionElem.select("rdf|Description").attr("rdf:about");
        ProjectCollection collection = new ProjectCollection(projectRdfId);
        collection.setRdfId(collRdfId);
        String collParentRdfId = collectionElem.select("ore|isAggregatedBy").attr("rdf:resource");
        if (collParentRdfId != null && ! collParentRdfId.isEmpty()) {
          collection.setParentRdfId(collParentRdfId);
        } else {
          collParentRdfId = projectElem.select("dcterms|isPartOf").attr("rdf:resource");
        }
        project.addCollection(collRdfId, collection);
      }
      projectsByRdfId.put(projectRdfId, project);
      projects.put(projectId, project);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return project;
  }
  
	private void readProjectXmlFile(Project project, File xmlConfigFile) throws ApplicationException {
    try {
  	  Document projectXmlDoc = Jsoup.parse(xmlConfigFile, "utf-8");
      Elements collectionElems = projectXmlDoc.select("wsp > collection");
      for (int i=0; i< collectionElems.size(); i++) {
        Element collectionElem = collectionElems.get(i);
        String collectionRdfId = collectionElem.select("collection").attr("id");
        ProjectCollection collection = project.getCollection(collectionRdfId);
        if (collection == null) {
          collection = new ProjectCollection(collectionRdfId);
          project.addCollection(collectionRdfId, collection);
        }
        Elements databaseElems = collectionElem.select("collection > db");
        for (int j=0; j< databaseElems.size(); j++) {
          Element databaseElem = databaseElems.get(j);
          Database database = Database.fromDatabaseElement(databaseElem);
          String databaseRdfId = database.getRdfId();
          collection.addDatabase(databaseRdfId, database);
          project.addDatabase(databaseRdfId, database);  // redundant also in project
        }
      }
      String updateCycle = projectXmlDoc.select("wsp > updateCycle").text();
      if (updateCycle != null && ! updateCycle.isEmpty()) {
        project.setUpdateCycle(updateCycle);
      } else {
        String status = project.getStatus();
        if (status != null) {
          if (status.equals("aktiv"))
            project.setUpdateCycle("monthly");
          else if (status.equals("abgeschlossen"))
            project.setUpdateCycle("halfyearly");
        } else {
          project.setUpdateCycle("halfyearly");  // default value
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
	}
	
  private void setConfigFilesLastModified(Project project, Date lastModified) throws ApplicationException {
    String projectId = project.getId();
    File metadataProjectFileRdf = new File(Constants.getInstance().getMetadataDir() + "/resources/" + projectId + ".rdf");
    long now = lastModified.getTime();
    metadataProjectFileRdf.setLastModified(now);
    File metadataProjectFileXml = new File(Constants.getInstance().getMetadataDir() + "/resources-addinfo/" + projectId + ".xml");
    metadataProjectFileXml.setLastModified(now);
  }
  
  private boolean isBaseProject(Project project) {
    boolean isBaseProject = false;
    String updateCycle = project.getUpdateCycle();
    if (updateCycle != null && updateCycle.equals("never"))
      isBaseProject = true;
    return isBaseProject;
  }
  
  private boolean isAkademiepublikationsProject(Project project) {
    boolean isAkademiepublikationsProject = false;
    String projectName = project.getId();
    if (projectName != null && projectName.startsWith("akademiepublikationen"))
      isAkademiepublikationsProject = true;
    return isAkademiepublikationsProject;
  }
  
	private void updateUserMetadata(Project project, String type) throws ApplicationException {
	  try {
      String projectFilePath = getProjectFilePath(project, type);
      File metadataProjectFile = new File(Constants.getInstance().getMetadataDir() + projectFilePath);
      File userMetadataProjectFile = new File(Constants.getInstance().getUserMetadataDir() + projectFilePath);
      FileUtils.copyFile(userMetadataProjectFile, metadataProjectFile);
      if (type.equals("rdf")) {
        readProjectRdfFile(metadataProjectFile);
      } else if (type.equals("xml")) {
        readProjectXmlFile(project, metadataProjectFile);
      }
      project.buildRdfPathes();
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
	}
	
  private boolean projectFileChangedByChecksum(Project project, String type) throws ApplicationException {
    boolean projectFileChanged = false;
    try {
      String projectFilePath = getProjectFilePath(project, type);
      File metadataProjectFile = new File(Constants.getInstance().getMetadataDir() + projectFilePath);
      String md5ChecksumMetadataProjectFile = getHexMd5Checksum(metadataProjectFile);
      File userMetadataProjectFile = new File(Constants.getInstance().getUserMetadataDir() + projectFilePath);
      String md5ChecksumUserMetadataProjectFile = getHexMd5Checksum(userMetadataProjectFile);
      if (! md5ChecksumMetadataProjectFile.equals(md5ChecksumUserMetadataProjectFile)) {
        projectFileChanged = true;
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return projectFileChanged;
  }
  
  private String getHexMd5Checksum(File file) throws ApplicationException {
    String checksum = null;
    try {
      checksum = DigestUtils.md5Hex(FileUtils.readFileToByteArray(file));
    } catch (Exception e) {
      LOGGER.error("CollectionReader: getHexMd5Checksum: " + file.toString() + ": " + e.getMessage());
      e.printStackTrace();
    }
    return checksum;
  }

  private boolean updateCycleReached(Project project) {
    Date lastModified = project.getLastModified();
    String updateCycle = project.getUpdateCycle();
    if (updateCycle == null)
      return false;
    return cycleReached(lastModified, updateCycle);
  }
  
  private boolean cycleReached(Date date, String cycle) {
    boolean updateCycleReached = false;
    Date now = new Date();
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    if (cycle.equals("monthly")) {
      cal.add(Calendar.MONTH, 1);
    } else if (cycle.equals("halfyearly")) {
      cal.add(Calendar.MONTH, 6);
    }
    Date datePlusCycle = cal.getTime();
    if (datePlusCycle.before(now))
      updateCycleReached = true;
    return updateCycleReached;
  }
  
  private String getProjectFilePath(Project project, String type) throws ApplicationException {
    String projectId = project.getId();
    String fileName = projectId + "." + type;
    String path = null;
    if (type.equals("rdf"))
      path = "/resources/";
    else if (type.equals("xml"))
      path = "/resources-addinfo/";
    String projectFilePath = path + fileName;
    return projectFilePath;
  }
  
}
