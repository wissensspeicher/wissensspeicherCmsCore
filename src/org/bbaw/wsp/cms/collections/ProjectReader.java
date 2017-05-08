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
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.util.Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;

public class ProjectReader {
  private static Logger LOGGER = Logger.getLogger(ProjectReader.class);
  private static ProjectReader projectReader;
  public static Comparator<Project> projectIdComparator = new Comparator<Project>() {
    public int compare(Project p1, Project p2) {
      if (p1.getId() == null && p2.getId() != null)
        return -1;
      if (p1.getId() != null && p2.getId() == null)
        return 1;
      if (p1.getId() == null && p2.getId() == null)
        return 0;
      return p1.getId().compareTo(p2.getId());
    }
  };
  public static Comparator<Project> projectTitleComparator = new Comparator<Project>() {
    public int compare(Project p1, Project p2) {
      if (p1.getTitle() == null && p2.getTitle() != null)
        return -1;
      if (p1.getTitle() != null && p2.getTitle() == null)
        return 1;
      if (p1.getTitle() == null && p2.getTitle() == null)
        return 0;
      return p1.getTitle().compareTo(p2.getTitle());
    }
  };
  public static Comparator<Subject> subjectNameComparator = new Comparator<Subject>() {
    public int compare(Subject s1, Subject s2) {
      if (s1.getName() == null && s2.getName() != null)
        return -1;
      if (s1.getName() != null && s2.getName() == null)
        return 1;
      if (s1.getName() == null && s2.getName() == null)
        return 0;
      return s1.getName().compareTo(s2.getName());
    }
  };
  public static Comparator<PeriodOfTime> periodOfTimeLabelComparator = new Comparator<PeriodOfTime>() {
    public int compare(PeriodOfTime p1, PeriodOfTime p2) {
      if (p1.getLabel() == null && p2.getLabel() != null)
        return -1;
      if (p1.getLabel() != null && p2.getLabel() == null)
        return 1;
      if (p1.getLabel() == null && p2.getLabel() == null)
        return 0;
      return p1.getLabel().compareTo(p2.getLabel());
    }
  };
  public static Comparator<Person> personNameComparator = new Comparator<Person>() {
    public int compare(Person p1, Person p2) {
      if (p1.getName() == null && p2.getName() != null)
        return -1;
      if (p1.getName() != null && p2.getName() == null)
        return 1;
      if (p1.getName() == null && p2.getName() == null)
        return 0;
      return p1.getName().compareTo(p2.getName());
    }
  };
  public static Comparator<Organization> organizationNameComparator = new Comparator<Organization>() {
    public int compare(Organization o1, Organization o2) {
      if (o1.getName() == null && o2.getName() != null)
        return -1;
      if (o1.getName() != null && o2.getName() == null)
        return 1;
      if (o1.getName() == null && o2.getName() == null)
        return 0;
      return o1.getName().compareTo(o2.getName());
    }
  };
  private ArrayList<String> globalExcludes;
  private ArrayList<String> globalContentCssSelectors;
  private HashMap<String, Person> normdataPersons;
  private HashMap<String, OutputType> normdataAgentClasses;
  private HashMap<String, Organization> normdataOrganizations;
  private HashMap<String, Subject> normdataSubjects;
  private HashMap<String, Location> normdataLocations;
  private HashMap<String, PeriodOfTime> normdataPeriodOfTime;
  private HashMap<String, OutputType> normdataOutputTypes;
  private HashMap<String, OutputType> normdataProjectOutputTypes;
  private HashMap<String, NormdataLanguage> normdataLanguages;
  private HashMap<String, Project> projects;  // key is id string and value is project
  private HashMap<String, Project> projectsByRdfId;  // key is projectRdfId string and value is project
  private HashMap<String, ProjectCollection> collections = new HashMap<String, ProjectCollection>();  // all project collections: key is collectionRdfId and value is collection
	
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
    globalContentCssSelectors = new ArrayList<String>();
    globalExcludes = new ArrayList<String>();
    readNormdataFiles();
    readMdsystemXmlFile();
    readConfFiles();
    LOGGER.info("Project reader initialized with " + projects.size() + " projects");
	}
	
	public ArrayList<Project> getProjects() {
		ArrayList<Project> projects = new ArrayList<Project>();
		java.util.Collection<Project> projectValues = this.projects.values();
		for (Project project : projectValues) {
			projects.add(project);
		}
    Collections.sort(projects, projectIdComparator);
		return projects;
	}
	
  private Date getLatestModified(ArrayList<File> files) {
    Date retDate = null;
    for (int i=0; i<files.size(); i++) {
      File f = files.get(i);
      if (f.exists()) {
        Date lastModified = new Date(f.lastModified());
        if (retDate == null || lastModified.after(retDate))
          retDate = lastModified;
      }
    }
    return retDate;
  }
  
  public ArrayList<Project> getProjectsSorted(String sortBy) {
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      projects.add(project);
    }
    Comparator<Project> projectComparator = projectIdComparator;
    if (sortBy.equals("label"))
      projectComparator = projectTitleComparator;
    Collections.sort(projects, projectComparator);
    return projects;
  }

  public ArrayList<Project> getProjectsByType(String type) {
    if (type.equals("rootOrganization"))
      return getRootOrganizationProjects();
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      String t = project.getType();
      if (t != null && t.equals(type))
        projects.add(project);
    }
    Collections.sort(projects, projectIdComparator);
    return projects;
  }
  
  private ArrayList<Project> getRootOrganizationProjects() {
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    HashMap<String, Project> rootOrgs = new HashMap<String, Project>();
    for (Project project : projectValues) {
      String orgRdfId = project.getOrganizationRdfId();
      if (orgRdfId != null) {
        Project rootProject = projectsByRdfId.get(orgRdfId);
        if (rootProject != null)
          rootOrgs.put(orgRdfId, rootProject);
      }
    }
    projects.addAll(rootOrgs.values());
    return projects;
  }

  public ArrayList<Project> getProjectsByProjectType(String projectType) {
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      String projType = project.getProjectType();
      if (projType != null && projType.equals(projectType))
        projects.add(project);
    }
    Collections.sort(projects, projectIdComparator);
    return projects;
  }
  
  public ArrayList<Project> getProjectsByStatus(String status) {
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      String projectStatus = project.getStatus();
      if (projectStatus != null && projectStatus.equals(status))
        projects.add(project);
    }
    Collections.sort(projects, projectIdComparator);
    return projects;
  }

  public ArrayList<Project> getProjectsByYear(String yearStr) throws ApplicationException {
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      String durationStr = project.getDuration();
      if (durationStr != null) {
        String startStr = durationStr.replaceAll(".*start=(.*?);.*", "$1"); 
        String endStr = durationStr.replaceAll(".*end=(.*?);.*", "$1");
        Util util = new Util();
        Date from = util.toDate(startStr);
        Date to = util.toDate(endStr);
        Date year = util.toDate(yearStr);
        if (from.getTime() <= year.getTime() && year.getTime() <= to.getTime())
          projects.add(project);
      }
    }
    Collections.sort(projects, projectIdComparator);
    return projects;
  }

  public ArrayList<Project> getProjectsByOrganizationRdfId(String orgRdfId) {
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      String projOrgRdfId = project.getOrganizationRdfId();
      if (projOrgRdfId != null && projOrgRdfId.equals(orgRdfId))
        projects.add(project);
    }
    Collections.sort(projects, projectIdComparator);
    return projects;
  }
  
  /**
   * 
   * @param labelStartStr
   * @return e.g. all "A-Projects" if labelStartStr = "A"
   */
  public ArrayList<Project> getProjectsByLabelStartStr(String labelStartStr) { 
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      String l = project.getTitle();
      if (l != null && l.startsWith(labelStartStr))
        projects.add(project);
    }
    Collections.sort(projects, projectTitleComparator);
    return projects;
  }
  
  public ArrayList<Organization> getOrganizations() {
    ArrayList<Organization> organizations = new ArrayList<Organization>();
    java.util.Collection<Organization> organizationValues = this.normdataOrganizations.values();
    for (Organization organization : organizationValues) {
      organizations.add(organization);
    }
    Collections.sort(organizations, organizationNameComparator);
    return organizations;
  }

  public ProjectCollection getCollection(String collectionRdfId) {
    return collections.get(collectionRdfId);
  }
  
  public ProjectCollection getRootCollection(String collectionRdfId) throws ApplicationException {
    ProjectCollection retColl = null;
    ProjectCollection coll = getCollection(collectionRdfId);
    if (coll != null) {
      retColl = coll.getRootCollection();
    } else {
      LOGGER.error("getRootCollection: " + collectionRdfId + " does not exist");
    }
    return retColl;
  }
  
  public String getCollectionMainLanguage(String collectionRdfId) throws ApplicationException {
    String mainLanguage = null;
    if (collectionRdfId == null)
      return null;
    ProjectCollection collection = getCollection(collectionRdfId);
    if (collection != null) {
      mainLanguage = collection.getMainLanguage();
    }
    return mainLanguage;
  }
  
  public ArrayList<ProjectCollection> getCollections(String projectRdfId) {
    Project project = getProjectByRdfId(projectRdfId);
    if (project != null) {
      ArrayList<ProjectCollection> collections = project.getCollections();
      return collections;
    } else {
      return null;
    }
  }
  
  public ArrayList<Subject> getSubjects() {
    ArrayList<Subject> subjects = new ArrayList<Subject>();
    java.util.Collection<Subject> values = this.normdataSubjects.values();
    for (Subject subject : values) {
      subjects.add(subject);
    }
    Collections.sort(subjects, subjectNameComparator);
    return subjects;
  }

  public ArrayList<Subject> getSubjects(String projectRdfId) {
    Project project = getProjectByRdfId(projectRdfId);
    if (project != null) {
      ArrayList<Subject> projectSubjects = project.getSubjects();
      return projectSubjects;
    } else {
      return null;
    }
  }
  
  public ArrayList<PeriodOfTime> getAllPeriodOfTime() {
    ArrayList<PeriodOfTime> pots = new ArrayList<PeriodOfTime>();
    java.util.Collection<PeriodOfTime> values = this.normdataPeriodOfTime.values();
    for (PeriodOfTime pot : values) {
      pots.add(pot);
    }
    Collections.sort(pots, periodOfTimeLabelComparator);
    return pots;
  }

  public ArrayList<String> getProjectTypes() {
    ArrayList<String> projectTypes = new ArrayList<String>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      String projectType = project.getProjectType();
      if (projectType != null && ! projectTypes.contains(projectType))
        projectTypes.add(projectType);
    }
    Collections.sort(projectTypes);
    return projectTypes;
  }

  public ArrayList<Person> getStaff() {
    ArrayList<Person> persons = new ArrayList<Person>();
    java.util.Collection<Person> values = this.normdataPersons.values();
    for (Person person : values) {
      persons.add(person);
    }
    Collections.sort(persons, personNameComparator);
    return persons;
  }

  public ArrayList<Person> getStaff(String projectRdfId) {
    Project project = getProjectByRdfId(projectRdfId);
    if (project != null) {
      ArrayList<Person> projectStaff = project.getStaff();
      return projectStaff;
    } else {
      return null;
    }
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

  public void remove(Project project) {
    String projectId = project.getId();
    projects.remove(projectId);
  }
  
  public Project getProjectByRdfId(String projectRdfId) {
    Project p = projectsByRdfId.get(projectRdfId);
    return p;
  }

  public Person getPerson(String aboutId) {
    return normdataPersons.get(aboutId);
  }
    
  public OutputType getAgentClass(String aboutId) {
    return normdataAgentClasses.get(aboutId);
  }

  public Organization getOrganization(String organizationRdfId) {
    return normdataOrganizations.get(organizationRdfId);
  }
  
  public Subject getSubject(String aboutId) {
    return normdataSubjects.get(aboutId);
  }
  
  public PeriodOfTime getPeriodOfTime(String aboutId) {
    return normdataPeriodOfTime.get(aboutId);
  }
  
  public Location getLocation(String aboutId) {
    return normdataLocations.get(aboutId);
  }
  
  public OutputType getType(String aboutId) {
    return normdataOutputTypes.get(aboutId);
  }
  
  public NormdataLanguage getNormdataLanguage(String aboutId) {
    return normdataLanguages.get(aboutId);
  }
  
  public ArrayList<String> getGlobalExcludes() {
    return globalExcludes;
  }
  
  public ArrayList<String> getGlobalContentCssSelectors() {
    return globalContentCssSelectors;
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
    readNormdataPersons();
    readNormdataAgentClasses();
    readNormdataOrganizations();
    readNormdataSubjects();
    updateGNDSubjectHeadingsInUseNormdataFile();
    readNormdataLocations();
    readNormdataPeriodOfTime();
    readNormdataOutputTypes();
    readNormdataProjectTypes();
    readNormdataLanguages();
  }
  
  private void readNormdataPersons() {
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
          String name = personElem.select("rdf|Description > foaf|name").text();
          if (name != null && ! name.isEmpty()) {
            person.setName(name);
          }
          String title = personElem.select("rdf|Description > foaf|title").text();
          if (title != null && ! title.isEmpty()) {
            person.setTitle(title);
          }
          String mbox = personElem.select("rdf|Description > foaf|mbox").text();
          if (mbox != null && ! mbox.isEmpty()) {
            person.setMbox(mbox);
          }
          String homepage = personElem.select("rdf|Description > foaf|homepage").text();
          if (homepage != null && ! homepage.isEmpty()) {
            person.setHomepage(homepage);
          }
          String gndId = personElem.select("rdf|Description > gndo|gndIdentifier").text();
          if (gndId != null && ! gndId.isEmpty()) {
            person.setGndId(gndId);
          }
          String functionOrRoleRdfId = personElem.select("rdf|Description > gndo|functionOrRole").text();
          if (functionOrRoleRdfId != null && ! functionOrRoleRdfId.isEmpty()) {
            OutputType agentClass = getAgentClass(functionOrRoleRdfId);
            if (agentClass != null) {
              String functionOrRole = agentClass.getLabel();
              person.setFunctionOrRole(functionOrRole);
            }
          }
          String rdfId = personElem.select("rdf|Description").attr("rdf:about");
          person.setRdfId(rdfId);          
          normdataPersons.put(rdfId, person);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + personNormdataFile + " failed");
      e.printStackTrace();
    }
  }
  
  private void readNormdataAgentClasses() {
    normdataAgentClasses = new HashMap<String, OutputType>();
    String normdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.normdata.agentClass.rdf";
    File normdataFile = new File(normdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(normdataFile, "utf-8");
      Elements agentClassElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! agentClassElems.isEmpty()) {
        for (int i=0; i< agentClassElems.size(); i++) {
          OutputType agentClass = new OutputType();
          Element agentClassElem = agentClassElems.get(i);
          String aboutId = agentClassElem.select("rdf|Description").attr("rdf:about");
          agentClass.setRdfId(aboutId);
          String label = agentClassElem.select("rdf|Description > rdfs|label[xml:lang=\"de\"]").text();
          if (label != null && ! label.isEmpty()) {
            agentClass.setLabel(label);
          }
          normdataAgentClasses.put(aboutId, agentClass);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + normdataFile + " failed");
      e.printStackTrace();
    }
  }

  private void readNormdataOrganizations() {
    normdataOrganizations = new HashMap<String, Organization>();
    String orgNormdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.normdata.organizationsProjects.rdf";
    File orgNormdataFile = new File(orgNormdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(orgNormdataFile, "utf-8");
      Elements orgElems = normdataFileDoc.select("rdf|RDF > rdf|Description > rdf|type[rdf:resource*=Organization]");
      if (! orgElems.isEmpty()) {
        for (int i=0; i< orgElems.size(); i++) {
          Organization organization = new Organization();
          Element orgElem = orgElems.get(i).parent();
          String name = orgElem.select("rdf|Description > foaf|name[xml:lang=\"de\"]").text();
          if (name != null && ! name.isEmpty()) {
            organization.setName(name);
          }
          String homepageUrl = orgElem.select("rdf|Description > foaf|homepage").attr("rdf:resource");
          if (homepageUrl != null && ! homepageUrl.isEmpty())
            organization.setHomepageUrl(homepageUrl);
          String aboutId = orgElem.select("rdf|Description").attr("rdf:about");
          organization.setRdfId(aboutId);
          normdataOrganizations.put(aboutId, organization);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + orgNormdataFile + " failed");
      e.printStackTrace();
    }
  }
  
  private void updateGNDSubjectHeadingsInUseNormdataFile() {
    boolean updateGndSubjectHeadingsInUseNormdataFile = false;
    File gndSubjectHeadingsInUseNormdataFile = new File(Constants.getInstance().getMetadataDir() + "/externalnormdata/GND_SubjectHeadingsUsed.rdf");
    ArrayList<File> projectRdfFiles = getProjectRdfFiles();
    try {
      if (! gndSubjectHeadingsInUseNormdataFile.exists()) {
        updateGndSubjectHeadingsInUseNormdataFile = true;
      } else {
        Date lastModifiedGndSubjectHeadingsInUseNormdataFile = new Date(gndSubjectHeadingsInUseNormdataFile.lastModified());
        Date lastModifiedProject = getLatestModified(projectRdfFiles);
        if (lastModifiedGndSubjectHeadingsInUseNormdataFile.before(lastModifiedProject))
          updateGndSubjectHeadingsInUseNormdataFile = true;
      }
      if (updateGndSubjectHeadingsInUseNormdataFile) {
        // read all gnd subjects headings
        HashMap<String, String> gndSubjectHeadings = new HashMap<String, String>();
        File normdataDir = new File(Constants.getInstance().getMetadataDir() + "/externalnormdata");
        ArrayList<File> gndSubjectHeadingNormdataFiles = new ArrayList<File>(FileUtils.listFiles(normdataDir, new WildcardFileFilter("GND_SubjectHeadingSensoStricto*.rdf"), null));
        File anotherFile = new File(normdataDir + "/GND_SubjectHeading001.rdf");
        gndSubjectHeadingNormdataFiles.add(anotherFile);
        for (File gndSubjectHeadingNormdataFile : gndSubjectHeadingNormdataFiles) {
          if (gndSubjectHeadingNormdataFile.exists()) {
            Document normdataFileDoc = Jsoup.parse(gndSubjectHeadingNormdataFile, "utf-8");
            Elements subjectElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
            if (! subjectElems.isEmpty()) {
              for (Element subjectElem : subjectElems) {
                String aboutId = subjectElem.select("rdf|Description").attr("rdf:about");
                gndSubjectHeadings.put(aboutId, subjectElem.outerHtml());
              }
            }
          } else {
            LOGGER.error("ProjectReader.updateGNDSubjectHeadingsInUseNormdataFile: File: " + gndSubjectHeadingNormdataFile + " does not exist");
          }
        }
        // write the gnd subjects in use file
        HashMap<String, String> subjectsInUseHashMap = new HashMap<String, String>();
        for (File projectRdfFile : projectRdfFiles) {
          Document projectRdfFileDoc = Jsoup.parse(projectRdfFile, "utf-8");
          Elements subjectElems = projectRdfFileDoc.select("dcterms|subject");
          if (! subjectElems.isEmpty()) {
            for (Element subjectElem : subjectElems) {
              String rdfId = subjectElem.attr("rdf:resource");
              if (rdfId.contains("d-nb.info/gnd"))
                subjectsInUseHashMap.put(rdfId, rdfId);
            }
          }
        }
        StringBuilder gndSubjectHeadingsInUseStrBuilder = new StringBuilder();
        gndSubjectHeadingsInUseStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        gndSubjectHeadingsInUseStrBuilder.append("<rdf:RDF>\n");
        java.util.Collection<String> subjectsInUse = subjectsInUseHashMap.values();
        for (String subjectRdfId : subjectsInUse) {
          String gndSubjectHeadingRdfXmlStr = gndSubjectHeadings.get(subjectRdfId);
          if (gndSubjectHeadingRdfXmlStr != null)
            gndSubjectHeadingsInUseStrBuilder.append(gndSubjectHeadingRdfXmlStr);
          else
            LOGGER.error("ProjectReader.updateGNDSubjectHeadingsInUseNormdataFile: " + subjectRdfId + " in one of your project rdf file is not contained in GND_SubjectHeadingSensoStricto*.rdf file");
        }
        gndSubjectHeadingsInUseStrBuilder.append("</rdf:RDF>\n");
        FileUtils.writeStringToFile(gndSubjectHeadingsInUseNormdataFile, gndSubjectHeadingsInUseStrBuilder.toString());
      }  
      // read the gnd subjects in use file
      Document normdataFileDoc = Jsoup.parse(gndSubjectHeadingsInUseNormdataFile, "utf-8");
      Elements subjectElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! subjectElems.isEmpty()) {
        for (Element subjectElem : subjectElems) {
          Subject subject = new Subject();
          String type = subjectElem.select("rdf|Description > rdf|type").attr("rdf:resource");
          if (type != null && ! type.isEmpty()) {
            subject.setType(type);
          }
          String name = subjectElem.select("rdf|Description > gndo|preferredNameForTheSubjectHeading[xml:lang=\"de\"]").text();
          if (name != null && ! name.isEmpty()) {
            subject.setName(name);
          }
          String gndId = subjectElem.select("rdf|Description > gndo|gndIdentifier").text();
          if (gndId != null && ! gndId.isEmpty())
            subject.setGndId(gndId);
          String aboutId = subjectElem.select("rdf|Description").attr("rdf:about");
          subject.setRdfId(aboutId);
          normdataSubjects.put(aboutId, subject);
        }
      }
    } catch (Exception e) {
      LOGGER.error("ProjectReader.updateGNDSubjectHeadingsInUseNormdataFile: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  private void readNormdataSubjects() {
    normdataSubjects = new HashMap<String, Subject>();
    String subjectNormdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.normdata.subjects.rdf";
    File subjectNormdataFile = new File(subjectNormdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(subjectNormdataFile, "utf-8");
      Elements subjectElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! subjectElems.isEmpty()) {
        for (int i=0; i< subjectElems.size(); i++) {
          Subject subject = new Subject();
          Element subjectElem = subjectElems.get(i);
          String type = subjectElem.select("rdf|Description > rdf|type").attr("rdf:resource");
          if (type != null && ! type.isEmpty()) {
            subject.setType(type);
          }
          String name = subjectElem.select("rdf|Description > rdfs|label[xml:lang=\"de\"]").text();
          if (name != null && ! name.isEmpty()) {
            subject.setName(name);
          }
          String gndId = subjectElem.select("rdf|Description > gndo|gndIdentifier").text();
          if (gndId != null && ! gndId.isEmpty())
            subject.setGndId(gndId);
          String aboutId = subjectElem.select("rdf|Description").attr("rdf:about");
          subject.setRdfId(aboutId);
          if (name != null && ! name.isEmpty()) {
            normdataSubjects.put(aboutId, subject);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + subjectNormdataFile + " failed");
      e.printStackTrace();
    }
    subjectNormdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.skos.au-ps.rdf";
    subjectNormdataFile = new File(subjectNormdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(subjectNormdataFile, "utf-8");
      Elements subjectElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! subjectElems.isEmpty()) {
        for (int i=0; i< subjectElems.size(); i++) {
          Subject subject = new Subject();
          Element subjectElem = subjectElems.get(i);
          String type = subjectElem.select("rdf|Description > rdf|type").attr("rdf:resource");
          if (type != null && ! type.isEmpty()) {
            subject.setType(type);
          }
          String name = subjectElem.select("rdf|Description > skos|prefLabel[xml:lang=\"de\"]").text();
          if (name != null && ! name.isEmpty()) {
            subject.setName(name);
          }
          String aboutId = subjectElem.select("rdf|Description").attr("rdf:about");
          subject.setRdfId(aboutId);
          if (name != null && ! name.isEmpty()) {
            normdataSubjects.put(aboutId, subject);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + subjectNormdataFile + " failed");
      e.printStackTrace();
    }
  }

  private void readNormdataLocations() {
    normdataLocations = new HashMap<String, Location>();
    String normdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.normdata.locations.rdf";
    File normdataFile = new File(normdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(normdataFile, "utf-8");
      Elements locationElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! locationElems.isEmpty()) {
        for (int i=0; i< locationElems.size(); i++) {
          Location location = new Location();
          Element locationElem = locationElems.get(i);
          String aboutId = locationElem.select("rdf|Description").attr("rdf:about");
          location.setRdfId(aboutId);
          String type = locationElem.select("rdf|Description > rdf|type").attr("rdf:resource");
          if (type != null && ! type.isEmpty()) {
            location.setType(type);
          }
          String label = locationElem.select("rdf|Description > rdfs|label[xml:lang=\"de\"]").text();
          if (label != null && ! label.isEmpty()) {
            location.setLabel(label);
          }
          String country = locationElem.select("rdf|Description > gn|countryCode").text();
          if (country != null && ! country.isEmpty())
            location.setCountry(country.toLowerCase());
          normdataLocations.put(aboutId, location);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + normdataFile + " failed");
      e.printStackTrace();
    }
  }

  private void readNormdataPeriodOfTime() {
    normdataPeriodOfTime = new HashMap<String, PeriodOfTime>();
    String normdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.normdata.periodOfTime.rdf";
    File normdataFile = new File(normdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(normdataFile, "utf-8");
      Elements potElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! potElems.isEmpty()) {
        for (int i=0; i< potElems.size(); i++) {
          PeriodOfTime pot = new PeriodOfTime();
          Element potElem = potElems.get(i);
          String aboutId = potElem.select("rdf|Description").attr("rdf:about");
          pot.setRdfId(aboutId);
          String label = potElem.select("rdf|Description > rdfs|label[xml:lang=\"de\"]").text();
          if (label != null && ! label.isEmpty()) {
            pot.setLabel(label);
          }
          String period = potElem.select("rdf|Description > dcterms|temporal").text();
          if (period != null && ! period.isEmpty())
            pot.setPeriod(period);
          if (aboutId != null && ! aboutId.isEmpty() && label != null && ! label.isEmpty())
            normdataPeriodOfTime.put(aboutId, pot);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + normdataFile + " failed");
      e.printStackTrace();
    }
  }

  private void readNormdataOutputTypes() {
    normdataOutputTypes = new HashMap<String, OutputType>();
    String normdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.skos.aggregationTypeContent.rdf";
    File normdataFile = new File(normdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(normdataFile, "utf-8");
      Elements typeElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! typeElems.isEmpty()) {
        for (int i=0; i< typeElems.size(); i++) {
          OutputType type = new OutputType();
          Element typeElem = typeElems.get(i);
          String aboutId = typeElem.select("rdf|Description").attr("rdf:about");
          type.setRdfId(aboutId);
          String label = typeElem.select("rdf|Description > skos|prefLabel[xml:lang=\"de\"]").text();
          if (label != null && ! label.isEmpty()) {
            type.setLabel(label);
          }
          normdataOutputTypes.put(aboutId, type);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + normdataFile + " failed");
      e.printStackTrace();
    }
    normdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.skos.aggregationTypeFormal.rdf";
    normdataFile = new File(normdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(normdataFile, "utf-8");
      Elements typeElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! typeElems.isEmpty()) {
        for (int i=0; i< typeElems.size(); i++) {
          OutputType type = new OutputType();
          Element typeElem = typeElems.get(i);
          String aboutId = typeElem.select("rdf|Description").attr("rdf:about");
          type.setRdfId(aboutId);
          String label = typeElem.select("rdf|Description > skos|prefLabel[xml:lang=\"de\"]").text();
          if (label != null && ! label.isEmpty()) {
            type.setLabel(label);
          }
          normdataOutputTypes.put(aboutId, type);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + normdataFile + " failed");
      e.printStackTrace();
    }
  }

  private void readNormdataProjectTypes() {
    normdataProjectOutputTypes = new HashMap<String, OutputType>();
    String normdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.skos.projectTypeContent.rdf";
    File normdataFile = new File(normdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(normdataFile, "utf-8");
      Elements typeElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! typeElems.isEmpty()) {
        for (int i=0; i< typeElems.size(); i++) {
          OutputType type = new OutputType();
          Element typeElem = typeElems.get(i);
          String aboutId = typeElem.select("rdf|Description").attr("rdf:about");
          type.setRdfId(aboutId);
          String label = typeElem.select("rdf|Description > skos|prefLabel[xml:lang=\"de\"]").text();
          if (label != null && ! label.isEmpty()) {
            type.setLabel(label);
          }
          normdataProjectOutputTypes.put(aboutId, type);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + normdataFile + " failed");
      e.printStackTrace();
    }
    normdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.skos.projectTypeFormal.rdf";
    normdataFile = new File(normdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(normdataFile, "utf-8");
      Elements typeElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! typeElems.isEmpty()) {
        for (int i=0; i< typeElems.size(); i++) {
          OutputType type = new OutputType();
          Element typeElem = typeElems.get(i);
          String aboutId = typeElem.select("rdf|Description").attr("rdf:about");
          type.setRdfId(aboutId);
          String label = typeElem.select("rdf|Description > skos|prefLabel[xml:lang=\"de\"]").text();
          if (label != null && ! label.isEmpty()) {
            type.setLabel(label);
          }
          normdataProjectOutputTypes.put(aboutId, type);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + normdataFile + " failed");
      e.printStackTrace();
    }
  }

  private void readNormdataLanguages() {
    normdataLanguages = new HashMap<String, NormdataLanguage>();
    String normdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.normdata.languages.rdf";
    File normdataFile = new File(normdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(normdataFile, "utf-8");
      Elements langElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! langElems.isEmpty()) {
        for (int i=0; i< langElems.size(); i++) {
          NormdataLanguage lang = new NormdataLanguage();
          Element langElem = langElems.get(i);
          String aboutId = langElem.select("rdf|Description").attr("rdf:about");
          lang.setRdfId(aboutId);
          String label = langElem.select("rdf|Description > rdfs|label[xml:lang=\"de\"]").text();
          if (label != null && ! label.isEmpty()) {
            lang.setLabel(label);
          }
          normdataLanguages.put(aboutId, lang);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + normdataFile + " failed");
      e.printStackTrace();
    }
    // read LEXVO_Languages.rdf
    normdataFileName = Constants.getInstance().getMetadataDir() + "/externalnormdata/LEXVO_Languages.rdf";
    normdataFile = new File(normdataFileName);
    try {
      Document normdataFileDoc = Jsoup.parse(normdataFile, "utf-8");
      Elements langElems = normdataFileDoc.select("rdf|RDF > rdf|Description");
      if (! langElems.isEmpty()) {
        for (int i=0; i< langElems.size(); i++) {
          NormdataLanguage lang = new NormdataLanguage();
          Element langElem = langElems.get(i);
          String aboutId = langElem.select("rdf|Description").attr("rdf:about");
          lang.setRdfId(aboutId);
          String label = langElem.select("rdf|Description > rdfs|label[xml:lang=\"de\"]").text();
          if (label != null && ! label.isEmpty()) {
            lang.setLabel(label);
          }
          normdataLanguages.put(aboutId, lang);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + normdataFile + " failed");
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
      Elements contentCssSelectors = mdsystemXmlDoc.select("mdsystem > contentCssSelectors > contentCssSelector");
      if (contentCssSelectors != null && ! contentCssSelectors.isEmpty()) {
        for (Element contentCssSelector : contentCssSelectors) {
          String contentCssSelectorStr = contentCssSelector.text();
          globalContentCssSelectors.add(contentCssSelectorStr);
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
      ArrayList<File> projectRdfFiles = getProjectRdfFiles();
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
        p.buildSubCollections();
        p.buildRdfPathes();
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  private ArrayList<File> getProjectRdfFiles() {
    String metadataRdfDirStr = Constants.getInstance().getMetadataDir() + "/resources";
    File metadataRdfDir = new File(metadataRdfDirStr);
    ArrayList<File> projectRdfFiles = new ArrayList<File>(FileUtils.listFiles(metadataRdfDir, null, false));
    Collections.sort(projectRdfFiles);
    return projectRdfFiles;
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
      String parentRdfId = projectElem.select("dcterms|isPartOf").attr("rdf:resource");
      if (parentRdfId != null && ! parentRdfId.isEmpty())
        project.setParentRdfId(parentRdfId);
      String organizationRdfId = projectElem.select("gndo|hierarchicalSuperior").attr("rdf:resource");
      if (organizationRdfId != null && ! organizationRdfId.isEmpty())
        project.setOrganizationRdfId(organizationRdfId);
      String projectId = projectElem.select("foaf|nick").text();
      if (projectId == null || projectId.isEmpty()) {
        LOGGER.error("ProjectReader: " + projectRdfFile + ": no project \"<foaf:nick>\" defined");
        return null;
      }
      project.setId(projectId);
      String title = projectElem.select("foaf|name[xml:lang=\"de\"]").text();
      if (title == null || title.isEmpty())
        title = projectElem.select("foaf|name").text();
      if (title != null && ! title.isEmpty())
        project.setTitle(title);
      String absstract = projectElem.select("dcterms|abstract[xml:lang=\"de\"]").text();
      if (absstract != null && ! absstract.isEmpty())
        project.setAbstract(absstract);
      Elements projectTypeElems = projectElem.select("dcterms|type");
      for (int i=0; i< projectTypeElems.size(); i++) {
        Element projectTypeElem = projectTypeElems.get(i);
        String projectTypeRdfId = projectTypeElem.attr("rdf:resource");
        if (projectTypeRdfId != null && projectTypeRdfId.contains("projectType/formal")) { 
          OutputType projectType = normdataProjectOutputTypes.get(projectTypeRdfId);
          if (projectType != null)
            project.setProjectType(projectType.getLabel()); // e.g. "Vorhaben"
        }
      }
      String duration = projectElem.select("dcterms|valid").text(); // e.g. "start=1970; end=2014-12-31; name=Laufzeit"
      if (duration != null && ! duration.isEmpty())
        project.setDuration(duration);
      String status = projectElem.select("foaf|status").text(); // e.g. "abgeschlossen"
      if (status != null && ! status.isEmpty())
        project.setStatus(status);
      String homepageUrl = projectElem.select("foaf|homepage").attr("rdf:resource");
      if (homepageUrl != null && ! homepageUrl.isEmpty())
        project.setHomepageUrl(homepageUrl);
      Elements languageElems = projectElem.select("dcterms|language");
      if (languageElems != null && ! languageElems.isEmpty()) {
        for (int i=0; i< languageElems.size(); i++) {
          Element languageElem = languageElems.get(i);
          String languageRdfId = languageElem.attr("rdf:resource");
          if (languageRdfId != null && ! languageRdfId.isEmpty()) {
            String language = getIso639Language(languageRdfId); // international 3 character id (e.g. "ger")
            project.addLanguage(language);
            project.addLanguageRdfId(languageRdfId);
            if (i == 0)
              project.setMainLanguage(language);
          }
        }
      } else {
        project.setMainLanguage("ger");
        LOGGER.error("ProjectReader: " + projectRdfFile + ": no project \"<dcterms:language>\" defined. Project: \"" + projectId + "\" set to default language: ger");
      }
      Elements subjectElems = projectElem.select("dcterms|subject");
      for (int i=0; i< subjectElems.size(); i++) {
        Element subjectElem = subjectElems.get(i);
        String subjectRdfId = subjectElem.attr("rdf:resource");
        Subject subject = getSubject(subjectRdfId);
        if (subject != null)
          project.addSubject(subjectRdfId, subject);
      }
      String spatialRdfId = projectElem.select("dcterms|spatial").attr("rdf:resource");
      if (spatialRdfId != null && ! spatialRdfId.isEmpty())
        project.setSpatialRdfId(spatialRdfId);  // subject place, e.g. "http://sws.geonames.org/3631298/"
      String temporalRdfId = projectElem.select("dcterms|temporal").attr("rdf:resource"); 
      if (temporalRdfId != null && ! temporalRdfId.isEmpty())
        project.setTemporalRdfId(temporalRdfId); // e.g. "http://wissensspeicher.bbaw.de/rdf/normdata/Neuzeit"
      Elements gndRelatedPersonsElems = projectElem.select("gndo|relatedPerson");
      for (int i=0; i< gndRelatedPersonsElems.size(); i++) {
        Element gndRelatedPersonsElem = gndRelatedPersonsElems.get(i);
        String personRdfId = gndRelatedPersonsElem.attr("rdf:resource");
        Person person = getPerson(personRdfId);
        if (person != null)
          project.addGndRelatedPerson(personRdfId, person); // subject person, e.g. "http://wissensspeicher.bbaw.de/rdf/normdata/HumboldtAlexander"
      }
      Elements staffElems = projectElem.select("dcterms|contributor");
      for (int i=0; i< staffElems.size(); i++) {
        Element staffPersonElem = staffElems.get(i);
        String personRdfId = staffPersonElem.attr("rdf:resource");
        String functionOrRole = null;
        if (personRdfId.isEmpty()) {
          String personRdfNodeId = staffPersonElem.attr("rdf:nodeID");
          if (! personRdfNodeId.isEmpty()) {
            Element personRdfNode = projectRdfDoc.select("rdf|RDF > rdf|Description[rdf:nodeID*=" + personRdfNodeId + "]").first();
            if (personRdfNode != null) {
              personRdfId = personRdfNode.select("dcterms|identifier").text(); 
              String functionOrRoleRdfId = personRdfNode.select("gndo|functionOrRole").attr("rdf:resource");
              if (functionOrRoleRdfId != null) {
                OutputType agentClass = getAgentClass(functionOrRoleRdfId);
                if (agentClass != null) {
                  functionOrRole = agentClass.getLabel();
                }
              }
            }
          }
        }
        Person person = getPerson(personRdfId);
        if (person != null) {
          person.setFunctionOrRole(functionOrRole);
          project.addStaffPerson(personRdfId, person); // e.g. "http://wissensspeicher.bbaw.de/rdf/normdata/Leitner"
        } else {
          LOGGER.error(projectId + ".rdf : contributor: " + personRdfId + " is not defined in normdata file");
        }
      }
      Elements relatedElems = projectElem.select("dcterms|relation");
      for (int i=0; i< relatedElems.size(); i++) {
        Element relatedElem = relatedElems.get(i);
        String relatedRdfId = relatedElem.attr("rdf:resource");
        project.addProjectRelation(relatedRdfId);
      }
      Elements collectionElems = projectRdfDoc.select("rdf|RDF > rdf|Description > rdf|type[rdf:resource*=Aggregation]");
      for (int i=0; i< collectionElems.size(); i++) {
        Element collectionElem = collectionElems.get(i).parent();
        String collRdfId = collectionElem.select("rdf|Description").attr("rdf:about");
        ProjectCollection collection = new ProjectCollection(projectRdfId);
        collection.setRdfId(collRdfId);
        String collParentRdfId = collectionElem.select("ore|isAggregatedBy").attr("rdf:resource"); // collection parent
        if (collParentRdfId != null && ! collParentRdfId.isEmpty()) {
          collection.setParentRdfId(collParentRdfId);
        } 
        String collTypeRdfId = collectionElem.select("dcterms|type").attr("rdf:resource");
        if (collTypeRdfId != null && ! collTypeRdfId.isEmpty()) {
          if (collTypeRdfId.contains("aggregationType/formal")) { 
            OutputType collType = getType(collTypeRdfId);
            if (collType != null)
              collection.setType(collType); // e.g. "Datenbank" or "Webseite"
          }
        }
        String collectionTitle = collectionElem.select("dcterms|title").text();
        if (collectionTitle != null && ! collectionTitle.isEmpty()) {
          if (collection.isHomepageType())
            collectionTitle = title + ": " + collectionTitle;  // project title + collection title
          collection.setTitle(collectionTitle);
        }
        String collAbstract = collectionElem.select("dcterms|abstract[xml:lang=\"de\"]").text();
        if (collAbstract != null && ! collAbstract.isEmpty())
          collection.setAbstract(collAbstract);
        String collectionHomepageUrl = collectionElem.select("foaf|homepage").attr("rdf:resource");
        if (collectionHomepageUrl != null && ! collectionHomepageUrl.isEmpty()) {
          collection.setHomepageUrl(collectionHomepageUrl);
        }
        Elements collectionLanguageElems = collectionElem.select("dcterms|language");
        for (int j=0; j< collectionLanguageElems.size(); j++) {
          Element collectionLanguageElem = collectionLanguageElems.get(j);
          String collLanguageRdfId = collectionLanguageElem.attr("rdf:resource");
          if (collLanguageRdfId != null && ! collLanguageRdfId.isEmpty()) {
            String language = getIso639Language(collLanguageRdfId); // international 3 character id (e.g. "ger")
            collection.addLanguage(language);
            collection.addLanguageRdfId(collLanguageRdfId);
          }
        }
        Elements collectionSubjectElems = collectionElem.select("dcterms|subject");
        for (int j=0; j<collectionSubjectElems.size(); j++) {
          Element subjectElem = collectionSubjectElems.get(j);
          String subjectRdfId = subjectElem.attr("rdf:resource");
          Subject subject = getSubject(subjectRdfId);
          if (subject != null) {
            collection.addSubject(subjectRdfId, subject);
            project.addSubject(subjectRdfId, subject);
          }
        }
        String collectionSpatialRdfId = collectionElem.select("dcterms|spatial").attr("rdf:resource");
        if (collectionSpatialRdfId != null && ! collectionSpatialRdfId.isEmpty())
          collection.setSpatialRdfId(collectionSpatialRdfId);  // subject place, e.g. "http://sws.geonames.org/3631298/"
        String collectionTemporalRdfId = collectionElem.select("dcterms|temporal").attr("rdf:resource"); 
        if (collectionTemporalRdfId != null && ! collectionTemporalRdfId.isEmpty())
          collection.setTemporalRdfId(collectionTemporalRdfId); // e.g. "http://wissensspeicher.bbaw.de/rdf/normdata/Neuzeit"
        Elements collectionGndRelatedPersonsElems = collectionElem.select("gndo|relatedPerson");
        for (int j=0; j<collectionGndRelatedPersonsElems.size(); j++) {
          Element gndRelatedPersonsElem = collectionGndRelatedPersonsElems.get(j);
          String personRdfId = gndRelatedPersonsElem.attr("rdf:resource");
          Person person = getPerson(personRdfId);
          if (person != null)
            collection.addGndRelatedPerson(personRdfId, person); // subject person, e.g. "http://wissensspeicher.bbaw.de/rdf/normdata/HumboldtAlexander"
        }
        Elements collectionStaffElems = collectionElem.select("dcterms|contributor");
        for (int j=0; j< collectionStaffElems.size(); j++) {
          Element staffPersonElem = collectionStaffElems.get(j);
          String personRdfId = staffPersonElem.attr("rdf:resource");
          Person person = getPerson(personRdfId);
          if (person != null) {
            collection.addStaffPerson(personRdfId, person);
          }
        }
        project.addCollection(collRdfId, collection);
        collections.put(collRdfId, collection);
      }
      projectsByRdfId.put(projectRdfId, project);
      projects.put(projectId, project);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return project;
  }
  
  private String getIso639Language(String languageRdfId) {
    String isoLanguage = languageRdfId;
    if (languageRdfId.contains("/"))
      isoLanguage = languageRdfId.substring(languageRdfId.lastIndexOf("/") + 1);
    isoLanguage = Language.getInstance().getISO639Code(isoLanguage);
    return isoLanguage;
  }
  
	private void readProjectXmlFile(Project project, File xmlConfigFile) throws ApplicationException {
    try {
  	  Document projectXmlDoc = Jsoup.parse(xmlConfigFile, "utf-8");
      Elements collectionElems = projectXmlDoc.select("wsp > collection");
      for (int i=0; i< collectionElems.size(); i++) {
        Element collectionElem = collectionElems.get(i);
        String collectionRdfId = collectionElem.select("collection").attr("id");
        collectionRdfId = collectionRdfId.trim();
        ProjectCollection collection = project.getCollection(collectionRdfId);
        if (collection == null) {
          collection = new ProjectCollection(collectionRdfId);
          project.addCollection(collectionRdfId, collection);
        }
        Elements databaseElems = collectionElem.select("collection > db");
        for (int j=0; j< databaseElems.size(); j++) {
          Element databaseElem = databaseElems.get(j);
          Database database = Database.fromDatabaseElement(databaseElem);
          database.setCollectionRdfId(collectionRdfId);
          String databaseRdfId = database.getRdfId();
          if (database.getType() != null && database.getType().equals("oai")) {
            String oaiSet = database.getOaiSet();
            if (oaiSet != null)
              databaseRdfId = databaseRdfId + "/" + oaiSet;
          }
          collection.addDatabase(databaseRdfId, database);
          project.addDatabase(databaseRdfId, database);  // redundant also in project
        }
        // read contentCssSelectors
        Elements contentCssSelectorElems = collectionElem.select("collection > contentCssSelectors > contentCssSelector");
        if (contentCssSelectorElems != null && ! contentCssSelectorElems.isEmpty()) {
          ArrayList<String> contentCssSelectors = new ArrayList<String>();
          for (int j=0; j< contentCssSelectorElems.size(); j++) {
            String contentCssSelectorStr = contentCssSelectorElems.get(j).text();
            if (contentCssSelectorStr != null && ! contentCssSelectorStr.isEmpty())
              contentCssSelectors.add(contentCssSelectorStr);
          }
          collection.setContentCssSelectors(contentCssSelectors);
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
      project.buildSubCollections();
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
