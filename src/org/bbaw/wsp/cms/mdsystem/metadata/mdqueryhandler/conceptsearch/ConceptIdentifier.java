package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.client.utils.URIUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.general.Constants;
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

	/**
	 * 
	 * @param query
	 * @param methode
	 */
	public void initIdentifying(String query, int methode) {
		Logger logger = Logger.getLogger(ConceptIdentifier.class);
		logger.info("initIdentifying");
		String path = "";

		MdSystemConfigReader confReader = MdSystemConfigReader.getInstance();
		String normdataFile = confReader.getNormDataFilePath();

		logger.info("urlToConfFile : " + normdataFile);
		this.results = new ArrayList<ConceptQueryResult>();
    URL queryAsUrl = null;
    if (query != null && query.toLowerCase().startsWith("http")) {
      try {
        queryAsUrl = new URL(query);
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
      this.results = scanForElement(Constants.getInstance().getMdsystemNormdataFile(), queryAsUrl, methode);
    } else {
      this.results = scanForElement(Constants.getInstance().getMdsystemNormdataFile(), query, methode);
    }
    /*
		for (ConceptQueryResult target : this.results) {
			System.out.println(target);
		}
		*/
	}

	@SuppressWarnings("unused")
	private String format(final String element, final String type) {
		String[] elementArray = element.split("[#]+");
		String[] typeArray = type.split("[/]+");
		return elementArray[elementArray.length - 1] + " - "
				+ typeArray[typeArray.length - 1];
	}

	/**
	 * get a source file and a search string, returns a list of @MdQueryResult
	 * which contains the string
	 * 
	 * @param file
	 * @param element
	 * @return
	 */
	private ArrayList<ConceptQueryResult> scanForElement(final String file, final String element, int methode) {
		Logger logger = Logger.getLogger(ConceptIdentifier.class);
		try {
			RdfMetadataExtractor extractor = MetadataExtractorFactory.newRdfMetadataParser(file);
			extractor.searchElements(element, methode);
			ArrayList<ConceptQueryResult> resultList = extractor.getResultList();

			return resultList;
		} catch (ApplicationException e) {
			logger.info("Couldn't identify document: " + file + " - "
					+ e.getMessage());
			return null;
		}
	}

	/**
   * get a source file and a search string, returns a list of @MdQueryResult
   * which contains the string
   * 
   * @param file
   * @param element
   * @return
   */
  private ArrayList<ConceptQueryResult> scanForElement(final String file, final URL element, int methode) {
    Logger logger = Logger.getLogger(ConceptIdentifier.class);
    try {
      RdfMetadataExtractor extractor = MetadataExtractorFactory.newRdfMetadataParser(file);
//      extractor.searchElements(element, methode);
      ArrayList<ConceptQueryResult> resultList = extractor.getResultList();

      return resultList;
    } catch (ApplicationException e) {
      logger.info("Couldn't identify document: " + file + " - "
          + e.getMessage());
      return null;
    }
  }
	
	public ArrayList<ConceptQueryResult> getResultList() {
		// easier QueryLibary.getInstance().getAllElements();
		return this.results;
	}

	/**
	 * offene fragen: was sind die wichtigsten konzepte? dazu gehören: person,
	 * vorhaben, ort . weitere? evtl nur suche in wsp.normdata.rdf
	 * 
	 * 
	 * case: identifiziere einen string als person
	 * 
	 * Strategie: eine person ist, wer einen "familyName" oder als "description"
	 * oder "functionOrRole" "Wissenschaftliche(r) Mitarbeiter(in)",
	 * "Arbeitsstellenleiter" etc hat suche also alle subjekte mit pf:textmatch in
	 * dem der suchterm vorkommt und als prädikat foaf:familyName hat falls es
	 * doppelungen gibt, entscheide nach häufigkeit, welches konzeot
	 * wahrschienlich gesucht wird <rdf:type
	 * rdf:resource="http://xmlns.com/foaf/0.1/Person"/> alle Personen haben das
	 * /Person tag
	 * 
	 * 
	 * ------------------------ case: identifiziere einen string als
	 * vorhaben/projekt
	 * 
	 * Strategie: ein vorhaben ist, was eine "description" mit dem inhalt
	 * "Vorhaben", "Arbeitsgruppe", "Drittmittelprojekt", etc hat suche also alle
	 * subjekte mit pf:textmatch in dem der suchterm vorkommt und als prädikat
	 * description und den entsprechenden inhalt hat falls es doppelungen gibt,
	 * entscheide nach häufigkeit, welches konzeot wahrschienlich gesucht wird
	 * <rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Project"/>
	 * 
	 * ------------------------ case: identifiziere einen string als ort
	 * 
	 * Strategie: ein ort ist, was als prädikat "type" und als objekt
	 * "dc:location" hat vorgehen s.o. <rdf:type
	 * rdf:resource="http://purl.org/dc/terms/Location"/>
	 * 
	 * -----------------------------
	 * 
	 * case: identifiziere einen string als
	 * "alles andere als person, vorhaben oder ort"
	 * 
	 * Strategie:
	 * 
	 * 
	 */

}
