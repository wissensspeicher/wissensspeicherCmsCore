package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.Person;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class ProjectCollection {
  private static Logger LOGGER = Logger.getLogger(ProjectReader.class);
  private String id;
  private String rdfId;
  private String projectRdfId; // rdfId of project
  private String parentRdfId; // rdfId of parent collection
  private String rdfPath; // path from this collection to root (project or organization)
  private String typeRdfId;  // e.g. http://wissensspeicher.bbaw.de/rdf/outputType#BibliographischeDatenbank
  private String homepageUrl;
  private String absstract; // abstract of project
  private String title;
  private ArrayList<String> languages = new ArrayList<String>(); // collection languages; first language is the main language
  private HashMap<String, Person> staff = new HashMap<String, Person>();  // collection staff: key is personRdfId and value is person
  private HashMap<String, Database> databases = new HashMap<String, Database>();  // // collection databases: key is databaseRdfId and value is database
  
  public ProjectCollection(String projectRdfId) {
    this.projectRdfId = projectRdfId;
  }
  
  public void addLanguage(String language) {
    languages.add(language);
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
  
  public boolean isResourceContainer() {
    boolean isResourceContainer = false;
    if (typeRdfId != null && (typeRdfId.endsWith("Edition") || typeRdfId.endsWith("Forschungsbericht") || typeRdfId.endsWith("Tagungsbericht")))   // TODO
      isResourceContainer = true;
    return isResourceContainer;
  }
  
  public void addStaffPerson(String personRdfId, Person person) {
    staff.put(personRdfId, person);
  }
  
  public Person getStaffPerson(String personRdfId) {
    return staff.get(personRdfId);
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
  
  private String calcRdfPath() throws ApplicationException {
    Project project = ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
    if (parentRdfId == null)
      return project.getRdfPath() + "###" + rdfId;
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

  public String getProjectRdfId() {
    return projectRdfId;
  }

  public void setProjectRdfId(String projectRdfId) {
    this.projectRdfId = projectRdfId;
  }

  public String getParentRdfId() {
    return parentRdfId;
  }
  
  public void setParentRdfId(String parentRdfId) {
    this.parentRdfId = parentRdfId;
  }
  
  public String getTypeRdfId() {
    return typeRdfId;
  }

  public void setTypeRdfId(String typeRdfId) {
    this.typeRdfId = typeRdfId;
  }

  public String getHomepageUrl() {
    return homepageUrl;
  }

  public void setHomepageUrl(String homepageUrl) {
    this.homepageUrl = homepageUrl;
  }

  public String getAbsstract() {
    return absstract;
  }

  public void setAbsstract(String absstract) {
    this.absstract = absstract;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getRdfPath() {
    return rdfPath;
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
  
  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (id != null)
      retJsonObject.put("id", id);
    if (rdfId != null)
      retJsonObject.put("rdfId", rdfId);
    if (projectRdfId != null)
      retJsonObject.put("projectRdfId", projectRdfId);
    if (parentRdfId != null)
      retJsonObject.put("parentRdfId", parentRdfId);
    if (rdfPath != null)
      retJsonObject.put("rdfPath", rdfPath);
    if (homepageUrl != null)
      retJsonObject.put("homepageUrl", homepageUrl);
    if (title != null)
      retJsonObject.put("label", title);
    if (absstract != null)
      retJsonObject.put("abstract", absstract);
    if (languages != null && ! languages.isEmpty()) {
      JSONArray jsonLanguages = new JSONArray();
      for (int i=0; i<languages.size(); i++) {
        String lang = languages.get(i);
        jsonLanguages.add(lang);
      }
      retJsonObject.put("languages", jsonLanguages);
    }
    if (staff != null && ! staff.isEmpty()) {
      JSONArray jsonStaff = new JSONArray();
      for (Iterator<Person> iterator = staff.values().iterator(); iterator.hasNext();) {
        Person person = iterator.next();
        jsonStaff.add(person.toJsonObject());
      }
      retJsonObject.put("staff", jsonStaff);
    }
    return retJsonObject;
  }
}
