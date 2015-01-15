package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.bbaw.wsp.cms.collections.Collection;
import org.bbaw.wsp.cms.collections.CollectionReader;
import org.bbaw.wsp.cms.dochandler.DBpediaSpotlightHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;

public class Facets implements Iterable<Facet> {
  private static float FACET_COUNT_WEIGHT = new Float(0.5);
  private static float SUPPORT_WEIGHT = new Float(0.5);
  protected String baseUrl;
  protected String outputOptions = "showAllFacets";
  protected Hashtable<String, Facet> facets;
  protected Hashtable<String, String[]> constraints;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getOutputOptions() {
    return outputOptions;
  }

  public void setOutputOptions(String outputOptions) {
    this.outputOptions = outputOptions;
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
    DBpediaSpotlightHandler dbPediaSpotlightHandler = null;
    try {
      dbPediaSpotlightHandler = DBpediaSpotlightHandler.getSupportInstance();
    }  catch (ApplicationException e) {
      // nothing
    }
    for (int i=0; i<facetResults.size(); i++) {
      FacetResult facetResult = facetResults.get(i);
      FacetResultNode facetResultRootNode = facetResult.getFacetResultNode();
      if (facetResultRootNode.getSubResults() != null) {
        Facet facet = new Facet();
        ArrayList<FacetValue> facetValues = new ArrayList<FacetValue>();
        int maxFacetCountHits = -1;
        int maxFacetSupport = -1;
        for ( Iterator<? extends FacetResultNode> facetIterator = facetResultRootNode.getSubResults().iterator(); facetIterator.hasNext(); ) {
          FacetResultNode facetResultNode = facetIterator.next();
          String facetName = facetResultNode.getLabel().getComponent(0);  // e.g. "collectionNames"
          String facetValue = facetResultNode.getLabel().getComponent(1);  // e.g. "mega"
          int facetCountHits = (int) facetResultNode.getValue();
          if (facetCountHits > maxFacetCountHits)
            maxFacetCountHits = facetCountHits;
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
              Integer support = dbPediaSpotlightHandler.getSupport(uri);
              if (support != null) {
                if (support > maxFacetSupport)
                  maxFacetSupport = support;
              }
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
            if (facetName.equals("entityPerson"))
              facetNameValue.setType("person");
            else if (facetName.equals("entityOrganisation"))
              facetNameValue.setType("organisation");
            else if (facetName.equals("entityPlace"))
              facetNameValue.setType("place");
            else if (facetName.equals("entityConcept"))
              facetNameValue.setType("concept");
            if (isProper(facetNameValue))
              facetValues.add(facetNameValue);
          }
        }
        if (! facetValues.isEmpty()) {
          for (int j=0; j<facetValues.size(); j++) {
            FacetValue fv = facetValues.get(j);
            float count = (float) fv.getCount();
            String uri = fv.getUri();
            float score = count / maxFacetCountHits; // e.g.: 55 / 92
            if (uri != null) {
              float facetCountScore = score;
              Integer support = dbPediaSpotlightHandler.getSupport(uri);
              if (support != null) {
                float supportLength = support.toString().length();
                float maxFacetSupportLength = new Integer(maxFacetSupport).toString().length();
                // score = (count * supportLength) / (maxFacetCountHits * maxFacetSupportLength); // e.g.: 55 * 5 / 92 * 6
                // float supportScore = support / maxFacetSupport;
                float supportScore = supportLength / maxFacetSupportLength;
                score = (facetCountScore * FACET_COUNT_WEIGHT) + (supportScore * SUPPORT_WEIGHT); // e.g. Lohnarbeit = (3/8 * 0.5) + (3/5 * 0.5) = 0.4875
              }
            }
            fv.setScore(score);
          }
          facet.setValues(facetValues);
          if (facets == null)
            facets = new Hashtable<String, Facet>();
          facets.put(facet.getId(), facet);
        }
      }
    }
    // add the virtual mainEntities facet
    Facet mainEntityFacet = buildMainEntitiesFacet();
    if (mainEntityFacet != null)
      facets.put(mainEntityFacet.getId(), mainEntityFacet);
  }

  private Facet buildMainEntitiesFacet() {
    if (facets == null)
      return null;
    ArrayList<FacetValue> mainEntityValues = new ArrayList<FacetValue>();
    Facet facetEntityPerson = facets.get("entityPerson");
    Facet facetEntityOrganisation = facets.get("entityOrganisation");
    Facet facetEntityPlace = facets.get("entityPlace");
    Facet facetEntityConcept = facets.get("entityConcept");
    if (facetEntityPerson != null) {
      ArrayList<FacetValue> facetValuesPerson = facetEntityPerson.getValues();
      Collections.sort(facetValuesPerson, FacetValue.SCORE_COMPARATOR);
      int countPersons = 3;
      ArrayList<FacetValue> facetValues = extractFacetValues(countPersons, facetValuesPerson);
      mainEntityValues.addAll(facetValues);
    }
    if (facetEntityOrganisation != null) {
      ArrayList<FacetValue> facetValuesOrganisation = facetEntityOrganisation.getValues();
      Collections.sort(facetValuesOrganisation, FacetValue.SCORE_COMPARATOR);
      int countOrganisations = 3;
      ArrayList<FacetValue> facetValues = extractFacetValues(countOrganisations, facetValuesOrganisation);
      mainEntityValues.addAll(facetValues);
    }
    if (facetEntityPlace != null) {
      ArrayList<FacetValue> facetValuesPlace = facetEntityPlace.getValues();
      Collections.sort(facetValuesPlace, FacetValue.SCORE_COMPARATOR);
      int countPlaces = 3;
      ArrayList<FacetValue> facetValues = extractFacetValues(countPlaces, facetValuesPlace);
      mainEntityValues.addAll(facetValues);
    }
    if (facetEntityConcept != null) {
      ArrayList<FacetValue> facetValuesConcept = facetEntityConcept.getValues();
      Collections.sort(facetValuesConcept, FacetValue.SCORE_COMPARATOR);
      int countConcepts = 18 - mainEntityValues.size();
      ArrayList<FacetValue> facetValues = extractFacetValues(countConcepts, facetValuesConcept);
      mainEntityValues.addAll(facetValues);
    }
    Collections.sort(mainEntityValues, FacetValue.SCORE_COMPARATOR);
    Facet mainEntityFacet = new Facet("mainEntities", mainEntityValues);
    return mainEntityFacet;
  }

  private ArrayList<FacetValue> extractFacetValues(int count, ArrayList<FacetValue> facetValues) {
    int counter = 0;
    ArrayList<FacetValue> retFacetValues = new ArrayList<FacetValue>();
    while (counter < count && counter < facetValues.size()) {
      FacetValue fv = facetValues.get(counter);
      if (isProper(fv)) {
        retFacetValues.add(fv);
      } else {
        count++; // if value is not proper then count is incremented
      }
      counter++;
    }
    return retFacetValues;
  }
  
  private boolean isProper(FacetValue fv) {
    DBpediaSpotlightHandler dbPediaSpotlightHandler = null;
    try {
      dbPediaSpotlightHandler = DBpediaSpotlightHandler.getInstance();
    } catch (ApplicationException e) {
      // nothing
    }
    String uri = fv.getUri();
    if (uri == null)
      return true;
    else if (dbPediaSpotlightHandler != null && uri != null && dbPediaSpotlightHandler.isProperUri(uri))
      return true;
    else 
      return false;
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
    Collections.sort(facetList, Facet.ID_COMPARATOR);
    for (int i=0; i<facetList.size(); i++) {
      Facet facet = facetList.get(i);
      retStr = retStr + facet.getId() + ": ";
      ArrayList<FacetValue> facetValues = facet.getValues();
      Collections.sort(facetValues, FacetValue.COUNT_COMPARATOR);
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
    Collections.sort(facetList, Facet.ID_COMPARATOR);
    for (int i=0; i<facetList.size(); i++) {
      Facet facet = facetList.get(i);
      ArrayList<FacetValue> facetValues = facet.getValues();
      String facetId = facet.getId();
      if (facetId.contains("entity") || facetId.equals("mainEntities"))
        Collections.sort(facetValues, FacetValue.SCORE_COMPARATOR);
      else
        Collections.sort(facetValues, FacetValue.COUNT_COMPARATOR);
      if ((outputOptions.contains("showMainEntitiesFacet") && facetId.equals("mainEntities")) || outputOptions.contains("showAllFacets") || outputOptions.contains("showAll")) {
        retStr = retStr + "<li>" + facetId;
        retStr = retStr + "<ul>";
        for (int j=0; j<facetValues.size(); j++) {
          FacetValue facetValue = facetValues.get(j);
          String facetValueName = facetValue.getName();
          String facetValueValue = facetValue.getValue();
          float facetValueScore = facetValue.getScore();
          String facetValueUri = facetValue.getUri();
          String gnd = facetValue.getGnd();
          String facetValueNameHtml = facetValueName;
          if (facetValueUri != null) {
            facetValueNameHtml = " <uri>" + facetValueName + "</uri>";
            if (facetId.contains("entity") || facetId.equals("mainEntities")) {
              DBpediaResource entity = new DBpediaResource();
              entity.setBaseUrl(baseUrl);
              entity.setUri(facetValueUri);
              entity.setGnd(gnd);
              entity.setName(facetValueName);
              entity.setScore(facetValueScore);
              String type = "concept";
              String facetValueType = facetValue.getType();
              if (facetValueType != null)
                type = facetValueType;
              if (facetId.equals("entityPerson"))
                type = "person";
              else if (facetId.equals("entityOrganisation"))
                type = "organisation";
              else if (facetId.equals("entityPlace"))
                type = "place";
              entity.setType(type);
              facetValueNameHtml = entity.toHtmlStr(false);
              if (facetId.equals("mainEntities"))
                facetValueNameHtml = entity.toHtmlStr(true);
            }
          }
          retStr = retStr + "<li>" + facetValueNameHtml + ": " + facetValueValue + "</li>";
        }
        retStr = retStr + "</ul>";
        retStr = retStr + "</li>";
      }
    }
    retStr = retStr + "</ul>";
    return retStr;
  }

  public JSONObject toJsonObject() {
    JSONObject retJsonObject = new JSONObject();
    if (facets == null || facets.values() == null)
      return retJsonObject;
    ArrayList<Facet> facetList = new ArrayList<Facet>(facets.values());
    Collections.sort(facetList, Facet.ID_COMPARATOR);
    for (int i=0; i<facetList.size(); i++) {
      Facet facet = facetList.get(i);
      String facetId = facet.getId();
      if ((outputOptions.contains("showMainEntitiesFacet") && facetId.equals("mainEntities")) || outputOptions.contains("showAllFacets") || outputOptions.contains("showAll")) {
        ArrayList<FacetValue> facetValues = facet.getValues();
        if (facetId.contains("entity") || facetId.equals("mainEntities"))
          Collections.sort(facetValues, FacetValue.SCORE_COMPARATOR);
        else
          Collections.sort(facetValues, FacetValue.COUNT_COMPARATOR);
        JSONArray jsonValuesFacets = new JSONArray();
        for (int j=0; j<facetValues.size(); j++) {
          FacetValue facetValue = facetValues.get(j);
          String facetValueName = facetValue.getName();
          String facetValueValue = facetValue.getValue();
          facetValueValue = StringUtils.resolveXmlEntities(facetValueValue);
          float facetValueScore = facetValue.getScore();
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
            if (facetId.contains("entity") || facetId.equals("mainEntities")) {
              DBpediaResource entity = new DBpediaResource();
              entity.setBaseUrl(baseUrl);
              entity.setUri(facetValueUri);
              entity.setGnd(facetValueGnd);
              entity.setName(facetValueName);
              entity.setScore(facetValueScore);
              String type = "concept";
              String facetValueType = facetValue.getType();
              if (facetValueType != null)
                type = facetValueType;
              if (facetId.equals("entityPerson"))
                type = "person";
              else if (facetId.equals("entityOrganisation"))
                type = "organisation";
              else if (facetId.equals("entityPlace"))
                type = "place";
              entity.setType(type);
              JSONObject jsonEntity = entity.toJsonObject();
              jsonFacetValue = jsonEntity;
            }
          }
          jsonValuesFacets.add(jsonFacetValue);
        }
        retJsonObject.put(facet.getId(), jsonValuesFacets);
      }
    }
    return retJsonObject;
  }

	@Override
	public Iterator<Facet> iterator() {
		return facets.values().iterator();
	}

}
