package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;

public class Facets {
  private Hashtable<String, Facet> facets;

  public Facets(List<FacetResult> facetResults) {
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
          FacetValue facetNameValue = new FacetValue();
          facetNameValue.setName(facetValue);
          facetNameValue.setValue(String.valueOf(facetCountHits));
          facetNameValue.setCount(facetCountHits);
          facetValues.add(facetNameValue);
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
      retStr = retStr + "<li>" + facet.getId();
      retStr = retStr + "<ul>";
      for (int j=0; j<facetValues.size(); j++) {
        FacetValue facetValue = facetValues.get(j);
        retStr = retStr + "<li>" + facetValue.toString() + "</li>";
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
      ArrayList<FacetValue> facetValues = facet.getValues();
      Collections.sort(facetValues, facetValueComparator);
      JSONArray jsonValuesFacets = new JSONArray();
      for (int j=0; j<facetValues.size(); j++) {
        FacetValue facetValue = facetValues.get(j);
        String facetValueName = facetValue.getName();
        String facetValueValue = facetValue.getValue();
        facetValueValue = StringUtils.resolveXmlEntities(facetValueValue);
        JSONObject jsonFacetValue = new JSONObject();
        jsonFacetValue.put("value", facetValueName);
        jsonFacetValue.put("count", facetValueValue); 
        jsonValuesFacets.add(jsonFacetValue);
      }
      retJsonObject.put(facet.getId(), jsonValuesFacets);
    }
    return retJsonObject;
  }

}
