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

  public String getParentOfFoaf(final String literal) {
    final String query = "//foaf:name[text()='" + literal + "']/../@rdf:about";
    System.out.println("wspNormdataIntegrator - query: " + query);
    final String parentAbout = (String) super.buildXPath(query, false);
    System.out.println("wspNormdataIntegrator: parentAbout " + parentAbout);
    return parentAbout;
  }
}
