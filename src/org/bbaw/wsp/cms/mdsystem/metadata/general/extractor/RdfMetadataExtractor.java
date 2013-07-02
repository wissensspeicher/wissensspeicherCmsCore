package org.bbaw.wsp.cms.mdsystem.metadata.general.extractor;

import java.util.ArrayList;
import java.util.HashMap;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentfierSearchMode;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptQueryResult;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This class is able to parse a rdf file that is used as input for the triple store.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 25.10.2012
 * 
 *       change: 08.11.2012 - added getXmlBaseValue()-method to extract the Last change: 07.01.2013 - new Methode added for scanning Strings in Documents.
 * 
 *       first matching xml base attribut (which contains the real URL)
 */
public class RdfMetadataExtractor extends MetadataExtractor {
  /**
   * Key to access the *:type from the normdata.
   */
  public static final String TYPE = "type";
  /**
   * Key to access the *:description (* should match a dc - hopefully ;) )
   */
  private static final String DESCRIPTION = "description";
  /**
   * Key to access the *:functionOrRole (* should match gnd...)
   */
  private static final String FUNCTION_OR_ROLE = "functionOrRole";
  /**
   * The URI which identifies a project.
   */
  private static final String TYPE_PROJECT = "http://xmlns.com/foaf/0.1/Project";
  private static final String TYPE_PERSON = "http://xmlns.com/foaf/0.1/Person";
  private static final String DESCRIPTION_HISTORICAL_PERSON = "Historische Person";
  private static final String DESCRIPTION_WORK_LEADER = "Arbeitsstellenleiter";

  ArrayList<ConceptQueryResult> targetList;

  /**
   * Create a new ModsMetadataParser instance.
   * 
   * @param uri
   *          - the URI to the knowledge store metadata record.
   * @throws ApplicationException
   *           if the resource to be parsed is not validated by Saxon.
   * @throws IllegalArgumentException
   *           if the uri is null, empty or doesn't refer to an existing file.
   */
  public RdfMetadataExtractor(final String uri, final HashMap<String, String> namespaces) throws ApplicationException {
    super(uri, namespaces);
    targetList = new ArrayList<ConceptQueryResult>();
  }

  /**
   * Build path to the rdf:about attribute. It contains the identifier of the described resource.
   * 
   * @return {@link String} the attribute's value (the uri of the described resource)
   * @throws ApplicationException
   *           if the rdf:about isn't tagged
   */
  public String getRdfAboutValue() throws ApplicationException {
    final String erg = (String) buildXPath("//rdf:Description[1]/@rdf:about", false); // First
    if (erg.equals("")) {
      throw new ApplicationException("No attribute rdf:about found. This is required for the authentification of the document!");
    } // node
    return erg;
  }

