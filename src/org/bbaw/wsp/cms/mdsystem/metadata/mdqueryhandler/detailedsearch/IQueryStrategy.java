/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

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
  * @param literal
  *          the literal as String
  * @return the generic type as specified in the concrete strategy class. Mostly it's a {@link ResultSet}.
  */
  T queryGraphOfLiteral(String variables, String literal, String type);

  /**
   * Delegate a sparqlQuery to a useful handler instance and execute it. Use the {@link SparqlCommandBuilder} to build a valid sparql statement.
   * 
   * @param subject
   *          the subject as URL
   * @return the generic type as specified in the concrete strategy class. Mostly it's a {@link ResultSet}.
   */
  T querySubject(Resource subject);
  
//  /**
//   * Delegate a sparqlQuery to a useful handler instance and execute it. Use the {@link SparqlCommandBuilder} to build a valid sparql statement.
//   * 
//   * @param id
//   *          the projectId as String
//   * @return the generic type as specified in the concrete strategy class. Mostly it's a {@link ResultSet}.
//   */
//  T queryAllProjectInfo(String projectId, boolean isProjectId);

  /**
   * Delegate a sparqlQuery to a useful handler instance and execute it. Use the {@link SparqlCommandBuilder} to build a valid sparql statement.
   * 
   * @param id
   *          the projectId as String
   * @return the generic type as specified in the concrete strategy class. Mostly it's a {@link ResultSet}.
   */
  T queryAllProjectInfoAndResolveUris(String projectUri, boolean isProjectId);
  
  /**
   * Delegate a sparqlQuery to a useful handler instance and execute it. Use the {@link SparqlCommandBuilder} to build a valid sparql statement.
   * 
   * @param id
   *          the projectId as String
   * @return the generic type as specified in the concrete strategy class. Mostly it's a {@link ResultSet}.
   */
  T preloadAllProjectInfoAndResolveUris(String collectionIds);
  
  T preloadAnything(String query);
  
  /**
   * Query a single named graph.
   * 
   * @param namedGraphUrl
   * @return a container of generic type which contains each triple for a given quad.
   */
  T queryGraph(URL namedGraphUrl);

  /**
   * Query a single named graph for a given subject.
   * 
   * @param namedGraphUrl
   * @param subject
   * @return a container of generic type which contains each triple for the given named graph and subject.
   */
  T queryGraph(URL namedGraphUrl, String subject);

  /**
   * Delegate a sparQl query to a useful handler instance. Ensure, that the only the given namedGraph is queried.
   * 
   * @param namedGraphUrl
   *          URL the namedGraph to be queried.
   * @param sparqlSelectQuery
   *          String the select statement for the given namedGraph.
   * @return {@link ResultSet}
   */
  ResultSet delegateNamedGraphQuery(final URL namedGraphUrl, final String sparqlSelectQuery);

  /**
   * Free the subsystem of the concrete QueryStrategy (e.g. close an index...). The SparqlAdapter will call this method after a query to ensure, that the {@link IQueryStrategy} does its work to return to a fresh state.
   */
  void free();

}
