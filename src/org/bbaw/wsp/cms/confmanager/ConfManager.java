package org.bbaw.wsp.cms.confmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.bbaw.wsp.cms.harvester.PathExtractor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import org.bbaw.wsp.cms.general.Constants;

public class ConfManager {

  ConfManagerResultWrapper cmrw;
  private HashMap<String, ConfManagerResultWrapper> wrapperContainer;
  private static ConfManager confManager;

  private ConfManager() {
    wrapperContainer = new HashMap<String, ConfManagerResultWrapper>();
    try {
      readConfigs();
    } catch (XPathExpressionException e) {
      e.printStackTrace();
    }
  }

  public static ConfManager getInstance(){
    if(confManager == null)
      confManager = new ConfManager();
    return confManager;
  }
  
  /**
   * checks if an update of a project is necessary by checking configuration
   * file
   * 
   * @throws XPathExpressionException
   */
  private void checkCollectionConfFiles() throws XPathExpressionException {
    System.out.println("---------------");
    System.out.println("checking configuration files...");

    // holt alle konfiguratiuonsdateien aus dem konf-Ordner
    PathExtractor ext = new PathExtractor();
    List<String> configsList = ext.extractPathLocally(Constants.getInstance().getConfDir());
    System.out.println("Anzahl der konfugirationsdateien : " + configsList.size());
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    // docFactory.setNamespaceAware(true);
    DocumentBuilder builder;

    XPathFactory xPathFactory = XPathFactory.newInstance();
    try {
      File configFile = null;
      // �berpr�ft alle konf-dateien auf update und f�hrt es bei bedarf aus
      for (String configXml : configsList) {
        System.out.println("checking : " + configXml);
        builder = docFactory.newDocumentBuilder();
        configFile = new File(configXml);
        Document document = null;
        try{
          document = builder.parse(configFile);
        
          // /////////////////
          XPath xPath = xPathFactory.newXPath();
          NodeList nl = document.getElementsByTagName("update");
          XPathExpression updateExpr = xPath.compile("//collection/update/text()");
          Object updateResult = updateExpr.evaluate(document, XPathConstants.STRING);
          String updateNode = (String) updateResult;
          // /////////////////
  
          if (updateNode != null) {
            if(!updateNode.trim().equals("")){
              org.w3c.dom.Node n = nl.item(0);
              // String updateValue = n.getTextContent();
              if (updateNode.trim().equals("true")) {
    
                System.out.println("update tag is set on : " + updateNode);
                cmrw = new ConfManagerResultWrapper();
    
                NodeList idlist = document.getElementsByTagName("collectionId");
                // darf jeweils nur ein node enthalten sein
                Node idNode = idlist.item(0);
                if (!idNode.getTextContent().equals("")) {
                  cmrw.setCollectionId(idNode.getTextContent());
                }
                NodeList nodeliste = document.getElementsByTagName("mainLanguage");
                // darf jeweils nur ein node enthalten sein
                Node langNode = nodeliste.item(0);
                if (!langNode.getTextContent().equals("")) {
                  cmrw.setMainLanguage(langNode.getTextContent());
                }
    
                NodeList collNamelist = document.getElementsByTagName("name");
                // darf jeweils nur ein node enthalten sein
                Node nameNode = collNamelist.item(0);
                if (!nameNode.getTextContent().equals("")) {
                  cmrw.setCollectionName(nameNode.getTextContent());
                }
    
                NodeList fieldNodes = document.getElementsByTagName("field");
                ArrayList<String> fields = new ArrayList<String>();
                fields = new ArrayList<String>();
                for (int i = 0; i < fieldNodes.getLength(); i++) {
                  if (!fieldNodes.item(i).getTextContent().equals("")) {
                    fields.add((fieldNodes.item(i).getTextContent().trim()));
                  }
                }
                cmrw.setFields(fields);

                NodeList nodeli = document.getElementsByTagName("collectionDataUrl");
                Node node = nodeli.item(0);
                if (!node.getTextContent().trim().equals("")) {
                  cmrw.setCollectionDataUrl(node.getTextContent());
                  extractUrlsFromCollections(node.getTextContent(), cmrw);
                  
                  // flag auf false setzen durch serialisierung in xml
                  n.setTextContent("false");
                  FileOutputStream os = new FileOutputStream(configFile);
                  XMLSerializer ser = new XMLSerializer(os, null);
                  ser.serialize(document);
    
                }
                wrapperContainer.put(idNode.getTextContent(), cmrw);
              }
            }else{
              // create Dummy
              cmrw = new ConfManagerResultWrapper();
              wrapperContainer.put("", cmrw);
            }
          }else{
            //create Dummy
            cmrw = new ConfManagerResultWrapper();
            wrapperContainer.put("", cmrw);
          }
        }catch(Exception e){
          e.printStackTrace();
          System.out.println("no ressources to update");
        }
      }
    } catch (ParserConfigurationException e1) {
      e1.printStackTrace();
    }
  }

  /**
   * der extractor holt alle projekt zugeh�rigen Urls
   */
  private void extractUrlsFromCollections(String collectionDataUrl, ConfManagerResultWrapper cmrw) {
    System.out.println("collecting urls of resources that need update...");
    if(!collectionDataUrl.equals("")){
      PathExtractor extractor = new PathExtractor();
      List<String> collectionUrls = extractor.initExtractor(collectionDataUrl);
      cmrw.setCollectionUrls(collectionUrls);
    }
  }

  public void readConfigs() throws XPathExpressionException {
    checkCollectionConfFiles();
  }

  public ConfManagerResultWrapper getResultWrapper(String collectionId) {
    ConfManagerResultWrapper cmrw = wrapperContainer.get(collectionId);
    return cmrw;
  }
}
