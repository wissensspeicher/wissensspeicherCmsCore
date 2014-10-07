package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.json.simple.JSONObject;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class DBpediaResource implements Comparable<DBpediaResource> {
  private String baseUrl;
  private String uri;
  private String name;
  private Integer support;
  private String types;
  private Double similarity;
  private Integer frequency;

  public static ArrayList<DBpediaResource> fromXmlStr(XQueryEvaluator xQueryEvaluator, String xmlStr) throws ApplicationException {
    ArrayList<DBpediaResource> retEntities = null;
    XdmValue xmdValues = xQueryEvaluator.evaluate(xmlStr, "//resource");
    if (xmdValues != null && xmdValues.size() > 0) {
      XdmSequenceIterator xmdValuesIterator = xmdValues.iterator();
      retEntities = new ArrayList<DBpediaResource>();
      while (xmdValuesIterator.hasNext()) {
        XdmItem xdmItem = xmdValuesIterator.next();
        String xdmItemStr = xdmItem.toString();
        DBpediaResource entity = new DBpediaResource();
        String uri = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/resource/uri)");
        if (uri != null)
          entity.setUri(uri);
        String name = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/resource/name)");
        if (name != null && ! name.isEmpty()) {
          entity.setName(name);
        }
        String supportStr = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/resource/support)");
        if (supportStr != null)
          entity.setSupport(new Integer(supportStr));
        String similarityStr = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/resource/similarity)");
        if (similarityStr != null)
          entity.setSimilarity(new Double(similarityStr));
        String types = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/resource/types)");
        if (types != null)
          entity.setTypes(types);
        String frequencyStr = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/resource/frequency)");
        if (frequencyStr != null)
          entity.setFrequency(new Integer(frequencyStr));
        retEntities.add(entity);
      }
      Collections.sort(retEntities);
    }
    return retEntities;
  }
  
  public String getBaseUrl() {
    return baseUrl;
  }
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
  public String getUri() {
    return uri;
  }
  public void setUri(String uri) {
    this.uri = uri;
    int begin = uri.lastIndexOf("resource/") + 9;
    name = uri.substring(begin);
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public Integer getSupport() {
    return support;
  }
  public void setSupport(Integer support) {
    this.support = support;
  }
  public String getTypes() {
    return types;
  }
  public void setTypes(String types) {
    this.types = types;
  }
  public Double getSimilarity() {
    return similarity;
  }
  public void setSimilarity(Double similarity) {
    this.similarity = similarity;
  }
  public Integer getFrequency() {
    return frequency;
  }
  public void setFrequency(Integer frequency) {
    this.frequency = frequency;
  }

  public String toXmlStr() {
    if (uri == null)
      return null;
    String retStr = "<resource>";
    retStr = retStr + "\n<uri>" + uri + "</uri>";
    if (support != null)
      retStr = retStr + "\n<support>" + support + "</support>";
    if (types != null)
      retStr = retStr + "\n<types>" + StringUtils.deresolveXmlEntities(types) + "</types>";
    if (similarity != null)
      retStr = retStr + "\n<similarity>" + similarity + "</similarity>"; 
    if (frequency != null)
      retStr = retStr + "\n<frequency>" + frequency + "</frequency>";
    retStr = retStr + "\n</resource>";
    return retStr;
  }
  
  public String toRdfStr() {
    if (uri == null)
      return null;
    String retStr = "<dcterms:subject>\n";
    retStr = retStr + "  <rdf:Description rdf:about=\"" + uri + "\">\n";
    retStr = retStr + "    <rdf:type rdf:resource=\"http://www.w3.org/2004/02/skos/core#Concept\"/>\n";
    retStr = retStr + "    <rdfs:label>" + name + "</rdfs:label>\n";
    // TODO 
    /*
    if (support != null)
      retStr = retStr + "\n<support>" + support + "</support>";
    if (types != null)
      retStr = retStr + "\n<types>" + StringUtils.deresolveXmlEntities(types) + "</types>";
    if (similarity != null)
      retStr = retStr + "\n<similarity>" + similarity + "</similarity>"; 
    if (frequency != null)
      retStr = retStr + "\n<frequency>" + frequency + "</frequency>";
    */
    retStr = retStr + "  </rdf:Description>\n";
    retStr = retStr + "</dcterms:subject>\n";
    return retStr;
  }
  
  public String toHtmlStr() {
    if (name == null || uri == null)
      return null;
    String retStr = "<span class=\"entity\">";
    String queryHtmlStr = "<a href=\"" + baseUrl + "/query/QueryDocuments?query=entities:&quot;" + name + "&quot;\">" + name + "</a>";
    String refHtmlStr = "<a href=\"" + uri + "\"><img src=\"../images/rdfSmall.gif\" alt=\"DBpedia resource\" border=\"0\" height=\"15\" width=\"15\"></a>";
    retStr = retStr + queryHtmlStr + " (" + refHtmlStr + ")" + "</span>";
    retStr = retStr + "</span>";
    return retStr;
  }

  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (name == null || uri == null)
      return null;
    retJsonObject.put("name", name);
    try {
      String refEnc = URIUtil.encodeQuery(uri);
      retJsonObject.put("reference", refEnc);
    } catch (URIException e) {}
    try {
      String queryLink = baseUrl + "/query/QueryDocuments?query=entities:\"" + name + "\"";
      String queryLinkEnc = URIUtil.encodeQuery(queryLink);
      retJsonObject.put("referenceQuery", queryLinkEnc);
    } catch (URIException e) {}
    return retJsonObject;
  }

  @Override
  public int compareTo(DBpediaResource r) {  
    return name.compareToIgnoreCase(r.name);
  }  

  public String toString() {
    return name + "[" + uri + "]";
  }
  
}
