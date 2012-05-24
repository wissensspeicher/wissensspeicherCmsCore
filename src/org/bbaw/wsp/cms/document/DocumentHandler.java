package org.bbaw.wsp.cms.document;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

import org.apache.commons.io.FileUtils;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;
import org.bbaw.wsp.cms.transform.GetFragmentsContentHandler;
import org.bbaw.wsp.cms.transform.XslResourceTransformer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.sun.org.apache.xerces.internal.parsers.SAXParser;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizer;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

/**
 * Handler for documents (singleton). 
 */
public class DocumentHandler {
  private long beginOfOperation;
  private long endOfOperation;
  
  public void doOperation(CmsDocOperation docOperation) throws ApplicationException{
    String operationName = docOperation.getName();  
    if (operationName.equals("create") || operationName.equals("update")) {
      createOrUpdate(docOperation);
    } else if (operationName.equals("delete")) {
      delete(docOperation);
    } else if (operationName.equals("importAllDocumentsLocallyExist")) {
      importAllDocumentsLocally();
    }
  }
  
  private void importAllDocumentsLocally() throws ApplicationException {
    System.out.println("Start of DocumentHandler. This operation could be time consuming because documents are indexed (normal indexing times are 10 seconds for a document) ...");
    beginOperation();
    // saveDocumentFiles();  // TODO
    endOperation();
    System.out.println("The DocumentHandler needed: " + (endOfOperation - beginOfOperation) + " ms" );
  }
  
