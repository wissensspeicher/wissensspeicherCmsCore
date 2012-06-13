package org.bbaw.wsp.cms.confmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import  org.bbaw.wsp.cms.general.Constants;

public class ConfManager {
  
    private PrintWriter out;
    private List<String> updateUrls;
    private String collectionDataUrl;
    private String collectionId;
    private List<String> collectionUrls;
    ConfManagerResultWrapper cmrw;
    private String mainLanguage;
    ArrayList<String> fields;

    public ConfManager(){
      cmrw = new ConfManagerResultWrapper();
    }
    
    /**
     * checks if an update of a project is necessary by checking configuration file
     * @throws XPathExpressionException 
     */
    private void checkForChangesInConfigurations() throws XPathExpressionException{
        System.out.println("---------------");
        System.out.println("checking configuration files...");
        
        //holt alle konfiguratiuonsdateien aus dem konf-Ordner
        PathExtractor ext = new PathExtractor();
        List<String> configsList = ext.extractPathLocally(Constants.getInstance().getConfDir());
        System.out.println("Anzahl der konfugirationsdateien : "+configsList.size());
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        //docFactory.setNamespaceAware(true);
        DocumentBuilder builder;

        XPathFactory xPathFactory = XPathFactory.newInstance();
        try {
            File configFile = null;
            //�berpr�ft alle konf-dateien auf update und f�hrt es bei bedarf aus
            for (String configXml : configsList) {
                System.out.println("checking : "+configXml);
                builder = docFactory.newDocumentBuilder();
                configFile = new File(configXml); 
                Document document = builder.parse(configFile);

                ///////////////////
                XPath xPath = xPathFactory.newXPath();
                XPathExpression expr = xPath.compile("//collection/collectionDataUrl/text()");
                Object result = expr.evaluate(document, XPathConstants.NODESET);
                NodeList nodes = (NodeList) result;
                for (int i = 0; i < nodes.getLength(); i++) {
                    System.out.println(" XPath nodes : "+nodes.item(i).getNodeValue());
                }
                ///////////////////
                
                NodeList nl = document.getElementsByTagName("update");
                XPathExpression updateExpr = xPath.compile("//collection/update/text()");
                Object updateResult = updateExpr.evaluate(document, XPathConstants.STRING);
                String updateNode = (String) updateResult;
                System.out.println("*************");
                System.out.println("updateNodes : "+updateNode);
                System.out.println("*************");
                ///////////////////
                
                if(updateNode != null || updateNode.trim().equals("")){
                    this.updateUrls = new ArrayList<String>();
                    org.w3c.dom.Node n = nl.item(0);  
//                    String updateValue = n.getTextContent();
                    if(updateNode.trim().equals("true")){
                        System.out.println("update tag is set on : "+updateNode);
                        NodeList nodeli = document.getElementsByTagName("collectionDataUrl");
                        
                        NodeList nodelis = document.getElementsByTagName("collectionId");
                        //darf jeweils nur ein node enthalten sein
                        Node node = nodeli.item(0);
                        Node nameNode = nodelis.item(0);
                        if(!nameNode.getTextContent().equals("")){
                            this.collectionId = nameNode.getTextContent(); 
                        }
                        NodeList nodeliste = document.getElementsByTagName("mainLanguage");
                        //darf jeweils nur ein node enthalten sein
                        Node langNode = nodeliste.item(0);
                        if(!langNode.getTextContent().equals("")){
                            this.mainLanguage = langNode.getTextContent(); 
                        }
                        NodeList fieldNodes = document.getElementsByTagName("fields");
                        this.fields = new ArrayList<String>();
                        for(int i =0;i<fieldNodes.getLength();i++){
                          if(!fieldNodes.item(i).getTextContent().equals("")){
//                            this.fields.add((fieldNodes.item(i).getTextContent()));
                          }
                        }
                        if(!langNode.getTextContent().equals("")){
                            this.mainLanguage = langNode.getTextContent(); 
                        }
                        if(!node.getTextContent().trim().equals("")){
                            this.collectionDataUrl = node.getTextContent();
                            //flag auf false setzen durch serialisierung in xml
                            n.setTextContent("false");              
                            FileOutputStream os = new FileOutputStream(configFile);
                            XMLSerializer ser = new XMLSerializer(os, null);
                            ser.serialize(document);
                            
                        }
                    }
                }
            }
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     *  der extractor holt alle projekt zugeh�rigen Urls 
     */
    private void extractUrlsFromCollections(){
        System.out.println("collecting urls of resources that need update");
        PathExtractor extractor = new PathExtractor();
        this.collectionUrls = extractor.initExtractor(this.collectionDataUrl);
        System.out.println("number of urls to update : "+collectionUrls.size());
        System.out.println("ressource location example: "+collectionUrls.get(0));
    }
   
    public ConfManagerResultWrapper getCollectionInfos() throws XPathExpressionException{
      ConfManagerResultWrapper cmrw = new ConfManagerResultWrapper(); 
      checkForChangesInConfigurations();
      extractUrlsFromCollections();
      cmrw.setCollectionUrls(collectionUrls);
      cmrw.setCollectionId(this.collectionId);
      cmrw.setMainLanguage(this.mainLanguage);
      cmrw.setCollectionDataUrl(this.collectionDataUrl);
      cmrw.setFields(this.fields);
      return cmrw;
    }
    
//  public void backup(){
//      //backups the index every time it's updated and deletes, if necessary, the oldest one 
//      IndexBackupManager backuper = IndexBackupManager.getInstace();
//      backuper.backupindex();
//      if(backuper.checkBackupCount() > 9){
//          String fileName = backuper.getLeastModified();
//          System.out.println("leastmodified : "+fileName);
//          boolean delete = backuper.deleteDirectory(new File(WspProperties.INDEX_BACKUP + "/" + fileName));
//          System.out.println("delete? : "+delete);
//      }
//  }
    
}

