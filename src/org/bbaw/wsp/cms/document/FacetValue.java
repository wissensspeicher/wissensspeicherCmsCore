package org.bbaw.wsp.cms.document;

public class FacetValue {
  private String name;
  private String value;
  private Integer count;

  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getValue() {
    return value;
  }
  
  public void setValue(String value) {
    this.value = value;
  }
  
  public String toString() {
    return name + " [" + count + "]";
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }
  
  
}