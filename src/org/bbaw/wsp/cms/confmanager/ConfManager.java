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
    private String projectUrl;
    private List<String> ressourceLocations; 

    public ConfManager(){
    }
    
    /**
     * checks if an update of a project is necessary by checking configuration file
     */
    public void checkForChanges(){
        System.out.println("---------------");
        System.out.println("checking configuration files...");
        
        //holt alle konfiguratiuonsdateien aus dem konf-Ordner
        PathExtractor ext = new PathExtractor();
        List<String> configsList = ext.extractPathLocally(Constants.getInstance().getConfDir());
        System.out.println("Anzahl der konfugirationsdateien : "+configsList.size());
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            File configFile = null;
            //überprüft alle konf-dateien auf update und führt es bei bedarf aus
            for (String configXml : configsList) {
                System.out.println("checking : "+configXml);
                builder = docFactory.newDocumentBuilder();
                configFile = new File(configXml); 
                Document document = builder.parse(configFile);

                NodeList nl = document.getElementsByTagName("update");

                if(nl.getLength() == 1){
                    this.updateUrls = new ArrayList<String>();
                    org.w3c.dom.Node n = nl.item(0);  
                    String updateValue = n.getTextContent();
                    if(updateValue.trim().equals("true")){
                        System.out.println("update tag is set on : "+updateValue);
                        NodeList nodeli = document.getElementsByTagName("projectDataUrl");
//                        TODO
                        //NodeList nodeli = document.getElementsByTagName("projectId");
                        
                        NodeList nodelis = document.getElementsByTagName("name");
                        //darf jeweils nur ein node enthalten sein
                        Node node = nodeli.item(0);
                        Node nameNode= nodelis.item(0);
                        String projectName = "";
                        if(!nameNode.getTextContent().equals("")){
                            projectName = nameNode.getTextContent(); 
                        }
                        if(!node.getTextContent().trim().equals("")){
                            this.projectUrl = node.getTextContent();
                            getUrlsFromProjects();
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
     *  der extractor holt alle projekt zugehörigen Urls 
     */
    public void getUrlsFromProjects(){
        System.out.println("collecting urls of resources that need update");
        PathExtractor extractor = new PathExtractor();
        this.ressourceLocations = extractor.initExtractor(this.projectUrl);
        System.out.println("number of urls to update : "+ressourceLocations.size());
        System.out.println("ressource location example: "+ressourceLocations.get(0));
    }
    
    
    public List<String> getRessourceLocations() {
        return ressourceLocations;
    }

    public void setOutStream(PrintWriter out) {
        this.out = out;
        System.out.println("setOutStream");
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

