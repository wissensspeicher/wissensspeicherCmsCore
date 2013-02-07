package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.URL;
import java.util.Map;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.HitRecordContainer;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools.SparqlCommandBuilder;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDB;

/**
 * This Class is meant to be the connector between @QueryHandler sent by the GUI
 * and the @RdfHandler. It combines Strings to valid Sparql queries
 * 
 * @author marco juergens
 * 
 */
public class SparqlAdapter implements ISparqlAdapter {
  private final RdfHandler handler;
  private final Dataset dataset;
  private HitRecordContainer hitRecordContainer;

  /**
   * Create a new SparqlAdapter.
   */
  public SparqlAdapter(final Dataset dataset) {
    handler = new RdfHandler();
    this.dataset = dataset;
  }

  /**
   * Delegate a single query.
   * 
   * @param query
   * @return
   */
  private ResultSet delegateQuery(final String query) {
    final QueryExecution ex = handler.selectSomething(query, dataset);
    ex.getContext().set(TDB.symUnionDefaultGraph, true);
    final ResultSet results = ex.execSelect();
    return results;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.lang.String)
   */
  public void buildSparqlQuery(final String literal) {
    // dataset.begin(ReadWrite.READ);

    final String query = SparqlCommandBuilder.SELECT_USING_INDEX.getSelectQueryString("*", null, literal);
    System.out.println("Builded query " + query);
    final Map<URL, ResultSet> resultMap = handler.queryAllNamedModels(dataset, query);
    for (final URL graphUrl : resultMap.keySet()) {
      System.out.println("Result for " + graphUrl);
      final ResultSet results = resultMap.get(graphUrl);

      while (results.hasNext()) {
        final QuerySolution solution = results.next();
        System.out.println("o: " + solution.getLiteral("o"));
      }
    }

    // final Query q = Que ryFactory.create(query);
    // System.out.println("Builded query " + query);
    // final QueryExecution qExec = QueryExecutionFactory.create(q,
    // handler.getUnionModel(dataset));
    // qExec.getContext().set(TDB.symUnionDefaultGraph, true);
    // // qExec.execSelect().nextSolution().
    // ResultSetFormatter.out(System.out, qExec.execSelect(), q);
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.net.URL, java.lang.String)
   */
  public void buildSparqlQuery(final URL namedGraphUrl, final String literal) {
    final String query = SparqlCommandBuilder.SELECT_NAMED_USING_INDEX.getSelectQueryString("*", namedGraphUrl.toExternalForm(), literal);
    System.out.println("Builded query " + query);
    final ResultSet results = delegateQuery(query);
    final StmtIterator it = results.getResourceModel().listStatements();

    // while (it.hasNext()) {
    // final Statement statement = it.next();
    // System.out.println(statement);
    // }

    while (results.hasNext()) { // hier kommt man nicht weiter, wie an die
      // Tripels ran???
      final QuerySolution solution = results.next();
      // System.out.println(solution);
      // System.out.println("s: " + solution.getResource("s"));
      System.out.println("o: " + solution.getLiteral("s"));
    }
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.net.URL, java.net.URL, java.lang.String)
   */
  public void buildSparqlQuery(final URL subject, final URL predicate, final String object) {
    final String query = SparqlCommandBuilder.SELECT_DEFAULT.getSelectQueryString("*", null, "<" + subject.toExternalForm() + "> <" + predicate.toExternalForm() + "> " + object);
    System.out.println("Builded query " + query);
    final QueryExecution ex = delegateQuery(query);
    handler.selectSomething(query, dataset);
    for (final String bla : ex.execSelect().getResultVars()) {
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
  public String findRelatedConcepts(final URL subject) {
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
  public void buildSparqlQuery(final URL namedGraphUrl, final URL subject, final URL predicate, final String object) {
    final String query = SparqlCommandBuilder.SELECT_NAMED.getSelectQueryString("*", namedGraphUrl.toExternalForm(), "<" + subject.toExternalForm() + "> <" + predicate.toExternalForm() + "> " + object);
    System.out.println("Builded query " + query);
    final QueryExecution ex = delegateQuery(query);
    handler.selectSomething(query, dataset);
    for (final String bla : ex.execSelect().getResultVars()) {
      System.out.println(bla);
    }

    final Query q = QueryFactory.create(query);
    System.out.println("Builded query " + query);
    final QueryExecution qExec = QueryExecutionFactory.create(q, dataset);
    // for (String bla : ex.execSelect().getResultVars()) {
    //
    // // System.out.println(bla);
    // }
    ResultSetFormatter.out(System.out, qExec.execSelect(), q);
  }

  public HitRecordContainer getHitRecordContainer() {
    return hitRecordContainer;
  }

  /**
   * evtl. könnte diese Klasse eine abgewandelte Klasse von @FusekiClient
   * werden
   */

}
