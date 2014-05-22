package org.bbaw.wsp.cms.dochandler.parser.text.parser.mapper;

import java.util.Date;

import org.apache.tika.metadata.Metadata;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.DateMapper;
import org.bbaw.wsp.cms.document.MetadataRecord;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * 
 * This tool class offers static methods to map metadata from the
 * {@link Metadata} objects in TIKA to our {@link MetadataRecord} class.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 11.03.2013
 * 
 */

public class TikaMetadataMapper implements IMetadataFields {
  /**
   * Match general metadata, such as content-type, coding..
   * 
   * @param tikaMetadata
   * @param mdRecord
   */
  public static void matchGeneral(final Metadata tikaMetadata, final MetadataRecord mdRecord) {
    @SuppressWarnings("deprecation")
    final String pageCount = tikaMetadata.get(Metadata.PAGE_COUNT);
    if (pageCount != null) {
      mdRecord.setPageCount(Integer.parseInt(pageCount));
    }

    for (final String field : tikaMetadata.names()) {
      final String value = tikaMetadata.get(field);
      switch (field.toLowerCase()) {
      case IMetadataFields.TITLE:
        mdRecord.setTitle(value);
        break;
      case IMetadataFields.CONTENT_TYPE:
        mdRecord.setType(value);
        break;
      case IMetadataFields.CONTENT_ENCODING:
        mdRecord.setEncoding(value);
        break;
      case IMetadataFields.KEYWORDS:
        mdRecord.setSubject(concatenate(mdRecord.getSubject(), IMetadataFields.MULTIVALUE_SEPARATOR, value));
        break;
      case IMetadataFields.DESCRIPTION:
        mdRecord.setDescription(value);
        break;
      }
    }
  }

  /**
   * Match special Dublin Core (DC) metadata.
   * 
   * @param tikaMetadata
   * @param mdRecord
   */
  public static void matchDublinCore(final Metadata tikaMetadata, final MetadataRecord mdRecord) {
    // Metadaten - Felder
    for (final String field : tikaMetadata.names()) {
      final String value = tikaMetadata.get(field);
      switch (field.toLowerCase()) {
      case IMetadataFields.DC_DESCRIPTION:
        mdRecord.setDescription(value);
        break;
      case IMetadataFields.DC_PUBLISHER:
        mdRecord.setPublisher(value);
        break;
      case IMetadataFields.DC_LANGUAGE:
        mapLanguage(value, mdRecord);
        break;
      case IMetadataFields.DC_RIGHTS:
        mdRecord.setRights(value);
        break;
      case IMetadataFields.DC_IDENTIFIER:
        mdRecord.setIdentifier(value);
        break;
      case IMetadataFields.DC_DATE:
        final Date insert = mapDate(value);
        if (insert != null) {
          mdRecord.setDate(insert);
        }
        break;
      case IMetadataFields.DC_DATE_CREATED:
        final Date insertCreated = mapDate(value);
        if (insertCreated != null) {
          mdRecord.setCreationDate(insertCreated);
        }
        break;
      case IMetadataFields.DC_TITLE:
        mdRecord.setTitle(value);
        break;
      case IMetadataFields.DC_CREATOR:
        mdRecord.setCreator(concatenate(mdRecord.getCreator(), IMetadataFields.MULTIVALUE_SEPARATOR, value));
        break;
      case IMetadataFields.DC_SUBJECT:
        mdRecord.setSubject(concatenate(mdRecord.getSubject(), IMetadataFields.MULTIVALUE_SEPARATOR, value));
        break;
      case IMetadataFields.DC_COVERAGE:
        mdRecord.setCoverage(value);
        break;
      case IMetadataFields.DC_CONTRIBUTOR:
        mdRecord.setContributor(concatenate(mdRecord.getContributor(), IMetadataFields.MULTIVALUE_SEPARATOR, value));
        break;
      case IMetadataFields.DC_MODIFIED:
        final Date modified = mapDate(value);
        if (modified != null) {
          mdRecord.setDate(modified);
        }
        break;
      }
    }
  }

  /**
   * Concatenated two values.
   * 
   * @param oldValue
   *          may be null
   * @param newValue
   *          mustn't be null
   * @return a {@link String}, newValue if oldValue is null or the concatened
   *         String (oldValue [separator] newValue)
   */
  private static String concatenate(final String oldValue, final char seperator, final String newValue) {
    if (oldValue == null || oldValue.trim().isEmpty()) {
      return newValue;
    }
    final StringBuilder builder = new StringBuilder();
    builder.append(oldValue);
    builder.append(seperator);
    builder.append(newValue);
    return builder.toString();
  }

  /**
   * Map a date String to our {@link Date} field.
   * 
   * @param dateString
   *          the given Date string.
   * @param mdRecord
   */
  private static Date mapDate(final String dateString) {
    try {
      return DateMapper.mapDate(dateString);
    } catch (final ApplicationException e) {
      System.err.println(e);
    }
    return null;
  }

  /**
   * Map a language String to our ISO 639 - 3 language term.
   * 
   * @param langString
   *          the given language string.
   * @param mdRecord
   */
  private static void mapLanguage(final String langString, final MetadataRecord mdRecord) {
    switch (langString) {
    case "de":
      mdRecord.setLanguage("deu");
      break;
    default:
      System.err.println("No ISO 639-3 language mapping found for input string: " + langString + " in MetadataMatcher.mapLanguage()!");
    }
  }
}
