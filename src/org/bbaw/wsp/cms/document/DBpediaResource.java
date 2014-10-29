package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Comparator;

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
  private String type;
  private Double similarity;
  private Integer frequency;

  public static Comparator<DBpediaResource> FrequencyComparatorDESC = new Comparator<DBpediaResource>() {
    public int compare(DBpediaResource r1, DBpediaResource r2) {
      if (r1.frequency == null && r2.frequency == null)
        return 0;
      if (r1.frequency == null)
        return 1;
      if (r2.frequency == null)
        return -1;
      return r2.frequency.compareTo(r1.frequency);
    }
  };
  
  public static Comparator<DBpediaResource> SimilarityComparatorDESC = new Comparator<DBpediaResource>() {
    public int compare(DBpediaResource r1, DBpediaResource r2) {
      if (r1.similarity == null && r2.similarity == null)
        return 0;
      if (r1.similarity == null)
        return 1;
      if (r2.similarity == null)
        return -1;
      if (r1.similarity.equals(r2.similarity))
        return r2.frequency.compareTo(r1.frequency);
      return r2.similarity.compareTo(r1.similarity);
    }
  };
  
  public static Comparator<DBpediaResource> NameComparator = new Comparator<DBpediaResource>() {
    public int compare(DBpediaResource r1, DBpediaResource r2) {
      if (r1.name == null && r2.name == null)
        return 0;
      if (r1.name == null)
        return -1;
      if (r2.name == null)
        return 1;
      return r1.name.compareTo(r2.name);
    }
  };
  
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
        String type = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/resource/type)");
        if (type != null)
          entity.setType(type);
        String frequencyStr = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/resource/frequency)");
        if (frequencyStr != null)
          entity.setFrequency(new Integer(frequencyStr));
        retEntities.add(entity);
      }
      // Collections.sort(retEntities);
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
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
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
    String uriStrEscaped = StringUtils.deresolveXmlEntities(uri);
    uriStrEscaped = uriStrEscaped.replaceAll("'", "&apos;");
    retStr = retStr + "\n<uri>" + uriStrEscaped + "</uri>";
    if (type != null)
      retStr = retStr + "\n<type>" + type + "</type>";
    if (support != null)
      retStr = retStr + "\n<support>" + support + "</support>";
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
    String uriStrEscaped = StringUtils.deresolveXmlEntities(uri);
    uriStrEscaped = uriStrEscaped.replaceAll("'", "&apos;");
    String nameEscaped = StringUtils.deresolveXmlEntities(name);
    nameEscaped = nameEscaped.replaceAll("'", "&apos;");
    String retStr = "<dcterms:relation rdf:resource=\"" + uriStrEscaped + "\">\n";
    String typeUrl = getTypeUrl(type);
    retStr = retStr + "  <rdf:type rdf:resource=\"" + typeUrl + "\"/>\n";
    retStr = retStr + "  <rdfs:label>" + nameEscaped + "</rdfs:label>\n";
    if (support != null)
      retStr = retStr + "  <dbpedia-spotlight:support>" + support + "</dbpedia-spotlight:support>\n";
    if (similarity != null)
      retStr = retStr + "  <dbpedia-spotlight:similarity>" + similarity + "</dbpedia-spotlight:similarity>\n"; 
    if (frequency != null)
      retStr = retStr + "  <dbpedia-spotlight:frequency>" + frequency + "</dbpedia-spotlight:frequency>\n";
    retStr = retStr + "</dcterms:relation>\n";
    return retStr;
  }
  
  public String toHtmlStr() {
    if (name == null || uri == null)
      return null;
    String retStr = "<span class=\"" + type + "\">";
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
    if (type != null && ! type.isEmpty())
      retJsonObject.put("type", type);
    try {
      String refEnc = URIUtil.encodeQuery(uri);
      retJsonObject.put("reference", refEnc);
      String queryLink = baseUrl + "/query/QueryDocuments?query=entities:\"" + name + "\"";
      String queryLinkEnc = URIUtil.encodeQuery(queryLink);
      retJsonObject.put("referenceQuery", queryLinkEnc);
    } catch (URIException e) {}
    if (support != null)
      retJsonObject.put("dbpediaSpotlightSupport", support);
    if (similarity != null)
      retJsonObject.put("dbpediaSpotlightSimilarity", similarity);
    if (frequency != null)
      retJsonObject.put("dbpediaSpotlightFrequency", frequency);
    return retJsonObject;
  }

  @Override
  public int compareTo(DBpediaResource r) {  
    if (name == null && r.name == null)
      return 0;
    if (name == null)
      return -1;
    if (r.name == null)
      return 1;
    return name.compareToIgnoreCase(r.name);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DBpediaResource) {
      String toCompare = ((DBpediaResource) o).uri;
      return uri.equals(toCompare);
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return uri.hashCode();
  }  

  public String toString() {
    return name + "[" + uri + "]";
  }

  private String getTypeUrl(String simpleType) {
    String typeUrl = null;
    if (simpleType.equals("person"))
      typeUrl = "http://dbpedia.org/ontology/Person";
    else if (simpleType.equals("organisation"))
      typeUrl = "http://dbpedia.org/ontology/Organisation";
    else if (simpleType.equals("place"))
      typeUrl = "http://dbpedia.org/ontology/Place";
    else if (simpleType.equals("concept"))
      typeUrl = "http://www.w3.org/2004/02/skos/core#Concept";
    else 
      typeUrl = "http://www.w3.org/2004/02/skos/core#Concept";
    return typeUrl;
  }
}