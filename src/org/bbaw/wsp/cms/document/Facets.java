package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.bbaw.wsp.cms.collections.Collection;
import org.bbaw.wsp.cms.collections.CollectionReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;

public class Facets implements Iterable<Facet> {
  protected String baseUrl;
  protected Hashtable<String, Facet> facets;
  protected Hashtable<String, String[]> constraints;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Hashtable<String, String[]> getConstraints() {
    return constraints;
  }

  public void setConstraints(Hashtable<String, String[]> constraints) {
    this.constraints = constraints;
  }

  public Facets(List<FacetResult> facetResults) {
    this(facetResults, null);
  }
  
  public Facets(List<FacetResult> facetResults, Hashtable<String, String[]> constraints) {
    this.constraints = constraints;
    for (int i=0; i<facetResults.size(); i++) {
      FacetResult facetResult = facetResults.get(i);
      FacetResultNode facetResultRootNode = facetResult.getFacetResultNode();
      if (facetResultRootNode.getSubResults() != null) {
        Facet facet = new Facet();
        ArrayList<FacetValue> facetValues = new ArrayList<FacetValue>();
        for ( Iterator<? extends FacetResultNode> facetIterator = facetResultRootNode.getSubResults().iterator(); facetIterator.hasNext(); ) {
          FacetResultNode facetResultNode = facetIterator.next();
          String facetName = facetResultNode.getLabel().getComponent(0);  // e.g. "collectionNames"
          String facetValue = facetResultNode.getLabel().getComponent(1);  // e.g. "mega"
          int facetCountHits = (int) facetResultNode.getValue();
          if (facet.getId() == null)
            facet.setId(facetName);
          boolean isFacetValueProper = isProperValue(facetValue);
          boolean isFacetValueInConstraints = isFacetValueInConstraints(facetName, facetValue);
          if (facetName != null && isFacetValueProper && isFacetValueInConstraints) {
            FacetValue facetNameValue = new FacetValue();
            if (facetValue.contains("<uri>")) {
              int from = facetValue.indexOf("<uri>") + 5;
              int to = facetValue.indexOf("</uri>");
              String uri = facetValue.substring(from, to);
              facetNameValue.setUri(uri);
              facetValue = facetValue.replaceAll("<uri>.+</uri>", "");
            }
            if (facetValue.contains("<gnd>")) {
              int from = facetValue.indexOf("<gnd>") + 5;
              int to = facetValue.indexOf("</gnd>");
              String gnd = facetValue.substring(from, to);
              if (gnd != null && ! gnd.isEmpty())
                facetNameValue.setGnd(gnd);
              facetValue = facetValue.replaceAll("<gnd>.*</gnd>", "");
            }
            facetNameValue.setName(facetValue);
            facetNameValue.setValue(String.valueOf(facetCountHits));
            facetNameValue.setCount(facetCountHits);
            facetValues.add(facetNameValue);
          }
        }
        if (! facetValues.isEmpty()) {
          facet.setValues(facetValues);
          if (facets == null)
            facets = new Hashtable<String, Facet>();
          facets.put(facet.getId(), facet);
        }
      }
    }
  }

  private boolean isProperValue(String val) {
    if (val == null)
      return false;
    if (val.trim().isEmpty())
      return false;
    String regExprSpecialChars = "[@\"%!#\\$\\^&\\*\\?<>\\|_~=\\+\\-\\.:,;/\\(\\)\\[\\]]";  
    String newVal = val.replaceAll(regExprSpecialChars, ""); // remove all special characters
    newVal = newVal.trim();
    if (newVal.isEmpty())
      return false;
    if (newVal.toLowerCase().equals("null"))
      return false;
    return true;
  }
  
  private boolean isFacetValueInConstraints(String fieldName, String val) {
    boolean isFacetValueInConstraints = true;
    if (constraints != null) {
      String[] facetConstraints = constraints.get(fieldName);
      if (facetConstraints != null) {
        isFacetValueInConstraints = false;
        for (int j=0; j<facetConstraints.length; j++) {
          String fc = facetConstraints[j];
          if (val.contains(fc)) {
            isFacetValueInConstraints = true;
            break;
          }
        }
      }
    }
    return isFacetValueInConstraints;
  }
  
  public int size() {
    if (facets != null)
      return facets.size();
    else
      return -1;
  }
  
  public Facet getFacet(String id) {
    return facets.get(id);
  }

  public String toString() {
    if (facets == null || facets.values() == null)
      return "";
    String retStr = "";
    ArrayList<Facet> facetList = new ArrayList<Facet>(facets.values());
    Comparator<Facet> facetComparator = new Comparator<Facet>() {
      public int compare(Facet f1, Facet f2) {
        return f1.getId().compareTo(f2.getId());
      }
    };
    Collections.sort(facetList, facetComparator);
    Comparator<FacetValue> facetValueComparator = new Comparator<FacetValue>() {
      public int compare(FacetValue fv1, FacetValue fv2) {
        return fv2.getCount().compareTo(fv1.getCount());
      }
    };
    for (int i=0; i<facetList.size(); i++) {
      Facet facet = facetList.get(i);
      retStr = retStr + facet.getId() + ": ";
      ArrayList<FacetValue> facetValues = facet.getValues();
      Collections.sort(facetValues, facetValueComparator);
      for (int j=0; j<facetValues.size(); j++) {
        FacetValue facetValue = facetValues.get(j);
        retStr = retStr + facetValue.toString() + ", ";
      }
      retStr = retStr.substring(0, retStr.length() - 2);  // remove last ", "
      retStr = retStr + ";\n";
    }
    return retStr;
  }

