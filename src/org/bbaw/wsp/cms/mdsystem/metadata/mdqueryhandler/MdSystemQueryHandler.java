package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.fuseki.http.DatasetAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentfierSearchMode;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentifier;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptQueryResult;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.ISparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.SparqlAdapterFactory;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.fuseki.FusekiClient;
import org.bbaw.wsp.cms.mdsystem.util.MdSystemConfigReader;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.util.DatasetUtils;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * handles the JSON-query sent from the GUI and delegates to @SparqlAdapter and @ConceptIdentifier
 * 
 * @author marco juergens
 * 
 */

public class MdSystemQueryHandler {

  private ConceptIdentifier identifier;
  private ISparqlAdapter sparqlAdapter;
  private static MdSystemQueryHandler mdSystemQueryHandler;
  private WspRdfStore store;

  private MdSystemQueryHandler() {
  }

  public static MdSystemQueryHandler getInstance() {
    if (mdSystemQueryHandler == null) {
      mdSystemQueryHandler = new MdSystemQueryHandler();
    }
    return mdSystemQueryHandler;
  }

  public void init() {
    this.store = WspRdfStore.getInstance();
    URL datasetUrl;
    try {
      datasetUrl = new URL("http://localhost:3030/ds");
      sparqlAdapter = SparqlAdapterFactory.getDefaultAdapter(datasetUrl);
      identifier = new ConceptIdentifier();
    } catch (final MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // sparqlAdapter = new SparqlAdapter(store.getDataset());
  }

  /**
   * use locally
   * 
   * @param query
   * @return
   */
  public List<ConceptQueryResult> getConcept(final String query) {
    identifier.initIdentifying(query, ConceptIdentfierSearchMode.METHODE_OR);
    final List<ConceptQueryResult> results = identifier.getResultList();
    return results;
  }

  /**
   * get number of triples 
   */
  public String getTripleCount() {
    URL datasetUrl = null;
    try {
      datasetUrl = new URL("http://localhost:3030/ds");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    FusekiClient fusekiCl = FusekiClient.getInstance();
    ResultSet rs = fusekiCl.performSelect(datasetUrl.toExternalForm(), "SELECT (COUNT(*) AS ?number) { ?s ?p ?o  }");
    //can only be one result, but nonetheless
    List<String> rsList = new ArrayList<String>();
    while(rs.hasNext()){
      rsList.add(rs.next().getLiteral("?number").getString());
    }
    System.out.println("rsList.size() : "+rsList.size());
    String number = null;  
    if(rsList.size() == 1){
      number = rsList.get(0);
    }else{
      number = "No counting of triples possible.";
    }
        System.out.println("number : "+number);
    return number;
  }
  
  /**
   * get number of Named Graphs
   * @return
   */
  public String getNumberOfGraphs(){
    URL datasetUrl = null;
    try {
      datasetUrl = new URL("http://localhost:3030/ds");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    FusekiClient fusekiCl = FusekiClient.getInstance();
    ResultSet rs = fusekiCl.performSelect(datasetUrl.toExternalForm(), "SELECT (Count(DISTINCT ?g) AS ?numGraphs) WHERE { GRAPH ?g { ?s ?p ?o } }");
    //can only be one result, but nonetheless
    List<String> rsList = new ArrayList<String>();
    while(rs.hasNext()){
      rsList.add(rs.next().getLiteral("?numGraphs").getString());
    }
    System.out.println("rsList.size() : "+rsList.size());
    String number = null;  
    if(rsList.size() == 1){
      number = rsList.get(0);
    }else{
      number = "No counting of triples possible.";
    }
        System.out.println("number : "+number);
    return number;
  }
}
