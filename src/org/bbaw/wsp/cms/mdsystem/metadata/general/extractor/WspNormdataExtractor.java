/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.general.extractor;

import java.util.HashMap;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * Normdata extractor for our WSP normdata file.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 23.05.2013
 * 
 */
public class WspNormdataExtractor extends MetadataExtractor {

  public WspNormdataExtractor(final String filePath, final HashMap<String, String> namespaces) throws ApplicationException {
    super(filePath, namespaces);
  }

  /**
   * Get the container's resource of a given foaf literal.
   * 
   * @param literal
   *          the text within the foaf:name node
   * @return the container's URI
   */
  public String getParentOfFoaf(final String literal) {
    final String query = "//foaf:name[text()='" + literal + "']/../@rdf:about";
    System.out.println("wspNormdataIntegrator - query: " + query);
    final String parentAbout = (String) super.buildXPath(query, false);
    System.out.println("wspNormdataIntegrator: parentAbout " + parentAbout);
    return parentAbout;
  }

  /**
   * Get the container's resource of a given person.
   * 
   * @param givenName
   * @param familyName
   * @return
   */
  public String getParentOfGivenFamily(final String givenName, final String familyName) {
    final String queryGiven = "//foaf:givenName[text()='" + givenName + "']/../@rdf:about";
    final String givenParentAbout = (String) super.buildXPath(queryGiven, false);
    System.out.println("wspNormdataIntegrator - given " + givenParentAbout);
    final String queryFamily = "//foaf:familyName[text()='" + familyName + "']/../@rdf:about";
    final String familyParentAbout = (String) super.buildXPath(queryFamily, false);
    System.out.println("wspNormdataIntegrator - family" + familyParentAbout);

    // it's possible that there are more than one containers for a family name (e.g. for the popular familyname "Schneider").
    // so we need to split the container URIs and find the one existing URI that is contained in the matching containers of the givenName.
    final String[] familyNames = familyParentAbout.split(" ");
    for (final String singleFamilyName : familyNames) {
      if (givenParentAbout.contains(singleFamilyName)) {
        return singleFamilyName;
      }
    }
    return null;
  }
}
