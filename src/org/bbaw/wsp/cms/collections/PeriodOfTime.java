package org.bbaw.wsp.cms.collections;

import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class PeriodOfTime {
  private String rdfId;
  private String label;
  private String period; // e.g.: "start=-1200; end=0600; name=Antike"

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

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (rdfId != null)
      retJsonObject.put("rdfId", rdfId);
    if (label != null)
      retJsonObject.put("label", label);
    if (period != null)
      retJsonObject.put("period", period);
    return retJsonObject;
  }

}
