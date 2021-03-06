package org.bbaw.wsp.cms.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.search.grouping.SearchGroup;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.search.grouping.term.TermFirstPassGroupingCollector;
import org.apache.lucene.search.grouping.term.TermSecondPassGroupingCollector;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.suggest.tst.TSTLookup;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.bbaw.wsp.cms.collections.OutputType;
import org.bbaw.wsp.cms.collections.Project;
import org.bbaw.wsp.cms.collections.ProjectCollection;
import org.bbaw.wsp.cms.collections.ProjectReader;
import org.bbaw.wsp.cms.document.DBpediaResource;
import org.bbaw.wsp.cms.document.Facets;
import org.bbaw.wsp.cms.document.GroupDocuments;
import org.bbaw.wsp.cms.document.GroupHits;
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
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class IndexHandler {
  private static IndexHandler instance;
  private static Logger LOGGER = Logger.getLogger(IndexHandler.class);
  private static final String[] QUERY_EXPANSION_FIELDS_ALL = {"author", "title", "description", "subject", "subjectControlled", "swd", "ddc", "entities", "persons", "places", "tokenOrig"};
  private static final String[] QUERY_EXPANSION_FIELDS_ALL_MORPH = {"author", "title", "description", "subject", "subjectControlled", "swd", "ddc", "entities", "persons", "places", "tokenMorph"};
  private static final String[] QUERY_EXPANSION_FIELDS_PROJECTS_ALL = {"label", "abstract", "subjects", "staffNames", "collectionLabels", "collectionAbstracts"};
  private static final String[] QUERY_EXPANSION_FIELDS_PROJECTS_ALL_MORPH = {"label", "abstract", "subjects", "staffNames", "collectionLabels", "collectionAbstracts"};
  private static final int HIGHLIGHT_BIG_DOCUMENT_CHAR_SIZE = 500000;
  private HashMap<String, Integer> facetFieldCounts = new HashMap<String, Integer>();
  private IndexWriter documentsIndexWriter;
  private IndexWriter projectsIndexWriter;
  private SearcherManager documentsSearcherManager;
  private SearcherManager projectsSearcherManager;
  private DirectoryReader documentsIndexReader;
  private DirectoryReader projectsIndexReader;
  private PerFieldAnalyzerWrapper documentsPerFieldAnalyzer;
  private PerFieldAnalyzerWrapper projectsPerFieldAnalyzer;
  private HashMap<String, Token> tokens; // all tokens of all query expansion fields
  private TSTLookup suggester;
  private TaxonomyWriter taxonomyWriter;
  private TaxonomyReader taxonomyReader;
  private TaxonomyWriter projectsTaxonomyWriter;
  private TaxonomyReader projectsTaxonomyReader;
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
    projectsIndexWriter = getProjectsWriter();
    documentsSearcherManager = getNewSearcherManager(documentsIndexWriter);
    projectsSearcherManager = getNewSearcherManager(projectsIndexWriter);
    documentsIndexReader = getDocumentsReader();
    projectsIndexReader = getProjectsReader();
    taxonomyWriter = getTaxonomyWriter();
    taxonomyReader = getTaxonomyReader();
    projectsTaxonomyWriter = getProjectsTaxonomyWriter();
    projectsTaxonomyReader = getProjectsTaxonomyReader();
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
    tokens = new HashMap<String, Token>();
    for (int i=0; i<QUERY_EXPANSION_FIELDS_ALL.length; i++) {
      String fieldName = QUERY_EXPANSION_FIELDS_ALL[i];
      addToken(fieldName, "", 10000000); // get all token in all query expansion fields: needs ca. 4 sec. for 3 Mio tokens
    }
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
      projectsTaxonomyWriter.commit();
      documentsIndexWriter.commit();
      projectsIndexWriter.commit();
    } catch (Exception e) {
      try {
        taxonomyWriter.rollback();
        projectsTaxonomyWriter.rollback();
        documentsIndexWriter.rollback();
        projectsIndexWriter.rollback();
      } catch (Exception ex) {
        // nothing
      }
      throw new ApplicationException(e);
    }
  }
  
  public void indexDocument(MetadataRecord mdRecord) throws ApplicationException {
    commitCounter++;
    // first delete document in documentsIndex
    deleteDocumentLocal(mdRecord);
    indexDocumentLocal(mdRecord);
    // performance gain (each commit needs at least ca. 60 ms extra): a commit is only done if commitInterval (e.g. 500) is reached 
    if (commitCounter >= commitInterval) {
      commit();
      commitCounter = 0;
    }
  }

  public void indexProjects(ArrayList<Project> projects) throws ApplicationException {
    deleteAllProjectsLocal();
    commit();
    for (int i=0; i<projects.size(); i++) {
      Project project = projects.get(i);
      indexProjectLocal(project);
    }
    commit();
  }

  private void indexProjectLocal(Project project) throws ApplicationException {
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
      String id = project.getId();
      if (id != null) { 
        Field field = new Field("id", id, ftStoredAnalyzed);
        doc.add(field);
        FacetField facetFieldId = new FacetField("id", id);
        doc.add(facetFieldId);
      }
      String rdfId = project.getRdfId();
      if (rdfId != null) {
        Field field = new Field("rdfId", rdfId, ftStoredAnalyzed); 
        doc.add(field);
        FacetField facetFieldRdfId = new FacetField("rdfId", rdfId);
        doc.add(facetFieldRdfId);
      }
      String organizationRdfId = project.getOrganizationRdfId();
      if (organizationRdfId != null) {
        Field field = new Field("organizationRdfId", organizationRdfId, ftStoredAnalyzed); 
        doc.add(field);
        Project organization = ProjectReader.getInstance().getProjectByRdfId(organizationRdfId);
        if (organization != null) {
          String organizationLabel = organization.getTitle();
          Field orgField = new Field("organizationLabel", organizationLabel, ftStoredAnalyzed); 
          doc.add(orgField);
        }
      }
      String label = project.getTitle();
      if (label != null) {
        Field field = new Field("label", label, ftStoredAnalyzed); 
        doc.add(field);
      }
      String absstract = project.getAbstract();
      if (absstract != null) {
        Field field = new Field("abstract", absstract, ftStoredAnalyzed); 
        doc.add(field);
      }
      String projectType = project.getProjectType();
      if (projectType != null) {
        Field projectTypeField = new Field("type", projectType, ftStoredAnalyzed);
        doc.add(projectTypeField);
        FacetField facetFieldProjectType = new FacetField("type", projectType);
        doc.add(facetFieldProjectType);
      }
      String duration = project.getDuration();
      if (duration != null) {
        Field durationField = new Field("duration", duration, ftStoredAnalyzed);
        doc.add(durationField);
      }
      String status = project.getStatus();
      if (status != null) {
        Field statusField = new Field("status", status, ftStoredAnalyzed);
        doc.add(statusField);
        FacetField facetFieldStatus = new FacetField("status", status);
        doc.add(facetFieldStatus);
      }
      String mainLanguage = project.getMainLanguageLabel();
      if (mainLanguage != null) {
        Field mainLanguageField = new Field("mainLanguage", mainLanguage, ftStoredAnalyzed);
        doc.add(mainLanguageField);
        FacetField facetFieldMainLanguage = new FacetField("mainLanguage", mainLanguage);
        doc.add(facetFieldMainLanguage);
      }
      ArrayList<String> subjects = project.getSubjectsStr();
      if (subjects != null) {
        String subjectsStr = org.apache.commons.lang3.StringUtils.join(subjects, "###");
        Field field = new Field("subjects", subjectsStr, ftStoredAnalyzed); 
        doc.add(field);
        for (int i=0; i<subjects.size(); i++) {
          String s = subjects.get(i).trim();
          if (! s.isEmpty()) {
            FacetField facetField = new FacetField("subject", s);
            doc.add(facetField);
          }
        }
      }
      String place = project.getPlace();
      if (place != null) {
        Field placeField = new Field("place", place, ftStoredAnalyzed);
        doc.add(placeField);
      }
      String periodOfTime = project.getPeriodOfTime();
      if (periodOfTime != null) {
        Field periodOfTimeField = new Field("periodOfTime", periodOfTime, ftStoredAnalyzed);
        doc.add(periodOfTimeField);
        FacetField facetFieldPeriodOfTime = new FacetField("periodOfTime", periodOfTime);
        doc.add(facetFieldPeriodOfTime);
      }
      ArrayList<String> gndRelatedPersons = project.getGndRelatedPersonsStr();
      if (gndRelatedPersons != null) {
        String gndRelatedPersonsStr = org.apache.commons.lang3.StringUtils.join(gndRelatedPersons, "###");
        Field gndRelatedPersonsField = new Field("persons", gndRelatedPersonsStr, ftStoredAnalyzed);
        doc.add(gndRelatedPersonsField);
      }
      ArrayList<String> staff = project.getStaffStr();
      if (staff != null) {
        String staffStr = org.apache.commons.lang3.StringUtils.join(staff, "###");
        Field field = new Field("staff", staffStr, ftStoredAnalyzed); 
        doc.add(field);
      }
      ArrayList<ProjectCollection> collections = project.getAllCollections();
      if (collections != null) {
        String collectionLabels = "";
        String collectionAbstracts = "";
        for (int i=0; i<collections.size(); i++) {
          ProjectCollection collection = collections.get(i);
          String collectionLabel = collection.getTitle();
          if (collectionLabel != null && ! collectionLabel.isEmpty())
            collectionLabels = collectionLabels + "###" + collectionLabel;
          String collectionAbstract = collection.getAbstract();
          if (collectionAbstract != null && ! collectionAbstract.isEmpty())
            collectionAbstracts = collectionAbstracts + "###" + collectionAbstract;
        }
        Field collectionLabelsField = new Field("collectionLabels", collectionLabels, ftStoredAnalyzed); 
        doc.add(collectionLabelsField);
        Field collectionAbstractsField = new Field("collectionAbstracts", collectionAbstracts, ftStoredAnalyzed); 
        doc.add(collectionAbstractsField);
      }
      // facet creation
      FacetsConfig facetsConfig = new FacetsConfig();
      facetsConfig.setMultiValued("subject", true);
      Document docWithFacets = facetsConfig.build(projectsTaxonomyWriter, doc);
      projectsIndexWriter.addDocument(docWithFacets);
    } catch (Exception e) {
      LOGGER.error("indexProjectLocal: " + project.getRdfId());
      e.printStackTrace();
      throw new ApplicationException(e);
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
      String projectId = mdRecord.getProjectId();
      if (projectId != null) {
        Field projectField = new Field("projectId", projectId, ftStoredAnalyzed);
        if (projectId.equals("jdg"))
          projectField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        doc.add(projectField);
        Field projectFieldSorted = new SortedDocValuesField("projectIdSorted", new BytesRef(projectId));
        doc.add(projectFieldSorted);
        FacetField facetField = new FacetField("projectId", projectId);
        doc.add(facetField);
      }
      String projectRdfId = mdRecord.getProjectRdfId();
      ProjectReader projectReader = ProjectReader.getInstance();
      if (projectRdfId != null) {
        Field projectRdfIdField = new Field("projectRdfId", projectRdfId, ftStoredAnalyzed);
        if (projectId.equals("jdg"))
          projectRdfIdField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        doc.add(projectRdfIdField);
        Field projectRdfIdFieldSorted = new SortedDocValuesField("projectRdfIdSorted", new BytesRef(projectRdfId));
        doc.add(projectRdfIdFieldSorted);
        FacetField facetField = new FacetField("projectRdfId", projectRdfId);
        doc.add(facetField);
        Project project = projectReader.getProjectByRdfId(projectRdfId);
        String projectLabel = project.getTitle();
        if (projectLabel != null) {
          Field projectLabelField = new Field("projectLabel", projectLabel, ftStoredAnalyzed);
          doc.add(projectLabelField);
          Field projectLabelFieldSorted = new SortedDocValuesField("projectLabelSorted", new BytesRef(projectLabel));
          doc.add(projectLabelFieldSorted);
        }
        String projectAbstract = project.getAbstract();
        if (projectAbstract != null) {
          Field projectAbstractField = new Field("projectAbstract", projectAbstract, ftStoredAnalyzed);
          doc.add(projectAbstractField);
        }
        String projectType = project.getProjectType();
        if (projectType != null) {
          Field projectTypeField = new Field("projectType", projectType, ftStoredAnalyzed);
          doc.add(projectTypeField);
        }
        String projectDuration = project.getDuration();
        if (projectDuration != null) {
          Field projectDurationField = new Field("projectDuration", projectDuration, ftStoredAnalyzed);
          doc.add(projectDurationField);
        }
        String projectStatus = project.getStatus();
        if (projectStatus != null) {
          Field projectStatusField = new Field("projectStatus", projectStatus, ftStoredAnalyzed);
          doc.add(projectStatusField);
        }
        String projectMainLanguage = project.getMainLanguageLabel();
        if (projectMainLanguage != null) {
          Field projectMainLanguageField = new Field("projectMainLanguage", projectMainLanguage, ftStoredAnalyzed);
          doc.add(projectMainLanguageField);
        }
        ArrayList<String> projectSubjects = project.getSubjectsStr();
        if (projectSubjects != null) {
          String projectSubjectsStr = org.apache.commons.lang3.StringUtils.join(projectSubjects, "###");
          Field projectSubjectsField = new Field("projectSubjects", projectSubjectsStr, ftStoredAnalyzed);
          doc.add(projectSubjectsField);
        }
        String projectPlace = project.getPlace();
        if (projectPlace != null) {
          Field projectPlaceField = new Field("projectPlace", projectPlace, ftStoredAnalyzed);
          doc.add(projectPlaceField);
        }
        String projectPeriodOfTime = project.getPeriodOfTime();
        if (projectPeriodOfTime != null) {
          Field projectPeriodOfTimeField = new Field("projectPeriodOfTime", projectPeriodOfTime, ftStoredAnalyzed);
          doc.add(projectPeriodOfTimeField);
        }
        ArrayList<String> projectGndRelatedPersons = project.getGndRelatedPersonsStr();
        if (projectGndRelatedPersons != null) {
          String projectGndRelatedPersonsStr = org.apache.commons.lang3.StringUtils.join(projectGndRelatedPersons, "###");
          Field projectGndRelatedPersonsField = new Field("projectPersons", projectGndRelatedPersonsStr, ftStoredAnalyzed);
          doc.add(projectGndRelatedPersonsField);
        }
        ArrayList<String> projectStaff = project.getStaffStr();
        if (projectStaff != null) {
          String projectStaffStr = org.apache.commons.lang3.StringUtils.join(projectStaff, "###");
          Field projectStaffField = new Field("projectStaff", projectStaffStr, ftStoredAnalyzed);
          doc.add(projectStaffField);
        }
      }
      String organizationRdfId = mdRecord.getOrganizationRdfId();
      if (organizationRdfId != null) {
        Field organizationRdfIdField = new Field("organizationRdfId", organizationRdfId, ftStoredAnalyzed);
        if (projectId.equals("jdg"))
          organizationRdfIdField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        doc.add(organizationRdfIdField);
        FacetField facetField = new FacetField("organizationRdfId", organizationRdfId);
        doc.add(facetField);
      }
      String collectionRdfId = mdRecord.getCollectionRdfId();
      if (collectionRdfId != null) {
        Field collectionRdfIdField = new Field("collectionRdfId", collectionRdfId, ftStoredAnalyzed);
        if (projectId.equals("jdg"))
          collectionRdfIdField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        doc.add(collectionRdfIdField);
        Field collectionRdfIdFieldSorted = new SortedDocValuesField("collectionRdfIdSorted", new BytesRef(collectionRdfId));
        doc.add(collectionRdfIdFieldSorted);
        FacetField facetField = new FacetField("collectionRdfId", collectionRdfId);
        doc.add(facetField);
        ProjectCollection collection = projectReader.getCollection(collectionRdfId);
        String collectionLabel = collection.getTitle();
        if (collectionLabel != null) {
          Field collectionLabelField = new Field("collectionLabel", collectionLabel, ftStoredAnalyzed);
          doc.add(collectionLabelField);
          Field collectionLabelFieldSorted = new SortedDocValuesField("collectionLabelSorted", new BytesRef(collectionLabel));
          doc.add(collectionLabelFieldSorted);
        }
        String collectionAbstract = collection.getAbstract();
        if (collectionAbstract != null) {
          Field collectionAbstractField = new Field("collectionAbstract", collectionAbstract, ftStoredAnalyzed);
          doc.add(collectionAbstractField);
        }
        OutputType collectionType = collection.getType();
        if (collectionType != null) {
          String collectionTypeLabel = collectionType.getLabel();
          Field collectionTypeField = new Field("collectionType", collectionTypeLabel, ftStoredAnalyzed);
          doc.add(collectionTypeField);
          FacetField facetFieldCollectionType = new FacetField("collectionType", collectionTypeLabel);
          doc.add(facetFieldCollectionType);
        }
        ArrayList<String> collectionLanguageLabels = collection.getLanguageLabels();
        if (collectionLanguageLabels != null) {
          String collectionLanguagesStr = "";
          for (int i=0; i<collectionLanguageLabels.size(); i++) {
            String languageLabel = collectionLanguageLabels.get(i);
            collectionLanguagesStr = collectionLanguagesStr + languageLabel + "###";
          }
          if (collectionLanguagesStr.endsWith("###"))
            collectionLanguagesStr.substring(0, collectionLanguagesStr.length() - 3); // remove last "###"
          Field collectionLanguagesField = new Field("collectionLanguages", collectionLanguagesStr, ftStoredAnalyzed);
          doc.add(collectionLanguagesField);
        }
        ArrayList<String> collectionSubjects = collection.getSubjectsStr();
        if (collectionSubjects != null) {
          String collectionSubjectsStr = org.apache.commons.lang3.StringUtils.join(collectionSubjects, "###");
          Field collectionSubjectsField = new Field("collectionSubjects", collectionSubjectsStr, ftStoredAnalyzed);
          doc.add(collectionSubjectsField);
          for (int i=0; i<collectionSubjects.size(); i++) {
            String s = collectionSubjects.get(i).trim();
            if (! s.isEmpty()) {
              FacetField facetFieldCollectionSubject = new FacetField("collectionSubject", s);
              doc.add(facetFieldCollectionSubject);
            }
          }
        }
        String collectionPlace = collection.getPlace();
        if (collectionPlace != null) {
          Field collectionPlacePlaceField = new Field("collectionPlace", collectionPlace, ftStoredAnalyzed);
          doc.add(collectionPlacePlaceField);
        }
        String collectionPeriodOfTime = collection.getPeriodOfTime();
        if (collectionPeriodOfTime == null) {
          Project project = collection.getProject();
          if (project != null)
            collectionPeriodOfTime = project.getPeriodOfTime();
        }
        if (collectionPeriodOfTime != null) {
          Field collectionPeriodOfTimeField = new Field("collectionPeriodOfTime", collectionPeriodOfTime, ftStoredAnalyzed);
          doc.add(collectionPeriodOfTimeField);
          FacetField facetFieldCollectionPeriodOfTime = new FacetField("collectionPeriodOfTime", collectionPeriodOfTime);
          doc.add(facetFieldCollectionPeriodOfTime);
        }
        ArrayList<String> collectionGndRelatedPersons = collection.getGndRelatedPersonsStr();
        if (collectionGndRelatedPersons != null) {
          String collectionGndRelatedPersonsStr = org.apache.commons.lang3.StringUtils.join(collectionGndRelatedPersons, "###");
          Field collectiontGndRelatedPersonsField = new Field("collectionPersons", collectionGndRelatedPersonsStr, ftStoredAnalyzed);
          doc.add(collectiontGndRelatedPersonsField);
          for (int i=0; i<collectionGndRelatedPersons.size(); i++) {
            String p = collectionGndRelatedPersons.get(i).trim();
            if (! p.isEmpty()) {
              FacetField facetFieldCollectionPerson = new FacetField("collectionPerson", p);
              doc.add(facetFieldCollectionPerson);
            }
          }
        }
        ArrayList<String> collectionStaff = collection.getStaffStr();
        if (collectionStaff != null) {
          String collectionStaffStr = org.apache.commons.lang3.StringUtils.join(collectionStaff, "###");
          Field collectionStaffField = new Field("collectionStaff", collectionStaffStr, ftStoredAnalyzed);
          doc.add(collectionStaffField);
        }
      }
      String dbRdfId = mdRecord.getDatabaseRdfId();
      if (dbRdfId != null) {
        Field databaseRdfIdField = new Field("databaseRdfId", dbRdfId, ftStoredAnalyzed);
        if (projectId.equals("jdg"))
          databaseRdfIdField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        doc.add(databaseRdfIdField);
        FacetField facetField = new FacetField("databaseRdfId", dbRdfId);
        doc.add(facetField);
      }
      String creator = mdRecord.getCreator();
      if (creator != null) {
        Field authorField = new Field("author", creator, ftStoredAnalyzed);
        doc.add(authorField);
        if (projectId.equals("jdg"))
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
      String titleStr = mdRecord.getTitle();
      if (titleStr != null) {
        Field titleField = new Field("title", titleStr, ftStoredAnalyzed);
        doc.add(titleField);
        if (projectId.equals("jdg"))
          titleField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
        titleStr = titleStr.toLowerCase();  // so that sorting is lower case
        Field titleFieldSorted = new SortedDocValuesField("titleSorted", new BytesRef(titleStr));
        doc.add(titleFieldSorted);
      }
      String alternativeTitleStr = mdRecord.getAlternativeTitle();
      if (alternativeTitleStr != null) {
        Field alternativeTitleField = new Field("alternativeTitle", alternativeTitleStr, ftStoredAnalyzed);
        doc.add(alternativeTitleField);
        if (projectId.equals("jdg"))
          alternativeTitleField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String publisher = mdRecord.getPublisher();
      if (publisher != null && ! publisher.isEmpty()) {
        String[] publishers = publisher.split(".");
        if (publishers.length == 0) {
          publishers = new String[1];
          publishers[0] = publisher;
        }
        for (int i=0; i<publishers.length; i++) {
          String p = publishers[i].trim();
          if (! p.isEmpty()) {
            FacetField facetField = new FacetField("publisher", p);
            doc.add(facetField);
          }
        }
        Field publisherField = new Field("publisher", publisher, ftStoredAnalyzed);
        doc.add(publisherField);
        if (projectId.equals("jdg"))
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
      if (yearStr != null) {
        Field dateField = new Field("date", yearStr, ftStoredAnalyzed);
        doc.add(dateField);
        if (projectId.equals("jdg"))
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
        if (projectId.equals("jdg"))
          descriptionField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String subject = mdRecord.getSubject();
      if (subject != null) {
        Field subjectField = new Field("subject", subject, ftStoredAnalyzed);
        doc.add(subjectField);
        if (projectId.equals("jdg"))
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
        Field subjectControlledDetailsField = new Field("subjectControlledDetails", subjectControlledDetails, ftStoredNotAnalyzed);
        doc.add(subjectControlledDetailsField);
        String subjectControlled = mdRecord.getSubjectControlled();
        if (subjectControlled != null && ! subjectControlled.isEmpty()) {
          Field subjectControlledField = new Field("subjectControlled", subjectControlled, ftStoredAnalyzed);
          doc.add(subjectControlledField);
          String[] dcTermsSubjects = subjectControlled.split("###");
          for (int i=0; i<dcTermsSubjects.length; i++) {
            String s = dcTermsSubjects[i].trim();
            if (! s.isEmpty()) {
              FacetField facetField = new FacetField("subjectControlled", s);
              doc.add(facetField);
            }
          }
        }
      }
      String swd = mdRecord.getSwd();
      if (swd != null) {
        Field swdField = new Field("swd", swd, ftStoredAnalyzed);
        doc.add(swdField);
        if (projectId.equals("jdg"))
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
        if (projectId.equals("jdg"))
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
        if (projectId.equals("jdg"))
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
        if (projectId.equals("jdg"))
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
        if (projectId.equals("jdg"))
          personsField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      if (mdRecord.getPersonsDetails() != null) {
        Field personsDetailsField = new StoredField("personsDetails", mdRecord.getPersonsDetails());
        doc.add(personsDetailsField);
      }
      if (mdRecord.getPlaces() != null) {
        Field placesField = new Field("places", mdRecord.getPlaces(), ftStoredAnalyzed);
        doc.add(placesField);
        if (projectId.equals("jdg"))
          placesField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String language = mdRecord.getLanguage();
      if (language == null) {
        if (collectionRdfId != null) {
          String mainLanguage = ProjectReader.getInstance().getCollectionMainLanguage(collectionRdfId);
          if (mainLanguage != null) {
            language = mainLanguage;
            mdRecord.setLanguage(mainLanguage);
          }
        }
      }
      if (language != null) {
        Field languageField = new Field("language", mdRecord.getLanguage(), ftStoredAnalyzed);
        doc.add(languageField);
        if (projectId.equals("jdg"))
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
      String extent = mdRecord.getExtent();
      if (extent != null) {
        Field extentField = new Field("extent", extent, ftStoredAnalyzed);
        doc.add(extentField);
        if (projectId.equals("jdg"))
          extentField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String docTokensOrig = mdRecord.getTokenOrig();
      if (docTokensOrig != null) {
        Field tokenOrigField = new Field("tokenOrig", docTokensOrig, ftStoredAnalyzed);
        doc.add(tokenOrigField);
        if (projectId.equals("jdg"))
          tokenOrigField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String docTokensNorm = mdRecord.getTokenNorm();
      if (docTokensNorm != null) {
        Field tokenNormField = new Field("tokenNorm", docTokensNorm, ftStoredAnalyzed);
        doc.add(tokenNormField);
        if (projectId.equals("jdg"))
          tokenNormField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String docTokensMorph = mdRecord.getTokenMorph();
      if (docTokensMorph != null) {
        Field tokenMorphField = new Field("tokenMorph", docTokensMorph, ftStoredAnalyzed);
        doc.add(tokenMorphField);
        if (projectId.equals("jdg"))
          tokenMorphField.setBoost(0.1f);  // jdg records should be ranked lower (because there are too much of them)
      }
      String content = mdRecord.getContent();
      if (content != null) {
        Field contentField = new Field("content", content, ftStoredAnalyzed);
        doc.add(contentField);
        // performance issue: in queries the lucene highlighter has too bad performance for bigger documents, so we save only the first 500.000 chars for highlighting
        if (content.length() > HIGHLIGHT_BIG_DOCUMENT_CHAR_SIZE) {
          String highlightContent = content.substring(0, 500000);
          Field highlightContentField = new Field("highlightContent", highlightContent, ftStoredAnalyzed);
          doc.add(highlightContentField);
        }
        if (projectId.equals("jdg"))
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
      facetsConfig.setMultiValued("collectionPerson", true);
      facetsConfig.setMultiValued("collectionSubject", true);
      Document docWithFacets = facetsConfig.build(taxonomyWriter, doc);

      documentsIndexWriter.addDocument(docWithFacets);
      
    } catch (Exception e) {
      LOGGER.error("indexDocumentLocal: " + mdRecord.getDocId());
      e.printStackTrace();
      throw new ApplicationException(e);
    }
  }

  public TSTLookup getSuggester() throws ApplicationException {
    // one time init of the suggester, if it is null (needs ca. 4 sec. for 3 Mio. token)
    if (suggester == null) {
      suggester = new TSTLookup();
      try {
        ArrayList<Token> tokensArrayList = new ArrayList<Token>(tokens.values());
        suggester.build(new TokenArrayListIterator(tokensArrayList));  // put all tokens into the suggester
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
    } catch (Exception e) {
      LOGGER.error("deleteDocumentLocal: " + mdRecord.getDocId());
      throw new ApplicationException(e);
    }
  }

  private void deleteAllProjectsLocal() throws ApplicationException {
    try {
      projectsIndexWriter.deleteAll();
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  public int deleteProject(String projectId) throws ApplicationException {
    int countDeletedDocs = -1;
    try {
      String queryDocumentsByProjectId = "projectId:" + projectId;
      if (projectId.equals("edoc"))
        queryDocumentsByProjectId = "webUri:*edoc.bbaw.de*";
      Hits projectDocHits = queryDocuments("lucene", queryDocumentsByProjectId, null, null, null, null, null, 0, 9, false, false); // TODO FastTaxonomyFacetCounts verursacht bei vielen deletes ArrayIndexOutOfBoundsException: 600000
      ArrayList<org.bbaw.wsp.cms.document.Document> projectDocs = projectDocHits.getHits();
      if (projectDocs != null) {
        countDeletedDocs = projectDocHits.getSize();
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
    String queryDatabaseRdfId = "databaseRdfId:\"" + databaseRdfId + "\"";
    Hits databaseDocHits = queryDocuments("lucene", queryDatabaseRdfId, null, null, null, null, null, 0, 9, false, false);
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
      Hits docHits = queryDocuments("lucene", deleteQuery.toString(), null, null, null, null, null, 0, 9, false, false);
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

  /**
   * Method is thread safe.
   * @param queryLanguage
   * @param queryStr
   * @param sortFieldNames
   * @param groupByField
   * @param fieldExpansion
   * @param language
   * @param from
   * @param to
   * @param withHitFragments
   * @param translate
   * @return
   * @throws ApplicationException
   */
  public Hits queryDocuments(String queryLanguage, String queryStr, String[] sortFieldNames, String groupByField, String[] groupBySortFieldNames, String fieldExpansion, String language, int from, int to, boolean withHitFragments, boolean translate) throws ApplicationException {
    Hits hits = null;
    IndexSearcher searcher = null;
    Date begin = new Date();
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
        query = buildFieldExpandedQuery("docs", queryParser, queryStr, fieldExpansion);
      }
      LanguageHandler languageHandler = new LanguageHandler();
      if (translate) {
        ArrayList<String> queryTerms = fetchTerms(query);
        languageHandler.translate(queryTerms, language);
      }
      Query morphQuery = buildMorphQuery(query, language, false, translate, languageHandler);
      ArrayList<String> germanQueryLemmas = fetchTerms(morphQuery, "deu", true, true);
      Query highlighterQuery = buildHighlighterQuery(query, language, translate, languageHandler);
      Sort sort = new Sort();
      SortField scoreSortField = new SortField(null, Type.SCORE); // default sort
      sort.setSort(scoreSortField);
      if (sortFieldNames != null) {
        sort = buildSort(sortFieldNames);  // build sort criteria
      }
      FacetsCollector facetsCollector = new FacetsCollector(true);
      Collector searchCollector = facetsCollector;
      TermSecondPassGroupingCollector groupByCollector = null;
      // groupBy query: first part: build query and collectors
      if (groupByField != null) {
        Sort groupBySort = Sort.RELEVANCE; // default sort: Lucene score
        if (groupBySortFieldNames != null) {
          groupBySort = buildSort(groupBySortFieldNames);  // build sort criteria
        }
        int maxDocsPerGroup = 5;  // default value
        if (groupByField.endsWith(")")) {
          int index1 = groupByField.indexOf("(");
          int index2 = groupByField.indexOf(")");
          String maxDocsPerGroupStr = groupByField.substring(index1 + 1, index2);
          maxDocsPerGroup = Integer.parseInt(maxDocsPerGroupStr);
          groupByField = groupByField.substring(0, index1);
        }
        String groupByFieldNameSorted = groupByField + "Sorted";
        TermFirstPassGroupingCollector termFirstPassGroupCollector = new TermFirstPassGroupingCollector(groupByFieldNameSorted, groupBySort, 1000);
        Query matchAllDocsQuery = new MatchAllDocsQuery();
        FacetsCollector.search(searcher, matchAllDocsQuery, 1, sort, true, true, termFirstPassGroupCollector);
        Collection<SearchGroup<BytesRef>> fieldGroups = termFirstPassGroupCollector.getTopGroups(0, true);
        groupByCollector = new TermSecondPassGroupingCollector(groupByFieldNameSorted, fieldGroups, groupBySort, sort, maxDocsPerGroup, true, true, true);
        searchCollector = MultiCollector.wrap(facetsCollector, groupByCollector);
      }
      TopDocs resultDocs = FacetsCollector.search(searcher, morphQuery, to + 1, sort, true, true, searchCollector);
      FacetsConfig facetsConfig = new FacetsConfig();
      FastTaxonomyFacetCounts facetCounts = new FastTaxonomyFacetCounts(taxonomyReader, facetsConfig, facetsCollector);
      List<FacetResult> tmpFacetResult = new ArrayList<FacetResult>();
      String[] facetsStr = {"organizationRdfId", "projectId", "projectRdfId", "collectionRdfId", "collectionType", "collectionPeriodOfTime", "collectionSubject", "collectionPerson", "databaseRdfId", "language", "author", "publisher", "date", "subject", "subjectControlled", "swd", "ddc", "entityPerson", "entityOrganisation", "entityPlace", "entityConcept", "type"};
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
        FastVectorHighlighter highlighter = new FastVectorHighlighter(true, false); // fieldMatch: false: cause any field to be highlighted regardless of whether the query matched specifically on them. The default behaviour is true, meaning that only fields that hold a query match will be highlighted.
        for (int i=from; i<=toTmp; i++) { 
          int docId = resultDocs.scoreDocs[i].doc;
          float score = resultDocs.scoreDocs[i].score;
          Document luceneDoc = searcher.doc(docId, docFields);
          org.bbaw.wsp.cms.document.Document doc = new org.bbaw.wsp.cms.document.Document(luceneDoc);
          doc.setScore(score);
          IndexableField highlightContentField = getHighlightContentField(luceneDoc);
          if (withHitFragments && highlightContentField != null) {
            ArrayList<String> hitFragments = getFragments(highlighter, highlighterQuery, docId, highlightContentField.name());
            if (hitFragments != null)
              doc.setHitFragments(hitFragments);
          }
          docs.add(doc);
        }
        GroupHits groupByHits = null;
        // groupBy query: second part: build query result 
        if (groupByField != null) {
          TopGroups<BytesRef> topGroups = groupByCollector.getTopGroups(0);
          if (topGroups != null) {
            GroupDocs<BytesRef>[] topGroupDocsTmp = topGroups.groups;
            // remove the empty groups
            ArrayList<GroupDocs<BytesRef>> topGroupDocs = new ArrayList<GroupDocs<BytesRef>>();
            for (int i=0; i<topGroupDocsTmp.length; i++) {
              GroupDocs<BytesRef> group = topGroupDocsTmp[i];
              int totalGroupHitsSize = group.totalHits;
              if (totalGroupHitsSize > 0)
                topGroupDocs.add(group);
            }            
            groupByHits = new GroupHits();
            groupByHits.setSizeTotalGroups(topGroupDocs.size());
            ArrayList<GroupDocuments> groupHitsArrayList = new ArrayList<GroupDocuments>();
            HashSet<String> docFieldsToFill = getDocFields();
            int groupByTo = to;
            if (topGroupDocs.size() <= groupByTo)
              groupByTo = topGroupDocs.size() - 1;
            for (int i=from; i<=groupByTo; i++) {
              GroupDocs<BytesRef> group = topGroupDocs.get(i);
              GroupDocuments groupDocuments = new GroupDocuments();
              int totalGroupHitsSize = group.totalHits;
              float groupMaxScore = group.maxScore;
              groupDocuments.setSize(totalGroupHitsSize);
              groupDocuments.setMaxScore(groupMaxScore);
              for (int j=0; j<group.scoreDocs.length; j++) {
                int docId = group.scoreDocs[j].doc;
                float score = group.scoreDocs[j].score;
                Document luceneDoc = searcher.doc(docId, docFieldsToFill);  // Performance: for bigger docs (field "content") this needs ca. 250 ms
                if (j==0) {
                  IndexableField luceneGroupByField = luceneDoc.getField(groupByField);
                  if (luceneGroupByField != null) {
                    String groupName = luceneGroupByField.stringValue();
                    groupDocuments.setName(groupName);
                    groupDocuments.setQuery(groupByField);
                  }
                }
                org.bbaw.wsp.cms.document.Document doc = new org.bbaw.wsp.cms.document.Document(luceneDoc);
                doc.setScore(score);
                IndexableField highlightContentField = getHighlightContentField(luceneDoc);
                if (withHitFragments && highlightContentField != null) {
                  ArrayList<String> hitFragments = getFragments(highlighter, highlighterQuery, docId, highlightContentField.name()); // Performance: for bigger docs (field "content") this needs ca. 500 ms
                  if (hitFragments != null)
                    doc.setHitFragments(hitFragments);
                }
                groupDocuments.addDocument(doc);
              }
              groupHitsArrayList.add(groupDocuments);
            }
            if (groupHitsArrayList.isEmpty())
              groupHitsArrayList = null;
            groupByHits.setGroupDocuments(groupHitsArrayList);
          }
        }
        int sizeTotalDocuments = documentsIndexReader.numDocs();
        // Terms terms = MultiFields.getTerms(documentsIndexReader, "tokenOrig");
        // int sizeTotalTerms = (int) terms.size(); // terms.size() does not work: delivers always -1 
        int sizeTotalTerms = tokens.size(); // term count over tokenOrig
        // DocFreqComparator cmp = new HighFreqTerms.DocFreqComparator();  // TODO
        // TermStats[] termStats = HighFreqTerms.getHighFreqTerms(documentsIndexReader, 20, "tokenOrig", cmp); // TODO aus lucene-misc: um Stopworte bereinigen
        // long sizeTotalTermsFreq = documentsIndexReader.getSumTotalTermFreq("tokenOrig");  // count of all words/terms in this field TODO
        Date end = new Date();
        long elapsedTime = end.getTime() - begin.getTime();
        if (docs != null) {
          hits = new Hits(docs, from, to);
          hits.setMaxScore(resultDocs.getMaxScore());
          hits.setSize(resultDocs.totalHits);
          hits.setSizeTotalDocuments(sizeTotalDocuments);
          hits.setSizeTotalTerms(sizeTotalTerms);
          hits.setElapsedTime(elapsedTime);
          hits.setQuery(morphQuery);
          hits.setFacets(facets);
          hits.setGroupByHits(groupByHits);
          hits.setQueryLemmas(germanQueryLemmas);
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

  private IndexableField getHighlightContentField(Document luceneDoc) {
    IndexableField highlightContentField = luceneDoc.getField("content");
    if (highlightContentField != null) {
      // lucene highlighting performance is bad for bigger documents so for bigger documents the smaller field highlightContent is used
      if (highlightContentField.stringValue().length() > HIGHLIGHT_BIG_DOCUMENT_CHAR_SIZE)
        highlightContentField = luceneDoc.getField("highlightContent");
    }
    if (highlightContentField == null)
      highlightContentField = luceneDoc.getField("title");
    return highlightContentField;
  }
  
  private ArrayList<String> getFragments(FastVectorHighlighter highlighter, Query highlighterQuery, int luceneDocId, String fieldName) throws ApplicationException {
    ArrayList<String> hitFragments = null;
    try {
      if (highlighterQuery != null) {
        FieldQuery highlighterFieldQuery = highlighter.getFieldQuery(highlighterQuery, documentsIndexReader);  // indexReader muss angegeben werden, damit die WildcardQuery etc. Ergebnisse liefert
        String[] textfragments = highlighter.getBestFragments(highlighterFieldQuery, documentsIndexReader, luceneDocId, fieldName, 100, 2);
        if (textfragments.length > 0) {
          hitFragments = new ArrayList<String>();
          for (int j=0; j<textfragments.length; j++) {
            String textFragment = textfragments[j];
            textFragment = textFragment.trim();  
            textFragment = checkHitFragment(textFragment);
            hitFragments.add(textFragment);
          }
        }
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return hitFragments;
  }
  
  public Hits queryProjects(String queryLanguage, String queryStr, String[] sortFieldNames, String fieldExpansion, String language, int from, int to, boolean translate) throws ApplicationException {
    Hits hits = null;
    IndexSearcher searcher = null;
    try {
      makeProjectsSearcherManagerUpToDate();
      searcher = projectsSearcherManager.acquire();
      String defaultQueryFieldName = "label";
      QueryParser queryParser = new QueryParser(defaultQueryFieldName, projectsPerFieldAnalyzer);
      queryParser.setAllowLeadingWildcard(true);
      Query query = null;
      if (queryStr.equals("*")) {
        query = new MatchAllDocsQuery();
      } else {
        if (queryLanguage != null && queryLanguage.equals("gl")) {
          queryStr = translateGoogleLikeQueryToLucene(queryStr);
        }
        query = buildFieldExpandedQuery("projects", queryParser, queryStr, fieldExpansion);
      }
      LanguageHandler languageHandler = new LanguageHandler();
      if (translate) {
        ArrayList<String> queryTerms = fetchTerms(query);
        languageHandler.translate(queryTerms, language);
      }
      Query morphQuery = buildMorphQuery(query, language, false, translate, languageHandler);
      Sort sort = new Sort();
      SortField scoreSortField = new SortField(null, Type.SCORE); // default sort
      sort.setSort(scoreSortField);
      if (sortFieldNames != null) {
        sort = buildSort(sortFieldNames);  // build sort criteria
      }
      FacetsCollector facetsCollector = new FacetsCollector(true);
      TopDocs resultDocs = FacetsCollector.search(searcher, morphQuery, to + 1, sort, true, true, facetsCollector);
      FacetsConfig facetsConfig = new FacetsConfig();
      FastTaxonomyFacetCounts facetCounts = new FastTaxonomyFacetCounts(projectsTaxonomyReader, facetsConfig, facetsCollector);
      List<FacetResult> tmpFacetResult = new ArrayList<FacetResult>();
      String[] facetsStr = {"id", "rdfId", "mainLanguage", "periodOfTime", "status", "type", "subject"};
      for (int f=0; f<facetsStr.length; f++) {
        String facetStr = facetsStr[f];
        Integer facetFieldCount = 1000;
        FacetResult facetResult = facetCounts.getTopChildren(facetFieldCount, facetStr);
        if (facetResult != null)
          tmpFacetResult.add(facetResult);
      }
      Facets facets = null;
      if (! tmpFacetResult.isEmpty()) {
        facets = new Facets(tmpFacetResult, null);
      }
      int toTmp = to;
      if (resultDocs.totalHits <= to)
        toTmp = resultDocs.totalHits - 1;
      HashSet<String> projectFields = getProjectFields();
      if (resultDocs != null) {
        ArrayList<org.bbaw.wsp.cms.document.Document> docs = new ArrayList<org.bbaw.wsp.cms.document.Document>();
        for (int i=from; i<=toTmp; i++) { 
          int docID = resultDocs.scoreDocs[i].doc;
          float score = resultDocs.scoreDocs[i].score;
          Document luceneDoc = searcher.doc(docID, projectFields);
          org.bbaw.wsp.cms.document.Document doc = new org.bbaw.wsp.cms.document.Document(luceneDoc);
          doc.setScore(score);
          docs.add(doc);
        }
        int sizeTotalDocuments = projectsIndexReader.numDocs();
        if (docs != null) {
          hits = new Hits(docs, from, to);
          hits.setMaxScore(resultDocs.getMaxScore());
          hits.setSize(resultDocs.totalHits);
          hits.setSizeTotalDocuments(sizeTotalDocuments);
          hits.setQuery(morphQuery);
          hits.setFacets(facets);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Lucene IndexHandler: queryProjects: " + e.getMessage());
      e.printStackTrace();
      throw new ApplicationException(e);
    } finally {
      try {
        if (searcher != null)
          projectsSearcherManager.release(searcher);
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
      String organizationRdfId = null;
      IndexableField organizationRdfIdField = doc.getField("organizationRdfId");
      if (organizationRdfIdField != null)
        organizationRdfId = organizationRdfIdField.stringValue();
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
      String alternativeTitle = null;
      IndexableField alternativeTitleField = doc.getField("alternativeTitle");
      if (alternativeTitleField != null)
        alternativeTitle = alternativeTitleField.stringValue();
      String publisher = null;
      IndexableField publisherField = doc.getField("publisher");
      if (publisherField != null)
        publisher = publisherField.stringValue();
      String language = null;
      IndexableField languageField = doc.getField("language");
      if (languageField != null)
        language = languageField.stringValue();
      else {
        String mainLang = ProjectReader.getInstance().getCollectionMainLanguage(collectionRdfId);
        if (mainLang != null)
          language = mainLang;
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
      String extent = null;
      IndexableField extentField = doc.getField("extent");
      if (extentField != null)
        extent = extentField.stringValue();
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
      mdRecord.setOrganizationRdfId(organizationRdfId);
      mdRecord.setCollectionRdfId(collectionRdfId);
      mdRecord.setDatabaseRdfId(databaseRdfId);
      mdRecord.setCreator(author);
      mdRecord.setCreatorDetails(authorDetails);
      mdRecord.setTitle(title);
      mdRecord.setAlternativeTitle(alternativeTitle);
      mdRecord.setPublisher(publisher);
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
      mdRecord.setExtent(extent);
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

  private void addToken(String fieldName, String value, int count) throws ApplicationException {
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
            Token token = tokens.get(termContentStr);
            if (token == null) {
              token = new Token(termContent);
              token.setFreq(docFreq);
              tokens.put(termContentStr, token);
            } else {
              int tokenFreq = token.getFreq() + docFreq;
              token.setFreq(tokenFreq);
            }
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
      if (projectsTaxonomyWriter != null)
        projectsTaxonomyWriter.close();
      if (documentsIndexWriter != null)
        documentsIndexWriter.close();
      if (projectsIndexWriter != null)
        projectsIndexWriter.close();
      if (documentsSearcherManager != null)
        documentsSearcherManager.close();
      if (projectsSearcherManager != null)
        projectsSearcherManager.close();
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
      if (projectsIndexWriter != null)
        projectsIndexWriter.forceMerge(1);
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
        boolean addQueryTerm = true;
        if (logicalAndQueryTerms.size() > 1 && queryTerm.equals("*"))
          addQueryTerm = false; // special case: if we have the "*" + another query terms, then the "*" is nonsense, so delete him because of performance reasons (e.g. query = "* collectionType:Webseite" is semantically the same as "collectionType:Webseite")
        if (addQueryTerm)
          luceneQueryStr = luceneQueryStr + "+" + queryTerm + " "; 
      }
      luceneQueryStr = luceneQueryStr.substring(0, luceneQueryStr.length() - 1);
    }
    return luceneQueryStr;
  }
  
  private Query buildFieldExpandedQuery(String type, QueryParser queryParser, String queryStr, String fieldExpansion) throws ApplicationException {
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
      String[] queryExpansionFields = QUERY_EXPANSION_FIELDS_ALL;
      if (type.equals("docs") && fieldExpansion.equals("allMorph"))
        queryExpansionFields = QUERY_EXPANSION_FIELDS_ALL_MORPH;
      else if (type.equals("projects") && fieldExpansion.equals("all"))
        queryExpansionFields = QUERY_EXPANSION_FIELDS_PROJECTS_ALL;
      else if (type.equals("projects") && fieldExpansion.equals("allMorph"))
        queryExpansionFields = QUERY_EXPANSION_FIELDS_PROJECTS_ALL_MORPH;
      if (fieldExpansion.equals("none")) {
        Query q = queryParser.parse(queryStr);
        retQuery = q;
      } else {
        BooleanQuery.Builder boolQueryBuilder = new BooleanQuery.Builder();
        HashMap<String, Query> booleanClauses = new HashMap<String, Query>();
        for (int i=0; i < queryExpansionFields.length; i++) {
          String expansionField = queryExpansionFields[i];  // e.g. "author"
          String expansionFieldQueryStr = expansionField + ":(" + queryStr + ")";
          Query q = queryParser.parse(expansionFieldQueryStr);
          Query qTmp = booleanClauses.get(q.toString());
          if (qTmp == null) { // if query is not already contained in boolean clauses
            booleanClauses.put(q.toString(), q);
            boolQueryBuilder.add(q, BooleanClause.Occur.SHOULD);
          }
        }
        retQuery = boolQueryBuilder.build();
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

  /**
   * recursively fetch all terms of the query
   * 
   * @param query
   * @return
   */
  private ArrayList<String> fetchTerms(Query query, String language, boolean onlyLemmas, boolean onlyFulltextFields) throws ApplicationException {
    ArrayList<String> terms = new ArrayList<String>();
    if (query instanceof TermQuery) {
      TermQuery termQuery = (TermQuery) query;
      terms = fetchTerms(termQuery, language, onlyLemmas, onlyFulltextFields);
    } else if (query instanceof BooleanQuery) {
      BooleanQuery booleanQuery = (BooleanQuery) query;
      terms = fetchTerms(booleanQuery, language, onlyLemmas, onlyFulltextFields);
    } else if (query instanceof PrefixQuery) {
      PrefixQuery prefixQuery = (PrefixQuery) query;
      String prefix = prefixQuery.getPrefix().text();
      if (! prefix.isEmpty())
        terms.add(prefix);
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
    } else if (query instanceof MatchAllDocsQuery) {
      // nothing
    } else {
      String queryStr = query.toString();
      terms.add(queryStr); 
      // all other cases: PrefixQuery, PhraseQuery, FuzzyQuery, TermRangeQuery, ...
    }
    return terms;
  }

  private ArrayList<String> fetchTerms(TermQuery termQuery, String language, boolean onlyLemmas, boolean onlyFulltextFields) throws ApplicationException {
    if (language == null)
      language = "eng";
    ArrayList<String> terms = new ArrayList<String>();
    Term termQueryTerm = termQuery.getTerm();
    String term = termQuery.getTerm().text();
    String fieldName = termQueryTerm.field();
    boolean termIsValidFulltextField = false;
    if (onlyFulltextFields) {
      if (fieldName != null && (fieldName.equals("tokenOrig") || fieldName.equals("tokenMorph")))
        termIsValidFulltextField = true;
    }
    if (onlyLemmas) {
      if (termIsValidFulltextField) {
        LexHandler lexHandler = LexHandler.getInstance();
        ArrayList<Lemma> lemmas = lexHandler.getLemmas(term, "form", language, Normalizer.DICTIONARY, true); 
        if (lemmas == null) {
          if (! terms.contains(term)) {
            terms.add(term);
          }
        } else {
          for (int i = 0; i < lemmas.size(); i++) {
            Lemma lemma = lemmas.get(i);
            String lemmaName = lemma.getLemmaName();
            if (! terms.contains(term)) {
              terms.add(lemmaName);
            }
          }
        }
      }
      return terms;
    }
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

  private ArrayList<String> fetchTerms(BooleanQuery query, String language, boolean onlyLemmas, boolean onlyFulltextFields) throws ApplicationException {
    ArrayList<String> terms = new ArrayList<String>();
    List<BooleanClause> booleanClauses = query.clauses();
    for (int i = 0; i < booleanClauses.size(); i++) {
      BooleanClause boolClause = booleanClauses.get(i);
      Query q = boolClause.getQuery();
      ArrayList<String> qTerms = fetchTerms(q, language, onlyLemmas, onlyFulltextFields);
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
      documentsFieldAnalyzers.put("organizationRdfId", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("collectionRdfId", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("collectionType", new StandardAnalyzer());
      documentsFieldAnalyzers.put("author", new StandardAnalyzer());
      documentsFieldAnalyzers.put("title", new StandardAnalyzer());
      documentsFieldAnalyzers.put("alternativeTitle", new StandardAnalyzer());
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
      documentsFieldAnalyzers.put("extent", new StandardAnalyzer());
      documentsFieldAnalyzers.put("schemaName", new StandardAnalyzer());
      documentsFieldAnalyzers.put("lastModified", new KeywordAnalyzer());
      documentsFieldAnalyzers.put("tokenOrig", new StandardAnalyzer());
      documentsFieldAnalyzers.put("tokenNorm", new StandardAnalyzer());
      documentsFieldAnalyzers.put("tokenMorph", new StandardAnalyzer());
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

  private IndexWriter getProjectsWriter() throws ApplicationException {
    IndexWriter writer = null;
    String luceneProjectsDirectoryStr = Constants.getInstance().getLuceneProjectsDir();
    try {
      Map<String, Analyzer> projectsFieldAnalyzers = new HashMap<String, Analyzer>();
      projectsFieldAnalyzers.put("id", new StandardAnalyzer());
      projectsFieldAnalyzers.put("rdfId", new KeywordAnalyzer()); 
      projectsFieldAnalyzers.put("label", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("abstract", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("type", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("duration", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("status", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("mainLanguage", new StandardAnalyzer());
      projectsFieldAnalyzers.put("subjects", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("place", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("periodOfTime", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("persons", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("staff", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("collectionLabels", new StandardAnalyzer()); 
      projectsFieldAnalyzers.put("collectionAbstracts", new StandardAnalyzer()); 
      projectsPerFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), projectsFieldAnalyzers);
      IndexWriterConfig conf = new IndexWriterConfig(projectsPerFieldAnalyzer);
      conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
      conf.setRAMBufferSizeMB(300);  // 300 MB because some documents are big; 16 MB is default 
      Path luceneProjectsDirectory = Paths.get(luceneProjectsDirectoryStr);
      FSDirectory fsDirectory = FSDirectory.open(luceneProjectsDirectory);
      writer = new IndexWriter(fsDirectory, conf);
      writer.commit();
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return writer;
  }

  private Sort buildSort(String[] sortFieldNames) {
    Sort sort = new Sort();
    ArrayList<SortField> sortFields = new ArrayList<SortField>();
    for (int i=0; i<sortFieldNames.length; i++) {
      String sortFieldName = sortFieldNames[i];
      Type sortFieldType = getDocSortFieldType(sortFieldName);
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
  
  private HashSet<String> getDocFields() {
    HashSet<String> fields = new HashSet<String>();
    fields.add("id");
    fields.add("docId");
    fields.add("identifier");
    fields.add("uri");
    fields.add("webUri");
    fields.add("projectId");
    fields.add("projectRdfId");
    fields.add("organizationRdfId");
    fields.add("collectionRdfId");
    fields.add("collectionType");
    fields.add("databaseRdfId");
    fields.add("author");
    fields.add("authorDetails");
    fields.add("title");
    fields.add("alternativeTitle");
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
    fields.add("extent");
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
  
  private HashSet<String> getDocFieldsWithoutContent() {
    HashSet<String> fields = new HashSet<String>();
    fields.add("id");
    fields.add("docId");
    fields.add("identifier");
    fields.add("uri");
    fields.add("webUri");
    fields.add("projectId");
    fields.add("projectRdfId");
    fields.add("organizationRdfId");
    fields.add("collectionRdfId");
    fields.add("collectionType");
    fields.add("databaseRdfId");
    fields.add("author");
    fields.add("authorDetails");
    fields.add("title");
    fields.add("alternativeTitle");
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
    fields.add("extent");
    fields.add("schemaName");
    fields.add("lastModified");
    fields.add("entities");
    fields.add("entitiesDetails");
    fields.add("persons");
    fields.add("personsDetails");
    fields.add("places");
    return fields;
  }

  private HashSet<String> getProjectFields() {
    HashSet<String> fields = new HashSet<String>();
    fields.add("id");
    fields.add("rdfId");
    fields.add("label");
    fields.add("abstract");
    fields.add("type");
    fields.add("duration");
    fields.add("status");
    fields.add("mainLanguage");
    fields.add("subjects");
    fields.add("place");
    fields.add("periodOfTime");
    fields.add("persons");
    fields.add("staff");
    fields.add("collectionLabels");
    fields.add("collectionAbstracts");
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

  private DirectoryReader getProjectsReader() throws ApplicationException {
    DirectoryReader reader = null;
    String luceneProjectsDirectoryStr = Constants.getInstance().getLuceneProjectsDir();
    Path luceneProjectsDirectory = Paths.get(luceneProjectsDirectoryStr);
    try {
      FSDirectory fsDirectory = FSDirectory.open(luceneProjectsDirectory);
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

  private TaxonomyWriter getProjectsTaxonomyWriter() throws ApplicationException {
    TaxonomyWriter taxonomyWriter = null;
    String taxonomyDirStr = Constants.getInstance().getLuceneProjectsTaxonomyDir();
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
  
  private TaxonomyReader getProjectsTaxonomyReader() throws ApplicationException {
    TaxonomyReader taxonomyReader = null;
    String taxonomyDirStr = Constants.getInstance().getLuceneProjectsTaxonomyDir();
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

  private void makeProjectsSearcherManagerUpToDate() throws ApplicationException {
    try {
      boolean isCurrent = projectsSearcherManager.isSearcherCurrent();
      if (!isCurrent) {
        projectsSearcherManager.maybeRefresh();
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
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