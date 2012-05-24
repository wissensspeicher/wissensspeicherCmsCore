package org.bbaw.wsp.cms.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.Term;
import org.bbaw.wsp.cms.document.DocumentHandler;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;
import org.bbaw.wsp.cms.transform.GetFragmentsContentHandler;
import org.bbaw.wsp.cms.transform.HighlightContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.sun.org.apache.xerces.internal.parsers.SAXParser;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.dict.db.LexHandler;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.MorphologyCache;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.WordContentHandler;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizer;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizerContentHandler;

public class TestLocal {
  private IndexHandler indexer;

  public static void main(String[] args) throws ApplicationException {
    try {
      TestLocal test = new TestLocal();
      test.init();
      // test.testXml();
      // test.tokenizeXmlFragment();
      // test.getFragments("/Users/jwillenborg/tmp/writeFragments/Benedetti_1585.xml");
      // File srcFile = new File("/Users/jwillenborg/mpdl/data/xml/documents/echo/la/Benedetti_1585/pages/page-13-morph.xml");
      // String page13 = FileUtils.readFileToString(srcFile, "utf-8");
      // test.highlight(page13, "s", 6, "reg", "relatiuum");
      test.testCalls();
      test.end();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void init() throws ApplicationException {
    indexer = IndexHandler.getInstance();
  }
  
  private void end() throws ApplicationException {
    indexer.end();
  }

  private void testXml() throws ApplicationException {
    try {
      File srcFile = new File("/Users/jwillenborg/mpdl/data/xml/documents/tei/de/dt-ptolemaeus-tei-merge2.xml");
      FileReader docFileReader = new FileReader(srcFile);
      XmlTokenizer docXmlTokenizer = new XmlTokenizer(docFileReader);
      docXmlTokenizer.setDocIdentifier("/tei/de/dt-ptolemaeus-tei-merge2.xml");
      docXmlTokenizer.tokenize();  
      ArrayList<XmlTokenizerContentHandler.Element> elements = docXmlTokenizer.getElements("s");
      String bla = "";
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void testCalls() throws ApplicationException {
    Date before = new Date();
    System.out.println("Indexing start: " + before.getTime());
    String docIdGoerz = "/tei/de/dt-ptolemaeus-tei-merge2.xml";
    String docSrcUrlStr = "http://mpdl-system.mpiwg-berlin.mpg.de/mpdl/getDoc?doc=" + docIdGoerz;
    DocumentHandler docHandler = new DocumentHandler();
    CmsDocOperation docOperation = new CmsDocOperation("update", docSrcUrlStr, null, docIdGoerz);
    // docHandler.doOperation(docOperation);
    String docIdBenedetti = "/echo/la/Benedetti_1585.xml";
    docSrcUrlStr = "http://mpdl-system.mpiwg-berlin.mpg.de/mpdl/getDoc?doc=" + docIdBenedetti;
    docHandler = new DocumentHandler();
    docOperation = new CmsDocOperation("update", docSrcUrlStr, null, docIdBenedetti);
    // docHandler.doOperation(docOperation);
    String docIdAdams = "/echo/de/Adams_1785_S7ECRGW8.xml";
    docSrcUrlStr = "http://mpdl-system.mpiwg-berlin.mpg.de/mpdl/getDoc?doc=" + docIdAdams;
    docHandler = new DocumentHandler();
    docOperation = new CmsDocOperation("update", docSrcUrlStr, null, docIdAdams);
    // docHandler.doOperation(docOperation);
    String docIdMonte = "/archimedes/la/monte_mecha_036_la_1577.xml";
    docSrcUrlStr = "http://mpdl-system.mpiwg-berlin.mpg.de/mpdl/getDoc?doc=" + docIdMonte;
    docHandler = new DocumentHandler();
    docOperation = new CmsDocOperation("update", docSrcUrlStr, null, docIdMonte);
    // docHandler.doOperation(docOperation);
    String docIdEinstein = "/diverse/de/Einst_Antwo_de_1912.xml";
    docSrcUrlStr = "http://mpdl-system.mpiwg-berlin.mpg.de:30060/mpdl/getDoc?doc=" + docIdEinstein;
    docHandler = new DocumentHandler();
    docOperation = new CmsDocOperation("update", docSrcUrlStr, null, docIdEinstein);
    // docHandler.doOperation(docOperation);
    // indexer.deleteDocument(docIdGoerz);
    // indexer.deleteDocument(docIdBenedetti);
    Date end = new Date();
    System.out.println("Indexing end: : " + end.getTime());
    String queryField = "tokenOrig";
    String queryStr = "tempore";
    System.out.println("Query: " + queryField + " contains: " + queryStr);
    // ArrayList<Document> docs = indexer.queryDocuments(queryField, queryStr);
    ArrayList<Document> docs = indexer.queryDocument(docIdMonte, queryField, queryStr);
    for (int i=0; i<docs.size(); i++) {
      Document doc = docs.get(i);
      Fieldable f = doc.getFieldable("docId");
      if (f != null) {
        String id = f.stringValue();
        System.out.print("<doc>" + id + "</doc>");
      }
      Fieldable fContent = doc.getFieldable("xmlContent");
      if (fContent != null) {
        String content = fContent.stringValue();
        System.out.print("<doc>" + content + "</doc>");
      }
    }
    System.out.println("");
    System.out.print("Browse documents: ");
    ArrayList<Term> terms = indexer.getTerms("docId", "", 1000);
    for (int i=0; i<terms.size(); i++) {
      Term term = terms.get(i);
      System.out.print(term + ", ");
    }
    System.out.print("Tokens in tokenOrig: ");
    ArrayList<Term> tokenTerms = indexer.getTerms("tokenOrig", "a", 100);
    for (int i=0; i<tokenTerms.size(); i++) {
      Term term = tokenTerms.get(i);
      System.out.print(term + ", ");
    }
    MorphologyCache.getInstance().end();
    LexHandler.getInstance().end();
  }

  private Hashtable<Integer, StringBuilder> getFragments(String fileName) throws ApplicationException {
    try {
      GetFragmentsContentHandler getFragmentsContentHandler = new GetFragmentsContentHandler();
      XMLReader xmlParser = new SAXParser();
      xmlParser.setContentHandler(getFragmentsContentHandler);
      InputSource inputSource = new InputSource(fileName);
      xmlParser.parse(inputSource);
      Hashtable<Integer, StringBuilder> resultFragments = getFragmentsContentHandler.getResultPages();
      return resultFragments;
    } catch (SAXException e) {
      throw new ApplicationException(e);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  private String tokenizeXmlFragment() throws ApplicationException {
    String result = null;
    try {
      String xmlFragment = new String(FileUtils.readFileToByteArray(new File("/Users/jwillenborg/tmp/testFragment2.xml")), "utf-8");
      String srcUrlStr = "http://mpdl-system.mpiwg-berlin.mpg.de/mpdl/page-query-result.xql?document=/echo/la/Benedetti_1585.xml&mode=pureXml&pn=13";
      URL srcUrl = new URL(srcUrlStr);
      InputStream inputStream = srcUrl.openStream();
      BufferedInputStream in = new BufferedInputStream(inputStream);
      xmlFragment = IOUtils.toString(in, "utf-8");
      in.close();

      XmlTokenizer xmlTokenizer = new XmlTokenizer(new StringReader(xmlFragment));
      xmlTokenizer.setLanguage("lat");
      String[] stopElements = {"var"};
      // xmlTokenizer.setOutputFormat("string");
      String[] outputOptions = {"withLemmas"};
      xmlTokenizer.setOutputOptions(outputOptions);
      xmlTokenizer.setStopElements(stopElements);
      xmlTokenizer.tokenize();
      result = xmlTokenizer.getXmlResult();
      System.out.println(result);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return result;
  }
  
  private String normalizeWords(String xmlStr) throws ApplicationException {
    try {
      WordContentHandler wordContentHandler = new WordContentHandler("norm");
      XMLReader xmlParser = new SAXParser();
      xmlParser.setContentHandler(wordContentHandler);
      StringReader strReader = new StringReader(xmlStr);
      InputSource inputSource = new InputSource(strReader);
      xmlParser.parse(inputSource);
      String result = wordContentHandler.getResult();
      return result;
    } catch (SAXException e) {
      throw new ApplicationException(e);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private String highlight(String xmlStr, String highlightElem, int highlightElemPos, String highlightQueryType, String highlightQuery) throws ApplicationException {
    String result = null;
    try {
      xmlStr = normalizeWords(xmlStr);
      HighlightContentHandler highlightContentHandler = new HighlightContentHandler(highlightElem, highlightElemPos, highlightQueryType, highlightQuery);
      highlightContentHandler.setFirstPageBreakReachedMode(true);
      XMLReader xmlParser = new SAXParser();
      xmlParser.setContentHandler(highlightContentHandler);
      StringReader stringReader = new StringReader(xmlStr);
      InputSource inputSource = new InputSource(stringReader);
      xmlParser.parse(inputSource);
      result = highlightContentHandler.getResult().toString();
    } catch (SAXException e) {
      throw new ApplicationException(e);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return result;
  }
}
