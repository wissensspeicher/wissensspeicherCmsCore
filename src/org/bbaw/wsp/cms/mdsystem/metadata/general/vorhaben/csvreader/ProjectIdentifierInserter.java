package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben.csvreader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbaw.wsp.cms.dochandler.parser.document.IDocument;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.DocumentParser;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * 
 * Insert missing identifiers to an existing CSV.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.03.2013
 * 
 */
public class ProjectIdentifierInserter {
  /**
   * Path to Anetts doc file.
   */
  private static final String INPUT_DOC = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/Vorhaben/Vorhabenliste_komplett_fertig.odt";
  private static final String OUTPUTCSV = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/Vorhaben/Vorhaben_converted_identifier.csv";
  private static final String DESCRIPTION_VORHABEN = "vorhaben";

  public static void main(final String[] args) {
    final File csvFile = new File(ProjectReaderFromCsv.INPUT_CSV);
    final List<ParsedProject> projects = ProjectReaderFromCsv.readFromCsv(csvFile);

    parseEach(projects);

    // writeToCsv(projects);

    // Print fetched identifiers
    for (final ParsedProject changedPro : projects) {
      if (changedPro.getDescription() != null) {
        System.out.println("project --> " + changedPro.getVorhaben() + " - " + changedPro.getProject() + " - " + changedPro.getIdentifier() + " - " + changedPro.getDescription());
      }
    }
  }

  private static void writeToCsv(final List<ParsedProject> projects) {
    try {
      final Writer out = new FileWriter(OUTPUTCSV);
      final CSVWriter<ParsedProject> csvWriter = new CSVWriterBuilder<ParsedProject>(out).strategy(CSVStrategy.UK_DEFAULT).entryConverter(new ParsedProjectEntryConverter()).build();
      csvWriter.writeAll(projects);
    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // final CSVWriter<ARecordEntry> csvWriter = new
    // CSVWriterBuilder<ARecordEntry>(out

  }

  /**
   * Parse Anetts Vorhaben Liste for each project.
   * 
   * @param projects
   *          a list of projects which are fetched from an CSV file.
   */
  private static void parseEach(final List<ParsedProject> projects) {
    final File anettsDoc = new File(INPUT_DOC);
    final String fulltext = parseText(anettsDoc);

    for (final ParsedProject project : projects) {
      fetchIdentifier(project, fulltext);
    }
  }

  /**
   * Fetch the identifier from Anetts doc.
   * 
   * @param project
   *          the identifier will be set on the project
   */
  private static void fetchIdentifier(final ParsedProject project, final String fulltext) {

    final int projPos = fulltext.indexOf(project.getProject());
    final int vorPos = fulltext.indexOf(project.getVorhaben());
    if (projPos != -1 && !project.getProject().trim().isEmpty()) {
      System.out.println("proj found" + project.getProject());
      final String hopeIdContains = fulltext.substring(projPos, projPos + 200);

      /*
       * ------ fetch identifier --------
       */
      final Pattern p = Pattern.compile("(?i)identifier:(.*?)\\n");
      for (final Matcher m = p.matcher(hopeIdContains); m.find();) {
        final String identifier = m.group(1);
        project.setIdentifier(identifier);
      }
      /*
       * --------------------------------
       */
      /*
       * ------ fetch description --------
       */
      final Pattern pDescription = Pattern.compile("(?i)description:(.*?)\\n");
      for (final Matcher m = pDescription.matcher(hopeIdContains); m.find();) {
        final String strDescription = m.group(1).trim();
        project.setDescription(strDescription);

        // case: found a project which is a vorhaben
        if (strDescription.toLowerCase().contains(DESCRIPTION_VORHABEN)) {
          if (project.getProject() != null) {
            final String oldProject = project.getProject();
            project.setProject("---");
            project.setVorhaben(oldProject);
          }
        }

        // project.setIdentifier(identifier);
      }
      /*
       * --------------------------------
       */
    } else if (vorPos != -1 && !project.getProject().equals("---")) {
      System.out.println("vor found" + project.getVorhaben());

      final String hopeIdContains = fulltext.substring(projPos, projPos + 200);
      /*
       * ------ fetch identifier --------
       */
      final Pattern p = Pattern.compile("(?i)identifier:(.*?)\\n");
      for (final Matcher m = p.matcher(hopeIdContains); m.find();) {
        final String identifier = m.group(1);
        project.setIdentifier(identifier);
      }
      /*
       * --------------------------------
       */
      /*
       * ------ fetch description --------
       */
      final Pattern pDescription = Pattern.compile("(?i)description:(.*?)\\n");
      for (final Matcher m = pDescription.matcher(hopeIdContains); m.find();) {
        final String strDescription = m.group(1).trim();
        project.setDescription(strDescription);

        // // case: found a vorhaben which actually isn't a vorhaben
        // if (!strDescription.toLowerCase().contains(DESCRIPTION_VORHABEN)) {
        // if (project.getVorhaben() != null) {
        // final String oldVorhaben = project.getVorhaben();
        // project.setVorhaben("---");
        // project.setProject(oldVorhaben);
        // }
        // }
      }
      /*
       * --------------------------------
       */
    }

  }

  // private static void fetchIdentifier(final ParsedProject project, final
  // String fulltext) {
  // final Scanner scanner = new Scanner(fulltext);
  //
  // boolean scanningParent = false;
  // boolean scanningProject = false;
  // boolean scanningIdentifier = false;
  // String actIdentifier = "";
  // while (scanner.hasNext()) {
  // final String actLine = scanner.next();
  // if (!scanningParent && !scanningProject && actLine.startsWith("*")) { //
  // project
  // // detected
  //
  // if (actLine.contains(project.getCreator())) { // parent project
  // System.out.println("----> scanning project");
  // scanningParent = true;
  // } else if (actLine.contains(project.getTitle())) { // project
  // System.out.println("----> scanning project");
  // scanningProject = true;
  // }
  // } else if ((scanningParent || scanningProject || scanningIdentifier) &&
  // !actLine.startsWith("*")) {
  // if (actLine.toLowerCase().contains("identifier:")) {
  // scanningIdentifier = true;
  // System.out.println("-----> scanning iden");
  // actIdentifier = actLine.substring(actLine.indexOf(":"));
  // System.out.println("act id: " + actIdentifier);
  // } else if (!actLine.toLowerCase().contains("identifier:") &&
  // !actLine.toLowerCase().contains(": ") &&
  // !actLine.matches("[a-zA-Z]+:[0-9a-zA-Z]*")) {
  // actIdentifier += actLine;
  // System.out.println("act id: " + actIdentifier);
  // } else {
  // project.setIdentifier(actIdentifier);
  // actIdentifier = "";
  // return;
  // }
  // } else {
  // // no identifier
  // scanningParent = false;
  // scanningProject = false;
  // }
  //
  // }
  // }

  /**
   * Start parsing.
   * 
   * @return the String containing the fulltext or an empty String.
   */
  private static String parseText(final File docFile) {
    final DocumentParser tikaParser = new DocumentParser();
    IDocument tikaDoc;
    try {
      tikaDoc = tikaParser.parse(docFile.toURI().toString());
      final String docTokensOrig = tikaDoc.getTextOrig();
      return docTokensOrig;
    } catch (final ApplicationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return "";
    }
  }

}
