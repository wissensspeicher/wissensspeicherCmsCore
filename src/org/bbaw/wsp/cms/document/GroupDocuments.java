package org.bbaw.wsp.cms.document;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class GroupDocuments {
  private String name;
  private int size; // count of total hits 
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

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
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
