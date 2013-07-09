/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentifier;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.SparqlAdapter;

/**
 * The result type is an enum to differ the result types of both, the
 * <ul>
 * <li>{@link SparqlAdapter}</li>
 * <li>{@link ConceptIdentifier}</li>
 * </ul>
 * 
 * Result types can be
 * <ul>
 * <li>literals</li>
 * <li>named graphes</li>
 * <li>...</li>
 * </ul>
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 09.07.2013
 * 
 */
public enum MdSystemResultType {
  /**
   * @see ISparqlAdapter, result from querying only a literal on the default graph (buildSparqlQuery(literal)).
   */
  LITERAL_DEFAULT_GRAPH,
  /**
   * @see ISparqlAdapter, result from querying only a literal on a named graph (buildSparqlQuery(namedGraphUrl, literal)).
   */
  LITERAL_NAMED_GRAPH,
  /**
   * @see ISparqlAdapter, result from querying a named graph (buildSparqlQuery(namedGraphUrl)).
   */
  ALL_TRIPLES_NAMED_GRAPH,
  /**
   * @see ISparqlAdapter, result from querying the default graph by a given subject (buildSparqlQuery(subject)).
   */
  ALL_TRIPLES_GIVEN_SUBJECT,
  /**
   * @see ISparqlAdapter, result from querying the default graph for related concepts (findRelatedConcepts(node, minNumberTriples, maxNumberTriples).
   */
  RELATED_CONCEPTS;

  @Override
  public String toString() {
    return "" + this;
  }

}
