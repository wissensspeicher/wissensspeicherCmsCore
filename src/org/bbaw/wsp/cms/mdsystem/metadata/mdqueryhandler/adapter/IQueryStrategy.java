/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.ResultSet;

/**
 * An IQueryStrategy encapsulates methods how the {@link SparqlAdapter} queries
 * a dataset
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 08.02.2013
 * 
 */
public interface IQueryStrategy<T> {

  /**
   * Delegate a sparqlQuery to a useful handler instance and execute it.
   * 
   * @param sparqlSelectQuery
   *          a valid SparQl Statement.
   * @return the generic type as specified in the concrete strategy class.
   *         Mostly it's a {@link ResultSet}.
   */
  T delegateQuery(String sparqlSelectQuery);

  /**
   * Delegate a sparqlQuery to a useful handler instance and execute it. Use the
   * {@link SparqlCommandBuilder} to build a valid sparql statement.
   * 
   * @param literal
   *          the literal as String
   * @return the generic type as specified in the concrete strategy class.
   *         Mostly it's a {@link ResultSet}.
   */
  T queryLiteral(String literal);
}
