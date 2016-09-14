package org.bbaw.wsp.cms.collections;

public class Subject {
  private String rdfId;
  private String type; // e.g.: &gnd;SubjectHeading, http://www.w3.org/2004/02/skos/core#Concept, http://purl.org/dc/terms/DDC 
  private String name;
  private String gndIdentifier; // e.g.: http://d-nb.info/gnd/4099309-7

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

  public String getGndIdentifier() {
    return gndIdentifier;
  }

  public void setGndIdentifier(String gndIdentifier) {
    this.gndIdentifier = gndIdentifier;
  }


}
