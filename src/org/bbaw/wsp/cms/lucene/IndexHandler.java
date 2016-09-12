package org.bbaw.wsp.cms.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.suggest.tst.TSTLookup;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.bbaw.wsp.cms.collections.Project;
import org.bbaw.wsp.cms.collections.ProjectReader;
import org.bbaw.wsp.cms.document.DBpediaResource;
import org.bbaw.wsp.cms.document.Facets;
import org.bbaw.wsp.cms.document.Hits;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.document.Token;
import org.bbaw.wsp.cms.document.TokenArrayListIterator;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.general.Constants;
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
  private static final String[] QUERY_EXPANSION_FIELDS_ALL = {"author", "title", "description", "subject", "subjectControlled", "swd", "ddc", "entities", "persons", "places", "tokenOrig"};
  private static final String[] QUERY_EXPANSION_FIELDS_ALL_MORPH = {"author", "title", "description", "subject", "subjectControlled", "swd", "ddc", "entities", "persons", "places", "tokenMorph"};
  private HashMap<String, Integer> facetFieldCounts = new HashMap<String, Integer>();
  private IndexWriter documentsIndexWriter;
  private IndexWriter nodesIndexWriter;
  private SearcherManager documentsSearcherManager;
  private SearcherManager nodesSearcherManager;
  private DirectoryReader documentsIndexReader;
  private PerFieldAnalyzerWrapper documentsPerFieldAnalyzer;
  private PerFieldAnalyzerWrapper nodesPerFieldAnalyzer;
  private ArrayList<Token> tokens; // all tokens in tokenOrig
  private TSTLookup suggester;
  private TaxonomyWriter taxonomyWriter;
  private TaxonomyReader taxonomyReader;
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

  public void init() throws ApplicationException {
    documentsIndexWriter = getDocumentsWriter();
    nodesIndexWriter = getNodesWriter();
    documentsSearcherManager = getNewSearcherManager(documentsIndexWriter);
    nodesSearcherManager = getNewSearcherManager(nodesIndexWriter);
    documentsIndexReader = getDocumentsReader();
    taxonomyWriter = getTaxonomyWriter();
    taxonomyReader = getTaxonomyReader();
    xQueryEvaluator = new XQueryEvaluator();
    facetFieldCounts.put("projectId", 1000);
    facetFieldCounts.put("language", 1000);
    facetFieldCounts.put("author", 10);
    facetFieldCounts.put("publisher", 10);
    facetFieldCounts.put("date", 10);
    facetFieldCounts.put("subject", 10);
    facetFieldCounts.put("subjectControlled", 10);
    facetFieldCounts.put("swd", 10);
    facetFieldCounts.put("ddc", 10);
    facetFieldCounts.put("entityPerson", 100);
    facetFieldCounts.put("entityOrganisation", 100);
    facetFieldCounts.put("entityPlace", 100);
    facetFieldCounts.put("entityConcept", 100);
    facetFieldCounts.put("type", 1000);
    Date before = new Date();
    tokens = getToken("tokenOrig", "", 10000000); // get all token: needs ca. 4 sec. for 3 Mio tokens
    Date after = new Date();
    LOGGER.info("Reading of all tokens in Lucene index successfully with: " + tokens.size() + " tokens (" + "elapsed time: " + (after.getTime() - before.getTime()) + " ms)");
  }

  public void reopenTaxonomyReader() throws ApplicationException {
    try {
      if (taxonomyReader != null)
        taxonomyReader.close();
      taxonomyReader = getTaxonomyReader();
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
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
  
  public void indexDocument(MetadataRecord mdRecord) throws ApplicationException {
    commitCounter++;
    // first delete document in documentsIndex and nodesIndex
    deleteDocumentLocal(mdRecord);
    indexDocumentLocal(mdRecord);
    // performance gain (each commit needs at least ca. 60 ms extra): a commit is only done if commitInterval (e.g. 500) is reached 
    if (commitCounter >= commitInterval) {
      commit();
      commitCounter = 0;
    }
  }

  private void indexDocumentLocal(MetadataRecord mdRecord) throws ApplicationException {
    try {
      Document doc = new Document();
      FieldType ftStoredAnalyzed = new FieldType();
      ftStoredAnalyzed.setStored(true);
      ftStoredAnalyzed.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
      ftStoredAnalyzed.setTokenized(true);
      ftStoredAnalyzed.setStoreTermVectors(true);
      ftStoredAnalyzed.setStoreTermVectorOffsets(true);
      ftStoredAnalyzed.setStoreTermVectorPositions(true);
      ftStoredAnalyzed.setStoreTermVectorPayloads(true);
      FieldType ftStoredNotAnalyzed = new FieldType();
      ftStoredNotAnalyzed.setStored(true);
      ftStoredNotAnalyzed.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
      ftStoredNotAnalyzed.setTokenized(false);
      FieldType ftNumericStored = new FieldType();
      ftNumericStored.setStored(true);      
      ftNumericStored.setIndexOptions(IndexOptions.DOCS);
      ftNumericStored.setOmitNorms(true);
      ftNumericStored.setNumericType(FieldType.NumericType.INT);
      ftNumericStored.setDocValuesType(DocValuesType.NUMERIC);
      int id = mdRecord.getId(); // short id (auto incremented)
      if (id != -1) { 
        Field idField = new IntField("id", id, ftNumericStored);
        doc.add(idField);
      }
      String docId = mdRecord.getDocId();
      Field docIdField = new Field("docId", docId, ftStoredAnalyzed); 
      doc.add(docIdField);
      String docIdSortedStr = docId.toLowerCase();  // so that sorting is lower case
      Field docIdFieldSorted = new SortedDocValuesField("docIdSorted", new BytesRef(docIdSortedStr)); // the sort field is indexed but not stored (no doc(docId) is possible in this field
      doc.add(docIdFieldSorted);
      String identifier = mdRecord.getIdentifier();
      if (identifier != null) {
        Field identifierField = new Field("identifier", identifier, ftStoredAnalyzed);
        doc.add(identifierField);
      }
      String uri = mdRecord.getUri();
      if (uri != null) {
        Field uriField = new Field("uri", uri, ftStoredAnalyzed);
        doc.add(uriField);
      }
      String project = mdRecord.getProjectId();
      if (project != null) {
        Field projectField = new Field("projectId", project, ftStoredAnalyzed);
        if (project.equals("jdg"))
          projectField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        doc.add(projectField);
        FacetField facetField = new FacetField("projectId", project);
        doc.add(facetField);
      }
      String projectRdfId = mdRecord.getProjectRdfId();
      if (projectRdfId != null) {
        Field projectRdfIdField = new Field("projectRdfId", projectRdfId, ftStoredAnalyzed);
        if (project.equals("jdg"))
          projectRdfIdField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        doc.add(projectRdfIdField);
        FacetField facetField = new FacetField("projectRdfId", projectRdfId);
        doc.add(facetField);
      }
      String collectionRdfId = mdRecord.getCollectionRdfId();
      if (collectionRdfId != null) {
        Field collectionRdfIdField = new Field("collectionRdfId", collectionRdfId, ftStoredAnalyzed);
        if (project.equals("jdg"))
          collectionRdfIdField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        doc.add(collectionRdfIdField);
        FacetField facetField = new FacetField("collectionRdfId", collectionRdfId);
        doc.add(facetField);
      }
      String dbRdfId = mdRecord.getDatabaseRdfId();
      if (dbRdfId != null) {
        Field databaseRdfIdField = new Field("databaseRdfId", dbRdfId, ftStoredAnalyzed);
        if (project.equals("jdg"))
          databaseRdfIdField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        doc.add(databaseRdfIdField);
        FacetField facetField = new FacetField("databaseRdfId", dbRdfId);
        doc.add(facetField);
      }
      String creator = mdRecord.getCreator();
      if (creator != null) {
        Field authorField = new Field("author", creator, ftStoredAnalyzed);
        doc.add(authorField);
        if (project.equals("jdg"))
          authorField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        if (creator != null)
          creator = creator.toLowerCase();  // so that sorting is lower case
        Field authorFieldSorted = new SortedDocValuesField("authorSorted", new BytesRef(creator));
        doc.add(authorFieldSorted);
      } else {
        FacetField facetField = new FacetField("author", "unbekannt");
        doc.add(facetField);
      }
      String creatorDetails = mdRecord.getCreatorDetails();
      if (creatorDetails != null) {
        Field authorDetailsField = new StoredField("authorDetails", creatorDetails);
        doc.add(authorDetailsField);
        ArrayList<Person> authors = Person.fromXmlStr(xQueryEvaluator, creatorDetails);
        for (int i=0; i<authors.size(); i++) {
          Person person = authors.get(i);
          String authorName = person.getName();
          if (! authorName.isEmpty()) {
            FacetField facetField = new FacetField("author", authorName);
            doc.add(facetField);
          }
        }
      }
      if (mdRecord.getTitle() != null) {
        Field titleField = new Field("title", mdRecord.getTitle(), ftStoredAnalyzed);
        doc.add(titleField);
        if (project.equals("jdg"))
          titleField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        String titleStr = mdRecord.getTitle();
        if (titleStr != null)
          titleStr = titleStr.toLowerCase();  // so that sorting is lower case
        Field titleFieldSorted = new SortedDocValuesField("titleSorted", new BytesRef(titleStr));
        doc.add(titleFieldSorted);
      }
      String publisher = mdRecord.getPublisher();
      if (publisher != null) {
        String[] publishers = publisher.split(";");
        for (int i=0; i<publishers.length; i++) {
          String p = publishers[i].trim();
          if (! p.isEmpty()) {
            FacetField facetField = new FacetField("publisher", p);
            doc.add(facetField);
          }
        }
        Field publisherField = new Field("publisher", publisher, ftStoredAnalyzed);
        doc.add(publisherField);
        if (project.equals("jdg"))
          publisherField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        if (publisher != null)
          publisher = publisher.toLowerCase();  // so that sorting is lower case
        Field publisherFieldSorted = new SortedDocValuesField("publisherSorted", new BytesRef(publisher));
        doc.add(publisherFieldSorted);
      } else {
        FacetField facetField = new FacetField("publisher", "unbekannt");
        doc.add(facetField);
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
        Field dateField = new Field("date", yearStr, ftStoredAnalyzed);
        doc.add(dateField);
        if (project.equals("jdg"))
          dateField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        Field dateFieldSorted = new SortedDocValuesField("dateSorted", new BytesRef(yearStr));
        doc.add(dateFieldSorted);
        FacetField facetField = new FacetField("date", yearStr);
        doc.add(facetField);
      } else {
        FacetField facetField = new FacetField("date", "unbekannt");
        doc.add(facetField);
      }
      if (mdRecord.getDescription() != null) {
        Field descriptionField = new Field("description", mdRecord.getDescription(), ftStoredAnalyzed);
        doc.add(descriptionField);
        if (project.equals("jdg"))
          descriptionField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String subject = mdRecord.getSubject();
      if (subject != null) {
        Field subjectField = new Field("subject", subject, ftStoredAnalyzed);
        doc.add(subjectField);
        if (project.equals("jdg"))
          subjectField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        String[] subjects = subject.split(",");
        if (subject.contains(";"))
          subjects = subject.split(";");
        if (subject.contains("###"))
          subjects = subject.split("###");
        for (int i=0; i<subjects.length; i++) {
          String s = subjects[i].trim();
          if (! s.isEmpty()) {
            FacetField facetField = new FacetField("subject", s);
            doc.add(facetField);
          }
        }
      } else {
        FacetField facetField = new FacetField("subject", "unbekannt");
        doc.add(facetField);
      }
      String subjectControlledDetails = mdRecord.getSubjectControlledDetails();
      if (subjectControlledDetails != null) {
        String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
        String dcTermsSubjectsStr = xQueryEvaluator.evaluateAsStringValueJoined(subjectControlledDetails, namespaceDeclaration + "/subjects/dcterms:subject/rdf:Description/rdfs:label", "###");
        Field subjectControlledField = new Field("subjectControlled", dcTermsSubjectsStr, ftStoredAnalyzed);
        doc.add(subjectControlledField);
        Field subjectControlledDetailsField = new Field("subjectControlledDetails", subjectControlledDetails, ftStoredNotAnalyzed);
        doc.add(subjectControlledDetailsField);
        String[] dcTermsSubjects = dcTermsSubjectsStr.split("###");
        for (int i=0; i<dcTermsSubjects.length; i++) {
          String s = dcTermsSubjects[i].trim();
          if (! s.isEmpty()) {
            FacetField facetField = new FacetField("subjectControlled", s);
            doc.add(facetField);
          }
        }
      }
      String swd = mdRecord.getSwd();
      if (swd != null) {
        Field swdField = new Field("swd", swd, ftStoredAnalyzed);
        doc.add(swdField);
        if (project.equals("jdg"))
          swdField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        String[] swds = swd.split(",");
        if (swd.contains(";"))
          swds = swd.split(";");
        for (int i=0; i<swds.length; i++) {
          String s = swds[i].trim();
          if (! s.isEmpty()) {
            FacetField facetField = new FacetField("swd", s);
            doc.add(facetField);
          }
        }
      } else {
        FacetField facetField = new FacetField("swd", "unbekannt");
        doc.add(facetField);
      }
      if (mdRecord.getDdc() != null) {
        Field ddcField = new Field("ddc", mdRecord.getDdc(), ftStoredAnalyzed);
        doc.add(ddcField);
        if (project.equals("jdg"))
          ddcField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        FacetField facetField = new FacetField("ddc", mdRecord.getDdc());
        doc.add(facetField);
      } else {
        FacetField facetField = new FacetField("ddc", "unbekannt");
        doc.add(facetField);
      }
      if (mdRecord.getRights() != null) {
        Field rightsField = new Field("rights", mdRecord.getRights(), ftStoredAnalyzed);
        doc.add(rightsField);
      }
      if (mdRecord.getLicense() != null) {
        Field licenseField = new Field("license", mdRecord.getLicense(), ftStoredAnalyzed);
        doc.add(licenseField);
      }
      if (mdRecord.getAccessRights() != null) {
        Field accessRightsField = new Field("accessRights", mdRecord.getAccessRights(), ftStoredAnalyzed);
        doc.add(accessRightsField);
      }
      if (mdRecord.getLastModified() != null) {
        Date lastModified = mdRecord.getLastModified();
        String xsDateStr = new Util().toXsDate(lastModified);
        Field lastModifiedField = new Field("lastModified", xsDateStr, ftStoredAnalyzed);
        doc.add(lastModifiedField);
        long time = lastModified.getTime();
        String timeStr = String.valueOf(time);
        Field lastModifiedFieldSorted = new SortedDocValuesField("lastModifiedSorted", new BytesRef(timeStr));
        doc.add(lastModifiedFieldSorted);
      }
      if (mdRecord.getSchemaName() != null) {
        Field schemaField = new Field("schemaName", mdRecord.getSchemaName(), ftStoredAnalyzed);
        doc.add(schemaField);
        String schemaStr = mdRecord.getSchemaName();
        if (schemaStr != null)
          schemaStr = schemaStr.toLowerCase();  // so that sorting is lower case
        Field schemaFieldSorted = new SortedDocValuesField("schemaNameSorted", new BytesRef(schemaStr));
        doc.add(schemaFieldSorted);
      }
      if (mdRecord.getType() != null) {
        Field typeField = new Field("type", mdRecord.getType(), ftStoredAnalyzed);
        doc.add(typeField);
        if (project.equals("jdg"))
          typeField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        FacetField facetField = new FacetField("type", mdRecord.getType());
        doc.add(facetField);
      } else {
        FacetField facetField = new FacetField("type", "unbekannt");
        doc.add(facetField);
      }
      if (mdRecord.getSystem() != null) {
        Field systemTypeField = new Field("systemType", mdRecord.getSystem(), ftStoredNotAnalyzed);
        doc.add(systemTypeField);
      }
      if (mdRecord.getEntities() != null) {
        Field entitiesField = new Field("entities", mdRecord.getEntities(), ftStoredAnalyzed);
        doc.add(entitiesField);
        if (project.equals("jdg"))
          entitiesField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      if (mdRecord.getEntitiesDetails() != null) {
        ArrayList<DBpediaResource> entities = DBpediaResource.fromXmlStr(xQueryEvaluator, mdRecord.getEntitiesDetails());
        String entitiesUris = "";
        for (int i=0; i<entities.size(); i++) {
          DBpediaResource entity = entities.get(i);
          String entityType = entity.getType();
          String entityName = entity.getName();
          String entityUri = entity.getUri();
          entitiesUris = entitiesUris + " " + entityUri;
          String entityFacetStr = entityName + "<uri>" + entityUri + "</uri>";
          String entityGnd = entity.getGnd();
          if (entityGnd != null && ! entityGnd.isEmpty()) {
            entityFacetStr = entityFacetStr + "<gnd>" + entityGnd + "</gnd>";
            entitiesUris = entitiesUris + " " + entityGnd;
          }
          if (entityType != null && entityType.equals("person")) {
            FacetField facetField = new FacetField("entityPerson", entityFacetStr);
            doc.add(facetField);
          } else if (entityType != null && entityType.equals("organisation")) {
            FacetField facetField = new FacetField("entityOrganisation", entityFacetStr);
            doc.add(facetField);
          } else if (entityType != null && entityType.equals("place")) {
            FacetField facetField = new FacetField("entityPlace", entityFacetStr);
            doc.add(facetField);
          } else if (entityType != null && entityType.equals("concept")) {
            FacetField facetField = new FacetField("entityConcept", entityFacetStr);
            doc.add(facetField);
          } else {
            FacetField facetField = new FacetField("entityConcept", entityFacetStr);
            doc.add(facetField);
          }
        }
        Field entitiesDetailsField = new StoredField("entitiesDetails", mdRecord.getEntitiesDetails());
        doc.add(entitiesDetailsField);
        Field entitiesUrisField = new Field("entitiesUris", entitiesUris, ftStoredAnalyzed);
        doc.add(entitiesUrisField);
      }
      if (mdRecord.getPersons() != null) {
        Field personsField = new Field("persons", mdRecord.getPersons(), ftStoredAnalyzed);
        doc.add(personsField);
        if (project.equals("jdg"))
          personsField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      if (mdRecord.getPersonsDetails() != null) {
        Field personsDetailsField = new StoredField("personsDetails", mdRecord.getPersonsDetails());
        doc.add(personsDetailsField);
      }
      if (mdRecord.getPlaces() != null) {
        Field placesField = new Field("places", mdRecord.getPlaces(), ftStoredAnalyzed);
        doc.add(placesField);
        if (project.equals("jdg"))
          placesField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String language = mdRecord.getLanguage();
      if (language == null) {
        Project p = ProjectReader.getInstance().getProject(project);
        String mainLanguage = p.getMainLanguage();
        if (mainLanguage != null) {
          language = mainLanguage;
          mdRecord.setLanguage(mainLanguage);
        }
      }
      if (language != null) {
        Field languageField = new Field("language", mdRecord.getLanguage(), ftStoredAnalyzed);
        doc.add(languageField);
        if (project.equals("jdg"))
          languageField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        String langStr = mdRecord.getLanguage();
        if (langStr != null)
          langStr = langStr.toLowerCase();  // so that sorting is lower case
        Field languageFieldSorted = new SortedDocValuesField("languageSorted", new BytesRef(langStr));
        doc.add(languageFieldSorted);
        FacetField facetField = new FacetField("language", mdRecord.getLanguage());
        doc.add(facetField);
      } else {
        FacetField facetField = new FacetField("language", "unbekannt");
        doc.add(facetField);
      }
      int pageCount = mdRecord.getPageCount();
      if (pageCount != -1) {
        String pageCountStr = String.valueOf(pageCount);
        Field pageCountField = new Field("pageCount", pageCountStr, ftStoredAnalyzed);
        doc.add(pageCountField);
      }
      String docTokensOrig = mdRecord.getTokenOrig();
      if (docTokensOrig != null) {
        Field tokenOrigField = new Field("tokenOrig", docTokensOrig, ftStoredAnalyzed);
        doc.add(tokenOrigField);
        if (project.equals("jdg"))
          tokenOrigField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String docTokensReg = mdRecord.getTokenReg();
      if (docTokensReg != null) {
        Field tokenRegField = new Field("tokenReg", docTokensReg, ftStoredAnalyzed);
        doc.add(tokenRegField);
        if (project.equals("jdg"))
          tokenRegField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String docTokensNorm = mdRecord.getTokenNorm();
      if (docTokensNorm != null) {
        Field tokenNormField = new Field("tokenNorm", docTokensNorm, ftStoredAnalyzed);
        doc.add(tokenNormField);
        if (project.equals("jdg"))
          tokenNormField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String docTokensMorph = mdRecord.getTokenMorph();
      if (docTokensMorph != null) {
        Field tokenMorphField = new Field("tokenMorph", docTokensMorph, ftStoredAnalyzed);
        doc.add(tokenMorphField);
        if (project.equals("jdg"))
          tokenMorphField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String contentXml = mdRecord.getContentXml();
      if (contentXml != null) {
        Field contentXmlField = new Field("xmlContent", contentXml, ftStoredAnalyzed);
        doc.add(contentXmlField);
        if (project.equals("jdg"))
          contentXmlField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String content = mdRecord.getContent();
      if (content != null) {
        Field contentField = new Field("content", content, ftStoredAnalyzed);
        doc.add(contentField);
        if (project.equals("jdg"))
          contentField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
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
            if (webId != null)
              webId = StringUtils.resolveXmlEntities(webId);
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
        Field webUriField = new Field("webUri", webUri, ftStoredAnalyzed);
        doc.add(webUriField);
      }

      // facet creation
      FacetsConfig facetsConfig = new FacetsConfig();
      facetsConfig.setMultiValued("author", true);
      facetsConfig.setMultiValued("publisher", true);
      facetsConfig.setMultiValued("subject", true);
      facetsConfig.setMultiValued("subjectControlled", true);
      facetsConfig.setMultiValued("swd", true);
      facetsConfig.setMultiValued("entityPerson", true);
      facetsConfig.setMultiValued("entityOrganisation", true);
      facetsConfig.setMultiValued("entityPlace", true);
      facetsConfig.setMultiValued("entityConcept", true);
      Document docWithFacets = facetsConfig.build(taxonomyWriter, doc);

      documentsIndexWriter.addDocument(docWithFacets);
      
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
      LOGGER.error("indexDocumentLocal: " + mdRecord.getDocId());
      e.printStackTrace();
      throw new ApplicationException(e);
    }
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
  
  private void deleteDocumentLocal(MetadataRecord mdRecord) throws ApplicationException {
    String docId = mdRecord.getDocId();
    try {
      Term termIdentifier = new Term("docId", docId);
      documentsIndexWriter.deleteDocuments(termIdentifier);
      nodesIndexWriter.deleteDocuments(termIdentifier);
    } catch (Exception e) {
      LOGGER.error("deleteDocumentLocal: " + mdRecord.getDocId());
      throw new ApplicationException(e);
    }
  }

  public int deleteProject(String projectId) throws ApplicationException {
    int countDeletedDocs = -1;
    try {
      String queryDocumentsByProjectId = "projectId:" + projectId;
      if (projectId.equals("edoc"))
        queryDocumentsByProjectId = "webUri:*edoc.bbaw.de*";
      Hits projectDocHits = queryDocuments("lucene", queryDocumentsByProjectId, null, null, null, 0, 9, false, false); // TODO FastTaxonomyFacetCounts verursacht bei vielen deletes ArrayIndexOutOfBoundsException: 600000
      ArrayList<org.bbaw.wsp.cms.document.Document> projectDocs = projectDocHits.getHits();
      if (projectDocs != null) {
        countDeletedDocs = projectDocHits.getSize();
        // delete all nodes of each document in project
        for (int i=0; i<projectDocs.size(); i++) {
          org.bbaw.wsp.cms.document.Document doc = projectDocs.get(i);
          IndexableField docIdField = doc.getField("docId");
          String docId = docIdField.stringValue();
          Term termDocId = new Term("docId", docId);
          nodesIndexWriter.deleteDocuments(termDocId);
        }
        // delete all documents in project
        if (projectId.equals("edoc")) {
          Term term = new Term("webUri", "*edoc.bbaw.de*");
          Query deleteQuery = new WildcardQuery(term);
          documentsIndexWriter.deleteDocuments(deleteQuery);
        } else {
          Term termProjectName = new Term("projectId", projectId);
          documentsIndexWriter.deleteDocuments(termProjectName);
        }
      }
      commit();
    } catch (Exception e) {
      LOGGER.error("Lucene IndexHandler: deleteProject: " + e.getMessage());
      e.printStackTrace();
      throw new ApplicationException(e);
    }
    return countDeletedDocs;
  }

  public int deleteProjectDBByRdfId(String databaseRdfId) throws ApplicationException {
    String queryDatabaseRdfId = "databaseRdfId:" + databaseRdfId;
    Hits databaseDocHits = queryDocuments("lucene", queryDatabaseRdfId, null, null, null, 0, 9, false, false);
    int countDeletedDocs = databaseDocHits.getSize();
    try {
      Term termDatabaseRdfId = new Term("databaseRdfId", databaseRdfId);
      documentsIndexWriter.deleteDocuments(termDatabaseRdfId);
      commit();
    } catch (Exception e) {
      LOGGER.error("Lucene IndexHandler: deleteProjectDBByName: " + e.getMessage());
      e.printStackTrace();
      throw new ApplicationException(e);
    }
    return countDeletedDocs;
  }

  public int deleteProjectDBByType(String projectId, String dbType) throws ApplicationException {
    int countDeletedDocs = 0;
    try {
      TermQuery termQueryProjectName = new TermQuery(new Term("projectId", projectId));
      TermQuery termQuerySystemType = new TermQuery(new Term("systemType", dbType));
      ArrayList<TermQuery> deleteQueryTerms = new ArrayList<TermQuery>();
      deleteQueryTerms.add(termQueryProjectName);
      deleteQueryTerms.add(termQuerySystemType);      
      Query deleteQuery = buildBooleanShouldQuery(deleteQueryTerms);
      Hits docHits = queryDocuments("lucene", deleteQuery.toString(), null, null, null, 0, 9, false, false);
      countDeletedDocs = docHits.getSize();
      documentsIndexWriter.deleteDocuments(deleteQuery);
      commit();
    } catch (Exception e) {
      LOGGER.error("Lucene IndexHandler: deleteProjectDBByType: " + e.getMessage());
      e.printStackTrace();
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
      QueryParser queryParser = new QueryParser(defaultQueryFieldName, documentsPerFieldAnalyzer);
      queryParser.setAllowLeadingWildcard(true);
      Query query = null;
      Hashtable<String, String[]> facetConstraints = null;
      if (queryStr.equals("*")) {
        query = new MatchAllDocsQuery();
      } else {
        if (queryLanguage != null && queryLanguage.equals("gl")) {
          queryStr = translateGoogleLikeQueryToLucene(queryStr);
        }
        facetConstraints = getFacetConstraints(queryStr);
        query = buildFieldExpandedQuery(queryParser, queryStr, fieldExpansion);
      }
      LanguageHandler languageHandler = new LanguageHandler();
      if (translate) {
        ArrayList<String> queryTerms = fetchTerms(query);
        languageHandler.translate(queryTerms, language);
      }
      Query morphQuery = buildMorphQuery(query, language, false, translate, languageHandler);
      Query highlighterQuery = buildHighlighterQuery(query, language, translate, languageHandler);
      Sort sort = new Sort();
      SortField scoreSortField = new SortField(null, Type.SCORE); // default sort
      sort.setSort(scoreSortField);
      if (sortFieldNames != null) {
        sort = buildSort(sortFieldNames, "doc");  // build sort criteria
      }
      FacetsCollector facetsCollector = new FacetsCollector(true);
      TopDocs resultDocs = FacetsCollector.search(searcher, morphQuery, to + 1, sort, true, true, facetsCollector);
      FacetsConfig facetsConfig = new FacetsConfig();
      FastTaxonomyFacetCounts facetCounts = new FastTaxonomyFacetCounts(taxonomyReader, facetsConfig, facetsCollector);
      List<FacetResult> tmpFacetResult = new ArrayList<FacetResult>();
      String[] facetsStr = {"projectId", "projectRdfId", "collectionRdfId", "databaseRdfId", "language", "author", "publisher", "date", "subject", "subjectControlled", "swd", "ddc", "entityPerson", "entityOrganisation", "entityPlace", "entityConcept", "type"};
      for (int f=0; f<facetsStr.length; f++) {
        String facetStr = facetsStr[f];
        Integer facetFieldCount = 1000;
        Integer tmpFacetFieldCount = facetFieldCounts.get(facetStr);
        if (tmpFacetFieldCount != null)
          facetFieldCount = tmpFacetFieldCount;
        FacetResult facetResult = facetCounts.getTopChildren(facetFieldCount, facetStr);
        if (facetResult != null)
          tmpFacetResult.add(facetResult);
      }
      Facets facets = null;
      if (! tmpFacetResult.isEmpty()) {
        facets = new Facets(tmpFacetResult, facetConstraints);
      }
      int toTmp = to;
      if (resultDocs.totalHits <= to)
        toTmp = resultDocs.totalHits - 1;
      HashSet<String> docFields = getDocFields();
      if (resultDocs != null) {
        ArrayList<org.bbaw.wsp.cms.document.Document> docs = new ArrayList<org.bbaw.wsp.cms.document.Document>();
        ArrayList<Float> scores = new ArrayList<Float>();
        FastVectorHighlighter highlighter = new FastVectorHighlighter(true, false); // fieldMatch: false: cause any field to be highlighted regardless of whether the query matched specifically on them. The default behaviour is true, meaning that only fields that hold a query match will be highlighted.
        for (int i=from; i<=toTmp; i++) { 
          int docID = resultDocs.scoreDocs[i].doc;
          float score = resultDocs.scoreDocs[i].score;
          Document luceneDoc = searcher.doc(docID, docFields);
          org.bbaw.wsp.cms.document.Document doc = new org.bbaw.wsp.cms.document.Document(luceneDoc);
          if (withHitFragments) {
            ArrayList<String> hitFragments = new ArrayList<String>();
            IndexableField docContentField = luceneDoc.getField("content");
            if (docContentField != null && highlighterQuery != null) {
              FieldQuery highlighterFieldQuery = highlighter.getFieldQuery(highlighterQuery, documentsIndexReader);  // indexReader muss angegeben werden, damit die WildcardQuery etc. Ergebnisse liefert
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
          scores.add(score);
        }
        // fetch the "best" hits
        boolean withBestHits = false;  // TODO
        Hits bestHits = null;
        if (withBestHits && resultDocs != null) {
          int numberOfBestHits = 1000;
          int maxDocsForEachProject = 5;
          TopFieldCollector topFieldCollectorBestDocs = TopFieldCollector.create(sort, numberOfBestHits, true, true, true); 
          searcher.search(morphQuery, topFieldCollectorBestDocs);
          TopDocs resultBestDocs = topFieldCollectorBestDocs.topDocs();
          ArrayList<org.bbaw.wsp.cms.document.Document> bestDocs = new ArrayList<org.bbaw.wsp.cms.document.Document>();
          ArrayList<Float> bestScores = new ArrayList<Float>();
          Hashtable<String, Integer> collCounters = new Hashtable<String, Integer>();
          for (int i=0; i<numberOfBestHits; i++) { 
            int docID = resultBestDocs.scoreDocs[i].doc;
            float score = resultBestDocs.scoreDocs[i].score;
            HashSet<String> docCollectionNameField = getDocFieldProjectId();
            Document luceneDoc = searcher.doc(docID, docCollectionNameField);
            String collName = luceneDoc.getField("projectId").stringValue();
            Integer collCounter = collCounters.get(collName);
            if (collCounter == null) {
              collCounter = new Integer(0);
            }
            collCounter++;
            collCounters.put(collName, collCounter);
            if (collCounter <= maxDocsForEachProject) {
              Document fullLuceneDoc = searcher.doc(docID, docFields);
              org.bbaw.wsp.cms.document.Document fullDoc = new org.bbaw.wsp.cms.document.Document(fullLuceneDoc);
              bestDocs.add(fullDoc);
              bestScores.add(score);
            }
          }
          int bestTo = from + numberOfBestHits;
          if (from == 0)
            bestTo = numberOfBestHits - 1;
          bestHits = new Hits(bestDocs, from, bestTo);
        }
        int sizeTotalDocuments = documentsIndexReader.numDocs();
        // terms.size() does not work: delivers -1
        // Terms terms = MultiFields.getTerms(documentsIndexReader, "tokenOrig");
        // int sizeTotalTerms = (int) terms.size(); // TODO test performance
        int sizeTotalTerms = tokens.size(); // term count over tokenOrig
        long sizeTotalTermsFreq = documentsIndexReader.getSumTotalTermFreq("tokenOrig");  // count of all words/terms in this field TODO
        if (docs != null) {
          hits = new Hits(docs, from, to);
          hits.setMaxScore(resultDocs.getMaxScore());
          hits.setScores(scores);
          hits.setSize(resultDocs.totalHits);
          hits.setSizeTotalDocuments(sizeTotalDocuments);
          hits.setSizeTotalTerms(sizeTotalTerms);
          hits.setQuery(morphQuery);
          hits.setFacets(facets);
          // hits.setBestHits(bestHits); // TODO
        }
      }
    } catch (Exception e) {
      LOGGER.error("Lucene IndexHandler: queryDocuments: " + e.getMessage());
      e.printStackTrace();
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

  private Hashtable<String, String[]> getFacetConstraints(String luceneQueryStr) {
    Hashtable<String, String[]> facetConstraints = null;
    if (luceneQueryStr.contains("author:")) {
      String authorFieldValues = luceneQueryStr.replaceAll("(.*?)author:\\((.*?)\\)(.*)", "$2");  // TODO weitere Felder
      if (authorFieldValues != null) {
        String[] fieldValues = authorFieldValues.split(" \"");
        if (fieldValues != null) {
          for (int i=0; i<fieldValues.length; i++)
            fieldValues[i] = fieldValues[i].replaceAll("\"", "");
        }
        facetConstraints = new Hashtable<String, String[]>();
        facetConstraints.put("author", fieldValues);
      }
    }
    return facetConstraints;
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
      QueryParser queryParser1 = new QueryParser(fieldNameDocId, nodesPerFieldAnalyzer);
      queryParser1.setAllowLeadingWildcard(true);
      Query queryDocId = queryParser1.parse(docId);
      String defaultQueryFieldName = "tokenOrig";
      QueryParser queryParser2 = new QueryParser(defaultQueryFieldName, nodesPerFieldAnalyzer);
      queryParser2.setAllowLeadingWildcard(true);
      Query query = queryParser2.parse(queryStr);
      String language = docMetadataRecord.getLanguage();
      if (language == null || language.equals("")) {
        String projectId = docMetadataRecord.getProjectId();
        Project project = ProjectReader.getInstance().getProject(projectId);
        if (project != null) {
          String mainLang = project.getMainLanguage();
          if (mainLang != null)
            language = mainLang;
        } 
      }
      LanguageHandler languageHandler = new LanguageHandler();
      Query morphQuery = buildMorphQuery(query, language, languageHandler);
      BooleanQuery.Builder queryDocBuilder = new BooleanQuery.Builder();
      queryDocBuilder.add(queryDocId, BooleanClause.Occur.MUST);
      queryDocBuilder.add(morphQuery, BooleanClause.Occur.MUST);
      BooleanQuery queryDoc = queryDocBuilder.build();
      Sort sortByPosition = new Sort(new SortField("position", Type.INT));
      TopDocs topDocs = searcher.search(queryDoc, 100000, sortByPosition);
      topDocs.setMaxScore(1);
      int toTmp = to;
      if (topDocs.scoreDocs.length <= to)
        toTmp = topDocs.scoreDocs.length - 1;
      if (topDocs != null) {
        ArrayList<org.bbaw.wsp.cms.document.Document>  docs = new ArrayList<org.bbaw.wsp.cms.document.Document>();
        for (int i=from; i<=toTmp; i++) {
          int docID = topDocs.scoreDocs[i].doc;
          HashSet<String> nodeFields = getNodeFields();
          Document luceneDoc = searcher.doc(docID, nodeFields);
          org.bbaw.wsp.cms.document.Document doc = new org.bbaw.wsp.cms.document.Document(luceneDoc);
          String pageNumber = "-1";
          IndexableField fPageNumber = doc.getField("pageNumber");
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
      IndexableField idField = doc.getField("id");
      if (idField != null) {
        String idStr = idField.stringValue();
        id = Integer.valueOf(idStr);
      }
      String identifier = null;
      IndexableField identifierField = doc.getField("identifier");
      if (identifierField != null)
        identifier = identifierField.stringValue();
      String uri = null;
      IndexableField uriField = doc.getField("uri");
      if (uriField != null)
        uri = uriField.stringValue();
      String webUri = null;
      IndexableField webUriField = doc.getField("webUri");
      if (webUriField != null)
        webUri = webUriField.stringValue();
      String projectId = null;
      IndexableField projectIdField = doc.getField("projectId");
      if (projectIdField != null)
        projectId = projectIdField.stringValue();
      String projectRdfId = null;
      IndexableField projectRdfIdField = doc.getField("projectRdfId");
      if (projectRdfIdField != null)
        projectRdfId = projectRdfIdField.stringValue();
      String collectionRdfId = null;
      IndexableField collectionRdfIdField = doc.getField("collectionRdfId");
      if (collectionRdfIdField != null)
        collectionRdfId = collectionRdfIdField.stringValue();
      String databaseRdfId = null;
      IndexableField databaseRdfIdField = doc.getField("databaseRdfId");
      if (databaseRdfIdField != null)
        databaseRdfId = databaseRdfIdField.stringValue();
      String author = null;
      IndexableField authorField = doc.getField("author");
      if (authorField != null)
        author = authorField.stringValue();
      String authorDetails = null;
      IndexableField authorDetailsField = doc.getField("authorDetails");
      if (authorDetailsField != null)
        authorDetails = authorDetailsField.stringValue();
      String title = null;
      IndexableField titleField = doc.getField("title");
      if (titleField != null)
        title = titleField.stringValue();
      String language = null;
      IndexableField languageField = doc.getField("language");
      if (languageField != null)
        language = languageField.stringValue();
      else {
        Project project = ProjectReader.getInstance().getProject(projectId);
        if (project != null) {
          String mainLang = project.getMainLanguage();
          if (mainLang != null)
            language = mainLang;
        } 
      }
      Date yearDate = null;
      IndexableField dateField = doc.getField("date");
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
      IndexableField descriptionField = doc.getField("description");
      if (descriptionField != null)
        description = descriptionField.stringValue();
      String subject = null;
      IndexableField subjectField = doc.getField("subject");
      if (subjectField != null)
        subject = subjectField.stringValue();
      String swd = null;
      IndexableField swdField = doc.getField("swd");
      if (swdField != null)
        swd = swdField.stringValue();
      String ddc = null;
      IndexableField ddcField = doc.getField("ddc");
      if (ddcField != null)
        ddc = ddcField.stringValue();
      String rights = null;
      IndexableField rightsField = doc.getField("rights");
      if (rightsField != null)
        rights = rightsField.stringValue();
      String license = null;
      IndexableField licenseField = doc.getField("license");
      if (licenseField != null)
        license = licenseField.stringValue();
      String accessRights = null;
      IndexableField accessRightsField = doc.getField("accessRights");
      if (accessRightsField != null)
        accessRights = accessRightsField.stringValue();
      int pageCount = -1;
      IndexableField pageCountField = doc.getField("pageCount");
      if (pageCountField != null) {
        String pageCountStr = pageCountField.stringValue();
        pageCount = Integer.valueOf(pageCountStr);
      }
      String type = null;
      IndexableField typeField = doc.getField("type");
      if (typeField != null)
        type = typeField.stringValue();
      String systemType = null;
      IndexableField systemTypeField = doc.getField("systemType");
      if (systemTypeField != null)
        systemType = systemTypeField.stringValue();
      String entitiesDetailsStr = null;
      IndexableField entitiesDetailsField = doc.getField("entitiesDetails");
      if (entitiesDetailsField != null) {
        entitiesDetailsStr = entitiesDetailsField.stringValue();
      }
      String personsStr = null;
      IndexableField personsField = doc.getField("persons");
      if (personsField != null) {
        personsStr = personsField.stringValue();
      }
      String personsDetailsStr = null;
      IndexableField personsDetailsField = doc.getField("personsDetails");
      if (personsDetailsField != null) {
        personsDetailsStr = personsDetailsField.stringValue();
      }
      String placesStr = null;
      IndexableField placesField = doc.getField("places");
      if (placesField != null) {
        placesStr = placesField.stringValue();
      }
      String schemaName = null;
      IndexableField schemaNameField = doc.getField("schemaName");
      if (schemaNameField != null)
        schemaName = schemaNameField.stringValue();
      Date lastModified = null;
      IndexableField lastModifiedField = doc.getField("lastModified");
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
      mdRecord.setProjectId(projectId);
      mdRecord.setProjectRdfId(projectRdfId);
      mdRecord.setCollectionRdfId(collectionRdfId);
      mdRecord.setDatabaseRdfId(databaseRdfId);
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
      mdRecord.setSystem(systemType);
      mdRecord.setEntitiesDetails(entitiesDetailsStr);
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
    try {
      if (value == null)
        value = "";
      makeIndexReaderUpToDate();
      Terms terms = MultiFields.getTerms(documentsIndexReader, fieldName);
      TermsEnum termsEnum = null;
      if (terms != null) {
        termsEnum = terms.iterator();
        termsEnum.seekCeil(new BytesRef(value)); // jumps to the value or if not exactly found jumps to the next ceiling term 
        BytesRef termBytes = termsEnum.term();
        while (termBytes != null && counter < count) {
          termBytes = termsEnum.term();
          if (termBytes == null)
            break;
          String termContentStr = termBytes.utf8ToString();
          if (termContentStr.startsWith(value)) {
            Term termContent = new Term(fieldName, termBytes);
            int docFreq = termsEnum.docFreq();
            // long totalTermFreq = termsEnum.totalTermFreq(); // TODO
            // token.setDocFreq(docFreq);
            // token.setFreq(totalTermFreq);
            Token token = new Token(termContent);
            token.setFreq(docFreq);
            retToken.add(token);
            counter++;
          } else {
            break;
          }
          termBytes = termsEnum.next();
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
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
        Terms terms = documentsIndexReader.getTermVector(docIdInt, fieldName);
        if (terms != null) {
          boolean success = false;
          retToken = new ArrayList<Token>();
          TermsEnum termsEnum = terms.iterator();
          PostingsEnum postingsEnum = null;
          while (counter < count) {
            BytesRef termBytes = termsEnum.next();
            if (termBytes == null)
              break;
            String termStr = termBytes.utf8ToString();
            if (termStr.startsWith(value))
              success = true;
            if (success) {
              counter++;
              postingsEnum = termsEnum.postings(postingsEnum);
              postingsEnum.nextDoc();  // get the first doc, then freq could be determinded
              int freq = postingsEnum.freq();
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

  public ArrayList<Token> getFrequentToken(String docId, String fieldName, int count) throws ApplicationException {
    return null; // TODO
    /*
    ArrayList<Token> retToken = null;
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
          if (freqs.length < count)
            count = freqs.length;
          if (terms != null) {
            retToken = new ArrayList<Token>();
            for (int i=1; i<=count; i++) {
              int maxFreqPos = -1;
              int maxFreq = -1;
              for (int j=0; j<terms.length; j++) {
                int termFreq = freqs[j];
                if (termFreq > maxFreq) {
                  maxFreq = termFreq;
                  maxFreqPos = j;
                }
              }
              String maxFreqTermStr = terms[maxFreqPos];
              Term t = new Term(fieldName, maxFreqTermStr);
              Token tok = new Token(t);
              tok.setFreq(maxFreq);
              retToken.add(tok);
              terms = ArrayUtils.remove(terms, maxFreqPos);
              freqs = ArrayUtils.remove(freqs, maxFreqPos);
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
  */
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
        BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
        for (int i=0; i < QUERY_EXPANSION_FIELDS_ALL.length; i++) {
          String expansionField = QUERY_EXPANSION_FIELDS_ALL[i];  // e.g. "author"
          String expansionFieldQueryStr = expansionField + ":(" + queryStr + ")";
          Query q = queryParser.parse(expansionFieldQueryStr);
          boolQueryBuilder.add(q, BooleanClause.Occur.SHOULD);
        }
        retQuery = boolQueryBuilder.build();
      } else if (fieldExpansion.equals("allMorph")) {
        BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
        for (int i=0; i < QUERY_EXPANSION_FIELDS_ALL_MORPH.length; i++) {
          String expansionField = QUERY_EXPANSION_FIELDS_ALL_MORPH[i];  // e.g. "author"
          String expansionFieldQueryStr = expansionField + ":(" + queryStr + ")";
          Query q = queryParser.parse(expansionFieldQueryStr);
          boolQueryBuilder.add(q, BooleanClause.Occur.SHOULD);
        }
        retQuery = boolQueryBuilder.build();
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
    try {
      Query fulltextQuery = removeNonFulltextFields(query);
      if (fulltextQuery != null)
        retQuery = buildMorphQuery(fulltextQuery, language, true, translate, languageHandler);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return retQuery;
  }
  
  private Query buildMorphQuery(Query query, String language, LanguageHandler languageHandler) throws ApplicationException {
    return buildMorphQuery(query, language, false, false, languageHandler);
  }

  private Query removeNonFulltextFields(Query query) throws ApplicationException {
    Query retQuery = null;
    if (query instanceof TermQuery || query instanceof PhraseQuery || query instanceof WildcardQuery || query instanceof FuzzyQuery || query instanceof PrefixQuery) {
      if (isFulltextQuery(query))
        retQuery = query;
      else 
        retQuery = null;
    } else if (query instanceof BooleanQuery) {
      BooleanQuery booleanQuery = (BooleanQuery) query;
      retQuery = removeNonFulltextFields(booleanQuery);
    } else {
      retQuery = query; // TODO: all other cases
    }
    return retQuery;
  }

  private boolean isFulltextQuery(Query query) throws ApplicationException {
    boolean isFulltextQuery = false;
    if (query instanceof TermQuery) {
      TermQuery termQuery = (TermQuery) query;
      Term term = termQuery.getTerm();
      isFulltextQuery = isFulltextTerm(term);
    } else if (query instanceof WildcardQuery) {
      WildcardQuery wildcardTermQuery = (WildcardQuery) query;
      Term term = wildcardTermQuery.getTerm();
      isFulltextQuery = isFulltextTerm(term);
    } else if (query instanceof FuzzyQuery) {
      FuzzyQuery fuzzyTermQuery = (FuzzyQuery) query;
      Term term = fuzzyTermQuery.getTerm();
      isFulltextQuery = isFulltextTerm(term);
    } else if (query instanceof PrefixQuery) {
      PrefixQuery prefixQuery = (PrefixQuery) query;
      Term term = prefixQuery.getPrefix();
      isFulltextQuery = isFulltextTerm(term);
    } else if (query instanceof PhraseQuery) {
      PhraseQuery phraseQuery = (PhraseQuery) query;
      Term[] terms = phraseQuery.getTerms();
      Term firstTerm = null;
      if (terms != null && terms.length > 0)
        firstTerm = terms[0];
      if (firstTerm != null) {
        isFulltextQuery = isFulltextTerm(firstTerm);
      } 
    }
    return isFulltextQuery;
  }

  private boolean isFulltextTerm(Term term) {
    boolean isFulltext = false;
    String fieldName = term.field();
    if (fieldName != null && (fieldName.equals("tokenOrig") || fieldName.equals("tokenMorph")))
      isFulltext = true;
    return isFulltext;
  }
  
  private BooleanQuery removeNonFulltextFields(BooleanQuery booleanQuery) throws ApplicationException {
    if (booleanQuery == null)
      return null;
    BooleanQuery retQuery = null;
    List<BooleanClause> clauses = booleanQuery.clauses();
    ArrayList<BooleanClause> fulltextClauses = new ArrayList<BooleanClause>();
    for (int i=0; i<clauses.size(); i++) {
      BooleanClause clause = clauses.get(i);
      Query clauseQuery = clause.getQuery();
      if (clauseQuery instanceof TermQuery || clauseQuery instanceof PhraseQuery || clauseQuery instanceof WildcardQuery || clauseQuery instanceof FuzzyQuery || clauseQuery instanceof PrefixQuery) {
        if (isFulltextQuery(clauseQuery))
          fulltextClauses.add(clause);
      } else if (clauseQuery instanceof BooleanQuery) {
        BooleanQuery bQuery = (BooleanQuery) clauseQuery;
        retQuery = removeNonFulltextFields(bQuery);
      } else {
        fulltextClauses.add(clause); // all other cases: PrefixQuery, FuzzyQuery, TermRangeQuery, ...
      }
    }
    if (! fulltextClauses.isEmpty()) {
      BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
      for (int i=0; i<fulltextClauses.size(); i++) {
        BooleanClause clause = fulltextClauses.get(i);
          boolQueryBuilder.add(clause);
      }
      retQuery = boolQueryBuilder.build();
    }
    return retQuery;
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
    BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
    for (int i = 0; i < queryTerms.size(); i++) {
      TermQuery termQuery = queryTerms.get(i);
      boolQueryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
    }
    BooleanQuery retBooleanQuery = boolQueryBuilder.build();
    return retBooleanQuery;
  }
  
  private Query buildMorphQuery(BooleanQuery query, String language, boolean withAllForms, boolean translate, LanguageHandler languageHandler) throws ApplicationException {
    BooleanQuery.Builder morphBoolQueryBuilder = new BooleanQuery.Builder();
    List<BooleanClause> booleanClauses = query.clauses();
    for (int i = 0; i < booleanClauses.size(); i++) {
      BooleanClause boolClause = booleanClauses.get(i);
      Query q = boolClause.getQuery();
      Query morphQuery = buildMorphQuery(q, language, withAllForms, translate, languageHandler);
      BooleanClause.Occur occur = boolClause.getOccur();
      morphBoolQueryBuilder.add(morphQuery, occur);
    }
    BooleanQuery morphBooleanQuery = morphBoolQueryBuilder.build();
    return morphBooleanQuery;
  }

  public ArrayList<String> fetchTerms(String queryStr) throws ApplicationException {
    ArrayList<String> terms = null;
    String defaultQueryFieldName = "tokenOrig";
    try {
      Query query = new QueryParser(defaultQueryFieldName, nodesPerFieldAnalyzer).parse(queryStr);
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
    List<BooleanClause> booleanClauses = query.clauses();
    for (int i = 0; i < booleanClauses.size(); i++) {
      BooleanClause boolClause = booleanClauses.get(i);
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
      Query query = new QueryParser(defaultQueryFieldName, nodesPerFieldAnalyzer).parse(queryStr);
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
    List<BooleanClause> booleanClauses = query.clauses();
    for (int i = 0; i < booleanClauses.size(); i++) {
      BooleanClause boolClause = booleanClauses.get(i);
      Query q = boolClause.getQuery();
      ArrayList<String> qTerms = fetchTerms(q, language);
      BooleanClause.Occur occur = boolClause.getOccur();
      if (occur == BooleanClause.Occur.SHOULD || occur == BooleanClause.Occur.MUST)
        terms.addAll(qTerms);
    }
    return terms;
  }

  public Document getDocument(String docId) throws ApplicationException {
    Document doc = null;
    IndexSearcher searcher = null;
    try {
      makeDocumentsSearcherManagerUpToDate();
      searcher = documentsSearcherManager.acquire();
      String fieldNameDocId = "docId";
      String docIdQuery = QueryParser.escape(docId);
      Query queryDocId = new QueryParser(fieldNameDocId, documentsPerFieldAnalyzer).parse(docIdQuery);
      TopDocs topDocs = searcher.search(queryDocId, 100000);
      topDocs.setMaxScore(1);
      if (topDocs != null && topDocs.scoreDocs != null && topDocs.scoreDocs.length > 0) {
        int docID = topDocs.scoreDocs[0].doc;
        HashSet<String> docFields = getDocFields();
        doc = searcher.doc(docID, docFields);
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
      Query queryId = new QueryParser(fieldNameId, documentsPerFieldAnalyzer).parse(idQuery);
      queryId = NumericRangeQuery.newIntRange(fieldNameId, 0, 10000000, false, false);
      Sort sortByIdReverse = new Sort(new SortField("id", Type.INT, true));  // reverse order
      TopDocs topDocs = searcher.search(queryId, 1, sortByIdReverse);
      topDocs.setMaxScore(1);
      if (topDocs != null && topDocs.scoreDocs != null && topDocs.scoreDocs.length > 0) {
        int docID = topDocs.scoreDocs[0].doc;
        HashSet<String> docFields = getDocFields();
        Document doc = searcher.doc(docID, docFields);
        if (doc != null) {
          IndexableField maxIdField = doc.getField("id");
          maxIdStr = maxIdField.stringValue();
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
      Query queryDocId = new QueryParser(fieldNameDocId, documentsPerFieldAnalyzer).parse(docId);
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
    try {
      Map<String, Analyzer> documentsFieldAnalyzers = new HashMap<String, Analyzer>();
      documentsFieldAnalyzers.put("id", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("docId", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("identifier", new KeywordAnalyzer()); 
      documentsFieldAnalyzers.put("uri", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("webUri", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("projectId", new StandardAnalyzer());
      documentsFieldAnalyzers.put("projectRdfId", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("collectionRdfId", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("author", new StandardAnalyzer());
      documentsFieldAnalyzers.put("title", new StandardAnalyzer());
      documentsFieldAnalyzers.put("language", new StandardAnalyzer());
      documentsFieldAnalyzers.put("publisher", new StandardAnalyzer());
      documentsFieldAnalyzers.put("date", new StandardAnalyzer());
      documentsFieldAnalyzers.put("description", new StandardAnalyzer());
      documentsFieldAnalyzers.put("subject", new StandardAnalyzer());
      documentsFieldAnalyzers.put("subjectControlled", new StandardAnalyzer());
      documentsFieldAnalyzers.put("swd", new StandardAnalyzer());
      documentsFieldAnalyzers.put("ddc", new StandardAnalyzer());
      documentsFieldAnalyzers.put("entities", new StandardAnalyzer());
      documentsFieldAnalyzers.put("subject", new StandardAnalyzer());
      documentsFieldAnalyzers.put("rights", new StandardAnalyzer());
      documentsFieldAnalyzers.put("license", new StandardAnalyzer());
      documentsFieldAnalyzers.put("accessRights", new StandardAnalyzer());
      documentsFieldAnalyzers.put("type", new KeywordAnalyzer()); // e.g. mime type "text/xml"
      documentsFieldAnalyzers.put("systemType", new KeywordAnalyzer()); // e.g. "dbRecord"
      documentsFieldAnalyzers.put("pageCount", new KeywordAnalyzer()); 
      documentsFieldAnalyzers.put("schemaName", new StandardAnalyzer());
      documentsFieldAnalyzers.put("lastModified", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("tokenOrig", new StandardAnalyzer());
      documentsFieldAnalyzers.put("tokenReg", new StandardAnalyzer());
      documentsFieldAnalyzers.put("tokenNorm", new StandardAnalyzer());
      documentsFieldAnalyzers.put("tokenMorph", new StandardAnalyzer());
      documentsFieldAnalyzers.put("xmlContent", new StandardAnalyzer());
      documentsFieldAnalyzers.put("content", new StandardAnalyzer());
      documentsPerFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), documentsFieldAnalyzers);
      IndexWriterConfig conf = new IndexWriterConfig(documentsPerFieldAnalyzer);
      conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
      conf.setRAMBufferSizeMB(300);  // 300 MB because some documents are big; 16 MB is default 
      conf.setMaxBufferedDeleteTerms(1);  // so that indexReader.terms() delivers immediately and without optimize() the correct number of terms
      Path luceneDocsDirectory = Paths.get(luceneDocsDirectoryStr);
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
    try {
      Map<String, Analyzer> nodesFieldAnalyzers = new HashMap<String, Analyzer>();
      nodesFieldAnalyzers.put("docId", new KeywordAnalyzer());
      nodesFieldAnalyzers.put("language", new StandardAnalyzer()); // language (through xml:id): e.g. "lat"
      nodesFieldAnalyzers.put("pageNumber", new KeywordAnalyzer()); // page number (through element pb): e.g. "13"
      nodesFieldAnalyzers.put("lineNumber", new KeywordAnalyzer()); // line number on the page (through element lb): e.g. "17"
      nodesFieldAnalyzers.put("elementName", new KeywordAnalyzer()); // element name: e.g. "tei:s"
      nodesFieldAnalyzers.put("elementDocPosition", new KeywordAnalyzer()); // absolute position of element in document: e.g. "4711"
      nodesFieldAnalyzers.put("elementPosition", new KeywordAnalyzer()); // position in parent node (in relation to other nodes of the same name): e.g. "5"
      nodesFieldAnalyzers.put("elementAbsolutePosition", new KeywordAnalyzer()); // absolute position in document (in relation to other nodes of the same name): e.g. "213"
      nodesFieldAnalyzers.put("elementPagePosition", new KeywordAnalyzer()); // position in relation to other nodes of the same name: e.g. "213"
      nodesFieldAnalyzers.put("xmlId", new KeywordAnalyzer()); // xml id: e.g. "4711bla"
      nodesFieldAnalyzers.put("xpath", new KeywordAnalyzer()); // xpath: e.g. "/echo[1]/text[1]/p[1]/s[5]"
      nodesFieldAnalyzers.put("tokenOrig", new StandardAnalyzer());
      nodesFieldAnalyzers.put("tokenReg", new StandardAnalyzer());
      nodesFieldAnalyzers.put("tokenNorm", new StandardAnalyzer());
      nodesFieldAnalyzers.put("tokenMorph", new StandardAnalyzer());
      nodesFieldAnalyzers.put("xmlContent", new StandardAnalyzer());
      nodesPerFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), nodesFieldAnalyzers);
      IndexWriterConfig conf = new IndexWriterConfig(nodesPerFieldAnalyzer);
      conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
      conf.setRAMBufferSizeMB(300);  // 300 MB because some documents are big; 16 MB is default 
      Path luceneNodesDirectory = Paths.get(luceneNodesDirectoryStr);
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
      Type sortFieldType = getDocSortFieldType(sortFieldName);
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

  private Type getDocSortFieldType(String fieldName) {
    Type type = Type.STRING;
    if (fieldName.equals("lastModified"))
      type = Type.LONG;
    return type;
  }
  
  private Type getNodeSortFieldType(String fieldName) {
    Type type = Type.STRING;
    if (fieldName.equals("pageNumber") || fieldName.equals("lineNumber") || fieldName.equals("elementDocPosition")) 
      type = Type.INT;
    return type;
  }

  private HashSet<String> getDocFields() {
    HashSet<String> fields = new HashSet<String>();
    fields.add("id");
    fields.add("docId");
    fields.add("identifier");
    fields.add("uri");
    fields.add("webUri");
    fields.add("projectId");
    fields.add("projectRdfId");
    fields.add("collectionRdfId");
    fields.add("databaseRdfId");
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
    fields.add("systemType");
    fields.add("pageCount");
    fields.add("schemaName");
    fields.add("lastModified");
    fields.add("entities");
    fields.add("entitiesDetails");
    fields.add("persons");
    fields.add("personsDetails");
    fields.add("places");
    fields.add("content");
    return fields;
  }
  
  private HashSet<String> getDocFieldProjectId() {
    HashSet<String> fields = new HashSet<String>();
    fields.add("projectId");
    return fields;
  }
  
  private HashSet<String> getNodeFields() {
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
    return fields;
  }
  
  private SearcherManager getNewSearcherManager(IndexWriter indexWriter) throws ApplicationException {
    SearcherManager searcherManager = null;
    try {
      searcherManager = new SearcherManager(indexWriter, true, null);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return searcherManager;
  }

  private DirectoryReader getDocumentsReader() throws ApplicationException {
    DirectoryReader reader = null;
    String luceneDocsDirectoryStr = Constants.getInstance().getLuceneDocumentsDir();
    Path luceneDocsDirectory = Paths.get(luceneDocsDirectoryStr);
    try {
      FSDirectory fsDirectory = FSDirectory.open(luceneDocsDirectory);
      reader = DirectoryReader.open(fsDirectory);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return reader;
  }

  private TaxonomyWriter getTaxonomyWriter() throws ApplicationException {
    TaxonomyWriter taxonomyWriter = null;
    String taxonomyDirStr = Constants.getInstance().getLuceneTaxonomyDir();
    Path taxonomyDirF = Paths.get(taxonomyDirStr);
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
    Path taxonomyDirF = Paths.get(taxonomyDirStr);
    try {
      Directory taxonomyDir = FSDirectory.open(taxonomyDirF);
      taxonomyReader = new DirectoryTaxonomyReader(taxonomyDir);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return taxonomyReader;
  }

  private void makeIndexReaderUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = documentsIndexReader.isCurrent();
      if (! isCurrent) {
        documentsIndexReader = DirectoryReader.openIfChanged(documentsIndexReader);
      }
      TaxonomyReader.openIfChanged(taxonomyReader); 
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  private void makeDocumentsSearcherManagerUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = documentsSearcherManager.isSearcherCurrent();
      if (! isCurrent) {
        documentsSearcherManager.maybeRefresh();
      }
      TaxonomyReader.openIfChanged(taxonomyReader); 
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  private void makeNodesSearcherManagerUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = nodesSearcherManager.isSearcherCurrent();
      if (!isCurrent) {
        nodesSearcherManager.maybeRefresh();
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