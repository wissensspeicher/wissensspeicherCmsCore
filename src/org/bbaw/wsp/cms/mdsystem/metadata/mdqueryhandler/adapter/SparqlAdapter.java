package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.RelatedHitStatement.Relationship;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
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
  private int relatedMaxNode;
  private int relatedMinNode;

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
      RDFNode subject = null;
      if (solution.getResource("s") != null) {
        subject = solution.getResource("s");
      }
      RDFNode predicate = null;
      if (solution.getResource("p") != null) {
        predicate = solution.getResource("p");
      }
      RDFNode literal = null;
      if (solution.getLiteral("lit") != null) {
        literal = solution.getLiteral("lit");
      }
      double score = 0;
      if (solution.getLiteral("score") != null) {
        score = solution.getLiteral("score").getDouble();
      }
      RDFNode subjParent = null;
      if (solution.getResource("sParent") != null) {
        subjParent = solution.getResource("sParent");
      }
      RDFNode predParent = null;
      if (solution.getResource("pParent") != null) {
        predParent = solution.getResource("pParent");
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

  // public HitRecordContainer getHitRecordContainer() {
  // // return hitRecordContainer;
  // }

  private List<HitStatement> handleRelatedSolution(final T results) {
    final List<HitStatement> resultStatements = new ArrayList<HitStatement>();
    if (results instanceof ResultSet) { // returned by QueryStrategyFuseki
      final ResultSet realResults = (ResultSet) results;
      while (realResults.hasNext()) {
        final QuerySolution solution = realResults.next();
        for (int i = this.relatedMinNode; i <= this.relatedMaxNode; i++) {
          // ..:: top-down query (node is subject) ::..
          final RDFNode topDownRelatedSubj = solution.get("o" + (i - 1)); // subject is object in the parent statement
          final RDFNode topwDownRelatedPred = solution.get("p" + i);
          final RDFNode topDownRelatedNode = solution.get("o" + i);

          // the subject (object of the parent statement) doesn't need to be part of another statement
          if (topDownRelatedSubj != null && topDownRelatedSubj.isResource() && topwDownRelatedPred != null && topDownRelatedNode != null) {
            final RelatedHitStatement relatedStatement = new RelatedHitStatement(topDownRelatedSubj, topwDownRelatedPred, topDownRelatedNode, 0, null, null, i, Relationship.CHILD);
            if (!resultStatements.contains(relatedStatement)) {
              resultStatements.add(relatedStatement);
            }
          }

          // ..:: bottom-up query (node is object) ::..
          final RDFNode bottomRelatedSubj = solution.get("s1" + i);
          final RDFNode bottomDownRelatedPred = solution.get("p1" + i);
          final RDFNode bottomRelatedNode = solution.get("s1" + (i - 1)); // object is subject in the child statement

          if (bottomRelatedNode != null && bottomRelatedNode.isResource() && bottomDownRelatedPred != null && bottomRelatedSubj != null) {
            final RelatedHitStatement bottomRelatedStatement = new RelatedHitStatement(bottomRelatedSubj, bottomDownRelatedPred, bottomRelatedNode, 0, null, null, i, Relationship.PARENT);
            if (!resultStatements.contains(bottomRelatedStatement)) {
              resultStatements.add(bottomRelatedStatement);
            }
          }
        }
      }
    }

    return resultStatements;
  }

  /**
   * 
   * 
   * @param node
   * @param numberOfTriples
   * @param maxNumberTriples
   * @return
   */
  private String buildRelatedQuery(final String node, final int minNumberTriples, final int maxNumberTriples) {
    final StringBuilder sparqlQueryBuilder = new StringBuilder("SELECT DISTINCT " + PLACEHOLDER_LASTNODE + " {\n");
    int prevPredIndex = 0;
    int prevObjIndex = 0;
    // graph patterns
    String selection = ""; // concatenaed string for the selection part of the sparql command

    /*
     * ..:: required graph patterns ::..
     */
    sparqlQueryBuilder.append("\n\t{"); // left side of UNION
    for (int i = 0; i < minNumberTriples; i++) {
      if (i == 0) { // first pattern
        sparqlQueryBuilder.append("\n\t\t" + node + " ?p1 ?o1." + "\n");
        prevPredIndex = 1;
        prevObjIndex = 1;
      } else {
        if (i == minNumberTriples - 1) {
          selection += "?o" + (minNumberTriples - 1) + " ?p" + (prevPredIndex + 1) + " ?o" + minNumberTriples;
        }
        sparqlQueryBuilder.append("\n\t\t" + "?o" + prevObjIndex + " ?p" + (prevPredIndex + 1) + " ?o" + (prevObjIndex + 1) + ".\n");

        prevPredIndex += 1;
        prevObjIndex += 1;
      }
    }

    /*
     * ..::::::::::::::::::::::::::::..
     */

    /*
     * ..:: optional graph patterns ::..
     */
    for (int j = minNumberTriples; j < maxNumberTriples; j++) {
      sparqlQueryBuilder.append("\n\tOPTIONAL {\n ");
      selection += " ?p" + (prevPredIndex + 1) + " ?o" + (prevObjIndex + 1);
      // apply pattern
      sparqlQueryBuilder.append("\n\t\t" + "?o" + prevObjIndex + " ?p" + (prevPredIndex + 1) + " ?o" + (prevObjIndex + 1) + ".\n");
      prevPredIndex += 1;
      prevObjIndex += 1;

      if (j == maxNumberTriples - 1) {
        for (int ij = maxNumberTriples; ij > minNumberTriples; ij--) {
          /*
           * ..:: additional filters ::..
           */
          String filter = "";
          for (int k = ij - 1; k > 0; k--) {
            filter += "?o" + (k) + " != " + "?o" + ij;
            if (k > 1) {
              filter += " && ";
            }
          }

          if (!filter.equals("")) {
            sparqlQueryBuilder.append("\n\t\tFILTER ( ");
            sparqlQueryBuilder.append(filter);
            sparqlQueryBuilder.append(") ");
          }

          // sparqlQuery += "?o" + (ij + 1) + " ?x" + (ij + 1) + " ?o" + prevObjIndex + "";

          /*
           * ..::::::::::::::::::::::::..
           */
          sparqlQueryBuilder.append("}\n");
        }
      }
    }
    sparqlQueryBuilder.append("} \n"); // end of left side of UNION

    sparqlQueryBuilder.append("\tUNION \n");
    /*
     * ..:: right side of UNION ::..
     */
    sparqlQueryBuilder.append("{ \n");

    for (int i = 0; i < minNumberTriples; i++) {
      if (i == 0) { // first pattern
        sparqlQueryBuilder.append("\n\t\t" + "?s11 ?p11 " + node + ".\n");
        prevPredIndex = 1;
        prevObjIndex = 1;
      } else {
        if (i == minNumberTriples - 1) {
          selection += " ?p1" + (prevPredIndex + 1) + " ?s1" + minNumberTriples + " ?s1" + (minNumberTriples - 1);
        }
        sparqlQueryBuilder.append("\n\t\t" + "?s1" + (prevObjIndex + 1) + " ?p1" + (prevPredIndex + 1) + " ?s1" + (prevObjIndex) + ".\n");

        prevPredIndex += 1;
        prevObjIndex += 1;
      }
    }

    /*
     * ..::::::::::::::::::::::::::::..
     */

    /*
     * ..:: optional graph patterns ::..
     */
    for (int j = minNumberTriples; j < maxNumberTriples; j++) {
      sparqlQueryBuilder.append("\n\tOPTIONAL {\n ");
      selection += " ?p1" + (prevPredIndex + 1) + " ?s1" + (prevObjIndex + 1);
      // apply pattern
      sparqlQueryBuilder.append("\n\t\t" + "?s1" + (prevObjIndex + 1) + " ?p1" + (prevPredIndex + 1) + " ?s1" + prevObjIndex + ".\n");
      prevPredIndex += 1;
      prevObjIndex += 1;

      if (j == maxNumberTriples - 1) {
        for (int ij = maxNumberTriples; ij > minNumberTriples; ij--) {
          /*
           * ..:: additional filters ::..
           */
          String filter = "";
          for (int k = ij - 1; k > 0; k--) {
            filter += "?s1" + (k) + " != " + "?s1" + ij;
            if (k > 1) {
              filter += " && ";
            }
          }

          if (!filter.equals("")) {
            sparqlQueryBuilder.append("\n\t\tFILTER ( ");
            sparqlQueryBuilder.append(filter);
            sparqlQueryBuilder.append(") ");
          }

          // sparqlQuery += "?o" + (ij + 1) + " ?x" + (ij + 1) + " ?o" + prevObjIndex + "";

          /*
           * ..::::::::::::::::::::::::..
           */
          sparqlQueryBuilder.append("\n\t}\n");
        }
      }
    }
    sparqlQueryBuilder.append("} \n"); // end of right side of UNION

    sparqlQueryBuilder.append("}\n");
    String sparqlQuery = sparqlQueryBuilder.toString();
    sparqlQuery = sparqlQuery.replace(PLACEHOLDER_LASTNODE, selection);
    this.relatedMaxNode = prevObjIndex;
    relatedMinNode = minNumberTriples;
    return sparqlQuery;
  }

  /**
   * evtl. könnte diese Klasse eine abgewandelte Klasse von @FusekiClient werden
   */

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.ISparqlAdapter#findRelatedConcepts(java.lang.String, int, int)
   */
  public List<HitStatement> findRelatedConcepts(final String node, final int minNumberTriples, final int maxNumberTriples) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("FindRelatedConcept: value for argument node mustn't be null.");
    }
    if (minNumberTriples < 2) {
      throw new IllegalArgumentException("FindRelatedConcept: value for argument minNumberTriples mustn't be less than 2.");
    }
    final String sparqlQuery = buildRelatedQuery(node, minNumberTriples, maxNumberTriples);
    final List<HitStatement> statements = this.handleRelatedSolution(queryStrategy.delegateQuery(sparqlQuery));
    return statements;
  }

}
