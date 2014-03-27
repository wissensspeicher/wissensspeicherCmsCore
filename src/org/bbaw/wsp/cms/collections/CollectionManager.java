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
import org.bbaw.wsp.cms.document.Person;
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
  private CollectionReader collectionReader;
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
      addDocuments(collection);
    }
  }

  /**
   * Update of the collection with that collectionId
   * @param collectionId
   * @throws ApplicationException
   */
  public void updateCollection(String collectionId) throws ApplicationException {
    Collection collection = collectionReader.getCollection(collectionId);
    addDocuments(collection);
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
    if (collectionId.equals("edoc")) {
      mdRecords = getMetadataRecordsEdoc(collection);
    } else {
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
      int counter = 0;
      if (xmdValueResources != null && xmdValueResources.size() > 0) {
        while (xmdValueResourcesIterator.hasNext()) {
          counter++;
          XdmItem xdmItemResource = xmdValueResourcesIterator.next();
          String xdmItemResourceStr = xdmItemResource.toString();
          MetadataRecord mdRecord = getMdRecord(xQueryEvaluator, xdmItemResourceStr);  // get the mdRecord out of rdf string
          if (mdRecord != null) {
            mdRecord.setCollectionNames(collectionId);
            mdRecord.setId(counter); // collection wide id
            mdRecord = createMainFieldsMetadataRecord(mdRecord, collection);
            mdRecords.add(mdRecord);
            // if it is an eXist directory then fetch all subUrls/mdRecords of that directory
            if (mdRecord.isEXistDir()) {
              String eXistUrl = mdRecord.getWebUri();
              if (eXistUrl != null) {
                ArrayList<MetadataRecord> mdRecordsEXist = new ArrayList<MetadataRecord>();
                if (eXistUrl.endsWith("/"))
                  eXistUrl = eXistUrl.substring(0, eXistUrl.length() - 1);
                String excludesStr = collection.getExcludesStr();
                List<String> eXistUrls = extractDocumentUrls(eXistUrl, excludesStr);
                for (int i=0; i<eXistUrls.size(); i++) {
                  counter++;
                  String eXistSubUrlStr = eXistUrls.get(i);
                  MetadataRecord mdRecordEXist = getNewMdRecord(eXistSubUrlStr); // with docId and webUri
                  mdRecordEXist.setCollectionNames(collectionId);
                  mdRecord.setId(counter); // collection wide id
                  mdRecordEXist = createMainFieldsMetadataRecord(mdRecordEXist, collection);
                  mdRecordsEXist.add(mdRecordEXist);
                }
                mdRecords.addAll(mdRecordsEXist);
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
  
  private MetadataRecord createMainFieldsMetadataRecord(MetadataRecord mdRecord, Collection collection) throws ApplicationException {
    try {
      Tika tika = new Tika();
      String webUriStr = mdRecord.getWebUri();
      URL uri = new URL(webUriStr);
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
          LOGGER.error("get mime type failed for: " + webUriStr);
          e.printStackTrace();
        }
        String fileExtension = "html";
        if (mimeType != null && mimeType.contains("html"))
          fileExtension = "html";
        else if (mimeType != null && mimeType.contains("pdf"))
          fileExtension = "pdf";
        else if (mimeType != null && mimeType.contains("xml"))
          fileExtension = "xml";
        int recordId = mdRecord.getId();
        String fileName = "indexxx" + recordId + "." + fileExtension;
        if (uriPath.endsWith("/"))
          uriPath = uriPath + fileName;
        else
          uriPath = uriPath + "/" + fileName;
      }
      String collectionId = collection.getId();
      String docId = "/" + collectionId + uriPath;
      String mimeType = getMimeType(docId);
      if (mimeType == null) {  // last chance: try it with tika
        try {
          mimeType = tika.detect(uri);
        } catch (IOException e) {
          LOGGER.error("get mime type failed for: " + webUriStr);
          e.printStackTrace();
        }
      }
      mdRecord.setDocId(docId);
      mdRecord.setUri(webUriStr);
      mdRecord.setType(mimeType);
      // if no language is set then take the mainLanguage of the project
      if (mdRecord.getLanguage() == null) {
        String mainLanguage = collection.getMainLanguage();
        mdRecord.setLanguage(mainLanguage);
      }
      Hashtable<String, XQuery> xQueries = collection.getxQueries();
      mdRecord.setxQueries(xQueries);
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
    return mdRecord;
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
    String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
    String resourceIdUrlStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "string(/rdf:Description/dc:identifier/@rdf:resource)");
    if (resourceIdUrlStr == null || resourceIdUrlStr.trim().isEmpty())
      LOGGER.error("No identifier given in resource: \"" + xmlRdfStr + "\"");
    MetadataRecord mdRecord = getNewMdRecord(resourceIdUrlStr);
    String type = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:type/text()");
    if (type != null && type.equals("eXistDir"))
      mdRecord.setSystem(type);
    String creator = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:creator/text()");
    if (creator != null) {
      creator = StringUtils.resolveXmlEntities(creator.trim());
      mdRecord.setCreator(creator);
      String[] creators = creator.split(";");
      String creatorDetails = "<persons>";
      for (int i=0; i<creators.length; i++) {
        String cStr = creators[i];
        Person c = new Person(cStr);
        creatorDetails = creatorDetails + "\n" + c.toXmlStr();
      }
      creatorDetails = creatorDetails + "\n</persons>";
      mdRecord.setCreatorDetails(creatorDetails);
    }
    String title = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:title/text()");
    if (title != null)
      title = StringUtils.resolveXmlEntities(title.trim());
    mdRecord.setTitle(title);
    String publisher = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:publisher/text()");
    if (publisher != null)
      publisher = StringUtils.resolveXmlEntities(publisher.trim());
    mdRecord.setPublisher(publisher);
    String subject = xQueryEvaluator.evaluateAsStringValueJoined(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:subject", "###");
    if (subject != null)
      subject = StringUtils.resolveXmlEntities(subject.trim());
    mdRecord.setSubject(subject);
    XdmValue xmdValueDcTerms = xQueryEvaluator.evaluate(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dcterms:subject");
    XdmSequenceIterator xmdValueDcTermsIterator = xmdValueDcTerms.iterator();
    if (xmdValueDcTerms != null && xmdValueDcTerms.size() > 0) {
      String subjectControlled = "<subjects>";
      while (xmdValueDcTermsIterator.hasNext()) {
        XdmItem xdmItemDcTerm = xmdValueDcTermsIterator.next();
        String xdmItemDcTermStr = xdmItemDcTerm.toString(); // e.g. <dcterms:subject rdf:type="http://www.w3.org/2004/02/skos/core#Concept" rdf:resource="http://de.dbpedia.org/resource/Kategorie:Karl_Marx"/>
        subjectControlled = subjectControlled + "\n" + xdmItemDcTermStr;
      }
      subjectControlled = subjectControlled + "\n</subjects>";
      mdRecord.setSubjectControlled(subjectControlled);
    }
    String dateStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:date/text()");
    Date date = null;
    if (dateStr != null && ! dateStr.equals("")) {
      dateStr = StringUtils.resolveXmlEntities(dateStr.trim());
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
      abstractt = StringUtils.resolveXmlEntities(abstractt.trim());
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

  private MetadataRecord getNewMdRecord(String urlStr) {
    MetadataRecord mdRecord = new MetadataRecord();
    try {
      URL resourceIdUrl = new URL(urlStr);
      String identifier = resourceIdUrl.getFile();
      identifier = StringUtils.deresolveXmlEntities(identifier.trim());
      if (identifier.startsWith("/"))
        identifier = identifier.substring(1);
      mdRecord.setDocId(identifier);
      if (urlStr != null)
        urlStr = StringUtils.deresolveXmlEntities(urlStr.trim());
      mdRecord.setWebUri(urlStr);
    } catch (MalformedURLException e) {
      LOGGER.error("Malformed Url \"" +  urlStr + "\" in resource");
      return null;
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
