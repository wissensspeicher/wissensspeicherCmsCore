package org.bbaw.wsp.cms.dochandler.parser.text.parser;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbaw.wsp.cms.document.MetadataRecord;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * 
 * This tool class offers static methods to map given (extracted) dates from the
 * {@link Metadata} objects in TIKA to our {@link MetadataRecord} class.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 11.03.2013
 * 
 */

public class DateMapper {

  /**
   * Map a date as String to our {@link MetadataRecord} java nativ {@link Date}.
   * 
   * @param dateString
   *          the extracted Date as String.
   * @param mdRecord
   * @throws ApplicationException
   */
  @SuppressWarnings("finally")
  public static Date mapDate(final String dateString) throws ApplicationException {
    Date insertDate = null;
    try {
      /*
       * see http://www.w3.org/TR/NOTE-datetime for the correct date format
       * YYYY-MM-DDThh:mm
       */
      final Pattern pW3C = Pattern.compile("([0-9]{4,})-([0-9]{2,})-([0-9]{2,})T([0-9]{2,}):([0-9]{2,}):([0-9]{2,})"); // meta
      for (final Matcher m = pW3C.matcher(dateString); m.find();) {
        final int year = new Integer(m.group(1));
        final int month = new Integer(m.group(2));
        final Integer day = new Integer(m.group(3));
        final Integer hour = new Integer(m.group(4));
        final Integer minute = new Integer(m.group(5));
        final Integer second = new Integer(m.group(6));
        final Calendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        insertDate = cal.getTime();
      }
      if (insertDate == null) {
        /*
         * see
         * http://www.bbaw.de/bbaw/Forschung/Forschungsprojekte/ig/de/Ueberblick
         * for example YYYY-MM-DD hh:mm
         */
        final Pattern pDate = Pattern.compile("([0-9]{4,})-([0-9]{2,})-([0-9]{2,})\\s([0-9]{2,}):([0-9]{2,}):([0-9]{2,})"); // meta
        for (final Matcher m = pDate.matcher(dateString); m.find();) {
          final int year = new Integer(m.group(1));
          final int month = new Integer(m.group(2));
          final Integer day = new Integer(m.group(3));
          final Integer hour = new Integer(m.group(4));
          final Integer minute = new Integer(m.group(5));
          final Integer second = new Integer(m.group(6));
          final Calendar cal = new GregorianCalendar();
          cal.clear();
          cal.set(year, month - 1, day, hour, minute, second);
          insertDate = cal.getTime();
        }
      }

      if (insertDate == null) { // java nativ
        insertDate = DateFormat.getDateInstance().parse(dateString);
      }

    } catch (final ParseException e) {

      throw new ApplicationException("Unable to parse the given date String: " + dateString + " in DateMapper.mapDate()!");
    } finally {
      if (insertDate != null) {
        return insertDate;
      }
      return null;
    }
  }
}
