package org.bbaw.wsp.cms.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.LabelAndValue;
import org.bbaw.wsp.cms.collections.DBpediaSpotlightHandler;
import org.bbaw.wsp.cms.collections.Organization;
import org.bbaw.wsp.cms.collections.OutputType;
import org.bbaw.wsp.cms.collections.Project;
import org.bbaw.wsp.cms.collections.ProjectCollection;
import org.bbaw.wsp.cms.collections.ProjectReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;

public class Facets implements Iterable<Facet> {
  private static Logger LOGGER = Logger.getLogger(Facets.class);
  private static float FACET_COUNT_WEIGHT = new Float(0.5);
  private static float SUPPORT_WEIGHT = new Float(0.5);
  private static ProjectReader projectReader;
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
      Facet facet = new Facet();
      ArrayList<FacetValue> facetValues = new ArrayList<FacetValue>();
      int maxFacetCountHits = -1;
      int maxFacetSupport = -1;
      LabelAndValue[] labelValues = facetResult.labelValues;
      for (int j=0; j<labelValues.length; j++) {
        LabelAndValue labelValue = labelValues[j];
        String facetName = facetResult.dim;
        String facetValue = labelValue.label; 
        int facetCountHits = labelValue.value.intValue();
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
    // add the virtual mainEntities facet
    Facet mainEntityFacet = buildMainEntitiesFacet();
    if (mainEntityFacet != null)
      facets.put(mainEntityFacet.getId(), mainEntityFacet);
    // add the virtual collectionGroup facets
    Collection<Facet> collectionGroupFacets = buildCollectionGroupFacets();
    if (collectionGroupFacets != null) {
      for (Facet collectionFacet : collectionGroupFacets) {
        if (collectionFacet.getValues() != null && ! collectionFacet.getValues().isEmpty())
          facets.put(collectionFacet.getId(), collectionFacet);
      }
    }
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

  private Collection<Facet> buildCollectionGroupFacets() {
    if (facets == null)
      return null;
    initProjectReader();
    String[] collectionFacetNames = 
      {"groupedCollectionType", "groupedCollectionPeriodOfTime", "groupedCollectionPerson", "groupedCollectionSubject", "groupedCollectionLanguage", "groupedCollectionOrganization", "groupedCollectionProject"};
    HashMap<String, Facet> collectionFacets = new HashMap<String, Facet>();
    for (String collectionFacetName : collectionFacetNames) {
      Facet collectionFacet = new Facet(collectionFacetName, new ArrayList<FacetValue>());
      collectionFacets.put(collectionFacetName, collectionFacet);
    }
    Facet facetCollectionRdfId = facets.get("collectionRdfId");
    if (facetCollectionRdfId != null) {
      ArrayList<FacetValue> facetValuesCollectionRdfId = facetCollectionRdfId.getValues();
      for (FacetValue facetValueCollectionRdfId : facetValuesCollectionRdfId) {
        String collectionRdfId = facetValueCollectionRdfId.getName();
        ProjectCollection collection = projectReader.getCollection(collectionRdfId);
        if (collection != null) {
          for (String collectionFacetName : collectionFacetNames) {
            ArrayList<FacetValue> facetValues = collectionFacets.get(collectionFacetName).getValues();
            ArrayList<String> fieldValues = getFieldValues(collectionFacetName, collection);
            if (fieldValues != null) {
              for (String fieldValue : fieldValues) {
                FacetValue facetValue = getFacetValue(fieldValue, facetValues);
                if (facetValue != null) {
                  Integer count = facetValue.getCount();
                  if (count != null) {
                    Integer newCount = count + 1;
                    facetValue.setCount(newCount);
                    facetValue.setValue(newCount.toString());
                  }
                } else {
                  facetValue = new FacetValue();
                  facetValue.setName(fieldValue);
                  facetValue.setCount(1);
                  facetValue.setValue("1");
                  facetValues.add(facetValue);
                }
              }
            }
          }
        }
      }
    }
    return collectionFacets.values();
  }

  private void initProjectReader() {
    try {
      projectReader = ProjectReader.getInstance();
    } catch (Exception e) {
      // nothing 
    }
  }
  
  private ArrayList<String> getFieldValues(String fieldName, ProjectCollection collection) {
    ArrayList<String> retValues = null;
    if (fieldName.equals("groupedCollectionType")) {
      OutputType type = collection.getType();
      if (type != null) {
        retValues = new ArrayList<String>();
        retValues.add(type.getLabel());
        return retValues;
      }
    } else if (fieldName.equals("groupedCollectionPeriodOfTime")) {
      try {
        String periodOfTime = collection.getPeriodOfTime();
        if (periodOfTime == null)
          periodOfTime = collection.getProject().getPeriodOfTime();
        if (periodOfTime != null) {
          retValues = new ArrayList<String>();
          retValues.add(periodOfTime);
        }
        return retValues;
      } catch (ApplicationException e) {
        return null;
      }
    } else if (fieldName.equals("groupedCollectionPerson")) {
      return collection.getGndRelatedPersonsStr();
    } else if (fieldName.equals("groupedCollectionSubject")) {
      return collection.getSubjectsStr();
    } else if (fieldName.equals("groupedCollectionLanguage")) {
      try {
        retValues = collection.getLanguageLabels();
        return retValues;
      } catch (ApplicationException e) {
        return null;
      }
    } else if (fieldName.equals("groupedCollectionProject")) {
      try {
        Project project = collection.getProject();
        retValues = new ArrayList<String>();
        retValues.add(project.getTitle());
        return retValues;
      } catch (ApplicationException e) {
        return null;
      }
    } else if (fieldName.equals("groupedCollectionOrganization")) {
      try {
        String orgRdfId = collection.getProject().getOrganizationRdfId();
        if (orgRdfId != null) {
          Project rootProject = projectReader.getProjectByRdfId(orgRdfId);
          if (rootProject != null) {
            retValues = new ArrayList<String>();
            retValues.add(rootProject.getTitle());
          }
        }
        return retValues;
      } catch (ApplicationException e) {
        return null;
      }
    }
    return retValues;
  }
  
  private FacetValue getFacetValue(String facetValueValue, ArrayList<FacetValue> facetValues) {
    FacetValue facetValue = null;
    for (FacetValue fValue : facetValues) {
      String fValueValue = fValue.getName();
      if (fValueValue != null && fValueValue.equals(facetValueValue))
        return fValue;
    }
    return facetValue;
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
      dbPediaSpotlightHandler = DBpediaSpotlightHandler.getStopwordsInstance();
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

  public String toHtmlString(boolean smart) {
    if (facets == null || facets.values() == null)
      return "";
    StringBuilder retStrBuilder = new StringBuilder();
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
        if (facetId.equals("mainEntities")) {
          retStrBuilder.append("<ul><li data-jstree='{\"icon\":\"glyphicon glyphicon-th-large\",\"opened\":true,\"selected\":true}'>" + facetId);
        } else { 
          retStrBuilder.append("<ul><li data-jstree='{\"icon\":\"glyphicon glyphicon-th-large\"}'>" + facetId);
        }
        retStrBuilder.append("<ul>");
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
              if (facetId.equals("mainEntities"))
                facetValueNameHtml = entity.toJsTreeHtmlStr(true, smart);
              else
                facetValueNameHtml = entity.toJsTreeHtmlStr(false, smart);
            }
          }
          if (j==0)
            retStrBuilder.append("<li data-jstree='{\"icon\":\"glyphicon glyphicon-star\",\"opened\":true}'>" + facetValueNameHtml + ": " + facetValueValue + "</li>");
          else
            retStrBuilder.append("<li data-jstree='{\"icon\":\"glyphicon glyphicon-star\"}'>" + facetValueNameHtml + ": " + facetValueValue + "</li>");
        }
        retStrBuilder.append("</ul>");
      }
      retStrBuilder.append("</ul>");
    }
    return retStrBuilder.toString();
  }

  public JSONObject toJsonObject() {
    JSONObject retJsonObject = new JSONObject();
    if (facets == null || facets.values() == null)
      return retJsonObject;
    ArrayList<Facet> facetList = new ArrayList<Facet>(facets.values());
    Collections.sort(facetList, Facet.ID_COMPARATOR);
    ProjectReader projectReader = null;
    try {
      projectReader = ProjectReader.getInstance();
    } catch (Exception e) {
      // nothing 
    }
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
          Float facetValueScore = facetValue.getScore();
          JSONObject jsonFacetValue = new JSONObject();
          jsonFacetValue.put("count", facetValueValue); 
          String facetValueUri = facetValue.getUri();
          String facetValueGnd = facetValue.getGnd();
          if (facetValueUri == null) {
            jsonFacetValue.put("label", facetValueName);
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
              jsonEntity.put("count", facetValueValue);
              jsonFacetValue = jsonEntity;
            }
          }
          if (facetId.equals("collectionRdfId") || facetId.equals("collection")) {
            try {
              ProjectCollection collection = projectReader.getCollection(facetValueName);
              JSONObject jsonCollection = collection.toJsonObject();
              jsonCollection.put("count", facetValueValue);
              jsonFacetValue = jsonCollection;
              facetId = "collection";
            } catch (Exception e) {
              LOGGER.error("Facets.toJson(): Collection: " + facetValueName + " not found by ProjectReader (" + e.getMessage() + ")");
            }
          } else if (facetId.equals("projectRdfId") || facetId.equals("project")) {
            try {
              Project project = projectReader.getProjectByRdfId(facetValueName);
              JSONObject jsonProject = project.toJsonObject(false);
              jsonProject.put("count", facetValueValue);
              jsonFacetValue = jsonProject;
              facetId = "project";
            } catch (Exception e) {
              LOGGER.error("Facets.toJson(): Project: " + facetValueName + " not found by ProjectReader (" + e.getMessage() + ")");
            }
          } else if (facetId.equals("organizationRdfId") || facetId.equals("organization")) {
            try {
              Organization organization = projectReader.getOrganization(facetValueName);
              JSONObject jsonOrganization = organization.toJsonObject();
              jsonOrganization.put("count", facetValueValue);
              jsonFacetValue = jsonOrganization;
              facetId = "organization";
            } catch (Exception e) {
              LOGGER.error("Facets.toJson(): Organization: " + facetValueName + " not found by ProjectReader (" + e.getMessage() + ")");
            }
          }
          jsonValuesFacets.add(jsonFacetValue);
        }
        retJsonObject.put(facetId, jsonValuesFacets);
      }
    }
    return retJsonObject;
  }

  @Override
  public Iterator<Facet> iterator() {
    return facets.values().iterator();
  }

}