  /**
   * Allows to search in the given document. Methode creates QueryTargets - Elements which contains the given String somewhere
   * 
   * @param element
   * @throws ApplicationException
   */
  public void searchElements(final String element, final int strategie) throws ApplicationException {

    final String[] elements = element.toLowerCase().split("[ ]+");

    final String number = (String) buildXPath("count(//rdf:Description)", false);

    final int count = Integer.parseInt(number);
    for (int i = 1; i <= count; ++i) {

      final String temp = (String) buildXPath("//rdf:Description[" + i + "]/*", false);

      if (checkIfContains(elements, temp.toLowerCase(), strategie)) {
        final ConceptQueryResult target = new ConceptQueryResult();
        targetList.add(target);

        String query = "//rdf:Description[" + i + "]/@rdf:about";
        addToTarget("Description", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:type/@rdf:resource";
        addToTarget(TYPE, queryExecute(query), target);

        // vllt nicht wichtig
        query = "//rdf:Description[" + i + "]/*:imports/@rdf:resource";
        addToTarget("imports", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:isPartOf/@rdf:resource";
        addToTarget("isPartOf", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:description";
        addToTarget("description", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:label";
        addToTarget("label", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:languageCode";
        addToTarget("languageCode", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:familyName";
        addToTarget("familyName", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:mbox";
        addToTarget("mbox", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:givenName";
        addToTarget("givenName", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:title";
        addToTarget("title", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:functionOrRole";
        addToTarget("functionOrRole", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:definition";
        addToTarget("definition", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:status";
        addToTarget("status", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:relation/@rdf:resource";
        addToTarget("relation", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:coverage/@rdf:resource";
        addToTarget("coverage", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:topic/@rdf:datatype";
        addToTarget("topic", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:label";
        addToTarget("label", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:fieldOfActivity";
        addToTarget("fieldOfActivity", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:geographicAreaCode";
        addToTarget("geographicAreaCode", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:abstract";
        addToTarget("abstract", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:homepage/@rdf:resource";
        addToTarget("homepage", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:valid";
        addToTarget("valid", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:name";
        addToTarget("name", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:contributor/@rdf:resource";
        addToTarget("contributor", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:nick";
        addToTarget("nick", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:hasPart/@rdf:resource";
        addToTarget("hasPart", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:isReplacedBy/@rdf:resource";
        addToTarget("isReplacedBy", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:gndIdentifier";
        addToTarget("gndIdentifier", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:replaces/@rdf:resource";
        addToTarget("replaces", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:identifier/@rdf:resource";
        addToTarget("identifier", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:relatedCorporateBody/@rdf:resource";
        addToTarget("relatedCorporateBody", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:contributingCorporateBody/@rdf:resource";
        addToTarget("contributingCorporateBody", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:contributingPerson/@rdf:resource";
        addToTarget("contributingPerson", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:contributingCorporateBody/@rdf:resource";
        addToTarget("contributingCorporateBody", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:temporal";
        addToTarget("temporal", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:requires/@rdf:resource";
        addToTarget("requires", queryExecute(query), target);

        query = "//rdf:Description[" + i + "]/*:fundedBy/@rdf:resource";
        addToTarget("fundedBy", queryExecute(query), target);

        // new functionality: calculate score for the given target
        calculateScore(elements, target);
      }
    }

  }

  /**
   * Calculate the score for the given elements and the resulting target (an RDF node within the normdata)
   * 
   * @param elements
   *          array of String, each element contains the client's search terms.
   * @param target
   *          the {@link ConceptQueryResult} for the matched RDF node
   */
  private void calculateScore(final String[] elements, final ConceptQueryResult target) {

    if (elements.length == 1) { // one search term, so the score differs
      final ArrayList<String> type = target.getValue(TYPE);
      if (type != null) {
        for (final String typeNode : type) {
          if (typeNode.contains(TYPE_PERSON)) {
            final ArrayList<String> description = target.getValue(DESCRIPTION);
            if (description != null) {
              for (final String descriptionNode : description) {
                if (descriptionNode.contains(DESCRIPTION_HISTORICAL_PERSON)) { // highest priority
                  target.setPriority(ConceptIdentifierPriority.FIRST);
                } else if (descriptionNode.contains(DESCRIPTION_WORK_LEADER)) { // second highest priority
                  target.setPriority(ConceptIdentifierPriority.SECOND);
                }
              }
            }
            final ArrayList<String> functionOrRole = target.getValue(FUNCTION_OR_ROLE);
            if (functionOrRole != null) {
              for (final String functionOrRoleNode : functionOrRole) {
                if (functionOrRoleNode.contains(DESCRIPTION_HISTORICAL_PERSON)) {
                  target.setPriority(ConceptIdentifierPriority.FIRST);
                }
              }
            }
            // no person
          } else if (typeNode.contains(TYPE_PROJECT)) { // third highest priority
            target.setPriority(ConceptIdentifierPriority.THIRD);
          }
        }
      }
    }
  }

  /**
   * Checks an Description of given Search-Strings. returns true if found
   * 
   * @param elements
   * @param temp
   * @param strategie
   * @return
   */
  private Boolean checkIfContains(final String[] elements, final String temp, final int strategie) {

    switch (elements.length) {
    case 0:
      throw new IllegalArgumentException("Not a vaild search string.");
    case 1:
      if (temp.contains(elements[0])) {
        return true;
      }
      break;
    case 2:
      if (strategie == ConceptIdentfierSearchMode.METHODE_AND) {
        if (temp.contains(elements[0]) && temp.contains(elements[1])) {
          return true;
        }

      } else if (temp.contains(elements[0]) || temp.contains(elements[1])) {
        return true;
      }
      break;

    default:
      if (strategie == ConceptIdentfierSearchMode.METHODE_AND) {
        if (temp.contains(elements[0]) && temp.contains(elements[1]) && temp.contains(elements[2])) {
          return true;
        }
      } else if (temp.contains(elements[0]) || temp.contains(elements[1]) || temp.contains(elements[2])) {
        return true;
      }
      break;

    }
    return false;
  }

  /**
   * is used to add given elements to given Querytarget instance, also checks for empty Queryresults
   * 
   * @param key
   * @param value
   * @param target
   */
  private void addToTarget(final String key, final Object value, final ConceptQueryResult target) {
    if (value instanceof String[]) {
      final String[] temp = (String[]) value;
      for (final String s : temp) {
        if (s != null && !s.equals("")) {
          target.addToMap(key, s);
        }
      }
    } else {
      if (value instanceof String) {
        final String temp = (String) value;
        if (value != null && !value.equals("")) {
          target.addToMap(key, temp);
        }
      }
    }
  }

  public ArrayList<ConceptQueryResult> getResultList() {
    return targetList;
  }

  /**
   * Executes the given command in xslt returns the result as String
   * 
   * @param query
   * @return
   */
  private Object queryExecute(final String query) {
    return buildNormdataXPath(query);
  }

  /**
   * Build path to the xml:base attribute. The first matching attribute will be returned.
   * 
   * @return {@link String} the xml:base attribute's value (the uri of the described resource)
   * @throws ApplicationException
   *           if the xml:base isn't tagged
   */
  public String getXmlBaseValue() throws ApplicationException {
    final String erg = (String) buildXPath("//*/@xml:base", false);
    if (erg.equals("")) {
      throw new ApplicationException("No attribute xml:base found. This is required for the authentification of the document!");
    }
    return erg;
  }

}
