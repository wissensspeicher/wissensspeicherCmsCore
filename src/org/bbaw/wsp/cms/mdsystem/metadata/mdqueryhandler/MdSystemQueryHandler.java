package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.collections.Collection;
import org.bbaw.wsp.cms.collections.CollectionReader;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentfierSearchMode;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentifier;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptQueryResult;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.HitGraphContainer;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.IQueryStrategy;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.ISparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.QueryStrategyJena;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.SparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.SparqlAdapterFactory;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.fuseki.FusekiClient;
import org.bbaw.wsp.cms.mdsystem.util.StatisticsScheduler;

import sun.util.logging.resources.logging;

import com.hp.hpl.jena.query.ResultSet;

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
  private boolean statTimerFlag = false;
  private  Logger logger;
  
  private MdSystemQueryHandler() {
    this.store = WspRdfStore.getInstance();
    URL datasetUrl;
    logger = Logger.getLogger(MdSystemQueryHandler.class);
    try {
      //default case is to use Fuseki
      datasetUrl = new URL("http://localhost:3030/ds");
      sparqlAdapter = SparqlAdapterFactory.getDefaultAdapter(datasetUrl);
      logger.info("sparql adapter startet, uses fuseki");
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
    if(cal.getTimeInMillis() > sched.getTimeStamp() || sched.getTimeStamp() == -1) {
      logger.info("refreshing statistics");
      sched.setTotalNumberOfGraphs(getNumberOfGraphs());
      sched.setTotalNumberOfTriple(getTripleCount());
      sched.setNewTimeStamp(System.currentTimeMillis());
    }
  }

  /**
   * singleton
   * @return
   */
  public static MdSystemQueryHandler getInstance() {
    if (mdSystemQueryHandler == null) {
      mdSystemQueryHandler = new MdSystemQueryHandler();
    }
    return mdSystemQueryHandler;
  }

  private ArrayList<Collection> getCollectionIds(){
    ArrayList<Collection> collectionList = null;
    try {
      collectionList = CollectionReader.getInstance().getCollections();
    } catch (ApplicationException e) {
      e.printStackTrace();
    }
    return collectionList;
  }
  
  public HitGraphContainer preloadAllProjectInf(){
    ArrayList<Collection> collectionIds = getCollectionIds();
    StringBuilder sb = new StringBuilder();
    sb.append(
      "SELECT *\n"+ 
          "WHERE {\n"+ 
          "GRAPH <http://wsp.normdata.rdf/> { ?s ?p ?o }\n"+ 
        "}");
    String sparqlPattern = sb.toString();
    logger.info("preloadAllProjectInf, sparqlPattern: "+sparqlPattern);
    return sparqlAdapter.sparqlDirectly(sparqlPattern);
//    return sparqlAdapter.sparqlAllProjectInf(sparqlPattern);
  }
//  
//  public HitGraphContainer preloadPersonInf(){
//    ArrayList<Collection> collectionIds = getCollectionIds();
//    StringBuilder sb = new StringBuilder();
//    sb.append(
//    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
//    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
//    "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
//    "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" +
//    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
//    "PREFIX gnd: <http://d-nb.info/standards/elementset/gnd#>" +
//    " \n"+
//
//    "SELECT DISTINCT ?projectId ?function ?title ?preName ?lastName ?email ?gnd" +
//    " \n"+
//    "WHERE {\n" +
//    "?projectId ?pr ?ob .\n" +
//    "?projectId rdf:type foaf:Project .\n" +
//    "?ob rdf:type foaf:Person.\n" +
//
//    "OPTIONAL {\n" +
//    "?ob gnd:functionOrRole ?function ." +
//    "}\n" +
//    "OPTIONAL {" +
//    "?ob foaf:title ?title ." +
//    "}\n" +
//    "OPTIONAL {" +
//    "?ob dcterms:identifier ?gnd ." +
//    "}" +
//    "OPTIONAL {" +
//    "?ob foaf:givenName ?preName ." +
//    "}\n" +
//    "OPTIONAL {" +
//    "?ob foaf:familyName ?lastName ." +
//    "}\n" +
//    "OPTIONAL {" +
//    "?ob foaf:mbox ?email ." +
//    "} \n}" +
//    " \n");
//    String sparqlPattern = sb.toString();
//    logger.info("preload Person Inf...");
//    return sparqlAdapter.sparqlDirectly(sparqlPattern);
//  }
//  
//  public HitGraphContainer preloadLingInf(){
//    ArrayList<Collection> collectionIds = getCollectionIds();
//    StringBuilder sb = new StringBuilder();
//    sb.append(
//    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
//    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"+
//    "PREFIX dcterms: <http://purl.org/dc/terms/>\n"+
//    "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n"+
//    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"+
//    "PREFIX gnd: <http://d-nb.info/standards/elementset/gnd#>\n"+
//    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+
//    " \n"+
//    
//    "SELECT DISTINCT ?projectId ?label ?languageCode ?isPartOf"+
//    " \n"+  
//    "WHERE {\n"+
//    "  ?projectId ?pr ?ob .\n"+
//    "  ?projectId rdf:type foaf:Project .\n"+
//    "  ?ob rdf:type dcterms:LinguisticSystem.\n"+
//    
//    "OPTIONAL {"+
//    "  ?ob gnd:languageCode ?languageCode."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob rdfs:label ?label."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dcterms:isPartOf ?isPartOf."+
//    "}\n"+
//    "}" +
//    " \n");
//    String sparqlPattern = sb.toString();
//    logger.info("preload Linguistic Inf...");
//    return sparqlAdapter.sparqlDirectly(sparqlPattern);
//  }
//  
//  public HitGraphContainer preloadLocInf(){
//    ArrayList<Collection> collectionIds = getCollectionIds();
//    StringBuilder sb = new StringBuilder();
//    sb.append(
//    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
//    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
//    "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
//    "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" +
//    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
//    "PREFIX gnd: <http://d-nb.info/standards/elementset/gnd#>\n" +
//    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
//    " \n"+
//    
//    "SELECT DISTINCT ?projectId ?label ?coordinates ?isPartOf ?subject ?geographicAreaCode ?identifier ?hasPart" +
//    " \n"+
//    "WHERE {\n" +
//    "  ?projectId ?pr ?ob .\n" +
//    "  ?projectId rdf:type foaf:Project .\n" +
//    "  ?ob rdf:type dcterms:Location .\n" +
//    
//    "OPTIONAL {" +
//    "  ?ob gnd:coordinates ?coordinates." +
//    "}\n" +
//    "OPTIONAL {" +
//    "  ?ob rdfs:label ?label." +
//    "}\n" +
//    "OPTIONAL {" +
//    "  ?ob dcterms:isPartOf ?isPartOf." +
//    "}\n" +
//    "OPTIONAL {" +
//    "  ?ob dc:subject ?subject." +
//    "}\n" +
//    "OPTIONAL {" +
//    "  ?ob gnd:geographicAreaCode ?geographicAreaCode." +
//    "}\n" +
//    "OPTIONAL {" +
//    "  ?ob dcterms:identifier ?identifier." +
//    "}\n" +
//    "OPTIONAL {" +
//    "  ?ob dcterms:hasPart ?hasPart." +
//    "}\n" +
//    "}" +
//    " \n");
//    String sparqlPattern = sb.toString();
//    logger.info("preload Location Inf...");
//    return sparqlAdapter.sparqlDirectly(sparqlPattern);
//  }
//  
//  
//  
//  public HitGraphContainer preloadProjInf(){
//    ArrayList<Collection> collectionIds = getCollectionIds();
//    StringBuilder sb = new StringBuilder();
//    sb.append(
//    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
//    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"+
//    "PREFIX dcterms: <http://purl.org/dc/terms/>\n"+
//    "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n"+
//    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"+
//    "PREFIX gnd: <http://d-nb.info/standards/elementset/gnd#>\n"+
//    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"+
//    "SELECT DISTINCT ?projectId ?typeRes ?contributor ?coverage ?abstract ?replaces ?topic ?valid ?isPartOf ?definition ?name ?status\n"+
//    "WHERE {\n"+
//    "  ?projectId ?pr ?ob .\n"+
//    "  ?projectId rdf:type foaf:Project .\n"+
//    "  ?ob rdf:type foaf:Project.\n"+
//
//    "OPTIONAL {"+
//    "  ?ob rdf:type ?typeRes."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dc:contributor ?contributor."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dc:coverage ?coverage."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dcterms:abstract ?abstract."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dcterms:replaces ?replaces."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob gnd:topic  ?topic."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dcterms:valid ?valid."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dcterms:isPartOf ?isPartOf."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob gnd:definition ?definition."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob foaf:name ?name."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob foaf:status ?status."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob gnd:founder ?founder."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob foaf:fundedBy ?fundedBy."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob foaf:nick ?nick."+
//    "}\n"+
//    "}" +
//    " \n");
//    String sparqlPattern = sb.toString();
//    logger.info("preload Project Inf: ");
//    logger.info(sparqlPattern);
//    return sparqlAdapter.sparqlDirectly(sparqlPattern);
//  }
//  
//  public HitGraphContainer preloadOrgInf(){
//    ArrayList<Collection> collectionIds = getCollectionIds();
//    StringBuilder sb = new StringBuilder();
//    sb.append(
//    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
//    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"+
//    "PREFIX dcterms: <http://purl.org/dc/terms/>\n"+
//    "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n"+
//    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"+
//    "PREFIX gnd: <http://d-nb.info/standards/elementset/gnd#>\n"+
//    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"+
//    "SELECT DISTINCT ?projectId ?typeRes ?type ?hasPart ?hp ?coverage ?name ?valid ?abstract ?status\n"+
//    "WHERE {\n"+
//    "  ?projectId ?pr ?ob .\n"+
//    "  ?projectId rdf:type foaf:Project .\n"+
//    "  ?ob rdf:type foaf:Organization.\n"+
//    
//    "OPTIONAL {"+
//    "  ?ob dcterms:hasPart ?hasPart."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob foaf:homepage ?hp."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dc:coverage ?coverage."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob rdf:type ?typeRes."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob foaf:name ?name."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dcterms:valid ?valid."+
//    "}"+
//    "OPTIONAL {"+
//    "  ?ob dcterms:abstract ?abstract."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob foaf:status ?status."+
//    "}\n"+
//    "}" +
//    " \n");
//    String sparqlPattern = sb.toString();
//    logger.info("preload Oragnization Inf...");
//    return sparqlAdapter.sparqlDirectly(sparqlPattern);
//  } 
//  
//  public HitGraphContainer preloadMediaInf(){
//    ArrayList<Collection> collectionIds = getCollectionIds();
//    StringBuilder sb = new StringBuilder();
//    sb.append(
//    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
//    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"+
//    "PREFIX dcterms: <http://purl.org/dc/terms/>\n"+
//    "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n"+
//    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"+
//    "PREFIX gnd: <http://d-nb.info/standards/elementset/gnd#>\n"+
//    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"+
//    "SELECT DISTINCT ?projectId ?typeRes ?label ?isPartOf\n"+
//    "WHERE {\n"+
//    "  ?projectId ?pr ?ob .\n"+
//    "  ?projectId rdf:type foaf:Project .\n"+
//    "  ?ob rdf:type dcterms:MediaType.\n"+
//    
//    "OPTIONAL {"+
//    "  ?ob rdf:type ?typeRes."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob rdfs:label ?label."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dcterms:isPartOf ?isPartOf."+
//    "}\n"+
//    "}" +
//    " \n");
//    String sparqlPattern = sb.toString();
//    logger.info("preload Media Inf...");
//    return sparqlAdapter.sparqlDirectly(sparqlPattern);
//  }
//  
//  public HitGraphContainer preloadPerOfTimeInf(){
//    ArrayList<Collection> collectionIds = getCollectionIds();
//    StringBuilder sb = new StringBuilder();
//    sb.append(
//    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
//    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"+
//    "PREFIX dcterms: <http://purl.org/dc/terms/>\n"+
//    "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n"+
//    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"+
//    "PREFIX gnd: <http://d-nb.info/standards/elementset/gnd#>\n"+
//    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"+
//    "SELECT DISTINCT ?projectId ?typeRes ?label ?temporal\n"+
//    "WHERE {\n"+
//    "  ?projectId ?pr ?ob .\n"+
//    "  ?projectId rdf:type foaf:Project .\n"+
//    "  ?ob rdf:type dcterms:PeriodOfTime.\n"+
//    
//    "OPTIONAL {"+
//    "  ?ob rdf:type ?typeRes."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob rdfs:label ?label."+
//    "}\n"+
//    "OPTIONAL {"+
//    "  ?ob dcterms:temporal ?temporal."+
//    "}\n"+
//    "}" +
//    " \n");
//    String sparqlPattern = sb.toString();
//    logger.info("preload Period of Time Inf...");
//    return sparqlAdapter.sparqlDirectly(sparqlPattern);
//  }
//  
//  
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
    String number = null;  
    if(rsList.size() == 1){
      number = rsList.get(0);
    }else{
      number = "No counting of triples possible.";
    }
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
    String number = null;  
    if(rsList.size() == 1){
      number = rsList.get(0);
    }else{
      number = "No counting of triples possible.";
    }
    return number;
  }
  
  public ISparqlAdapter getSparqlAdapter(){
    return this.sparqlAdapter;
  }
  
  public static ISparqlAdapter useFuseki() {
    URL fusekiDatasetUrl;
    try {
      fusekiDatasetUrl = new URL("http://localhost:3030/ds");
      return SparqlAdapterFactory.getDefaultAdapter(fusekiDatasetUrl);
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
      final IQueryStrategy<Map<URL, ResultSet>> queryStrategy = new QueryStrategyJena(jenamain);
      final ISparqlAdapter adapter = new SparqlAdapter<>(queryStrategy);
      return adapter;
    } catch (final ApplicationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
    // jenamain.makeDefaultGraphUnion();
  }

}
