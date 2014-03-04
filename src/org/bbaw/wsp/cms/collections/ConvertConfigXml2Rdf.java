package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

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

  public static void main(String[] args) throws ApplicationException {
    try {
      ConvertConfigXml2Rdf convertConfigXml2Rdf = new ConvertConfigXml2Rdf();
      convertConfigXml2Rdf.init();
      // convertConfigXml2Rdf.convert("mega");
      convertConfigXml2Rdf.convertAll();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
    xQueryEvaluator = new XQueryEvaluator();
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
      String inputNormdataFileName = Constants.getInstance().getMdsystemNormdataFile();
      File inputNormdataFile = new File(inputNormdataFileName);      
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
        LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\" out of config file with id: \"" + collectionId + "\" does not exist");
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
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": Field <foaf:nick> exists " + countFoafNickName + " times");
    if (countFoafName != null && ! countFoafName.contains("0") && ! countFoafName.contains("1"))
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": Field <foaf:name> exists " + countFoafName + " times");
    if (countDcLanguage != null && ! countDcLanguage.contains("0") && ! countDcLanguage.contains("1"))
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": Field <dc:language> exists " + countDcLanguage + " times");
    if (countFoafHomepage != null && ! countFoafHomepage.contains("0") && ! countFoafHomepage.contains("1"))
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": Field <foaf:homepage> exists " + countFoafHomepage + " times");
    String foafNickName = xQueryEvaluator.evaluateAsString(projectRdfStr, "/*:Description/*:nick[1]/text()");
    String foafName = xQueryEvaluator.evaluateAsString(projectRdfStr, "/*:Description/*:name[1]/text()");
    String dcLanguage = xQueryEvaluator.evaluateAsString(projectRdfStr, "/*:Description/*:language[1]/text()");
    String foafHomepage = xQueryEvaluator.evaluateAsString(projectRdfStr, "string(/*:Description/*:homepage[1]/@*:resource)");
    String rdfType = xQueryEvaluator.evaluateAsString(projectRdfStr, "string(/*:Description/*:type[1]/@*:resource)");
    if (foafNickName == null || foafNickName.isEmpty())
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": Mandatory field <foaf:nick> does not exist. /wsp/collection/id = \"" + collectionId + "\"");
    else if (! foafNickName.equals(collectionId))
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": \"<foaf:nick>" + foafNickName + "</foaf:nick>\" is not equal to /wsp/collection/id = \"" + collectionId + "\"");
    if (foafName == null || foafName.isEmpty())
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": Mandatory field <foaf:name> does not exist. /wsp/collection/name = \"" + collectionName + "\"");
    else if (! foafName.equals(collectionName))
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": \"<foaf:name>" + foafName + "</foaf:name>\" is not equal to /wsp/collection/name = \"" + collectionName + "\" (out of config file with id: \"" + collectionId + "\")");
    if (dcLanguage == null || dcLanguage.isEmpty())
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": Mandatory field <dc:language> does not exist. /wsp/collection/mainLanguage = \"" + mainLanguage + "\"");
    else if (! dcLanguage.equals(mainLanguage))
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": \"<dc:language>" + dcLanguage + "</dc:language>\" is not equal to /wsp/collection/mainLanguage = \"" + mainLanguage + "\" (out of config file with id: \"" + collectionId + "\")");
    if (foafHomepage == null || foafHomepage.isEmpty())
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": Mandatory field <foaf:homepage> does not exist. /wsp/collection/url/webBaseUrl = \"" + webBaseUrl + "\"");
    else if (! foafHomepage.equals(webBaseUrl))
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": \"<foaf:homepage>" + foafHomepage + "</foaf:homepage>\" is not equal to /wsp/collection/url/webBaseUrl = \"" + webBaseUrl + "\" (out of config file with id: \"" + collectionId + "\")");
    if (rdfType == null || rdfType.isEmpty())
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": Mandatory field <rdf:type> does not exist");
    else if (! rdfType.equals("http://xmlns.com/foaf/0.1/Project"))
      LOGGER.error("Error in wsp.normadata.rdf: RdfId: \"" + collectionRdfId + "\": \"<rdf:type>" + rdfType + "</rdf:type>\" is not equal to \"" + "http://xmlns.com/foaf/0.1/Project" + "\" (out of config file with id: \"" + collectionId + "\")");
  }
  
}
