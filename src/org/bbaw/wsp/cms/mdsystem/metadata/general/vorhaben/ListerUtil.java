/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben;

import java.io.File;

import org.bbaw.wsp.cms.dochandler.parser.document.IDocument;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.DocumentParser;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 04.04.2013
 * 
 */
public class ListerUtil {

  /**
   * Start parsing for any kind of file.
   * 
   * @return the String containing the fulltext or an empty String.
   */
  public static String parseText(final File f) {
    final DocumentParser tikaParser = new DocumentParser();
    IDocument tikaDoc;
    try {
      tikaDoc = tikaParser.parse(f.toURI().toString());
      final String docTokensOrig = tikaDoc.getTextOrig();
      return docTokensOrig;
    } catch (final ApplicationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return "";
    }
  }

}
