package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.lucene.index.IndexableField;
import org.bbaw.wsp.cms.collections.Project;
import org.bbaw.wsp.cms.collections.ProjectCollection;
import org.bbaw.wsp.cms.collections.ProjectReader;
import org.bbaw.wsp.cms.collections.Subject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class Document {
  private static XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
  private static Comparator<String> ignoreCaseComparator = new Comparator<String>() {
    public int compare(String s1, String s2) {
      return s1.compareToIgnoreCase(s2);
    }
  };
  private org.apache.lucene.document.Document document;
  private Float score;
  private ArrayList<String> hitFragments;
  private String firstHitPageNumber;  // page number of first hit
  private String baseUrl;
  
  public Document(org.apache.lucene.document.Document luceneDocument) {
    this.document = luceneDocument;
  }

  public org.apache.lucene.document.Document getDocument() {
    return document;
  }
  
  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public IndexableField getField(String field) {
    if (document != null)
      return document.getField(field);
    else 
      return null;
  }
  
  public void setDocument(org.apache.lucene.document.Document document) {
    this.document = document;
  }

  public Float getScore() {
    return score;
  }

  public void setScore(Float score) {
    this.score = score;
  }

  public ArrayList<String> getHitFragments() {
    return hitFragments;
  }

  public void setHitFragments(ArrayList<String> hitFragments) {
    this.hitFragments = hitFragments;
  }

  public String getFirstHitPageNumber() {
    return firstHitPageNumber;
  }

  public void setFirstHitPageNumber(String firstHitPageNumber) {
    this.firstHitPageNumber = firstHitPageNumber;
  }

  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject jsonObject = new JSONObject();
    try {
      Float luceneScore = getScore();
      if (luceneScore != null)
        jsonObject.put("luceneScore", luceneScore);
      IndexableField docProjectIdField = getField("projectId");
      String projectId = null;
      if (docProjectIdField != null) {
        projectId = docProjectIdField.stringValue();
        jsonObject.put("projectId", projectId);
      }
      Project project = null;
      if (projectId != null) {
        JSONObject jsonProject = new JSONObject();
        project = ProjectReader.getInstance().getProject(projectId);
        jsonProject.put("id", projectId);
        if (project != null) {
          String projectTitle = project.getTitle();
          if (projectTitle != null) {
            jsonProject.put("title", projectTitle);
          }
          String projectUrl = project.getHomepageUrl();
          if (projectUrl != null) {
            String encoded = URIUtil.encodeQuery(projectUrl);
            jsonProject.put("url", encoded);
          }
          jsonObject.put("project", jsonProject);
        }
      }
      IndexableField collectionRdfIdField = getField("collectionRdfId");
      if (collectionRdfIdField != null) {
        String collectionRdfId = collectionRdfIdField.stringValue();
        if (collectionRdfId != null && ! collectionRdfId.isEmpty() && project != null) {
          JSONObject jsonCollection = new JSONObject();
          jsonCollection.put("rdfId", collectionRdfId);
          ProjectCollection coll = project.getCollection(collectionRdfId);
          String collectionTitle = coll.getTitle();
          if (collectionTitle != null)
            jsonCollection.put("title", collectionTitle);
          String collectionHomepageUrl = coll.getHomepageUrl();
          if (collectionHomepageUrl != null)
            jsonCollection.put("url", collectionHomepageUrl);
          jsonObject.put("collection", jsonCollection);
        }
      }
      IndexableField databaseRdfIdField = getField("databaseRdfId");
      if (databaseRdfIdField != null) {
        String databaseRdfId = databaseRdfIdField.stringValue();
        if (databaseRdfId != null && ! databaseRdfId.isEmpty()) {
          JSONObject jsonDatabase = new JSONObject();
          jsonDatabase.put("rdfId", databaseRdfId);
          jsonObject.put("database", jsonDatabase);
        }
      }
      IndexableField languageField = getField("language");
      String lang = "";
      if (languageField != null) {
        lang = languageField.stringValue();
      }
      IndexableField docIdField = getField("docId");
      String docId = null;
      if(docIdField != null) {
        docId = docIdField.stringValue();
        jsonObject.put("docId", docId);
      }
      IndexableField docUriField = getField("uri");
      if (docUriField != null) {
        String docUri = docUriField.stringValue();
        String encoded = URIUtil.encodeQuery(docUri);
        jsonObject.put("uri", encoded);
      }
      // project link
      IndexableField webUriField = getField("webUri");
      String webUri = null;
      if (webUriField != null)
        webUri = webUriField.stringValue();
      if (webUri != null) {
        if (! webUri.contains("%"))
          webUri = URIUtil.encodeQuery(webUri);
        webUri = webUri.replaceAll("%23", "#");
        jsonObject.put("webUri", webUri);
      }
      if (project != null) {
        String homepageUrl = project.getHomepageUrl();
        if (homepageUrl != null) {
          String encoded = URIUtil.encodeQuery(homepageUrl);
          jsonObject.put("webBaseUri", encoded);
        }
        String projectRdfId = project.getRdfId();
        if (projectRdfId != null) {
          String projectDetailsUrl = baseUrl + "/query/QueryMdSystem?query=" + URIUtil.encodeQuery(projectRdfId) + "&detailedSearch=true";
          jsonObject.put("projectDetailsUri", projectDetailsUrl);
          jsonObject.put("rdfUri", projectRdfId);
        }
      }
      IndexableField docAuthorField = getField("author");
      if (docAuthorField != null) {
        JSONArray jsonDocAuthorDetails = new JSONArray();
        IndexableField docAuthorDetailsField = getField("authorDetails");
        if (docAuthorDetailsField != null) {
          String docAuthorDetailsXmlStr = docAuthorDetailsField.stringValue();
          jsonDocAuthorDetails = docPersonsDetailsXmlStrToJson(docAuthorDetailsXmlStr, lang);
        } else {
          String docAuthor = docAuthorField.stringValue();
          docAuthor = StringUtils.resolveXmlEntities(docAuthor);
          JSONObject jsonDocAuthor = new JSONObject();
          jsonDocAuthor.put("role", "author");
          jsonDocAuthor.put("name", docAuthor);
          String aboutPersonLink = baseUrl + "/query/About?query=" + docAuthor + "&type=person";
          if (lang != null && ! lang.isEmpty())
            aboutPersonLink = aboutPersonLink + "&language=" + lang;
          String aboutLinkEnc = URIUtil.encodeQuery(aboutPersonLink);
          jsonDocAuthor.put("referenceAbout", aboutLinkEnc);
          jsonDocAuthorDetails.add(jsonDocAuthor);
        }
        jsonObject.put("author", jsonDocAuthorDetails);
      }
      IndexableField docTitleField = getField("title");
      if (docTitleField != null) {
        String docTitle = docTitleField.stringValue();
        docTitle = StringUtils.resolveXmlEntities(docTitle);
        jsonObject.put("title", docTitle);
      }
      IndexableField alternativeTitleField = getField("alternativeTitle");
      if (alternativeTitleField != null) {
        String alternativeTitle = alternativeTitleField.stringValue();
        jsonObject.put("alternativeTitle", alternativeTitle);
      }
      if (languageField != null) {
        jsonObject.put("language", lang);
      }
      IndexableField descriptionField = getField("description");
      if (descriptionField != null) {
        String description = descriptionField.stringValue();
        description = StringUtils.resolveXmlEntities(description);
        jsonObject.put("description", description);
      }
      IndexableField docDateField = getField("date");
      if (docDateField != null) {
        jsonObject.put("date", docDateField.stringValue());
      }
      IndexableField lastModifiedField = getField("lastModified");
      if (lastModifiedField != null) {
        jsonObject.put("lastModified", lastModifiedField.stringValue());
      }
      IndexableField typeField = getField("type");
      if (typeField != null) {
        String type = typeField.stringValue();
        jsonObject.put("type", type);
      }
      IndexableField systemTypeField = getField("systemType");
      if (systemTypeField != null) {
        String systemType = systemTypeField.stringValue();
        jsonObject.put("systemType", systemType);
      }
      IndexableField docPageCountField = getField("pageCount");
      if (docPageCountField != null) {
        jsonObject.put("pageCount", docPageCountField.stringValue());
      }
      IndexableField extentField = getField("extent");
      if (extentField != null) {
        jsonObject.put("extent", extentField.stringValue());
      }
      ArrayList<String> hitFragments = getHitFragments();
      JSONArray jsonFragments = new JSONArray();
      if (hitFragments != null) {
        for (int j = 0; j < hitFragments.size(); j++) {
          String hitFragment = hitFragments.get(j);
          jsonFragments.add(hitFragment);
        }
      }
      jsonObject.put("fragments", jsonFragments);
      
      IndexableField entitiesDetailsField = getField("entitiesDetails");
      if (entitiesDetailsField != null) {
        String entitiesDetailsXmlStr = entitiesDetailsField.stringValue();
        JSONArray jsonDocEntitiesDetails = docEntitiesDetailsXmlStrToJson(entitiesDetailsXmlStr, lang);
        jsonObject.put("entities", jsonDocEntitiesDetails);
      }
      
      IndexableField personsField = getField("persons");
      IndexableField personsDetailsField = getField("personsDetails");
      JSONArray jsonDocPersonsDetails = new JSONArray();
      if (personsDetailsField != null) {
        String personsDetailsXmlStr = personsDetailsField.stringValue();
        jsonDocPersonsDetails = docPersonsDetailsXmlStrToJson(personsDetailsXmlStr, lang);
      } else if (personsField != null) {
        String personsStr = personsField.stringValue();
        String[] persons = personsStr.split("###");  // separator of persons
        for (int j=0; j<persons.length; j++) {
          String personName = persons[j];
          personName = StringUtils.resolveXmlEntities(personName);
          JSONObject jsonDocPerson = new JSONObject();
          jsonDocPerson.put("role", "mentioned");
          jsonDocPerson.put("name", personName);
          String aboutPersonLink = baseUrl + "/query/About?query=" + personName + "&type=person";
          if (lang != null && ! lang.isEmpty())
            aboutPersonLink = aboutPersonLink + "&language=" + lang;
          String aboutLinkEnc = URIUtil.encodeQuery(aboutPersonLink);
          jsonDocPerson.put("referenceAbout", aboutLinkEnc);
          jsonDocPersonsDetails.add(jsonDocPerson);
        }
      }
      if (! jsonDocPersonsDetails.isEmpty())
        jsonObject.put("persons", jsonDocPersonsDetails);
  
      IndexableField placesField = getField("places");
      if (placesField != null) {
        JSONArray jsonPlaces = new JSONArray();
        String placesStr = placesField.stringValue();
        String[] places = placesStr.split("###");  // separator of places
        places = cleanNames(places);
        Arrays.sort(places, ignoreCaseComparator);
        for (int j=0; j<places.length; j++) {
          String placeName = places[j];
          if (! placeName.isEmpty()) {
            JSONObject placeNameAndLink = new JSONObject();
            String placeLink = baseUrl + "/query/About?query=" + URIUtil.encodeQuery(placeName) + "&type=place";
            if (lang != null && ! lang.isEmpty())
              placeLink = placeLink + "&language=" + lang;
            placeNameAndLink.put("name", placeName);
            placeNameAndLink.put("link", placeLink);  
            jsonPlaces.add(placeNameAndLink);
          }
        }
        jsonObject.put("places", jsonPlaces);
      }
      IndexableField subjectControlledDetailsField = getField("subjectControlledDetails");
      if (subjectControlledDetailsField != null) {
        JSONArray jsonSubjects = new JSONArray();
        String subjectControlledDetailsStr = subjectControlledDetailsField.stringValue();
        org.jsoup.nodes.Document subjectControlledDetailsDoc = Jsoup.parse(subjectControlledDetailsStr);
        Elements dctermsSubjects = subjectControlledDetailsDoc.select("subjects > dcterms|subject");
        if (dctermsSubjects != null && dctermsSubjects.size() > 0) {
          for (int j=0; j<dctermsSubjects.size(); j++) {
            Element dctermsSubject = dctermsSubjects.get(j);
            // e.g.: <dcterms:subject rdf:resource="http://d-nb.info/gnd/4037764-7"/>
            String rdfIdSubject = dctermsSubject.attr("rdf:resource");
            Subject subject = ProjectReader.getInstance().getSubject(rdfIdSubject);
            if (subject != null) {
              String subjectRdfType = subject.getType();
              String subjectRdfLink = subject.getRdfId();
              String subjectName = subject.getName();
              JSONObject jsonSubject = new JSONObject();
              jsonSubject.put("type", subjectRdfType);
              jsonSubject.put("name", subjectName);
              jsonSubject.put("link", subjectRdfLink);
              jsonSubjects.add(jsonSubject);
            }
          }
        }
        jsonObject.put("subjectsControlled", jsonSubjects);
      }
      IndexableField subjectField = getField("subject");
      if (subjectField != null) {
        JSONArray jsonSubjects = new JSONArray();
        String subjectStr = subjectField.stringValue();
        String[] subjects = subjectStr.split("[,]");  // one separator of subjects
        if (subjectStr.contains("###"))
          subjects = subjectStr.split("###");  // another separator of subjects
        subjects = cleanNames(subjects);
        Arrays.sort(subjects, ignoreCaseComparator);
        for (int j=0; j<subjects.length; j++) {
          String subjectName = subjects[j];
          if (! subjectName.isEmpty()) {
            JSONObject subjectNameAndLink = new JSONObject();
            subjectNameAndLink.put("name", subjectName);
            String subjectLink = baseUrl + "/query/About?query=" + URIUtil.encodeQuery(subjectName) + "&type=subject";
            if (lang != null && ! lang.isEmpty())
              subjectLink = subjectLink + "&language=" + lang;
            subjectNameAndLink.put("link", subjectLink);
            jsonSubjects.add(subjectNameAndLink);
          }
        }
        jsonObject.put("subjects", jsonSubjects);
      }
      IndexableField swdField = getField("swd");
      if (swdField != null) {
        JSONArray jsonSwd = new JSONArray();
        String swdStr = swdField.stringValue();
        String[] swdEntries = swdStr.split("[,]");  // separator of swd entries
        swdEntries = cleanNames(swdEntries);
        Arrays.sort(swdEntries, ignoreCaseComparator);
        for (int j=0; j<swdEntries.length; j++) {
          String swdName = swdEntries[j];
          if (! swdName.isEmpty()) {
            JSONObject swdNameAndLink = new JSONObject();
            swdNameAndLink.put("name", swdName);
            String swdLink = baseUrl + "/query/About?query=" + URIUtil.encodeQuery(swdName) + "&type=swd";
            if (lang != null && ! lang.isEmpty())
              swdLink = swdLink + "&language=" + lang;
            swdNameAndLink.put("link", swdLink);  
            jsonSwd.add(swdNameAndLink);
          }
        }
        jsonObject.put("swd", jsonSwd);
      }
      IndexableField ddcField = getField("ddc");
      if (ddcField != null) {
        JSONArray jsonDdc = new JSONArray();
        String ddcStr = ddcField.stringValue();
        if (! ddcStr.isEmpty()) {
          JSONObject ddcNameAndLink = new JSONObject();
          ddcNameAndLink.put("name", ddcStr);
          String ddcLink = baseUrl + "/query/About?query=" + URIUtil.encodeQuery(ddcStr) + "&type=ddc";
          if (lang != null && ! lang.isEmpty())
            ddcLink = ddcLink + "&language=" + lang;
          ddcNameAndLink.put("link", ddcLink);
          jsonDdc.add(ddcNameAndLink);
        }
        jsonObject.put("ddc", jsonDdc);
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return jsonObject;
  }

  private JSONArray docEntitiesDetailsXmlStrToJson(String docEntitiesDetailsXmlStr, String language) throws ApplicationException {
    ArrayList<DBpediaResource> entities = DBpediaResource.fromXmlStr(xQueryEvaluator, docEntitiesDetailsXmlStr);
    JSONArray retArray = new JSONArray();
    for (int i=0; i<entities.size(); i++) {
      DBpediaResource entity = entities.get(i);
      entity.setBaseUrl(baseUrl);
      JSONObject jsonEntity = entity.toJsonObject();
      retArray.add(jsonEntity);
    }  
    return retArray;
  }
  
  private JSONArray docPersonsDetailsXmlStrToJson(String docPersonsDetailsXmlStr, String language) throws ApplicationException {
    ArrayList<Person> persons = Person.fromXmlStr(xQueryEvaluator, docPersonsDetailsXmlStr);
    JSONArray retArray = new JSONArray();
    for (int i=0; i<persons.size(); i++) {
      Person person = persons.get(i);
      String aboutPersonLink = "/query/About?query=" + person.getName() + "&type=person";
      if (language != null && ! language.isEmpty())
        aboutPersonLink = aboutPersonLink + "&language=" + language;
      person.setAboutLink(baseUrl + aboutPersonLink);
      JSONObject jsonPerson = person.toJsonObject();
      retArray.add(jsonPerson);
    }  
    return retArray;
  }
  
  private String[] cleanNames(String[] names) {
    for (int j=0; j<names.length; j++) {
      String placeName = names[j];
      placeName = placeName.trim();
      placeName = placeName.replaceAll("-\\s([^u]?)|\\.|^-", "$1");
      names[j] = placeName;
    }
    // Dubletten entfernen
    HashSet<String> namesSet = new HashSet<String>(Arrays.asList(names));
    String[] namesArray = new String[namesSet.size()];
    namesSet.toArray(namesArray);
    return namesArray;
  }
  
}
