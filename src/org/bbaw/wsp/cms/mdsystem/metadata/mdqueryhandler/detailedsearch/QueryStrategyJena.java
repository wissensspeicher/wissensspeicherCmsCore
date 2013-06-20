/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

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

  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.IQueryStrategy#delegateNamedGraphQuery(java.net.URL, java.lang.String)
   */
  @Override
  public ResultSet delegateNamedGraphQuery(final URL namedGraphUrl, final String sparqlSelectQuery) {
    /*
     * @TODO Jena irgendwie dazu bringen, dass wirklich nur der gegebene NamedGraph angesprochen wird... bitte jetzt nur die QueryStrategyFuseki nutzen...
     */
    final ResultSet resultSet = rdfhandler.selectSomething(sparqlSelectQuery, dataset).execSelect();
    return resultSet;
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
  public Map<URL, ResultSet> queryGraph(final URL namedGraphUrl) {
    final Map<URL, ResultSet> map = new HashMap<URL, ResultSet>();
    final String query = SparqlCommandBuilder.SELECT_NAMED.getSelectQueryString("*", namedGraphUrl, "?s ?p ?o");
    logger.info("queryGraph: " + query);
    final ResultSet resultSet = delegateNamedGraphQuery(namedGraphUrl, query);
    map.put(namedGraphUrl, resultSet);
    return map;
  }

  @Override
  public Map<URL, ResultSet> queryGraph(final URL namedGraphUrl, final String subject) {
    // TODO Auto-generated method stub
    // not recommended because JENA doesn't work for sparql queries within named graphes...
    return null;
  }

  @Override
  public Map<URL, ResultSet> querySubject(final Resource subject) {
    final String query = SparqlCommandBuilder.SELECT_CONTAINING_NAMED_GRAPH.getSelectQueryString("*", null, subject.getURI() + " ?p ?o");
    logger.info("querySubject: query builded -> " + query);
    return delegateQuery(query);
  }
}
