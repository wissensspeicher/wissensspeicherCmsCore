package org.bbaw.wsp.cms.collections;

import java.util.Hashtable;

public class Service {
  private String id;
  private String hostName;
  private String name;
  private Hashtable<String, String> properties;
  private Hashtable<String, String> parameters;

  public String toUrlStr() {
    String urlStr = "";
    if (hostName != null)
      urlStr = urlStr + "http://" + hostName;
    if (name != null)
      urlStr = urlStr + name;
    return urlStr;
  }
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Hashtable<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Hashtable<String, String> parameters) {
    this.parameters = parameters;
  }

  public Hashtable<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Hashtable<String, String> properties) {
    this.properties = properties;
  }

  public String getParamValue(String paramName) {
    if (parameters == null)
      return null;
    else return parameters.get(paramName);
  }
  
  public String getPropertyValue(String propName) {
    if (properties == null)
      return null;
    else return properties.get(propName);
  }
  
}