  public String toHtmlString() {
    if (facets == null || facets.values() == null)
      return "";
    String retStr = "<ul>";
    ArrayList<Facet> facetList = new ArrayList<Facet>(facets.values());
    Comparator<Facet> facetComparator = new Comparator<Facet>() {
      public int compare(Facet f1, Facet f2) {
        return f1.getId().compareTo(f2.getId());
      }
    };
    Collections.sort(facetList, facetComparator);
    Comparator<FacetValue> facetValueComparator = new Comparator<FacetValue>() {
      public int compare(FacetValue fv1, FacetValue fv2) {
        return fv2.getCount().compareTo(fv1.getCount());
      }
    };
    for (int i=0; i<facetList.size(); i++) {
      Facet facet = facetList.get(i);
      ArrayList<FacetValue> facetValues = facet.getValues();
      Collections.sort(facetValues, facetValueComparator);
      String facetId = facet.getId();
      retStr = retStr + "<li>" + facetId;
      retStr = retStr + "<ul>";
      for (int j=0; j<facetValues.size(); j++) {
        FacetValue facetValue = facetValues.get(j);
        String facetValueName = facetValue.getName();
        String facetValueValue = facetValue.getValue();
        String facetValueUri = facetValue.getUri();
        String gnd = facetValue.getGnd();
        String facetValueNameHtml = facetValueName;
        if (facetValueUri != null) {
          facetValueNameHtml = " <uri>" + facetValueName + "</uri>";
          if (facetId.contains("entity")) {
            DBpediaResource entity = new DBpediaResource();
            entity.setBaseUrl(baseUrl);
            entity.setUri(facetValueUri);
            entity.setGnd(gnd);
            entity.setName(facetValueName);
            String type = "concept";
            if (facetId.equals("entityPerson"))
              type = "person";
            else if (facetId.equals("entityPlace"))
              type = "place";
            entity.setType(type);
            facetValueNameHtml = entity.toHtmlStr();
          }
        }
        retStr = retStr + "<li>" + facetValueNameHtml + ": " + facetValueValue + "</li>";
      }
      retStr = retStr + "</ul>";
      retStr = retStr + "</li>";
    }
    retStr = retStr + "</ul>";
    return retStr;
  }

  public JSONObject toJsonObject() {
    JSONObject retJsonObject = new JSONObject();
    if (facets == null || facets.values() == null)
      return retJsonObject;
    ArrayList<Facet> facetList = new ArrayList<Facet>(facets.values());
    Comparator<Facet> facetComparator = new Comparator<Facet>() {
      public int compare(Facet f1, Facet f2) {
        return f1.getId().compareTo(f2.getId());
      }
    };
    Collections.sort(facetList, facetComparator);
    Comparator<FacetValue> facetValueComparator = new Comparator<FacetValue>() {
      public int compare(FacetValue fv1, FacetValue fv2) {
        return fv2.getCount().compareTo(fv1.getCount());
      }
    };
    for (int i=0; i<facetList.size(); i++) {
      Facet facet = facetList.get(i);
      String facetId = facet.getId();
      ArrayList<FacetValue> facetValues = facet.getValues();
      Collections.sort(facetValues, facetValueComparator);
      JSONArray jsonValuesFacets = new JSONArray();
      for (int j=0; j<facetValues.size(); j++) {
        FacetValue facetValue = facetValues.get(j);
        String facetValueName = facetValue.getName();
        String facetValueValue = facetValue.getValue();
        facetValueValue = StringUtils.resolveXmlEntities(facetValueValue);
        JSONObject jsonFacetValue = new JSONObject();
        if (facetId != null && facetId.equals("collectionNames")) {
          try {
            Collection coll = CollectionReader.getInstance().getCollection(facetValueName);
            String rdfId = coll.getRdfId();
            if (rdfId == null)
              rdfId = "none";
            jsonFacetValue.put("rdfUri", rdfId); 
            Date lastModified = coll.getLastModified();
            if (lastModified != null) {
              String xsDateStrLastModified = new Util().toXsDate(lastModified);
              jsonFacetValue.put("lastModified", xsDateStrLastModified);
            }
          } catch (Exception e) {
            jsonFacetValue.put("rdfUri", "none"); 
          }
        }
        jsonFacetValue.put("count", facetValueValue); 
        String facetValueUri = facetValue.getUri();
        String facetValueGnd = facetValue.getGnd();
        if (facetValueUri == null) {
          jsonFacetValue.put("value", facetValueName);
        } else {
          if (facetId.contains("entity")) {
            DBpediaResource entity = new DBpediaResource();
            entity.setBaseUrl(baseUrl);
            entity.setUri(facetValueUri);
            entity.setGnd(facetValueGnd);
            entity.setName(facetValueName);
            String type = "concept";
            if (facetId.equals("entityPerson"))
              type = "person";
            else if (facetId.equals("entityPlace"))
              type = "place";
            entity.setType(type);
            JSONObject jsonEntity = entity.toJsonObject();
            jsonFacetValue.put("value", jsonEntity);
          } 
        }
        jsonValuesFacets.add(jsonFacetValue);
      }
      retJsonObject.put(facet.getId(), jsonValuesFacets);
    }
    return retJsonObject;
  }

	@Override
	public Iterator<Facet> iterator() {
		return facets.values().iterator();
	}

}
