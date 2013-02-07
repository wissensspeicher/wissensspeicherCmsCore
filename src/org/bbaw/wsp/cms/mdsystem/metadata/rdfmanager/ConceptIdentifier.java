package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.util.ArrayList;

import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.RdfMetadataExtractor;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.factory.MetadataExtractorFactory;
import org.bbaw.wsp.cms.mdsystem.util.MdystemConfigReader;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * 
 * This class identifies a concept (URI) in the metadata stored in Jena TDB
 * 
 * @author marco juergens
 * 
 */
public class ConceptIdentifier {

	// Datengrundlage: wsp.normdata
	final String path = new String(MdystemConfigReader.getInstance()
			.getConfig().getNormdataPath());
	ArrayList<QueryTarget> result;

	public void initIdentifying(String query, int methode) {
		// ConceptIdentifier identifier = new ConceptIdentifier();
		this.result = new ArrayList<QueryTarget>();
		this.result = scanForElement(path, query, methode);

		for (QueryTarget target : this.result) {
			System.out.println(target);
		}

	}

	@SuppressWarnings("unused")
	private String format(final String element, final String type) {
		String[] elementArray = element.split("[#]+");
		String[] typeArray = type.split("[/]+");

		return elementArray[elementArray.length - 1] + " - "
				+ typeArray[typeArray.length - 1];
	}

	/**
	 * get a source file and a search string, returns a list of @QueryTarget
	 * which contains the string
	 * 
	 * @param file
	 * @param element
	 * @return
	 */
	private ArrayList<QueryTarget> scanForElement(final String file,
			final String element, int methode) {
		try {
			RdfMetadataExtractor fac = MetadataExtractorFactory
					.newRdfMetadataParser(file);
			fac.getElements(element, methode);
			ArrayList<QueryTarget> resultList = QueryLibary.getInstance()
					.getAllElements();

			// System.out.println("Identified document: " + identifier);
			return resultList;
		} catch (ApplicationException e) {
			System.out.println("Couldn't identify document: " + file + " - "
					+ e.getMessage());
			return null;
		}
	}

	public ArrayList<QueryTarget> getResultList() {
		// easier QueryLibary.getInstance().getAllElements();
		return this.result;
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
	 * "Arbeitsstellenleiter" etc hat suche also alle subjekte mit pf:textmatch
	 * in dem der suchterm vorkommt und als prädikat foaf:familyName hat falls
	 * es doppelungen gibt, entscheide nach häufigkeit, welches konzeot
	 * wahrschienlich gesucht wird <rdf:type
	 * rdf:resource="http://xmlns.com/foaf/0.1/Person"/> alle Personen haben das
	 * /Person tag
	 * 
	 * 
	 * ------------------------ case: identifiziere einen string als
	 * vorhaben/projekt
	 * 
	 * Strategie: ein vorhaben ist, was eine "description" mit dem inhalt
	 * "Vorhaben", "Arbeitsgruppe", "Drittmittelprojekt", etc hat suche also
	 * alle subjekte mit pf:textmatch in dem der suchterm vorkommt und als
	 * prädikat description und den entsprechenden inhalt hat falls es
	 * doppelungen gibt, entscheide nach häufigkeit, welches konzeot
	 * wahrschienlich gesucht wird <rdf:type
	 * rdf:resource="http://xmlns.com/foaf/0.1/Project"/>
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
