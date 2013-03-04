package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbaw.wsp.cms.dochandler.parser.document.IDocument;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.DocumentParser;
import org.bbaw.wsp.cms.document.MetadataRecord;

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

  private static final String DOCFILE = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/Vorhaben/Vorhabenliste_komplett_fertig.odt";

  private final File docFile;
  private final Map<String, String> resultMap;

  private final ArrayList<Vorhaben> vorhabenList;

  public VorhabenLister(final File docFile) {
    this.docFile = docFile;
    resultMap = new HashMap<>();
    vorhabenList = new ArrayList<>();
  }

  public static void main(final String[] args) {
    final VorhabenLister lister = new VorhabenLister(new File(DOCFILE));
    lister.fetchKeys();

  }

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
      final MetadataRecord tikaMDRecord = tikaDoc.getMetadata();
      return docTokensOrig;
    } catch (final ApplicationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return "";
    }
  }

}
