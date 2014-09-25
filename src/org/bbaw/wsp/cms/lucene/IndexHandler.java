package org.bbaw.wsp.cms.lucene;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.facet.index.CategoryDocumentBuilder;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.search.suggest.tst.TSTLookup;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.bbaw.wsp.cms.collections.Collection;
import org.bbaw.wsp.cms.collections.CollectionReader;
import org.bbaw.wsp.cms.document.Facets;
import org.bbaw.wsp.cms.document.Hits;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Token;
import org.bbaw.wsp.cms.document.TokenArrayListIterator;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;
import org.bbaw.wsp.cms.translator.LanguageHandler;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.dict.db.LexHandler;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.Form;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.Lemma;
import de.mpg.mpiwg.berlin.mpdl.lt.text.norm.Normalizer;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizer;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class IndexHandler {
  private static IndexHandler instance;
  private static Logger LOGGER = Logger.getLogger(IndexHandler.class);
  private static final String[] QUERY_EXPANSION_FIELDS_ALL = {"author", "title", "description", "subject", "subjectControlled", "swd", "ddc", "persons", "places", "tokenOrig"};
  private static final String[] QUERY_EXPANSION_FIELDS_ALL_MORPH = {"author", "title", "description", "subject", "subjectControlled", "swd", "ddc", "persons", "places", "tokenMorph"};
  private IndexWriter documentsIndexWriter;
  private IndexWriter nodesIndexWriter;
  private SearcherManager documentsSearcherManager;
  private SearcherManager nodesSearcherManager;
  private IndexReader documentsIndexReader;
  private PerFieldAnalyzerWrapper documentsPerFieldAnalyzer;
  private PerFieldAnalyzerWrapper nodesPerFieldAnalyzer;
  private ArrayList<Token> tokens; // all tokens in tokenOrig
  private TSTLookup suggester;
  private TaxonomyWriter taxonomyWriter;
  private TaxonomyReader taxonomyReader;
  private CategoryDocumentBuilder categoryDocBuilder;
  private XQueryEvaluator xQueryEvaluator;
  private int commitInterval = 10;  // default: commit after each 10th document
  private int commitCounter = 0;
  
  public static IndexHandler getInstance() throws ApplicationException {
    if (instance == null) {
      instance = new IndexHandler();
      instance.init();
    }
    return instance;
  }

  private void init() throws ApplicationException {
    documentsIndexWriter = getDocumentsWriter();
    documentsIndexWriter.setMaxFieldLength(1000000);
    nodesIndexWriter = getNodesWriter();
    nodesIndexWriter.setMaxFieldLength(1000000);
    documentsSearcherManager = getNewSearcherManager(documentsIndexWriter);
    nodesSearcherManager = getNewSearcherManager(nodesIndexWriter);
    documentsIndexReader = getDocumentsReader();
    taxonomyWriter = getTaxonomyWriter();
    taxonomyReader = getTaxonomyReader();
    categoryDocBuilder = getCategoryDocumentBuilder();
    xQueryEvaluator = new XQueryEvaluator();
    Date before = new Date();
    tokens = getToken("tokenOrig", "", 10000000); // get all token: needs ca. 4 sec. for 3 Mio tokens
    Date after = new Date();
    LOGGER.info("Reading of all tokens in Lucene index successfully with: " + tokens.size() + " tokens (" + "elapsed time: " + (after.getTime() - before.getTime()) + " ms)");
  }

  public void setCommitInterval(int commitInterval) {
    this.commitInterval = commitInterval;
  }
  
  public void commit() throws ApplicationException {
    try {
      taxonomyWriter.commit();
      documentsIndexWriter.commit();
      nodesIndexWriter.commit();
    } catch (Exception e) {
      try {
        taxonomyWriter.rollback();
        documentsIndexWriter.rollback();
        nodesIndexWriter.rollback();
      } catch (Exception ex) {
        // nothing
      }
      throw new ApplicationException(e);
    }
  }
  
  public void indexDocument(CmsDocOperation docOperation) throws ApplicationException {
    commitCounter++;
    // first delete document in documentsIndex and nodesIndex
    deleteDocumentLocal(docOperation);
    indexDocumentLocal(docOperation);
    // performance gain (each commit needs at least ca. 60 ms extra): a commit is only done if commitInterval (e.g. 500) is reached 
    if (commitCounter >= commitInterval) {
      commit();
      commitCounter = 0;
    }
  }

  private void indexDocumentLocal(CmsDocOperation docOperation) throws ApplicationException {
    try {
      MetadataRecord mdRecord = docOperation.getMdRecord();
      List<CategoryPath> categories = new ArrayList<CategoryPath>();
      Document doc = new Document();
      int id = mdRecord.getId(); // short id (auto incremented)
      if (id != -1) { 
        NumericField idField = new NumericField("id", Field.Store.YES, true);
        idField.setIntValue(id);
        doc.add(idField);
      }
      String docId = mdRecord.getDocId();
      Field docIdField = new Field("docId", docId, Field.Store.YES, Field.Index.ANALYZED);
      doc.add(docIdField);
      String docIdSortedStr = docId.toLowerCase();  // so that sorting is lower case
      Field docIdFieldSorted = new Field("docIdSorted", docIdSortedStr, Field.Store.YES, Field.Index.NOT_ANALYZED); 
      doc.add(docIdFieldSorted);
      String identifier = mdRecord.getIdentifier();
      if (identifier != null) {
        Field identifierField = new Field("identifier", identifier, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(identifierField);
      }
      String uri = mdRecord.getUri();
      if (uri == null)
        uri = docOperation.getSrcUrl();
      if (uri != null) {
        Field uriField = new Field("uri", uri, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(uriField);
      }
      String collectionNames = mdRecord.getCollectionNames();
      if (collectionNames != null) {
        categories.add(new CategoryPath("collectionNames", collectionNames));
        Field collectionNamesField = new Field("collectionNames", collectionNames, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(collectionNamesField);
      }
      String author = mdRecord.getCreator();
      if (author != null) {
        String[] authors = author.split(";");
        for (int i=0; i<authors.length; i++) {
          String a = authors[i].trim();
          categories.add(new CategoryPath("author", a));
        }
        Field authorField = new Field("author", author, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(authorField);
        if (author != null)
          author = author.toLowerCase();  // so that sorting is lower case
        Field authorFieldSorted = new Field("authorSorted", author, Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(authorFieldSorted);
      } else {
        categories.add(new CategoryPath("author", "unbekannt"));
      }
      if (mdRecord.getCreatorDetails() != null) {
        Field authorDetailsField = new Field("authorDetails", mdRecord.getCreatorDetails(), Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(authorDetailsField);
      }
      if (mdRecord.getTitle() != null) {
        Field titleField = new Field("title", mdRecord.getTitle(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(titleField);
        String titleStr = mdRecord.getTitle();
        if (titleStr != null)
          titleStr = titleStr.toLowerCase();  // so that sorting is lower case
        Field titleFieldSorted = new Field("titleSorted", titleStr, Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(titleFieldSorted);
      }
      String publisher = mdRecord.getPublisher();
      if (publisher != null) {
        String[] publishers = publisher.split(";");
        for (int i=0; i<publishers.length; i++) {
          String p = publishers[i].trim();
          categories.add(new CategoryPath("publisher", p));
        }
        Field publisherField = new Field("publisher", publisher, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(publisherField);
        if (publisher != null)
          publisher = publisher.toLowerCase();  // so that sorting is lower case
        Field publisherFieldSorted = new Field("publisherSorted", publisher, Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(publisherFieldSorted);
      } else {
        categories.add(new CategoryPath("publisher", "unbekannt"));
      }
      String yearStr = mdRecord.getYear();
      if (yearStr == null) {
        Date pubDate = mdRecord.getPublishingDate();
        if (pubDate != null) {
          Calendar cal = Calendar.getInstance();
          cal.setTime(pubDate);
          int year = cal.get(Calendar.YEAR);
          yearStr = String.valueOf(year);
        }
      }
      if (yearStr != null) {
        categories.add(new CategoryPath("date", yearStr));
        Field dateField = new Field("date", yearStr, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(dateField);
        Field dateFieldSorted = new Field("dateSorted", yearStr, Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(dateFieldSorted);
      } else {
        categories.add(new CategoryPath("date", "unbekannt"));
      }
      if (mdRecord.getDescription() != null) {
        Field descriptionField = new Field("description", mdRecord.getDescription(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(descriptionField);
      }
      String subject = mdRecord.getSubject();
      if (subject != null) {
        String[] subjects = subject.split(",");
        if (subject.contains(";"))
          subjects = subject.split(";");
        if (subject.contains("###"))
          subjects = subject.split("###");
        for (int i=0; i<subjects.length; i++) {
          String s = subjects[i].trim();
          if (! s.isEmpty())
            categories.add(new CategoryPath("subject", s));
        }
        Field subjectField = new Field("subject", subject, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(subjectField);
      } else {
        categories.add(new CategoryPath("subject", "unbekannt"));
      }
      String subjectControlledDetails = mdRecord.getSubjectControlledDetails();
      if (subjectControlledDetails != null) {
        String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
        String dcTermsSubjectsStr = xQueryEvaluator.evaluateAsStringValueJoined(subjectControlledDetails, namespaceDeclaration + "/subjects/dcterms:subject/rdf:Description/rdfs:label", "###");
        String[] dcTermsSubjects = dcTermsSubjectsStr.split("###");
        for (int i=0; i<dcTermsSubjects.length; i++) {
          String s = dcTermsSubjects[i].trim();
          if (! s.isEmpty())
            categories.add(new CategoryPath("subjectControlled", s));
        }
        Field subjectControlledField = new Field("subjectControlled", dcTermsSubjectsStr, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(subjectControlledField);
        Field subjectControlledDetailsField = new Field("subjectControlledDetails", subjectControlledDetails, Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(subjectControlledDetailsField);
      }
      String swd = mdRecord.getSwd();
      if (swd != null) {
        String[] swds = swd.split(",");
        if (swd.contains(";"))
          swds = swd.split(";");
        for (int i=0; i<swds.length; i++) {
          String s = swds[i].trim();
          categories.add(new CategoryPath("swd", s));
        }
        Field swdField = new Field("swd", swd, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(swdField);
      } else {
        categories.add(new CategoryPath("swd", "unbekannt"));
      }
      if (mdRecord.getDdc() != null) {
        categories.add(new CategoryPath("ddc", mdRecord.getDdc()));
        Field ddcField = new Field("ddc", mdRecord.getDdc(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(ddcField);
      } else {
        categories.add(new CategoryPath("ddc", "unbekannt"));
      }
      if (mdRecord.getRights() != null) {
        Field rightsField = new Field("rights", mdRecord.getRights(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(rightsField);
      }
      if (mdRecord.getLicense() != null) {
        Field licenseField = new Field("license", mdRecord.getLicense(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(licenseField);
      }
      if (mdRecord.getAccessRights() != null) {
        Field accessRightsField = new Field("accessRights", mdRecord.getAccessRights(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(accessRightsField);
      }
      if (mdRecord.getLastModified() != null) {
        Date lastModified = mdRecord.getLastModified();
        String xsDateStr = new Util().toXsDate(lastModified);
        Field lastModifiedField = new Field("lastModified", xsDateStr, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(lastModifiedField);
        long time = lastModified.getTime();
        String timeStr = String.valueOf(time);
        Field lastModifiedFieldSorted = new Field("lastModifiedSorted", timeStr, Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(lastModifiedFieldSorted);
      }
      if (mdRecord.getSchemaName() != null) {
        Field schemaField = new Field("schemaName", mdRecord.getSchemaName(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(schemaField);
        String schemaStr = mdRecord.getSchemaName();
        if (schemaStr != null)
          schemaStr = schemaStr.toLowerCase();  // so that sorting is lower case
        Field schemaFieldSorted = new Field("schemaNameSorted", schemaStr, Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(schemaFieldSorted);
      }
      if (mdRecord.getType() != null) {
        categories.add(new CategoryPath("type", mdRecord.getType()));
        Field typeField = new Field("type", mdRecord.getType(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(typeField);
      } else {
        categories.add(new CategoryPath("type", "unbekannt"));
      }
      if (mdRecord.getPersons() != null) {
        Field personsField = new Field("persons", mdRecord.getPersons(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(personsField);
      }
      if (mdRecord.getPersonsDetails() != null) {
        Field personsDetailsField = new Field("personsDetails", mdRecord.getPersonsDetails(), Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(personsDetailsField);
      }
      if (mdRecord.getPlaces() != null) {
        Field placesField = new Field("places", mdRecord.getPlaces(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(placesField);
      }
      String language = mdRecord.getLanguage();
      if (language != null) {
        categories.add(new CategoryPath("language", mdRecord.getLanguage()));
        Field languageField = new Field("language", mdRecord.getLanguage(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(languageField);
        String langStr = mdRecord.getLanguage();
        if (langStr != null)
          langStr = langStr.toLowerCase();  // so that sorting is lower case
        Field languageFieldSorted = new Field("languageSorted", langStr, Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(languageFieldSorted);
      } else {
        categories.add(new CategoryPath("language", "unbekannt"));
      }
      int pageCount = mdRecord.getPageCount();
      if (pageCount != -1) {
        String pageCountStr = String.valueOf(pageCount);
        Field pageCountField = new Field("pageCount", pageCountStr, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(pageCountField);
      }
      String docTokensOrig = mdRecord.getTokenOrig();
      if (docTokensOrig != null) {
        Field tokenOrigField = new Field("tokenOrig", docTokensOrig, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(tokenOrigField);
      }
      String docTokensReg = mdRecord.getTokenReg();
      if (docTokensReg != null) {
        Field tokenRegField = new Field("tokenReg", docTokensReg, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(tokenRegField);
      }
      String docTokensNorm = mdRecord.getTokenNorm();
      if (docTokensNorm != null) {
        Field tokenNormField = new Field("tokenNorm", docTokensNorm, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(tokenNormField);
      }
      String docTokensMorph = mdRecord.getTokenMorph();
      if (docTokensMorph != null) {
        Field tokenMorphField = new Field("tokenMorph", docTokensMorph, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(tokenMorphField);
      }
      String contentXml = mdRecord.getContentXml();
      if (contentXml != null) {
        Field contentXmlField = new Field("xmlContent", contentXml, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(contentXmlField);
      }
      String content = mdRecord.getContent();
      if (content != null) {
        Field contentField = new Field("content", content, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(contentField);
      }
      // save the webUrl field  
      String webUri = mdRecord.getWebUri();
      Hashtable<String, XQuery> xqueriesHashtable = mdRecord.getxQueries();
      String mimeType = mdRecord.getType();
      boolean isXml = false;
      if (mimeType != null && mimeType.contains("xml"))
        isXml = true;
      // perform xQueries only on XML-Documents
      if (isXml && xqueriesHashtable != null) {
        Enumeration<String> keys = xqueriesHashtable.keys();
        if (keys != null && keys.hasMoreElements()) {
          XQuery xQueryWebId = xqueriesHashtable.get("webId");
          if (xQueryWebId != null) {
            String webId = xQueryWebId.getResult();
            // Hack: if xQuery code is "fileName"
            boolean codeIsFileName = false;
            if (xQueryWebId.getCode() != null && xQueryWebId.getCode().equals("xxxFileName"))
              codeIsFileName = true;
            if (codeIsFileName) {
              int from = docId.lastIndexOf("/");
              String fileName = docId.substring(from + 1);
              webId = fileName;
            }
            if (webId != null) {
              webId = webId.trim();
              if (! webId.isEmpty()) {
                String xQueryPreStr = xQueryWebId.getPreStr();
                if (xQueryPreStr != null)
                  webUri = xQueryPreStr + webId;
                String xQueryAfterStr = xQueryWebId.getAfterStr();
                if (xQueryAfterStr != null)
                  webUri = webUri + xQueryAfterStr;
                if (xQueryPreStr == null && xQueryAfterStr == null)
                  webUri = webId;
              }
            }
          }
        }
      }
      mdRecord.setWebUri(webUri);
      if (webUri != null) {
        Field webUriField = new Field("webUri", webUri, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(webUriField);
      }

      // facet creation
      categoryDocBuilder.setCategoryPaths(categories);
      categoryDocBuilder.build(doc);

      documentsIndexWriter.addDocument(doc);
      
      // to save Lucene disk space and to gain performance the document nodes index is set off:
      /* 
      DocumentHandler docHandler = new DocumentHandler();
      boolean docIsXml = docHandler.isDocXml(docId);
      if (docIsXml) {
        // add all elements with the specified names of the document to nodesIndex
        ArrayList<XmlTokenizerContentHandler.Element> xmlElements = mdRecord.getXmlElements();
        if (xmlElements != null) {
          for (int i = 0; i < xmlElements.size(); i++) {
            XmlTokenizerContentHandler.Element element = xmlElements.get(i);
            Document nodeDoc = new Document();
            nodeDoc.add(docIdField);
            String nodeLanguage = element.lang;
            if (nodeLanguage == null)
              nodeLanguage = language;
            String nodePageNumber = String.valueOf(element.pageNumber);
            String nodeLineNumber = String.valueOf(element.lineNumber);
            String nodeElementName = String.valueOf(element.name);
            String nodeElementDocPosition = String.valueOf(element.docPosition);
            String nodeElementAbsolutePosition = String.valueOf(element.position);
            String nodeElementPagePosition = String.valueOf(element.pagePosition);
            String nodeElementPosition = String.valueOf(element.elemPosition);
            String nodeXmlId = element.xmlId;
            String nodeXpath = element.xpath;
            String nodeXmlContent = element.toXmlString();
            String nodeTokensOrig = element.getTokensStr("orig");
            String nodeTokensReg = element.getTokensStr("reg");
            String nodeTokensNorm = element.getTokensStr("norm");
            String nodeTokensMorph = element.getTokensStr("morph");
            if (nodeLanguage != null) {
              Field nodeLanguageField = new Field("language", nodeLanguage, Field.Store.YES, Field.Index.ANALYZED);
              nodeDoc.add(nodeLanguageField);
            }
            Field nodePageNumberField = new Field("pageNumber", nodePageNumber, Field.Store.YES, Field.Index.ANALYZED);
            nodeDoc.add(nodePageNumberField);
            Field nodeLineNumberField = new Field("lineNumber", nodeLineNumber, Field.Store.YES, Field.Index.ANALYZED);
            nodeDoc.add(nodeLineNumberField);
            Field nodeElementNameField = new Field("elementName", nodeElementName, Field.Store.YES, Field.Index.ANALYZED);
            nodeDoc.add(nodeElementNameField);
            Field nodeElementDocPositionField = new Field("elementDocPosition", nodeElementDocPosition, Field.Store.YES, Field.Index.ANALYZED);
            nodeDoc.add(nodeElementDocPositionField);
            Field nodeElementDocPositionFieldSorted = new Field("elementDocPositionSorted", nodeElementDocPosition, Field.Store.YES, Field.Index.NOT_ANALYZED);
            nodeDoc.add(nodeElementDocPositionFieldSorted);
            Field nodeElementAbsolutePositionField = new Field("elementAbsolutePosition", nodeElementAbsolutePosition, Field.Store.YES, Field.Index.ANALYZED);
            nodeDoc.add(nodeElementAbsolutePositionField);
            Field nodeElementPagePositionField = new Field("elementPagePosition", nodeElementPagePosition, Field.Store.YES, Field.Index.ANALYZED);
            nodeDoc.add(nodeElementPagePositionField);
            Field nodeElementPositionField = new Field("elementPosition", nodeElementPosition, Field.Store.YES, Field.Index.ANALYZED);
            nodeDoc.add(nodeElementPositionField);
            if (nodeXmlId != null) {
              Field nodeXmlIdField = new Field("xmlId", nodeXmlId, Field.Store.YES, Field.Index.ANALYZED);
              nodeDoc.add(nodeXmlIdField);
            }
            if (nodeXpath != null) {
              Field nodeXpathField = new Field("xpath", nodeXpath, Field.Store.YES, Field.Index.ANALYZED);
              nodeDoc.add(nodeXpathField);
            }
            if (nodeXmlContent != null) {
              Field nodeXmlContentField = new Field("xmlContent", nodeXmlContent, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
              nodeDoc.add(nodeXmlContentField);
            }
            if (nodeXmlContent != null) {
              String nodeXmlContentTokenized = toTokenizedXmlString(nodeXmlContent, nodeLanguage);
              Field nodeXmlContentTokenizedField = new Field("xmlContentTokenized", nodeXmlContentTokenized, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
              nodeDoc.add(nodeXmlContentTokenizedField);
            }
            if (nodeTokensOrig != null) {
              Field nodeTokenOrigField = new Field("tokenOrig", nodeTokensOrig, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
              nodeDoc.add(nodeTokenOrigField);
            }
            if (nodeTokensReg != null) {
              Field nodeTokenRegField = new Field("tokenReg", nodeTokensReg, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
              nodeDoc.add(nodeTokenRegField);
            }
            if (nodeTokensNorm != null) {
              Field nodeTokenNormField = new Field("tokenNorm", nodeTokensNorm, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
              nodeDoc.add(nodeTokenNormField);
            }
            if (nodeTokensMorph != null) {
              Field nodeTokenMorphField = new Field("tokenMorph", nodeTokensMorph, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
              nodeDoc.add(nodeTokenMorphField);
            }
    
            nodesIndexWriter.addDocument(nodeDoc);
          }
        }
      }
      */
    } catch (Exception e) {
      LOGGER.error("indexDocumentLocal: " + docOperation.getDocIdentifier());
      e.printStackTrace();
      throw new ApplicationException(e);
    }
  }

  public void deleteDocument(CmsDocOperation docOperation) throws ApplicationException {
    deleteDocumentLocal(docOperation);
    commit();
  }

  public int deleteCollection(String collectionName) throws ApplicationException {
    int countDeletedDocs = -1;
    countDeletedDocs = deleteCollectionLocal(collectionName);
    commit();
    return countDeletedDocs;
  }

  public TSTLookup getSuggester() throws ApplicationException {
    // one time init of the suggester, if it is null (needs ca. 2 sec. for 3 Mio. token)
    if (suggester == null) {
      suggester = new TSTLookup();
      try {
        suggester.build(new TokenArrayListIterator(tokens));  // put all tokens into the suggester
        LOGGER.info("Suggester successfully started with: " + tokens.size() + " tokens");
      } catch (IOException e) {
        throw new ApplicationException(e);
      }
    }
    return suggester;  
  }
  
  private void deleteDocumentLocal(CmsDocOperation docOperation) throws ApplicationException {
    String docId = docOperation.getDocIdentifier();
    try {
      Term termIdentifier = new Term("docId", docId);
      documentsIndexWriter.deleteDocuments(termIdentifier);
      nodesIndexWriter.deleteDocuments(termIdentifier);
    } catch (Exception e) {
      LOGGER.error("deleteDocumentLocal: " + docOperation.getDocIdentifier());
      throw new ApplicationException(e);
    }
  }

  private int deleteCollectionLocal(String collectionName) throws ApplicationException {
    int countDeletedDocs = -1;
    try {
      String queryDocumentsByCollectionName = "collectionNames:" + collectionName;
      Hits collectionDocHits = queryDocuments("lucene", queryDocumentsByCollectionName, null, null, null, 0, 100000, false, false);
      ArrayList<org.bbaw.wsp.cms.document.Document> collectionDocs = collectionDocHits.getHits();
      if (collectionDocs != null) {
        countDeletedDocs = collectionDocHits.getSize();
        // delete all nodes of each document in collection
        for (int i=0; i<collectionDocs.size(); i++) {
          org.bbaw.wsp.cms.document.Document doc = collectionDocs.get(i);
          Fieldable docIdFieldable = doc.getFieldable("docId");
          String docId = docIdFieldable.stringValue();
          Term termDocId = new Term("docId", docId);
          nodesIndexWriter.deleteDocuments(termDocId);
        }
        // delete all documents in collection
        Term termCollectionName = new Term("collectionNames", collectionName);
        documentsIndexWriter.deleteDocuments(termCollectionName);
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return countDeletedDocs;
  }

  public Hits queryDocuments(String queryLanguage, String queryStr, String[] sortFieldNames, String fieldExpansion, String language, int from, int to, boolean withHitFragments, boolean translate) throws ApplicationException {
    Hits hits = null;
    IndexSearcher searcher = null;
    try {
      makeDocumentsSearcherManagerUpToDate();
      searcher = documentsSearcherManager.acquire();
      String defaultQueryFieldName = "tokenOrig";
      QueryParser queryParser = new QueryParser(Version.LUCENE_35, defaultQueryFieldName, documentsPerFieldAnalyzer);
      queryParser.setAllowLeadingWildcard(true);
      Query query = null;
      if (queryStr.equals("*")) {
        query = new MatchAllDocsQuery();
      } else {
        if (queryLanguage != null && queryLanguage.equals("gl")) {
          queryStr = translateGoogleLikeQueryToLucene(queryStr);
        }
        query = buildFieldExpandedQuery(queryParser, queryStr, fieldExpansion);
      }
      LanguageHandler languageHandler = new LanguageHandler();
      if (translate) {
        ArrayList<String> queryTerms = fetchTerms(query);
        languageHandler.translate(queryTerms, language);
      }
      Query morphQuery = buildMorphQuery(query, language, false, translate, languageHandler);
      Query highlighterQuery = buildMorphQuery(query, language, true, translate, languageHandler);
      if (query instanceof PhraseQuery || query instanceof PrefixQuery || query instanceof FuzzyQuery || query instanceof TermRangeQuery) {
        highlighterQuery = query;  // TODO wenn sie rekursiv enthalten sind 
      }
      FastVectorHighlighter highlighter = new FastVectorHighlighter(true, false);
      Sort sort = new Sort();
      SortField scoreSortField = new SortField(null, SortField.SCORE); // default sort
      sort.setSort(scoreSortField);
      if (sortFieldNames != null) {
        sort = buildSort(sortFieldNames, "doc");  // build sort criteria
      }
      TopFieldCollector topFieldCollector = TopFieldCollector.create(sort, to + 1, true, true, true, true); // default topFieldCollector for TopDocs results, numHits: maximum is "to" (performance gain for every bigger result)
      FacetSearchParams facetSearchParams = new FacetSearchParams();
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("collectionNames"), 1000));
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("language"), 1000));
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("author"), 10));
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("publisher"), 10));
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("date"), 10));
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("subject"), 10));
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("subjectControlled"), 10));
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("swd"), 10));
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("ddc"), 10));
      facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("type"), 1000));
      FacetsCollector facetsCollector = new FacetsCollector(facetSearchParams, documentsIndexReader, taxonomyReader);
      Collector facetsCollectorWrapper = MultiCollector.wrap(topFieldCollector, facetsCollector);
      searcher.search(morphQuery, facetsCollectorWrapper);
      TopDocs resultDocs = topFieldCollector.topDocs();
      Facets facets = null;
      List<FacetResult> tmpFacetResult = facetsCollector.getFacetResults();
      if (tmpFacetResult != null && ! tmpFacetResult.isEmpty())
        facets = new Facets(tmpFacetResult);
      resultDocs.setMaxScore(1);
      int toTmp = to;
      if (resultDocs.totalHits <= to)
        toTmp = resultDocs.totalHits - 1;
      if (resultDocs != null) {
        ArrayList<org.bbaw.wsp.cms.document.Document> docs = new ArrayList<org.bbaw.wsp.cms.document.Document>();
        for (int i=from; i<=toTmp; i++) { 
          int docID = resultDocs.scoreDocs[i].doc;
          FieldSelector docFieldSelector = getDocFieldSelector();
          Document luceneDoc = searcher.doc(docID, docFieldSelector);
          org.bbaw.wsp.cms.document.Document doc = new org.bbaw.wsp.cms.document.Document(luceneDoc);
          if (withHitFragments) {
            ArrayList<String> hitFragments = new ArrayList<String>();
            Fieldable docContentField = luceneDoc.getFieldable("content");
            if (docContentField != null) {
              FieldQuery highlighterFieldQuery = highlighter.getFieldQuery(highlighterQuery);
              String[] textfragments = highlighter.getBestFragments(highlighterFieldQuery, documentsIndexReader, docID, docContentField.name(), 100, 2);
              if (textfragments.length > 0) {
                for (int j=0; j<textfragments.length; j++) {
                  String textFragment = textfragments[j];
                  textFragment = textFragment.trim();  
                  textFragment = checkHitFragment(textFragment);
                  hitFragments.add(textFragment);
                }
              }
            }
            if (! hitFragments.isEmpty())
              doc.setHitFragments(hitFragments);
          }
          /*
           * only needed when indexing works also on xml document nodes 
          // if xml document: try to add pageNumber to resulting doc
          DocumentHandler docHandler = new DocumentHandler();
          Fieldable docIdField = luceneDoc.getFieldable("docId");
          if (docIdField != null) {
            String docId = docIdField.stringValue();
            boolean docIsXml = docHandler.isDocXml(docId);
            if (docIsXml && ! queryStr.equals("*")) {
              Hits firstHit = queryDocument(docId, queryStr, 0, 0);
              if (firstHit != null && firstHit.getSize() > 0) {
                ArrayList<org.bbaw.wsp.cms.document.Document> firstHitHits = firstHit.getHits();
                if (firstHitHits != null && firstHitHits.size() > 0) {
                  org.bbaw.wsp.cms.document.Document firstHitDoc = firstHitHits.get(0);
                  Document firstHitLuceneDoc = firstHitDoc.getDocument();
                  Fieldable pageNumberField = firstHitLuceneDoc.getFieldable("pageNumber");
                  if (pageNumberField != null) {
                    String pageNumber = pageNumberField.stringValue();
                    doc.setFirstHitPageNumber(pageNumber);
                  }
                }
              }
            }
          }
          */
          docs.add(doc);
        }
        int sizeTotalDocuments = documentsIndexReader.numDocs();
        int sizeTotalTerms = tokens.size(); // term count over tokenOrig
        if (docs != null) {
          hits = new Hits(docs, from, to);
          hits.setSize(resultDocs.totalHits);
          hits.setSizeTotalDocuments(sizeTotalDocuments);
          hits.setSizeTotalTerms(sizeTotalTerms);
          hits.setQuery(morphQuery);
          hits.setFacets(facets);
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    } finally {
      try {
        if (searcher != null)
          documentsSearcherManager.release(searcher);
      } catch (IOException e) {
        // nothing
      }
    }
    // Do not use searcher after this!
    searcher = null;
    return hits;
  }

  public Hits queryDocument(String docId, String queryStr, int from, int to) throws ApplicationException {
    Hits hits = null;
    IndexSearcher searcher = null;
    MetadataRecord docMetadataRecord = getDocMetadata(docId);
    if (docMetadataRecord == null)
      return null;  // no document with that docId is in index
    try {
      makeNodesSearcherManagerUpToDate();
      searcher = nodesSearcherManager.acquire();
      String fieldNameDocId = "docId";
      QueryParser queryParser1 = new QueryParser(Version.LUCENE_35, fieldNameDocId, nodesPerFieldAnalyzer);
      queryParser1.setAllowLeadingWildcard(true);
      Query queryDocId = queryParser1.parse(docId);
      String defaultQueryFieldName = "tokenOrig";
      QueryParser queryParser2 = new QueryParser(Version.LUCENE_35, defaultQueryFieldName, nodesPerFieldAnalyzer);
      queryParser2.setAllowLeadingWildcard(true);
      Query query = queryParser2.parse(queryStr);
      String language = docMetadataRecord.getLanguage();
      if (language == null || language.equals("")) {
        String collectionNames = docMetadataRecord.getCollectionNames();
        Collection collection = CollectionReader.getInstance().getCollection(collectionNames);
        if (collection != null) {
          String mainLang = collection.getMainLanguage();
          if (mainLang != null)
            language = mainLang;
        } 
      }
      LanguageHandler languageHandler = new LanguageHandler();
      Query morphQuery = buildMorphQuery(query, language, languageHandler);
      BooleanQuery queryDoc = new BooleanQuery();
      queryDoc.add(queryDocId, BooleanClause.Occur.MUST);
      queryDoc.add(morphQuery, BooleanClause.Occur.MUST);
      Sort sortByPosition = new Sort(new SortField("position", SortField.INT));
      TopDocs topDocs = searcher.search(queryDoc, 100000, sortByPosition);
      topDocs.setMaxScore(1);
      int toTmp = to;
      if (topDocs.scoreDocs.length <= to)
        toTmp = topDocs.scoreDocs.length - 1;
      if (topDocs != null) {
        ArrayList<org.bbaw.wsp.cms.document.Document>  docs = new ArrayList<org.bbaw.wsp.cms.document.Document>();
        for (int i=from; i<=toTmp; i++) {
          int docID = topDocs.scoreDocs[i].doc;
          FieldSelector nodeFieldSelector = getNodeFieldSelector();
          Document luceneDoc = searcher.doc(docID, nodeFieldSelector);
          org.bbaw.wsp.cms.document.Document doc = new org.bbaw.wsp.cms.document.Document(luceneDoc);
          String pageNumber = "-1";
          Fieldable fPageNumber = doc.getFieldable("pageNumber");
          if (fPageNumber != null) {
            pageNumber = fPageNumber.stringValue();
          }
          if (! pageNumber.equals("0")) {
            docs.add(doc);
          }
        }
        if (docs != null) {
          hits = new Hits(docs, from, to);
          hits.setSize(topDocs.scoreDocs.length);
        }
      }
      searcher.close();
    } catch (Exception e) {
      throw new ApplicationException(e);
    } finally {
      try {
        if (searcher != null)
          documentsSearcherManager.release(searcher);
      } catch (IOException e) {
        // nothing
      }
    }
    // Do not use searcher after this!
    searcher = null;
    return hits;
  }

  public MetadataRecord getDocMetadata(String docId) throws ApplicationException {
    MetadataRecord mdRecord = null;
    Document doc = getDocument(docId);
    if (doc != null) {
      int id = -1;
      Fieldable idField = doc.getFieldable("id");
      if (idField != null) {
        String idStr = idField.stringValue();
        id = Integer.valueOf(idStr);
      }
      String identifier = null;
      Fieldable identifierField = doc.getFieldable("identifier");
      if (identifierField != null)
        identifier = identifierField.stringValue();
      String uri = null;
      Fieldable uriField = doc.getFieldable("uri");
      if (uriField != null)
        uri = uriField.stringValue();
      String webUri = null;
      Fieldable webUriField = doc.getFieldable("webUri");
      if (webUriField != null)
        webUri = webUriField.stringValue();
      String collectionNames = null;
      Fieldable collectionNamesField = doc.getFieldable("collectionNames");
      if (collectionNamesField != null)
        collectionNames = collectionNamesField.stringValue();
      String author = null;
      Fieldable authorField = doc.getFieldable("author");
      if (authorField != null)
        author = authorField.stringValue();
      String authorDetails = null;
      Fieldable authorDetailsField = doc.getFieldable("authorDetails");
      if (authorDetailsField != null)
        authorDetails = authorDetailsField.stringValue();
      String title = null;
      Fieldable titleField = doc.getFieldable("title");
      if (titleField != null)
        title = titleField.stringValue();
      String language = null;
      Fieldable languageField = doc.getFieldable("language");
      if (languageField != null)
        language = languageField.stringValue();
      else {
        Collection collection = CollectionReader.getInstance().getCollection(collectionNames);
        if (collection != null) {
          String mainLang = collection.getMainLanguage();
          if (mainLang != null)
            language = mainLang;
        } 
      }
      Date yearDate = null;
      Fieldable dateField = doc.getFieldable("date");
      if (dateField != null) {
        String dateStr = dateField.stringValue();
        if (dateStr != null && !dateStr.equals("")) {
          dateStr = StringUtils.deresolveXmlEntities(dateStr);
          String yearStr = new Util().toYearStr(dateStr); // test if possible
          // etc
          if (yearStr != null) {
            yearDate = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          }
        }
      }
      String description = null;
      Fieldable descriptionField = doc.getFieldable("description");
      if (descriptionField != null)
        description = descriptionField.stringValue();
      String subject = null;
      Fieldable subjectField = doc.getFieldable("subject");
      if (subjectField != null)
        subject = subjectField.stringValue();
      String swd = null;
      Fieldable swdField = doc.getFieldable("swd");
      if (swdField != null)
        swd = swdField.stringValue();
      String ddc = null;
      Fieldable ddcField = doc.getFieldable("ddc");
      if (ddcField != null)
        ddc = ddcField.stringValue();
      String rights = null;
      Fieldable rightsField = doc.getFieldable("rights");
      if (rightsField != null)
        rights = rightsField.stringValue();
      String license = null;
      Fieldable licenseField = doc.getFieldable("license");
      if (licenseField != null)
        license = licenseField.stringValue();
      String accessRights = null;
      Fieldable accessRightsField = doc.getFieldable("accessRights");
      if (accessRightsField != null)
        accessRights = accessRightsField.stringValue();
      int pageCount = -1;
      Fieldable pageCountField = doc.getFieldable("pageCount");
      if (pageCountField != null) {
        String pageCountStr = pageCountField.stringValue();
        pageCount = Integer.valueOf(pageCountStr);
      }
      String type = null;
      Fieldable typeField = doc.getFieldable("type");
      if (typeField != null)
        type = typeField.stringValue();
      String personsStr = null;
      Fieldable personsField = doc.getFieldable("persons");
      if (personsField != null) {
        personsStr = personsField.stringValue();
      }
      String personsDetailsStr = null;
      Fieldable personsDetailsField = doc.getFieldable("personsDetails");
      if (personsDetailsField != null) {
        personsDetailsStr = personsDetailsField.stringValue();
      }
      String placesStr = null;
      Fieldable placesField = doc.getFieldable("places");
      if (placesField != null) {
        placesStr = placesField.stringValue();
      }
      String schemaName = null;
      Fieldable schemaNameField = doc.getFieldable("schemaName");
      if (schemaNameField != null)
        schemaName = schemaNameField.stringValue();
      Date lastModified = null;
      Fieldable lastModifiedField = doc.getFieldable("lastModified");
      if (lastModifiedField != null) {
        String lastModifiedXSDateStr = lastModifiedField.stringValue();
        lastModified = new Util().toDate(lastModifiedXSDateStr);
      }
      mdRecord = new MetadataRecord();
      mdRecord.setDocId(docId);
      mdRecord.setId(id);
      mdRecord.setUri(uri);
      mdRecord.setWebUri(webUri);
      mdRecord.setIdentifier(identifier);
      mdRecord.setCollectionNames(collectionNames);
      mdRecord.setCreator(author);
      mdRecord.setCreatorDetails(authorDetails);
      mdRecord.setTitle(title);
      mdRecord.setDate(yearDate);
      mdRecord.setLanguage(language);
      mdRecord.setDescription(description);
      mdRecord.setSubject(subject);
      mdRecord.setSwd(swd);
      mdRecord.setDdc(ddc);
      mdRecord.setLicense(license);
      mdRecord.setRights(rights);
      mdRecord.setAccessRights(accessRights);
      mdRecord.setPageCount(pageCount);
      mdRecord.setType(type);
      mdRecord.setPersons(personsStr);
      mdRecord.setPersonsDetails(personsDetailsStr);
      mdRecord.setPlaces(placesStr);
      mdRecord.setSchemaName(schemaName);
      mdRecord.setLastModified(lastModified);
    }
    return mdRecord;
  }

  public ArrayList<Token> getToken(String fieldName, String value, int count) throws ApplicationException {
    ArrayList<Token> retToken = new ArrayList<Token>();
    int counter = 0;
    TermEnum terms = null;
    try {
      if (value == null)
        value = "";
      Term term = new Term(fieldName, value);
      makeIndexReaderUpToDate();
      terms = documentsIndexReader.terms(term);
      while (terms != null && fieldName != null && terms.term() != null && fieldName.equals(terms.term().field()) && counter < count) {
        Term termContent = terms.term();
        String termContentStr = termContent.text();
        if (termContentStr.startsWith(value)) {
          int docFreq = terms.docFreq();
          Token token = new Token(termContent);
          token.setFreq(docFreq);
          retToken.add(token);
          counter++;
        }
        if (! terms.next() || ! termContentStr.startsWith(value))
          break;
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    } finally {
      if (terms != null) {
        try {
          terms.close();
        } catch (IOException e) {
          // nothing
        }
      }
    }
    return retToken;
  }

  public ArrayList<Token> getToken(String docId, String fieldName, String value, int count) throws ApplicationException {
    ArrayList<Token> retToken = null;
    if (value == null)
      value = "";
    int counter = 0;
    IndexSearcher searcher = null;
    try {
      makeDocumentsSearcherManagerUpToDate();
      makeIndexReaderUpToDate();
      searcher = documentsSearcherManager.acquire();
      Query queryDocId = new TermQuery(new Term("docId", docId));
      TopDocs topDocs = searcher.search(queryDocId, 1);
      if (topDocs != null) {
        int docIdInt = topDocs.scoreDocs[0].doc;
        TermFreqVector termFreqVector = documentsIndexReader.getTermFreqVector(docIdInt, fieldName);
        if (termFreqVector != null) {
          String[] terms = termFreqVector.getTerms();
          int[] freqs = termFreqVector.getTermFrequencies();
          boolean success = false;
          if (terms != null) {
            retToken = new ArrayList<Token>();
            for (int i = 0; i < terms.length; i++) {
              String termStr = terms[i];
              if (termStr.startsWith(value))
                success = true;
              if (success) {
                counter++;
                int freq = freqs[i];
                Term t = new Term(fieldName, termStr);
                Token tok = new Token(t);
                tok.setFreq(freq);
                retToken.add(tok);
              }
              if (counter >= count)
                break;
              success = false;
            }
          }
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    } finally {
      try {
        if (searcher != null)
          documentsSearcherManager.release(searcher);
      } catch (IOException e) {
        // nothing
      }
    }
    // Do not use searcher after this!
    searcher = null;
    return retToken;
  }

  public void end() throws ApplicationException {
    try {
      if (taxonomyWriter != null)
        taxonomyWriter.close();
      if (documentsIndexWriter != null)
        documentsIndexWriter.close();
      if (nodesIndexWriter != null)
        nodesIndexWriter.close();
      if (documentsSearcherManager != null)
        documentsSearcherManager.close();
      if (nodesSearcherManager != null)
        nodesSearcherManager.close();
      if (documentsIndexReader != null)
        documentsIndexReader.close();
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  public void optimize() throws ApplicationException {
    try {
      if (documentsIndexWriter != null)
        documentsIndexWriter.forceMerge(1);
      if (nodesIndexWriter != null)
        nodesIndexWriter.forceMerge(1);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private String translateGoogleLikeQueryToLucene(String queryStr) {
    int apostrophCounter = org.apache.commons.lang3.StringUtils.countMatches(queryStr, "\"");
    if (apostrophCounter == 1 || apostrophCounter == 3 || apostrophCounter == 5 || apostrophCounter == 7 || apostrophCounter == 9)
      queryStr = queryStr.replaceAll("\"", "");
    String luceneQueryStr = queryStr;
    if (queryStr.matches(".+\\|.+")) {
      luceneQueryStr = luceneQueryStr.replaceAll("\\|", " ");  // logical or queries
    } else if (queryStr.matches("(.+) (-.+?)")) {
      // nothing, same as in google like // logical andnot queries
    } else if (queryStr.matches(".+~.+")) {
      luceneQueryStr = queryStr; // near/fuzzy queries
    } else { 
      List<String> logicalAndQueryTerms = new ArrayList<String>();
      Matcher matchFields = Pattern.compile("(\\S*:\\(.+?\\)|\\S*:\\S*)\\s*").matcher(queryStr); // matches fields (e.g. "author:leibniz" or "author:(leibniz humboldt)" 
      while (matchFields.find())
        logicalAndQueryTerms.add(matchFields.group(1));
      String queryStrWithoutFields = matchFields.replaceAll("");
      Matcher matchWordsAndPhrases = Pattern.compile("([^\"]\\S*|\".+?\"|[^\"]:\\(.+\\))\\s*").matcher(queryStrWithoutFields); // matches phrases (e.g. "bla1 bla2 bla3") or single words delimited by spaces ("S*" means: all characters but not spaces; "s*" means: spaces)
      while (matchWordsAndPhrases.find())
        logicalAndQueryTerms.add(matchWordsAndPhrases.group(1));
      luceneQueryStr = "";
      for (int i=0; i<logicalAndQueryTerms.size(); i++) {
        String queryTerm = logicalAndQueryTerms.get(i);
        luceneQueryStr = luceneQueryStr + "+" + queryTerm + " "; 
      }
      luceneQueryStr = luceneQueryStr.substring(0, luceneQueryStr.length() - 1);
    }
    return luceneQueryStr;
  }
  
  private Query buildFieldExpandedQuery(QueryParser queryParser, String queryStr, String fieldExpansion) throws ApplicationException {
    Query retQuery = null;
    if (fieldExpansion == null)
      fieldExpansion = "none";
    /*
    Set<Term> queryTerms = new HashSet<Term>();
    query.extractTerms(queryTerms);
    // if original query contains fields then the original query is returned
    if (queryTerms != null && ! queryTerms.isEmpty())
      return query;
    */
    try {
      if (fieldExpansion.equals("all")) {
        BooleanQuery boolQuery = new BooleanQuery();
        for (int i=0; i < QUERY_EXPANSION_FIELDS_ALL.length; i++) {
          String expansionField = QUERY_EXPANSION_FIELDS_ALL[i];  // e.g. "author"
          String expansionFieldQueryStr = expansionField + ":(" + queryStr + ")";
          Query q = queryParser.parse(expansionFieldQueryStr);
          boolQuery.add(q, BooleanClause.Occur.SHOULD);
        }
        retQuery = boolQuery;
      } else if (fieldExpansion.equals("allMorph")) {
        BooleanQuery boolQuery = new BooleanQuery();
        for (int i=0; i < QUERY_EXPANSION_FIELDS_ALL_MORPH.length; i++) {
          String expansionField = QUERY_EXPANSION_FIELDS_ALL_MORPH[i];  // e.g. "author"
          String expansionFieldQueryStr = expansionField + ":(" + queryStr + ")";
          Query q = queryParser.parse(expansionFieldQueryStr);
          boolQuery.add(q, BooleanClause.Occur.SHOULD);
        }
        retQuery = boolQuery;
      } else if (fieldExpansion.equals("none")) {
        Query q = queryParser.parse(queryStr);
        retQuery = q;
      }
    } catch (ParseException e) {
      throw new ApplicationException(e);
    }
    return retQuery;
  }
  
  private Query buildHighlighterQuery(Query query, String language, boolean translate, LanguageHandler languageHandler) throws ApplicationException {
    Query retQuery = null;
    // TODO 
    // Query fulltextQuery = removeNonFulltextFields(query); // fulltextQuery enthält die Volltextanfrage als tokenOrig oder tokenMorph
    // retQuery = buildMorphQuery(fulltextQuery, language, true, translate, languageHandler);
    return retQuery;
  }
  
  private Query buildMorphQuery(Query query, String language, LanguageHandler languageHandler) throws ApplicationException {
    return buildMorphQuery(query, language, false, false, languageHandler);
  }

  private Query buildMorphQuery(Query query, String language, boolean withAllForms, boolean translate, LanguageHandler languageHandler) throws ApplicationException {
    Query morphQuery = null;
    if (query instanceof TermQuery) {
      TermQuery termQuery = (TermQuery) query;
      morphQuery = buildMorphQuery(termQuery, language, withAllForms, translate, languageHandler);
    } else if (query instanceof BooleanQuery) {
      BooleanQuery booleanQuery = (BooleanQuery) query;
      morphQuery = buildMorphQuery(booleanQuery, language, withAllForms, translate, languageHandler);
    } else {
      morphQuery = query; // all other cases: PrefixQuery, PhraseQuery, FuzzyQuery, TermRangeQuery, ...
    }
    return morphQuery;
  }

  private Query buildMorphQuery(TermQuery inputTermQuery, String fromLang, boolean withAllForms, boolean translate, LanguageHandler languageHandler) throws ApplicationException {
    String fromLanguage = null;
    String inputTerm = inputTermQuery.getTerm().text();
    if (fromLang == null || fromLang.isEmpty()) {
      String detectedLang = languageHandler.detectLanguage(inputTerm);
      if (detectedLang != null)
        fromLanguage = detectedLang;
    } else {
      fromLanguage = fromLang;
    }
    LexHandler lexHandler = LexHandler.getInstance();
    String fieldName = inputTermQuery.getTerm().field();
    ArrayList<TermQuery> queryTerms = new ArrayList<TermQuery>();
    if (fieldName != null && fieldName.equals("tokenMorph")) {
      ArrayList<Lemma> lemmas = lexHandler.getLemmas(inputTerm, "form", fromLanguage, Normalizer.DICTIONARY, true);
      if (lemmas == null) {  // if no lemmas are found then do a query in tokenOrig TODO should this really be done ?
        if (translate) {
          ArrayList<String> terms = new ArrayList<String>();
          terms.add(inputTerm);
          ArrayList<String> translatedTerms = languageHandler.getTranslations(terms, fromLanguage);
          if (translatedTerms == null || translatedTerms.isEmpty())
            translatedTerms = terms;
          for (int i=0; i<translatedTerms.size(); i++) {
            String translatedTerm = translatedTerms.get(i);
            Term translatedTermTokenOrig = new Term("tokenOrig", translatedTerm);
            TermQuery translatedTermQueryInTokenOrig = new TermQuery(translatedTermTokenOrig);
            queryTerms.add(translatedTermQueryInTokenOrig);
          }
        } else {
          Term termTokenOrig = new Term("tokenOrig", inputTerm);
          TermQuery termQueryInTokenOrig = new TermQuery(termTokenOrig);
          queryTerms.add(termQueryInTokenOrig);
        }
      } else {
        if (translate) {
          ArrayList<String> morphTerms = new ArrayList<String>();
          for (int i=0; i<lemmas.size(); i++) {
            Lemma lemma = lemmas.get(i);
            if (withAllForms) { // all word forms are put into the query as boolean or clauses: needed in fragments search when all forms should be highlighted
              ArrayList<Form> forms = lemma.getFormsList();
              for (int j=0; j<forms.size(); j++) {
                Form form = forms.get(j);
                String formName = form.getFormName();
                morphTerms.add(formName);
              } 
            } else {
              String lemmaName = lemma.getLemmaName();
              morphTerms.add(lemmaName);
            }
          }
          ArrayList<String> translatedMorphTerms = languageHandler.getTranslations(morphTerms, fromLanguage); 
          if (translatedMorphTerms == null || translatedMorphTerms.isEmpty())
            translatedMorphTerms = morphTerms;
          for (int i=0; i<translatedMorphTerms.size(); i++) {
            String translatedMorphTermStr = translatedMorphTerms.get(i);
            Term translatedMorphTerm = new Term(fieldName, translatedMorphTermStr);
            TermQuery translatedMorphTermQuery = new TermQuery(translatedMorphTerm);
            queryTerms.add(translatedMorphTermQuery);
          }
        } else {
          for (int i = 0; i < lemmas.size(); i++) {
            Lemma lemma = lemmas.get(i);
            if (withAllForms) { // all word forms are put into the query as boolean or clauses: needed in fragments search when all forms should be highlighted
              ArrayList<Form> forms = lemma.getFormsList();
              for (int j=0; j<forms.size(); j++) {
                Form form = forms.get(j);
                Term formTerm = new Term(fieldName, form.getFormName());
                TermQuery morphTermQuery = new TermQuery(formTerm);
                queryTerms.add(morphTermQuery);
              } 
            } else {
              Term lemmaTerm = new Term(fieldName, lemma.getLemmaName());
              TermQuery morphTermQuery = new TermQuery(lemmaTerm);
              queryTerms.add(morphTermQuery);
            }
          }
        }
      }
    } else {
      // if it is not the morph field then do a normal query 
      if (translate) {
        String inputTermQueryField = inputTermQuery.getTerm().field();
        ArrayList<String> terms = new ArrayList<String>();
        terms.add(inputTerm);
        ArrayList<String> translatedTerms = languageHandler.getTranslations(terms, fromLanguage);
        if (translatedTerms == null || translatedTerms.isEmpty())
          translatedTerms = terms;
        for (int i=0; i<translatedTerms.size(); i++) {
          String translatedTerm = translatedTerms.get(i);
          Term translatedTermTokenOrig = new Term(inputTermQueryField, translatedTerm);
          TermQuery translatedTermQueryInTokenOrig = new TermQuery(translatedTermTokenOrig);
          queryTerms.add(translatedTermQueryInTokenOrig);
        }
      } else {
        return inputTermQuery;
      }
    }
    Query retQuery = buildBooleanShouldQuery(queryTerms);
    return retQuery;
  }

  private Query buildBooleanShouldQuery(ArrayList<TermQuery> queryTerms) throws ApplicationException {
    BooleanQuery retBooleanQuery = new BooleanQuery();
    for (int i = 0; i < queryTerms.size(); i++) {
      TermQuery termQuery = queryTerms.get(i);
      retBooleanQuery.add(termQuery, BooleanClause.Occur.SHOULD);
    }
    return retBooleanQuery;
  }
  
  private Query buildMorphQuery(BooleanQuery query, String language, boolean withAllForms, boolean translate, LanguageHandler languageHandler) throws ApplicationException {
    BooleanQuery morphBooleanQuery = new BooleanQuery();
    BooleanClause[] booleanClauses = query.getClauses();
    for (int i = 0; i < booleanClauses.length; i++) {
      BooleanClause boolClause = booleanClauses[i];
      Query q = boolClause.getQuery();
      Query morphQuery = buildMorphQuery(q, language, withAllForms, translate, languageHandler);
      BooleanClause.Occur occur = boolClause.getOccur();
      morphBooleanQuery.add(morphQuery, occur);
    }
    return morphBooleanQuery;
  }

  public ArrayList<String> fetchTerms(String queryStr) throws ApplicationException {
    ArrayList<String> terms = null;
    String defaultQueryFieldName = "tokenOrig";
    try {
      Query query = new QueryParser(Version.LUCENE_35, defaultQueryFieldName, nodesPerFieldAnalyzer).parse(queryStr);
      terms = fetchTerms(query);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return terms;
  }

  /**
   * recursively fetch all terms of the query
   * 
   * @param query
   * @return
   */
  private ArrayList<String> fetchTerms(Query query) throws ApplicationException {
    ArrayList<String> terms = new ArrayList<String>();
    if (query instanceof TermQuery) {
      TermQuery termQuery = (TermQuery) query;
      String termText = termQuery.getTerm().text();
      if (! terms.contains(termText))
        terms.add(termText);
    } else if (query instanceof BooleanQuery) {
      BooleanQuery booleanQuery = (BooleanQuery) query;
      terms = fetchTerms(booleanQuery);
    } else if (query instanceof PrefixQuery) {
      PrefixQuery prefixQuery = (PrefixQuery) query;
      String termText = prefixQuery.getPrefix().text();
      if (! terms.contains(termText))
        terms.add(termText);
    } else if (query instanceof PhraseQuery) {
      PhraseQuery phraseQuery = (PhraseQuery) query;
      Term[] termsArray = phraseQuery.getTerms();
      if (termsArray != null) {
        for (int i=0; i<termsArray.length; i++) {
          Term t = termsArray[i];
          String termText = t.text();
          if (! terms.contains(termText))
            terms.add(t.text());
        }
      }
    } else {
      String queryStr = query.toString();
      terms.add(queryStr); // TODO: all other cases properly: FuzzyQuery, TermRangeQuery, ...
    }
    return terms;
  }

  private ArrayList<String> fetchTerms(BooleanQuery query) throws ApplicationException {
    ArrayList<String> terms = new ArrayList<String>();
    BooleanClause[] booleanClauses = query.getClauses();
    for (int i = 0; i < booleanClauses.length; i++) {
      BooleanClause boolClause = booleanClauses[i];
      Query q = boolClause.getQuery();
      ArrayList<String> qTerms = fetchTerms(q);
      BooleanClause.Occur occur = boolClause.getOccur();
      if (occur == BooleanClause.Occur.SHOULD || occur == BooleanClause.Occur.MUST) {
        for (int j=0; j<qTerms.size(); j++) {
          String qTerm = qTerms.get(j);
          if (! terms.contains(qTerm))
            terms.add(qTerm);
        }
      }
    }
    return terms;
  }

  public ArrayList<String> fetchTerms(String queryStr, String language) throws ApplicationException {
    ArrayList<String> terms = null;
    String defaultQueryFieldName = "tokenOrig";
    try {
      Query query = new QueryParser(Version.LUCENE_35, defaultQueryFieldName, nodesPerFieldAnalyzer).parse(queryStr);
      terms = fetchTerms(query, language);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return terms;
  }

  /**
   * recursively fetch all terms of the query
   * 
   * @param query
   * @return
   */
  private ArrayList<String> fetchTerms(Query query, String language) throws ApplicationException {
    ArrayList<String> terms = new ArrayList<String>();
    if (query instanceof TermQuery) {
      TermQuery termQuery = (TermQuery) query;
      terms = fetchTerms(termQuery, language);
    } else if (query instanceof BooleanQuery) {
      BooleanQuery booleanQuery = (BooleanQuery) query;
      terms = fetchTerms(booleanQuery, language);
    } else {
      String queryStr = query.toString();
      terms.add(queryStr); 
      // all other cases: PrefixQuery, PhraseQuery, FuzzyQuery, TermRangeQuery, ...
    }
    return terms;
  }

  private ArrayList<String> fetchTerms(TermQuery termQuery, String language) throws ApplicationException {
    if (language == null)
      language = "eng";
    ArrayList<String> terms = new ArrayList<String>();
    Term termQueryTerm = termQuery.getTerm();
    String term = termQuery.getTerm().text();
    String fieldName = termQueryTerm.field();
    if (fieldName != null && fieldName.equals("tokenMorph")) {
      LexHandler lexHandler = LexHandler.getInstance();
      ArrayList<Lemma> lemmas = lexHandler.getLemmas(term, "form", language, Normalizer.DICTIONARY, true); 
      // TODO : language über den translator service holen
      if (lemmas == null) {
        terms.add(term);
      } else {
        for (int i = 0; i < lemmas.size(); i++) {
          Lemma lemma = lemmas.get(i);
          ArrayList<Form> forms = lemma.getFormsList();
          for (int j = 0; j < forms.size(); j++) {
            Form form = forms.get(j);
            String formName = form.getFormName();
            terms.add(formName);
          }
        }
      }
    } else {
      terms.add(term);
    }
    return terms;
  }

  private ArrayList<String> fetchTerms(BooleanQuery query, String language) throws ApplicationException {
    ArrayList<String> terms = new ArrayList<String>();
    BooleanClause[] booleanClauses = query.getClauses();
    for (int i = 0; i < booleanClauses.length; i++) {
      BooleanClause boolClause = booleanClauses[i];
      Query q = boolClause.getQuery();
      ArrayList<String> qTerms = fetchTerms(q, language);
      BooleanClause.Occur occur = boolClause.getOccur();
      if (occur == BooleanClause.Occur.SHOULD || occur == BooleanClause.Occur.MUST)
        terms.addAll(qTerms);
    }
    return terms;
  }

  private Document getDocument(String docId) throws ApplicationException {
    Document doc = null;
    IndexSearcher searcher = null;
    try {
      makeDocumentsSearcherManagerUpToDate();
      searcher = documentsSearcherManager.acquire();
      String fieldNameDocId = "docId";
      String docIdQuery = docId;
      if (docId.contains(" ") || docId.contains(":"))
        docIdQuery = "\"" + docId + "\"";
      Query queryDocId = new QueryParser(Version.LUCENE_35, fieldNameDocId, documentsPerFieldAnalyzer).parse(docIdQuery);
      TopDocs topDocs = searcher.search(queryDocId, 100000);
      topDocs.setMaxScore(1);
      if (topDocs != null && topDocs.scoreDocs != null && topDocs.scoreDocs.length > 0) {
        int docID = topDocs.scoreDocs[0].doc;
        FieldSelector docFieldSelector = getDocFieldSelector();
        doc = searcher.doc(docID, docFieldSelector);
      }
      searcher.close();
    } catch (Exception e) {
      throw new ApplicationException(e);
    } finally {
      try {
        if (searcher != null)
          documentsSearcherManager.release(searcher);
      } catch (IOException e) {
        // nothing
      }
    }
    // Do not use searcher after this!
    searcher = null;
    return doc;
  }

  private int generateId() throws ApplicationException {
    int id = findMaxId() + 1;
    return id;
  }

  /**
   * Performance: needs ca. 10 ms per call
   * @return
   * @throws ApplicationException
   */
  public int findMaxId() throws ApplicationException {
    String maxIdStr = null;
    IndexSearcher searcher = null;
    try {
      makeDocumentsSearcherManagerUpToDate();
      searcher = documentsSearcherManager.acquire();
      String fieldNameId = "id";
      String idQuery = "[* TO *]";
      Query queryId = new QueryParser(Version.LUCENE_35, fieldNameId, documentsPerFieldAnalyzer).parse(idQuery);
      queryId = NumericRangeQuery.newIntRange(fieldNameId, 0, 10000000, false, false);
      Sort sortByIdReverse = new Sort(new SortField("id", SortField.INT, true));  // reverse order
      TopDocs topDocs = searcher.search(queryId, 1, sortByIdReverse);
      topDocs.setMaxScore(1);
      if (topDocs != null && topDocs.scoreDocs != null && topDocs.scoreDocs.length > 0) {
        int docID = topDocs.scoreDocs[0].doc;
        FieldSelector docFieldSelector = getDocFieldSelector();
        Document doc = searcher.doc(docID, docFieldSelector);
        if (doc != null) {
          Fieldable maxIdField = doc.getFieldable("id");
          maxIdStr = maxIdField.stringValue();
        }
      }
      
      searcher.close();
    } catch (Exception e) {
      throw new ApplicationException(e);
    } finally {
      try {
        if (searcher != null)
          documentsSearcherManager.release(searcher);
      } catch (IOException e) {
        // nothing
      }
    }
    // Do not use searcher after this!
    searcher = null;
    // return the maxId
    int maxId = 0;
    if (maxIdStr != null) {
      maxId = Integer.valueOf(maxIdStr).intValue();
    }
    return maxId;
  }
  
  public Hits moreLikeThis(String docId, int from, int to) throws ApplicationException {
    Hits hits = null;
    ArrayList<org.bbaw.wsp.cms.document.Document>  wspDocs = null;
    IndexSearcher searcher1 = null;
    IndexSearcher searcher2 = null;
    try {
      makeDocumentsSearcherManagerUpToDate();
      searcher1 = documentsSearcherManager.acquire();
      String fieldNameDocId = "docId";
      Query queryDocId = new QueryParser(Version.LUCENE_35, fieldNameDocId, documentsPerFieldAnalyzer).parse(docId);
      TopDocs topDocs = searcher1.search(queryDocId, 100000);
      topDocs.setMaxScore(1);
      int docID = -1;
      if (topDocs != null && topDocs.scoreDocs != null && topDocs.scoreDocs.length > 0) {
        docID = topDocs.scoreDocs[0].doc;
      }
      makeDocumentsSearcherManagerUpToDate();
      searcher2 = documentsSearcherManager.acquire();
      MoreLikeThis mlt = new MoreLikeThis(documentsIndexReader);  // TODO documentsIndexReader is ok ?
      mlt.setFieldNames(new String[]{"content"});  // similarity function works against these fields
      mlt.setMinWordLen(2);
      mlt.setBoost(true);
      Query queryMoreLikeThis = mlt.like(docID);
      TopDocs moreLikeThisDocs = searcher2.search(queryMoreLikeThis, 10);
      moreLikeThisDocs.setMaxScore(10);
      if (moreLikeThisDocs != null) { 
        if (wspDocs == null)
          wspDocs = new ArrayList<org.bbaw.wsp.cms.document.Document>();
        for (int i=0; i<moreLikeThisDocs.scoreDocs.length; i++) {
          int docIdent = moreLikeThisDocs.scoreDocs[i].doc;
          Document luceneDoc = searcher2.doc(docIdent);
          org.bbaw.wsp.cms.document.Document wspDoc = new org.bbaw.wsp.cms.document.Document(luceneDoc);
          wspDocs.add(wspDoc);
        }
      }
      if (wspDocs != null) {
        hits = new Hits(wspDocs, from, to);
        hits.setSize(moreLikeThisDocs.scoreDocs.length);
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    } finally {
      try {
        if (searcher1 != null)
          documentsSearcherManager.release(searcher1);
        if (searcher2 != null)
          documentsSearcherManager.release(searcher2);
      } catch (IOException e) {
        // nothing
      }
    }
    // Do not use searcher after this!
    searcher1 = null;
    searcher2 = null;
    
    return hits;
  }
  
  private IndexWriter getDocumentsWriter() throws ApplicationException {
    IndexWriter writer = null;
    String luceneDocsDirectoryStr = Constants.getInstance().getLuceneDocumentsDir();
    File luceneDocsDirectory = new File(luceneDocsDirectoryStr);
    try {
      Map<String, Analyzer> documentsFieldAnalyzers = new HashMap<String, Analyzer>();
      documentsFieldAnalyzers.put("id", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("docId", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("identifier", new KeywordAnalyzer()); 
      documentsFieldAnalyzers.put("uri", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("webUri", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("collectionNames", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("author", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("title", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("language", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("publisher", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("date", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("description", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("subject", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("subjectControlled", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("swd", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("ddc", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("rights", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("license", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("accessRights", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("type", new KeywordAnalyzer()); // e.g. mime type "text/xml"
      documentsFieldAnalyzers.put("pageCount", new KeywordAnalyzer()); 
      documentsFieldAnalyzers.put("schemaName", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("lastModified", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("tokenOrig", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("tokenReg", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("tokenNorm", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("tokenMorph", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("xmlContent", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("content", new StandardAnalyzer(Version.LUCENE_35));
      documentsPerFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_35), documentsFieldAnalyzers);
      IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, documentsPerFieldAnalyzer);
      conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
      conf.setRAMBufferSizeMB(300);  // 300 MB because some documents are big; 16 MB is default 
      conf.setMaxBufferedDeleteTerms(1);  // so that indexReader.terms() delivers immediately and without optimize() the correct number of terms
      FSDirectory fsDirectory = FSDirectory.open(luceneDocsDirectory);
      writer = new IndexWriter(fsDirectory, conf);
      writer.commit(); // when directory is empty this creates init files
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return writer;
  }

  private IndexWriter getNodesWriter() throws ApplicationException {
    IndexWriter writer = null;
    String luceneNodesDirectoryStr = Constants.getInstance().getLuceneNodesDir();
    File luceneNodesDirectory = new File(luceneNodesDirectoryStr);
    try {
      Map<String, Analyzer> nodesFieldAnalyzers = new HashMap<String, Analyzer>();
      nodesFieldAnalyzers.put("docId", new KeywordAnalyzer());
      nodesFieldAnalyzers.put("language", new StandardAnalyzer(Version.LUCENE_35)); // language (through xml:id): e.g. "lat"
      nodesFieldAnalyzers.put("pageNumber", new KeywordAnalyzer()); // page number (through element pb): e.g. "13"
      nodesFieldAnalyzers.put("lineNumber", new KeywordAnalyzer()); // line number on the page (through element lb): e.g. "17"
      nodesFieldAnalyzers.put("elementName", new KeywordAnalyzer()); // element name: e.g. "tei:s"
      nodesFieldAnalyzers.put("elementDocPosition", new KeywordAnalyzer()); // absolute position of element in document: e.g. "4711"
      nodesFieldAnalyzers.put("elementPosition", new KeywordAnalyzer()); // position in parent node (in relation to other nodes of the same name): e.g. "5"
      nodesFieldAnalyzers.put("elementAbsolutePosition", new KeywordAnalyzer()); // absolute position in document (in relation to other nodes of the same name): e.g. "213"
      nodesFieldAnalyzers.put("elementPagePosition", new KeywordAnalyzer()); // position in relation to other nodes of the same name: e.g. "213"
      nodesFieldAnalyzers.put("xmlId", new KeywordAnalyzer()); // xml id: e.g. "4711bla"
      nodesFieldAnalyzers.put("xpath", new KeywordAnalyzer()); // xpath: e.g. "/echo[1]/text[1]/p[1]/s[5]"
      nodesFieldAnalyzers.put("tokenOrig", new StandardAnalyzer(Version.LUCENE_35));
      nodesFieldAnalyzers.put("tokenReg", new StandardAnalyzer(Version.LUCENE_35));
      nodesFieldAnalyzers.put("tokenNorm", new StandardAnalyzer(Version.LUCENE_35));
      nodesFieldAnalyzers.put("tokenMorph", new StandardAnalyzer(Version.LUCENE_35));
      nodesFieldAnalyzers.put("xmlContent", new StandardAnalyzer(Version.LUCENE_35));
      nodesPerFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_35), nodesFieldAnalyzers);
      IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, nodesPerFieldAnalyzer);
      conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
      conf.setRAMBufferSizeMB(300);  // 300 MB because some documents are big; 16 MB is default 
      FSDirectory fsDirectory = FSDirectory.open(luceneNodesDirectory);
      writer = new IndexWriter(fsDirectory, conf);
      writer.commit();
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return writer;
  }

  private Sort buildSort(String[] sortFieldNames, String type) {
    Sort sort = new Sort();
    ArrayList<SortField> sortFields = new ArrayList<SortField>();
    for (int i=0; i<sortFieldNames.length; i++) {
      String sortFieldName = sortFieldNames[i];
      int sortFieldType = getDocSortFieldType(sortFieldName);
      if (type.equals("node"))
        sortFieldType = getNodeSortFieldType(sortFieldName);
      String realSortFieldName = getDocSortFieldName(sortFieldName);
      SortField sortField = new SortField(realSortFieldName, sortFieldType);
      sortFields.add(sortField);
    }
    if (sortFieldNames.length == 1) {
      SortField sortField1 = sortFields.get(0);
      sort.setSort(sortField1);
    } else if (sortFieldNames.length == 2) {
      SortField sortField1 = sortFields.get(0);
      SortField sortField2 = sortFields.get(1);
      sort.setSort(sortField1, sortField2);
    } else if (sortFieldNames.length == 3) {
      SortField sortField1 = sortFields.get(0);
      SortField sortField2 = sortFields.get(1);
      SortField sortField3 = sortFields.get(2);
      sort.setSort(sortField1, sortField2, sortField3);
    }
    return sort;
  }

  private String getDocSortFieldName(String fieldName) {
    String sortFieldName = fieldName + "Sorted";
    return sortFieldName;
  }

  private int getDocSortFieldType(String fieldName) {
    int type = SortField.STRING;
    if (fieldName.equals("lastModified"))
      type = SortField.LONG;
    return type;
  }
  
  private int getNodeSortFieldType(String fieldName) {
    int type = SortField.STRING;
    if (fieldName.equals("pageNumber") || fieldName.equals("lineNumber") || fieldName.equals("elementDocPosition")) 
      type = SortField.INT;
    return type;
  }

  private FieldSelector getDocFieldSelector() {
    HashSet<String> fields = new HashSet<String>();
    fields.add("id");
    fields.add("docId");
    fields.add("identifier");
    fields.add("uri");
    fields.add("webUri");
    fields.add("collectionNames");
    fields.add("author");
    fields.add("authorDetails");
    fields.add("title");
    fields.add("language");
    fields.add("publisher");
    fields.add("date");
    fields.add("description");
    fields.add("subject");
    fields.add("subjectControlled");
    fields.add("subjectControlledDetails");
    fields.add("swd");
    fields.add("ddc");
    fields.add("rights");
    fields.add("license");
    fields.add("type");
    fields.add("pageCount");
    fields.add("schemaName");
    fields.add("lastModified");
    fields.add("persons");
    fields.add("personsDetails");
    fields.add("places");
    fields.add("content");
    FieldSelector fieldSelector = new SetBasedFieldSelector(fields, fields);
    return fieldSelector;
  }
  
  private FieldSelector getNodeFieldSelector() {
    HashSet<String> fields = new HashSet<String>();
    fields.add("docId");
    fields.add("language");
    fields.add("pageNumber");
    fields.add("lineNumber");
    fields.add("elementName");
    fields.add("elementDocPosition");
    fields.add("elementPosition");
    fields.add("elementAbsolutePosition");
    fields.add("elementPagePosition");
    fields.add("xmlId");
    fields.add("xpath");
    fields.add("xmlContent");
    fields.add("xmlContentTokenized");
    FieldSelector fieldSelector = new SetBasedFieldSelector(fields, fields);
    return fieldSelector;
  }
  
  private SearcherManager getNewSearcherManager(IndexWriter indexWriter) throws ApplicationException {
    SearcherManager searcherManager = null;
    try {
      searcherManager = new SearcherManager(indexWriter, true, null, null);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return searcherManager;
  }

  private IndexReader getDocumentsReader() throws ApplicationException {
    IndexReader reader = null;
    String luceneDocsDirectoryStr = Constants.getInstance().getLuceneDocumentsDir();
    File luceneDocsDirectory = new File(luceneDocsDirectoryStr);
    try {
      FSDirectory fsDirectory = FSDirectory.open(luceneDocsDirectory);
      reader = IndexReader.open(fsDirectory, true);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return reader;
  }

  private TaxonomyWriter getTaxonomyWriter() throws ApplicationException {
    TaxonomyWriter taxonomyWriter = null;
    String taxonomyDirStr = Constants.getInstance().getLuceneTaxonomyDir();
    File taxonomyDirF = new File(taxonomyDirStr);
    try {
      Directory taxonomyDir = FSDirectory.open(taxonomyDirF);
      taxonomyWriter = new DirectoryTaxonomyWriter(taxonomyDir, OpenMode.CREATE_OR_APPEND);
      taxonomyWriter.commit();
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return taxonomyWriter;
  }
  
  private TaxonomyReader getTaxonomyReader() throws ApplicationException {
    TaxonomyReader taxonomyReader = null;
    String taxonomyDirStr = Constants.getInstance().getLuceneTaxonomyDir();
    File taxonomyDirF = new File(taxonomyDirStr);
    try {
      Directory taxonomyDir = FSDirectory.open(taxonomyDirF);
      taxonomyReader = new DirectoryTaxonomyReader(taxonomyDir);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return taxonomyReader;
  }

  private CategoryDocumentBuilder getCategoryDocumentBuilder() throws ApplicationException {
    CategoryDocumentBuilder categoryDocumentBuilder = null;
    try {
      categoryDocumentBuilder = new CategoryDocumentBuilder(taxonomyWriter);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return categoryDocumentBuilder;
  }
  
  private void makeIndexReaderUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = documentsIndexReader.isCurrent();
      if (!isCurrent) {
        documentsIndexReader = IndexReader.openIfChanged(documentsIndexReader);
      }
      taxonomyReader.refresh();
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  private void makeDocumentsSearcherManagerUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = documentsSearcherManager.isSearcherCurrent();
      if (!isCurrent) {
        documentsSearcherManager.maybeReopen();
      }
      taxonomyReader.refresh(); 
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  private void makeNodesSearcherManagerUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = nodesSearcherManager.isSearcherCurrent();
      if (!isCurrent) {
        nodesSearcherManager.maybeReopen();
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  private String toTokenizedXmlString(String xmlStr, String language) throws ApplicationException {
    String xmlPre = "<tokenized xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:m=\"http://www.w3.org/1998/Math/MathML\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:svg=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">";
    String xmlPost = "</tokenized>";
    String xmlStrTmp = xmlPre + xmlStr + xmlPost;
    StringReader xmlInputStringReader = new StringReader(xmlStrTmp);
    XmlTokenizer xmlTokenizer = new XmlTokenizer(xmlInputStringReader);
    xmlTokenizer.setLanguage(language);
    String[] outputOptions = { "withLemmas" };
    xmlTokenizer.setOutputOptions(outputOptions);
    xmlTokenizer.tokenize();
    String result = xmlTokenizer.getXmlResult();
    return result;
  }

  private String escapeLuceneChars(String inputStr) {
    String luceneCharsStr = "+-&|!(){}[]^~*?:\\"; // Lucene escape symbols
    StringBuilder retStrBuilder = new StringBuilder();
    for (int i = 0; i < inputStr.length(); i++) {
      char c = inputStr.charAt(i);
      if (luceneCharsStr.contains(String.valueOf(c)))
        retStrBuilder.append("\\");
      retStrBuilder.append(c);
    }
    return retStrBuilder.toString();
  }

  /**
   * sorgt für sinnvolle satzanfänge
   * 
   * @param fragment
   */
  private String checkHitFragment(String fragment) {
    if (fragment.startsWith(".") 
        || fragment.startsWith(":") 
        || fragment.startsWith(",") 
        || fragment.startsWith("-") 
        || fragment.startsWith(";") 
        || fragment.startsWith("?")
        || fragment.startsWith(")") 
        || fragment.startsWith("!")) {
      fragment = fragment.substring(1, fragment.length());
      // finds first occurence of a given string out.println("first index of point : "+StringUtils.indexOfAny(fragment, "."));
    }
    return fragment;
  }

}