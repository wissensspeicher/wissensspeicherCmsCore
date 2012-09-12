package org.bbaw.wsp.cms.dochandler.parser.text.parser;

import org.apache.tika.parser.odf.OpenDocumentParser;

/**
 * The ODFParser. It uses the Singleton pattern. Only one instance can exist.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 08.08.2012
 * 
 */
public class OdfParserImpl extends ResourceParser {
  private static OdfParserImpl instance;

  /**
   * Return the only existing instance. The instance uses an Apache TIKA
   * OpenDocument parser.
   * 
   * @return
   */
  public static OdfParserImpl getInstance() {
    if (instance == null) {
      return new OdfParserImpl();
    }
    return instance;
  }

  private OdfParserImpl() {
    super(new OpenDocumentParser());
  }
}
