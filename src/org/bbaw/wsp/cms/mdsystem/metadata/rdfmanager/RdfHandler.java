package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.RdfMetadataExtractor;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.factory.MetadataExtractorFactory;
import org.openjena.riot.Lang;
import org.openjena.riot.RiotLoader;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.FileManager;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This class is designed to operate on jena tdb
 * 
 * @author marco juergens
 * @author sascha feldmann
 * @author marco seidler
 * 
 */
public class RdfHandler {

  private static final String URN_UNION = "urn:x-arq:UnionGraph";
  private Logger logger;

  /**
   * adds a certain statement without Sparql if the ressource already exists nothing is added
   * 
   * @param model
   * @param uri
   * @param predicate
   * @param literalObj
   */
  public void updateByJenaApi(final Model model, final String uri, final String predicate, final String literalObj) {
    System.out.println("adding ...");
    model.add(model.createResource(uri), model.createProperty(predicate), model.createLiteral(literalObj));
    model.commit();
    model.close();
    System.out.println("ressource added");
    // model.write(new PrintWriter(System.out));
    model.write(System.out, "N3");
  }

  /**
   * removes a certain statement without Sparql
   * 
   * @param model
   * @param uri
   * @param predicate
   * @param literalObj
   */
  public void deleteByJenaApi(final Model model, final String uri, final String predicate, final String literalObj) {
    System.out.println("looking for matching statement to remove...");
    final Resource res = model.getResource(uri);
    final String resString = res.toString();
    final StmtIterator it = model.listStatements();
    Statement statementToRemove = null;
    while (it.hasNext()) {
      if (it.nextStatement().getObject() != null) {
        final Statement st = it.nextStatement();
        final Resource subject = st.getSubject();
        if (subject.toString().equals(resString)) {
          if (st.getPredicate().toString().equals(predicate)) {
            if (st.getObject().toString().equals(literalObj)) {
              statementToRemove = st;
              break;
            }
          }
        }
      }
    }

    if (statementToRemove != null) {
      System.out.println("removing statement: " + statementToRemove);
      model.remove(statementToRemove);
    } else {
      System.out.println("no matching statement found.");
    }
    model.close();
  }

  /**
   * 
   * @param model
   * @return
   */
  public String getAllTriples(final Model model) {
    final StringBuffer dump = new StringBuffer();

    final StmtIterator statements = model.listStatements();
    while (statements.hasNext()) {
      final Statement statement = statements.nextStatement();
      final RDFNode object = statement.getObject();
      dump.append(statement.getSubject().getLocalName()).append(' ').append(statement.getPredicate().getLocalName()).append(' ').append((object instanceof Resource || object instanceof Literal ? object.toString() : '"' + object.toString() + "\"")).append('\n');
    }
    return dump.toString();
  }

  /**
   * INSERT
   * 
   * @param dataset
   * @param queryString
   */
  public void performUpdate(final Dataset dataset, final String queryString) {
    final GraphStore graphStore = GraphStoreFactory.create(dataset);
    final UpdateRequest request = UpdateFactory.create(queryString);
    final UpdateProcessor proc = UpdateExecutionFactory.create(request, graphStore);
    proc.execute();
  }

  /**
   * Perform a select query on a dataset. Use the union model of all named graphes to execute the query.
   * 
   * @param sparqlSelect
   * @param dataset
   * @return
   */
  public QueryExecution selectSomething(final String sparqlSelect, final Dataset dataset) {
    final QueryExecution queExec = QueryExecutionFactory.create(sparqlSelect, getUnionModel(dataset));
    return queExec;
  }

  /**
   * Return the union model of all named graphes.
   * 
   * @param dataset
   * @return
   */
  public Model getUnionModel(final Dataset dataset) {
    return dataset.getNamedModel(URN_UNION);
  }

  /**
   * 
   * @param sparqlSelect
   * @param m
   *          a {@link Model}
   * @return
   */
  public QueryExecution selectSomething(final String sparqlSelect, final Model m) {
    final QueryExecution queExec = QueryExecutionFactory.create(sparqlSelect, m);
    return queExec;
  }

  /**
   * fill Graph
   * 
   * @param model
   * @param loc
   */
  public Model fillModelFromFile(final Model model, final String loc) {
    final Model moodel = FileManager.get().readModel(model, loc);
    return moodel;
  }

