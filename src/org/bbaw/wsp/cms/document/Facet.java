package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Comparator;

public class Facet {
  public static Comparator<Facet> ID_COMPARATOR = new Comparator<Facet>() {
    public int compare(Facet f1, Facet f2) {
      return f1.getId().compareTo(f2.getId());
    }
  };
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
