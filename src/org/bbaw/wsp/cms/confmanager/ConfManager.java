package org.bbaw.wsp.cms.confmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;

import org.bbaw.wsp.cms.harvester.PathExtractor;

import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

import org.bbaw.wsp.cms.general.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

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
    // holt alle Konfiguratiuonsdateien aus dem konf-Ordner
    PathExtractor ext = new PathExtractor();
    List<String> configsList = ext.extractPathLocally(Constants.getInstance().getConfDir());
    System.out.println("Anzahl der Konfigurationsdateien : " + configsList.size());
    try {
      File configFile = null;
      // Ueberprueft alle Konf-dateien auf update und fuehrt es bei Bedarf aus
      for (String configXml : configsList) {
        System.out.println("checking : " + configXml);
        XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
        configFile = new File(configXml);
        URL srcUrl = configFile.toURI().toURL();
        String update = xQueryEvaluator.evaluateAsString(srcUrl, "//collection/update/text()");
        if (update != null && update.equals("true")) {
          System.out.println("update tag is set on : " + update);
          cmrw = new ConfManagerResultWrapper();
          String collectionId = xQueryEvaluator.evaluateAsString(srcUrl, "//collectionId/text()");
          if (collectionId != null) {
            cmrw.setCollectionId(collectionId);
          }
          String mainLanguage = xQueryEvaluator.evaluateAsString(srcUrl, "//mainLanguage/text()");
          if (mainLanguage != null) {
            cmrw.setMainLanguage(mainLanguage);
          }
          String name = xQueryEvaluator.evaluateAsString(srcUrl, "//name/text()");
          if (name != null) {
            cmrw.setCollectionName(name);
          }
          String fieldsStr = xQueryEvaluator.evaluateAsStringValueJoined(srcUrl, "//field");
          ArrayList<String> fields = new ArrayList<String>();
          if (fields != null) {
            fieldsStr = fieldsStr.trim();
            String[] fieldsArray = fieldsStr.split(" ");
            for (int i=0; i<fieldsArray.length; i++) {
              String field = fieldsArray[i];
              fields.add(field);
            }
            cmrw.setFields(fields);
          }
          String collectionDataUrl = xQueryEvaluator.evaluateAsString(srcUrl, "//collectionDataUrl/text()");
          if (collectionDataUrl != null) {
            cmrw.setCollectionDataUrl(collectionDataUrl);
            extractUrlsFromCollections(collectionDataUrl, cmrw);
            // flag im Konfigurations-File auf false setzen durch serialisierung in das File
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document configFileDocument = builder.parse(configFile);
            NodeList updateNodeList = configFileDocument.getElementsByTagName("update");
            Node n = updateNodeList.item(0);
            n.setTextContent("false");
            FileOutputStream os = new FileOutputStream(configFile);
            XMLSerializer ser = new XMLSerializer(os, null);
            ser.serialize(configFileDocument);  //  Vorsicht: wenn es auf true ist: es wird alles neu indexiert
          }
          wrapperContainer.put(collectionId, cmrw);
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * der Extractor holt alle Projekt zugehoerigen Urls
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
