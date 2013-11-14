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
    return name + " [" + count + "] : " + value;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
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
