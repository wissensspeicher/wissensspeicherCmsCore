package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.Person;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.Util;

public class Project {
  private static Logger LOGGER = Logger.getLogger(Project.class);
  public static Comparator<Person> personNameComparator = new Comparator<Person>() {
    public int compare(Person p1, Person p2) {
      return p1.getName().compareTo(p2.getName());
    }
  };
  public static Comparator<Subject> subjectNameComparator = new Comparator<Subject>() {
    public int compare(Subject s1, Subject s2) {
      return s1.getName().compareTo(s2.getName());
    }
  };
  private String id;  // nick name
  private String rdfId;  // rdfId of this project
  private String parentRdfId;  // rdf id of which this project is part of 
  private String rdfPath; // path from this project to root (project or organization)
  private String organizationRdfId; // rdf id of organization (e.g.: http://wissensspeicher.bbaw.de/rdf/normdata/BBAW)
  private String type; // project or organization
  private String projectType; // e.g. "Vorhaben", "Initiative"
  private String title;  // title of project
  private String absstract; // abstract of project
  private String homepageUrl; // homepage url
  private String spatialRdfId; // spatial, e.g. "http://sws.geonames.org/2950157/"
  private String temporalRdfId; // temporal, e.g. "http://wissensspeicher.bbaw.de/rdf/normdata/Neuzeit"
  private Date lastModified; // last modified
  private String status; // e.g. "aktiv" or "abgeschlossen" 
  private String duration; // e.g. "start=1970; end=2014-12-31; name=Laufzeit"
  private String updateCycle; // e.g. "monthly" or "halfyearly" 
  private String mainLanguage;
  private ArrayList<String> languages = new ArrayList<String>(); // all languages
  private ArrayList<String> languageRdfIds = new ArrayList<String>(); // language rdf ids
  private HashMap<String, ProjectCollection> allProjectCollections = new HashMap<String, ProjectCollection>();  // all project collections: key is collectionRdfId and value is collection
  private ArrayList<ProjectCollection> collections = new ArrayList<ProjectCollection>();  // sub collections
  private HashMap<String, Database> databases = new HashMap<String, Database>();  // project databases (redundant to collection databases): key is databaseRdfId and value is database
  private HashMap<String, Subject> subjects = new HashMap<String, Subject>();  // project subjects: key is subjectRdfId and value is subject
  private HashMap<String, Person> gndRelatedPersons = new HashMap<String, Person>();  // project gnd related persons: key is personRdfId and value is person
  private HashMap<String, Person> staff = new HashMap<String, Person>();  // project staff: key is personRdfId and value is person
  private HashMap<String, String> projectRelations = new HashMap<String, String>();  // project relations: key is projectRdfId and value is the same projectRdfId

  public ProjectCollection getCollection(String collRdfId) {
    return allProjectCollections.get(collRdfId);
  }
  
  public void addCollection(String collRdfId, ProjectCollection collection) {
    allProjectCollections.put(collRdfId, collection);
  }
  
  public void addSubCollection(ProjectCollection collection) {
    collections.add(collection);
  }
  
  public void addDatabase(String databaseRdfId, Database database) {
    databases.put(databaseRdfId, database);
  }
  
  public void addStaffPerson(String personRdfId, Person person) {
    staff.put(personRdfId, person);
  }
  
  public Person getStaffPerson(String personRdfId) {
    return staff.get(personRdfId);
  }
  
  public void addSubject(String subjectRdfId, Subject subject) {
    subjects.put(subjectRdfId, subject);
  }
  
  public Subject getSubject(String subjectRdfId) {
    return subjects.get(subjectRdfId);
  }
  
