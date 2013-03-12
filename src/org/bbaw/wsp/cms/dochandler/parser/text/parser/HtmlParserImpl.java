package org.bbaw.wsp.cms.dochandler.parser.text.parser;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.html.HtmlParser;
import org.bbaw.wsp.cms.document.MetadataRecord;

/**
 * This class parses an HTML file. It uses the Singleton pattern. Only one
 * instance can exist.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 08.08.2012
 * 
 */
public class HtmlParserImpl extends ResourceParser {
  private static HtmlParserImpl instance;

  /**
   * Return the only existing instance. The instance uses an Apache TIKA HTML
   * parser.
   * 
   * @return
   */
  public static HtmlParserImpl getInstance() {
    if (instance == null) {
      return new HtmlParserImpl();
    }
    return instance;
  }

  protected HtmlParserImpl() {
    super(new HtmlParser());
  }

  @Override
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.dochandler.parser.text.parser.ResourceParser#matchMetadata
   * (org.apache.tika.metadata.Metadata,
   * org.bbaw.wsp.cms.document.MetadataRecord)
   */
  protected void matchMetadata(final Metadata metadata, final MetadataRecord mdRecord) {
    super.matchMetadata(metadata, mdRecord);
    MetadataMapper.matchDublinCore(metadata, mdRecord);
  }
}
