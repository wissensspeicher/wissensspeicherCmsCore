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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
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
      return p1.getId().compareTo(p2.getId());
    }
  };
  public static Comparator<Project> projectTitleComparator = new Comparator<Project>() {
    public int compare(Project p1, Project p2) {
      return p1.getTitle().compareTo(p2.getTitle());
    }
  };
  public static Comparator<Subject> subjectNameComparator = new Comparator<Subject>() {
    public int compare(Subject s1, Subject s2) {
      return s1.getName().compareTo(s2.getName());
    }
  };
  public static Comparator<Person> personNameComparator = new Comparator<Person>() {
    public int compare(Person p1, Person p2) {
      return p1.getName().compareTo(p2.getName());
    }
  };
  private ArrayList<String> globalExcludes;
  private HashMap<String, Person> normdataPersons;
  private HashMap<String, Organization> normdataOrganizations;
  private HashMap<String, Subject> normdataSubjects;
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

  public ArrayList<Project> getProjectsSorted(String sortBy) {
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      projects.add(project);
    }
    Comparator<Project> projectComparator = projectIdComparator;
    if (sortBy.equals("title"))
      projectComparator = projectTitleComparator;
    Collections.sort(projects, projectComparator);
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
      String validStr = project.getValid();
      if (validStr != null) {
        String startStr = validStr.replaceAll(".*start=(.*?);.*", "$1"); 
        String endStr = validStr.replaceAll(".*end=(.*?);.*", "$1");
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

  public ArrayList<Project> findProjects(String query) {
    Occur occur = BooleanClause.Occur.MUST;
    String[] queryTerms = {};
    if (query.contains("\" ")) {
      ArrayList<String> queryTermsArrayList = new ArrayList<String>();
      occur = BooleanClause.Occur.MUST;
      Matcher matchFields = Pattern.compile("(\\S*:\".+?\"|\\S*:\\S*)\\s*").matcher(query); // matches fields (e.g. "title:Humboldt" or "title:"Alexander von Humboldt"") 
      while (matchFields.find())
        queryTermsArrayList.add(matchFields.group(1));
      queryTerms = queryTermsArrayList.toArray(new String[queryTermsArrayList.size()]);
    } else if (query.contains("\"|")) {
      ArrayList<String> queryTermsArrayList = new ArrayList<String>();
      occur = BooleanClause.Occur.SHOULD;
      Matcher matchFields = Pattern.compile("(\\S*:\".+?\"|\\S*:\\S*)\\|*").matcher(query); // matches fields (e.g. "title:Humboldt" or "title:"Alexander von Humboldt"") 
      while (matchFields.find())
        queryTermsArrayList.add(matchFields.group(1));
      queryTerms = queryTermsArrayList.toArray(new String[queryTermsArrayList.size()]);
    } else if (query.contains(" ")) {
      occur = BooleanClause.Occur.MUST;
      queryTerms = query.split(" ");
    } else if (query.contains("|")) {
      occur = BooleanClause.Occur.SHOULD;
      queryTerms = query.split("\\|");
    }
    BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
    for (int i = 0; i < queryTerms.length; i++) {
      String[] fieldQuery = queryTerms[i].split(":");
      String fieldName = fieldQuery[0];
      String fieldValue = fieldQuery[1];
      TermQuery termQuery = new TermQuery(new Term(fieldName, fieldValue));
      boolQueryBuilder.add(termQuery, occur);
    }
    BooleanQuery booleanQuery = boolQueryBuilder.build();
    return findProjects(booleanQuery);
  }
  
  private ArrayList<Project> findProjects(BooleanQuery booleanQuery) {
    ArrayList<Project> projects = new ArrayList<Project>();
    java.util.Collection<Project> projectValues = this.projects.values();
    for (Project project : projectValues) {
      List<BooleanClause> clauses = booleanQuery.clauses();
      boolean matchProject = false;
      for (int i=0; i<clauses.size(); i++) {
        BooleanClause clause = clauses.get(i);
        Query clauseQuery = clause.getQuery();
        Occur occurence = clause.getOccur(); // "should" or "must"
        if (clauseQuery instanceof TermQuery) {
          TermQuery termQuery = (TermQuery) clauseQuery;
          String fieldName = termQuery.getTerm().field();
          String projectFieldValue = "";
          if (fieldName.equals("title")) 
            projectFieldValue = project.getTitle();
          else if (fieldName.equals("abstract"))
            projectFieldValue = project.getAbstract();
          String termQueryStr = termQuery.getTerm().text();
          termQueryStr = termQueryStr.replaceAll("\\*", ".*");
          termQueryStr = termQueryStr.replaceAll("\\?", ".");
          termQueryStr = termQueryStr.replaceAll("\"", "");
          String regExpr = ".*" + termQueryStr + ".*";
          boolean match = projectFieldValue.matches(regExpr); 
          if (! match && occurence == BooleanClause.Occur.MUST) {
            matchProject = false;
            break;
          }
          if (match && occurence == BooleanClause.Occur.SHOULD) {
            matchProject = true;
            break;
          }
          if (match && occurence == BooleanClause.Occur.MUST)
            matchProject = true;
        }
      }
      if (matchProject)
        projects.add(project);
    }
    Collections.sort(projects, projectIdComparator);
    return projects;
  }

  public ProjectCollection getCollection(String collectionRdfId) {
    return collections.get(collectionRdfId);
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
  
  public Organization getOrganization(String aboutId) {
    return normdataOrganizations.get(aboutId);
  }
  
  public Subject getSubject(String aboutId) {
    return normdataSubjects.get(aboutId);
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
    readNormdataPersons();
    readNormdataOrganizations();
    readNormdataSubjects();
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
          String homepage = personElem.select("rdf|Description > foaf|ho mepage").text();
          if (homepage != null && ! homepage.isEmpty()) {
            person.setHomepage(homepage);
          }
          String gndId = personElem.select("rdf|Description > gnd|gndIdentifier").text();
          if (gndId != null && ! gndId.isEmpty()) {
            person.setGndId(gndId);
          }
          String functionOrRoleRdfId = personElem.select("rdf|Description > gnd|functionOrRole").text();
          if (functionOrRoleRdfId != null && ! functionOrRoleRdfId.isEmpty()) {
            person.setFunctionOrRoleRdfId(functionOrRoleRdfId);
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
  
  private void readNormdataOrganizations() {
    normdataOrganizations = new HashMap<String, Organization>();
    String orgNormdataFileName = Constants.getInstance().getMetadataDir() + "/normdata/wsp.normdata.organizations.rdf";
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
          String gndId = subjectElem.select("rdf|Description > gnd|gndIdentifier").text();
          if (gndId != null && ! gndId.isEmpty())
            subject.setGndId(gndId);
          String aboutId = subjectElem.select("rdf|Description").attr("rdf:about");
          subject.setRdfId(aboutId);
          normdataSubjects.put(aboutId, subject);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + subjectNormdataFile + " failed");
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
      Elements projectTypeElems = projectElem.select("dcterms|type");
      for (int i=0; i< projectTypeElems.size(); i++) {
        Element projectTypeElem = projectTypeElems.get(i);
        String projectTypeRdfId = projectTypeElem.attr("rdf:resource");
        if (projectTypeRdfId != null && projectTypeRdfId.contains("projectType")) {
          int index = projectTypeRdfId.lastIndexOf("#");
          String projectType = projectTypeRdfId.substring(index + 1);
          project.setProjectType(projectType);
        }
      }
      String title = projectElem.select("foaf|name[xml:lang=\"de\"]").text();
      if (title == null || title.isEmpty())
        title = projectElem.select("foaf|name").text();
      if (title != null && ! title.isEmpty())
        project.setTitle(title);
      String absstract = projectElem.select("dcterms|abstract[xml:lang=\"de\"]").text();
      if (absstract != null && ! absstract.isEmpty())
        project.setAbstract(absstract);
      String status = projectElem.select("foaf|status").text();
      if (status != null && ! status.isEmpty())
        project.setStatus(status);
      String valid = projectElem.select("dcterms|valid").text();
      if (valid != null && ! valid.isEmpty())
        project.setValid(valid);
      String homepageUrl = projectElem.select("foaf|homepage").attr("rdf:resource");
      if (homepageUrl != null && ! homepageUrl.isEmpty())
        project.setHomepageUrl(homepageUrl);
      String temporalRdfId = projectElem.select("dcterms|temporal").attr("rdf:resource");
      if (temporalRdfId != null && ! temporalRdfId.isEmpty())
        project.setTemporalRdfId(temporalRdfId);
      String parentRdfId = projectElem.select("dcterms|isPartOf").attr("rdf:resource");
      if (parentRdfId != null && ! parentRdfId.isEmpty())
        project.setParentRdfId(parentRdfId);
      String languageRdfId = projectElem.select("dcterms|language").attr("rdf:resource");
      if (languageRdfId != null && ! languageRdfId.isEmpty()) {
        String mainLanguage = getIso639Language(languageRdfId); // international 3 character id (e.g. "ger")
        project.setMainLanguage(mainLanguage);
      } else {
        project.setMainLanguage("ger");
        LOGGER.error("ProjectReader: " + projectRdfFile + ": no project \"<dcterms:language>\" defined. Project: \"" + projectId + "\" set to default language: ger");
      }
      Elements staffElems = projectElem.select("dcterms|contributor");
      for (int i=0; i< staffElems.size(); i++) {
        Element staffPersonElem = staffElems.get(i);
        String personRdfId = staffPersonElem.attr("rdf:resource");
        Person person = getPerson(personRdfId);
        if (person != null)
          project.addStaffPerson(personRdfId, person);
      }
      Elements subjectElems = projectElem.select("dcterms|subject");
      for (int i=0; i< subjectElems.size(); i++) {
        Element subjectElem = subjectElems.get(i);
        String subjectRdfId = subjectElem.attr("rdf:resource");
        Subject subject = new Subject();
        subject.setRdfId(subjectRdfId);
        subject.setGndId(subjectRdfId);
        project.addSubject(subjectRdfId, subject);
      }
      String spatialRdfId = projectElem.select("dcterms|spatial").attr("rdf:resource");
      if (spatialRdfId != null && ! spatialRdfId.isEmpty())
        project.setSpatialRdfId(spatialRdfId);
      Elements relatedElems = projectElem.select("dcterms|related");
      for (int i=0; i< relatedElems.size(); i++) {
        Element relatedElem = relatedElems.get(i);
        String relatedRdfId = relatedElem.attr("rdf:resource");
        project.addProjectRelation(relatedRdfId);
      }
      Elements gndRelatedPersonsElems = projectElem.select("gnd|relatedPerson");
      for (int i=0; i< gndRelatedPersonsElems.size(); i++) {
        Element gndRelatedPersonsElem = gndRelatedPersonsElems.get(i);
        String personRdfId = gndRelatedPersonsElem.attr("rdf:resource");
        Person person = getPerson(personRdfId);
        if (person != null)
          project.addGndRelatedPerson(personRdfId, person);
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
        String collAbstract = collectionElem.select("dcterms|abstract[xml:lang=\"de\"]").text();
        if (collAbstract != null && ! collAbstract.isEmpty())
          collection.setAbsstract(collAbstract);
        Elements collectionStaffElems = collectionElem.select("dcterms|contributor");
        for (int j=0; j< collectionStaffElems.size(); j++) {
          Element staffPersonElem = collectionStaffElems.get(j);
          String personRdfId = staffPersonElem.attr("rdf:resource");
          Person person = getPerson(personRdfId);
          if (person != null)
            collection.addStaffPerson(personRdfId, person);
        }
        Elements collectionLanguageElems = collectionElem.select("dcterms|language");
        for (int j=0; j< collectionLanguageElems.size(); j++) {
          Element collectionLanguageElem = collectionLanguageElems.get(j);
          String collLanguageRdfId = collectionLanguageElem.attr("rdf:resource");
          if (collLanguageRdfId != null && ! collLanguageRdfId.isEmpty()) {
            String language = getIso639Language(collLanguageRdfId); // international 3 character id (e.g. "ger")
            collection.addLanguage(language);
          }
        }
        String collectionHomepageUrl = collectionElem.select("foaf|homepage").attr("rdf:resource");
        if (collectionHomepageUrl != null && ! collectionHomepageUrl.isEmpty()) {
          collection.setHomepageUrl(collectionHomepageUrl);
        }
        String collectionTitle = collectionElem.select("dcterms|title").text();
        if (collectionTitle != null && ! collectionTitle.isEmpty()) {
          collection.setTitle(collectionTitle);
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
