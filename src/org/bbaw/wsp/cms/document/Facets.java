package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;

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
          String facetValue = facetResultNode.getLabel().getComponent(1);  // e.g. "gcs"
          double facetCountHits = facetResultNode.getValue();
          if (facet.getId() == null)
            facet.setId(facetName);
          FacetValue facetNameValue = new FacetValue();
          facetNameValue.setName(facetValue);
          facetNameValue.setValue(String.valueOf(facetCountHits));
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
    String retStr = "";
    for ( Iterator<Facet> facetIterator = facets.values().iterator(); facetIterator.hasNext(); ) {
      Facet facet = facetIterator.next();
      retStr = retStr + facet.getId() + ": ";
      ArrayList<FacetValue> values = facet.getValues();
      for (int i=0; i<values.size(); i++) {
        FacetValue facetValue = values.get(i);
        retStr = retStr + facetValue.toString() + ", ";
      }
      retStr = retStr.substring(0, retStr.length() - 2);  // remove last ", "
      retStr = retStr + ";\n";
    }
    return retStr;
  }

  public String toJsonString() {
    // TODO
    String retStr = "";
    for ( Iterator<Facet> facetIterator = facets.values().iterator(); facetIterator.hasNext(); ) {
      Facet facet = facetIterator.next();
      retStr = retStr + facet.getId() + ": ";
      retStr = retStr + facet.getValues().toString();
      retStr = retStr + ";";
    }
    return retStr;
  }

}
