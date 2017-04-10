package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.collections.DBpediaSpotlightHandler;
import org.bbaw.wsp.cms.document.Annotation;
import org.bbaw.wsp.cms.document.DBpediaResource;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.general.Constants;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;

public class Annotator {
  private static Logger LOGGER = Logger.getLogger(Annotator.class);
  private String[] options;
  private MetadataHandler metadataHandler;
  private Harvester harvester;
  private String annotationDir;
  
  public static Annotator getInstance() throws ApplicationException {
    Annotator  annotator = new Annotator();
    annotator.init();
    return annotator;
  }
  
  private void init() throws ApplicationException {
    metadataHandler = MetadataHandler.getInstance();
    harvester = Harvester.getInstance();
    annotationDir = Constants.getInstance().getAnnotationsDir();
  }
  
  public void setOptions(String[] options) {
    this.options = options;
  }
  
  public void annotate(Project project) throws ApplicationException {
    int counter = 0;
    String projectId = project.getId();
    LOGGER.info("Annotate project: " + projectId + " ...");
    if (options != null && options.length == 1 && options[0].equals("dbType:crawl")) {
      // nothing when only the crawl db should be updated
    } else if (options != null && options.length == 1 && options[0].startsWith("dbName:")) {
      // nothing when only one db should be updated
    } else {
      File annoationProjectDir = new File(annotationDir + "/" + project.getId());
      FileUtils.deleteQuietly(annoationProjectDir);
    }
    if (options != null && options.length == 1 && options[0].startsWith("dbName:")) {
      // if db is specified: do not index the project rdf records
    } else {
      ArrayList<MetadataRecord> mdRecords = harvester.getMetadataRecordsByRecordsFile(project);
      if (mdRecords != null) {
        counter = counter + mdRecords.size();
        annotate(project, null, mdRecords);
      }
    }
    ArrayList<Database> projectDBs = project.getDatabases();
    if (options != null && options.length == 1 && options[0].equals("dbType:crawl")) {
      projectDBs = project.getDatabasesByType("crawl");
    } else if (options != null && options.length == 1 && options[0].startsWith("dbName:")) {
      String dbName = options[0].replaceAll("dbName:", "");
      projectDBs = project.getDatabasesByName(dbName);
    }
    if (projectDBs != null) {
      for (int i=0; i<projectDBs.size(); i++) {
        Database projectDB = projectDBs.get(i);
        String dbType = projectDB.getType();
        boolean annotateDB = dbType != null && (dbType.equals("crawl") || dbType.equals("eXist") || dbType.equals("oai"));
        if (annotateDB) {
          ArrayList<MetadataRecord> dbMdRecords = harvester.getMetadataRecordsByRecordsFile(project, projectDB);
          if (dbMdRecords != null) {
            counter = counter + dbMdRecords.size();
            annotate(project, projectDB, dbMdRecords);
          }
        }
      }
    }
    LOGGER.info("Project: " + projectId + " with " + counter + " records annotated");
  }
  
