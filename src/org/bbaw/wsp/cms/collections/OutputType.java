package org.bbaw.wsp.cms.collections;

import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class OutputType {
  private String rdfId;  // e.g. http://wissensspeicher.bbaw.de/rdf/outputType#BibliographischeDatenbank
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

  public boolean isResourceContainer() {
    boolean isResourceContainer = false;
    if (rdfId != null && (rdfId.endsWith("Edition") || rdfId.endsWith("Forschungsbericht") || rdfId.endsWith("Tagungsbericht")))   // TODO
      isResourceContainer = true;
    return isResourceContainer;
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
