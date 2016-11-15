package org.bbaw.wsp.cms.collections;

import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class NormdataLanguage {
  private String rdfId;  // e.g. http://lexvo.org/id/iso639-3/eng
  private String label;  

  public String getRdfId() {
    return rdfId;
  }

  public void setRdfId(String rdfId) {
    this.rdfId = rdfId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (rdfId != null)
      retJsonObject.put("rdfId", rdfId);
    if (label != null)
      retJsonObject.put("label", label);
    return retJsonObject;
  }

}
