/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;
import java.util.List;

/**
 * Adapter interface which is used to communicate with the {@link SparqlAdapter} .
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * 
 */
public interface ISparqlAdapter {

  /**
   * Build a sparql query from only one literal. Query the default graph. Here, the pf:textmatch method is used.
   * 
   * @TODO not only index search
   * 
   * @param literal
   *          the literal String.
   * @return
   */
  HitGraphContainer buildSparqlQuery(String literal);

  /**
   * Build a sparql query from only one literal. Here, the pf:textmatch method is used.
   * 
   * @param namedGraphUrl
   *          the {@link URL} of the named graph to be queried.
   * @param literal
   *          the literal String.
   */
  void buildSparqlQuery(URL namedGraphUrl, String literal);
  
  /**
   * Build a sparql query to receive all data in a specific graph 
   * 
   * @param namedGraphUrl
   *          the {@link URL} of the named graph to be queried.
   */
  void buildSparqlQuery(URL namedGraphUrl);
  
  /**
   * "Das ganz normale Pattern" :) mit ?p als String
   * 
   * @param subject
   * @param predicate
   * @param object
   */
  void buildSparqlQuery(URL subject, String predicate, String object);
  
  /**
   * "Das ganz normale Pattern" :) mit ?p als URL
   * 
   * @param subject
   * @param predicate
   * @param object
   */
  void buildSparqlQuery(URL subject, URL predicate, String object);

  /**
   * "Das ganz normale Pattern" :) auf einem Named Graphen
   */
  void buildSparqlQuery(URL namedGraphUrl, URL subject, URL predicate, String object);

  /**
   * Find related concepts. A related concept is a statement that has an object which is related to a given node via a given number of triples.
   * 
   * @param node
   *          the node (as String in the form <URL>) whose related concepts will be found.
   * @param minNumberTriples
   *          the minimum number of triples that are between the node and the related concept. Must be a minimum of 2.
   * @param maxNumberTriples
   *          the maximum number of triples that are between the node and the related concept.
   * @return a list of {@link HitStatement}. The resulting HitStatements are related triples. The subject within the {@link HitStatement} is the related concept.
   * @throws IllegalArgumentException
   *           if the minNumberTriples is less than 2 or one of the parameters is null.
   */
  List<HitStatement> findRelatedConcepts(String node, int minNumberTriples, int maxNumberTriples) throws IllegalArgumentException;
}
