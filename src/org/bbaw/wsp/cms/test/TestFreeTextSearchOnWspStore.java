package org.bbaw.wsp.cms.test;

import java.sql.Date;

import org.apache.jena.larq.LARQ;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;

import com.hp.hpl.jena.rdf.model.Model;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * Several tests on the {@link WspRdfStore} to compare free text search and
 * regex - queries.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * 
 */
public class TestFreeTextSearchOnWspStore {

  private JenaMain main;
  private Model unionModel;

  public TestFreeTextSearchOnWspStore() {
    main = new JenaMain();
    try {
      main.initStore();
      unionModel = main.returnUnionModel();
      LARQ.setDefaultIndex(main.getIndexStore().getCurrentIndex());
      // main.getIndexStore().addModelToIndex(unionModel);
    } catch (ApplicationException e) {
      System.err.println("Couldn't initialise store " + e);
    }
  }

  /**
   * Do regex query
   * 
   * @return the time the query ran
   */
  public long doRegexQuery() {
    long startTime = System.currentTimeMillis();
    String query = "prefix dc:  <http://purl.org/elements/1.1/>" + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>" + "select ?eDocAggr (COUNT(distinct ?field) as ?numberFields) (COUNT(?person) as ?numberHits)" + " {" + " ?eDocAggr ?field ?person " + " Filter(regex(?person, \"Humboldt\"))" + " } " + "GROUP BY ?eDocAggr " + "ORDER BY DESC(?numberHits)";
    main.performQuery(query, unionModel);
    long endTime = System.currentTimeMillis();

    return endTime - startTime;
  }

  /**
   * Do the lucene query
   * 
   * @return the time the query ran
   */
  public long doLuceneQuery() {
    long startTime = System.currentTimeMillis();
    String query = "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#> \n" + "prefix dc:  <http://purl.org/elements/1.1/>\n" + "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n" + "select ?eDocAggr (COUNT(distinct ?field) as ?numberFields) (COUNT(?person) as ?numberHits)" + "\n {" + " \n?person  pf:textMatch '+Humboldt'.\n " + " ?eDocAggr ?field ?person. " + "\n } " + "\nGROUP BY ?eDocAggr " + "\nORDER BY DESC(?numberHits)";
    // String query = "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>"
    // //
    // +"SELECT * FROM NAMED <http://edoc.bbaw.de/volltexte/2006/30/pdf/23IXsS15gx9g.pdf> {"
    // +"SELECT *  { "
    // +"    ?lit pf:textMatch '+Humboldt'. "
    // +"}";
    System.out.println("Lucene query string: " + query);
    main.performQuery(query, unionModel);
    long endTime = System.currentTimeMillis();

    return endTime - startTime;
  }

  public void startComparison() {
    System.out.println("General data");
    System.out.println("\n\n");
    System.out.println("Number of triples of all union model: " + main.getNumberOfTriples());
    System.out.println();
    System.out.println("Staring REGEX query...");
    System.out.println("Start time:  " + System.currentTimeMillis());
    System.out.println("\n\n");
    long regExTime = doRegexQuery();
    System.out.println("Staring LUCENE query...");
    System.out.println("Start time:  " + System.currentTimeMillis());
    System.out.println("\n\n");
    long luceneTime = doLuceneQuery();
    System.out.println("\n\n");
    long directIndexTime = doDirectIndexSearch();

    System.out.println("Time for regEx-function: " + regExTime + "ms");
    System.out.println("Time for lucene-function: " + luceneTime + "ms");
    System.out.println("Time for direct lucene-search: " + directIndexTime + "ms");
    System.out.println("\n");
    System.out.println("Difference between lucene and regEx: " + (luceneTime - regExTime) + "ms");

  }

  private long doDirectIndexSearch() {
    long startTime = System.currentTimeMillis();
    String query = "+Humboldt";
    main.getIndexStore().readIndex(query);
    long endTime = System.currentTimeMillis();

    return endTime - startTime;
  }

  public static void main(String[] args) {
    new TestFreeTextSearchOnWspStore().startComparison();
  }
}
