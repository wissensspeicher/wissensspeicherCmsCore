package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.general.Constants;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class ConvertConfigXml2Rdf {
  private static Logger LOGGER = Logger.getLogger(ConvertConfigXml2Rdf.class);
  private CollectionReader collectionReader;
  private String outputRdfDir = "/home/joey/dataWsp/projects/";
  private XQueryEvaluator xQueryEvaluator;
  private File inputNormdataFile;
  public static void main(String[] args) throws ApplicationException {
    try {
      ConvertConfigXml2Rdf convertConfigXml2Rdf = new ConvertConfigXml2Rdf();
      convertConfigXml2Rdf.init();
      // convertConfigXml2Rdf.convertAll();
      convertConfigXml2Rdf.proofRdfProjects();
      // convertConfigXml2Rdf.convert("mega");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    xQueryEvaluator = new XQueryEvaluator();
    String inputNormdataFileName = Constants.getInstance().getMdsystemNormdataFile();
    inputNormdataFile = new File(inputNormdataFileName);      
  }
  
  private void convertAll() throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (int i=0; i<collections.size(); i++) {
      Collection collection = collections.get(i);
      convert(collection);
    }
  }
  
  private void convert(String collectionId) throws ApplicationException {
    convert(collectionReader.getCollection(collectionId));
  }
  
  private void convert(Collection collection) throws ApplicationException {
    String collectionId = collection.getId();
    String collectionRdfId = collection.getRdfId();
    try {
      File outputRdfFile = new File(outputRdfDir + collectionId + "-resources.rdf");
      StringBuilder rdfStrBuilder = new StringBuilder();
      rdfStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      rdfStrBuilder.append("<rdf:RDF \n");
      rdfStrBuilder.append("   xmlns=\"" + collectionRdfId + "\" \n");
      rdfStrBuilder.append("   xml:base=\"" + collectionRdfId + "\" \n");
      rdfStrBuilder.append("   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n");
      rdfStrBuilder.append("   xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" \n");
      rdfStrBuilder.append("   xmlns:dc=\"http://purl.org/elements/1.1/\" \n");
      rdfStrBuilder.append("   xmlns:dcterms=\"http://purl.org/dc/terms/\" \n");
      rdfStrBuilder.append("   xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" \n");
      rdfStrBuilder.append("   xmlns:gnd=\"http://d-nb.info/standards/elementset/gnd#\" \n");
      rdfStrBuilder.append("   xmlns:ore=\"http://www.openarchives.org/ore/terms/\" \n");
      rdfStrBuilder.append(">\n");
      URL inputNormdataFileUrl = inputNormdataFile.toURI().toURL();
      String projectRdfStr = xQueryEvaluator.evaluateAsString(inputNormdataFileUrl, "/*:RDF/*:Description[@*:about='" + collectionRdfId + "']");
      if (projectRdfStr != null && ! projectRdfStr.trim().isEmpty()) {
        proofProjectRdfStr(projectRdfStr, collection); // compare rdf project fields with config fields
        WspUrl[] dataUrls = collection.getDataUrls();
        if (dataUrls != null) {
          for (int i=0; i<dataUrls.length; i++) {
            WspUrl dataUrl = dataUrls[i];
            String dataUrlStr = dataUrl.getUrl();
            rdfStrBuilder.append("<rdf:Description rdf:about=\"" + dataUrlStr + "\">\n");
            rdfStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
            rdfStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + collectionRdfId + "\"/>\n");
            rdfStrBuilder.append("  <dc:identifier rdf:resource=\"" + dataUrlStr + "\"/>\n");
            if (dataUrl.isEXistDir()) {
              rdfStrBuilder.append("  <dc:type>eXistDir</dc:type>\n");
              rdfStrBuilder.append("  <!-- special use of this field to express exclusions of directories -->\n");
              String excludesStr = collection.getExcludesStr();
              if (excludesStr != null) {
                String[] exludesStrArray = excludesStr.split(" ");            
                rdfStrBuilder.append("  <dc:coverage>\n");
                for (int j=0; j<exludesStrArray.length; j++) {
                  String exludeStr = exludesStrArray[j];
                  rdfStrBuilder.append("    <exclude>" + exludeStr + "</exclude>\n");
                }
                rdfStrBuilder.append("  </dc:coverage>\n");
              }
            }
            rdfStrBuilder.append("  <dc:rights>http://creativecommons.org/licenses/by-sa/4.0/</dc:rights>\n");
            rdfStrBuilder.append("</rdf:Description>\n");
          }
        }
      } else {
        LOGGER.error("Project: \"" + collectionRdfId + "\" (configId: \"" + collectionId + "\") does not exist");
      }
      rdfStrBuilder.append("</rdf:RDF>");
      FileUtils.writeStringToFile(outputRdfFile, rdfStrBuilder.toString());
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  private void proofProjectRdfStr(String projectRdfStr, Collection collection) throws ApplicationException {
    String collectionId = collection.getId();
    String collectionRdfId = collection.getRdfId();
    String collectionName = collection.getName();
    String mainLanguage = collection.getMainLanguage();
    String webBaseUrl = collection.getWebBaseUrl();
    String countFoafNickName = xQueryEvaluator.evaluateAsString(projectRdfStr, "count(/*:Description/*:nick)");
    String countFoafName = xQueryEvaluator.evaluateAsString(projectRdfStr, "count(/*:Description/*:name)");
    String countDcLanguage = xQueryEvaluator.evaluateAsString(projectRdfStr, "count(/*:Description/*:language)");
    String countFoafHomepage = xQueryEvaluator.evaluateAsString(projectRdfStr, "count(/*:Description/*:homepage/@*:resource)");
    if (countFoafNickName != null && ! countFoafNickName.contains("0") && ! countFoafNickName.contains("1"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Field <foaf:nick> exists " + countFoafNickName + " times");
    if (countFoafName != null && ! countFoafName.contains("0") && ! countFoafName.contains("1"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Field <foaf:name> exists " + countFoafName + " times");
    if (countDcLanguage != null && ! countDcLanguage.contains("0") && ! countDcLanguage.contains("1"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Field <dc:language> exists " + countDcLanguage + " times");
    if (countFoafHomepage != null && ! countFoafHomepage.contains("0") && ! countFoafHomepage.contains("1"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Field <foaf:homepage> exists " + countFoafHomepage + " times");
    String foafNickName = xQueryEvaluator.evaluateAsString(projectRdfStr, "/*:Description/*:nick[1]/text()");
    String foafName = xQueryEvaluator.evaluateAsString(projectRdfStr, "/*:Description/*:name[1]/text()");
    String dcLanguage = xQueryEvaluator.evaluateAsString(projectRdfStr, "/*:Description/*:language[1]/text()");
    String foafHomepage = xQueryEvaluator.evaluateAsString(projectRdfStr, "string(/*:Description/*:homepage[1]/@*:resource)");
    String rdfType = xQueryEvaluator.evaluateAsString(projectRdfStr, "string(/*:Description/*:type[1]/@*:resource)");
    if (foafNickName == null || foafNickName.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <foaf:nick rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + collectionId + "</foaf:nick>");
    else if (! foafNickName.equals(collectionId))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change \"<foaf:nick>" + foafNickName + "</foaf:nick>\" to <foaf:nick rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + collectionId + "</foaf:nick>");
    if (foafName == null || foafName.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <foaf:name rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + collectionName + "</foaf:name>");
    else if (! foafName.equals(collectionName))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change \"<foaf:name>" + foafName + "</foaf:name>\" to <foaf:name rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + collectionName + "</foaf:name>");
    if (dcLanguage == null || dcLanguage.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <dc:language>" + mainLanguage + "</dc:language>");
    else if (! dcLanguage.equals(mainLanguage))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change \"<dc:language>" + dcLanguage + "</dc:language>\" to <dc:language>" + mainLanguage + "</dc:language>");
    if (foafHomepage == null || foafHomepage.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <foaf:homepage rdf:resource=\"" + webBaseUrl + "\"/>");
    else if (! foafHomepage.equals(webBaseUrl))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change \"<foaf:homepage rdf:resource=\"" + foafHomepage + "\"/>\" to <foaf:homepage rdf:resource=\"" + webBaseUrl + "\"/>");
    if (rdfType == null || rdfType.isEmpty())
      LOGGER.error("Project: \"" + collectionRdfId + "\": Insert mandatory field <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Project\"/>");
    else if (! rdfType.equals("http://xmlns.com/foaf/0.1/Project"))
      LOGGER.error("Project: \"" + collectionRdfId + "\": Change <rdf:type> to <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Project\"/>");
  }

  private void proofRdfProjects() throws ApplicationException {
    try {
      URL inputNormdataFileUrl = inputNormdataFile.toURI().toURL();
      String rdfTypeProject = "http://xmlns.com/foaf/0.1/Project"; 
      XdmValue xmdValueProjects = xQueryEvaluator.evaluate(inputNormdataFileUrl, "/*:RDF/*:Description[*:type[@*:resource='" + rdfTypeProject + "']]");
      XdmSequenceIterator xmdValueProjectsIterator = xmdValueProjects.iterator();
      if (xmdValueProjects != null && xmdValueProjects.size() > 0) {
        while (xmdValueProjectsIterator.hasNext()) {
          XdmItem xdmItemProject = xmdValueProjectsIterator.next();
          String xdmItemProjectStr = xdmItemProject.toString();
          String projectRdfId = xQueryEvaluator.evaluateAsString(xdmItemProjectStr, "string(/*:Description/@*:about)");
          String foafNickName = xQueryEvaluator.evaluateAsString(xdmItemProjectStr, "/*:Description/*:nick[1]/text()");
          if (foafNickName == null || foafNickName.isEmpty()) {
            LOGGER.error("Project: \"" + projectRdfId + "\" has no \"<foaf:nick> entry");
          } else {
            Collection coll = collectionReader.getCollection(foafNickName);
            if (coll == null)
              LOGGER.error("Project: \"" + projectRdfId + "\" with \"<foaf:nick>" + foafNickName + "</foaf:nick>\"" + " has no config file");
          }
        }
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
}
