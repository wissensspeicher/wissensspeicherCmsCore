package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.bbaw.wsp.cms.document.Person;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Project {
  public static Comparator<ProjectCollection> projectCollectionIdComparator = new Comparator<ProjectCollection>() {
    public int compare(ProjectCollection p1, ProjectCollection p2) {
      return p1.getId().compareTo(p2.getId());
    }
  };
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
  private String temporalRdfId; // temporal, e.g. "http://wissensspeicher.bbaw.de/rdf/normdata/Neuzeit"
  private String spatialRdfId; // spatial, e.g. "http://sws.geonames.org/2950157/"
  private Date lastModified; // last modified
  private String status; // e.g. "aktiv" or "abgeschlossen" 
  private String valid; // e.g. "start=1970; end=2014-12-31; name=Laufzeit"
  private String updateCycle; // e.g. "monthly" or "halfyearly" 
  private String mainLanguage;
  private ArrayList<String> languages = new ArrayList<String>(); // all languages
  private HashMap<String, ProjectCollection> allProjectCollections = new HashMap<String, ProjectCollection>();  // all project collections: key is collectionRdfId and value is collection
  private ArrayList<ProjectCollection> collections = new ArrayList<ProjectCollection>();  // sub collections
  private HashMap<String, Database> databases = new HashMap<String, Database>();  // project databases (redundant to collection databases): key is databaseRdfId and value is database
  private HashMap<String, Person> staff = new HashMap<String, Person>();  // project staff: key is personRdfId and value is person
  private HashMap<String, Subject> subjects = new HashMap<String, Subject>();  // project subjects: key is subjectRdfId and value is subject
  private HashMap<String, String> projectRelations = new HashMap<String, String>();  // project relations: key is projectRdfId and value is the same projectRdfId
  private HashMap<String, Person> gndRelatedPersons = new HashMap<String, Person>();  // project gnd related persons: key is personRdfId and value is person

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
  
  public void addProjectRelation(String projectRdfId) {
    projectRelations.put(projectRdfId, projectRdfId);
  }
  
  public ArrayList<ProjectCollection> getCollections() {
    return collections;
  }
  
  public ArrayList<Person> getStaff() {
    ArrayList<Person> staffPersons = new ArrayList<Person>();
    if (staff != null) {
      java.util.Collection<Person> values = this.staff.values();
      for (Person person : values) {
        staffPersons.add(person);
      }
    }
    Collections.sort(staffPersons, personNameComparator);
    return staffPersons;
  }
  
  public ArrayList<Subject> getSubjects() {
    ArrayList<Subject> subjects = new ArrayList<Subject>();
    if (staff != null) {
      java.util.Collection<Subject> values = this.subjects.values();
      for (Subject subject : values) {
        subjects.add(subject);
      }
    }
    Collections.sort(subjects, subjectNameComparator);
    return subjects;
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
      // if (collParentRdfId.equals(rdfId)) {  
        addSubCollection(collection);
      } else {  // parent is collection
        ProjectCollection parentCollection = getCollection(collParentRdfId);
        parentCollection.addSubCollection(collection);
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
  public String getValid() {
    return valid;
  }
  public void setValid(String valid) {
    this.valid = valid;
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
  public void setMainLanguage(String mainLanguage) {
    this.mainLanguage = mainLanguage;
  }

  public JSONObject toJsonObject() throws ApplicationException {
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
    if (homepageUrl != null)
      retJsonObject.put("homepageUrl", homepageUrl);
    if (title != null)
      retJsonObject.put("label", title);
    if (absstract != null)
      retJsonObject.put("abstract", absstract);
    if (temporalRdfId != null) {
      PeriodOfTime pot = ProjectReader.getInstance().getPeriodOfTime(temporalRdfId);
      if (pot != null)
        retJsonObject.put("periodOfTime", pot.toJsonObject());
    }
    if (spatialRdfId != null) {
      Location location = ProjectReader.getInstance().getLocation(spatialRdfId);
      if (location != null)
        retJsonObject.put("location", location.toJsonObject());
    }
    if (status != null)
      retJsonObject.put("status", status);
    if (mainLanguage != null)
      retJsonObject.put("mainLanguage", mainLanguage);
    if (languages != null && ! languages.isEmpty()) {
      JSONArray jsonLanguages = new JSONArray();
      for (int i=0; i<languages.size(); i++) {
        String lang = languages.get(i);
        jsonLanguages.add(lang);
      }
      retJsonObject.put("languages", jsonLanguages);
    }
    if (collections != null && ! collections.isEmpty()) {
      JSONArray jsonCollections = new JSONArray();
      for (Iterator<ProjectCollection> iterator = collections.iterator(); iterator.hasNext();) {
        ProjectCollection collection = iterator.next();
        jsonCollections.add(collection.toJsonObject());
      }
      retJsonObject.put("collections", jsonCollections);
    }
    if (staff != null && ! staff.isEmpty()) {
      JSONArray jsonStaff = new JSONArray();
      for (Iterator<Person> iterator = staff.values().iterator(); iterator.hasNext();) {
        Person person = iterator.next();
        jsonStaff.add(person.toJsonObject());
      }
      retJsonObject.put("staff", jsonStaff);
    }
    if (subjects != null && ! subjects.isEmpty()) {
      JSONArray jsonSubjects = new JSONArray();
      for (Iterator<Subject> iterator = subjects.values().iterator(); iterator.hasNext();) {
        Subject subject = iterator.next();
        jsonSubjects.add(subject.toJsonObject());
      }
      retJsonObject.put("subjects", jsonSubjects);
    }
    if (projectRelations != null && ! projectRelations.isEmpty()) {
      JSONArray jsonProjectRelations = new JSONArray();
      for (Iterator<String> iterator = projectRelations.values().iterator(); iterator.hasNext();) {
        String relatedProjectRdfId = iterator.next();
        jsonProjectRelations.add(relatedProjectRdfId);
      }
      retJsonObject.put("projectRelations", jsonProjectRelations);
    }
    if (gndRelatedPersons != null && ! gndRelatedPersons.isEmpty()) {
      JSONArray jsonGndRelatedPersons = new JSONArray();
      for (Iterator<Person> iterator = gndRelatedPersons.values().iterator(); iterator.hasNext();) {
        Person person = iterator.next();
        jsonGndRelatedPersons.add(person.toJsonObject());
      }
      retJsonObject.put("gndRelatedPersons", jsonGndRelatedPersons);
    }
    return retJsonObject;
  }
}
