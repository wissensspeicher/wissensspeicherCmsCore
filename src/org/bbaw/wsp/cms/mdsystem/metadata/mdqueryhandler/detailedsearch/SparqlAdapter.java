package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.collections.Collection;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.MdSystemResultType;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.RelatedHitStatement.Relationship;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

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
        this.handleSolution(container, solution, null, null);
      }
    } else if (results instanceof HashMap<?, ?>) { // query strategy returns hash map in the form named graph url - value
      @SuppressWarnings("unchecked")
      final Map<URL, ResultSet> resultMap = (HashMap<URL, ResultSet>) results;
      for (final URL namedGraph : resultMap.keySet()) {
        final ResultSet realResults = resultMap.get(namedGraph);
        while (realResults.hasNext()) {
          final QuerySolution solution = realResults.next();
          this.handleSolution(container, solution, namedGraph, null);
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
   * @param subject
   *          an {@link RDFNode} for the subject to be set. This is done if the client looks for triples for a subject. Leave it null if the sparql result will contain a subject.
   */
  private void handleSolution(final HitGraphContainer container, final QuerySolution solution, final URL namedGraphUrl, final RDFNode pSubject) {
    try {
      final URL graphUrl;
      if (namedGraphUrl == null && solution.getResource("g") != null) { // means: sparql query contains the graph name
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
      if (pSubject == null) { // didn't the client pass a subject node?
        if (solution.get("s") != null) {
          subject = solution.get("s");
        }
      } else {
        subject = pSubject;
      }
      RDFNode predicate = null;
      if (solution.get("p") != null) {
        predicate = solution.get("p");
      }
      RDFNode literal = null;
      if (solution.getLiteral("lit") != null) {
        literal = solution.getLiteral("lit");
      }
      if (solution.get("o") != null) {
        
          literal = solution.get("o");
        
      }
      RDFNode resolvedPer = null;
      if (solution.get("resolvedPer") != null) {
        resolvedPer = solution.get("resolvedPer");
      }
      RDFNode resolvedProj = null;
      if (solution.get("resolvedProj") != null) {
        resolvedProj = solution.get("resolvedProj");
      }
      RDFNode resolvedLoc = null;
      if (solution.get("resolvedLoc") != null) {
        resolvedLoc = solution.get("resolvedLoc");
      }
      RDFNode resolvedTime = null;
      if (solution.get("resolvedTime") != null) {
        resolvedTime = solution.get("resolvedTime");
      }
      RDFNode resolvedLing = null;
      if (solution.get("resolvedLing") != null) {
        resolvedLing = solution.get("resolvedLing");
      }
      RDFNode resolvedOrg = null;
      if (solution.get("resolvedOrg") != null) {
        resolvedOrg = solution.get("resolvedOrg");
      }
      RDFNode resolvedMed = null;
      if (solution.get("resolvedMed") != null) {
        resolvedMed = solution.get("resolvedMed");
      }
      RDFNode resolvedEvnt = null;
      if (solution.get("resolvedEvnt") != null) {
        resolvedEvnt = solution.get("resolvedEvnt");
      }
      
      RDFNode projectId = null;
      if (solution.get("projectId") != null) {
        projectId = solution.get("projectId");
      }
      RDFNode typeRes = null;
      if (solution.get("typeRes") != null) {
        typeRes = solution.get("typeRes");
      }
      RDFNode function = null;
      if (solution.get("function") != null) {
        function = solution.get("function");
      }
      RDFNode title = null;
      if (solution.get("title") != null) {
        title = solution.get("title");
      }
      RDFNode preName = null;
      if (solution.get("preName") != null) {
        preName = solution.get("preName");
      }
      RDFNode lastName = null;
      if (solution.get("lastName") != null) {
        lastName = solution.get("lastName");
      }
      RDFNode email = null;
      if (solution.get("email") != null) {
        email = solution.get("email");
      }
      RDFNode label = null;
      if (solution.get("label") != null) {
        label = solution.get("label");
      }
      RDFNode languageCode = null;
      if (solution.get("languageCode") != null) {
        languageCode = solution.get("languageCode");
      }
      RDFNode isPartOf = null;
      if (solution.get("isPartOf") != null) {
        isPartOf = solution.get("isPartOf");
      }
      RDFNode coordinates = null;
      if (solution.get("coordinates") != null) {
        coordinates = solution.get("coordinates");
      }
      RDFNode dcSubject = null;
      if (solution.get("dcSubject") != null) {
        dcSubject = solution.get("dcSubject");
      }
      RDFNode geographicAreaCode = null;
      if (solution.get("geographicAreaCode") != null) {
        geographicAreaCode = solution.get("geographicAreaCode");
      }
      RDFNode identifier = null;
      if (solution.get("identifier") != null) {
        identifier = solution.get("identifier");
      }
      RDFNode hasPart = null;
      if (solution.get("hasPart") != null) {
        hasPart = solution.get("hasPart");
      }
      RDFNode nick = null;
      if (solution.get("nick") != null) {
        nick = solution.get("nick");
      }
      RDFNode name = null;
      if (solution.get("name") != null) {
        name = solution.get("name");
      }
      RDFNode definition = null;
      if (solution.get("definition") != null) {
        definition = solution.get("definition");
      }
      RDFNode topic = null;
      if (solution.get("topic") != null) {
        topic = solution.get("topic");
      }
      RDFNode status = null;
      if (solution.get("status") != null) {
        status = solution.get("status");
      }
      RDFNode coverage = null;
      if (solution.get("coverage") != null) {
        coverage = solution.get("coverage");
      }
      RDFNode dctermsAbstract = null;
      if (solution.get("dctermsAbstract") != null) {
        dctermsAbstract = solution.get("dctermsAbstract");
      }
      RDFNode replaces = null;
      if (solution.get("replaces") != null) {
        replaces = solution.get("replaces");
      }
      RDFNode valid = null;
      if (solution.get("valid") != null) {
        valid = solution.get("valid");
      }
      RDFNode founder = null;
      if (solution.get("founder") != null) {
        founder = solution.get("founder");
      }
      RDFNode contributor = null;
      if (solution.get("contributor") != null) {
        contributor = solution.get("contributor");
      }
      RDFNode fundedBy = null;
      if (solution.get("fundedBy") != null) {
        fundedBy = solution.get("fundedBy");
      }
      RDFNode hp = null;
      if (solution.get("hp") != null) {
        hp = solution.get("hp");
      }
      RDFNode temporal = null;
      if (solution.get("temporal") != null) {
        temporal = solution.get("temporal");
      }
      
      double score = 0;
      if (solution.getLiteral("score") != null) {
        score = solution.getLiteral("score").getDouble();
      }
      RDFNode subjParent = null;
      if (solution.get("sParent") != null) {
        subjParent = solution.get("sParent");
      }
      RDFNode predParent = null;
      if (solution.get("pParent") != null) {
        predParent = solution.get("pParent");
      }
      final HitStatement statement = new HitStatement(subject, predicate, literal, score, subjParent, predParent, 
          resolvedPer, resolvedLing, resolvedTime, resolvedProj, resolvedLoc, resolvedOrg, resolvedMed, resolvedEvnt,
          projectId,typeRes,function,title,preName,lastName,email,label,languageCode,isPartOf,coordinates,dcSubject,geographicAreaCode,
          identifier,hasPart,nick,name,definition,topic,status,coverage,dctermsAbstract,replaces,valid,founder,contributor,fundedBy,hp,temporal
          );
      hitGraph.addStatement(statement);

      // set result type
      if (score == 0) { // no literal/LARQ search (because a score would be returned)
        statement.setResultType(MdSystemResultType.ALL_TRIPLES_GIVEN_SUBJECT);
      } else {
        if (namedGraphUrl == null) {
          statement.setResultType(MdSystemResultType.LITERAL_DEFAULT_GRAPH);
        } else {
          statement.setResultType(MdSystemResultType.LITERAL_NAMED_GRAPH);
        }
      }

    } catch (final MalformedURLException e) {
      logger.info("SparQlAdapter: not a valid URL (should be one): " + e.getMessage());
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
  // final ResultSet results = delegateQuery(query);
  // final StmtIterator it = results.getResourceModel().listStatements();
  //
  // // while (it.hasNext()) {
  // // final Statement statement = it.next();
  // // }
  //
  // while (results.hasNext()) { // hier kommt man nicht weiter, wie an die
  // // Tripels ran???
  // final QuerySolution solution = results.next();
  // }
  // }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.ISparqlAdapter#buildSparqlQuery(java.net.URL)
   */
  public HitGraphContainer buildSparqlQuery(final URL namedGraphUrl) {
    final T results = queryStrategy.queryGraph(namedGraphUrl);
    freeQueryStrategy();
    final HitGraphContainer hitGraphContainer = this.handleNamedGraphResults(namedGraphUrl, results);
    return hitGraphContainer;
  }

  /**
   * Handle the results for buildSparqlQuery(final URL namedGraphUrl).
   * 
   * @param namedGraphUrl
   * 
   * @param results
   * @return HitGraph to be delegated to the client
   */
  private HitGraphContainer handleNamedGraphResults(final URL namedGraphUrl, final T results) {
    final HitGraphContainer container = new HitGraphContainer(new Date());
    ResultSet realResults = null;
    final HitGraph hitGraph = new HitGraph(namedGraphUrl);
    if (results instanceof ResultSet) { // QueryStrategyFuseki
      realResults = (ResultSet) results;

    } else if (results instanceof HashMap<?, ?>) { // returned by QueryStrategyJena
      @SuppressWarnings("unchecked")
      final Map<URL, ResultSet> resultMap = (HashMap<URL, ResultSet>) results;
      realResults = resultMap.get(namedGraphUrl);
    }

    if (realResults != null) {
      while (realResults.hasNext()) {
        final QuerySolution solution = realResults.next();
        final RDFNode object = solution.get("o");
        final RDFNode predicate = solution.get("p");
        final RDFNode subject = solution.get("s");
        final HitStatement hitStatement = new HitStatement(subject, predicate, object, 0, null, null, null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            , null, null, null, null, null);
        hitGraph.addStatement(hitStatement);
        hitStatement.setResultType(MdSystemResultType.ALL_TRIPLES_NAMED_GRAPH);
      }
    }

    container.addHitRecord(namedGraphUrl, hitGraph);
    return container;
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.ISparqlAdapter#buildSparqlQuery(java.net.URL, java.lang.String)
   */
  public HitGraphContainer buildSparqlQuery(final URL namedGraphUrl, final String subject) {
    final T results = queryStrategy.queryGraph(namedGraphUrl, subject);
    freeQueryStrategy();
    final HitGraphContainer hitGraphContainer = this.handleNamedGraphSubjectResults(namedGraphUrl, subject, results);
    return hitGraphContainer;
  }

  /**
   * Handle the results for buildSparqlQuery(final URL namedGraphUrl, final String subject)
   * 
   * @param namedGraphUrl
   * 
   * @param results
   * @return HitGraph to be delegated to the client
   */
  private HitGraphContainer handleNamedGraphSubjectResults(final URL namedGraphUrl, final String pSubject, final T results) {
    final HitGraphContainer container = new HitGraphContainer(new Date());
    ResultSet realResults = null;
    final HitGraph hitGraph = new HitGraph(namedGraphUrl);
    if (results instanceof ResultSet) { // QueryStrategyFuseki
      realResults = (ResultSet) results;

    } else if (results instanceof HashMap<?, ?>) { // returned by QueryStrategyJena
      @SuppressWarnings("unchecked")
      final Map<URL, ResultSet> resultMap = (HashMap<URL, ResultSet>) results;
      realResults = resultMap.get(namedGraphUrl);
    }

    if (realResults != null) {
      while (realResults.hasNext()) {
        final QuerySolution solution = realResults.next();
        final RDFNode object = solution.get("o");
        final RDFNode predicate = solution.get("p");
        // subject is given by the client (who uses the SparqlAdapter)
        final RDFNode subject = new ResourceImpl(pSubject);
        final HitStatement hitStatement = new HitStatement(subject, predicate, object, 0, null, null, null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            , null, null, null, null, null);
        hitGraph.addStatement(hitStatement);
        hitStatement.setResultType(MdSystemResultType.ALL_TRIPLES_NAMED_GRAPH_AND_GIVEN_SUBJECT);
      }
    }
    container.addHitRecord(namedGraphUrl, hitGraph);
    return container;
  }

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
            final RelatedHitStatement relatedStatement = new RelatedHitStatement(topDownRelatedSubj, topwDownRelatedPred, topDownRelatedNode, 0, null, null, null, null, null, null, null, null, null, null, i, Relationship.CHILD
                , null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
                , null, null, null, null, null
                );
            if (!resultStatements.contains(relatedStatement)) {
              resultStatements.add(relatedStatement);
              relatedStatement.setResultType(MdSystemResultType.RELATED_CONCEPTS);
            }
          }

          // ..:: bottom-up query (node is object) ::..
          final RDFNode bottomRelatedSubj = solution.get("s1" + i);
          final RDFNode bottomDownRelatedPred = solution.get("p1" + i);
          final RDFNode bottomRelatedNode = solution.get("s1" + (i - 1)); // object is subject in the child statement

          if (bottomRelatedNode != null && bottomRelatedNode.isResource() && bottomDownRelatedPred != null && bottomRelatedSubj != null) {
            final RelatedHitStatement bottomRelatedStatement = new RelatedHitStatement(bottomRelatedSubj, bottomDownRelatedPred, bottomRelatedNode, 0, null, null, null, null, null, null, null, null, null, null, i, Relationship.PARENT
                , null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
                , null, null, null, null, null
                );
            if (!resultStatements.contains(bottomRelatedStatement)) {
              resultStatements.add(bottomRelatedStatement);
              bottomRelatedStatement.setResultType(MdSystemResultType.RELATED_CONCEPTS);
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

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.ISparqlAdapter#buildSparqlQuery(com.hp.hpl.jena.rdf.model.Resource)
   */
  public HitGraphContainer buildSparqlQuery(final Resource subject) {
    final T results = queryStrategy.querySubject(subject);
    freeQueryStrategy();
    final HitGraphContainer hitGraphContainer = this.handleSubjectResults(results, subject);
    return hitGraphContainer;
  }

  /**
   * Handle the results for buildSparqlQuery(final Resource subject)
   * 
   * @param namedGraphUrl
   * 
   * @param results
   * @param subject
   * @return HitGraph to be delegated to the client
   */
  private HitGraphContainer handleSubjectResults(final T results, final Resource subject) {
    final HitGraphContainer container = new HitGraphContainer(new Date()); // result
    // container
    if (results instanceof ResultSet) { // QueryStrategyFuseki
      final ResultSet realResults = (ResultSet) results;
      while (realResults.hasNext()) {
        final QuerySolution solution = realResults.next();
        this.handleSolution(container, solution, null, subject);
      }
    } else if (results instanceof HashMap<?, ?>) { // query strategy returns hash map in the form named graph url - value
      @SuppressWarnings("unchecked")
      final Map<URL, ResultSet> resultMap = (HashMap<URL, ResultSet>) results;
      for (final URL namedGraph : resultMap.keySet()) {
        final ResultSet realResults = resultMap.get(namedGraph);
        while (realResults.hasNext()) {
          final QuerySolution solution = realResults.next();
          this.handleSolution(container, solution, namedGraph, subject);
        }
      }
    }
    return container;
  }

  @Override
  public HitGraphContainer buildSparqlQuery(String projectUri, boolean isProjectId) {
    logger.info("building Sparql Query");
    final T results = queryStrategy.queryAllProjectInfoAndResolveUris(projectUri, isProjectId);
    freeQueryStrategy();
    return this.handleProjectIdResults(results);
  }

  private HitGraphContainer handleProjectIdResults(T results) {
    final HitGraphContainer container = new HitGraphContainer(new Date()); // result
    // container
    if (results instanceof ResultSet) { // QueryStrategyFuseki
      final ResultSet realResults = (ResultSet) results;
      while (realResults.hasNext()) {
        final QuerySolution solution = realResults.next();
        this.handleSolution(container, solution, null, null);
      }
    } else if (results instanceof HashMap<?, ?>) { // query strategy returns hash map in the form named graph url - value
      @SuppressWarnings("unchecked")
      final Map<URL, ResultSet> resultMap = (HashMap<URL, ResultSet>) results;
      for (final URL namedGraph : resultMap.keySet()) {
        final ResultSet realResults = resultMap.get(namedGraph);
        while (realResults.hasNext()) {
          final QuerySolution solution = realResults.next();
          this.handleSolution(container, solution, namedGraph, null);
        }
      }
    }

    return container;
  }

  @Override
  public HitGraphContainer sparqlAllProjectInf(String collectionsIdPattern) {
    logger.info("sparql all Project and related Info");
    final T results = queryStrategy.preloadAllProjectInfoAndResolveUris(collectionsIdPattern);
    freeQueryStrategy();
    return this.handleProjectIdResults(results);
  }
  
  @Override
  public HitGraphContainer sparqlDirectly(String pattern){
    final T results = queryStrategy.preloadAnything(pattern);
    freeQueryStrategy();
    return this.handleProjectIdResults(results);
  }
  
}
