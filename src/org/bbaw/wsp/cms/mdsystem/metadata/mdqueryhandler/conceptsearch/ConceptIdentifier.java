package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.ConceptIdentifierPriority;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.RdfMetadataExtractor;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.factory.MetadataExtractorFactory;
import org.bbaw.wsp.cms.mdsystem.util.MdSystemConfigReader;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * 
 * This class identifies a concept (URI) in the metadata stored in Jena TDB
 * 
 * @author marco juergens
 * 
 */
public class ConceptIdentifier {

  // Datengrundlage: wsp.normdata.rdf
  // final String path = new
  // String(MdSystemConfigReader.getInstance().getConfig().getNormdataPath());
  ArrayList<ConceptQueryResult> results;
  private HashMap<ConceptIdentifierPriority, ArrayList<ConceptQueryResult>> sortedResultsMap;

  /**
   * 
   * @param query
   * @param methode
   */
  public void initIdentifying(final String query, final int methode) {
    final Logger logger = Logger.getLogger(ConceptIdentifier.class);
    logger.info("initIdentifying");
    final MdSystemConfigReader confReader = MdSystemConfigReader.getInstance();
    final String normdataFile = confReader.getNormDataFilePath();

    logger.info("urlToConfFile : " + normdataFile);
    results = new ArrayList<ConceptQueryResult>();
    RdfMetadataExtractor extractor;
    URL queryAsUrl = null;
    if (query != null && query.toLowerCase().startsWith("http")) {
      try {
        queryAsUrl = new URL(query);
      } catch (final MalformedURLException e) {
        e.printStackTrace();
      }
      extractor = scanForElement(Constants.getInstance().getMdsystemNormdataFile(), queryAsUrl, methode);
    } else {
      extractor = scanForElement(Constants.getInstance().getMdsystemNormdataFile(), query, methode);
    }
    results = extractor.getResultList();
    sortedResultsMap = extractor.getPriorityTargetMap();

    /*
     * for (ConceptQueryResult target : this.results) { System.out.println(target); }
     */
  }

  @SuppressWarnings("unused")
  private String format(final String element, final String type) {
    final String[] elementArray = element.split("[#]+");
    final String[] typeArray = type.split("[/]+");
    return elementArray[elementArray.length - 1] + " - " + typeArray[typeArray.length - 1];
  }

  /**
   * get a source file and a search string, returns a list of @MdQueryResult which contains the string
   * 
   * @param file
   * @param element
   * @return
   */
  private RdfMetadataExtractor scanForElement(final String file, final String element, final int methode) {
    final Logger logger = Logger.getLogger(ConceptIdentifier.class);
    try {
      final RdfMetadataExtractor extractor = MetadataExtractorFactory.newRdfMetadataParser(file);
      extractor.searchElements(element, methode);
      final ArrayList<ConceptQueryResult> resultList = extractor.getResultList();

      return extractor;
    } catch (final ApplicationException e) {
      logger.info("Couldn't identify document: " + file + " - " + e.getMessage());
      return null;
    }
  }

  /**
   * get a source file and a search string, returns a list of @MdQueryResult which contains the string
   * 
   * @param file
   * @param element
   * @return
   */
  private RdfMetadataExtractor scanForElement(final String file, final URL element, final int methode) {
    final Logger logger = Logger.getLogger(ConceptIdentifier.class);
    try {
      final RdfMetadataExtractor extractor = MetadataExtractorFactory.newRdfMetadataParser(file);
      // extractor.searchElements(element, methode);
      final ArrayList<ConceptQueryResult> resultList = extractor.getResultList();

      return extractor;
    } catch (final ApplicationException e) {
      logger.info("Couldn't identify document: " + file + " - " + e.getMessage());
      return null;
    }
  }

