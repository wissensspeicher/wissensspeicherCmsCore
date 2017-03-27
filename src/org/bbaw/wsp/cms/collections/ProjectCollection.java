package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.Person;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class ProjectCollection {
  private static Logger LOGGER = Logger.getLogger(ProjectReader.class);
  public static Comparator<Subject> subjectNameComparator = new Comparator<Subject>() {
    public int compare(Subject s1, Subject s2) {
      return s1.getName().compareTo(s2.getName());
    }
  };
  private String rdfId;
  private String projectRdfId; // rdfId of project
  private String parentRdfId; // rdfId of parent collection
  private String rdfPath; // path from this collection to root (project or organization)
  private OutputType type;  // type e.g. "Edition"
  private String homepageUrl;
  private String absstract; // abstract of project
  private String title;
  private String spatialRdfId; // spatial, e.g. "http://sws.geonames.org/2950157/"
  private String temporalRdfId; // temporal, e.g. "http://wissensspeicher.bbaw.de/rdf/normdata/Neuzeit"
  private ArrayList<String> languages = new ArrayList<String>(); // collection languages; first language is the main language
  private ArrayList<String> languageRdfIds = new ArrayList<String>(); // collection language rdf ids; first language is the main language
  private HashMap<String, Subject> subjects = new HashMap<String, Subject>();  // project subjects: key is subjectRdfId and value is subject
  private HashMap<String, Person> gndRelatedPersons = new HashMap<String, Person>();  // project gnd related persons: key is personRdfId and value is person
  private HashMap<String, Person> staff = new HashMap<String, Person>();  // collection staff: key is personRdfId and value is person
  private ArrayList<String> contentCssSelectors; // css selectors to find the content element in html pages e.g.: "#content"
  private ArrayList<ProjectCollection> collections = new ArrayList<ProjectCollection>(); // sub collections
  private HashMap<String, Database> databases = new HashMap<String, Database>();  // // collection databases: key is databaseRdfId and value is database
  
  public ProjectCollection(String projectRdfId) {
    this.projectRdfId = projectRdfId;
  }
  
  public void addLanguage(String language) {
    languages.add(language);
  }
  
  public void addLanguageRdfId(String languageRdfId) {
    languageRdfIds.add(languageRdfId);
  }
  
  public ArrayList<String> getLanguageRdfIds() {
    return languageRdfIds;
  }
  
  public void addSubCollection(ProjectCollection collection) {
    collections.add(collection);
  }
  
  public String getMainLanguage() throws ApplicationException {
    String mainLang = null;
    if (languages.size() > 0) {
      mainLang = languages.get(0);
    } else {
      if (projectRdfId != null) {
        Project project = ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
        if (project != null) {
          String projMainLang = project.getMainLanguage();
          if (projMainLang != null)
            mainLang = projMainLang;
        }
      }
    }
    return mainLang;
  }
  
  public ArrayList<String> getLanguages() {
    return languages;  
  }
  
  public ArrayList<String> getLanguageLabels() throws ApplicationException {
    ArrayList<String> languageLabels = null;
    ArrayList<String> languageRdfIds = getLanguageRdfIds();
    if (languageRdfIds != null && ! languageRdfIds.isEmpty()) {
      languageLabels = new ArrayList<String>();
      for (int i=0; i<languageRdfIds.size(); i++) {
        String languageRdfId = languageRdfIds.get(i);
        NormdataLanguage normdataLanguage = ProjectReader.getInstance().getNormdataLanguage(languageRdfId);
        if (normdataLanguage != null)
          languageLabels.add(normdataLanguage.getLabel());
      }
    }
    return languageLabels;
  }
  
  public String getSpatialRdfId() {
    return spatialRdfId;
  }

  public void setSpatialRdfId(String spatialRdfId) {
    this.spatialRdfId = spatialRdfId;
  }

  public String getPlace() throws ApplicationException {
    String place = null;
    if (spatialRdfId != null) {
      Location location = ProjectReader.getInstance().getLocation(spatialRdfId);
      if (location != null)
        place = location.getLabel();
    }
    return place;
  }
  
  public String getPeriodOfTime() throws ApplicationException {
    String periodOfTimeStr = null;
    if (temporalRdfId != null) {
      PeriodOfTime periodOdTime = ProjectReader.getInstance().getPeriodOfTime(temporalRdfId);
      if (periodOdTime != null)
        periodOfTimeStr = periodOdTime.getLabel();
    }
    return periodOfTimeStr;
  }
  
  public String getTemporalRdfId() {
    return temporalRdfId;
  }

  public void setTemporalRdfId(String temporalRdfId) {
    this.temporalRdfId = temporalRdfId;
  }

  public void addSubject(String subjectRdfId, Subject subject) {
    subjects.put(subjectRdfId, subject);
  }
  
  public Subject getSubject(String subjectRdfId) {
    return subjects.get(subjectRdfId);
  }

  public ArrayList<Subject> getSubjects() {
    ArrayList<Subject> subjects = null;
    if (this.subjects != null) {
      subjects = new ArrayList<Subject>();
      java.util.Collection<Subject> values = this.subjects.values();
      for (Subject subject : values) {
        subjects.add(subject);
      }
      Collections.sort(subjects, subjectNameComparator);
    }
    return subjects;
  }

  public ArrayList<String> getSubjectsStr() {
    ArrayList<String> subjects = null;
    if (this.subjects != null) {
      subjects = new ArrayList<String>();
      java.util.Collection<Subject> subjectValues = this.subjects.values();
      for (Subject subject : subjectValues) {
        String subjectStr = subject.getName();
        if (subjectStr != null)
          subjects.add(subjectStr);
      }
      Collections.sort(subjects);
    }
    return subjects;
  }

  public void addGndRelatedPerson(String personRdfId, Person person) {
    gndRelatedPersons.put(personRdfId, person);
  }
  
  public ArrayList<String> getGndRelatedPersonsStr() {
    ArrayList<String> gndRelatedPersons = null;
    if (this.gndRelatedPersons != null) {
      gndRelatedPersons = new ArrayList<String>();
      java.util.Collection<Person> personValues = this.gndRelatedPersons.values();
      for (Person person : personValues) {
        String personStr = person.getName();
        if (personStr != null)
          gndRelatedPersons.add(personStr);
      }
      Collections.sort(gndRelatedPersons);
    }
    return gndRelatedPersons;
  }

  public void addStaffPerson(String personRdfId, Person person) {
    staff.put(personRdfId, person);
  }
  
  public Person getStaffPerson(String personRdfId) {
    return staff.get(personRdfId);
  }
  
  public ArrayList<String> getStaffStr() {
    ArrayList<String> staffPersons = null;
    if (this.staff != null) {
      staffPersons = new ArrayList<String>();
      java.util.Collection<Person> personValues = this.staff.values();
      for (Person person : personValues) {
        String personStr = person.getName();
        if (personStr != null)
          staffPersons.add(personStr);
      }
      Collections.sort(staffPersons);
    }
    return staffPersons;
  }

  public Database getDatabase(String databaseRdfId) {
    return databases.get(databaseRdfId);
  }
  
  public void addDatabase(String databaseRdfId, Database database) {
    databases.put(databaseRdfId, database);
  }
  
  public void buildRdfPath() throws ApplicationException {
    this.rdfPath = calcRdfPath();
  }

  public ProjectCollection getRootCollection() throws ApplicationException {
    ProjectCollection rootColl = null;
    if (parentRdfId == null)
      return this;
    Project project = ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
    ProjectCollection parentCollection = project.getCollection(parentRdfId);
    rootColl = parentCollection.getRootCollection();  // recursive to rootCollection
    return rootColl;
  }
  
  private String calcRdfPath() throws ApplicationException {
    Project project = ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
    if (parentRdfId == null) {
      if (project == null) {
        LOGGER.error("Collection: " + rdfId + " calcRdfPath: projectRdfId: " + projectRdfId + " not found");
        return "null";
      } else {
        return project.getRdfPath() + "###" + rdfId;
      }
    }
    ProjectCollection parentCollection = project.getCollection(parentRdfId);
    if (parentCollection == null) {
      Project parentProject = ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
      if (parentProject == null) {
        LOGGER.error("Collection: " + rdfId + " calcRdfPath: parent rdfId: " + parentRdfId + " not found");
        return "null";
      }
      return parentProject.getRdfPath() + "###" + rdfId;
    }
    String collectionRdfPath = parentCollection.calcRdfPath() + "###" + rdfId;
    return collectionRdfPath;
  }
  
  public String getRdfId() {
    return rdfId;
  }

  public void setRdfId(String rdfId) {
    this.rdfId = rdfId;
  }

  public String getProjectRdfId() {
    return projectRdfId;
  }

  public void setProjectRdfId(String projectRdfId) {
    this.projectRdfId = projectRdfId;
  }

  public Project getProject() throws ApplicationException {
    return ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
  }
  
  public String getParentRdfId() {
    return parentRdfId;
  }
  
  public void setParentRdfId(String parentRdfId) {
    this.parentRdfId = parentRdfId;
  }
  
  public OutputType getType() {
    return type;
  }

  public void setType(OutputType type) {
    this.type = type;
  }

  public boolean isHomepageType() {
    boolean isHomepageType = false;
    if (type != null) {
      String typeRdfId = type.getRdfId();
      if (typeRdfId.matches(".*#Homepage.*"))
        isHomepageType = true;
    }
    return isHomepageType;
  }
  
  public String getHomepageUrl() {
    return homepageUrl;
  }

  public void setHomepageUrl(String homepageUrl) {
    this.homepageUrl = homepageUrl;
  }

  public String getAbstract() {
    return absstract;
  }

  public void setAbstract(String absstract) {
    this.absstract = absstract;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ArrayList<String> getContentCssSelectors() {
    return contentCssSelectors;
  }

  public void setContentCssSelectors(ArrayList<String> contentCssSelectors) {
    this.contentCssSelectors = contentCssSelectors;
  }

  public String getRdfPath() {
    return rdfPath;
  }

  public ArrayList<Database> getDatabasesByType(String dbType) {
    ArrayList<Database> retDBs = null;
    if (databases != null) {
      java.util.Collection<Database> databaseValues = this.databases.values();
      for (Database db : databaseValues) {
        String type = db.getType();
        if (type != null && type.equals(dbType)) {
          if (retDBs == null) {
            retDBs = new ArrayList<Database>();
          }
          retDBs.add(db);
        }
      }
    }
    return retDBs;
  }
  
  public ArrayList<Database> getDatabasesByName(String dbName) {
    ArrayList<Database> retDBs = new ArrayList<Database>();
    if (databases != null) {
      java.util.Collection<Database> databaseValues = this.databases.values();
      for (Database db : databaseValues) {
        String name = db.getName();
        if (name != null && name.equals(dbName))
          retDBs.add(db);
      }
    }
    return retDBs;
  }
  
  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (rdfId != null)
      retJsonObject.put("rdfId", rdfId);
    if (type != null) {
      String typeLabel = type.getLabel();
      retJsonObject.put("type", typeLabel);
    }
    if (homepageUrl != null) {
      try {
        homepageUrl = URIUtil.encodeQuery(homepageUrl);
      } catch (Exception e) {
        // nothing
      }
      retJsonObject.put("url", homepageUrl);
    }
    if (title != null)
      retJsonObject.put("label", title);
    if (absstract != null)
      retJsonObject.put("abstract", absstract);
    ArrayList<String> languageLabels = getLanguageLabels();
    if (languageLabels != null) {
      JSONArray jsonLanguages = new JSONArray();
      for (int i=0; i<languageLabels.size(); i++) {
        String lang = languageLabels.get(i);
        jsonLanguages.add(lang);
      }
      retJsonObject.put("languages", jsonLanguages);
    }
    if (subjects != null && ! subjects.isEmpty()) {
      JSONArray jsonSubjects = new JSONArray();
      for (Iterator<Subject> iterator = subjects.values().iterator(); iterator.hasNext();) {
        Subject subject = iterator.next();
        jsonSubjects.add(subject.toJsonObject());
      }
      retJsonObject.put("subjects", jsonSubjects);
    }
    String place = getPlace();
    if (place != null) {
      retJsonObject.put("place", place);
    }
    String periodOfTime = getPeriodOfTime();
    if (periodOfTime != null) {
      retJsonObject.put("periodOfTime", periodOfTime);
    }
    if (gndRelatedPersons != null && ! gndRelatedPersons.isEmpty()) {
      JSONArray jsonGndRelatedPersons = new JSONArray();
      for (Iterator<Person> iterator = gndRelatedPersons.values().iterator(); iterator.hasNext();) {
        Person person = iterator.next();
        jsonGndRelatedPersons.add(person.toJsonObject());
      }
      retJsonObject.put("persons", jsonGndRelatedPersons);
    }
    if (staff != null && ! staff.isEmpty()) {
      JSONArray jsonStaff = new JSONArray();
      for (Iterator<Person> iterator = staff.values().iterator(); iterator.hasNext();) {
        Person person = iterator.next();
        jsonStaff.add(person.toJsonObject());
      }
      retJsonObject.put("staff", jsonStaff);
    }
    if (collections != null && ! collections.isEmpty()) {
      JSONArray jsonCollections = new JSONArray();
      for (Iterator<ProjectCollection> iterator = collections.iterator(); iterator.hasNext();) {
        ProjectCollection collection = iterator.next();
        jsonCollections.add(collection.toJsonObject());
      }
      retJsonObject.put("collections", jsonCollections);
    }
    return retJsonObject;
  }
}
