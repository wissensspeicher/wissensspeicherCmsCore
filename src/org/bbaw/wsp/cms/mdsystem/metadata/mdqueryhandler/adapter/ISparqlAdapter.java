/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import java.net.URL;


/**
 * Adapter interface which is used to communicate with the {@link SparqlAdapter}
 * .
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * 
 */
public interface ISparqlAdapter {

  /**
   * Build a sparql query from only one literal. Query the default graph. Here,
   * the pf:textmatch method is used.
   * 
   * @param literal
   *          the literal String.
   * @return
   */
  HitGraphContainer buildSparqlQuery(String literal);

  /**
   * Build a sparql query from only one literal. Here, the pf:textmatch method
   * is used.
   * 
   * @param namedGraphUrl
   *          the {@link URL} of the named graph to be queried.
   * @param literal
   *          the literal String.
   */
  void buildSparqlQuery(URL namedGraphUrl, String literal);

  /**
   * "Das ganz normale Pattern" :)
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
   * Verwandte Konzepte finden --> wie? z.B. eine Aussage der Form: Subjekt ist
   * verbunden mit Objekt Dieses Objekt wiederrum steht in Relation mit einem
   * anderen Objekt. [Evtl: das andere Objekt wiederrum ist mit einem weiteren
   * Objekt verbunden]
   * 
   * Dann wird evtl. das Objekt der "Stufe" 2 oder 3 zurückgeliefert.
   */
  String findRelatedConcepts(URL subject);
}
