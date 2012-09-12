package org.bbaw.wsp.cms.dochandler.parser.text.parser;

import org.apache.tika.parser.microsoft.OfficeParser;

/**
 * This class parses a DOC file. It uses the Singleton pattern. Only one
 * instance can exist.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 08.08.2012
 * 
 */
public class DocParserImpl extends ResourceParser {
  private static DocParserImpl instance;

  /**
   * Return the only existing instance. The instance uses an Apache TIKA Doc
   * parser.
   * 
   * @return
   */
  public static DocParserImpl getInstance() {
    if (instance == null) {
      return new DocParserImpl();
    }
    return instance;
  }

  private DocParserImpl() {
    super(new OfficeParser());
  }

}
