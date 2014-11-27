package org.bbaw.wsp.cms.dochandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.FileNameMap;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

import org.apache.log4j.Logger;
import org.apache.commons.io.FileUtils;
import org.bbaw.wsp.cms.collections.Collection;
import org.bbaw.wsp.cms.collections.CollectionManager;
import org.bbaw.wsp.cms.collections.CollectionReader;
import org.bbaw.wsp.cms.dochandler.parser.document.IDocument;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.DocumentParser;
import org.bbaw.wsp.cms.document.Annotation;
import org.bbaw.wsp.cms.document.DBpediaResource;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.document.XQuery;
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
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.Token;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizer;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizerContentHandler;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

/**
 * Handler for documents (singleton). 
 */
public class DocumentHandler {
  private static Logger LOGGER = Logger.getLogger(DocumentHandler.class);
  
  public void doOperation(CmsDocOperation docOperation) throws ApplicationException{
    String operationName = docOperation.getName();  
    if (operationName.equals("create")) {
      create(docOperation);
    } else if (operationName.equals("delete")) {
      delete(docOperation);
    } else if (operationName.equals("updateCollection")) {
      CollectionManager collectionManager = CollectionManager.getInstance();
      String collectionId = docOperation.getCollectionId();
      collectionManager.updateCollection(collectionId);
    } else if (operationName.equals("deleteCollection")) {
      deleteCollection(docOperation);
    }
  }
  
