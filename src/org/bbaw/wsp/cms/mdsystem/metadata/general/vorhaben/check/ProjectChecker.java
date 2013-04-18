/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben.check;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben.ListerUtil;
import org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben.csvreader.ParsedProject;
import org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben.csvreader.ProjectReaderFromCsv;

/**
 * This small util class offers a method to check the (generated) csv files if they are complete.
 * 
 * It is used to fit Anett's doc.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 04.04.2013
 * 
 */
public class ProjectChecker {

  private static final String CSV_FILE_VORHABEN = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/Vorhaben/fertige Tabellen/Detaillierte Vorhabentabelle_V26_03_13.csv";
  private static final String CSV_FILE_PROJEKT = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/Vorhaben/fertige Tabellen/Detaillierte Projekttabelle_V26_03_13.csv";
  private static final String DOC_FILE = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/Vorhaben/Vorhabenliste_komplett_fertig.odt";

  /**
   * Check a generated csv file.
   * 
   * @param csvFile
   * @param docFile
   *          Anett's doc file. The script looks up all projects in this doc file and checks if those are contained in the csv file.
   * @return a list of project names which are not contained in the csv file.
   */
  public static List<String> checkCsvFile(final File csvFile, final File docFile) {
    final List<ParsedProject> projectsFromCsv = ProjectReaderFromCsv.readFromCsv(csvFile);
    final List<String> projectsInDoc = fetchProjects(docFile);
    final List<String> notContained = new ArrayList<>();

    for (final String projectInDoc : projectsInDoc) {
      boolean isContained = false;
      for (final ParsedProject parsedProject : projectsFromCsv) {
        if (parsedProject.getProject().contains(projectInDoc) || parsedProject.getVorhaben().contains(projectInDoc)) {
          isContained = true;
        }
      }
      if (!isContained) {
        notContained.add(projectInDoc);
      }
    }
    return notContained;
  }

  /**
   * Fetch the projects from Anett's doc file.
   * 
   * @param docFile
   * @return
   */
  private static List<String> fetchProjects(final File docFile) {
    final String fulltext = ListerUtil.parseText(docFile);
    final List<String> projects = new ArrayList<>();
    final Pattern pProject = Pattern.compile("(?i)\\*(.*?)\n(?i)");
    for (final Matcher m = pProject.matcher(fulltext); m.find();) {
      String projectName = m.group(1);
      projectName = projectName.replaceFirst("Title:", "");
      projectName = projectName.replaceFirst("Creator:", "");
      projectName = projectName.trim();
      projects.add(projectName);
    }
    return projects;
  }

  public static void main(final String[] args) {
    final List<String> vorhabenNot = ProjectChecker.checkCsvFile(new File(CSV_FILE_VORHABEN), new File(DOC_FILE));
    final List<String> projectNot = ProjectChecker.checkCsvFile(new File(CSV_FILE_PROJEKT), new File(DOC_FILE));
    findMissing(vorhabenNot, projectNot);
  }

  private static void findMissing(final List<String> vorhabenNot, final List<String> projectNot) {
    for (final String string : vorhabenNot) {
      if (!projectNot.contains(string)) {
        System.out.println(string + " is definitly not contained in the resulting csv.");
      }
    }
  }
}
