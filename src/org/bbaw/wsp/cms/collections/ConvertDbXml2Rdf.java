package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class ConvertDbXml2Rdf {
  private CollectionReader collectionReader;
  private String outputRdfDir = "/home/juergens/wspEtc/rdfData/transformFromSqlDump/";
  private XQueryEvaluator xQueryEvaluator;

  public static void main(String[] args) throws ApplicationException {
    try {
      ConvertDbXml2Rdf convertDbXml2Rdf = new ConvertDbXml2Rdf();
      convertDbXml2Rdf.init();
      convertDbXml2Rdf.convert("avhseklit");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    xQueryEvaluator = new XQueryEvaluator();
  }
  
  private void convert(String collectionId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(collectionId);
    Database db = collection.getDatabase();
    Hashtable<String, Row> resources = new Hashtable<String, Row>();
    try {
      String inputXmlDumpFileName = db.getXmlDumpFileName();
      File inputXmlDumpFile = new File(inputXmlDumpFileName);      
      String dbName = db.getName();  // e.g. "avh_biblio";
      File outputRdfFile = new File(outputRdfDir + collectionId + "/" + dbName + ".rdf");
      StringBuilder rdfStrBuilder = new StringBuilder();
      rdfStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
      rdfStrBuilder.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:ore=\"http://www.openarchives.org/ore/terms/\" xmlns:dc=\"http://purl.org/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:edm=\"http://www.europeana.eu/schemas/edm/\" xmlns:xi=\"http://www.w3.org/2001/XInclude\" xml:base=\""+collection.getRdfId()+"\">");
      URL xmlDumpFileUrl = inputXmlDumpFile.toURI().toURL();
      String mainResourcesTable = db.getMainResourcesTable();  // e.g. "titles";
      String mainResourcesTableId = db.getMainResourcesTableId();  // e.g. "title_id";
      XdmValue xmdValueMainResources = xQueryEvaluator.evaluate(xmlDumpFileUrl, "/" + dbName + "/" + mainResourcesTable);
      XdmSequenceIterator xmdValueMainResourcesIterator = xmdValueMainResources.iterator();
      if (xmdValueMainResources != null && xmdValueMainResources.size() > 0) {
        while (xmdValueMainResourcesIterator.hasNext()) {
          XdmItem xdmItemMainResource = xmdValueMainResourcesIterator.next();
          String xdmItemMainResourceStr = xdmItemMainResource.toString();
          Row row = xml2row(xdmItemMainResourceStr);
          String rowRdfStr = row2rdf(collection, row);
          rdfStrBuilder.append(rowRdfStr);
          String id = row.getFieldValue(mainResourcesTableId);
          resources.put(id, row);
        }
      }
      rdfStrBuilder.append("</rdf:RDF>");
      FileUtils.writeStringToFile(outputRdfFile, rdfStrBuilder.toString());
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  private Row xml2row(String xmlStr) throws ApplicationException {
    Row row = new Row();
    XdmValue xmdValueFields = xQueryEvaluator.evaluate(xmlStr, "/*/*"); // e.g. /titles/title
    XdmSequenceIterator xmdValueFieldsIterator = xmdValueFields.iterator();
    if (xmdValueFields != null && xmdValueFields.size() > 0) {
      while (xmdValueFieldsIterator.hasNext()) {
        XdmItem xdmItemField = xmdValueFieldsIterator.next();
        String fieldStr = xdmItemField.toString();
        String fieldName = xQueryEvaluator.evaluateAsString(fieldStr, "*/name()");
        String fieldValue = xdmItemField.getStringValue();
        row.addField(fieldName, fieldValue);
      }
    }
    return row;  
  }

  private String row2rdf(Collection collection, Row row) {
    String collectionId = collection.getId();
    String collectionRdfId = collection.getRdfId();
    String mainLanguage = collection.getMainLanguage();
    Database db = collection.getDatabase();
    String mainResourcesTableId = db.getMainResourcesTableId();
    String webIdPreStr = db.getWebIdPreStr();
    String webIdAfterStr = db.getWebIdAfterStr();
    StringBuilder rdfStrBuilder = new StringBuilder();
    String id = row.getFieldValue(mainResourcesTableId);
    String rdfId = "http://" + collectionId + ".bbaw.de/id/" + id;
    String rdfWebId = rdfId;
    if (webIdPreStr != null)
      rdfWebId = webIdPreStr + id;
    if (webIdAfterStr != null)
      rdfWebId = rdfWebId + webIdAfterStr;
    String rdfAggregationId = rdfId + "/Aggregation";
    //TODO delete xml:base
    rdfStrBuilder.append("<rdf:Description rdf:about=\"" + rdfWebId + "\">");
    rdfStrBuilder.append("<ore:describedBy rdf:resource=\"" + collectionRdfId + "\"/>");
    rdfStrBuilder.append("<ore:describes rdf:resource=\"" + rdfAggregationId + "\"/>");
    rdfStrBuilder.append("<rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/ResourceMap\"/>");
    rdfStrBuilder.append("<dc:creator rdf:resource=\"http://wsp.normdata.rdf/Wissensspeicher\"/>");
    int year = Calendar.getInstance().get(Calendar.YEAR);
    int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
    int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    String dateStr = year + "-" + month + "-" + day;
    rdfStrBuilder.append("<dcterms:created rdf:datatype=\"http://www.w3.org/2001/XMLSchema#date\">" + dateStr + "</dcterms:created>");
    rdfStrBuilder.append("<dcterms:rights rdf:resource=\"http://creativecommons.org/licenses/by-nc/2.5/\"/>");
    rdfStrBuilder.append("</rdf:Description>");
    rdfStrBuilder.append("<rdf:Description rdf:about=\"" + rdfAggregationId + "\">");
    rdfStrBuilder.append("<ore:describedBy rdf:resource=\"" + rdfWebId + "\"/>");
    rdfStrBuilder.append("<rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/Aggregation\"/>");
    rdfStrBuilder.append("<dcterms:identifier>" + id + "</dcterms:identifier>");
    rdfStrBuilder.append("<dc:identifier rdf:resource=\"" + rdfWebId + "\"/>");
    String dbFieldCreator = db.getDbField("creator");
    if (dbFieldCreator != null) {
      String creator = row.getFieldValue(dbFieldCreator);
      if (creator != null && ! creator.isEmpty()) {
        creator = StringUtils.deresolveXmlEntities(creator);
        rdfStrBuilder.append("<dc:creator>" + creator + "</dc:creator>");
      }
    }
    String dbFieldTitle = db.getDbField("title");
    if (dbFieldTitle != null) {
      String title = row.getFieldValue(dbFieldTitle);
      if (title != null && ! title.isEmpty()) {
        title = StringUtils.deresolveXmlEntities(title);
        rdfStrBuilder.append("<dc:title>" + title + "</dc:title>");
      }
    }
    String dbFieldPublisher = db.getDbField("publisher");
    if (dbFieldPublisher != null) {
      String publisher = row.getFieldValue(dbFieldPublisher);
      if (publisher != null && ! publisher.isEmpty()) {
        publisher = StringUtils.deresolveXmlEntities(publisher);
        rdfStrBuilder.append("<dc:publisher>" + publisher + "</dc:publisher>");
        // rdfStrBuilder.append("<dc:publisher rdf:resource=\"" + publisher + "\"/>");
      }
    }
    String dbFieldDate = db.getDbField("date");
    if (dbFieldDate != null) {
      String date = row.getFieldValue(dbFieldDate);
      if (date != null && ! date.isEmpty()) {
        date = StringUtils.deresolveXmlEntities(date);
        rdfStrBuilder.append("<dc:date>" + date + "</dc:date>");
      }
    }
    String dbFieldPages = db.getDbField("extent");
    if (dbFieldPages != null) {
      String pages = row.getFieldValue(dbFieldPages);
      if (pages != null && ! pages.isEmpty()) {
        pages = StringUtils.deresolveXmlEntities(pages);
        rdfStrBuilder.append("<dc:pages>" + pages + "</dc:pages>");
      }
    }
    String dbFieldLanguage = db.getDbField("language");
    String language = row.getFieldValue(dbFieldLanguage);
    if (language == null && mainLanguage != null)
      language = mainLanguage;
    else if (language == null && mainLanguage == null)
      language = "deu";
    rdfStrBuilder.append("<dc:language><dcterms:ISO639-3><rdf:value>" + language + "</rdf:value></dcterms:ISO639-3></dc:language>");
    rdfStrBuilder.append("<dc:format><dcterms:IMT rdf:value=\"text/html\"/></dc:format>");
    String dbFieldSubject = db.getDbField("subject");
    if (dbFieldSubject != null) {
      String subject = row.getFieldValue(dbFieldSubject);
      if (subject != null && ! subject.isEmpty()) {
        String[] subjects = subject.split("###");
        if (subjects != null) {
          for (int i=0; i<subjects.length; i++) {
            String s = subjects[i];
            s = StringUtils.deresolveXmlEntities(s);
            rdfStrBuilder.append("<dc:subject>" + s + "</dc:subject>");
          }
        }
      }
    }
    String dbFieldAbstract = db.getDbField("abstract");
    if (dbFieldAbstract != null) {
      String abstractt = row.getFieldValue(dbFieldAbstract);
      if (abstractt != null && ! abstractt.isEmpty()) {
        abstractt = StringUtils.deresolveXmlEntities(abstractt);
        rdfStrBuilder.append("<dc:abstract>" + abstractt + "</dc:abstract>");
      }
    }
    // TODO further dc fields
    rdfStrBuilder.append("</rdf:Description>");
    return rdfStrBuilder.toString();
  }

}
