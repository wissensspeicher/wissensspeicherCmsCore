/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.fuseki.FusekiClient;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.ResultSet;

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
    final String query = SparqlCommandBuilder.SELECT_USING_INDEX_AND_NAMED_GRAPH.getSelectQueryString("*", null, literal);
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
  public ResultSet querySubject(URL subject) {
    final String query = SparqlCommandBuilder.SELECT_DEFAULT.getSelectQueryString("*", null, subject.toString());
    return delegateQuery(query);
  }

  @Override
  public ResultSet queryGraph(URL namedGraphUrl) {
    final String query = SparqlCommandBuilder.SELECT_NAMED.getSelectQueryString("*", namedGraphUrl, "?s ?p ?o");
    return delegateQuery(query);
  }

}
