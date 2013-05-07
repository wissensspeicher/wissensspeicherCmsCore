package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * This Class is meant to be the connector between @QueryHandler sent by the GUI and the @RdfHandler. It combines Strings to valid Sparql queries
 * 
 * @author marco juergens
 * @param <T>
 * 
 */
public class SparqlAdapter<T> implements ISparqlAdapter {
  private static final Logger logger = Logger.getLogger(SparqlAdapter.class);
  private static final String PLACEHOLDER_LASTNODE = "%%PLACEHOLDER_LASTNODE%%";
  private final IQueryStrategy<T> queryStrategy;
  /**
   * Index of the last queried node within the findRelatedConcepts().
   */
  private int relatedLastNode;

  /**
   * Create a new SparqlAdapter.
   * 
   * @param <T>
   */
  public SparqlAdapter(final IQueryStrategy<T> queryStrategy) {
    this.queryStrategy = queryStrategy;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery (java.lang.String)
   */
  public HitGraphContainer buildSparqlQuery(final String literal) {
    final T results = queryStrategy.queryLiteral(literal);
    freeQueryStrategy();
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
   * Do some work on the query strategy after it performed a query.
   */
  private void freeQueryStrategy() {
    queryStrategy.free();
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
        this.handleSolution(container, solution, null);
      }
    } else if (results instanceof HashMap<?, ?>) { // query strategy returns hash map in the form named graph url - value
      @SuppressWarnings("unchecked")
      final Map<URL, ResultSet> resultMap = (HashMap<URL, ResultSet>) results;
      for (final URL namedGraph : resultMap.keySet()) {
        final ResultSet realResults = resultMap.get(namedGraph);
        while (realResults.hasNext()) {
          final QuerySolution solution = realResults.next();
          this.handleSolution(container, solution, namedGraph);
        }
      }
    }

    return container;

  }

  /**
   * Handle a single solution (which is from a {@link ResultSet}
   * 
   * @param container
   *          the {@link HitGraphContainer} to be filled with records.
   * @param solution
   *          the {@link QuerySolution}
   * @param namedGraphUrl
   *          some query strategy operate for single named graphes and can pass the named url belonging to the given solution to this method. Otherwise, the named graph will be fetched by the sparql query. Set it to null.
   */
  private void handleSolution(final HitGraphContainer container, final QuerySolution solution, final URL namedGraphUrl) {
    try {
      final URL graphUrl;
      if (namedGraphUrl == null) { // means: sparql query contains the graph name
        graphUrl = new URL(solution.getResource("g").getURI());
      } else { // means: sparql query was executed on a single named graph -> the graph name is passed as argument
        graphUrl = namedGraphUrl;
      }
      final HitGraph hitGraph;
      if (!container.contains(graphUrl)) { // create new hit graph
        hitGraph = new HitGraph(graphUrl);
        container.addHitRecord(graphUrl, hitGraph);
      } else {
        hitGraph = container.getHitGraph(graphUrl);
      }
      String subject = null;
      if (solution.getResource("s") != null) {
        subject = solution.getResource("s").toString();
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
      String subjParent = null;
      if (solution.getResource("sParent") != null) {
        subjParent = solution.getResource("sParent").toString();
      }
      URL predParent = null;
      if (solution.getResource("pParent") != null) {
        predParent = new URL(solution.getResource("pParent").getURI());
      }
      final HitStatement statement = new HitStatement(subject, predicate, literal, score, subjParent, predParent);
      hitGraph.addStatement(statement);

    } catch (final MalformedURLException e) {
      logger.error("SparQlAdapter: not a valid URL (should be one): " + e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery (java.net.URL, java.lang.String)
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
   * @see org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery (java.net.URL, java.net.URL, java.lang.String)
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
   * @see org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.ISparqlAdapter#buildSparqlQuery (java.net.URL, java.net.URL, java.net.URL, java.lang.String)
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

  @Override
  public String findRelatedConcepts(final String node, final int numberOfTriples) {
    final String sparqlQuery = buildRelatedQuery(node, numberOfTriples);
    System.out.println("query: " + sparqlQuery);
    this.handleRelatedSolution(queryStrategy.delegateQuery(sparqlQuery));
    return null;
  }

  // public HitRecordContainer getHitRecordContainer() {
  // // return hitRecordContainer;
  // }

  private void handleRelatedSolution(final T results) {
    if (results instanceof ResultSet) {
      final ResultSet realResults = (ResultSet) results;
      while (realResults.hasNext()) {
        final QuerySolution solution = realResults.next();
        final RDFNode relatedNode = solution.get("o" + this.relatedLastNode);
        final RDFNode relatedPred = solution.get("p" + this.relatedLastNode);
        System.out.println("related node: " + relatedNode.toString());
        System.out.println("related pred: " + relatedPred.toString());
        // try { // try to handle result as literal
        // final Literal relatedLiteral = solution.getLiteral("o" + this.relatedLastNode);
        // if (relatedLiteral != null) {
        // System.out.println(relatedLiteral.getLexicalForm());
        // }
        // } catch (final ClassCastException e) {
        //
        // }
      }
    }
  }

  /**
   * @param node
   * @param numberOfTriples
   * @return
   */
  private String buildRelatedQuery(final String node, final int numberOfTriples) {
    String sparqlQuery = "SELECT DISTINCT " + PLACEHOLDER_LASTNODE + " {\n";
    int prevPredIndex = 0;
    int prevSubjIndex = 0;
    int prevObjIndex = 0;
    for (int i = 0; i < numberOfTriples; i++) { // case a: node is subject
      if (i == 0) {
        sparqlQuery += node + " ?p1 ?o1." + "\n";
        prevPredIndex = 1;
        prevSubjIndex = 1;
        prevObjIndex = 1;
      } else {
        sparqlQuery += "?o" + prevObjIndex + " ?p" + (prevPredIndex + 1) + " ?o" + (prevObjIndex + 1) + ".\n";
        prevPredIndex += 1;
        prevObjIndex += 1;
      }
    }
    sparqlQuery += "}\n";
    sparqlQuery = sparqlQuery.replace(PLACEHOLDER_LASTNODE, "?o" + prevObjIndex + "?p" + prevObjIndex);
    this.relatedLastNode = prevObjIndex;
    return sparqlQuery;
  }

  /**
   * evtl. könnte diese Klasse eine abgewandelte Klasse von @FusekiClient werden
   */

}
