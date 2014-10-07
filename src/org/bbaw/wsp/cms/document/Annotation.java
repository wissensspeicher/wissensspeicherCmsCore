package org.bbaw.wsp.cms.document;

import java.util.List;

public class Annotation {
  private String id;
  private List<DBpediaResource> resources;

  public Annotation() {
  }
  
  public Annotation(String id, List<DBpediaResource> resources) {
    this.id = id;
    this.resources = resources;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<DBpediaResource> getResources() {
    return resources;
  }

  public void setResources(List<DBpediaResource> resources) {
    this.resources = resources;
  }

  public boolean isEmpty() {
    if (resources == null || resources.isEmpty())
      return true;
    else 
      return false;
  }
}
