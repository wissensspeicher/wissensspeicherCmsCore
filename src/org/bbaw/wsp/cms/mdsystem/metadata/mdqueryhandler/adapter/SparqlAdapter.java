package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.fuseki.FusekiClient;
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
 * @param <T>
 * 
 */
public class SparqlAdapter<T> implements ISparqlAdapter {
  private final IQueryStrategy<T> queryStrategy;
  private final HitGraphContainer hitRecordContainer;

  /**
   * Create a new SparqlAdapter.
   * 
   * @param <T>
   */
  public SparqlAdapter(final IQueryStrategy<T> queryStrategy) {
    this.queryStrategy = queryStrategy;
    hitRecordContainer = new HitGraphContainer();
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.lang.String)
   */
  public HitGraphContainer buildSparqlQuery(final String literal) {
    final T results = queryStrategy.queryLiteral(literal);

    return this.handleLiteralResults(results);

    // final Query q = Que ryFactory.create(query);
    // System.out.println("Builded query " + query);
    // final QueryExecution qExec = QueryExecutionFactory.create(q,
    // handler.getUnionModel(dataset));
    // qExec.getContext().set(TDB.symUnionDefaultGraph, true);
    // // qExec.execSelect().nextSolution().
    // ResultSetFormatter.out(System.out, qExec.execSelect(), q);
  }

  /**
   * Handle results of an {@link IQueryStrategy} depending on the return type.
   * 
   * @param results
   *          a generic type.
   */
  private HitGraphContainer handleLiteralResults(final T results) {
    final HitGraphContainer container = new HitGraphContainer(new Date()); // result
                                                                           // container
    if (results instanceof ResultSet) {
      final ResultSet realResults = (ResultSet) results;
      while (realResults.hasNext()) {
        final QuerySolution solution = realResults.next();
        try {
          final URL graphUrl = new URL(solution.getResource("g").getURI());
          final HitGraph hitGraph;
          if (!container.contains(graphUrl)) {
            hitGraph = new HitGraph(graphUrl);
            container.addHitRecord(graphUrl, hitGraph);
          } else {
            hitGraph = container.getHitGraph(graphUrl);
          }
          URL subject = null;
          if (solution.getResource("s") != null) {
            subject = new URL(solution.getResource("s").getURI());
          }
          URL predicate = null;
          if (solution.getResource("p") != null) {
            predicate = new URL(solution.getResource("p").getURI());
          }
          String literal = null;
          if (solution.getLiteral("lit") != null) {
            literal = solution.getLiteral("lit").toString();
          }
          double score = 0;
          if (solution.getLiteral("score") != null) {
            score = solution.getLiteral("score").getDouble();
          }
          final HitStatement statement = new HitStatement(subject, predicate, literal, score);
          hitGraph.addStatement(statement);

        } catch (final MalformedURLException e) {
          System.err.println("SparQlAdapter: not a valid URL (should be one): " + e.getMessage());
        }
        // System.out.println("o: " + solution.getLiteral("o"));
      }
    } else if (results instanceof HashMap<?, ?>) {

    }

    return container;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.net.URL, java.lang.String)
   */
  // public void buildSparqlQuery(final URL namedGraphUrl, final String literal)
  // {
  // final String query =
  // SparqlCommandBuilder.SELECT_NAMED_USING_INDEX.getSelectQueryString("*",
  // namedGraphUrl.toExternalForm(), literal);
  // System.out.println("Builded query " + query);
  // final ResultSet results = delegateQuery(query);
  // final StmtIterator it = results.getResourceModel().listStatements();
  //
  // // while (it.hasNext()) {
  // // final Statement statement = it.next();
  // // System.out.println(statement);
  // // }
  //
  // while (results.hasNext()) { // hier kommt man nicht weiter, wie an die
  // // Tripels ran???
  // final QuerySolution solution = results.next();
  // // System.out.println(solution);
  // // System.out.println("s: " + solution.getResource("s"));
  // System.out.println("o: " + solution.getLiteral("s"));
  // }
  // }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery
   * (java.net.URL, java.net.URL, java.lang.String)
   */
  public void buildSparqlQuery(final URL subject, final URL predicate, final String object) {
    // final String query =
    // SparqlCommandBuilder.SELECT_DEFAULT.getSelectQueryString("*", null, "<" +
    // subject.toExternalForm() + "> <" + predicate.toExternalForm() + "> " +
    // object);
    // System.out.println("Builded query " + query);
    // final QueryExecution ex = delegateQuery(query);
    // handler.selectSomething(query, dataset);
    // for (final String bla : ex.execSelect().getResultVars()) {
    // System.out.println(bla);
    // }
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
    // final String query =
    // SparqlCommandBuilder.SELECT_NAMED.getSelectQueryString("*",
    // namedGraphUrl.toExternalForm(), "<" + subject.toExternalForm() + "> <" +
    // predicate.toExternalForm() + "> " + object);
    // System.out.println("Builded query " + query);
    // final QueryExecution ex = delegateQuery(query);
    // handler.selectSomething(query, dataset);
    // for (final String bla : ex.execSelect().getResultVars()) {
    // System.out.println(bla);
    // }
    //
    // final Query q = QueryFactory.create(query);
    // System.out.println("Builded query " + query);
    // final QueryExecution qExec = QueryExecutionFactory.create(q, dataset);
    // // for (String bla : ex.execSelect().getResultVars()) {
    // //
    // // // System.out.println(bla);
    // // }
    // ResultSetFormatter.out(System.out, qExec.execSelect(), q);
  }

  @Override
  public void buildSparqlQuery(final URL namedGraphUrl, final String literal) {
    // TODO Auto-generated method stub

  }

  // public HitRecordContainer getHitRecordContainer() {
  // // return hitRecordContainer;
  // }

  /**
   * evtl. könnte diese Klasse eine abgewandelte Klasse von @FusekiClient
   * werden
   */

}
