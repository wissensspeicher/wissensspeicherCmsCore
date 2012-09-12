/**
 * This package contains several test classes.
 */
package org.bbaw.wsp.cms.dochandler.parser.test;

import org.bbaw.wsp.cms.dochandler.parser.document.IDocument;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.DocumentParser;
import java.io.*;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * Test of the {@link DocumentParser} which saves the fulltexts and maybe
 * extracted metadata in an {@link IDocument}.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * 
 */
public class DocumentParserTest {

  private static final String EXAMPLE_EDOC_URL = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/opus32_bbaw_volltexte_20120607/volltexte/2006/1/index.html";
  private static final String EXAMPLE_EDOC_HTTP_URL = "http://edoc.bbaw.de/volltexte/2010/1486/pdf/Heft_17.pdf";
  private static final String EXAMPLE_PDF_URL = "http://192.168.1.203/wsp/Czmiel_Juergens_proposal_DH2012_TheAcademysDigitalStoreOfKnowledge.pdf";

  public static void main(String[] args) throws UnsupportedEncodingException {
    DocumentParser parser = new DocumentParser();

    testEDoc(parser);
    // testPdf(parser);
    // testEDocHttp(parser);
  }

  /*
   * Test of an eDoc.
   */
  public static void testEDoc(DocumentParser parser) throws UnsupportedEncodingException {
    try {
      IDocument documentModel = parser.parse(EXAMPLE_EDOC_URL);
      System.out.println("DocumentModel: \n\n" + documentModel);
    } catch (ApplicationException e) {
      System.out.println(e);
    }
  }

  /*
   * Test of an eDoc via HTTP.
   */
  public static void testEDocHttp(DocumentParser parser) {
    try {
      IDocument documentModel = parser.parse(EXAMPLE_EDOC_HTTP_URL);
      System.out.println("DocumentModel: \n\n" + documentModel);
    } catch (ApplicationException e) {
      System.out.println(e);
    }
  }

  /*
   * Test of a "normal" pdf (not an eDoc).
   */
  public static void testPdf(DocumentParser parser) {
    try {
      IDocument documentModel = parser.parse(EXAMPLE_PDF_URL);
      System.out.println("DocumentModel: \n\n" + documentModel);
    } catch (ApplicationException e) {
      System.out.println(e);
    }
  }

}
