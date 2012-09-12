package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.bbaw.wsp.cms.dochandler.DocumentHandler;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class CollectionManager {
  private CollectionReader collectionReader;  // has the collection infos of the configuration files
  private int counter = 0;
  private static CollectionManager confManager;

  public static CollectionManager getInstance() throws ApplicationException {
    if(confManager == null) {
      confManager = new CollectionManager();
      confManager.init();
    }
    return confManager;
  }
  
  private void init() throws ApplicationException {
    collectionReader = CollectionReader.getInstance();
  }
  
  /**
   * Update of all collections (if update parameter in configuration is true)
   */
  public void updateCollections() throws ApplicationException {
    updateCollections(false);
  }
  
  /**
   * Update of all collections
   * @param forceUpdate if true then update is always done (also if update parameter in configuration is false)
   * @throws ApplicationException
   */
  public void updateCollections(boolean forceUpdate) throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      updateCollection(collection, forceUpdate);
    }
  }

  /**
   * Update of the collection with that collectionId (if update parameter in configuration is true)
   * @param collectionId
   * @throws ApplicationException
   */
  public void updateCollection(String collectionId) throws ApplicationException {
    updateCollection(collectionId, false);
  }
  
  /**
   * Update of the collection with that collectionId
   * @param collectionId
   * @param forceUpdate if true then update is always done (also if update parameter in configuration is false)
   * @throws ApplicationException
   */
  public void updateCollection(String collectionId, boolean forceUpdate) throws ApplicationException {
    Collection collection = collectionReader.getCollection(collectionId);
    updateCollection(collection, forceUpdate);
  }

  private void updateCollection(Collection collection, boolean forceUpdate) throws ApplicationException {
    boolean isUpdateNecessary = collection.isUpdateNecessary();
    if (isUpdateNecessary || forceUpdate) {
      String collectionDataUrl = collection.getDataUrl();
      String excludesStr = collection.getExcludesStr();
      List<String> collectionDocumentUrls = null;
      if (collectionDataUrl.endsWith("/")) {
        collectionDocumentUrls = extractDocumentUrls(collectionDataUrl, excludesStr);
        collection.setDocumentUrls(collectionDocumentUrls);
      } else {
        collectionDocumentUrls = new ArrayList<String>();
        collectionDocumentUrls.add(collectionDataUrl);
        collection.setDocumentUrls(collectionDocumentUrls);
      }
      addDocuments(collection); 
      String configFileName = collection.getConfigFileName();
      File configFile = new File(configFileName);
      setUpdate(configFile, false);
    }
  }
  
  private void addDocuments(Collection collection) throws ApplicationException {
    DocumentHandler docHandler = new DocumentHandler();
    try {
      List<String> documentUrls = collection.getDocumentUrls();
      for (int i=0; i<documentUrls.size(); i++) {
        URL uri = new URL(documentUrls.get(i));
        String uriPath = uri.getPath();
        String prefix = "/exist/rest/db";
        if(uriPath.startsWith(prefix)){
          uriPath = uriPath.substring(prefix.length());
        }
        String docUrl = documentUrls.get(i);
        String docId = "/" + collection.getId() + uriPath;
        counter++;
        Date now = new Date();
        System.out.println(counter + ". " + now.toString() + " Collection: " + collection.getId() + ": Create: " + docId);
        CmsDocOperation docOp = new CmsDocOperation("create", docUrl, null, docId);
        String collectionId = collection.getId();
        docOp.setCollectionNames(collectionId);
        ArrayList<String> fields = collection.getFields();
        String[] fieldsArray = new String[fields.size()];
        for (int j=0;j<fields.size();j++) {
          String f = fields.get(j);
          fieldsArray[j] = f;
        }
        docOp.setElementNames(fieldsArray);
        String mainLanguage = collection.getMainLanguage();
        docOp.setMainLanguage(mainLanguage);
        try {
          docHandler.doOperation(docOp);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
  }

  private void setUpdate(File configFile, boolean update) throws ApplicationException {
    try {
      // flag im Konfigurations-File auf false setzen durch Serialisierung in das File
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = docFactory.newDocumentBuilder();
      Document configFileDocument = builder.parse(configFile);
      NodeList updateNodeList = configFileDocument.getElementsByTagName("update");
      Node n = updateNodeList.item(0);
      if (update)
        n.setTextContent("true");
      else 
        n.setTextContent("false");
      FileOutputStream os = new FileOutputStream(configFile);
      XMLSerializer ser = new XMLSerializer(os, null);
      ser.serialize(configFileDocument);  //  Vorsicht: wenn es auf true ist: es wird alles neu indexiert
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  private List<String> extractDocumentUrls(String collectionDataUrl, String excludesStr) {
    List<String> documentUrls = null;
    if (! collectionDataUrl.equals("")){
      PathExtractor extractor = new PathExtractor();
      documentUrls = extractor.initExtractor(collectionDataUrl, excludesStr);
    }
    return documentUrls;
  }

}
