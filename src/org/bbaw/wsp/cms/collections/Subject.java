package org.bbaw.wsp.cms.collections;

import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Subject {
  private String rdfId;
  private String type; // e.g.: &gnd;SubjectHeading, http://www.w3.org/2004/02/skos/core#Concept, http://purl.org/dc/terms/DDC 
  private String name;
  private String gndId; // e.g.: http://d-nb.info/gnd/4099309-7

  public String getRdfId() {
    return rdfId;
  }

  public void setRdfId(String rdfId) {
    this.rdfId = rdfId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getGndId() {
    return gndId;
  }

  public void setGndId(String gndId) {
    this.gndId = gndId;
  }

  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (rdfId != null)
      retJsonObject.put("rdfId", rdfId);
    if (type != null)
      retJsonObject.put("type", type);
    if (name != null)
      retJsonObject.put("label", name);
    if (gndId != null)
      retJsonObject.put("gndId", gndId);
    return retJsonObject;
  }

}
