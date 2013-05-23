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

  public WspNormdataExtractor(final String uri) throws ApplicationException {
    super(uri, new HashMap<String, String>());
  }
}
