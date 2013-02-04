package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.RdfMetadataExtractor;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.factory.MetadataExtractorFactory;
import org.openjena.riot.Lang;
import org.openjena.riot.RiotLoader;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
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

  /**
   * adds a certain statement without Sparql if the ressource already exists
   * nothing is added
   * 
   * @param model
   * @param uri
   * @param predicate
   * @param literalObj
   */
  public void updateByJenaApi(Model model, String uri, String predicate, String literalObj) {
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
  public void deleteByJenaApi(Model model, String uri, String predicate, String literalObj) {
    System.out.println("looking for matching statement to remove...");
    Resource res = model.getResource(uri);
    String resString = res.toString();
    StmtIterator it = model.listStatements();
    Statement statementToRemove = null;
    while (it.hasNext()) {
      if (it.nextStatement().getObject() != null) {
        Statement st = it.nextStatement();
        Resource subject = st.getSubject();
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
  public String getAllTriples(Model model) {
    StringBuffer dump = new StringBuffer();

    StmtIterator statements = model.listStatements();
    while (statements.hasNext()) {
      Statement statement = statements.nextStatement();
      RDFNode object = statement.getObject();
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
  public void performUpdate(Dataset dataset, String queryString) {
    GraphStore graphStore = GraphStoreFactory.create(dataset);
    UpdateRequest request = UpdateFactory.create(queryString);
    UpdateProcessor proc = UpdateExecutionFactory.create(request, graphStore);
    proc.execute();
  }

  /**
   * counts triple in a defaultgraph of the dataset
   * 
   * @param dataset
   */
  public void count(Dataset dataset) {
    QueryExecution qExec = QueryExecutionFactory.create("SELECT (count(*) AS ?count) { ?s ?p ?o} LIMIT 10", dataset);
    ResultSet rs = qExec.execSelect();
    System.out.println("");
    System.out.println("_____________triples im defaultgraph_______________");
    try {
      ResultSetFormatter.out(rs);
    } finally {
      qExec.close();
    }

  }

  /**
   * 
   * @param sparqlSelect
   * @param dataset
   * @return
   */
  public QueryExecution selectSomething(String sparqlSelect, Dataset dataset) {
    QueryExecution queExec = QueryExecutionFactory.create(sparqlSelect, dataset.getDefaultModel());
    return queExec;
  }

  /**
   * 
   * @param sparqlSelect
   * @param m
   *          a {@link Model}
   * @return
   */
  public QueryExecution selectSomething(String sparqlSelect, Model m) {
    QueryExecution queExec = QueryExecutionFactory.create(sparqlSelect, m);
    return queExec;
  }

  /**
   * fill Graph
   * 
   * @param model
   * @param loc
   */
  public Model fillModelFromFile(Model model, String loc) {
    Model moodel = FileManager.get().readModel(model, loc);
    return moodel;
  }

  /**
   * takes a file and scans it for the about id
   * 
   * @param file
   * @return String as like as ID of the file or null if the document couldn't
   *         be identificated.
   */

  public String scanID(final String file) {
    try {
      RdfMetadataExtractor fac = MetadataExtractorFactory.newRdfMetadataParser(file);
      String identifier = fac.getXmlBaseValue();
      System.out.println("Identified document: " + identifier);
      return identifier;
    } catch (ApplicationException e) {
      System.out.println("Couldn't identify document: " + file + " - " + e.getMessage());
      return null;
    }
  }

  public void readQuadsViaRiot(String filename, DatasetGraph dataset, Lang lang, String baseURI) {
    RiotLoader.read(filename, dataset, lang, baseURI);
  }

  public void readFileViaRiot(String filename, DatasetGraph dataset, Lang lang, String baseURI) {
    RiotLoader.read(filename, dataset, lang, baseURI);
  }

  /**
   * Helper method that splits up a URI into a namespace and a local part. It
   * uses the prefixMap to recognize namespaces, and replaces the namespace part
   * by a prefix.
   * 
   * @param prefixMap
   * @param resource
   */
  public static String[] split(PrefixMapping prefixMap, Resource resource) {
    String uri = resource.getURI();
    if (uri == null) {
      return new String[] { null, null };
    }
    Map<String, String> prefixMapMap = prefixMap.getNsPrefixMap();
    Set<String> prefixes = prefixMapMap.keySet();
    String[] split = { null, null };
    for (String key : prefixes) {
      String ns = prefixMapMap.get(key);
      if (uri.startsWith(ns)) {
        split[0] = key;
        split[1] = uri.substring(ns.length());
        return split;
      }
    }
    split[1] = uri;
    return split;
  }
}
