/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.ResultSet;

/**
 * An IQueryStrategy encapsulates methods how the {@link SparqlAdapter} queries a dataset
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
   * @return the generic type as specified in the concrete strategy class. Mostly it's a {@link ResultSet}.
   */
  T delegateQuery(String sparqlSelectQuery);

  /**
   * Delegate a sparqlQuery to a useful handler instance and execute it. Use the {@link SparqlCommandBuilder} to build a valid sparql statement.
   * 
   * @param literal
   *          the literal as String
   * @return the generic type as specified in the concrete strategy class. Mostly it's a {@link ResultSet}.
   */
  T queryLiteral(String literal);

  /**
   * Delegate a sparqlQuery to a useful handler instance and execute it. Use the {@link SparqlCommandBuilder} to build a valid sparql statement.
   * 
   * @param subject
   *          the subject as URL
   * @return the generic type as specified in the concrete strategy class. Mostly it's a {@link ResultSet}.
   */
  T querySubject(URL subject);
  
  /**
   * Free the subsystem of the concrete QueryStrategy (e.g. close an index...). The SparqlAdapter will call this method after a query to ensure, that the {@link IQueryStrategy} does its work to return to a fresh state.
   */
  void free();
}
