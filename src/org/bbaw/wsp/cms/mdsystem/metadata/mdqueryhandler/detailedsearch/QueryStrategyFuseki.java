/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.fuseki.FusekiClient;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Strategy which uses an {@link FusekiClient} to query an RdfStore.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 08.02.2013
 * 
 */
public class QueryStrategyFuseki implements IQueryStrategy<ResultSet> {
  private final URL fusekiUrl;
  private final FusekiClient fusekiHandler;
  private static Logger logger = Logger.getLogger(QueryStrategyFuseki.class);

  /**
   * Delegate the sparql query to a fuseki server.
   * 
   * @param fusekiDatasetUrl
   *          the {@link URL} to the fuseki dataset (e.g. http://domain/[yourdataset]
   */
  public QueryStrategyFuseki(final URL fusekiDatasetUrl) {
    fusekiUrl = fusekiDatasetUrl;
    fusekiHandler = new FusekiClient();
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy #delegateQuery(java.lang.String)
   */
  public ResultSet delegateQuery(final String sparqlSelectQuery) {
    final ResultSet results = fusekiHandler.performSelect(fusekiUrl.toExternalForm(), sparqlSelectQuery);
    return results;
  }

  /**
   * 
   * @return the {@link URL} to the fuseki dataset (e.g.: http://domain/dataset)
   */
  public URL getFusekiUrl() {
    return fusekiUrl;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy #queryLiteral(java.lang.String)
   */
  public ResultSet queryLiteral(final String literal) {
    final String query = SparqlCommandBuilder.SELECT_USING_INDEX_CONTAINING_NAMED_GRAPH.getSelectQueryString("*", null, literal, null, null);
    return delegateQuery(query);
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy#free()
   */
  public void free() {
    // TODO Auto-generated method stub

  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.IQueryStrategy#querySubject(java.net.URL)
   */
  public ResultSet querySubject(final Resource subject) {
    final String query = SparqlCommandBuilder.SELECT_CONTAINING_NAMED_GRAPH.getSelectQueryString("*", null, subject.getURI() + " ?p ?o", null, null);
    logger.info("querySubject: query builded -> " + query);
    return delegateQuery(query);
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.IQueryStrategy#queryGraph(java.net.URL)
   */
  public ResultSet queryGraph(final URL namedGraphUrl) {
    final String query = SparqlCommandBuilder.SELECT_NAMED.getSelectQueryString("*", namedGraphUrl, "?s ?p ?o", null, null);
    return delegateQuery(query);
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.IQueryStrategy#delegateNamedGraphQuery(java.net.URL, java.lang.String)
   */
  public ResultSet delegateNamedGraphQuery(final URL namedGraphUrl, final String sparqlSelectQuery) {
    /*
     * this method is not neccessary, because fuseki is intelligent enough to handle a "SELECT ... FROM NAMED <graphUri>" query ;)
     */
    return null;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.IQueryStrategy#queryGraph(java.net.URL, java.lang.String)
   */
  public ResultSet queryGraph(final URL namedGraphUrl, final String subject) {
    final String query = SparqlCommandBuilder.SELECT_NAMED.getSelectQueryString("*", namedGraphUrl, subject + " ?p ?o", null, null);
    logger.info("queryGraph_namedGraphUrl_subject: query builded -> " + query);
    return delegateQuery(query);
  }

//  @Override
//  public ResultSet queryAllProjectInfo(String projectId, boolean isProjectId) {
//    final String query = SparqlCommandBuilder.SELECT_ALL_PROJECT_INFO_FOR_GIVEN_ID.getSelectQueryString("?s ?p ?o", null, null, projectId, null);
//    return delegateQuery(query);
//  }
  
  @Override
  public ResultSet queryAllProjectInfoAndResolveUris(String projectUri, boolean isProjectId) {
    final String query = SparqlCommandBuilder.SELECT_ALL_PROJECT_INFO_AND_RESOlVE_FOR_GIVEN_ID.getSelectQueryString(null, null, null, null, projectUri);
    return delegateQuery(query);
  }
}
