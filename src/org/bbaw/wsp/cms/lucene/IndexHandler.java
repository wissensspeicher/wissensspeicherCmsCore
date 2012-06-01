package org.bbaw.wsp.cms.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.bbaw.wsp.cms.document.DocumentHandler;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.dict.db.LexHandler;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.Form;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.Lemma;
import de.mpg.mpiwg.berlin.mpdl.lt.text.norm.Normalizer;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizer;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.XmlTokenizerContentHandler;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;

public class IndexHandler {
  private static IndexHandler instance;
  private IndexWriter documentsIndexWriter;
  private IndexWriter nodesIndexWriter;
  private SearcherManager documentsSearcherManager;
  private SearcherManager nodesSearcherManager;
  private IndexReader documentsIndexReader;
  private PerFieldAnalyzerWrapper documentsPerFieldAnalyzer;
  private PerFieldAnalyzerWrapper nodesPerFieldAnalyzer;
  
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
  }
  
  public void indexDocument(CmsDocOperation docOperation) throws ApplicationException {
    try {
      // first delete document in documentsIndex and nodesIndex
      deleteDocumentLocal(docOperation);
      indexDocumentLocal(docOperation);
      documentsIndexWriter.commit();
      nodesIndexWriter.commit();
    } catch (Exception e) {
      try {
        documentsIndexWriter.rollback();
        nodesIndexWriter.rollback();
      } catch (Exception ex) {
        // nothing
      }
      throw new ApplicationException(e);
    }    
  }
  
  private void indexDocumentLocal(CmsDocOperation docOperation) throws ApplicationException {
    FileReader fr = null;
    try {
      MetadataRecord mdRecord = docOperation.getMdRecord();
      String docIdentifier = docOperation.getDocIdentifier();
      DocumentHandler docHandler = new DocumentHandler();
      String docFileName = docHandler.getDocFullFileName(docIdentifier);
      // add document to documentsIndex
      Document doc = new Document();
      String docId = mdRecord.getDocId();
      Field docIdField = new Field("docId", docId, Field.Store.YES, Field.Index.ANALYZED);
      doc.add(docIdField);
      String identifier = mdRecord.getIdentifier();
      if (identifier != null) {
        Field identifierField = new Field("identifier", identifier, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(identifierField);
      }
      String echoId = mdRecord.getEchoId();
      if (echoId != null) {
        Field echoIdField = new Field("echoId", echoId, Field.Store.YES, Field.Index.ANALYZED);
        doc.add(echoIdField);
      }
      if (mdRecord.getCreator() != null) {
        Field authorField = new Field("author", mdRecord.getCreator(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(authorField);
      }
      if (mdRecord.getTitle() != null) {
        Field titleField = new Field("title", mdRecord.getTitle(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
        doc.add(titleField);
      }
      if (mdRecord.getLanguage() != null) {
        Field languageField = new Field("language", mdRecord.getLanguage(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(languageField);
      }
      if (mdRecord.getYear() != null) {
        Field dateField = new Field("date", mdRecord.getYear(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(dateField);
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
      }
      if (mdRecord.getSchemaName() != null) {
        Field schemaField = new Field("schemaName", mdRecord.getSchemaName(), Field.Store.YES, Field.Index.ANALYZED);
        doc.add(schemaField);
      }
      
      String language = mdRecord.getLanguage();
      InputStreamReader docFileReader = new InputStreamReader(new FileInputStream(docFileName), "utf-8");  // to guarantee that utf-8 is used (if not done, it does not work on Tomcat which has another default charset)
      XmlTokenizer docXmlTokenizer = new XmlTokenizer(docFileReader);
      docXmlTokenizer.setDocIdentifier(docId);
      docXmlTokenizer.setLanguage(language);
      docXmlTokenizer.setOutputFormat("string");
      String[] outputOptionsWithLemmas = {"withLemmas"}; // so all tokens are fetched with lemmas (costs performance)
      docXmlTokenizer.setOutputOptions(outputOptionsWithLemmas); 
      String[] normFunctionNone = {"none"};
      docXmlTokenizer.setNormFunctions(normFunctionNone);
      docXmlTokenizer.tokenize();  

      int pageCount = docXmlTokenizer.getPageCount();
      String pageCountStr = String.valueOf(pageCount);
      Field pageCountField = new Field("pageCount", pageCountStr, Field.Store.YES, Field.Index.ANALYZED);
      doc.add(pageCountField);
      
      String[] outputOptionsEmpty = {};
      docXmlTokenizer.setOutputOptions(outputOptionsEmpty); // must be set to null so that the normalization function works 
      
      String docTokensOrig = docXmlTokenizer.getStringResult();
      String[] normFunctionReg = {"reg"};
      docXmlTokenizer.setNormFunctions(normFunctionReg);
      String docTokensReg = docXmlTokenizer.getStringResult();
      String[] normFunctionNorm = {"norm"};
      docXmlTokenizer.setNormFunctions(normFunctionNorm);
      String docTokensNorm = docXmlTokenizer.getStringResult();
      docXmlTokenizer.setOutputOptions(outputOptionsWithLemmas); 
      String docTokensMorph = docXmlTokenizer.getStringResult();

      Field tokenOrigField = new Field("tokenOrig", docTokensOrig, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
      Field tokenRegField = new Field("tokenReg", docTokensReg, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
      Field tokenNormField = new Field("tokenNorm", docTokensNorm, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
      Field tokenMorphField = new Field("tokenMorph", docTokensMorph, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
      doc.add(tokenOrigField);
      doc.add(tokenRegField);
      doc.add(tokenNormField);
      doc.add(tokenMorphField);
      
      documentsIndexWriter.addDocument(doc);

      // add all elements with the specified names of the document to nodesIndex
      String elementNames = docOperation.getElementNames();
      ArrayList<XmlTokenizerContentHandler.Element> elements = docXmlTokenizer.getElements(elementNames);
      for (int i=0; i<elements.size(); i++) {
        XmlTokenizerContentHandler.Element element = elements.get(i);
        Document nodeDoc = new Document();
        nodeDoc.add(docIdField);
        String nodeLanguage = element.lang;
        if (nodeLanguage == null)
          nodeLanguage = language;
        String nodePageNumber = String.valueOf(element.pageNumber);
        String nodeLineNumber = String.valueOf(element.lineNumber);
        String nodeElementName = String.valueOf(element.name);
        String nodeElementPosition = String.valueOf(element.elemPosition);
        String nodeElementAbsolutePosition = String.valueOf(element.position);
        String nodeElementPagePosition = String.valueOf(element.pagePosition);
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
        Field nodeElementPositionField = new Field("elementPosition", nodeElementPosition, Field.Store.YES, Field.Index.ANALYZED);
        nodeDoc.add(nodeElementPositionField);
        Field nodeElementAbsolutePositionField = new Field("elementAbsolutePosition", nodeElementAbsolutePosition, Field.Store.YES, Field.Index.ANALYZED);
        nodeDoc.add(nodeElementAbsolutePositionField);
        Field nodeElementPagePositionField = new Field("elementPagePosition", nodeElementPagePosition, Field.Store.YES, Field.Index.ANALYZED);
        nodeDoc.add(nodeElementPagePositionField);
        if (nodeXmlId != null) {
          Field nodeXmlIdField = new Field("xmlId", nodeXmlId, Field.Store.YES, Field.Index.ANALYZED);
          nodeDoc.add(nodeXmlIdField);
        }
        if (nodeXpath != null) {
          Field nodeXpathField = new Field("xpath", nodeXpath, Field.Store.YES, Field.Index.ANALYZED);
          nodeDoc.add(nodeXpathField);
        }
        if (nodeXmlContent != null) {
          Field nodeXmlContentField = new Field("xmlContent", nodeXmlContent, Field.Store.YES, Field.Index.NO);  // stored but not indexed
          nodeDoc.add(nodeXmlContentField);
        }
        if (nodeXmlContent != null) {
          String nodeXmlContentTokenized = toTokenizedXmlString(nodeXmlContent, nodeLanguage);
          Field nodeXmlContentTokenizedField = new Field("xmlContentTokenized", nodeXmlContentTokenized, Field.Store.YES, Field.Index.NO);  // stored but not indexed
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
    } catch (Exception e) {
      throw new ApplicationException(e);
    } finally {
      try {
        if (fr != null)
          fr.close();
      } catch (Exception e) {
        // nothing
      }
    }    
  }

  public void deleteDocument(CmsDocOperation docOperation) throws ApplicationException {
    try {
      deleteDocumentLocal(docOperation);
      documentsIndexWriter.commit();
      nodesIndexWriter.commit();
    } catch (Exception e) {
      try {
        documentsIndexWriter.rollback();
        nodesIndexWriter.rollback();
      } catch (Exception ex) {
        // nothing
      }
      throw new ApplicationException(e);
    }    
  }
  
  private void deleteDocumentLocal(CmsDocOperation docOperation) throws ApplicationException {
    String docId = docOperation.getDocIdentifier();
    try {
      Term termIdentifier = new Term("docId", docId); 
      documentsIndexWriter.deleteDocuments(termIdentifier);
      nodesIndexWriter.deleteDocuments(termIdentifier);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }    
  }
  
  public ArrayList<Document> queryDocuments(String queryStr, String language) throws ApplicationException {
    ArrayList<Document> docs = null;
    IndexSearcher searcher = null;
    try {
      makeDocumentsSearcherManagerUpToDate();
      searcher = documentsSearcherManager.acquire();
      String defaultQueryFieldName = "tokenOrig";
      Query query = new QueryParser(Version.LUCENE_35, defaultQueryFieldName, documentsPerFieldAnalyzer).parse(queryStr);
      Query morphQuery = buildMorphQuery(query, language);
      TopDocs topDocs = searcher.search(morphQuery, 1000);
      topDocs.setMaxScore(1);
      if (topDocs != null) {
        if (docs == null)
          docs = new ArrayList<Document>();
        for (int i=0; i<topDocs.scoreDocs.length; i++) {
          int docID = topDocs.scoreDocs[i].doc;
          Document doc = searcher.doc(docID);
          docs.add(doc);
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
    return docs;
  }
  
  public ArrayList<Document> queryDocument(String docId, String queryStr) throws ApplicationException {
    ArrayList<Document> docs = null;
    IndexSearcher searcher = null;
    MetadataRecord docMetadataRecord = getDocMetadata(docId);
    try {
      makeNodesSearcherManagerUpToDate();
      searcher = nodesSearcherManager.acquire();
      String fieldNameDocId = "docId";
      Query queryDocId = new QueryParser(Version.LUCENE_35, fieldNameDocId, nodesPerFieldAnalyzer).parse(docId);
      String defaultQueryFieldName = "tokenOrig";
      Query query = new QueryParser(Version.LUCENE_35, defaultQueryFieldName, nodesPerFieldAnalyzer).parse(queryStr);
      String language = docMetadataRecord.getLanguage();
      Query morphQuery = buildMorphQuery(query, language);
      BooleanQuery queryDoc = new BooleanQuery();
      queryDoc.add(queryDocId, BooleanClause.Occur.MUST);
      queryDoc.add(morphQuery, BooleanClause.Occur.MUST);
      Sort sortByPosition = new Sort(new SortField("position", SortField.INT));
      TopDocs topDocs = searcher.search(queryDoc, 100000, sortByPosition);
      topDocs.setMaxScore(1);
      if (topDocs != null) {
        if (docs == null)
          docs = new ArrayList<Document>();
        for (int i=0; i<topDocs.scoreDocs.length; i++) {
          int docID = topDocs.scoreDocs[i].doc;
          Document doc = searcher.doc(docID);
          docs.add(doc);
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
    return docs;
  }

  public MetadataRecord getDocMetadata(String docId) throws ApplicationException {
    MetadataRecord mdRecord = null;
    Document doc = getDocument(docId);
    if (doc != null) {
      String identifier = null;
      Fieldable identifierField = doc.getFieldable("identifier");
      if (identifierField != null)
        identifier = identifierField.stringValue();
      String echoId = null;
      Fieldable echoIdField = doc.getFieldable("echoId");
      if (echoIdField != null)
        echoId = echoIdField.stringValue();
      String author = null;
      Fieldable authorField = doc.getFieldable("author");
      if (authorField != null)
        author = authorField.stringValue();
      String title = null;
      Fieldable titleField = doc.getFieldable("title");
      if (titleField != null)
        title = titleField.stringValue();
      String language = null;
      Fieldable languageField = doc.getFieldable("language");
      if (languageField != null)
        language = languageField.stringValue();
      Date yearDate = null;
      Fieldable dateField = doc.getFieldable("date");
      if (dateField != null) {
        String dateStr = dateField.stringValue();
        if (dateStr != null && ! dateStr.equals("")) {
          dateStr = StringUtils.deresolveXmlEntities(dateStr);
          String yearStr = new Util().toYearStr(dateStr);  // test if possible etc
          if (yearStr != null) {
            yearDate = new Util().toDate(yearStr + "-01-01T00:00:00.000Z");
          }
        }
      }
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
      mdRecord.setIdentifier(identifier);
      mdRecord.setEchoId(echoId);
      mdRecord.setCreator(author);
      mdRecord.setTitle(title);
      mdRecord.setDate(yearDate);
      mdRecord.setLanguage(language);
      mdRecord.setLicense(license);
      mdRecord.setRights(rights);
      mdRecord.setAccessRights(accessRights);
      mdRecord.setPageCount(pageCount);
      mdRecord.setSchemaName(schemaName);
      mdRecord.setLastModified(lastModified);
    }
    return mdRecord;
  }
  
  public ArrayList<Term> getTerms(String fieldName, String value, int count) throws ApplicationException {
    ArrayList<Term> retTerms = null;
    int counter = 0;
    TermEnum terms = null;
    try {
      if (value == null)
        value = "";
      Term term = new Term(fieldName, value); 
      makeIndexReaderUpToDate();
      terms = documentsIndexReader.terms(term);
      while (terms != null && fieldName != null && fieldName.equals(terms.term().field()) && counter < count) {
        if (retTerms == null)
          retTerms = new ArrayList<Term>();
        Term termContent = terms.term();
        retTerms.add(termContent);
        counter++;
        if (!terms.next())
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
    return retTerms;
  }
  
  public ArrayList<Term> getTerms(String docId, String fieldName, String value, int count) throws ApplicationException {
    ArrayList<Term> retTerms = null;
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
          boolean success = false;
          if (terms != null) {
            retTerms = new ArrayList<Term>();
            for (int i=0; i<terms.length; i++) {
              String termStr = terms[i];
              if (termStr.startsWith(value))
                success = true;
              if (success) {
                counter++;
                Term t = new Term(fieldName, termStr);
                retTerms.add(t);
              }
              if (counter >= count)
                break;
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
    return retTerms;
  }
  
  public void end() throws ApplicationException {
    try {
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
  
  /**
   * recursively build the morphological query
   * @param query
   * @return
   */
  private Query buildMorphQuery(Query query, String language) throws ApplicationException {
    Query morphQuery = null;
    if (query instanceof TermQuery) {
      TermQuery termQuery = (TermQuery) query;
      morphQuery = buildMorphQuery(termQuery, language);
    } else if (query instanceof BooleanQuery) {
      BooleanQuery booleanQuery = (BooleanQuery) query;
      morphQuery = buildMorphQuery(booleanQuery, language);
    } else {
      morphQuery = query; // all other cases: PrefixQuery, PhraseQuery, FuzzyQuery, TermRangeQuery, ...
    }
    return morphQuery;
  }
  
  private Query buildMorphQuery(TermQuery termQuery, String language) throws ApplicationException {
    Query morphQuery = null;
    String term = termQuery.getTerm().text();
    String fieldName = termQuery.getTerm().field();
    if (fieldName != null && fieldName.equals("tokenMorph")) {
      LexHandler lexHandler = LexHandler.getInstance();
      ArrayList<Lemma> lemmas = lexHandler.getLemmas(term, "form", language, Normalizer.DICTIONARY, true);  // TODO : language über den translator service holen
      if (lemmas == null) {
        // if no lemmas are found then do a query in tokenOrig  TODO should this really be done ?
        Term termTokenOrig = new Term("tokenOrig", term);
        TermQuery termQueryInTokenOrig = new TermQuery(termTokenOrig);
        morphQuery = termQueryInTokenOrig;
      } else {
        if (lemmas.size() == 1) {
          Lemma lemma = lemmas.get(0);
          Term lemmaTerm = new Term(fieldName, lemma.getLemmaName());
          morphQuery = new TermQuery(lemmaTerm);
        } else {
          BooleanQuery morphBooleanQuery = new BooleanQuery();
          for (int i=0; i<lemmas.size(); i++) {
            Lemma lemma = lemmas.get(i);
            Term lemmaTerm = new Term(fieldName, lemma.getLemmaName());
            TermQuery morphTermQuery = new TermQuery(lemmaTerm);
            morphBooleanQuery.add(morphTermQuery, BooleanClause.Occur.SHOULD);
          }
          morphQuery = morphBooleanQuery;
        }
      }
    } else {
      morphQuery = termQuery;  // if it is not the morph field then do a normal query TODO ?? perhaps other fields should also be queried morphological e.g. title etc.
    }
    return morphQuery;
  }

  private Query buildMorphQuery(BooleanQuery query, String language) throws ApplicationException {
    BooleanQuery morphBooleanQuery = new BooleanQuery();
    BooleanClause[] booleanClauses = query.getClauses();
    for (int i=0; i<booleanClauses.length; i++) {
      BooleanClause boolClause = booleanClauses[i];
      Query q = boolClause.getQuery();
      Query morphQuery = buildMorphQuery(q, language);
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
   * @param query
   * @return
   */
  private ArrayList<String> fetchTerms(Query query) throws ApplicationException {
    ArrayList<String> terms = new ArrayList<String>();
    if (query instanceof TermQuery) {
      TermQuery termQuery = (TermQuery) query;
      String termQueryStr = termQuery.getTerm().text();
      terms.add(termQueryStr);
    } else if (query instanceof BooleanQuery) {
      BooleanQuery booleanQuery = (BooleanQuery) query;
      terms = fetchTerms(booleanQuery);
    } else {
      String queryStr = query.toString();
      terms.add(queryStr); // all other cases: PrefixQuery, PhraseQuery, FuzzyQuery, TermRangeQuery, ...
    }
    return terms;
  }
  
  private ArrayList<String> fetchTerms(BooleanQuery query) throws ApplicationException {
    ArrayList<String> terms = new ArrayList<String>();
    BooleanClause[] booleanClauses = query.getClauses();
    for (int i=0; i<booleanClauses.length; i++) {
      BooleanClause boolClause = booleanClauses[i];
      Query q = boolClause.getQuery();
      ArrayList<String> qTerms = fetchTerms(q);
      BooleanClause.Occur occur = boolClause.getOccur();
      if (occur == BooleanClause.Occur.SHOULD || occur == BooleanClause.Occur.MUST)
        terms.addAll(qTerms);
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
      terms.add(queryStr); // all other cases: PrefixQuery, PhraseQuery, FuzzyQuery, TermRangeQuery, ...
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
      ArrayList<Lemma> lemmas = lexHandler.getLemmas(term, "form", language, Normalizer.DICTIONARY, true);  // TODO : language über den translator service holen
      if (lemmas == null) {
        terms.add(term);
      } else {
        for (int i=0; i<lemmas.size(); i++) {
          Lemma lemma = lemmas.get(i);
          ArrayList<Form> forms = lemma.getFormsList();
          for (int j=0; j<forms.size(); j++) {
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
    for (int i=0; i<booleanClauses.length; i++) {
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
      Query queryDocId = new QueryParser(Version.LUCENE_35, fieldNameDocId, documentsPerFieldAnalyzer).parse(docId);
      TopDocs topDocs = searcher.search(queryDocId, 100000);
      topDocs.setMaxScore(1);
      if (topDocs != null && topDocs.scoreDocs != null && topDocs.scoreDocs.length > 0) {
        int docID = topDocs.scoreDocs[0].doc;
        doc = searcher.doc(docID);
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
  
  private IndexWriter getDocumentsWriter() throws ApplicationException {
    IndexWriter writer = null;
    String luceneDocsDirectoryStr = Constants.getInstance().getLuceneDocumentsDir();
    File luceneDocsDirectory = new File(luceneDocsDirectoryStr);
    try {
      Map<String, Analyzer> documentsFieldAnalyzers = new HashMap<String, Analyzer>();
      documentsFieldAnalyzers.put("docId", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("identifier", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("echoId", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("author", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("title", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("language", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("date", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("rights", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("license", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("accessRights", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("type", new KeywordAnalyzer());  // e.g. mime type "text/xml"
      documentsFieldAnalyzers.put("pageCount", new KeywordAnalyzer());  
      documentsFieldAnalyzers.put("schemaName", new KeywordAnalyzer());  
      documentsFieldAnalyzers.put("lastModified", new KeywordAnalyzer());  
      documentsFieldAnalyzers.put("tokenOrig", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("tokenReg", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("tokenNorm", new StandardAnalyzer(Version.LUCENE_35));
      documentsFieldAnalyzers.put("tokenMorph", new StandardAnalyzer(Version.LUCENE_35));
      documentsPerFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_35), documentsFieldAnalyzers);
      IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, documentsPerFieldAnalyzer);
      conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
      FSDirectory fsDirectory = FSDirectory.open(luceneDocsDirectory);
      writer = new IndexWriter(fsDirectory, conf);
      writer.commit();  // when directory is empty this creates init files
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
      nodesFieldAnalyzers.put("identifier", new KeywordAnalyzer());
      nodesFieldAnalyzers.put("language", new KeywordAnalyzer());  // language (through xml:id): e.g. "lat"
      nodesFieldAnalyzers.put("pageNumber", new KeywordAnalyzer());  // page number (through element pb): e.g. "13"
      nodesFieldAnalyzers.put("lineNumber", new KeywordAnalyzer());  // line number on the page (through element lb): e.g. "17"
      nodesFieldAnalyzers.put("elementName", new KeywordAnalyzer());  // element name: e.g. "tei:s"
      nodesFieldAnalyzers.put("elementPosition", new KeywordAnalyzer());  // position in parent node (in relation to other nodes of the same name): e.g. "5"
      nodesFieldAnalyzers.put("elementAbsolutePosition", new KeywordAnalyzer());  // absolute position in document (in relation to other nodes of the same name): e.g. "213"
      nodesFieldAnalyzers.put("elementPagePosition", new KeywordAnalyzer());  // position in relation to other nodes of the same name: e.g. "213"
      nodesFieldAnalyzers.put("xmlId", new KeywordAnalyzer());  // xml id: e.g. "4711bla"
      nodesFieldAnalyzers.put("xpath", new KeywordAnalyzer());  // xpath: e.g. "/echo[1]/text[1]/p[1]/s[5]"
      nodesFieldAnalyzers.put("tokenOrig", new StandardAnalyzer(Version.LUCENE_35));
      nodesFieldAnalyzers.put("tokenReg", new StandardAnalyzer(Version.LUCENE_35));
      nodesFieldAnalyzers.put("tokenNorm", new StandardAnalyzer(Version.LUCENE_35));
      nodesFieldAnalyzers.put("tokenMorph", new StandardAnalyzer(Version.LUCENE_35));
      nodesPerFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_35), nodesFieldAnalyzers);
      IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, nodesPerFieldAnalyzer);
      conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
      FSDirectory fsDirectory = FSDirectory.open(luceneNodesDirectory);
      writer = new IndexWriter(fsDirectory, conf);
      writer.commit();
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return writer;
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

  private void makeIndexReaderUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = documentsIndexReader.isCurrent();
      if (! isCurrent) {
        documentsIndexReader = IndexReader.openIfChanged(documentsIndexReader);
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void makeDocumentsSearcherManagerUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = documentsSearcherManager.isSearcherCurrent();
      if (! isCurrent) {
        documentsSearcherManager.maybeReopen();
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private void makeNodesSearcherManagerUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = nodesSearcherManager.isSearcherCurrent();
      if (! isCurrent) {
        nodesSearcherManager.maybeReopen();
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private String toTokenizedXmlString(String xmlStr, String language) throws ApplicationException {
    String xmlPre = "<tokenized xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">";
    String xmlPost = "</tokenized>";
    String xmlStrTmp = xmlPre + xmlStr + xmlPost;
    StringReader xmlInputStringReader = new StringReader(xmlStrTmp);
    XmlTokenizer xmlTokenizer = new XmlTokenizer(xmlInputStringReader);
    xmlTokenizer.setLanguage(language);
    String[] outputOptions = {"withLemmas"};
    xmlTokenizer.setOutputOptions(outputOptions);
    xmlTokenizer.tokenize();
    String result = xmlTokenizer.getXmlResult();
    return result;
  }

  private String escapeLuceneChars(String inputStr) {
    String luceneCharsStr = "+-&|!(){}[]^~*?:\\";  // Lucene escape symbols
    StringBuilder retStrBuilder = new StringBuilder();
    for (int i=0; i<inputStr.length(); i++) {
      char c = inputStr.charAt(i);
      if (luceneCharsStr.contains(String.valueOf(c)))
        retStrBuilder.append("\\");
      retStrBuilder.append(c);
    }
    return retStrBuilder.toString();
  }

  
}