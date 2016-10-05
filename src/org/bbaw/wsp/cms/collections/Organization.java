package org.bbaw.wsp.cms.collections;

import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Organization {
  private String rdfId;
  private String name;
  private String homepageUrl;

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

  public String getHomepageUrl() {
    return homepageUrl;
  }

  public void setHomepageUrl(String homepageUrl) {
    this.homepageUrl = homepageUrl;
  }

  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (rdfId != null)
      retJsonObject.put("rdfId", rdfId);
    if (name != null)
      retJsonObject.put("name", name);
    if (homepageUrl != null)
      retJsonObject.put("homepageUrl", homepageUrl);
    return retJsonObject;
  }
}
