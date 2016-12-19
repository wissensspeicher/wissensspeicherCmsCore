package org.bbaw.wsp.cms.document;

import java.util.ArrayList;

import org.bbaw.wsp.cms.collections.Project;
import org.bbaw.wsp.cms.collections.ProjectCollection;
import org.bbaw.wsp.cms.collections.ProjectReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class GroupDocuments {
  private String query; // groupBy query which lead to this GroupDocuments 
  private String name;  // groupBy value e.g. "avh" if query was "projectId"
  private int size; // count of total hits 
  private float maxScore; // max score of this group
  private ArrayList<Document> documents;
  private String baseUrl;
  
  public void addDocument(Document document) {
    if (documents == null)
      documents = new ArrayList<Document>();
    documents.add(document);
  }

  public boolean hasDocuments() {
    if (documents == null || documents.isEmpty())
      return false;
    else 
      return true;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String groupName) {
    this.name = groupName;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public float getMaxScore() {
    return maxScore;
  }

  public void setMaxScore(float maxScore) {
    this.maxScore = maxScore;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("groupName", name);
    jsonObject.put("numberOfHits", size);
    jsonObject.put("maxScore", maxScore);
    if (query != null && query.startsWith("projectId")) {
      Project project = ProjectReader.getInstance().getProject(name);
      if (project != null) {
        jsonObject.put("project", project.toJsonObject(true));
      }
    } else if (query != null && query.startsWith("collectionRdfId")) {
      ProjectCollection collection = ProjectReader.getInstance().getCollection(name);
      Project project = collection.getProject();
      if (collection != null) {
        jsonObject.put("project", project.toJsonObject(false));
        jsonObject.put("collection", collection.toJsonObject());
      }
    }
    JSONArray jsonHits = new JSONArray();
    for (int i=0; i<documents.size(); i++) {
      Document doc = documents.get(i);
      doc.setBaseUrl(baseUrl);
      JSONObject jsonHit = doc.toJsonObject();
      jsonHits.add(jsonHit);
    }
    jsonObject.put("hits", jsonHits);
    return jsonObject;
  }
}
