/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;

/**
 * Strategy which uses an {@link RdfHandler} to query an RdfStore.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 08.02.2013
 * 
 */
public class QueryStrategyHandler implements IQueryStrategy<ResultSet> {

  private final RdfHandler rdfHandler;
  private final Dataset dataset;

  /**
   * Delegate the sparql query to an {@link RdfHandler}.
   * 
   * @param rdfHandler
   *          the {@link RdfHandler}
   */
  public QueryStrategyHandler(final RdfHandler rdfHandler, final Dataset dataset) {
    this.rdfHandler = rdfHandler;
    this.dataset = dataset;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy
   * #delegateQuery(java.lang.String)
   */
  public ResultSet delegateQuery(final String sparqlSelectQuery) {
    final QueryExecution qEx = rdfHandler.selectSomething(sparqlSelectQuery, dataset);
    final ResultSet results = qEx.execSelect();
    return results;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy
   * #queryLiteral(java.lang.String)
   */
  public ResultSet queryLiteral(final String literal) {
    final String query = SparqlCommandBuilder.SELECT_USING_INDEX_AND_NAMED_GRAPH.getSelectQueryString("*", null, literal);
    // query =
    // "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#> SELECT * {?s pf:textMatch '+Humboldt'} LIMIT 10";
    System.out.println("QueryUsingHandler: generated query " + query);
    return delegateQuery(query);
  }

}
