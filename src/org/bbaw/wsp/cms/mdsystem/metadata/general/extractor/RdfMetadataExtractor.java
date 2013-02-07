package org.bbaw.wsp.cms.mdsystem.metadata.general.extractor;

import java.util.HashMap;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This class is able to parse a rdf file that is used as input for the triple
 * store.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 25.10.2012
 * 
 *       Last change: 08.11.2012 - added getXmlBaseValue()-method to extract the
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

	public void getElements(String element) throws ApplicationException {

		String number = (String) buildXPath("count(//rdf:Description)", false);

		int count = Integer.parseInt(number);
		System.out.println(count);

		// result.add((String) buildXPath("//rdf:Description[" + (i + 1)
		// + "]/rdf:type/@rdf:resource", false)); // First

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
