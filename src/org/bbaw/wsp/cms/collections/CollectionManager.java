package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.bbaw.wsp.cms.dochandler.DocumentHandler;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.EdocIndexMetadataFetcherTool;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.general.Constants;
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

  public static void main(String[] args) throws ApplicationException {
    try {
      if (args != null) {
        String operation = args[0];
        String collectionId = args[1];
        CollectionManager collectionManager = getInstance();
        if (operation.equals("update"))
          collectionManager.updateCollection(collectionId);
        else if (operation.equals("delete"))
          collectionManager.deleteCollection(collectionId);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

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

  public ArrayList<MetadataRecord> getMetadataRecordsByRdfFile(Collection collection) throws ApplicationException {
    String collectionId = collection.getId();
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>();
    try {
      String metadataRdfDir = Constants.getInstance().getMdsystemConfDir() + "/resources/";
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
                  mdRecordEXist.setId(counter); // collection wide id
                  mdRecordEXist = createMainFieldsMetadataRecord(mdRecordEXist, collection);
                  mdRecordEXist.setCreator(mdRecord.getCreator());
                  mdRecordEXist.setTitle(mdRecord.getTitle());
                  mdRecordEXist.setPublisher(mdRecord.getPublisher());
                  mdRecordEXist.setDate(mdRecord.getDate());
                  if (mdRecord.getLanguage() != null)
                    mdRecordEXist.setLanguage(mdRecord.getLanguage());
                  mdRecordsEXist.add(mdRecordEXist);
                }
                mdRecords.addAll(mdRecordsEXist);
              }
            } else if(mdRecord.isDirectory()) {
              String fileDirUrlStr = mdRecord.getWebUri();
              if (fileDirUrlStr != null) {
                ArrayList<MetadataRecord> mdRecordsDir = new ArrayList<MetadataRecord>();
                String fileExtensions = "html;htm;pdf;doc";
                if (mdRecord.getType() != null)
                  fileExtensions = mdRecord.getType();
                String fileDirStr = null;
                try {
                  URL fileDirUrl = new URL(fileDirUrlStr);
                  String fileDirUrlHost = fileDirUrl.getHost();
                  String fileDirUrlPath = fileDirUrl.getFile();
                  fileDirStr = Constants.getInstance().getExternalDocumentsDir() + "/" + fileDirUrlHost + fileDirUrlPath;
                } catch (MalformedURLException e) {
                  throw new ApplicationException(e);
                }
                java.util.Collection<File> files = getFiles(fileDirStr, fileExtensions);
                Iterator<File> filesIter = files.iterator();
                while (filesIter.hasNext()) {
                  counter++;
                  File file = filesIter.next();
                  String fileAbsolutePath = file.getAbsolutePath();
                  String fileRelativePath = fileAbsolutePath.replaceFirst(fileDirStr, "");
                  String webUrl = fileDirUrlStr + fileRelativePath;
                  String uri = "file:" + fileAbsolutePath;
                  MetadataRecord mdRecordDirEntry = getNewMdRecord(webUrl);
                  mdRecordDirEntry.setCollectionNames(collectionId);
                  mdRecordDirEntry.setId(counter); // collection wide id
                  mdRecordDirEntry = createMainFieldsMetadataRecord(mdRecordDirEntry, collection);
                  mdRecordDirEntry.setUri(uri);
                  mdRecordsDir.add(mdRecordDirEntry);
                }
                mdRecords.addAll(mdRecordsDir);
              }
            }
            // do not add the eXistDir or directory itself as a record (only the normal resource mdRecord) 
            if (! mdRecord.isEXistDir() && ! mdRecord.isDirectory()) {
              mdRecords.add(mdRecord);
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
      String uriStr = mdRecord.getUri();
      URL uri = new URL(webUriStr);
      if (uriStr != null)
        uri = new URL(uriStr);
      String uriPath = uri.getPath();
      String prefix = "/exist/rest/db";
      if (uriPath.startsWith(prefix)) {
        uriPath = uriPath.substring(prefix.length());
      }
      // no file with extension (such as a.xml or b.pdf) but a directory: then a special default file-name is used in docId
      if (! uriPath.toLowerCase().matches(".*\\.csv$|.*\\.gif$|.*\\.jpg$|.*\\.jpeg$|.*\\.html$|.*\\.htm$|.*\\.log$|.*\\.mp3$|.*\\.pdf$|.*\\.txt$|.*\\.xml$|.*\\.doc$")) {
        String mimeType = null;
        try {
          boolean isDbRecord = mdRecord.isDbRecord();
          if (! isDbRecord) {
            mimeType = tika.detect(uri); // much faster with tika
          }
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
      if (mdRecord.getUri() == null)
        mdRecord.setUri(webUriStr);
      if (mdRecord.getType() == null)
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

  private java.util.Collection<File> getFiles(String fileDirStr, String fileExtensions) throws ApplicationException {
    String[] fileExts = fileExtensions.split(";");
    for (int i=0; i<fileExts.length; i++) {
      String fileExt = fileExts[i];
      fileExts[i] = "." + fileExt;
    }
    SuffixFileFilter suffixFileFilter = new SuffixFileFilter(fileExts);
    File fileDir = new File(fileDirStr);
    java.util.Collection<File> files = FileUtils.listFiles(fileDir, suffixFileFilter, TrueFileFilter.INSTANCE);
    return files;
  }
  
  private String getMimeType(String docId) {
    String mimeType = null;
    FileNameMap fileNameMap = URLConnection.getFileNameMap();  // map with 53 entries such as "application/xml"
    mimeType = fileNameMap.getContentTypeFor(docId);
    return mimeType;
  }

  private MetadataRecord getMdRecord(XQueryEvaluator xQueryEvaluator, String xmlRdfStr) throws ApplicationException {
    String namespaceDeclaration = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; ";
    // download url of resource
    String resourceIdUrlStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "string(/rdf:Description/dc:identifier/@rdf:resource)");
    resourceIdUrlStr = StringUtils.resolveXmlEntities(resourceIdUrlStr);
    if (resourceIdUrlStr == null || resourceIdUrlStr.trim().isEmpty())
      LOGGER.error("No identifier given in resource: \"" + xmlRdfStr + "\"");
    MetadataRecord mdRecord = getNewMdRecord(resourceIdUrlStr);
    // web url of resource
    String resourceIdentifierStr = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:identifier/text()");
    if (resourceIdentifierStr != null && ! resourceIdentifierStr.isEmpty()) {
      resourceIdentifierStr = StringUtils.resolveXmlEntities(resourceIdentifierStr);
      try {
        new URL(resourceIdentifierStr); // test if identifier is a url, if so then set it as the webUri
        mdRecord.setWebUri(resourceIdentifierStr);
      } catch (MalformedURLException e) {
        // nothing
      }
      mdRecord.setUri(resourceIdUrlStr);
    }
    String type = xQueryEvaluator.evaluateAsString(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:type/text()");
    if (type != null)
      mdRecord.setSystem(type);  // e.g. "eXistDir" or "dbRecord" or "directory" or "oai"
    XdmValue xmdValueAuthors = xQueryEvaluator.evaluate(xmlRdfStr, namespaceDeclaration + "/rdf:Description/dc:creator");
    if (xmdValueAuthors != null && xmdValueAuthors.size() > 0) {
      XdmSequenceIterator xmdValueAuthorsIterator = xmdValueAuthors.iterator();
      ArrayList<Person> authors = new ArrayList<Person>();
      while (xmdValueAuthorsIterator.hasNext()) {
        XdmItem xdmItemAuthor = xmdValueAuthorsIterator.next();
        String name = xdmItemAuthor.getStringValue();
        if (name != null) {
          name = name.replaceAll("\n|\\s\\s+", " ").trim();
        }
        if (name != null && ! name.isEmpty()) {
          Person author = new Person(name);
          authors.add(author);
        }
      }
      String creator = "";
      String creatorDetails = "<persons>";
      for (int i=0; i<authors.size(); i++) {
        Person author = authors.get(i);
        creator = creator + author.getName();
        if (authors.size() != 1)
          creator = creator + ". ";
        creatorDetails = creatorDetails + "\n" + author.toXmlStr();
      }
      creator = StringUtils.deresolveXmlEntities(creator.trim());
      creatorDetails = creatorDetails + "\n</persons>";
      if (creator != null && ! creator.isEmpty())
        mdRecord.setCreator(creator);
      if (authors != null && ! authors.isEmpty())
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
      String subjectControlledDetails = "<subjects>";
      while (xmdValueDcTermsIterator.hasNext()) {
        XdmItem xdmItemDcTerm = xmdValueDcTermsIterator.next();
        /* e.g.:
         * <dcterms:subject>
             <rdf:Description rdf:about="http://de.dbpedia.org/resource/Kategorie:Karl_Marx">
               <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#Concept"/>
               <rdfs:label>Karl Marx</rdfs:label>
             </rdf:description>
           </dcterms:subject>
         */
        String xdmItemDcTermStr = xdmItemDcTerm.toString();
        subjectControlledDetails = subjectControlledDetails + "\n" + xdmItemDcTermStr;
      }
      subjectControlledDetails = subjectControlledDetails + "\n</subjects>";
      mdRecord.setSubjectControlledDetails(subjectControlledDetails);
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
  
  public class FileExtensionsFilter implements FileFilter {
    private String[] fileExtensions;
    
    public FileExtensionsFilter(String[] fileExtensions) {
      this.fileExtensions = fileExtensions;
    }
    
    public boolean accept(File file) {
      for (String extension : fileExtensions) {
        if (file.getName().toLowerCase().endsWith(extension)) {
          return true;
        }
      }
      return false;
    }
  }

}