  public List<ConceptQueryResult> getResultList() {
    // easier QueryLibary.getInstance().getAllElements();
    final List<ConceptQueryResult> mergedResultList = new ArrayList<>();
    ConceptIdentifierPriority priority = ConceptIdentifierPriority.HIGH;
    while (priority != ConceptIdentifierPriority.END) {
      if (sortedResultsMap.get(priority) != null) {
        mergedResultList.addAll(sortedResultsMap.get(priority));
      }
      priority = priority.getNextPriority();
    }
    return mergedResultList;
    // return _sortResultList(results);
  }

  /**
   * Sort the resultList by using the priorities in the {@link ConceptIdentifierPriority}.
   * 
   * @TODO: Aufwandsbetrachtung. Für wenige ConceptQueries ist die Sortierung perfomant. Allerdings steigt der Aufwand linear (Anzahl der QueryResults * Anzahl Prioritäten).
   * 
   *        Einfache Lösung: eine zweite HashMap mit Key: Priorität und Value: ArrayList für schnellen Zugriff, die value-lists werden dann einfach hintereinander geaddet.
   * 
   * 
   * @param results
   *          the results returned by the extractor.
   * @return
   * @deprecated replaced by priorities map now.
   */
  @Deprecated
  private ArrayList<ConceptQueryResult> _sortResultList(final ArrayList<ConceptQueryResult> results) {
    final ArrayList<ConceptQueryResult> sortedResults = new ArrayList<>();
    ConceptIdentifierPriority currentPriority = ConceptIdentifierPriority.START.getNextPriority();
    while (currentPriority != ConceptIdentifierPriority.END) {
      _addForGivenPriority(results, sortedResults, currentPriority);
      currentPriority = currentPriority.getNextPriority();
    }
    // add undefined priorites
    _addForGivenPriority(results, sortedResults, ConceptIdentifierPriority.UNDEFINED);
    return sortedResults;
  }

  /**
   * Add all the query results with the given priority to the sortedResults list.
   * 
   * 
   * @param results
   *          the array list from the beginning.
   * @param sortedResults
   *          the sorted list.
   * @param givenPriority
   *          the ConceptIdentifierPriority
   */
  private void _addForGivenPriority(final ArrayList<ConceptQueryResult> results, final ArrayList<ConceptQueryResult> sortedResults, final ConceptIdentifierPriority givenPriority) {
    for (final ConceptQueryResult queryResult : results) {
      if (queryResult.getResultPriority().equals(givenPriority)) {
        sortedResults.add(queryResult);
      }
    }
  }

  /**
   * offene fragen: was sind die wichtigsten konzepte? dazu gehören: person, vorhaben, ort . weitere? evtl nur suche in wsp.normdata.rdf
   * 
   * 
   * case: identifiziere einen string als person
   * 
   * Strategie: eine person ist, wer einen "familyName" oder als "description" oder "functionOrRole" "Wissenschaftliche(r) Mitarbeiter(in)", "Arbeitsstellenleiter" etc hat suche also alle subjekte mit pf:textmatch in dem der suchterm vorkommt und als prädikat foaf:familyName hat falls es doppelungen gibt, entscheide nach häufigkeit, welches konzeot wahrschienlich gesucht wird <rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Person"/> alle Personen haben das /Person tag
   * 
   * 
   * ------------------------ case: identifiziere einen string als vorhaben/projekt
   * 
   * Strategie: ein vorhaben ist, was eine "description" mit dem inhalt "Vorhaben", "Arbeitsgruppe", "Drittmittelprojekt", etc hat suche also alle subjekte mit pf:textmatch in dem der suchterm vorkommt und als prädikat description und den entsprechenden inhalt hat falls es doppelungen gibt, entscheide nach häufigkeit, welches konzeot wahrschienlich gesucht wird <rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Project"/>
   * 
   * ------------------------ case: identifiziere einen string als ort
   * 
   * Strategie: ein ort ist, was als prädikat "type" und als objekt "dc:location" hat vorgehen s.o. <rdf:type rdf:resource="http://purl.org/dc/terms/Location"/>
   * 
   * -----------------------------
   * 
   * case: identifiziere einen string als "alles andere als person, vorhaben oder ort"
   * 
   * Strategie:
   * 
   * @param sortedResults
   * 
   * 
   */

}
