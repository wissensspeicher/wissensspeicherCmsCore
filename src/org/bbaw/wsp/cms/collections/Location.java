package org.bbaw.wsp.cms.collections;

import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Location {
  private String rdfId;
  private String type; // e.g.: &dcterms;Location 
  private String label;
  private String country; // e.g.: http://d-nb.info/gnd/4099309-7

  public String getRdfId() {
    return rdfId;
  }

  public void setRdfId(String rdfId) {
    this.rdfId = rdfId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (rdfId != null)
      retJsonObject.put("rdfId", rdfId);
    if (type != null)
      retJsonObject.put("type", type);
    if (label != null)
      retJsonObject.put("label", label);
    if (country != null)
      retJsonObject.put("country", country);
    return retJsonObject;
  }

}
