package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.HitRecordContainer;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.SparqlAdapter;

/**
 * handles the JSON-query sent from the GUI and delegates to @SparqlAdapter and @ConceptIdentifier
 * 
 * @author marco juergens
 * 
 */

public class MdSystemQueryHandler {

  private ConceptIdentifier identifier;
  private SparqlAdapter sparqlAdapter;

  public MdSystemQueryHandler() {
    init();
  }

  private void init() {
    WspRdfStore store = WspRdfStore.getInstance();
    sparqlAdapter = new SparqlAdapter(store.getDataset());
    this.identifier = new ConceptIdentifier();
  }

  public void receiveQueryFromGui(String query) {
    ArrayList<String> resList = getConcept(query);
    if (!resList.isEmpty()) {
      // wenn vorhaben identifiziert wurde
      try {
        sparqlAdapter.buildSparqlQuery(new URL(resList.get(1)), resList.get(0));
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    } else {
      sparqlAdapter.buildSparqlQuery(query);
    }

  }

  public ArrayList<String> getConcept(String query) {
    // TODO : Unterscheidung zwischen einem und mehreren Strings in der Query
    identifier.initIdentifying(query);
    return identifier.getResultList();

  }

  public void getSparqlHits() {
    // HitRecordContainer hits = sparqlAdapter.getHitRecordContainer();
  }

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
