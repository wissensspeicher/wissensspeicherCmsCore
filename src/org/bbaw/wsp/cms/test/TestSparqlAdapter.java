package org.bbaw.wsp.cms.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.ISparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.QueryStrategyFuseki;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.QueryStrategyHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.QueryStrategyJena;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.SparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ResultSet;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * Test of the SparqlAdapter.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * 
 */
public class TestSparqlAdapter {

  public static void main(final String[] args) throws ApplicationException, MalformedURLException {

    final ISparqlAdapter adapter = useFuseki();
    adapter.buildSparqlQuery("+Humboldt");
    // adapter.buildSparqlQuery(new
    // URL("http://edoc.bbaw.de/volltexte/2006/1/pdf/29kstnGPLz2IM.pdf"),
    // "+ein");
    // URL subject = new URL("http://wsp.bbaw.de/edoc/2006/1/aggregation");
    // URL predicate = new URL("http://purl.org/elements/1.1/abstract");
    // String object;
    // adapter.buildSparqlQuery(new
    // URL("http://edoc.bbaw.de/volltexte/2006/1/pdf/29kstnGPLz2IM.pdf"),
    // subject, predicate, "?o");
  }

  public static ISparqlAdapter useFuseki() {
    URL fusekiDatasetUrl;
    try {
      fusekiDatasetUrl = new URL("http://localhost:3030/ds");
      final IQueryStrategy<ResultSet> strategy = new QueryStrategyFuseki(fusekiDatasetUrl);
      final ISparqlAdapter adapter = new SparqlAdapter<ResultSet>(strategy);
      return adapter;
    } catch (final MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }

  }

  public static ISparqlAdapter useJena() {
    final JenaMain jenamain = new JenaMain();
    try {
      jenamain.initStore();
      final RdfHandler handler = new RdfHandler();
      final Dataset dataset = jenamain.getDataset();
      final IQueryStrategy<Map<URL, ResultSet>> queryStrategy = new QueryStrategyJena(handler, dataset);
      final ISparqlAdapter adapter = new SparqlAdapter<>(queryStrategy);
      return adapter;
    } catch (final ApplicationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
    // jenamain.makeDefaultGraphUnion();

  }

  public static ISparqlAdapter useRdfHandler() {
    final JenaMain jenamain = new JenaMain();

    try {
      jenamain.initStore();
      final RdfHandler handler = new RdfHandler();
      final Dataset dataset = jenamain.getDataset();
      final IQueryStrategy<ResultSet> queryStrategy = new QueryStrategyHandler(handler, dataset);
      final ISparqlAdapter adapter = new SparqlAdapter<>(queryStrategy);
      return adapter;
    } catch (final ApplicationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }

  }
}