  private void createOrUpdate(CmsDocOperation docOperation) throws ApplicationException {
    try {
      String operationName = docOperation.getName();  
      String srcUrlStr = docOperation.getSrcUrl(); 
      String docIdentifier = docOperation.getDocIdentifier();
      String elementNames = docOperation.getElementNames();
      if (elementNames == null)
        elementNames = "s head caption variables description";
      String docDirName = getDocDir(docIdentifier);
      String docDestFileName = getDocFullFileName(docIdentifier); 
      URL srcUrl = null;
      String protocol = null;
      if (srcUrlStr != null && ! srcUrlStr.equals("empty")) {
        srcUrl = new URL(srcUrlStr);
        protocol = srcUrl.getProtocol();
      }
      File docDestFile = new File(docDestFileName);
      boolean docExists = docDestFile.exists();
      if (operationName.equals("create") && docExists) {
        throw new ApplicationException("Document: " + docIdentifier + " already exists. Please use another name or perform the operation \"Update\" of that document.");
      }
      if (operationName.equals("update") && ! docExists) {
        throw new ApplicationException("Document: " + docIdentifier + " does not exist. Please use a name that exists and perform the operation \"Update\" again or perform the operation \"Create\" of that document");
      }
      // parse validation on file
      XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
      XdmNode docNode = xQueryEvaluator.parse(srcUrl); // if it is not parseable an exception with a detail message is thrown 
      String docType = getNodeType(docNode);  // archimedes, echo, TEI or html 
      if (docType == null) {
        docOperation.setErrorMessage("file type of: " + srcUrlStr + "is not supported (valid types are: TEI, echo, archimedes, html)");
        return;
      }
      // perform operation on file system
      if (protocol.equals("file")) {
        docOperation.setStatus("upload file: " + srcUrlStr + " to CMS");
      } else {
        docOperation.setStatus("download file from: " + srcUrlStr + " to CMS");
      }
      FileUtils.copyURLToFile(srcUrl, docDestFile, 100000, 100000);

      // replace anchor in echo documents and also add the number attribute to figures
      String docDestFileNameUpgrade = docDestFileName + ".upgrade";
      File docDestFileUpgrade = new File(docDestFileNameUpgrade);
      XslResourceTransformer replaceAnchorTransformer = new XslResourceTransformer("replaceAnchor.xsl");
      String docDestFileUrlStr = docDestFile.getPath();
      String result = replaceAnchorTransformer.transform(docDestFileUrlStr);
      FileUtils.writeStringToFile(docDestFileUpgrade, result, "utf-8");
      
      // generate toc file (toc, figure, handwritten)
      XslResourceTransformer tocTransformer = new XslResourceTransformer("toc.xsl");
      File tocFile = new File(docDirName + "/toc.xml");
      String tocResult = tocTransformer.transform(docDestFileName);
      FileUtils.writeStringToFile(tocFile, tocResult, "utf-8");

      // Get metadata info of the xml document
      MetadataRecord mdRecord = new MetadataRecord();
      mdRecord.setDocId(docIdentifier);
      mdRecord.setType("text/xml");
      docOperation.setStatus("extract metadata of: " + srcUrlStr + " to CMS");
      mdRecord = getMetadataRecord(docDestFile, docType, mdRecord);

      // save all pages as single xml files (untokenized and tokenized)
      docOperation.setStatus("extract page fragments of: " + srcUrlStr + " to CMS");
      File docDir = new File(docDirName + "/pages");
      FileUtils.deleteQuietly(docDir);  // first delete pages directory
      Hashtable<Integer, StringBuilder> pageFragments = getFragments(docDestFileNameUpgrade, "pb");
      int pageCount = pageFragments.size();
      for (int page=1; page<=pageCount; page++) {
        String fragment = pageFragments.get(new Integer(page)).toString();
        fragment = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + fragment;
        String docPageFileName = docDirName + "/pages/page-" + page + ".xml";
        File docPageFile = new File(docPageFileName);
        FileUtils.writeStringToFile(docPageFile, fragment, "utf-8");
        String language = mdRecord.getLanguage();
        String tokenizedXmlStr = tokenizeWithLemmas(fragment, language);
        tokenizedXmlStr = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + tokenizedXmlStr;
        String docPageTokenizedFileName = docDirName + "/pages/page-" + page + "-morph.xml";
        File docPageTokenizedFile = new File(docPageTokenizedFileName);
        FileUtils.writeStringToFile(docPageTokenizedFile, tokenizedXmlStr, "utf-8");
      }
      
      // perform operation on Lucene
      docOperation.setStatus(operationName + " document: " + docIdentifier + " in CMS");
      docOperation.setMdRecord(mdRecord);
      docOperation.setElementNames(elementNames);
      IndexHandler indexHandler = IndexHandler.getInstance();
      indexHandler.indexDocument(docOperation);
      
      // TODO: perform operation on version management system
      // String luceneDocumentsDirectory = Constants.getInstance().getLuceneDocumentsDir();
      // String luceneNodesDirectory = Constants.getInstance().getLuceneNodesDir();
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void delete(CmsDocOperation docOperation) throws ApplicationException {
    String operationName = docOperation.getName();  
    String docIdentifier = docOperation.getDocIdentifier();
    if (docIdentifier == null || docIdentifier.trim().equals(""))
      throw new ApplicationException("Your document identifier is empty. Please specify a document identifier for your document.");
    String docDirStr = getDocDir(docIdentifier);
    File docDir = new File(docDirStr);
    boolean docExists = docDir.exists();
    if (! docExists) {
      throw new ApplicationException("Document:" + docIdentifier + " does not exists. Please use a name that exists and perform the operation \"Delete\" again.");
    }
    // perform operation on file system
    docOperation.setStatus(operationName + " document: " + docIdentifier + " in CMS");
    FileUtils.deleteQuietly(docDir);
      
    // perform operation on Lucene
    IndexHandler indexHandler = IndexHandler.getInstance();
    indexHandler.deleteDocument(docOperation);
      
    // TODO: perform operation on version management system
    // String luceneDocumentsDirectory = Constants.getInstance().getLuceneDocumentsDir();
    // String luceneNodesDirectory = Constants.getInstance().getLuceneNodesDir();
  }
  
  private MetadataRecord getMetadataRecord(File xmlFile, String schemaName, MetadataRecord mdRecord) throws ApplicationException {
    if (schemaName == null)
      return mdRecord;
    try {
      URL srcUrl = xmlFile.toURI().toURL();
      XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
      if (schemaName.equals("archimedes"))
        mdRecord = getMetadataRecordArch(xQueryEvaluator, srcUrl, mdRecord);
      else if (schemaName.equals("echo"))
        mdRecord = getMetadataRecordEcho(xQueryEvaluator, srcUrl, mdRecord);
      else if (schemaName.equals("TEI"))
        mdRecord = getMetadataRecordTei(xQueryEvaluator, srcUrl, mdRecord);
      else if (schemaName.equals("html"))
        mdRecord = getMetadataRecordHtml(xQueryEvaluator, srcUrl, mdRecord);
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    mdRecord.setLastModified(new Date());
    return mdRecord;
  }

  private MetadataRecord getMetadataRecordArch(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/archimedes//info");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/info/cvs_file");
      if (identifier != null)
        identifier = StringUtils.deresolveXmlEntities(identifier);
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/info/author");
      if (creator != null)
        creator = StringUtils.deresolveXmlEntities(creator);
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/info/title");
      if (title != null)
        title = StringUtils.deresolveXmlEntities(title);
      String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/info/lang");
      if (language != null)
        language = StringUtils.deresolveXmlEntities(language);
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/info/date");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr);
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null)
          date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
      }
      String rights = "open access";
      String license = "http://echo.mpiwg-berlin.mpg.de/policy/oa_basics/declaration";
      String accessRights = "free";
      String echoDir = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/info/echodir");
      String docId = mdRecord.getDocId();
      String echoIdTmp = docId;
      if (docId != null && ! docId.isEmpty()) {
        int start = docId.lastIndexOf("/");
        if (start != -1)
          start = start + 1;
        else 
          start = 0;
        int end = docId.lastIndexOf(".");
        if (end == -1)
          end = docId.length();
        echoIdTmp = docId.substring(start, end);
      }
      String echoId = "/permanent/archimedes/" + echoIdTmp;
      if (echoDir != null && ! echoDir.isEmpty()) {
        echoId = echoDir;
      }
      mdRecord.setIdentifier(identifier);
      mdRecord.setEchoId(echoId);
      mdRecord.setLanguage(language);
      mdRecord.setCreator(creator);
      mdRecord.setTitle(title);
      mdRecord.setRights(rights);
      mdRecord.setDate(date);
      mdRecord.setLicense(license);
      mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("archimedes");
    return mdRecord;
  }
  
  private MetadataRecord getMetadataRecordEcho(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/*:echo/*:metadata");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:metadata/*:identifier");
      if (identifier != null)
        identifier = StringUtils.deresolveXmlEntities(identifier);
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:metadata/*:creator");
      if (creator != null)
        creator = StringUtils.deresolveXmlEntities(creator);
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:metadata/*:title");
      if (title != null)
        title = StringUtils.deresolveXmlEntities(title);
      String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:metadata/*:language");
      if (language != null)
        language = StringUtils.deresolveXmlEntities(language);
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:metadata/*:date");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr);
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null)
          date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z"); 
      }
      String rights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:metadata/*:rights");
      if (rights != null)
        rights = StringUtils.deresolveXmlEntities(rights);
      String license = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:metadata/*:license");
      if (license != null)
        license = StringUtils.deresolveXmlEntities(license);
      String accessRights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:metadata/*:accessRights");
      if (accessRights != null)
        accessRights = StringUtils.deresolveXmlEntities(accessRights);
      String echoDir = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:metadata/*:echodir");
      String echoIdTmp = identifier;
      if (identifier != null && ! identifier.isEmpty()) {
        int start = identifier.indexOf("ECHO:");
        if (start != -1)
          start = start + 5;
        else 
          start = 0;
        int end = identifier.lastIndexOf(".");
        if (end == -1)
          end = identifier.length();
        echoIdTmp = identifier.substring(start, end);
      }
      String echoId = "/permanent/library/" + echoIdTmp;
      if (echoDir != null && ! echoDir.isEmpty()) {
        echoId = echoDir;
      }
      mdRecord.setIdentifier(identifier);
      mdRecord.setEchoId(echoId);
      mdRecord.setLanguage(language);
      mdRecord.setCreator(creator);
      mdRecord.setTitle(title);
      mdRecord.setRights(rights);
      mdRecord.setDate(date);
      mdRecord.setLicense(license);
      mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//*:pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("echo");
    return mdRecord;
  }

  private MetadataRecord getMetadataRecordTei(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/*:TEI/*:teiHeader");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:idno");
      if (identifier != null)
        identifier = StringUtils.deresolveXmlEntities(identifier);
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:author");
      if (creator != null)
        creator = StringUtils.deresolveXmlEntities(creator);
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title");
      if (title != null)
        title = StringUtils.deresolveXmlEntities(title);
      String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/*:teiHeader/*:profileDesc/*:langUsage/*:language/@ident)");
      if (language != null)
        language = StringUtils.deresolveXmlEntities(language);
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:date");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr);
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null)
          date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
      }
      String rights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:availability");
      if (rights == null)
        rights = "open access";
      rights = StringUtils.deresolveXmlEntities(rights);
      String license = "http://echo.mpiwg-berlin.mpg.de/policy/oa_basics/declaration";
      String accessRights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/*:teiHeader/*:fileDesc/*:publicationStmt/*:availability/@status)");
      if (accessRights == null) 
        accessRights = "free";
      accessRights = StringUtils.deresolveXmlEntities(accessRights);
      mdRecord.setIdentifier(identifier);
      mdRecord.setEchoId(identifier);
      mdRecord.setLanguage(language);
      mdRecord.setCreator(creator);
      mdRecord.setTitle(title);
      mdRecord.setRights(rights);
      mdRecord.setDate(date);
      mdRecord.setLicense(license);
      mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//*:pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("TEI");
    return mdRecord;
  }

  private MetadataRecord getMetadataRecordHtml(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/html/head");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/meta[@name = 'DC.identifier']/@content)");
      if (identifier != null && ! identifier.isEmpty())
        identifier = StringUtils.deresolveXmlEntities(identifier);
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/meta[@name = 'DC.creator']/@content)");
      if (creator != null && ! creator.isEmpty())
        creator = StringUtils.deresolveXmlEntities(creator);
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/meta[@name = 'DC.title']/@content)");
      if (title != null && ! title.isEmpty())
        title = StringUtils.deresolveXmlEntities(title);
      String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/meta[@name = 'DC.language']/@content)");
      if (language != null && ! language.isEmpty())
        language = StringUtils.deresolveXmlEntities(language);
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/meta[@name = 'DC.date']/@content)");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr);
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null)
          date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
      }
      String rights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/meta[@name = 'DC.rights']/@content)");
      if (rights != null && ! rights.isEmpty())
        rights = StringUtils.deresolveXmlEntities(rights);
      String license = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/meta[@name = 'DC.license']/@content)");
      if (license != null && ! license.isEmpty())
        license = StringUtils.deresolveXmlEntities(license);
      String accessRights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/meta[@name = 'DC.accessRights']/@content)");
      if (accessRights != null && ! accessRights.isEmpty())
        accessRights = StringUtils.deresolveXmlEntities(accessRights);
      mdRecord.setIdentifier(identifier);
      mdRecord.setEchoId(identifier);
      mdRecord.setLanguage(language);
      mdRecord.setCreator(creator);
      mdRecord.setTitle(title);
      mdRecord.setRights(rights);
      mdRecord.setDate(date);
      mdRecord.setLicense(license);
      mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("html");
    return mdRecord;
  }

  private String getNodeType(XdmNode node) {
    String nodeType = null;
    XdmSequenceIterator iter = node.axisIterator(Axis.CHILD);
    if (iter != null) {
      while (iter.hasNext()) {
        XdmNode firstChild = (XdmNode) iter.next();
        if (firstChild != null) {
          XdmNodeKind nodeKind = firstChild.getNodeKind();
          if (nodeKind.ordinal() == XdmNodeKind.ELEMENT.ordinal()) {
            QName nodeQName = firstChild.getNodeName();
            nodeType = nodeQName.getLocalName();
          }
        }
      }
    }
    return nodeType;
  }
  
  public String getDocFullFileName(String docId) {
    String docDir = getDocDir(docId);
    String docFileName = getDocFileName(docId);
    String docFullFileName = docDir + "/" + docFileName; 
    return docFullFileName;
  }
  
  public String getDocDir(String docId) {
    String documentsDirectory = Constants.getInstance().getDocumentsDir();
    int index = docId.indexOf(".xml");
    String subDocDir = docId.substring(0, index);
    if (subDocDir.startsWith("/"))
      subDocDir = subDocDir.substring(1);
    String docDir = documentsDirectory + "/" + subDocDir;
    return docDir;
  }
  
  private String getDocFileName(String docId) {
    String docFileName = docId;
    int index = docId.lastIndexOf("/");
    if (index != -1) {
      docFileName = docId.substring(index + 1);
    }
    return docFileName;
  }
  
  private Hashtable<Integer, StringBuilder> getFragments(String fileName, String milestoneElementName) throws ApplicationException {
    try {
      GetFragmentsContentHandler getFragmentsContentHandler = new GetFragmentsContentHandler(milestoneElementName);
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

  private String tokenizeWithLemmas(String xmlStr, String language) throws ApplicationException {
    StringReader strReader = new StringReader(xmlStr);
    XmlTokenizer xmlTokenizer = new XmlTokenizer(strReader);
    xmlTokenizer.setLanguage(language);
    String[] outputOptionsWithLemmas = {"withLemmas"}; // so all tokens are fetched with lemmas (costs performance)
    xmlTokenizer.setOutputOptions(outputOptionsWithLemmas); 
    xmlTokenizer.tokenize();  
    String retStr = xmlTokenizer.getXmlResult();
    return retStr;
  }
  
  private void beginOperation() {
    beginOfOperation = new Date().getTime();
  }

  private void endOperation() {
    endOfOperation = new Date().getTime();
  }

}