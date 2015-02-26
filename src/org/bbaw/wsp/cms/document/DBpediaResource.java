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
  private String type;
  private String language;
  private String uri;
  private String name;
  private String gnd;
  private Integer support;
  private Double similarity;
  private Float score;
  private Integer frequency;
  private String context;

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
        if (name == null || name.isEmpty()) {
          int begin = uri.lastIndexOf("resource/") + 9;
          name = uri.substring(begin);
        }
        entity.setName(name);
        String gnd = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/resource/gnd)");
        if (gnd != null && ! gnd.isEmpty())
          entity.setGnd(gnd);
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
  public String getGnd() {
    return gnd;
  }
  public void setGnd(String gnd) {
    this.gnd = gnd;
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
  public String getLanguage() {
    return language;
  }
  public void setLanguage(String language) {
    this.language = language;
  }
  public Float getScore() {
    return score;
  }
  public void setScore(Float score) {
    this.score = score;
  }
  public String getContext() {
    return context;
  }
  public void setContext(String context) {
    this.context = context;
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
    if (name != null) {
      String nameEscaped = StringUtils.deresolveXmlEntities(name);
      nameEscaped = nameEscaped.replaceAll("'", "&apos;");
      retStr = retStr + "\n<name>" + nameEscaped + "</name>";
    }
    if (gnd != null)
      retStr = retStr + "\n<gnd>" + gnd + "</gnd>";
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
    String retStr = "<dcterms:relation>\n";
    retStr = retStr + "  <rdf:Description rdf:about=\"" + uriStrEscaped + "\">\n";
    String typeUrl = getTypeUrl(type);
    retStr = retStr + "    <rdf:type rdf:resource=\"" + typeUrl + "\"/>\n";
    retStr = retStr + "    <rdfs:label>" + nameEscaped + "</rdfs:label>\n";
    if (gnd != null)
      retStr = retStr + "    <gnd:gndIdentifier rdf:resource=\"http://d-nb.info/gnd/" + gnd + "\"/>\n";
    if (support != null)
      retStr = retStr + "    <dbpedia-spotlight:support>" + support + "</dbpedia-spotlight:support>\n";
    if (similarity != null)
      retStr = retStr + "    <dbpedia-spotlight:similarity>" + similarity + "</dbpedia-spotlight:similarity>\n"; 
    if (frequency != null)
      retStr = retStr + "    <dbpedia-spotlight:frequency>" + frequency + "</dbpedia-spotlight:frequency>\n";
    if (context != null) {
      String contextEscaped = StringUtils.deresolveXmlEntities(context);
      contextEscaped = contextEscaped.replaceAll("'", "&apos;");
      retStr = retStr + "    <dbpedia-spotlight:context>" + contextEscaped + "</dbpedia-spotlight:context>\n";
    }
    retStr = retStr + "  </rdf:Description>\n";
    retStr = retStr + "</dcterms:relation>\n";
    return retStr;
  }
  
  public String toHtmlStr(boolean showType) {
    if (name == null || uri == null)
      return null;
    String retStr = "<span>";
    String queryHtmlStr = "<a href=\"" + baseUrl + "/query/QueryDocuments?queryLanguage=lucene&query=entitiesUris:%22" + uri + "%22&fieldExpansion=none\">" + name + "</a>";
    String refHtmlStr = "<a href=\"" + uri + "\"><img src=\"../images/rdfSmall.gif\" alt=\"DBpedia resource\" border=\"0\" height=\"15\" width=\"15\"></a>";
    String gndHtmlStr = "";
    if (gnd != null && ! gnd.isEmpty())
      gndHtmlStr = ", <a href=\"" + "http://d-nb.info/gnd/" + gnd + "\">GND</a>" ;
    String scoreHtmlStr = "";
    if (score != null)
      scoreHtmlStr = ", " + score;
    String typeHtmlStr = "";
    if (showType && type != null)
      typeHtmlStr = ", " + type;
    retStr = retStr + queryHtmlStr + " (" + refHtmlStr + gndHtmlStr + scoreHtmlStr + typeHtmlStr + ")" + "</span>";
    retStr = retStr + "</span>";
    return retStr;
  }

  public String toHtmlSmartStr(boolean showType) {
    if (name == null || uri == null)
      return null;
    String retStr = "<span>";
    String queryHtmlStr = "<a href=\"" + baseUrl + "/query/query.html?queryLanguage=lucene&query=entitiesUris:%22" + uri + "%22&fieldExpansion=none\">" + name + "</a>";
    String refHtmlStr = "<a href=\"" + uri + "\"><img src=\"../images/rdfSmall.gif\" alt=\"DBpedia resource\" border=\"0\" height=\"15\" width=\"15\"></a>";
    String gndHtmlStr = "";
    if (gnd != null && ! gnd.isEmpty())
      gndHtmlStr = ", <a href=\"" + "http://d-nb.info/gnd/" + gnd + "\">GND</a>" ;
    String scoreHtmlStr = "";
    if (score != null)
      scoreHtmlStr = ", " + score;
    String typeHtmlStr = "";
    if (showType && type != null)
      typeHtmlStr = ", " + type;
    retStr = retStr + queryHtmlStr + " (" + refHtmlStr + gndHtmlStr + scoreHtmlStr + typeHtmlStr + ")" + "</span>";
    retStr = retStr + "</span>";
    return retStr;
  }

  public String toJsTreeHtmlStr(boolean showType) {
    if (name == null || uri == null)
      return null;
    String scoreHtmlStr = "";
    if (score != null)
      scoreHtmlStr = "" + score;
    String typeHtmlStr = "";
    if (showType && type != null)
      typeHtmlStr = ", " + type;
    String retStr = name + " (" + scoreHtmlStr + typeHtmlStr + ")";
    retStr = retStr + "<ul>";
    retStr = retStr + "<li data-jstree='{\"icon\":\"glyphicon glyphicon-arrow-right\"}'><a href=\"" + baseUrl + "/query/QueryDocuments?queryLanguage=lucene&query=entitiesUris:%22" + uri + "%22&fieldExpansion=none\">" + "Query documents" + "</a></li>";
    retStr = retStr + "<li data-jstree='{\"icon\":\"glyphicon glyphicon-arrow-right\"}'><a href=\"" + uri + "\">DBpedia</a></li>";
    if (gnd != null && ! gnd.isEmpty())
      retStr = retStr +  "<li data-jstree='{\"icon\":\"glyphicon glyphicon-arrow-right\"}'><a href=\"" + "http://d-nb.info/gnd/" + gnd + "\">GND</a></li>" ;
    retStr = retStr + "</ul>";
    return retStr;
  }

  public JSONObject toJsonObject() {
    JSONObject retJsonObject = new JSONObject();
    if (name == null || uri == null)
      return null;
    retJsonObject.put("label", name);
    if (type != null && ! type.isEmpty())
      retJsonObject.put("type", type);
    if (language != null)
      retJsonObject.put("language", language);
    else 
      retJsonObject.put("language", "ger");
    try {
      String refEnc = URIUtil.encodeQuery(uri);
      retJsonObject.put("uri", refEnc);
      String queryLink = baseUrl + "/query/QueryDocuments?query=entitiesUris:\"" + uri + "\"";
      String queryLinkEnc = URIUtil.encodeQuery(queryLink);
      retJsonObject.put("uriFulltextQuery", queryLinkEnc);
    } catch (URIException e) {}
    if (gnd != null && ! gnd.isEmpty())
      retJsonObject.put("uriGnd", "http://d-nb.info/gnd/" + gnd);
    if (score != null)
      retJsonObject.put("score", score);
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
