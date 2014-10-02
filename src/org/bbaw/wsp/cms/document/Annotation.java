package org.bbaw.wsp.cms.document;

import java.util.List;

public class Annotation {
  private String id;
  private String spotlightAnnotationXmlStr;
  private List<String> resources;

  public Annotation() {
  }
  
  public Annotation(String id, List<String> resources) {
    this.id = id;
    this.resources = resources;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSpotlightAnnotationXmlStr() {
    return spotlightAnnotationXmlStr;
  }

  public void setSpotlightAnnotationXmlStr(String spotlightAnnotationXmlStr) {
    this.spotlightAnnotationXmlStr = spotlightAnnotationXmlStr;
  }

  public List<String> getResources() {
    return resources;
  }

  public void setResources(List<String> resources) {
    this.resources = resources;
  }

  public boolean isEmpty() {
    if (resources == null || resources.isEmpty())
      return true;
    else 
      return false;
  }
}
