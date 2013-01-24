package org.bbaw.wsp.cms.dochandler.parser.text.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbaw.wsp.cms.dochandler.parser.text.reader.IResourceReader;
import org.bbaw.wsp.cms.dochandler.parser.text.reader.ResourceReaderImpl;
import org.bbaw.wsp.cms.document.MetadataRecord;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This tool class provides methods to fetch DC fields into a given
 * {@link MetadataRecord}. Last change: - Added fields documentType, isbn,
 * creationDate, publishingDate - 06.09.12: throws {@link ApplicationException}
 * now - Added methods to check if a file is an eDoc index.html file
 * 
 * 24.01.2013: closed scanner
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * 
 */
public class EdocIndexMetadataFetcherTool {
  private static IResourceReader reader = new ResourceReaderImpl();

  /**
   * This class reads from an URL and fetches the DC tags directly (with String
   * operations).
   * 
   * It's designed for the eDoc server.
   * 
   * @param srcUrl
   *          - the basic Url as String
   * @param mdRecord
   *          - the {@link MetadataRecord} to fill
   * 
   *          Last change: bugfixed the publishingDate 06.06.2012 several
   *          changes
   * 
   * @return the complete {@link MetadataRecord}
   * @throws ApplicationException
   */
  public static MetadataRecord fetchHtmlDirectly(final String srcUrl, final MetadataRecord mdRecord) throws ApplicationException {
    InputStream in;
    try {
      in = reader.read(srcUrl);
      final Scanner scanner = new Scanner(in);
      scanner.useDelimiter("\n"); // delimiter via line break
      final StringBuilder builder = new StringBuilder();
      while (scanner.hasNext()) {
        builder.append(scanner.next()); // concat to one String
      }
      scanner.close();
      final String line = builder.toString();
      final StringBuilder creatorBuilder = new StringBuilder(); // fix: more than one
      // creator
      final Pattern p = Pattern.compile("(?i)<META NAME=\"(.*?)\" CONTENT=\"(.*?)\">(?i)"); // meta
      // pattern
      for (final Matcher m = p.matcher(line); m.find();) {
        final String tag = m.group(1);
        String content = m.group(2);

        // set content to null if no results
        if (content.trim().equals("")) {
          content = null;
        }

        if (tag.equals("DC.Date.Creation_of_intellectual_content")) { // creation
          // date
          final Calendar cal = new GregorianCalendar();
          cal.set(Calendar.YEAR, Integer.parseInt(content));
          cal.set(Calendar.DAY_OF_YEAR, 1);
          cal.set(Calendar.HOUR, 0);
          cal.set(Calendar.MINUTE, 0);
          cal.set(Calendar.SECOND, 0);
          cal.set(Calendar.MILLISECOND, 0);
          mdRecord.setCreationDate(cal.getTime());
        } else if (tag.equals("DC.Title")) {
          mdRecord.setTitle(content);
        } else if (tag.equals("DC.Creator")) {
          if (creatorBuilder.toString().length() == 0) {
            creatorBuilder.append(content);
          } else { // more creators are separated by ';'
            creatorBuilder.append(" ; " + content);
          }
          mdRecord.setCreator(creatorBuilder.toString());
        } else if (tag.equals("DC.Subject")) {
          mdRecord.setSubject(content); // DC.Subject follows the
          // Schlagwortnormdatei
        } else if (tag.equals("DC.Description")) {
          mdRecord.setDescription(content);
        } else if (tag.equals("DC.Identifier")) {
          if (content.contains("http://")) {
            mdRecord.setUri(content);
          } else if (content.contains("urn:")) {
            mdRecord.setUrn(content);
          }
        }
      }

      // hard-code "deutsche schlagwörter" to SWD
      final Pattern pGermanSubjects = Pattern.compile("(?i)<TD class=\"frontdoor\" valign=\"top\".*?>(.*?)</TD>");
      boolean matchNext = false;
      for (final Matcher m = pGermanSubjects.matcher(line); m.find();) {
        if (matchNext) {
          mdRecord.setSwd(m.group(1));
          break;
        } else if (m.group(1).contains("SWD-Schlagwörter")) { // higher priority
          // than Freie
          // Schlagwörter...
          matchNext = true;
        } else if (m.group(1).contains("Freie Schlagwörter (Deutsch")) {
          matchNext = true;
        }
      }

      final Pattern p2 = Pattern.compile("(?i)<TD class=\"frontdoor\" valign=\"top\"><B>(.*?)</B></TD>.*?<TD class=\"frontdoor\" valign=\"top\">(.*?)</TD><");
      for (final Matcher m = p2.matcher(line); m.find();) {
        final String key = m.group(1);
        String value = m.group(2).trim();

        // set content to null if no results
        if (value.trim().equals("")) {
          value = null;
        }
        if (key.contains("pdf-Format")) {
          final Pattern pLink = Pattern.compile("(?i)<a href=\"(.*?)(\".*?)\">.*?</a>");
          final Matcher mLink = pLink.matcher(key);
          mLink.find();
          mdRecord.setRealDocUrl(mLink.group(1));
        } else if (key.contains("SWD-Schlagwörter")) { // only german subjects
          mdRecord.setSubject(value);
        } else if (key.contains("DDC-Sachgruppe")) {
          mdRecord.setDdc(value);
        } else if (key.contains("Sprache")) {
          mdRecord.setLanguage(value);
        } else if (key.contains("Dokumentart")) {
          mdRecord.setDocumentType(value);
        } else if (key.contains("Publikationsdatum")) {
          final int day = Integer.parseInt(value.substring(0, value.indexOf(".")));
          final int month = Integer.parseInt(value.substring(value.indexOf(".") + 1, value.lastIndexOf(".")));
          final int year = Integer.parseInt(value.substring(value.lastIndexOf(".") + 1));

          final Calendar cal = new GregorianCalendar();
          cal.set(year, month - 1, day); // bugfixed: month is 0 based!
          mdRecord.setPublishingDate(cal.getTime());
        } else if (key.contains("ISBN")) {
          mdRecord.setIsbn(value);
        } else if (key.contains("Institut")) {
          mdRecord.setPublisher(value);
        } else if (key.contains("Collection")) {
          final Pattern pColl = Pattern.compile("(?i)<a.*?>(.*?)</a>");
          final Matcher mColl = pColl.matcher(value);
          mColl.find();
          final String collections = mColl.group(1);

          mdRecord.setCollectionNames(collections);
        } else if (key.contains("Kurzfassung auf Deutsch") && value != null && mdRecord.getDescription() != null) {
          mdRecord.setDescription(value);
        }
      }
      // Bugfix: Institut
      final Pattern p3 = Pattern.compile("(?i)<TD class=\"frontdoor\" valign=\"top\"><B>Institut:</B></TD>.*?<TD class=\"frontdoor\" valign=\"top\">(.*?)</TD><");
      for (final Matcher m = p3.matcher(line); m.find();) {
        mdRecord.setPublisher(m.group(1));
      }

      in.close();
      return mdRecord;
    } catch (final IOException e) {
      throw new ApplicationException("Problem while parsing " + srcUrl + " for DC tags " + e.getMessage());
    }

  }

