package org.bbaw.wsp.cms.confmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.harvester.PathExtractor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class CollectionReader {

  ConfManagerResultWrapper cmrw;
  private HashMap<String, ConfManagerResultWrapper> wrapperContainer;
  private static CollectionReader collectionReader;

  private CollectionReader() {
    wrapperContainer = new HashMap<String, ConfManagerResultWrapper>();
    readConfFiles();
  }

  public static CollectionReader getInstance() {
    if (collectionReader == null)
      collectionReader = new CollectionReader();
    return collectionReader;
  }

  private void readConfFiles(){
    System.out.println("---------------");
    System.out.println("reading configuration files...");

    // holt alle konfiguratiuonsdateien aus dem konf-Ordner
    PathExtractor ext = new PathExtractor();
    List<String> configsList = ext.extractPathLocally(Constants.getInstance().getConfDir());
    System.out.println("Anzahl der konfugirationsdateien : " + configsList.size());
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    // docFactory.setNamespaceAware(true);
    DocumentBuilder builder = null;

    File configFile = null;
    for (String configXml : configsList) {
      System.out.println("reading : " + configXml);
      try {
        builder = docFactory.newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      }
      configFile = new File(configXml);
      Document document = null;
        try {
          document = builder.parse(configFile);
        } catch (SAXException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
        cmrw = new ConfManagerResultWrapper();

        NodeList idlist = document.getElementsByTagName("collectionId");
        // darf jeweils nur ein node enthalten sein
        Node idNode = idlist.item(0);
        if(idNode != null){
          if (!idNode.getTextContent().equals("")) {
            cmrw.setCollectionId(idNode.getTextContent());
          }
        }
        NodeList nodeliste = document.getElementsByTagName("mainLanguage");
        // darf jeweils nur ein node enthalten sein
        Node langNode = nodeliste.item(0);
        if(langNode != null){
          if (!langNode.getTextContent().equals("")) {
            cmrw.setMainLanguage(langNode.getTextContent());
          }
        }
        NodeList collNamelist = document.getElementsByTagName("name");
        // darf jeweils nur ein node enthalten sein
        Node nameNode = collNamelist.item(0);
        if(nameNode != null){
          if (!nameNode.getTextContent().equals("")) {
            cmrw.setCollectionName(nameNode.getTextContent());
          }
        }
        
        NodeList fieldNodes = document.getElementsByTagName("field");
        ArrayList<String> fields = new ArrayList<String>();
        fields = new ArrayList<String>();
        if(fieldNodes != null){
          for (int i = 0; i < fieldNodes.getLength(); i++) {
            if (!fieldNodes.item(i).getTextContent().equals("")) {
              fields.add((fieldNodes.item(i).getTextContent().trim()));
            }
          }
        }
        cmrw.setFields(fields);

        NodeList nodeli = document.getElementsByTagName("collectionDataUrl");
        Node dataNode = nodeli.item(0);
        if(dataNode != null){
          if (!dataNode.getTextContent().trim().equals("")) {
            cmrw.setCollectionDataUrl(dataNode.getTextContent());
          }
        }
        
        wrapperContainer.put(cmrw.getCollectionId(), cmrw);
    }
  }

  public ConfManagerResultWrapper getResultWrapper(String collectionId) {
    ConfManagerResultWrapper cmrw = wrapperContainer.get(collectionId);
    return cmrw;
  }

}
