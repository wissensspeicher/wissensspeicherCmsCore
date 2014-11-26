package org.bbaw.wsp.cms.document;

import java.util.Comparator;

public class FacetValue {
  public static Comparator<FacetValue> COUNT_COMPARATOR = new Comparator<FacetValue>() {
    public int compare(FacetValue fv1, FacetValue fv2) {
      return fv2.getCount().compareTo(fv1.getCount());
    }
  };
  public static Comparator<FacetValue> SCORE_COMPARATOR = new Comparator<FacetValue>() {
    public int compare(FacetValue fv1, FacetValue fv2) {
      if (fv1.getScore() == null || fv2.getScore() == null)
        return 0;
      else 
        return fv2.getScore().compareTo(fv1.getScore());
    }
  };
  private String name;
  private String value;
  private String type;  // person, place or concept
  private Integer count; // number of documents for this facet
  private Float score; // normalized score of this facet
  private String uri;
  private String gnd;

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
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public String toString() {
    String nameStr = name;
    if (uri != null && ! uri.isEmpty() && gnd != null && !gnd.isEmpty())
      nameStr = nameStr + " [" + uri + ", " + gnd + "]";
    else if (uri != null && ! uri.isEmpty())
      nameStr = nameStr + " [" + uri + "]";
    String retStr = nameStr + ": " + value;
    return retStr;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }
  
  public Float getScore() {
    return score;
  }
  public void setScore(Float score) {
    this.score = score;
  }
  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getGnd() {
    return gnd;
  }
  public void setGnd(String gnd) {
    this.gnd = gnd;
  }
  @Override
  public int hashCode() {
  	final int prime = 31;
  	int result = 1;
  	result = prime * result + ((count == null) ? 0 : count.hashCode());
  	result = prime * result + ((name == null) ? 0 : name.hashCode());
  	result = prime * result + ((value == null) ? 0 : value.hashCode());
  	return result;
  }

  @Override
  public boolean equals(Object obj) {
  	if (this == obj)
  		return true;
  	if (obj == null)
  		return false;
  	if (getClass() != obj.getClass())
  		return false;
  	FacetValue other = (FacetValue) obj;
  	if (count == null) {
  		if (other.count != null)
  			return false;
  	} else if (!count.equals(other.count))
  		return false;
  	if (name == null) {
  		if (other.name != null)
  			return false;
  	} else if (!name.equals(other.name))
  		return false;
  	if (value == null) {
  		if (other.value != null)
  			return false;
  	} else if (!value.equals(other.value))
  		return false;
  	return true;
  }
  
}
