package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.net.URL;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.tdb.TDB;

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

  private QueryExecution getQueryExe(String query) {
    QueryExecution ex = handler.selectSomething(query, this.dataset);
    ex.getContext().set(TDB.symUnionDefaultGraph, true);
    return ex;
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
    // dataset.begin(ReadWrite.READ);
    String query = SparqlCommandBuilder.SELECT_USING_INDEX.getSelectQueryString("*", null, literal);
    // System.out.println("Builded query " + query);
    QueryExecution ex = this.getQueryExe(query);
    // ex.execSelect().nextSolution().
    for (String bla : ex.execSelect().getResultVars()) {
      System.out.println(bla);
    }

    // Query q = QueryFactory.create(query);
    // System.out.println("Builded query " + query);
    // final QueryExecution qExec = QueryExecutionFactory.create(q,
    // dataset.getDefaultModel());
    // qExec.getContext().set(TDB.symUnionDefaultGraph, true);
    // qExec.execSelect().nextSolution().
    // // ResultSetFormatter.out(System.out, qExec.execSelect(), q);
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
    String query = SparqlCommandBuilder.SELECT_NAMED_USING_INDEX.getSelectQueryString("*", namedGraphUrl.toExternalForm(), literal);
    System.out.println("Builded query " + query);
    QueryExecution ex = this.getQueryExe(query);
    for (String bla : ex.execSelect().getResultVars()) {

      // System.out.println(bla);
    }

    Query q = QueryFactory.create(query);
    System.out.println("Builded query " + query);
    final QueryExecution qExec = QueryExecutionFactory.create(q, dataset);
    // for (String bla : ex.execSelect().getResultVars()) {
    //
    // // System.out.println(bla);
    // }
    ResultSetFormatter.out(System.out, qExec.execSelect(), q);
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
    System.out.println("Builded query " + query);
    QueryExecution ex = this.getQueryExe(query);
    handler.selectSomething(query, dataset);
    for (String bla : ex.execSelect().getResultVars()) {
      System.out.println(bla);
    }
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

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.net.URL, java.net.URL, java.net.URL, java.lang.String)
   */
  public void buildSparqlQuery(URL namedGraphUrl, URL subject, URL predicate, String object) {
    String query = SparqlCommandBuilder.SELECT_NAMED.getSelectQueryString("*", namedGraphUrl.toExternalForm(), "<" + subject.toExternalForm() + "> <" + predicate.toExternalForm() + "> " + object);
    System.out.println("Builded query " + query);
    QueryExecution ex = this.getQueryExe(query);
    handler.selectSomething(query, dataset);
    for (String bla : ex.execSelect().getResultVars()) {
      System.out.println(bla);
    }

    Query q = QueryFactory.create(query);
    System.out.println("Builded query " + query);
    final QueryExecution qExec = QueryExecutionFactory.create(q, dataset);
    // for (String bla : ex.execSelect().getResultVars()) {
    //
    // // System.out.println(bla);
    // }
    ResultSetFormatter.out(System.out, qExec.execSelect(), q);
  }

  /**
   * evtl. könnte diese Klasse eine abgewandelte Klasse von @FusekiClient
   * werden
   */

}
