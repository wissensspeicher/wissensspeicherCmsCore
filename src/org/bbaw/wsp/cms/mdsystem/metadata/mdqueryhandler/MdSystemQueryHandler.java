package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.ISparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.SparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.SparqlAdapterFactory;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;
import org.bbaw.wsp.cms.mdsystem.util.WspJsonEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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

  private MdSystemQueryHandler() {
  }

  public static MdSystemQueryHandler getInstance(){
    if(mdSystemQueryHandler == null)
      mdSystemQueryHandler = new MdSystemQueryHandler();
    return mdSystemQueryHandler;
  }
  
  public void init() {
    final WspRdfStore store = WspRdfStore.getInstance();
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

//  public String queryConcepts(String query){
//    ArrayList<ConceptQueryResult> concepts = getConcept(query);
//    return createJson(query, concepts, true);
//  }
  
//  /**
//   * toDo: move to servlet
//   * @param query
//   */
//  public void receiveQueryFromGui(final String query) {
//    final ArrayList<ConceptQueryResult> resList = getConcept(query);
//    if (!resList.isEmpty()) {
//      // wenn vorhaben identifiziert wurde
//      try {
//        sparqlAdapter.buildSparqlQuery(new URL(resList.get(0).toString()), resList.get(1).toString());
//      } catch (final MalformedURLException e) {
//        e.printStackTrace();
//      }
//    } else {
//      sparqlAdapter.buildSparqlQuery(query);
//    }
//
//  }

  /**
   * use locally
   * 
   * @param query
   * @return
   */
  public ArrayList<ConceptQueryResult> getConcept(final String query) {
    identifier.initIdentifying(query, ConceptIdentfierSearchMode.METHODE_OR);
    ArrayList<ConceptQueryResult> results = identifier.getResultList();
    return results;
  }

  private String createJson(String query, ArrayList<ConceptQueryResult> concepts, boolean conceptSearch){
      WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
      jsonEncoder.clear();
      jsonEncoder.putStrings("searchTerm", query);
      jsonEncoder.putStrings("numberOfHits", String.valueOf(concepts.size()));
      JSONArray jsonOuterArray = new JSONArray();
      JSONObject jsonWrapper = null;
      if(conceptSearch){
          for (int i=0; i<concepts.size(); i++) {
              System.out.println("**************");
              System.out.println("results.get(i) : "+concepts.get(i));
              System.out.println("getSet() : "+concepts.get(i).getAllMDFields());
              Set<String> keys = concepts.get(i).getAllMDFields();
              JSONArray jsonInnerArray = new JSONArray();
              for (String s : keys) {
                  System.out.println("concepts.get(i).getValue(s) : "+concepts.get(i).getValue(s));
                  jsonWrapper = new JSONObject();
                  jsonWrapper.put(s, concepts.get(i).getValue(s));
                  jsonInnerArray.add(jsonWrapper);
              }
              System.out.println("*******************");
              jsonOuterArray.add(jsonInnerArray);
          }
      }
      jsonEncoder.putJsonObj("mdHits", jsonOuterArray);

      System.out.println(JSONValue.toJSONString(jsonEncoder.getJsonObject()));
      return JSONValue.toJSONString(jsonEncoder.getJsonObject());
  }
  
  // public getSparqlHits(){
  // HitRecordContainer hits = sparqlAdapter.getHitRecordContainer();
  // }
  //

  /**
   * offene Fragen: wie sieht die JSON-Query aus? was wird von einer suche in
   * den metadaten als ergebnis erwartet? eine trefferliste nach häufigkeit?
   * inhalte von literalen? wissensdomänen? wenn dies von der query abhängig
   * ist: woher weiß der SparqlAdapter, was zurückgeliefert werden soll? siehe @ConceptIdentifier
   * 
   * 
   * Alternative Strategien
   * 
   * Strategie nr 1: nimm suchterm und parameter aus JSON identifiziere das
   * konzept in den metadaten, dass dem suchterm(en) entspricht
   * (@ConceptIdentifier) suche im defaultgraph alle vorkommen des konzepts
   * falls es parameter gibt, stelle anhand der parameter einen sparql string
   * zusammen (@SparqlAdapter) wende den parametern entsprechend das passende
   * sparql-muster an liefere alle inhalte der objekte an die gui zurück
   * (geordnet trefferliste )
   * 
   * pros&cons: wahrscheinlich am meisten performance-lastig, wenn immer
   * zunächst in @ConceptIdentifier der query string mit den subjects
   * abgeglichen werden muss
   * 
   * ---------------------------
   * 
   * Strategie nr 2: nimm suchterm und parameter aus JSON suche im namedgraph
   * "wsp.normdata.rdf" mit hilfe von pf:textmatch alle vorkommen des suchterms
   * in den subjects suche alle subjects mit den prädikaten "topic", "about"
   * und "isPartOf" und speichere deren objekte auf variablen liefere diese als
   * Wissensdomänen an die GUI zurück suche alle subjects mit dem prädikat
   * "familyName" und liefere diese als Person (mit dc:description) an die GUI
   * zurück
   * 
   * pros&cons wsp.normdata.rdf fungiert quasi als "meta-metadata", was
   * vermeidet, dass bereits im ersten schritt ALLE metadaten durchsucht werden
   * müssen gibt nur grob in formation über einen suchterm und liefert
   * (wahrscheinlich) nicht alle zusammenhänge (z.B. da wsp.normdata.rdf nicht
   * die häufigkeit eines suchterms enthält) wie kann anhand der JSON-query
   * entschieden werden, ob in wsp.normdata.rdf oder im defaultgraph gesucht
   * werde muss?
   * 
   * -------------------------
   * 
   * Strategie nr 3: suche mit pf:textmatch nach dem suchterm in den metadaten
   * liefere geordnet nach häufigkeit des vorkommens an die gui zurück das
   * ergebnis in der GUI wäre so etwas wie:
   * "folgende Metadaten enthalten den suchterm xy"
   * 
   * pros&cons sehr simpel und schnell unterschidet nicht zwischen subjekt oder
   * objekt überlässt der gui die verwertung
   * 
   * ----------------------------------
   * 
   * 
   */

}
