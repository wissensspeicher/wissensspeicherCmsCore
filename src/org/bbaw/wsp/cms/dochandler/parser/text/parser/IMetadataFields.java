package org.bbaw.wsp.cms.dochandler.parser.text.parser;

import org.bbaw.wsp.cms.document.MetadataRecord;

/**
 * This interface contains all metadata fields that we map to our
 * {@link MetadataRecord} class.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 13.03.2013
 * 
 */
public interface IMetadataFields {
  /**
   * Type in the desired separator here. It's used if the metadata mapper
   * detects multivalues.
   */
  static final char MULTIVALUE_SEPARATOR = ';';

  static final String DC_DESCRIPTION = "dc.description";
  static final String DC_PUBLISHER = "dc.publisher";
  static final String DC_LANGUAGE = "dc.language";
  static final String DC_RIGHTS = "dc.rights";
  static final String DC_IDENTIFIER = "dc.identifier";
  static final String DC_DATE = "dc.date";
  static final String DC_DATE_CREATED = "dc.date.created";
  static final String DC_TITLE = "dc.title";
  static final String DC_CREATOR = "dc.creator";
  static final String DC_SUBJECT = "dc.subject";
  static final String DC_MODIFIED = "dc.date.modified";
  static final String DC_TITLE_COLON_SEPARATOR = "dc:title";
  static final String DC_COVERAGE = "dc.coverage";
  static final String TITLE = "title";
  static final String DC_CONTRIBUTOR = "dc.contributor";
  static final String CONTENT_TYPE = "content-type";
  static final String CONTENT_ENCODING = "content-encoding";
  static final String KEYWORDS = "keywords";
  /**
   * e.g. <meta name="description" ...>
   */
  static final String DESCRIPTION = "description";

}