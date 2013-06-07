/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
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
  private static Logger logger = Logger.getLogger(QueryStrategyJena.class);
  private final RdfHandler rdfhandler;
  private final Dataset dataset;
  private final JenaMain jenamain;

  /**
   * Delegate the sparql query to a local jena TDB using the {@link RdfHandler}.
   * 
   * @param jenamain
   *          the {@link JenaMain}
   * @throws IllegalArgumentException
   *           if the parameter is null
   */
  public QueryStrategyJena(final JenaMain jenamain) {
    if (jenamain == null) {
      throw new IllegalArgumentException("The parameter for jenamain in QueryStrategyJena mustn't be null");
    }
    rdfhandler = jenamain.getManager();
    dataset = jenamain.getDataset();
    this.jenamain = jenamain;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy #delegateQuery(java.lang.String)
   */
  public Map<URL, ResultSet> delegateQuery(final String sparqlSelectQuery) {
    final Map<URL, ResultSet> resultMap = rdfhandler.queryAllNamedModels(dataset, sparqlSelectQuery);
    return resultMap;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy #queryLiteral(java.lang.String)
   */
  public Map<URL, ResultSet> queryLiteral(final String literal) {
    final String query = SparqlCommandBuilder.SELECT_USING_INDEX.getSelectQueryString("*", null, literal);
    logger.info("QueryStrategyJena: builded sparql query: " + query);
    return delegateQuery(query);
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy#free()
   */
  public void free() {
    jenamain.getIndexStore().closeIndex(); // close larq index
  }

  @Override
  public Map<URL, ResultSet> querySubject(URL subject) {
    // TODO Auto-generated method stub
    return null;
  }
}