  /**
   * takes a file and scans it for the about id
   * 
   * @param file
   * @return String as like as ID of the file or null if the document couldn't be identificated.
   */

  public String scanID(final String file) {
    try {
      final RdfMetadataExtractor fac = MetadataExtractorFactory.newRdfMetadataParser(file);
      final String identifier = fac.getXmlBaseValue();
      System.out.println("Identified document: " + identifier);
      return identifier;
    } catch (final ApplicationException e) {
      System.out.println("Couldn't identify document: " + file + " - " + e.getMessage());
      return null;
    }
  }

  public void readQuadsViaRiot(final String filename, final DatasetGraph dataset, final Lang lang, final String baseURI) {
    RiotLoader.read(filename, dataset, lang, baseURI);
  }

  public void readFileViaRiot(final String filename, final DatasetGraph dataset, final Lang lang, final String baseURI) {
    RiotLoader.read(filename, dataset, lang, baseURI);
  }

  /**
   * Helper method that splits up a URI into a namespace and a local part. It uses the prefixMap to recognize namespaces, and replaces the namespace part by a prefix.
   * 
   * @param prefixMap
   * @param resource
   */
  public static String[] split(final PrefixMapping prefixMap, final Resource resource) {
    final String uri = resource.getURI();
    if (uri == null) {
      return new String[] { null, null };
    }
    final Map<String, String> prefixMapMap = prefixMap.getNsPrefixMap();
    final Set<String> prefixes = prefixMapMap.keySet();
    final String[] split = { null, null };
    for (final String key : prefixes) {
      final String ns = prefixMapMap.get(key);
      if (uri.startsWith(ns)) {
        split[0] = key;
        split[1] = uri.substring(ns.length());
        return split;
      }
    }
    split[1] = uri;
    return split;
  }

  /**
   * Execute a query on all named graphes in a dataset.
   * 
   * @param dataset
   *          the {@link Dataset}
   * @param sparqlSelect
   *          the sparQl query to be executed ON EACH named graph.
   * @return a {@link Map} with the URL of the named graph as key
   */
  public Map<URL, ResultSet> queryAllNamedModels(final Dataset dataset, final String sparqlSelect) {
    final Collection<URL> graphUris = fetchAllNamedGraphes(dataset);
    final Map<URL, ResultSet> resultMap = new HashMap<URL, ResultSet>();
    for (final URL url : graphUris) {
      final Model namedModel = dataset.getNamedModel(url.toExternalForm());
      final QueryExecution ex = this.selectSomething(sparqlSelect, namedModel);

      // final ResultSet results = ex.execSelect();
      resultMap.put(url, ex.execSelect());
    }
    return resultMap;
  }

  /**
   * Fetch a {@link Collection} of all {@link URL} of the named graphes within a dataset.
   * 
   * @param dataset
   *          the {@link Dataset}
   * @return a {@link Collection} of {@link URL}
   */
  public Collection<URL> fetchAllNamedGraphes(final Dataset dataset) {
    final Collection<URL> urls = new ArrayList<URL>();
    final Iterator<Node> it = dataset.asDatasetGraph().listGraphNodes();

    while (it.hasNext()) {
      final Node graphName = it.next();
      try {
        urls.add(new URL(graphName.getURI()));
      } catch (final MalformedURLException e) {
        getLogger().error("RdfHandler: Error while trying to fetch named graphes URI\n " + e.getMessage());
        // System.err.println();
      }
    }
    return urls;
  }

  /**
   * Build the larq index for a given dataset.
   * 
   * @param dataset
   *          the {@link Dataset} instance which contains the models to be indexed.
   * @param indexStore
   *          the {@link IndexStore} which will enrich and offer the active index.
   */
  public void buildLarqIndex(final Dataset dataset, final IndexStore indexStore) {
    final Collection<URL> namedGraphUrls = fetchAllNamedGraphes(dataset);
    for (final URL namedGraphUrl : namedGraphUrls) {
      getLogger().info("RdfHandler: trying to build larq index for named model " + namedGraphUrl);
      final Model namedGraphModel = dataset.getNamedModel(namedGraphUrl.toExternalForm());
      indexStore.addModelToIndex(namedGraphModel);
    }
  }

  private Logger getLogger() {
    if (null == logger) {
      final Logger logger = Logger.getLogger(getClass());
      this.logger = logger;
    }
    return logger;
  }
}
