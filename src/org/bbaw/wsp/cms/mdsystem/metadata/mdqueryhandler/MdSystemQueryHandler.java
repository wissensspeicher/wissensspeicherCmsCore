package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentfierSearchMode;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentifier;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptQueryResult;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.ISparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.SparqlAdapterFactory;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.fuseki.FusekiClient;
import org.bbaw.wsp.cms.mdsystem.util.StatisticsScheduler;

import com.hp.hpl.jena.query.ResultSet;

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
  private boolean statTimerFlag = false;
  
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
    
    StatisticsScheduler sched = StatisticsScheduler.getInstance();
    Calendar cal = Calendar.getInstance();  
    int intervall = 30;
    cal.add(Calendar.DAY_OF_MONTH, intervall * -1);
    //Wenn das aktuelle Datum -30 Tage kleiner ist als der TimeStamp, dann werden die statistiken aufgefrischt
//    System.out.println("sched.getTimeStamp() : "+sched.getTimeStamp());
    if(cal.getTimeInMillis() < sched.getTimeStamp() || sched.getTimeStamp() == 0){
      System.out.println("refreshing statistics");
      sched.setTotalNumberOfGraphs(getNumberOfGraphs());
      sched.setTotalNumberOfTriple(getTripleCount());
      sched.setNewTimeStamp(System.currentTimeMillis());
    }
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
