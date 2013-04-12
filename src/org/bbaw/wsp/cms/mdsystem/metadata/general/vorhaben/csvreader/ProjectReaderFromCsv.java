/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben.csvreader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.annotations.internal.ValueProcessorProvider;
import com.googlecode.jcsv.reader.CSVEntryParser;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.AnnotationEntryParser;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;

/**
 * Read from an CSV file and fill record class.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.03.2013
 * 
 */
public class ProjectReaderFromCsv {

  /**
   * Path to the input csv file.
   */
  public static final String INPUT_CSV = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/Vorhaben/InsertIdentifiers/V13_03_2013.csv";

  public static void main(final String[] args) {
    final File csvFile = new File(INPUT_CSV);
    final List<ParsedProject> projects = readFromCsv(csvFile);

    System.out.println(projects);
  }

  public static List<ParsedProject> readFromCsv(final File csvFile) {
    try {
      final Reader reader = new FileReader(csvFile);

      final ValueProcessorProvider provider = new ValueProcessorProvider();
      final CSVEntryParser<ParsedProject> entryParser = new AnnotationEntryParser<>(ParsedProject.class, provider);
      // strategy: UK -> see http://code.google.com/p/jcsv/wiki/CSVStrategy
      final CSVReader<ParsedProject> csvProjectReader = new CSVReaderBuilder<ParsedProject>(reader).strategy(CSVStrategy.UK_DEFAULT).entryParser(entryParser).build();

      // final Iterator<ParsedProject> it = csvProjectReader.iterator();
      // while (it.hasNext()) {
      // final ParsedProject actProject = it.next();
      // System.out.println("parsed: " + actProject + "\n\n");
      // }
      final List<ParsedProject> parsedProjects = csvProjectReader.readAll();

      return parsedProjects;
    } catch (final FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }
}
