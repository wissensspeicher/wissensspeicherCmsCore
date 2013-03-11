package org.bbaw.wsp.cms.dochandler.parser.text.parser;

import java.util.Date;

import org.apache.tika.metadata.Metadata;
import org.bbaw.wsp.cms.document.MetadataRecord;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * 
 * This tool class offers static methods to match metadata from the
 * {@link Metadata} objects in TIKA to our {@link MetadataRecord} class.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 11.03.2013
 * 
 */

public class MetadataMatcher {

  private static final String DC_DESCRIPTION = "dc.description";
  private static final String DC_PUBLISHER = "dc.publisher";
  private static final String DC_LANGUAGE = "dc.language";
  private static final String DC_RIGHTS = "dc.rights";
  private static final String DC_IDENTIFIER = "dc.identifier";
  private static final String DC_DATE = "dc.date";
  private static final String DC_DATE_CREATED = "dc.date.created";
  private static final String DC_TITLE = "dc.title";
  private static final String DC_CREATOR = "dc.creator";
  private static final String DC_SUBJECT = "dc.subject";
  private static final String DC_MODIFIED = "dc.date.modified";

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
    final String type = tikaMetadata.get(Metadata.CONTENT_TYPE);
    if (type != null) {
      mdRecord.setType(type);
    }
    @SuppressWarnings("deprecation")
    final String title = tikaMetadata.get(Metadata.TITLE);
    if (title != null) {
      mdRecord.setTitle(title);
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
      case DC_DESCRIPTION:
        mdRecord.setDescription(value);
        break;
      case DC_PUBLISHER:
        mdRecord.setPublisher(value);
        break;
      case DC_LANGUAGE:
        mapLanguage(value, mdRecord);
        break;
      case DC_RIGHTS:
        mdRecord.setRights(value);
        break;
      case DC_IDENTIFIER:
        mdRecord.setIdentifier(value);
        break;
      case DC_DATE:
        final Date insert = mapDate(value);
        if (insert != null) {
          mdRecord.setDate(insert);
        }
        break;
      case DC_DATE_CREATED:
        final Date insertCreated = mapDate(value);
        if (insertCreated != null) {
          mdRecord.setCreationDate(insertCreated);
        }
        break;
      case DC_TITLE:
        mdRecord.setTitle(value);
        break;
      case DC_CREATOR:
        String creatorString = mdRecord.getCreator();
        if (creatorString != null && !creatorString.isEmpty()) {
          creatorString += ("; " + value);
        } else {
          creatorString = value;
        }
        mdRecord.setCreator(creatorString);
        break;
      case DC_SUBJECT:
        mdRecord.setSubject(value);
        break;
      case DC_MODIFIED:
        final Date modified = mapDate(value);
        if (modified != null) {
          mdRecord.setDate(modified);
        }
        break;
      default:
        System.err.println("unmatched field for " + field);
        break;
      }
    }
    System.out.println("\n\n");
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
      ;
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