  private void create(CmsDocOperation docOperation) throws ApplicationException {
    try {
      String operationName = docOperation.getName();  
      String srcUrlStr = docOperation.getSrcUrl(); 
      String docId = docOperation.getDocIdentifier();
      String mainLanguage = docOperation.getMainLanguage();
      String[] elementNames = docOperation.getElementNames();
      MetadataRecord mdRecord = docOperation.getMdRecord();
      if (elementNames == null) {
        String[] defaultElementNames = {"persName", "placeName", "p", "s", "head"};
        docOperation.setElementNames(defaultElementNames); // default
      }
      String docDirName = getDocDir(docId);
      String docDestFileName = getDocFullFileName(docId); 
      boolean docIsXml = isDocXml(docId); 
      URL srcUrl = null;
      if (srcUrlStr != null && ! srcUrlStr.equals("empty")) {
        srcUrl = new URL(srcUrlStr);
      }
      File docDestFile = new File(docDestFileName);
      String mimeType = mdRecord.getType();
      if (mimeType == null) {
        mimeType = getMimeType(docId);
        mdRecord.setType(mimeType);
      }
      // perform operation on file system
      boolean isDbRecord = mdRecord.isDbRecord();
      boolean isText = isText(mimeType);
      if (! isDbRecord && isText) {
        boolean success = copyUrlToFile(srcUrl, docDestFile);  // several tries with delay
        if (! success)
          return;
      }
      if (mdRecord.getLastModified() == null) {
        Date lastModified = new Date();
        mdRecord.setLastModified(lastModified);
      }
      String docType = null;
      XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
      if (docIsXml) {
        // parse validation on file
        try {
          XdmNode docNode = xQueryEvaluator.parse(srcUrl); // if it is not parseable an exception with a detail message is thrown 
          docType = getNodeType(docNode);
          docType = docType.trim();
        } catch (ApplicationException e) {
          // second try with encoded blanks url
          String srcUrlFileEncodedStr = srcUrl.getFile().replaceAll(" ", "%20");
          String srcUrlEncodedStr = srcUrl.getProtocol() + "://" + srcUrl.getHost() + ":" + srcUrl.getPort() + srcUrlFileEncodedStr;
          URL srcUrlEncoded = new URL(srcUrlEncodedStr);
          try {
            XdmNode docNode = xQueryEvaluator.parse(srcUrlEncoded);
            docType = getNodeType(docNode);
            docType = docType.trim();
          } catch (Exception e2) {
            LOGGER.error(srcUrlStr + " is not xml parseable");
            e.printStackTrace();
          }
        }
      }
      if (docType == null) {
        docType = mimeType;
      }
      if (srcUrl != null && docType == null) {
        FileUtils.deleteQuietly(docDestFile);
        docOperation.setErrorMessage(srcUrlStr + " is not created: mime type is not supported");
        LOGGER.info(srcUrlStr + " is not created: mime type is not supported");
        return;
      }
      // document is xml fulltext document: is of type XML and not mets
      if (docIsXml && ! docType.equals("mets")) {
        // replace anchor in echo documents and also add the number attribute to figures
        String docDestFileNameUpgrade = docDestFileName + ".upgrade";
        File docDestFileUpgrade = new File(docDestFileNameUpgrade);
        XslResourceTransformer replaceAnchorTransformer = new XslResourceTransformer("replaceAnchor.xsl");
        String docDestFileUrlStr = docDestFile.getPath();
        String result = replaceAnchorTransformer.transform(docDestFileUrlStr);
        FileUtils.writeStringToFile(docDestFileUpgrade, result, "utf-8");
        
        // generate toc file (toc, figure, handwritten, persons, places, pages)
        XslResourceTransformer tocTransformer = new XslResourceTransformer("toc.xsl");
        File tocFile = new File(docDirName + "/toc.xml");
        String tocResult = tocTransformer.transform(docDestFileNameUpgrade);
        FileUtils.writeStringToFile(tocFile, tocResult, "utf-8");
        String persons = getPersons(tocResult, xQueryEvaluator); 
        String personsDetails = getPersonsDetails(tocResult, xQueryEvaluator); 
        String places = getPlaces(tocResult, xQueryEvaluator);
  
        // Get metadata info out of the xml document
        mdRecord.setPersons(persons);
        mdRecord.setPersonsDetails(personsDetails);
        mdRecord.setPlaces(places);
        docOperation.setStatus("extract metadata of: " + srcUrlStr + " to CMS");
        mdRecord = getMetadataRecord(docDestFileUpgrade, docType, mdRecord, xQueryEvaluator);
        String mdRecordLanguage = mdRecord.getLanguage();
        String langId = Language.getInstance().getLanguageId(mdRecordLanguage); // test if language code is supported
        if (langId == null)
          mdRecordLanguage = null;
        if (mdRecordLanguage == null && mainLanguage != null)
          mdRecord.setLanguage(mainLanguage);
          
        // save all pages as single xml files (untokenized and tokenized)
        docOperation.setStatus("extract page fragments of: " + srcUrlStr + " to CMS");
        File docDir = new File(docDirName + "/pages");
        FileUtils.deleteQuietly(docDir);  // first delete pages directory
        Hashtable<Integer, StringBuilder> pageFragments = getFragments(docDestFileNameUpgrade, "pb");
        int pageCount = pageFragments.size();
        if (pageCount == 0) {
          // no pb element is found: then the whole document is the first page
          String docXmlStr = FileUtils.readFileToString(docDestFileUpgrade, "utf-8");
          docXmlStr = docXmlStr.replaceAll("<\\?xml.*?\\?>", "");  // remove the xml declaration if it exists
          pageFragments = new Hashtable<Integer, StringBuilder>();
          pageFragments.put(new Integer(1), new StringBuilder(docXmlStr));
          pageCount = 1;
        }
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
      } else if (docIsXml && docType.equals("mets")) {
        mdRecord = getMetadataRecord(docDestFile, docType, mdRecord, xQueryEvaluator);
        String mdRecordLanguage = mdRecord.getLanguage();
        String langId = Language.getInstance().getLanguageId(mdRecordLanguage); // test if language code is supported
        if (langId == null)
          mdRecordLanguage = null;
        if (mdRecordLanguage == null && mainLanguage != null)
          mdRecord.setLanguage(mainLanguage);
      } 
      // build the documents fulltext fields
      if (srcUrl != null && docType != null && ! docType.equals("mets") && ! isDbRecord && isText)
        buildFulltextFields(docOperation);
      // perform operation on Lucene
      docOperation.setStatus(operationName + " document: " + docId + " in CMS");
      IndexHandler indexHandler = IndexHandler.getInstance();
      indexHandler.indexDocument(docOperation);
      // write oaiprovider file: has to be after indexing because the id field is set in indexing procedure
      writeOaiproviderFile(mdRecord, xQueryEvaluator);
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
  }
  
  private void deleteCollection(CmsDocOperation docOperation) throws ApplicationException {
    String collectionId = docOperation.getDocIdentifier();  // collectionId
    if (collectionId == null || collectionId.trim().equals(""))
      throw new ApplicationException("Delete collection: Your collection id is empty. Please specify a collection id");
    String documentsDirectory = Constants.getInstance().getDocumentsDir();
    File collectionDir = new File(documentsDirectory + "/" + collectionId);
    // perform operation on Lucene
    IndexHandler indexHandler = IndexHandler.getInstance();
    int countDeletedDocs = indexHandler.deleteCollection(collectionId);
    LOGGER.info(countDeletedDocs + " lucene documents in collection: \"" + collectionId + "\" successfully deleted");

    // perform operation on file system
    docOperation.setStatus("Delete collection directory: " + collectionDir + " in CMS");
    FileUtils.deleteQuietly(collectionDir);
    LOGGER.info("Collection directory: " + collectionDir + " successfully deleted");
    String oaiproviderDirStr = Constants.getInstance().getOaiproviderDir() + "/" + collectionId;
    File oaiproviderDir = new File(oaiproviderDirStr);
    FileUtils.deleteQuietly(oaiproviderDir);
    LOGGER.info("OAI provider directory: " + oaiproviderDirStr + " successfully deleted");
  }
  
  private void writeOaiproviderFile(MetadataRecord mdRecord, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    StringBuilder dcStrBuilder = new StringBuilder();  // Dublin core content string builder
    dcStrBuilder.append("<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">\n");
    String projectUrl = mdRecord.getWebUri();
    if (projectUrl == null)
      projectUrl = mdRecord.getIdentifier();
    if (projectUrl == null)
      projectUrl = mdRecord.getDocId();
    if (projectUrl != null && ! projectUrl.trim().isEmpty()) {
      projectUrl = StringUtils.deresolveXmlEntities(projectUrl);
      dcStrBuilder.append("  <dc:identifier>" + projectUrl + "</dc:identifier>\n");
    }
    String creator = mdRecord.getCreator();
    if (creator != null) {
      creator = StringUtils.deresolveXmlEntities(creator);
      dcStrBuilder.append("  <dc:creator>" + creator + "</dc:creator>\n");
    }
    String title = mdRecord.getTitle();
    if (title != null) {
      title = StringUtils.deresolveXmlEntities(title);
      dcStrBuilder.append("  <dc:title>" + title + "</dc:title>\n");
    }
    String publisher = mdRecord.getPublisher();
    if (publisher != null) {
      publisher = StringUtils.deresolveXmlEntities(publisher);
      dcStrBuilder.append("  <dc:publisher>" + publisher + "</dc:publisher>\n");
    }
    String language = mdRecord.getLanguage();
    if (language != null)
      dcStrBuilder.append("  <dc:language>" + language + "</dc:language>\n");
    String subject = mdRecord.getSubject();
    if (subject != null) {
      subject = StringUtils.deresolveXmlEntities(subject);
      if (subject.contains("###")) {
        String[] subjects = subject.split("###");
        for (int i=0; i<subjects.length; i++) {
          String s = subjects[i];
          dcStrBuilder.append("  <dc:subject>" + s + "</dc:subject>\n");
        }
      } else {
        dcStrBuilder.append("  <dc:subject>" + subject + "</dc:subject>\n");
      }
    }
    String subjectControlledDetails = mdRecord.getSubjectControlledDetails();
    if (subjectControlledDetails != null) {
      String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
      String dcTermsSubjectsStr = xQueryEvaluator.evaluateAsStringValueJoined(subjectControlledDetails, namespaceDeclaration + "/subjects/dcterms:subject/rdf:Description/rdfs:label", "###");
      String[] dcTermsSubjects = dcTermsSubjectsStr.split("###");
      for (int i=0; i<dcTermsSubjects.length; i++) {
        String s = dcTermsSubjects[i].trim();
        s = StringUtils.deresolveXmlEntities(s);
        if (! s.isEmpty())
          dcStrBuilder.append("  <dc:subject>" + s + "</dc:subject>\n");
      }
    }
    Date pubDate = mdRecord.getDate();
    if (pubDate != null) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(pubDate);
      int year = cal.get(Calendar.YEAR);
      int month = cal.get(Calendar.MONTH) + 1;
      String monthStr = String.valueOf(month);
      if (month < 10)
        monthStr = "0" + monthStr;
      int day = cal.get(Calendar.DAY_OF_MONTH);
      String dayStr = String.valueOf(day);
      if (day < 10)
        dayStr = "0" + dayStr;
      String dateStr = year + "-" + monthStr + "-" + dayStr;
      int hours = cal.get(Calendar.HOUR_OF_DAY);
      String hoursStr = String.valueOf(hours);
      if (hours < 10)
        hoursStr = "0" + hoursStr;
      int minutes = cal.get(Calendar.MINUTE);
      String minutesStr = String.valueOf(minutes);
      if (minutes < 10)
        minutesStr = "0" + minutesStr;
      int seconds = cal.get(Calendar.SECOND);
      String secondsStr = String.valueOf(seconds);
      if (seconds < 10)
        secondsStr = "0" + secondsStr;
      String timeStr = hoursStr + ":" + minutesStr + ":" + secondsStr;
      dcStrBuilder.append("  <dc:date>" + dateStr + "T" + timeStr + "Z" + "</dc:date>\n"); // e.g. "2013-07-03T12:09:59Z"
    }
    String type = "Text";
    dcStrBuilder.append("  <dc:type>" + type + "</dc:type>\n");
    String rights = mdRecord.getRights();
    if (rights != null)
      dcStrBuilder.append("  <dc:rights>" + rights + "</dc:rights>\n"); 
    dcStrBuilder.append("</oai_dc:dc>\n");
    String dcStr = dcStrBuilder.toString();
    String collectionId = mdRecord.getCollectionNames();
    String oaiproviderDirStr = Constants.getInstance().getOaiproviderDir();
    int id = mdRecord.getId();
    String docId = mdRecord.getDocId();
    String fileIdentifier = docId;
    if (id != -1)
      fileIdentifier = "" + id;
    String oaiDcFileName = fileIdentifier + "_oai_dc.xml";
    File oaiDcFile = new File(oaiproviderDirStr + "/" + collectionId + "/" + oaiDcFileName);
    try {
      FileUtils.writeStringToFile(oaiDcFile, dcStr, "utf-8");
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void buildFulltextFields(CmsDocOperation docOperation) throws ApplicationException {
    try {
      MetadataRecord mdRecord = docOperation.getMdRecord();
      String docId = mdRecord.getDocId();
      String language = mdRecord.getLanguage();
      boolean docIsXml = isDocXml(docId);
      String docTokensOrig = null;
      String docTokensNorm = null;
      String docTokensMorph = null;
      String contentXml = null;
      String content = null;
      String docFileName = getDocFullFileName(docId);
      XmlTokenizer docXmlTokenizer = null;
      if (docIsXml) {
        docFileName = getDocFullFileName(docId) + ".upgrade";
        // remove the header part of the xml fulltext so that is is not indexed in Lucene
        XslResourceTransformer removeElemTransformer = new XslResourceTransformer("removeElement.xsl");
        QName elemNameQName = new QName("elemName");
        XdmValue elemNameXdmValue = new XdmAtomicValue("teiHeader");  // TODO fetch name from collection configuration
        removeElemTransformer.setParameter(elemNameQName, elemNameXdmValue);
        String contentXmlRemovedHeader = removeElemTransformer.transform(docFileName);
        File docFile = new File(docFileName);
        FileUtils.writeStringToFile(docFile, contentXmlRemovedHeader, "utf-8");
        InputStreamReader docFileReader = new InputStreamReader(new FileInputStream(docFileName), "utf-8");
        // to guarantee that utf-8 is used (if not done, it does not work on Tomcat which has another default charset)
        docXmlTokenizer = new XmlTokenizer(docFileReader);
        docXmlTokenizer.setDocIdentifier(docId);
        docXmlTokenizer.setLanguage(language);
        docXmlTokenizer.setOutputFormat("string");
        String[] outputOptionsWithLemmas = { "withLemmas" }; // so all tokens are fetched with lemmas (costs performance)
        docXmlTokenizer.setOutputOptions(outputOptionsWithLemmas);
        String[] normFunctionNone = { "none" };
        docXmlTokenizer.setNormFunctions(normFunctionNone);
        docXmlTokenizer.tokenize();
  
        int pageCount = docXmlTokenizer.getPageCount();
        if (pageCount <= 0)
          pageCount = 1;  // each document at least has one page
  
        String[] outputOptionsEmpty = {};
        docXmlTokenizer.setOutputOptions(outputOptionsEmpty); 
        // must be set to null so that the normalization function works
        docTokensOrig = docXmlTokenizer.getStringResult();
        String[] normFunctionNorm = { "norm" };
        docXmlTokenizer.setNormFunctions(normFunctionNorm);
        docTokensNorm = docXmlTokenizer.getStringResult();
        docXmlTokenizer.setOutputOptions(outputOptionsWithLemmas);
        docTokensMorph = docXmlTokenizer.getStringResult();
        // fetch original xml content of the documents file
        contentXml = FileUtils.readFileToString(docFile, "utf-8");
        // fetch original content of the documents file (without xml tags)
        XslResourceTransformer charsTransformer = new XslResourceTransformer("chars.xsl");
        content = charsTransformer.transform(docFileName);
        // get elements from xml tokenizer 
        String[] elementNamesArray = docOperation.getElementNames();
        String elementNames = "";
        for (int i = 0; i < elementNamesArray.length; i++) {
          String elemName = elementNamesArray[i];
          elementNames = elementNames + elemName + " ";
        }
        elementNames = elementNames.substring(0, elementNames.length() - 1);
        ArrayList<XmlTokenizerContentHandler.Element> xmlElements = docXmlTokenizer.getElements(elementNames);
        // fill mdRecord
        mdRecord.setTokenOrig(docTokensOrig);
        mdRecord.setTokenNorm(docTokensNorm);
        mdRecord.setTokenMorph(docTokensMorph);
        mdRecord.setContentXml(contentXml);
        mdRecord.setContent(content);
        mdRecord.setXmlElements(xmlElements);
        mdRecord.setPageCount(pageCount);
      } else {
        DocumentParser tikaParser = new DocumentParser();
        try {
          IDocument tikaDoc = tikaParser.parse(docFileName);
          docTokensOrig = tikaDoc.getTextOrig();
          MetadataRecord tikaMDRecord = tikaDoc.getMetadata();
          if (tikaMDRecord != null) {
            int pageCount = tikaMDRecord.getPageCount();
            if (pageCount <= 0)
              pageCount = 1;  // each document at least has one page
            mdRecord.setPageCount(pageCount);
            if (mdRecord.getIdentifier() == null)
              mdRecord.setIdentifier(tikaMDRecord.getIdentifier());
            if (mdRecord.getCreator() == null) {
              String mimeType = mdRecord.getType();
              if (mimeType != null && mimeType.equals("application/pdf")) {
                // nothing: creator is not the creator of the document in mostly all pdf-documents, so is not considered
              } else {
                String tikaCreator = tikaMDRecord.getCreator();
                boolean isProper = isProper("author", tikaCreator);
                if (isProper)
                  mdRecord.setCreator(tikaCreator);
              }
            }
            if (mdRecord.getTitle() == null) {
              String mimeType = mdRecord.getType();
              if (mimeType != null && mimeType.equals("application/pdf")) {
                // nothing: title is not the title of the document in mostly all pdf-documents, so is not considered
              } else {
                mdRecord.setTitle(tikaMDRecord.getTitle());
              }
            }
            if (mdRecord.getLanguage() == null) {
              String tikaLang = tikaMDRecord.getLanguage();
              if (tikaLang != null) {
                String isoLang = Language.getInstance().getISO639Code(tikaLang);
                mdRecord.setLanguage(isoLang);
              }
            }
            if (mdRecord.getPublisher() == null)
              mdRecord.setPublisher(tikaMDRecord.getPublisher());
            if (mdRecord.getDate() == null)
              mdRecord.setDate(tikaMDRecord.getDate());
            if (mdRecord.getDescription() == null)
              mdRecord.setDescription(tikaMDRecord.getDescription());
            if (mdRecord.getContributor() == null)
              mdRecord.setContributor(tikaMDRecord.getContributor());
            if (mdRecord.getCoverage() == null)
              mdRecord.setCoverage(tikaMDRecord.getCoverage());
            if (mdRecord.getSubject() == null)
              mdRecord.setSubject(tikaMDRecord.getSubject());
            if (mdRecord.getSwd() == null)
              mdRecord.setSwd(tikaMDRecord.getSwd());
            if (mdRecord.getDdc() == null)
              mdRecord.setDdc(tikaMDRecord.getDdc());
            if (mdRecord.getEncoding() == null)
              mdRecord.setEncoding(tikaMDRecord.getEncoding());
          }
        } catch (ApplicationException e) {
          LOGGER.error(e.getLocalizedMessage());
        }
        String mdRecordLanguage = mdRecord.getLanguage();
        String mainLanguage = docOperation.getMainLanguage();
        if (mdRecordLanguage == null && mainLanguage != null)
          mdRecord.setLanguage(mainLanguage);
        String lang = mdRecord.getLanguage();
        DocumentTokenizer docTokenizer = DocumentTokenizer.getInstance();
        String[] normFunctions = {"norm"};
        ArrayList<Token> normTokens = docTokenizer.getToken(docTokensOrig, lang, normFunctions);
        docTokensNorm = docTokenizer.buildStr(normTokens, lang, "norm");
        docTokensMorph = docTokenizer.buildStr(normTokens, lang, "morph");
        content = docTokensOrig;  // content is the same as docTokensOrig
        // fill mdRecord
        mdRecord.setTokenOrig(docTokensOrig);
        mdRecord.setTokenNorm(docTokensNorm);
        mdRecord.setTokenMorph(docTokensMorph);
        mdRecord.setContent(content);
      }
      DBpediaSpotlightHandler dbPediaSpotlightHandler = DBpediaSpotlightHandler.getInstance(); 
      Annotation docContentAnnotation = null;
      Annotation docTitleAnnotation = null;
      try {
        String docContent = content; // docTokensOrig;
        if (docTokensOrig == null && mdRecord.getDescription() != null && ! mdRecord.getDescription().isEmpty())
          docContent = mdRecord.getDescription();
        docContentAnnotation = dbPediaSpotlightHandler.annotate(docId, docContent, "0.99", 50);
        String docTitle = mdRecord.getTitle();
        if (docTitle != null)
          docTitleAnnotation = dbPediaSpotlightHandler.annotate(docId, docTitle, "0.5", 50);
      } catch (ApplicationException e) {
        LOGGER.error(e);
      }
      if (docContentAnnotation != null && ! docContentAnnotation.isEmpty()) {
        List<DBpediaResource> resources = docContentAnnotation.getResources();
        if (docTitleAnnotation != null) {
          List<DBpediaResource> docTitleResources = docTitleAnnotation.getResources();
          if (docTitleResources != null) {
            for (int i=docTitleResources.size()-1; i>=0; i--) {
              DBpediaResource r = docTitleResources.get(i);
              if (! resources.contains(r))
                resources.add(0, r); // add title entities at the beginning
            }
          }
        }
        String collName = mdRecord.getCollectionNames();
        Collection coll = CollectionReader.getInstance().getCollection(collName);
        String collRdfId = coll.getRdfId();
        if (resources != null) {
          StringBuilder dbPediaResourceNamesStrBuilder = new StringBuilder();
          StringBuilder dbPediaResourcesXmlStrBuilder = new StringBuilder();
          dbPediaResourcesXmlStrBuilder.append("<resources>\n");
          StringBuilder dbPediaSpotlightCollectionRdfStrBuilder = coll.getDbPediaSpotlightRdfStrBuilder();
          if (mdRecord.isEdocRecord()) {
            Collection edocColl = CollectionReader.getInstance().getCollection("edoc");
            dbPediaSpotlightCollectionRdfStrBuilder = edocColl.getDbPediaSpotlightRdfStrBuilder();
          }
          String uri = mdRecord.getUri();
          String webUri = mdRecord.getWebUri();
          if (webUri != null)
            uri = webUri;
          uri = StringUtils.deresolveXmlEntities(uri);
          if (dbPediaSpotlightCollectionRdfStrBuilder != null) {
            dbPediaSpotlightCollectionRdfStrBuilder.append("<rdf:Description rdf:about=\"" + uri + "\">\n");
            dbPediaSpotlightCollectionRdfStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
            dbPediaSpotlightCollectionRdfStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + collRdfId + "\"/>\n");
            dbPediaSpotlightCollectionRdfStrBuilder.append("  <dc:identifier rdf:resource=\"" + uri + "\"/>\n");
          }
          for (int i=0; i<resources.size(); i++) {
            DBpediaResource r = resources.get(i);
            dbPediaResourcesXmlStrBuilder.append(r.toXmlStr());
            if (dbPediaSpotlightCollectionRdfStrBuilder != null) {
              dbPediaSpotlightCollectionRdfStrBuilder.append(r.toRdfStr());
            }
            String resourceName = r.getName();
            if (i == resources.size() - 1)
              dbPediaResourceNamesStrBuilder.append(resourceName);
            else
              dbPediaResourceNamesStrBuilder.append(resourceName + "###");
          }
          if (dbPediaSpotlightCollectionRdfStrBuilder != null) {
            dbPediaSpotlightCollectionRdfStrBuilder.append("</rdf:Description>\n");
          }
          dbPediaResourcesXmlStrBuilder.append("</resources>\n");
          mdRecord.setEntities(dbPediaResourceNamesStrBuilder.toString());
          mdRecord.setEntitiesDetails(dbPediaResourcesXmlStrBuilder.toString());
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  private MetadataRecord getMetadataRecord(File xmlFile, String schemaName, MetadataRecord mdRecord, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    if (schemaName == null)
      return mdRecord;
    try {
      URL srcUrl = xmlFile.toURI().toURL();
      if (schemaName.equals("TEI"))
        mdRecord = getMetadataRecordTei(xQueryEvaluator, srcUrl, mdRecord);
      else if (schemaName.equals("mets"))
        mdRecord = getMetadataRecordMets(xQueryEvaluator, srcUrl, mdRecord);
      else if (schemaName.equals("html"))
        mdRecord = getMetadataRecordHtml(xQueryEvaluator, srcUrl, mdRecord);
      else
        mdRecord.setSchemaName(schemaName); // all other cases: set docType to schemaName
      evaluateXQueries(xQueryEvaluator, srcUrl, mdRecord);
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecord;
  }

  private MetadataRecord evaluateXQueries(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) {
    Hashtable<String, XQuery> xqueriesHashtable = mdRecord.getxQueries();
    if (xqueriesHashtable != null) {
      Enumeration<String> keys = xqueriesHashtable.keys();
      while (keys != null && keys.hasMoreElements()) {
        String key = keys.nextElement();
        XQuery xQuery = xqueriesHashtable.get(key);
        String xQueryCode = xQuery.getCode();
        try {
          String xQueryResult = xQueryEvaluator.evaluateAsString(srcUrl, xQueryCode);
          if (xQueryResult != null)
            xQueryResult = xQueryResult.trim();
          xQuery.setResult(xQueryResult);
        } catch (Exception e) {
          // nothing
        }
      }
    }
    return mdRecord;
  }
  
  private MetadataRecord getMetadataRecordTei(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/*:TEI/*:teiHeader");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:idno");
      if (identifier != null)
        identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      String creator = null;
      String creatorDetails = null;
      XdmValue xmdValueAuthors = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:author");
      if (xmdValueAuthors != null && xmdValueAuthors.size() > 0) {
        XdmSequenceIterator xmdValueAuthorsIterator = xmdValueAuthors.iterator();
        ArrayList<Person> authors = new ArrayList<Person>();
        while (xmdValueAuthorsIterator.hasNext()) {
          Person author = new Person();
          XdmItem xdmItemAuthor = xmdValueAuthorsIterator.next();
          String xdmItemAuthorStr = xdmItemAuthor.toString();
          String name = xdmItemAuthor.getStringValue();
          boolean isProper = isProper("author", name);
          if (! isProper)
            name = "";
          if (name != null) {
            name = name.replaceAll("\n|\\s\\s+", " ").trim();
          }
          String reference = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:author/*:persName/@ref)");
          if (reference != null && ! reference.isEmpty())
            author.setRef(reference);
          String forename = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:author/*:persName/*:forename)");
          if (forename != null && ! forename.isEmpty())
            author.setForename(forename.trim());
          String surname = xQueryEvaluator.evaluateAsString(xdmItemAuthorStr, "string(/*:author/*:persName/*:surname)");
          if (surname != null && ! surname.isEmpty())
            author.setSurname(surname.trim());
          if (surname != null && ! surname.isEmpty() && forename != null && ! forename.isEmpty())
            name = surname.trim() + ", " + forename.trim();
          else if (surname != null && ! surname.isEmpty() && (forename == null || forename.isEmpty()))
            name = surname.trim();
          if (! name.isEmpty()) {
            author.setName(name);
            authors.add(author);
          }
        }
        creator = "";
        creatorDetails = "<persons>";
        for (int i=0; i<authors.size(); i++) {
          Person author = authors.get(i);
          creator = creator + author.getName();
          if (authors.size() != 1)
            creator = creator + ". ";
          creatorDetails = creatorDetails + "\n" + author.toXmlStr();
        }
        creator = StringUtils.deresolveXmlEntities(creator.trim());
        creatorDetails = creatorDetails + "\n</persons>";
        if (creator.isEmpty())
          creator = null;
        if (authors.isEmpty())
          creatorDetails = null;
      }
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title");
      XdmValue xdmValueTitleIdno = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title/*:idno"); // e.g. in http://telotadev.bbaw.de:8078/exist/rest/db/WvH/Briefe/1827-02-20_RosenFriedrichAugust-WvH_481.xml
      if (xdmValueTitleIdno != null && xdmValueTitleIdno.size() > 0)
        title = xQueryEvaluator.evaluateAsString(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title/text()");
      XdmValue xdmValueTitleMain = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title[@type='main']");
      XdmValue xdmValueTitleShort = xQueryEvaluator.evaluate(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:titleStmt/*:title[@type='short']");
      if (xdmValueTitleShort != null && xdmValueTitleShort.size() > 0) {
        try {
          title = xdmValueTitleShort.getUnderlyingValue().getStringValue();
        } catch (XPathException e) {
          // nothing
        }
      } else if (xdmValueTitleMain != null && xdmValueTitleMain.size() > 0) {
        try {
          title = xdmValueTitleMain.getUnderlyingValue().getStringValue();
        } catch (XPathException e) {
          // nothing
        }
      }
      if (title != null) {
        title = StringUtils.deresolveXmlEntities(title.trim());
        if (title.isEmpty())
          title = null;
      }
      String language = null;
      String countLanguages = xQueryEvaluator.evaluateAsString(metadataXmlStr, "string(count(/*:teiHeader/*:profileDesc/*:langUsage/*:language))");
      if (countLanguages != null && countLanguages.equals("1"))
        language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/*:teiHeader/*:profileDesc/*:langUsage/*:language[1]/@ident)");
      if (language != null && language.isEmpty())
        language = null;
      if (language != null) {
        language = StringUtils.deresolveXmlEntities(language.trim());
        language = Language.getInstance().getISO639Code(language);
      }
      String publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:publisher/*:name", ", ");
      if (publisher == null || publisher.isEmpty())
        publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:publisher", ", ");
      if (publisher == null || publisher.isEmpty())
        publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:publisher", ", ");
      if (publisher == null || publisher.isEmpty())
        publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:bibl/*:publisher", ", ");
      if (publisher != null) {
        publisher = StringUtils.deresolveXmlEntities(publisher.trim());
        if (publisher.isEmpty())
          publisher = null;
      }
      String place = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:pubPlace", ", ");
      if (place == null || place.isEmpty())
        place = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:pubPlace");
      if (place != null) {
        place = StringUtils.deresolveXmlEntities(place.trim());
        if (place.isEmpty())
          place = null;
      }
      String publisherStr = null;
      boolean publisherEndsWithComma = false;
      if (publisher != null)
        publisherEndsWithComma = publisher.lastIndexOf(",") == publisher.length() - 1;
      if (publisher == null && place != null)
        publisherStr = place;
      else if (publisher != null && place == null)
        publisherStr = publisher;
      else if (publisher != null && place != null && publisherEndsWithComma)
        publisherStr = publisher + " " + place;
      else if (publisher != null && place != null && ! publisherEndsWithComma)
        publisherStr = publisher + ", " + place;
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:biblFull/*:publicationStmt/*:date");
      if (yearStr == null || yearStr.isEmpty())
        yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:date");
      if (yearStr == null || yearStr.isEmpty())
        yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:sourceDesc/*:bibl/*:date", ", ");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr.trim());
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null) {
          try {
            date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          } catch (Exception e) {
            // nothing
          }
        }
      }
      String subject = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:profileDesc/*:textClass/*:keywords/*:term");
      if (subject != null) {
        subject = StringUtils.deresolveXmlEntities(subject.trim());
      }
      String rights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "/*:teiHeader/*:fileDesc/*:publicationStmt/*:availability");
      if (rights == null || rights.trim().isEmpty())
        rights = "open access";
      rights = StringUtils.deresolveXmlEntities(rights.trim());
      String license = "http://echo.mpiwg-berlin.mpg.de/policy/oa_basics/declaration";
      String accessRights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/*:teiHeader/*:fileDesc/*:publicationStmt/*:availability/@status)");
      if (accessRights == null || accessRights.trim().isEmpty()) 
        accessRights = "free";
      accessRights = StringUtils.deresolveXmlEntities(accessRights.trim());
      mdRecord.setIdentifier(identifier);
      // set language onto mdRecord if it is not null else the mdReord language beware its language 
      if (language != null)
        mdRecord.setLanguage(language);
      if (mdRecord.getCreator() == null)
        mdRecord.setCreator(creator);
      if (mdRecord.getCreatorDetails() == null)
        mdRecord.setCreatorDetails(creatorDetails);
      if (mdRecord.getTitle() == null)
        mdRecord.setTitle(title);
      if (mdRecord.getPublisher() == null)
        mdRecord.setPublisher(publisherStr);
      if (mdRecord.getRights() == null)
        mdRecord.setRights(rights);
      if (mdRecord.getDate() == null)
        mdRecord.setDate(date);
      if (mdRecord.getSubject() == null)
        mdRecord.setSubject(subject);
      if (mdRecord.getLicense() == null)
        mdRecord.setLicense(license);
      if (mdRecord.getAccessRights() == null)
        mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//*:pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("TEI");
    return mdRecord;
  }

  private MetadataRecord getMetadataRecordMets(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStrDmd = xQueryEvaluator.evaluateAsString(srcUrl, "/*:mets/*:dmdSec");
    String metadataXmlStrAmd = xQueryEvaluator.evaluateAsString(srcUrl, "/*:mets/*:amdSec");
    if (metadataXmlStrDmd != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "string(/*:dmdSec/@ID)");
      if (identifier != null)
        identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      String webUri = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:digiprovMD/*:mdWrap/*:xmlData/*:links/*:presentation");
      if (webUri != null)
        webUri = StringUtils.deresolveXmlEntities(webUri.trim());
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:name");
      if (creator != null) {
        creator = StringUtils.deresolveXmlEntities(creator.trim());
        if (creator.isEmpty())
          creator = null;
      }
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:titleInfo");
      if (title != null)
        title = StringUtils.deresolveXmlEntities(title.trim());
      String language = null;  // TODO
      // String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "string(/*:teiHeader/*:profileDesc/*:langUsage/*:language[1]/@ident)");
      // if (language != null && language.isEmpty())
      //   language = null;
      // if (language != null) {
      //   language = StringUtils.deresolveXmlEntities(language.trim());
      //   language = Language.getInstance().getISO639Code(language);
      // }
      String publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:rightsMD/*:mdWrap/*:xmlData/*:rights/*:owner", ", ");
      if (publisher != null)
        publisher = StringUtils.deresolveXmlEntities(publisher.trim());
      String place = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:originInfo/*:place/*:placeTerm", ", ");
      if (place != null)
        place = StringUtils.deresolveXmlEntities(place.trim());
      String publisherStr = null;
      boolean publisherEndsWithComma = false;
      if (publisher != null)
        publisherEndsWithComma = publisher.lastIndexOf(",") == publisher.length() - 1;
      if (publisher == null && place != null)
        publisherStr = place;
      else if (publisher != null && place == null)
        publisherStr = publisher;
      else if (publisher != null && place != null && publisherEndsWithComma)
        publisherStr = publisher + " " + place;
      else if (publisher != null && place != null && ! publisherEndsWithComma)
        publisherStr = publisher + ", " + place;
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:dmdSec/*:mdWrap/*:xmlData/*:mods/*:originInfo/*:dateIssued");
      Date date = null; 
      if (yearStr != null && ! yearStr.equals("")) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr.trim());
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null) {
          try {
            date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          } catch (Exception e) {
            // nothing
          }
        }
      }
      String subject = null;  // TODO
      // String subject = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrDmd, "/*:teiHeader/*:profileDesc/*:textClass/*:keywords/*:term");
      // if (subject != null)
      //   subject = StringUtils.deresolveXmlEntities(subject.trim());
      String rightsOwner = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:rightsMD/*:mdWrap/*:xmlData/*:rights/*:owner");
      String rightsOwnerUrl = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStrAmd, "/*:amdSec/*:rightsMD/*:mdWrap/*:xmlData/*:rights/*:ownerSiteURL");
      String rights = null;
      if (rightsOwner != null)
        rights = StringUtils.deresolveXmlEntities(rightsOwner.trim());
      if (rightsOwner != null && rightsOwnerUrl != null)
        rights = rights + ", " + StringUtils.deresolveXmlEntities(rightsOwnerUrl.trim());
      else if (rightsOwner == null && rightsOwnerUrl != null)
        rights = StringUtils.deresolveXmlEntities(rightsOwnerUrl.trim());
      mdRecord.setIdentifier(identifier);
      mdRecord.setWebUri(webUri);
      mdRecord.setLanguage(language);
      if (mdRecord.getCreator() == null)
        mdRecord.setCreator(creator);
      if (mdRecord.getTitle() == null)
        mdRecord.setTitle(title);
      if (mdRecord.getPublisher() == null)
        mdRecord.setPublisher(publisherStr);
      if (mdRecord.getRights() == null)
        mdRecord.setRights(rights);
      if (mdRecord.getDate() == null)
        mdRecord.setDate(date);
      if (mdRecord.getSubject() == null)
        mdRecord.setSubject(subject);
    }
    mdRecord.setSchemaName("mets");
    return mdRecord;
  }

  private MetadataRecord getMetadataRecordHtml(XQueryEvaluator xQueryEvaluator, URL srcUrl, MetadataRecord mdRecord) throws ApplicationException {
    String metadataXmlStr = xQueryEvaluator.evaluateAsString(srcUrl, "/html/head");
    if (metadataXmlStr != null) {
      String identifier = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.identifier']/@content)");
      if (identifier != null && ! identifier.isEmpty())
        identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      String creator = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.creator']/@content)");
      boolean isProper = isProper("author", creator);
      if (! isProper)
        creator = "";
      if (creator != null) {
        creator = StringUtils.deresolveXmlEntities(creator.trim());
        if (creator.isEmpty())
          creator = null;
      }      
      String title = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.title']/@content)");
      if (title == null || title.isEmpty()) {
        title = xQueryEvaluator.evaluateAsString(metadataXmlStr, "string(/head/title)");
      }
      if (title != null) {
        title = StringUtils.deresolveXmlEntities(title.trim());
        if (title.isEmpty())
          title = null;
      }
      String language = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.language']/@content)");
      if (language != null && language.isEmpty())
        language = null;
      if (language != null && ! language.isEmpty()) {
        language = StringUtils.deresolveXmlEntities(language.trim());
        language = Language.getInstance().getISO639Code(language);
      }
      String publisher = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.publisher']/@content)");
      if (publisher != null) {
        publisher = StringUtils.deresolveXmlEntities(publisher.trim());
        if (publisher.isEmpty())
          publisher = null;
      }
      String yearStr = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.date']/@content)");
      Date date = null; 
      if (yearStr != null) {
        yearStr = StringUtils.deresolveXmlEntities(yearStr.trim());
        yearStr = new Util().toYearStr(yearStr);  // test if possible etc
        if (yearStr != null) {
          try {
            date = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          } catch (Exception e) {
            // nothing
          }
        }
      }
      String subject = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.subject']/@content)");
      if (subject != null) {
        subject = StringUtils.deresolveXmlEntities(subject.trim());
        if (subject.isEmpty())
          subject = null;
      }
      String description = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.description']/@content)");
      if (description != null) {
        description = StringUtils.deresolveXmlEntities(description.trim());
        if (description.isEmpty())
          description = null;
      }
      String rights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.rights']/@content)");
      if (rights != null) {
        rights = StringUtils.deresolveXmlEntities(rights.trim());
        if (rights.isEmpty())
          rights = null;
      }
      String license = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.license']/@content)");
      if (license != null) {
        license = StringUtils.deresolveXmlEntities(license.trim());
        if (license.isEmpty())
          license = null;
      }
      String accessRights = xQueryEvaluator.evaluateAsStringValueJoined(metadataXmlStr, "string(/head/meta[@name = 'DC.accessRights']/@content)");
      if (accessRights != null) {
        accessRights = StringUtils.deresolveXmlEntities(accessRights.trim());
        if (accessRights.isEmpty())
          accessRights = null;
      }
      mdRecord.setIdentifier(identifier);
      mdRecord.setLanguage(language);
      if (mdRecord.getCreator() == null)
        mdRecord.setCreator(creator);
      if (mdRecord.getTitle() == null)
        mdRecord.setTitle(title);
      if (mdRecord.getPublisher() == null)
        mdRecord.setPublisher(publisher);
      if (mdRecord.getRights() == null)
        mdRecord.setRights(rights);
      if (mdRecord.getDate() == null)
        mdRecord.setDate(date);
      if (mdRecord.getSubject() == null)
        mdRecord.setSubject(subject);
      if (mdRecord.getDescription() == null)
        mdRecord.setDescription(description);
      if (mdRecord.getLicense() == null)
        mdRecord.setLicense(license);
      if (mdRecord.getAccessRights() == null)
        mdRecord.setAccessRights(accessRights);
    }
    String pageCountStr = xQueryEvaluator.evaluateAsString(srcUrl, "count(//pb)");
    int pageCount = Integer.valueOf(pageCountStr);
    mdRecord.setPageCount(pageCount);
    mdRecord.setSchemaName("html");
    return mdRecord;
  }

  private String getPersons(String tocString, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    String persons = xQueryEvaluator.evaluateAsStringValueJoined(tocString, "/list/list[@type='persons']/item/person/name[not(. = preceding::person/name)]", "###"); // [not(. = preceding::person/name)] removes duplicates
    return persons;
  }
  
  private String getPersonsDetails(String tocString, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    String personsDetails = null;
    XdmValue xmdValuePersons = xQueryEvaluator.evaluate(tocString, "/list/list[@type='persons']/item/person[not(name = preceding::person/name)]"); // [not(name = preceding::person/name)] removes duplicates
    if (xmdValuePersons != null && xmdValuePersons.size() > 0) {
      XdmSequenceIterator xmdValuePersonsIterator = xmdValuePersons.iterator();
      personsDetails = "<persons>\n";
      while (xmdValuePersonsIterator.hasNext()) {
        XdmItem xdmItemPerson = xmdValuePersonsIterator.next();
        String xdmItemPersonStr = xdmItemPerson.toString();
        personsDetails = personsDetails + xdmItemPersonStr + "\n";
      }
      personsDetails = personsDetails + "</persons>";
    }
    return personsDetails;
  }
  
  private String getPlaces(String tocString, XQueryEvaluator xQueryEvaluator) throws ApplicationException {
    String places = xQueryEvaluator.evaluateAsStringValueJoined(tocString, "/list/list[@type='places']/item[not(. = preceding::item)]", "###"); 
    return places;
  }

  private boolean isProper(String fieldName, String fieldValue) {
    if (fieldValue == null)
      return true;
    boolean isProper = true;
    if (fieldName != null && fieldName.equals("author") && (fieldValue.equals("admin") || fieldValue.equals("user")))
      return false;
    return isProper;
  }

  private boolean copyUrlToFile(URL srcUrl, File docDestFile) {
    try {
      if (srcUrl != null) {
        FileUtils.copyURLToFile(srcUrl, docDestFile, 20000, 20000);
      }
    } catch (UnknownHostException e) {
      LOGGER.error("1. try: copyUrlToFile failed. UnknownHostException: " + e.getMessage() + " for Url: " + srcUrl);
      try {
        Thread.sleep(10000); // 10 secs delay
        FileUtils.copyURLToFile(srcUrl, docDestFile, 20000, 20000);
      } catch (IOException | InterruptedException e2) {
        LOGGER.error("2. try: copyUrlToFile failed. " + e2.getClass().getName() + " : " + e.getMessage() + " for Url: " + srcUrl);
        try {
          Thread.sleep(10000); // another 10 secs delay
          FileUtils.copyURLToFile(srcUrl, docDestFile, 20000, 20000);
        } catch (IOException | InterruptedException e3) {
          LOGGER.error("3. try: copyUrlToFile failed. " + e3.getClass().getName() + " : " + e.getMessage() + " for Url: " + srcUrl);
          FileUtils.deleteQuietly(docDestFile);
          return false;
        }
      }
    } catch (SocketTimeoutException e) {
      LOGGER.error("copyUrlToFile failed. Socket timeout for: " + srcUrl);
      FileUtils.deleteQuietly(docDestFile);
      return false;
    } catch (IOException e) {
      try {
        // second try with encoded blanks url
        String srcUrlFileEncodedStr = srcUrl.getFile().replaceAll(" ", "%20");
        String srcUrlEncodedStr = srcUrl.getProtocol() + "://" + srcUrl.getHost() + ":" + srcUrl.getPort() + srcUrlFileEncodedStr;
        URL srcUrlEncoded = new URL(srcUrlEncodedStr);
        FileUtils.copyURLToFile(srcUrlEncoded, docDestFile, 20000, 20000);
      } catch (IOException e2) {
        LOGGER.error("copyUrlToFile failed. IOException: " + e.getMessage() + " for Url: " + srcUrl);
        FileUtils.deleteQuietly(docDestFile);
        return false;
      }
    }
    return true;
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
    if (docId == null || docId.trim().isEmpty())
      return null;
    String docDir = getDocDir(docId);
    String docFileName = getDocFileName(docId);
    String docFullFileName = docDir + "/" + docFileName; 
    return docFullFileName;
  }
  
  public boolean isDocXml(String docId) {
    boolean isXml = false;
    String fileExt = getDocFileExtension(docId);
    if (fileExt != null) {
      fileExt = fileExt.toLowerCase();
      if (fileExt.equals("xml"))
        isXml = true;
    }
    return isXml;
  }

  public boolean isText(String mimeType) {
    boolean isText = true;
    if (mimeType != null && mimeType.contains("image"))
      return false;
    return isText;
  }

  public String getMimeType(String docId) {
    String mimeType = null;
    String fileName = getDocFileName(docId);
    if (fileName != null) {
      fileName = fileName.toLowerCase();
      FileNameMap fileNameMap = URLConnection.getFileNameMap();
      mimeType = fileNameMap.getContentTypeFor(fileName);
    }
    return mimeType;
  }

  public String getDocDir(String docId) {
    if (docId == null || docId.trim().isEmpty())
      return null;
    String documentsDirectory = Constants.getInstance().getDocumentsDir();
    String docDir = documentsDirectory;
    String docFileName = getDocFileName(docId);
    String docFilePath = getDocFilePath(docId);
    if (docFilePath != null)
      docDir = docDir + docFilePath;
    if (docFileName != null)
      docDir = docDir + "/" + docFileName;
    else
      docDir = docDir + "/" + "XXXXX";
    return docDir;
  }
  
  private String getDocFileName(String docId) {
    String docFileName = docId.trim();
    int index = docId.lastIndexOf("/");
    if (index != -1) {
      docFileName = docId.substring(index + 1);
    }
    return docFileName;
  }
  
  private String getDocFileExtension(String docId) {
    docId = docId.trim();
    String fileExt = null;
    int index = docId.lastIndexOf(".");
    if (index != -1) {
      fileExt = docId.substring(index + 1);
    }
    return fileExt;
  }
  
  private String getDocFilePath(String docId) {
    String docFilePath = null;
    int index = docId.lastIndexOf("/");
    if (index >= 0) {
      docFilePath = docId.substring(0, index);
    }
    if (docFilePath != null && ! docFilePath.startsWith("/"))
      docFilePath = docFilePath + "/";
    return docFilePath;
  }

  private Hashtable<Integer, StringBuilder> getFragments(String fileName, String milestoneElementName) throws ApplicationException {
    try {
      GetFragmentsContentHandler getFragmentsContentHandler = new GetFragmentsContentHandler(milestoneElementName);
      XMLReader xmlParser = new SAXParser();
      xmlParser.setContentHandler(getFragmentsContentHandler);
      InputSource inputSource = new InputSource("file:" + fileName);
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
    String[] nwbElements = {"lb", "br", "cb"};  // non word breaking elements // TODO: "hi" cause bug
    xmlTokenizer.setNWBElements(nwbElements);
    xmlTokenizer.setOutputOptions(outputOptionsWithLemmas); 
    xmlTokenizer.tokenize();  
    String retStr = xmlTokenizer.getXmlResult();
    return retStr;
  }
  
}