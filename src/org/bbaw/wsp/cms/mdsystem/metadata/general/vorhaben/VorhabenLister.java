package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbaw.wsp.cms.dochandler.parser.document.IDocument;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.DocumentParser;
import org.bbaw.wsp.cms.document.MetadataRecord;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.googlecode.jcsv.writer.CSVEntryConverter;
import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * 
 */

/**
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 04.03.2013
 * 
 */
public class VorhabenLister {
  private static final String DUMMY_VORHABEN = "KOPFZELLEN";

  /**
   * Convert to CSV via an {@link CSVEntryConverter} implementation
   * 
   * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
   * @since 05.03.2013
   * 
   */
  public class VorhabenCsvConverter implements CSVEntryConverter<ARecordEntry> {

    private final TreeMap<String, Integer> sortedKeysMap;

    public VorhabenCsvConverter(final TreeMap<String, Integer> sortedKeysMap) {
      this.sortedKeysMap = sortedKeysMap;
    }

    @Override
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.googlecode.jcsv.writer.CSVEntryConverter#convertEntry(java.lang.Object
     * )
     */
    public String[] convertEntry(final ARecordEntry recordEntry) {
      final int numberCols = 2 + sortedKeysMap.size();
      final String[] columns = new String[numberCols];
      if (recordEntry.name.equals(DUMMY_VORHABEN)) {
        int i = 2;
        columns[0] = "Creator";
        columns[1] = "Title";
        for (final String fieldKey : sortedKeysMap.keySet()) {
          columns[i] = fieldKey;
          ++i;
        }
      } else {
        columns[0] = (recordEntry instanceof Vorhaben ? recordEntry.getName() : "---");

        columns[1] = (recordEntry instanceof Projekt ? recordEntry.getName() : "---");

        int i = 2;
        for (final String key : sortedKeysMap.keySet()) {
          String value = recordEntry.getFields().get(key);
          if (value == null) {
            value = "";
          }
          columns[i] = value;
          ++i;
        }
      }
      return columns;
    }
  }

  /**
   * ValueComparator which is used to sort the values of an unsorted {@link Map}
   * descending.
   * 
   * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
   * @since 05.03.2013
   * 
   */
  public class ValueComparator implements Comparator<String> {

    private final HashMap<String, Integer> map;

    public ValueComparator(final HashMap<String, Integer> map) {
      this.map = map;
    }

    @Override
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(final String s1, final String s2) {
      final int i1 = map.get(s1);
      final int i2 = map.get(s2);
      return (i1 == i2 ? 0 : (i1 > i2 ? -1 : 1));
    }

  }

  private static final String DOCFILE = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/Vorhaben/Vorhabenliste_komplett_fertig.odt";
  private static final String OUTPUTCSV = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/Vorhaben/Vorhaben_converted.csv";

  private final File docFile;
  private final HashMap<String, Integer> globalKeyMap;

  private final Set<Vorhaben> vorhabenSet;
  private final Set<Projekt> projectSet;

  public VorhabenLister(final File docFile) {
    this.docFile = docFile;
    globalKeyMap = new HashMap<>();
    vorhabenSet = new HashSet<>();
    projectSet = new HashSet<>();
    final Vorhaben dummy = new Vorhaben();
    dummy.setName(DUMMY_VORHABEN);
    vorhabenSet.add(dummy);
  }

  public static void main(final String[] args) {
    final VorhabenLister lister = new VorhabenLister(new File(DOCFILE));
    lister.fetchVorhaben();
    lister.writeToCsv(lister.vorhabenSet, lister.projectSet, lister.getSortedKeysMap());
    // final TreeMap<String, Integer> sortedKeysMap = lister.getSortedKeysMap();
    // System.out.println(lister.getSortedKeysMap());
    // lister.globalKeyMap.keySet()

    // System.out.println("\n\n\nParsing completed:............\n\n\n");
    // for (final Vorhaben v : lister.vorhabenList) {
    // System.out.println("Vorhaben: " + v.getName());
    // System.out.println("Felder: " + v.getFields());
    // }
    // System.out.println(lister.vorhabenList);
  }

  private TreeMap<String, Integer> getSortedKeysMap() {
    final ValueComparator myComparator = new ValueComparator(globalKeyMap);
    final TreeMap<String, Integer> sortedMap = new TreeMap<String, Integer>(myComparator);

    sortedMap.putAll(globalKeyMap);

    return sortedMap;
  }

  private void writeToCsv(final Set<Vorhaben> vorhabenAndProjectSet, final Set<Projekt> projectSet, final TreeMap<String, Integer> sortedKeysMap) {
    try {
      final Writer out = new FileWriter(OUTPUTCSV);

      final CSVWriter<ARecordEntry> csvWriter = new CSVWriterBuilder<ARecordEntry>(out).entryConverter(new VorhabenCsvConverter(sortedKeysMap)).build();

      // fetch dummy vorhaben
      for (final Vorhaben vorhaben : vorhabenAndProjectSet) {
        if (vorhaben.getName().equals(DUMMY_VORHABEN)) {
          csvWriter.write(vorhaben);
          break;
        }
      }

      for (final Vorhaben vorhaben : vorhabenAndProjectSet) {
        if (!vorhaben.getName().equals(DUMMY_VORHABEN)) {
          csvWriter.write(vorhaben); // Vorhaben Data first
          final List<ARecordEntry> projekte = vorhaben.getProjekte();
          csvWriter.writeAll(projekte); // project data
        }
      }

      csvWriter.flush();
      csvWriter.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private void fetchVorhaben() {
    final String parsedText = parseText();
    final Scanner scanner = new Scanner(parsedText);

    int i = 0;
    int jumpToLine = -1;
    while (scanner.hasNextLine()) {
      ++i;

      if (jumpToLine != -1 && i <= jumpToLine && scanner.hasNext()) { // skip
                                                                      // lines
        scanner.nextLine();
      } else { // stop skipping
        jumpToLine = -1;
        final String line = scanner.nextLine();
        if (line.trim().startsWith("*") && line.trim().endsWith("*")) { // identified
                                                                        // Vorhaben
          final int lineNumber = i;
          // Fill vorhaben record
          final Vorhaben vorhaben = new Vorhaben();
          final String vorhabenName = line.trim().substring(1, line.trim().length() - 1);
          vorhaben.setName(vorhabenName.replace("Creator: ", ""));
          vorhabenSet.add(vorhaben);
          System.out.println("\n\n-----------\n\n" + vorhabenName + "\n\n-----------\n\n");

          int retFieldNumber = fetchFields(vorhaben, parsedText, lineNumber);

          if (retFieldNumber == -1) {
            retFieldNumber = lineNumber;
          }

          final int retLineNumber = fetchProjekte(vorhaben, parsedText, retFieldNumber);

          vorhabenSet.add(vorhaben);

          if (retLineNumber != -1) {
            jumpToLine = retLineNumber;
          } else { // stop -> eof
            scanner.close();
            return;
          }
        }
      }
    }
    scanner.close();
  }

  private int fetchProjekte(final Vorhaben vorhaben, final String parsedText, final int lineNumber) {
    final String[] splittedText = parsedText.split("\n");

    boolean scanningProjekt = false; // indicates whether a project was detected
    Projekt aktProjekt = new Projekt();

    for (int i = lineNumber; i < splittedText.length; i++) {
      final String line = splittedText[i];

      if (line.trim().startsWith("*") && !line.trim().endsWith("*")) { // identified
                                                                       // Projekt
        if (scanningProjekt == false) {
          scanningProjekt = true;
          final String projektName = line.trim().substring(1, line.trim().length());
          System.out.println("\n\n+++++++++++\n\n" + projektName + "\n\n+++++++++++\n\n");
          aktProjekt.setName(projektName.replace("Title: ", ""));
        } else { // end of actual project
          vorhaben.addProjekt(aktProjekt);
          aktProjekt = new Projekt();
          final String projektName = line.trim().substring(1, line.trim().length());
          aktProjekt.setName(projektName.replace("Title: ", ""));
        }
      } else if (line.trim().startsWith("*") && line.trim().endsWith("*")) { // end
        // of
        // actual
        // Vorhaben
        return i;
      } else if (scanningProjekt) {
        fetchFields(aktProjekt, parsedText, i);
      } else { // end of file?
        return -1;
      }
    }
    return -1;
  }

  private void fetchFields(final Projekt aktProjekt, final String parsedText, final int lineNumber) {
    final String[] splittedText = parsedText.split("\n");

    boolean scanningField = false; // indicates whether a field was
                                   // detected
    String oldKey = "";
    String oldValue = "";
    for (int i = lineNumber; i < splittedText.length; i++) {
      final String line = splittedText[i];

      if ((line.trim().startsWith("*") && !line.trim().endsWith("*")) || (line.trim().startsWith("*") && line.trim().endsWith("*"))) {
        // stop here
        return;
      } else if (line.contains(":")) { // identified field
        final String key = line.substring(0, line.indexOf(":"));
        final String value = line.substring(line.indexOf(":") + 1, line.length());
        // value.replace(",", ";"); // neccessary -> , only for CSV seperations!

        aktProjekt.addField(key, value);
        // increase global quantitiy of keys in map
        globalKeyMap.put(key, (!globalKeyMap.containsKey(key) ? 1 : globalKeyMap.get(key) + 1)); // add
                                                                                                 // to
                                                                                                 // global
                                                                                                 // set
                                                                                                 // so
                                                                                                 // we
                                                                                                 // can
        // easily lookup for
        // keys
        scanningField = true;
        oldKey = key;
        oldValue = value;
      } else if (!line.contains(":") && scanningField) { // new line for actual
                                                         // project
        final String newValue = oldValue + "\n" + line;
        aktProjekt.addField(oldKey, newValue); // set new value
        oldValue = newValue;
      }
    }
  }

  private int fetchFields(final Vorhaben aktVorhaben, final String parsedText, final int lineNumber) {
    final String[] splittedText = parsedText.split("\n");

    boolean scanningField = false; // indicates whether a field was
                                   // detected
    String oldKey = "";
    String oldValue = "";
    for (int i = lineNumber; i < splittedText.length; i++) {
      final String line = splittedText[i];

      if ((line.trim().startsWith("*") && !line.trim().endsWith("*")) || (line.trim().startsWith("*") && line.trim().endsWith("*"))) {
        // stop here
        return i;
      } else if (line.contains(":")) { // identified field
        final String key = line.substring(0, line.indexOf(":"));
        final String value = line.substring(line.indexOf(":") + 1, line.length());
        // value.replace(",", ";"); // neccessary -> , only for CSV seperations!

        aktVorhaben.addField(key, value);
        globalKeyMap.put(key, (!globalKeyMap.containsKey(key) ? 0 : globalKeyMap.get(key) + 1)); // add
                                                                                                 // to
                                                                                                 // global
                                                                                                 // set
                                                                                                 // so
                                                                                                 // we
                                                                                                 // can
                                                                                                 // easily
                                                                                                 // lookup
                                                                                                 // for
        // keys
        scanningField = true;
        oldKey = key;
        oldValue = value;
      } else if (!line.contains(":") && scanningField) { // new line for actual
                                                         // project
        if (!line.trim().equals("")) {
          final String newValue = oldValue + "\n" + line;
          aktVorhaben.addField(oldKey, newValue); // set new value
          oldValue = newValue;
        }
      }
    }
    return -1;
  }

  // @formatter:off
  /*
   * 
   * Old regex version -> not working further than Vorhaben
   * 
  private void fetchKeys() {
    final String parsedText = parseText();
    // System.out.println("parsed Text:\n\n" + parsedText);

    // final Pattern p =
    // Pattern.compile("(?i)[*]{1,1}[:A-Za-z]+[*]{1,1}(.*?)[*]{1,1}[:A-Za-z]+[*]{1,1}(?i)");
    // final Pattern p = Pattern.compile("(?i)[*](.*?)[*](?i)");
    final Pattern p = Pattern.compile("(?i)[*](.*?)[*](.*\n*)*?[*](.*?)[*](?i)");
    for (final Matcher m = p.matcher(parsedText); m.find();) {
      final String parsedVorhaben = m.group(1);
      final String vorhabenContent = m.group(2);
      System.out.println("//// content zum vorhaben: " + vorhabenContent + "/////");

      // extract projects
      final Pattern pProject = Pattern.compile("[*]" + parsedVorhaben + "[*](.*\n*)*?[*](.*?)");
      for (final Matcher mProject = pProject.matcher(parsedText); mProject.find();) {
        final String parsedProject = mProject.group(1);
        System.out.println("\n +++++++ \n" + parsedProject + " \n +++++++ \n");
      }

      // Fill vorhaben record
      final Vorhaben vorhaben = new Vorhaben();
      vorhaben.setName(parsedVorhaben);
      vorhabenList.add(vorhaben);
      System.out.println("\n\n-----------\n\n" + parsedVorhaben + "\n\n-----------\n\n");
    }

  }
  */
  
  //@formatter:on

  /**
   * Start parsing.
   * 
   * @return the String containing the fulltext or an empty String.
   */
  private String parseText() {
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
