/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Resource;

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
   * @param subject
   *          the literal String.
   */
  HitGraph buildSparqlQuery(URL namedGraphUrl, String subject);

  /**
   * Build a sparql query to receive all data in a specific graph
   * 
   * @param namedGraphUrl
   *          the {@link URL} of the named graph to be queried.
   * @return {@link HitGraph} containing all triples (subject - predicate - object) for the given namedGraphUrl.
   */
  HitGraph buildSparqlQuery(URL namedGraphUrl);

  /**
   * Build a sparql query to recieve all triples for a given subject.
   * 
   * @param subject
   *          a {@link Resource}. A resource in jena is an URI.
   * @return {@link HitGraphContainer}
   */
  HitGraphContainer buildSparqlQuery(Resource subject);

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
