package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.dochandler.DBpediaSpotlightHandler;
import org.bbaw.wsp.cms.document.Annotation;
import org.bbaw.wsp.cms.document.DBpediaResource;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.general.Constants;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;

public class Annotator {
  private static Logger LOGGER = Logger.getLogger(Annotator.class);
  private static Annotator annotator;
  private CollectionReader collectionReader;
  private MetadataHandler metadataHandler;
  private Harvester harvester;
  private String annotationDir;
  
  public static Annotator getInstance() throws ApplicationException {
    if(annotator == null) {
      annotator = new Annotator();
      annotator.init();
    }
    return annotator;
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    metadataHandler = MetadataHandler.getInstance();
    harvester = Harvester.getInstance();
    annotationDir = Constants.getInstance().getAnnotationsDir();
  }
  
  public void annotateCollections() throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      annotate(collection);
    }
  }

  public void annotate(Collection collection) throws ApplicationException {
    int counter = 0;
    String collId = collection.getId();
    LOGGER.info("Annotate collection: " + collId + " ...");
    File harvestCollectionDir = new File(annotationDir + "/" + collection.getId());
    FileUtils.deleteQuietly(harvestCollectionDir);
    ArrayList<MetadataRecord> mdRecords = harvester.getMetadataRecordsByRecordsFile(collection);
    counter = counter + mdRecords.size();
    annotate(collection, null, mdRecords); 
    ArrayList<Database> collectionDBs = collection.getDatabases();
    if (collectionDBs != null) {
      for (int i=0; i<collectionDBs.size(); i++) {
        Database collectionDB = collectionDBs.get(i);
        String dbType = collectionDB.getType();
        if (dbType != null && (dbType.equals("crawl") || dbType.equals("eXist") || dbType.equals("oai"))) {
          ArrayList<MetadataRecord> dbMdRecords = harvester.getMetadataRecordsByRecordsFile(collection, collectionDB);
          counter = counter + dbMdRecords.size();
          annotate(collection, collectionDB, dbMdRecords);
        }
      }
    }
    LOGGER.info("Project: " + collId + " with " + counter + " records annotated");
  }
  
  public ArrayList<MetadataRecord> getMetadataRecordsByRecordsFile(Collection collection) throws ApplicationException {
    File recordsFile = new File(annotationDir + "/" + collection.getId() + "/metadata/records/" + collection.getId() + ".xml");
    ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecordsByRecordsFile(recordsFile);
    return mdRecords;
  }

  public ArrayList<MetadataRecord> getMetadataRecordsByRecordsFile(Collection collection, Database db) throws ApplicationException {
    File dbRecordsFile = new File(annotationDir + "/" + collection.getId() + "/metadata/records/" + collection.getId() + "-" + db.getName() + "-1.xml");
    ArrayList<MetadataRecord> mdRecords = metadataHandler.getMetadataRecordsByRecordsFile(dbRecordsFile);
    return mdRecords;
  }

  private void annotate(Collection collection, Database db, ArrayList<MetadataRecord> mdRecords) throws ApplicationException {
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
      annotate(collection, mdRecord, rdfStrBuilder);
      recordsXmlStrBuilder.append(mdRecord.toXmlStr());
    }
    recordsXmlStrBuilder.append("</records>\n");
    rdfStrBuilder.append("</rdf:RDF>\n"); 
    String collId = collection.getId();
    File rdfFile = new File(annotationDir + "/" + collId + "/metadata/annotations/" + collId + ".rdf");
    File recordsFile = new File(annotationDir + "/" + collId + "/metadata/records/" + collId + ".xml");
    if (db != null) {
      rdfFile = new File(annotationDir + "/" + collId + "/metadata/annotations/" + collId + "-" + db.getName() + "-1.rdf");
      recordsFile = new File(annotationDir + "/" + collId + "/metadata/records/" + collId + "-" + db.getName() + "-1.xml");
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
  
  private void annotate(Collection collection, MetadataRecord mdRecord, StringBuilder collectionRdfStrBuilder) throws ApplicationException {
    Annotation docContentAnnotation = null;
    Annotation docTitleAnnotation = null;
    String docId = mdRecord.getDocId();
    try {
      DBpediaSpotlightHandler dbPediaSpotlightHandler = DBpediaSpotlightHandler.getInstance();
      String docContent = harvester.getFulltext("content", mdRecord);
      if (docContent == null && mdRecord.getDescription() != null && ! mdRecord.getDescription().isEmpty())
        docContent = mdRecord.getDescription();
      if (docContent != null) {
        docContentAnnotation = dbPediaSpotlightHandler.annotate(collection, docId, docContent, "0.99", 50);
      }
      String docTitle = mdRecord.getTitle();
      if (docTitle != null)
        docTitleAnnotation = dbPediaSpotlightHandler.annotate(collection, docId, docTitle, "0.5", 50);
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
      String collRdfId = collection.getRdfId();
      if (resources != null) {
        StringBuilder resourceNamesStrBuilder = new StringBuilder();
        StringBuilder resourcesXmlStrBuilder = new StringBuilder();
        resourcesXmlStrBuilder.append("<resources>\n");
        String uri = mdRecord.getUri();
        String webUri = mdRecord.getWebUri();
        if (webUri != null)
          uri = webUri;
        uri = StringUtils.deresolveXmlEntities(uri);
        collectionRdfStrBuilder.append("<rdf:Description rdf:about=\"" + uri + "\">\n");
        collectionRdfStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
        collectionRdfStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + collRdfId + "\"/>\n");
        collectionRdfStrBuilder.append("  <dc:identifier rdf:resource=\"" + uri + "\"/>\n");
        for (int i=0; i<resources.size(); i++) {
          DBpediaResource r = resources.get(i);
          resourcesXmlStrBuilder.append(r.toXmlStr());
          collectionRdfStrBuilder.append(r.toRdfStr());
          String resourceName = r.getName();
          if (i == resources.size() - 1)
            resourceNamesStrBuilder.append(resourceName);
          else
            resourceNamesStrBuilder.append(resourceName + "###");
        }
        resourcesXmlStrBuilder.append("</resources>\n");
        collectionRdfStrBuilder.append("</rdf:Description>\n");
        mdRecord.setEntities(resourceNamesStrBuilder.toString());
        mdRecord.setEntitiesDetails(resourcesXmlStrBuilder.toString());
      }
    }
  }
}
