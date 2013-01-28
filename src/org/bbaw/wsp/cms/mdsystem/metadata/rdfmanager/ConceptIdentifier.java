package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.util.ArrayList;

/**
 * 
 * This class identifies a concept (URI) in the metadata stored in Jena TDB
 * 
 * @author marco juergens
 * 
 */
public class ConceptIdentifier {

	public static void main(String[] args) {
		final String path = new String(
				"/home/shk2/src/export_normdata_130128.rdf");
		RdfHandler handler = new RdfHandler();
		// ConceptIdentifier identifier = new ConceptIdentifier();
		ArrayList<String> result = handler.scanForElement(path, "Humboldt");
		// ArrayList<String> formatedResults = new ArrayList<String>();
		// for (int i = 0; i < result.size(); ++i) {
		//
		// formatedResults.add(identifier.format(result.get(i),
		// result.get(++i)));
		//
		// }

		for (String string : result) {
			System.out.println(string);
		}

	}

	private String format(final String element, final String type) {
		String[] elementArray = element.split("[#]+");
		String[] typeArray = type.split("[/]+");

		return elementArray[elementArray.length - 1] + " - "
				+ typeArray[typeArray.length - 1];
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
