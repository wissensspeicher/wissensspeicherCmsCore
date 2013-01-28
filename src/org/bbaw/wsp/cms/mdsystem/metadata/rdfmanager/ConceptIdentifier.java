package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

/**
 * 
 * This class identifies a concept (URI) in the metadata stored in Jena TDB
 * 
 * @author marco juergens
 *
 */
public class ConceptIdentifier {
	
	/**
	 * offene fragen:
	 * was sind die wichtigsten konzepte? dazu gehören: person, vorhaben, ort. weitere?
	 * evtl nur suche in wsp.normdata.rdf
	 * 
	 * 
	 * case:
	 * identifiziere einen string als person
	 * 
	 * Strategie:
	 * eine person ist, wer einen "familyName" oder als "description" oder "functionOrRole" "Wissenschaftliche(r) Mitarbeiter(in)", "Arbeitsstellenleiter" etc hat 
	 * suche also alle subjekte mit pf:textmatch in dem der suchterm vorkommt und als prädikat foaf:familyName hat
	 * falls es doppelungen gibt, entscheide nach häufigkeit, welches konzeot wahrschienlich gesucht wird 
	 * 
	 * ------------------------
	 * case:
	 * identifiziere einen string als vorhaben/projekt
	 * 
	 * Strategie:
	 * ein vorhaben ist, was eine "description" mit dem inhalt "Vorhaben", "Arbeitsgruppe", "Drittmittelprojekt", etc hat
	 * suche also alle subjekte mit pf:textmatch in dem der suchterm vorkommt und als prädikat description und den entsprechenden inhalt hat
	 * falls es doppelungen gibt, entscheide nach häufigkeit, welches konzeot wahrschienlich gesucht wird 
	 * 
	 * ------------------------
	 * case:
	 * identifiziere einen string als ort
	 * 
	 * Strategie:
	 * ein ort ist, was als prädikat "type" und als objekt "dc:location" hat
	 * vorgehen s.o.
	 * 
	 * 
	 * -----------------------------
	 * 
	 * case:
	 * identifiziere einen string als "alles andere als person, vorhaben oder ort"
	 * 
	 * Strategie:
	 * 
	 * 
	 */

}
