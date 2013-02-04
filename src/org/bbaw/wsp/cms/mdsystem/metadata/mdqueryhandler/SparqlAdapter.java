package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.URL;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;

/**
 * This Class is meant to be the connector between @QueryHandler sent by the GUI
 * and the @RdfHandler. It combines Strings to valid Sparql queries
 * 
 * @author marco juergens
 * 
 */
public class SparqlAdapter implements ISparqlAdapter {
  private RdfHandler handler;
  private Dataset dataset;

  /**
   * Create a new SparqlAdapter.
   */
  public SparqlAdapter(Dataset dataset) {
    this.handler = new RdfHandler();
    this.dataset = dataset;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.lang.String)
   */
  public void buildSparqlQuery(String literal) {
    String query = SparqlCommandBuilder.SELECT_USING_INDEX.getSelectQueryString("*", null, literal);
    System.out.println("Builded query " + query);
    QueryExecution ex = handler.selectSomething(query, this.dataset);
    for (String bla : ex.execSelect().getResultVars()) {
      System.out.println(bla);
    }
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.net.URL, java.lang.String)
   */
  public void buildSparqlQuery(URL namedGraphUrl, String literal) {
    String query = SparqlCommandBuilder.SELECT_NAMED.getSelectQueryString("*", namedGraphUrl.toExternalForm(), "?s ?p " + literal);
    handler.selectSomething(query, dataset);
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.net.URL, java.net.URL, java.lang.String)
   */
  public void buildSparqlQuery(URL subject, URL predicate, String object) {
    String query = SparqlCommandBuilder.SELECT_DEFAULT.getSelectQueryString("*", null, "<" + subject.toExternalForm() + "> <" + predicate.toExternalForm() + "> " + object);
    handler.selectSomething(query, dataset);
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#
   * findRelativeConcepts(java.net.URL)
   */
  public String findRelativeConcepts(URL subject) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * evtl. könnte diese Klasse eine abgewandelte Klasse von @FusekiClient
   * werden
   */

}
