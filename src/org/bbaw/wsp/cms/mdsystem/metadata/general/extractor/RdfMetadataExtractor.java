package org.bbaw.wsp.cms.mdsystem.metadata.general.extractor;

import java.util.HashMap;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.QueryTarget;

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
	public void getElements(String element) throws ApplicationException {

		String number = (String) buildXPath("count(//rdf:Description)", false);

		int count = Integer.parseInt(number);
		for (int i = 1; i <= count; ++i) {

			String temp = queryExecute("//rdf:Description[" + i + "]/*");

			if (temp.contains(element) || temp.toLowerCase().contains(element)) {
				QueryTarget target = new QueryTarget();

				String query = "//rdf:Description[" + i + "]/@rdf:about";
				addToTarget("Description", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/owl:versionInfo/@rdf:datatype";
				addToTarget("versionInfo", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/rdf:type/@rdf:resource";
				addToTarget("type", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/owl:imports/@rdf:resource";
				addToTarget("imports", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/dcterms:isPartOf/@rdf:resource";
				addToTarget("isPartOf", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/dc:description/@rdf:datatype";
				addToTarget("description", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/rdfs:label/@rdf:datatype";
				addToTarget("label", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/gnd:languageCode/@rdf:datatype";
				addToTarget("languageCode", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/foaf:familyName/@rdf:datatype";
				addToTarget("familyName", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/foaf:mbox/@rdf:datatype";
				addToTarget("mbox", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/foaf:givenName/@rdf:datatype";
				addToTarget("givenName", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/foaf:title/@rdf:datatype";
				addToTarget("title", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/@gnd:functionOrRole";
				addToTarget("functionOrRole", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/@gnd:definition";
				addToTarget("definition", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/foaf:status/@rdf:datatype";
				addToTarget("status", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/dc:coverage/@rdf:resource";
				addToTarget("coverage", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/gnd:topic/@rdf:datatype";
				addToTarget("topic", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/foaf:homepage/@rdf:resource";
				addToTarget("homepage", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/dcterms:valid/@rdf:datatype";
				addToTarget("valid", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/foaf:name/@rdf:datatype";
				addToTarget("name", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/dc:contributor/@rdf:resource";
				addToTarget("contributor", queryExecute(query), target);

				query = "//rdf:Description[" + i + "]/foaf:nick/@rdf:datatype";
				addToTarget("nick", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/dcterms:isReplacedBy/@rdf:resource";
				addToTarget("isReplacedBy", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/gnd:gndIdentifier/@rdf:datatype";
				addToTarget("gndIdentifier", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/gnd:relatedCorporateBody/@rdf:resource";
				addToTarget("relatedCorporateBody", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/gnd:contributingCorporateBody/@rdf:resource";
				addToTarget("contributingCorporateBody", queryExecute(query),
						target);

				query = "//rdf:Description[" + i
						+ "]/dcterms:temporal/@rdf:datatype";
				addToTarget("temporal", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/dcterms:requires/@rdf:resource";
				addToTarget("requires", queryExecute(query), target);

				query = "//rdf:Description[" + i
						+ "]/foaf:fundedBy/@rdf:resource";
				addToTarget("fundedBy", queryExecute(query), target);
			}
		}

	}

	/**
	 * is used to add given elements to given Querytarget instance, also checks
	 * for empty Queryresults
	 * 
	 * @param key
	 * @param query
	 * @param target
	 */
	private void addToTarget(String key, String query, QueryTarget target) {
		if (query != null && !query.equals(""))
			target.addToMap(key, query);
	}

	/**
	 * Executes the given command in xslt returns the result as String
	 * 
	 * @param query
	 * @return
	 */
	private String queryExecute(String query) {
		return (String) buildXPath(query, false);
	}

	@SuppressWarnings("unused")
	private Boolean checkIfUsefull(int i, String element) {

		String temp = "";
		temp += queryExecute("//rdf:Description[" + i + "]/@rdf:about");

		temp += queryExecute("//rdf:Description[" + i
				+ "]/owl:versionInfo/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/rdf:type/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/owl:imports/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/dcterms:isPartOf/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/dc:description/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/rdfs:label/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/gnd:languageCode/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:familyName/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:mbox/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:mbox/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:givenName/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:title/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i + "]/@gnd:functionOrRole");
		temp += queryExecute("//rdf:Description[" + i + "]/@gnd:definition");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:status/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/dc:coverage/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/gnd:topic/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:homepage/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/dcterms:valid/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:name/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/dc:contributor/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:nick/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/dcterms:isReplacedBy/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/gnd:gndIdentifier/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/gnd:relatedCorporateBody/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/gnd:contributingCorporateBody/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/dcterms:temporal/@rdf:datatype");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/dcterms:requires/@rdf:resource");
		temp += queryExecute("//rdf:Description[" + i
				+ "]/foaf:fundedBy/@rdf:resource");

		return temp.contains(element);
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