  /**
   * Check if the file is an index.html file to an eDoc.
   * 
   * LastChange: Performance optimation - check URI before reading from input
   * stream
   * 
   * @param uri
   *          - the URL as string to the index.html file.
   * @return true if the index.html file belongs to an eDoc.
   * @throws ApplicationException
   *           if the stream couldn't get opened.
   */
  public static boolean isEDocIndex(final String uri) throws ApplicationException {
    if ((uri.contains("edoc.bbaw.de") || uri.endsWith(".html")) && !uri.endsWith(".pdf")) {
      final InputStream in = reader.read(uri);
      if (in != null) {
        final Scanner scanner = new Scanner(in);
        scanner.useDelimiter("\n");
        final StringBuilder builder = new StringBuilder();
        while (scanner.hasNext()) {
          builder.append(scanner.next());
        }
        scanner.close();
        final String content = builder.toString();
        final Pattern p = Pattern.compile("(?i)<META NAME=\"(.*?)\" CONTENT=\"(.*?)\">(?i)");
        for (final Matcher m = p.matcher(content); m.find();) {
          final String tag = m.group(1);
          final String value = m.group(2);
          if (tag.equals("DC.Identifier") && value.contains("edoc.bbaw.de/")) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Check if the resource is an eDoc.
   * 
   * @param uri
   *          - the resource's URI.
   * @return true if the resource is an (KOBV) eDoc.
   */
  public static boolean isEDoc(final String uri) {
    // test local file system
    final File f = new File(uri);

    if (f.getParentFile().getName().equals("pdf") && new File(f.getParentFile().getParentFile(), "index.html").exists()) {
      return true;
    } else { // test HTTP
      try {
        final URL url = new URL(uri);
        final int pos = url.toExternalForm().lastIndexOf("/pdf");
        if (pos != -1) {
          final String newUrl = url.toExternalForm().substring(0, pos) + "/index.html";

          final URL indexUrl = new URL(newUrl);
          @SuppressWarnings("unused")
          final
          URLConnection conn = indexUrl.openConnection();

          return true;
        }
        return false;
      } catch (final MalformedURLException e) {
        return false;
      } catch (final IOException e) {
        return false;
      }
    }
  }

  /**
   * Fetch the eDoc's id as it's stored on the file system. This id can be used
   * for an OAI/ORE aggregation for example.
   * 
   * @param eDocUrl
   *          {@link String} the URL to the eDoc. This will be parsed for the
   *          id.
   * @return {@link Integer} the docID or -1 if the ID couldn'T be parsed.
   * @throws ApplicationException
   */
  public static int getDocId(final String eDocUrl) throws ApplicationException {
    if (eDocUrl == null || eDocUrl.isEmpty()) {
      throw new ApplicationException("The value for the eDocUrl in getDocId mustn't be null or empty.");
    }
    final Pattern p = Pattern.compile(".*/(.*?)/pdf/.*");
    final Matcher m = p.matcher(eDocUrl);
    if (m.find()) {
      return Integer.parseInt(m.group(1));
    }
    return -1;
  }

  /**
   * Fetch the edoc's year. This is used in the aggregation name for example.
   * 
   * @param realDocUrl
   * @return the year as it's stored in edoc server or null if it couldn't be
   *         parsed.
   * @throws ApplicationException
   */
  public static String getDocYear(final String eDocUrl) throws ApplicationException {
    if (eDocUrl == null || eDocUrl.isEmpty()) {
      throw new ApplicationException("The value for the eDocUrl in getDocYear mustn't be null or empty.");
    }
    final Pattern p = Pattern.compile(".*/(.*?)/(.*?)/pdf/.*");
    final Matcher m = p.matcher(eDocUrl);
    if (m.find()) {
      return m.group(1);
    }
    return null;
  }

}
