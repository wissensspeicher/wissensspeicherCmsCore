package org.bbaw.wsp.cms.mdsystem.metadata.general.extractor;

import java.util.ArrayList;
import java.util.HashMap;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.ConceptIdentfierSearchMode;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.ConceptQueryResult;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This class is able to parse a rdf file that is used as input for the triple
 * store.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 25.10.2012
 * 
 *       change: 08.11.2012 - added getXmlBaseValue()-method to extract the Last
 *       change: 07.01.2013 - new Methode added for scanning Strings in
 *       Documents.
 * 
 *       first matching xml base attribut (which contains the real URL)
 */
public class RdfMetadataExtractor extends MetadataExtractor {

	ArrayList<ConceptQueryResult> targetList;

	/**
	 * Create a new ModsMetadataParser instance.
	 * 
	 * @param uri
	 *            - the URI to the knowledge store metadata record.
	 * @throws ApplicationException
	 *             if the resource to be parsed is not validated by Saxon.
	 * @throws IllegalArgumentException
	 *             if the uri is null, empty or doesn't refer to an existing
	 *             file.
	 */
	public RdfMetadataExtractor(final String uri,
			final HashMap<String, String> namespaces)
			throws ApplicationException {
		super(uri, namespaces);
		targetList = new ArrayList<ConceptQueryResult>();
	}

	/**
	 * Build path to the rdf:about attribute. It contains the identifier of the
	 * described resource.
	 * 
	 * @return {@link String} the attribute's value (the uri of the described
	 *         resource)
	 * @throws ApplicationException
	 *             if the rdf:about isn't tagged
	 */
	public String getRdfAboutValue() throws ApplicationException {
		String erg = (String) buildXPath("//rdf:Description[1]/@rdf:about",
				false); // First
		if (erg.equals("")) {
			throw new ApplicationException(
					"No attribute rdf:about found. This is required for the authentification of the document!");
		} // node
		return erg;
	}

	/**
	 * Allows to search in the given document. Methode creates QueryTargets -
	 * Elements which contains the given String somewhere
	 * 
	 * @param element
	 * @throws ApplicationException
	 */
	public void searchElements(String element, int strategie)
			throws ApplicationException {

		String[] elements = element.toLowerCase().split("[ ]+");

		String number = (String) buildXPath("count(//rdf:Description)", false);

		int count = Integer.parseInt(number);
		for (int i = 1; i <= count; ++i) {

			String temp = (String) buildXPath("//rdf:Description[" + i + "]/*",
					false);

			if (checkIfContains(elements, temp.toLowerCase(), strategie)) {
				ConceptQueryResult target = new ConceptQueryResult();
				targetList.add(target);

				String query = "//rdf:Description[" + i + "]/@rdf:about";
				addToTarget("Description", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:type/@rdf:resource";
				addToTarget("type", queryExecute(query), target);

				// vllt nicht wichtig
				query = "//rdf:Description[" + i + "]/*:imports/@rdf:resource";
				addToTarget("imports", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:isPartOf/@rdf:resource";
				addToTarget("isPartOf", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:description";
				addToTarget("description", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:label";
				addToTarget("label", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:languageCode";
				addToTarget("languageCode", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:familyName";
				addToTarget("familyName", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:mbox";
				addToTarget("mbox", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:givenName";
				addToTarget("givenName", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:title";
				addToTarget("title", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:functionOrRole";
				addToTarget("functionOrRole", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:definition";
				addToTarget("definition", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:status";
				addToTarget("status", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:coverage/@rdf:resource";
				addToTarget("coverage", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:topic";
				addToTarget("topic", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:homepage/@rdf:resource";
				addToTarget("homepage", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:valid";
				addToTarget("valid", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:name";
				addToTarget("name", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/*:contributor/@rdf:resource";
				addToTarget("contributor", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:nick";
				addToTarget("nick", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/*:isReplacedBy/@rdf:resource";
				addToTarget("isReplacedBy", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:gndIdentifier";
				addToTarget("gndIdentifier", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/*:relatedCorporateBody/@rdf:resource";
				addToTarget("relatedCorporateBody", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/*:contributingCorporateBody/@rdf:resource";
				addToTarget("contributingCorporateBody", queryExecute(query),
						target);

				query = "//rdf:Description[" + i + "]/*:temporal";
				addToTarget("temporal", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:requires/@rdf:resource";
				addToTarget("requires", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/*:fundedBy/@rdf:resource";
				addToTarget("fundedBy", queryExecute(query), target);
			}
		}

	}

	/**
	 * Checks an Description of given Search-Strings. returns true if found
	 * 
	 * @param elements
	 * @param temp
	 * @param strategie
	 * @return
	 */
	private Boolean checkIfContains(String[] elements, String temp,
			int strategie) {

		switch (elements.length) {
		case 0:
			throw new IllegalArgumentException("Not a vaild search string.");
		case 1:
			if (temp.contains(elements[0]))
				return true;
			break;
		case 2:
			if (strategie == ConceptIdentfierSearchMode.METHODE_AND) {
				if (temp.contains(elements[0]) && temp.contains(elements[1]))
					return true;

			} else if (temp.contains(elements[0]) || temp.contains(elements[1]))
				return true;
			break;

		default:
			if (strategie == ConceptIdentfierSearchMode.METHODE_AND) {
				if (temp.contains(elements[0]) && temp.contains(elements[1])
						&& temp.contains(elements[2]))
					return true;
			} else if (temp.contains(elements[0]) || temp.contains(elements[1])
					|| temp.contains(elements[2]))
				return true;
			break;

		}
		return false;
	}

	/**
	 * is used to add given elements to given Querytarget instance, also checks
	 * for empty Queryresults
	 * 
	 * @param key
	 * @param value
	 * @param target
	 */
	private void addToTarget(String key, Object value, ConceptQueryResult target) {
		if (value instanceof String[]) {
			String[] temp = (String[]) value;
			for (String s : temp) {
				if (s != null && !s.equals("")) {
					target.addToMap(key, s);
				}
			}
		} else {
			if (value instanceof String) {
				String temp = (String) value;
				if (value != null && !value.equals(""))
					target.addToMap(key, temp);
			}
		}
	}

	public ArrayList<ConceptQueryResult> getResultList() {
		return this.targetList;
	}

	/**
	 * Executes the given command in xslt returns the result as String
	 * 
	 * @param query
	 * @return
	 */
	private Object queryExecute(String query) {
		return buildNormdataXPath(query);
	}

	/**
	 * Build path to the xml:base attribute. The first matching attribute will
	 * be returned.
	 * 
	 * @return {@link String} the xml:base attribute's value (the uri of the
	 *         described resource)
	 * @throws ApplicationException
	 *             if the xml:base isn't tagged
	 */
	public String getXmlBaseValue() throws ApplicationException {
		String erg = (String) buildXPath("//*/@xml:base", false);
		if (erg.equals("")) {
			throw new ApplicationException(
					"No attribute xml:base found. This is required for the authentification of the document!");
		}
		return erg;
	}

}
