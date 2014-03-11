package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.bbaw.wsp.cms.dochandler.DocumentHandler;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.EdocIndexMetadataFetcherTool;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class CollectionManager {
  private static Logger LOGGER = Logger.getLogger(CollectionManager.class);
  private static CollectionManager confManager;
  private CollectionReader collectionReader;  // has the collection infos of the configuration files
  private int counter = 0;
  private String metadataRdfDir = "config/mdsystem/resources/";

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
   * Update of all collections
   * @throws ApplicationException
   */
  public void updateCollections() throws ApplicationException {
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      updateCollection(collection);
    }
  }

  /**
   * Update of the collection with that collectionId
   * @param collectionId
   * @throws ApplicationException
   */
  public void updateCollection(String collectionId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(collectionId);
    updateCollection(collection);
  }

  public void deleteCollection(String collectionId) throws ApplicationException {
    DocumentHandler docHandler = new DocumentHandler();
    CmsDocOperation docOp = new CmsDocOperation("deleteCollection", null, null, collectionId);
    try {
      docHandler.doOperation(docOp);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void updateCollection(Collection collection) throws ApplicationException {
    WspUrl[] collectionDataUrls = collection.getDataUrls();
    String excludesStr = collection.getExcludesStr();
    if (collectionDataUrls != null) {
      List<String> collectionDocumentUrls = new ArrayList<String>();
      for (int i=0; i<collectionDataUrls.length; i++) {
        WspUrl wspUrl = collectionDataUrls[i];
        String urlStr = wspUrl.getUrl();
        if (wspUrl.isEXistDir()) {
          if (urlStr.endsWith("/"))
            urlStr = urlStr.substring(0, urlStr.length() - 1);
          List<String> collectionDocumentUrlsTemp = extractDocumentUrls(urlStr, excludesStr);
          collectionDocumentUrls.addAll(collectionDocumentUrlsTemp);
        } else {
          collectionDocumentUrls.add(urlStr);
        }
      }
      collection.setDocumentUrls(collectionDocumentUrls);
    }
    addDocuments(collection); 
  }
  
  private void addDocuments(Collection collection) throws ApplicationException {
    DocumentHandler docHandler = new DocumentHandler();
    ArrayList<MetadataRecord> mdRecords = getMetadataRecords(collection);
    if (mdRecords != null) {
      for (int i=0; i<mdRecords.size(); i++) {
        MetadataRecord mdRecord = mdRecords.get(i);
        String docUrl = mdRecord.getUri();
        String docId = mdRecord.getDocId();
        String collectionId = mdRecord.getCollectionNames();
        counter++;
        String logCreationStr = counter + ". " + "Collection: " + collectionId + ": Create resource: " + docUrl + " (" + docId + ")";
        if (docUrl == null)
          logCreationStr = counter + ". " + "Collection: " + collectionId + ": Create metadata resource: " + docId;
        LOGGER.info(logCreationStr);
        CmsDocOperation docOp = new CmsDocOperation("create", docUrl, null, docId);
        docOp.setMdRecord(mdRecord);
        ArrayList<String> fields = collection.getFields();
        if (fields != null && ! fields.isEmpty()) {
          String[] fieldsArray = new String[fields.size()];
          for (int j=0;j<fields.size();j++) {
            String f = fields.get(j);
            fieldsArray[j] = f;
          }
          docOp.setElementNames(fieldsArray);
        }
        String mainLanguage = collection.getMainLanguage();
        docOp.setMainLanguage(mainLanguage);
        try {
          docHandler.doOperation(docOp);
        } catch (Exception e) {
          e.printStackTrace();
        }
        // without that, there would be a memory leak (with many big documents in one collection)
        // with that the main big fields (content etc.) could be garbaged
        mdRecord.setAllNull();
      }
    }
  }

  private ArrayList<MetadataRecord> getMetadataRecords(Collection collection) throws ApplicationException {
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    String collectionId = collection.getId();
    List<String> documentUrls = collection.getDocumentUrls();
    if (documentUrls != null) {
      // TODO über RdfFiles einlesen
      mdRecords = getMetadataRecordsByDocUrls(collection);
    }
    if (collectionId.equals("edoc")) {
      mdRecords = getMetadataRecordsEdoc(collection);
    } else if (collectionId.equals("avhseklit") || collectionId.equals("avhunselbst")) {  // TODO  alle Ressourcen so einlesen
      mdRecords = getMetadataRecordsByRdfFile(collection);
    }
    if (mdRecords.size() == 0)
      return null;
    else 
      return mdRecords;
  }

  private ArrayList<MetadataRecord> getMetadataRecordsByRdfFile(Collection collection) throws ApplicationException {
    String collectionId = collection.getId();
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    try {
      File rdfRessourcesFile = new File(metadataRdfDir + collectionId + ".rdf");
      URL rdfRessourcesFileUrl = rdfRessourcesFile.toURI().toURL();
      String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
      XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
      XdmValue xmdValueResources = xQueryEvaluator.evaluate(rdfRessourcesFileUrl, namespaceDeclaration + "/rdf:RDF/rdf:Description");
      XdmSequenceIterator xmdValueResourcesIterator = xmdValueResources.iterator();
      if (xmdValueResources != null && xmdValueResources.size() > 0) {
        while (xmdValueResourcesIterator.hasNext()) {
          XdmItem xdmItemResource = xmdValueResourcesIterator.next();
          String xdmItemResourceStr = xdmItemResource.toString();
          MetadataRecord mdRecord = getMdRecord(xQueryEvaluator, xdmItemResourceStr);
          if (mdRecord != null) {
            String identifier = mdRecord.getDocId();
            String docId = "/" + collectionId + "/" + identifier;
            mdRecord.setDocId(docId);
            mdRecord.setCollectionNames(collectionId);
            if (mdRecord.getLanguage() == null) {
              String mainLanguage = collection.getMainLanguage();
              mdRecord.setLanguage(mainLanguage);
            }
            mdRecords.add(mdRecord);
            // if it is an eXist directory then fetch all documents of that directory
            if (mdRecord.isEXistDir()) {
              String eXistUrl = mdRecord.getWebUri();
              if (eXistUrl != null) {
                /* TODO auskommentieren wenn fertig
                ArrayList<MetadataRecord> mdRecordsEXist = new ArrayList<MetadataRecord>();
                if (eXistUrl.endsWith("/"))
                  eXistUrl = eXistUrl.substring(0, eXistUrl.length() - 1);
                String excludesStr = collection.getExcludesStr();
                List<String> eXistUrls = extractDocumentUrls(eXistUrl, excludesStr);
                for (int i=0; i<eXistUrls.size(); i++) {
                  MetadataRecord mdRecordEXist = new MetadataRecord();
                  // TODO alle Felder aus mdRecord holen und setzen
                  // TODO neue docId setzen
                  mdRecordsEXist.add(mdRecordEXist);
                }
                mdRecords.addAll(mdRecordsEXist);
                */
              }
            }
          } 
        }
      }
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecords;
  }
  
  private ArrayList<MetadataRecord> getMetadataRecordsByDocUrls(Collection collection) throws ApplicationException {
    String collectionId = collection.getId();
    List<String> documentUrls = collection.getDocumentUrls();
    Hashtable<String, XQuery> xQueries = collection.getxQueries();
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    try {
      Tika tika = new Tika();
      for (int i=0; i<documentUrls.size(); i++) {
        MetadataRecord mdRecord = new MetadataRecord();
        String docUrl = documentUrls.get(i);
        URL uri = new URL(docUrl);
        String uriPath = uri.getPath();
        String prefix = "/exist/rest/db";
        if (uriPath.startsWith(prefix)) {
          uriPath = uriPath.substring(prefix.length());
        }
        // no file with extension (such as a.xml or b.pdf) but a directory: then a special default file-name is used in docId
        if (! uriPath.toLowerCase().matches(".*\\.csv$|.*\\.gif$|.*\\.jpg$|.*\\.jpeg$|.*\\.html$|.*\\.htm$|.*\\.log$|.*\\.mp3$|.*\\.pdf$|.*\\.txt$|.*\\.xml$|.*\\.doc$")) {
          String mimeType = null;
          try {
            mimeType = tika.detect(uri); // much faster with tika
            /* old code which is slow, when the files behind the url's are large 
            HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            mimeType = connection.getContentType();
            */
          } catch (IOException e) {
            LOGGER.error("get mime type failed for: " + docUrl);
            e.printStackTrace();
          }
          String fileExtension = "html";
          if (mimeType != null && mimeType.contains("html"))
            fileExtension = "html";
          else if (mimeType != null && mimeType.contains("pdf"))
            fileExtension = "pdf";
          else if (mimeType != null && mimeType.contains("xml"))
            fileExtension = "xml";
          int pos = i + 1;
          String fileName = "indexxx" + pos + "." + fileExtension;
          if (uriPath.endsWith("/"))
            uriPath = uriPath + fileName;
          else
            uriPath = uriPath + "/" + fileName;
        }
        String docId = "/" + collectionId + uriPath;
        String mimeType = getMimeType(docId);
        if (mimeType == null) {  // last chance: try it with tika
          try {
            mimeType = tika.detect(uri);
          } catch (IOException e) {
            LOGGER.error("get mime type failed for: " + docUrl);
            e.printStackTrace();
          }
        }
        mdRecord.setType(mimeType);
        mdRecord.setDocId(docId);
        mdRecord.setUri(docUrl);
        // if mimeType is not xml then the docUrl is also the webUri
        if (mimeType != null && ! mimeType.contains("xml"))
          mdRecord.setWebUri(docUrl);
        mdRecord.setCollectionNames(collectionId);
        mdRecord.setxQueries(xQueries);
        mdRecords.add(mdRecord);
      }
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecords;
  }
  
  
  private ArrayList<MetadataRecord> getMetadataRecordsEdoc(Collection collection) throws ApplicationException {
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    String metadataRedundantUrlPrefix = collection.getMetadataRedundantUrlPrefix();
    String metadataUrlPrefix = collection.getMetadataUrlPrefix();
    String[] metadataUrls = collection.getMetadataUrls();
    for (int i=0; i<metadataUrls.length; i++) {
      String metadataUrl = metadataUrls[i];
      MetadataRecord mdRecord = new MetadataRecord();
      String docId = null;
      String uri = null;
      String webUri = null;
      EdocIndexMetadataFetcherTool.fetchHtmlDirectly(metadataUrl, mdRecord);
      if (mdRecord.getLanguage() != null) {
        String isoLang = Language.getInstance().getISO639Code(mdRecord.getLanguage());
        mdRecord.setLanguage(isoLang);
      }
      String httpEdocUrl = mdRecord.getRealDocUrl();
      String uriEdoc = mdRecord.getUri();  // e.g.: http://edoc.bbaw.de/volltexte/2009/1070/
      if (httpEdocUrl != null) {             
        String docIdTmp = httpEdocUrl.replaceAll(metadataUrlPrefix, "");
        docId = "/edoc" + docIdTmp;
        String fileEdocUrl = "file:" + metadataRedundantUrlPrefix + docIdTmp;
        uri = fileEdocUrl;
        if (uriEdoc == null) {
          String edocId = docIdTmp.replaceAll("pdf/.*$", "");
          webUri = metadataUrlPrefix + edocId;
        } else { 
          webUri = uriEdoc;
        }
      } else {
        LOGGER.error("Fetching metadata failed for: " + metadataUrl + " (no url in index.html found)");
      }
      if (docId != null && uri != null) {
        mdRecord.setDocId(docId);
        mdRecord.setUri(uri);
        mdRecord.setWebUri(webUri);
        if (mdRecord.getCollectionNames() == null)
          mdRecord.setCollectionNames("edoc");
        if (mdRecord.getLanguage() == null) {
          String mainLanguage = collection.getMainLanguage();
          mdRecord.setLanguage(mainLanguage);
        }
        mdRecord.setSchemaName(null);
        mdRecords.add(mdRecord);
      }
    }
    return mdRecords;
  }

  private List<String> extractDocumentUrls(String collectionDataUrl, String excludesStr) {
    List<String> documentUrls = null;
    if (! collectionDataUrl.equals("")){
      PathExtractor extractor = new PathExtractor();
      documentUrls = extractor.initExtractor(collectionDataUrl, excludesStr);
    }
    return documentUrls;
  }

  private String getMimeType(String docId) {
    String mimeType = null;
    FileNameMap fileNameMap = URLConnection.getFileNameMap();  // map with 53 entries such as "application/xml"
    mimeType = fileNameMap.getContentTypeFor(docId);
    return mimeType;
  }

  private MetadataRecord getMdRecord(XQueryEvaluator xQueryEvaluator, String xmlRdfStr) throws ApplicationException {
    MetadataRecord mdRecord = new MetadataRecord();
    String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
    String resourceIdUrlStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "string(/rdf:Description/dc:identifier/@rdf:resource)");
    if (resourceIdUrlStr == null || resourceIdUrlStr.trim().isEmpty())
      LOGGER.error("No identifier given in resource: \"" + xmlRdfStr + "\"");
    String identifier = null;
    try {
      URL resourceIdUrl = new URL(resourceIdUrlStr);
      identifier = resourceIdUrl.getFile();
      identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      if (identifier.startsWith("/"))
        identifier = identifier.substring(1);
      mdRecord.setDocId(identifier);
      if (resourceIdUrlStr != null)
        resourceIdUrlStr = StringUtils.deresolveXmlEntities(resourceIdUrlStr.trim());
      mdRecord.setWebUri(resourceIdUrlStr);
    } catch (MalformedURLException e) {
      LOGGER.error("Malformed URL \"" +  resourceIdUrlStr + "\" in resource: \n\"" + xmlRdfStr + "\"");
      return null;
    }
    String type = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:type/text()");
    if (type != null && type.equals("eXistDir"))
      mdRecord.setSystem(type);
    String creator = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:creator/text()");
    if (creator != null)
      creator = StringUtils.deresolveXmlEntities(creator.trim());
    mdRecord.setCreator(creator);
    String title = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:title/text()");
    if (title != null)
      title = StringUtils.deresolveXmlEntities(title.trim());
    mdRecord.setTitle(title);
    String publisher = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:publisher/text()");
    if (publisher != null)
      publisher = StringUtils.deresolveXmlEntities(publisher.trim());
    mdRecord.setPublisher(publisher);
    String subject = xQueryEvaluator.evaluateAsStringValueJoined(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:subject", "###");
    if (subject != null)
      subject = StringUtils.deresolveXmlEntities(subject.trim());
    mdRecord.setSubject(subject);
    String dateStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:date/text()");
    Date date = null;
    if (dateStr != null && ! dateStr.equals("")) {
      dateStr = StringUtils.deresolveXmlEntities(dateStr.trim());
      dateStr = new Util().toYearStr(dateStr);  // test if possible etc
      if (dateStr != null) {
        try {
          date = new Util().toDate(dateStr + "-01-01T00:00:00.000Z");
        } catch (Exception e) {
          // nothing
        }
      }
    }
    mdRecord.setDate(date);
    String language = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:language/text()");
    mdRecord.setLanguage(language);
    String mimeType = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:format/text()");
    mdRecord.setType(mimeType);
    String abstractt = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:abstract/text()");
    if (abstractt != null)
      abstractt = StringUtils.deresolveXmlEntities(abstractt.trim());
    mdRecord.setDescription(abstractt);
    String pagesStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:extent/text()");
    if (pagesStr != null && ! pagesStr.isEmpty()) {
      try {
        int pages = Integer.parseInt(pagesStr);
        mdRecord.setPageCount(pages);
      } catch (NumberFormatException e) {
        // nothing
      }
    }
    return mdRecord;    
  }
  
  /*
   * wird nicht mehr verwendet: nur noch als Demo
   */
  private void setUpdate(File configFile, boolean update) throws ApplicationException {
    try {
      // flag im Konfigurations-File auf false setzen durch Serialisierung in das File
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = docFactory.newDocumentBuilder();
      Document configFileDocument = builder.parse(configFile);
      NodeList updateNodeList = configFileDocument.getElementsByTagName("update");
      Node n = updateNodeList.item(0);
      if (n != null) {
        if (update)
          n.setTextContent("true");
        else 
          n.setTextContent("false");
        FileOutputStream os = new FileOutputStream(configFile);
        XMLSerializer ser = new XMLSerializer(os, null);
        ser.serialize(configFileDocument);  //  Vorsicht: wenn es auf true ist: es wird alles neu indexiert
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
}
