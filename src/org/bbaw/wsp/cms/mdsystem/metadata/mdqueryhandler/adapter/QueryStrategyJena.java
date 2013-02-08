/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import java.net.URL;
import java.util.Map;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ResultSet;

/**
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 08.02.2013
 * 
 */
public class QueryStrategyJena implements IQueryStrategy<Map<URL, ResultSet>> {

  private final RdfHandler rdfhandler;
  private final Dataset dataset;

  /**
   * Delegate the sparql query to a local jena TDB using the {@link RdfHandler}.
   * 
   * @param handler
   *          the {@link RdfHandler}
   * @param dataset
   *          the {@link Dataset} on which the query shall be performed.
   */
  public QueryStrategyJena(final RdfHandler handler, final Dataset dataset) {
    rdfhandler = handler;
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
  public Map<URL, ResultSet> delegateQuery(final String sparqlSelectQuery) {
    final Map<URL, ResultSet> resultMap = rdfhandler.queryAllNamedModels(dataset, sparqlSelectQuery);
    return resultMap;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy
   * #queryLiteral(java.lang.String)
   */
  public Map<URL, ResultSet> queryLiteral(final String literal) {
    final String query = SparqlCommandBuilder.SELECT_USING_INDEX.getSelectQueryString("*", null, literal);
    return delegateQuery(query);
  }
}
