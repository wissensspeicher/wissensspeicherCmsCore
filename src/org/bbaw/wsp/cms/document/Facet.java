package org.bbaw.wsp.cms.document;

import java.util.ArrayList;

public class Facet {
  private String id;
  private ArrayList<FacetValue> values;

  public Facet() {
  }
  
  public Facet(String id, ArrayList<FacetValue> values) {
    this.id = id;
    this.values = values;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ArrayList<FacetValue> getValues() {
    return values;
  }

  public void setValues(ArrayList<FacetValue> values) {
    this.values = values;
  }

}