  public void delete(Project project) throws ApplicationException {
    try {
      String annoationProjectDirStr = annotationDir + "/" + project.getId();
      Runtime.getRuntime().exec("rm -rf " + annoationProjectDirStr);  // fast also when the directory contains many files
      LOGGER.info("Project annotation directory: " + annoationProjectDirStr + " successfully deleted");
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  public ArrayList<MetadataRecord> getMetadataRecordsByRecordsFile(Project project) throws ApplicationException {
    File recordsFile = new File(annotationDir + "/" + project.getId() + "/metadata/records/" + project.getId() + ".xml");
    if (! recordsFile.exists()) {
      String harvestDir = Constants.getInstance().getHarvestDir();
      recordsFile = new File(harvestDir + "/" + project.getId() + "/metadata/records/" + project.getId() + ".xml");
    }
    ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecordsByRecordsFile(recordsFile);
    return mdRecords;
  }

  public ArrayList<MetadataRecord> getMetadataRecordsByRecordsFile(Project project, Database db) throws ApplicationException {
    File dbRecordsFile = new File(annotationDir + "/" + project.getId() + "/metadata/records/" + project.getId() + "-" + db.getName() + "-1.xml");
    if (! dbRecordsFile.exists()) {
      String harvestDir = Constants.getInstance().getHarvestDir();
      dbRecordsFile = new File(harvestDir + "/" + project.getId() + "/metadata/records/" + project.getId() + "-" + db.getName() + "-1.xml");
    }
    ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecordsByRecordsFile(dbRecordsFile);
    return mdRecords;
  }

  private void annotate(Project project, Database db, ArrayList<MetadataRecord> mdRecords) throws ApplicationException {
    String logStr = "Annotate metadata records (" + project.getId() + ") ...";
    if (db != null)
      logStr = "Annotate metadata records (" + project.getId() + ", " + db.getName() + ", " + db.getType() + ") ...";
    LOGGER.info(logStr);
    StringBuilder rdfStrBuilder = new StringBuilder(); 
    rdfStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    rdfStrBuilder.append("<rdf:RDF \n");
    rdfStrBuilder.append("   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n");
    rdfStrBuilder.append("   xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" \n");
    rdfStrBuilder.append("   xmlns:dc=\"http://purl.org/dc/elements/1.1/\" \n");
    rdfStrBuilder.append("   xmlns:gnd=\"http://d-nb.info/standards/elementset/gnd#/\" \n");
    rdfStrBuilder.append("   xmlns:dcterms=\"http://purl.org/dc/terms/\" \n");
    rdfStrBuilder.append("   xmlns:dbpedia-spotlight=\"http://spotlight.dbpedia.org/spotlight#\" \n");
    rdfStrBuilder.append(">\n");
    StringBuilder recordsXmlStrBuilder = new StringBuilder();
    recordsXmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    recordsXmlStrBuilder.append("<records>\n");
    for (int i=0; i<mdRecords.size(); i++) {
      MetadataRecord mdRecord = mdRecords.get(i);
      annotate(project, db, mdRecord, rdfStrBuilder);
      recordsXmlStrBuilder.append(mdRecord.toXmlStr());
      int iplus1 = i + 1;
      int annotateInterval = iplus1 % 100; // after each 100 annotations do Logging
      if (annotateInterval == 0) {
        LOGGER.info("Annotate " + iplus1 + "'th record: " + mdRecord.getWebUri());
      }
      // without that, there would be a memory leak (with many big documents in one project)
      // with that the main big fields (content etc.) could be garbaged
      mdRecord.setAllNull();
    }
    recordsXmlStrBuilder.append("</records>\n");
    rdfStrBuilder.append("</rdf:RDF>\n"); 
    String projectId = project.getId();
    File rdfFile = new File(annotationDir + "/" + projectId + "/metadata/annotations/" + projectId + ".rdf");
    File recordsFile = new File(annotationDir + "/" + projectId + "/metadata/records/" + projectId + ".xml");
    if (db != null) {
      rdfFile = new File(annotationDir + "/" + projectId + "/metadata/annotations/" + projectId + "-" + db.getName() + "-1.rdf");
      recordsFile = new File(annotationDir + "/" + projectId + "/metadata/records/" + projectId + "-" + db.getName() + "-1.xml");
    }
    String rdfStr = rdfStrBuilder.toString();
    String recordsXmlStr = recordsXmlStrBuilder.toString();
    try {
      if (! rdfStr.isEmpty())
        FileUtils.writeStringToFile(rdfFile, rdfStr, "utf-8");
      FileUtils.writeStringToFile(recordsFile, recordsXmlStr, "utf-8");
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  private void annotate(Project project, Database db, MetadataRecord mdRecord, StringBuilder projectRdfStrBuilder) throws ApplicationException {
    Annotation docContentAnnotation = null;
    Annotation docTitleAnnotation = null;
    String docId = mdRecord.getDocId();
    try {
      DBpediaSpotlightHandler dbPediaSpotlightHandler = DBpediaSpotlightHandler.getInstance();
      String docContent = harvester.getFulltext("content", mdRecord, db);
      if (docContent == null && mdRecord.getDescription() != null && ! mdRecord.getDescription().isEmpty())
        docContent = mdRecord.getDescription();
      if (docContent != null) {
        docContentAnnotation = dbPediaSpotlightHandler.annotate(project, docId, docContent, "0.99", 50);
      }
      String docTitle = mdRecord.getTitle();
      if (docTitle != null)
        docTitleAnnotation = dbPediaSpotlightHandler.annotate(project, docId, docTitle, "0.5", 50);
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
      String projectRdfId = project.getRdfId();
      // edoc: mdRecord.projectRdfId could be another project
      String mdRecordProjectRdfId = mdRecord.getProjectRdfId();
      if (mdRecordProjectRdfId != null && ! mdRecordProjectRdfId.isEmpty() && ! mdRecordProjectRdfId.equals(projectRdfId))
        projectRdfId = mdRecordProjectRdfId;
      if (resources != null) {
        StringBuilder resourceNamesStrBuilder = new StringBuilder();
        StringBuilder resourcesXmlStrBuilder = new StringBuilder();
        resourcesXmlStrBuilder.append("<resources>\n");
        String uri = mdRecord.getUri();
        String webUri = mdRecord.getWebUri();
        if (webUri != null)
          uri = webUri;
        uri = StringUtils.deresolveXmlEntities(uri);
        projectRdfStrBuilder.append("<rdf:Description rdf:about=\"" + uri + "\">\n");
        projectRdfStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
        projectRdfStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + projectRdfId + "\"/>\n");
        projectRdfStrBuilder.append("  <dc:identifier rdf:resource=\"" + uri + "\"/>\n");
        for (int i=0; i<resources.size(); i++) {
          DBpediaResource r = resources.get(i);
          resourcesXmlStrBuilder.append(r.toXmlStr());
          projectRdfStrBuilder.append(r.toRdfStr());
          String resourceName = r.getName();
          if (i == resources.size() - 1)
            resourceNamesStrBuilder.append(resourceName);
          else
            resourceNamesStrBuilder.append(resourceName + "###");
        }
        resourcesXmlStrBuilder.append("</resources>\n");
        projectRdfStrBuilder.append("</rdf:Description>\n");
        mdRecord.setEntities(resourceNamesStrBuilder.toString());
        mdRecord.setEntitiesDetails(resourcesXmlStrBuilder.toString());
      }
    }
  }
}