  public void addGndRelatedPerson(String personRdfId, Person person) {
    gndRelatedPersons.put(personRdfId, person);
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
  
  public void addProjectRelation(String projectRdfId) {
    projectRelations.put(projectRdfId, projectRdfId);
  }
  
  public ArrayList<ProjectCollection> getCollections() {
    return collections;
  }
  
  public ArrayList<ProjectCollection> getAllCollections() {
    ArrayList<ProjectCollection> retCollections = null;
    if (allProjectCollections != null && ! allProjectCollections.isEmpty()) {
      retCollections = new ArrayList<ProjectCollection>();
      java.util.Collection<ProjectCollection> values = this.allProjectCollections.values();
      for (ProjectCollection collection : values) {
        retCollections.add(collection);
      }
    }
    return retCollections;
  }
  
  public ArrayList<Person> getStaff() {
    ArrayList<Person> staffPersons = null;
    if (staff != null) {
      staffPersons = new ArrayList<Person>();
      java.util.Collection<Person> values = this.staff.values();
      for (Person person : values) {
        staffPersons.add(person);
      }
      Collections.sort(staffPersons, personNameComparator);
    }
    return staffPersons;
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
  
  public HashMap<String, Subject> getSubjectsHashMap() {
    return this.subjects;
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

  public ArrayList<Database> getDatabases() {
    ArrayList<Database> retDBs = new ArrayList<Database>();
    if (databases != null) {
      java.util.Collection<Database> databaseValues = this.databases.values();
      for (Database db : databaseValues) {
        retDBs.add(db);
      }
    }
    return retDBs;
  }
  
  public ArrayList<Database> getDatabasesByType(String dbType) {
    ArrayList<Database> retDBs = new ArrayList<Database>();
    if (databases != null) {
      java.util.Collection<Database> databaseValues = this.databases.values();
      for (Database db : databaseValues) {
        String type = db.getType();
        if (type != null && type.equals(dbType))
          retDBs.add(db);
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
  
  public void buildSubCollections() throws ApplicationException {
    java.util.Collection<ProjectCollection> collectionValues = this.allProjectCollections.values();
    for (ProjectCollection collection : collectionValues) {
      String collParentRdfId = collection.getParentRdfId();
      if (collParentRdfId == null) { // parent is project
        addSubCollection(collection);
      } else {  // parent is collection
        ProjectCollection parentCollection = getCollection(collParentRdfId);
        if (parentCollection == null) {
          LOGGER.error("Build subcollections: parent collection does not exist: " + collParentRdfId);
        } else {
          parentCollection.addSubCollection(collection);
          // subcollections with crawlDB's: add exclude
          ArrayList<Database> parentCrawlDBs = parentCollection.getDatabasesByType("crawl");
          ArrayList<Database> subcollectionCrawlDBs = collection.getDatabasesByType("crawl");
          if (parentCrawlDBs != null && subcollectionCrawlDBs != null) {
            Database parentCrawlDB = parentCrawlDBs.get(0);
            String parentCrawlDBRdfId = parentCrawlDB.getRdfId(); // only one db is supported
            String subcollectionCrawlDBRdfId = subcollectionCrawlDBs.get(0).getRdfId(); // only one db is supported
            if (subcollectionCrawlDBRdfId.startsWith(parentCrawlDBRdfId)) {
              String excludeStr = subcollectionCrawlDBRdfId.substring(parentCrawlDBRdfId.length());
              excludeStr = excludeStr + "/.*";
              parentCrawlDB.addExclude(excludeStr);
            }
          }
        }
      }
    }
  }
  
  public void buildRdfPathes() throws ApplicationException {
    this.rdfPath = calcRdfPath();
    java.util.Collection<ProjectCollection> collectionValues = this.allProjectCollections.values();
    for (ProjectCollection collection : collectionValues) {
      collection.buildRdfPath();
    }
  }
  
  private String calcRdfPath() throws ApplicationException {
    if (parentRdfId == null)
      return rdfId;
    Project parentProject = ProjectReader.getInstance().getProjectByRdfId(parentRdfId);
    if (parentProject == null)
      return rdfId;
    String projectRdfPath = parentProject.calcRdfPath() + "###" + rdfId;
    return projectRdfPath;
  }
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getRdfId() {
    return rdfId;
  }
  public void setRdfId(String rdfId) {
    this.rdfId = rdfId;
  }
  public String getParentRdfId() {
    return parentRdfId;
  }
  public void setParentRdfId(String parentRdfId) {
    this.parentRdfId = parentRdfId;
  }
  public String getRdfPath() {
    return rdfPath;
  }
  public String getOrganizationRdfId() {
    return organizationRdfId;
  }
  public void setOrganizationRdfId(String organizationRdfId) {
    this.organizationRdfId = organizationRdfId;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public String getProjectType() {
    return projectType;
  }
  public void setProjectType(String projectType) {
    this.projectType = projectType;
  }
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getAbstract() {
    return absstract;
  }
  public void setAbstract(String absstract) {
    this.absstract = absstract;
  }
  public String getTemporalRdfId() {
    return temporalRdfId;
  }
  public void setTemporalRdfId(String temporalRdfId) {
    this.temporalRdfId = temporalRdfId;
  }
  public String getSpatialRdfId() {
    return spatialRdfId;
  }
  public void setSpatialRdfId(String spatialRdfId) {
    this.spatialRdfId = spatialRdfId;
  }
  public Date getLastModified() {
    return lastModified;
  }
  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }
  public String getDuration() {
    return duration;
  }
  public void setDuration(String duration) {
    this.duration = duration;
  }
  public String getHomepageUrl() {
    return homepageUrl;
  }
  public void setHomepageUrl(String homepageUrl) {
    this.homepageUrl = homepageUrl;
  }
  public String getUpdateCycle() {
    return updateCycle;
  }
  public void setUpdateCycle(String updateCycle) {
    this.updateCycle = updateCycle;
  }
  public String getMainLanguage() {
    return mainLanguage;
  }
  public String getMainLanguageLabel() throws ApplicationException {
    String mainLanguageLabel = null;
    ArrayList<String> languageRdfIds = getLanguageRdfIds();
    if (languageRdfIds != null && ! languageRdfIds.isEmpty()) {
      String mainLanguageRdfId = languageRdfIds.get(0);
      NormdataLanguage normdataLanguage = ProjectReader.getInstance().getNormdataLanguage(mainLanguageRdfId);
      if (normdataLanguage != null)
        mainLanguageLabel = normdataLanguage.getLabel();
    }
    return mainLanguageLabel;
  }
  public void setMainLanguage(String mainLanguage) {
    this.mainLanguage = mainLanguage;
  }

  public JSONObject toJsonObject(boolean withCollections) throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (id != null)
      retJsonObject.put("id", id);
    if (rdfId != null)
      retJsonObject.put("rdfId", rdfId);
    if (organizationRdfId != null)
      retJsonObject.put("organizationRdfId", organizationRdfId);
    if (type != null)
      retJsonObject.put("type", type);
    if (projectType != null)
      retJsonObject.put("projectType", projectType);
    if (homepageUrl != null) {
      String encodedHomepageUrl = null;
      try {
        encodedHomepageUrl = URIUtil.encodeQuery(homepageUrl);
      } catch (Exception e) {
        // nothing
      }
      retJsonObject.put("url", encodedHomepageUrl);
    }
    if (title != null)
      retJsonObject.put("label", title);
    if (absstract != null)
      retJsonObject.put("abstract", absstract);
    if (duration != null)
      retJsonObject.put("duration", duration);
    if (status != null)
      retJsonObject.put("status", status);
    String mainLanguageLabel = getMainLanguageLabel();
    if (mainLanguageLabel != null)
      retJsonObject.put("mainLanguage", mainLanguageLabel);
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
    if (withCollections && collections != null && ! collections.isEmpty()) {
      JSONArray jsonCollections = new JSONArray();
      for (Iterator<ProjectCollection> iterator = collections.iterator(); iterator.hasNext();) {
        ProjectCollection collection = iterator.next();
        jsonCollections.add(collection.toJsonObject());
      }
      retJsonObject.put("collections", jsonCollections);
    }
    if (projectRelations != null && ! projectRelations.isEmpty()) {
      JSONArray jsonProjectRelations = new JSONArray();
      for (Iterator<String> iterator = projectRelations.values().iterator(); iterator.hasNext();) {
        String relatedProjectRdfId = iterator.next();
        jsonProjectRelations.add(relatedProjectRdfId);
      }
      retJsonObject.put("projectRelations", jsonProjectRelations);
    }
    if (lastModified != null) {
      String xsDateStrLastModified = new Util().toXsDate(lastModified);
      retJsonObject.put("lastModified", xsDateStrLastModified);
    }
    return retJsonObject;
  }
}
