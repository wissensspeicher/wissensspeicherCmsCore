package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.XQuery;
import org.bbaw.wsp.cms.general.Constants;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.Hit;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.Hits;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class CollectionReader {
  private static Logger LOGGER = Logger.getLogger(CollectionReader.class);
  private static CollectionReader collectionReader; 
  private static String NAMESPACE_DECLARATION = "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"; declare namespace dc=\"http://purl.org/dc/elements/1.1/\"; declare namespace dcterms=\"http://purl.org/dc/terms/\"; declare namespace foaf=\"http://xmlns.com/foaf/0.1/\"; ";
  private XQueryEvaluator xQueryEvaluator;
  private URL inputNormdataFileUrl;
	private HashMap<String, Collection> collectionContainer;  // key is collectionId string and value is collection
  private HashMap<String, WspUrl> wspUrls;  // key is wspUrl string and value is wspUrl 
	
	private CollectionReader() throws ApplicationException {
		collectionContainer = new HashMap<String, Collection>();
		wspUrls = new HashMap<String, WspUrl>();
		init();
		readConfFiles();
	}

	public static CollectionReader getInstance() throws ApplicationException {
		if (collectionReader == null)
			collectionReader = new CollectionReader();
		return collectionReader;
	}

	private void init() throws ApplicationException {
    try {
  	  xQueryEvaluator = new XQueryEvaluator();
      String inputNormdataFileName = Constants.getInstance().getMdsystemNormdataFile();
      File inputNormdataFile = new File(inputNormdataFileName);
      inputNormdataFileUrl = inputNormdataFile.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new ApplicationException(e);
    }
	}
	
	public ArrayList<Collection> getCollections() {
		ArrayList<Collection> collections = null;
		if (collectionContainer != null) {
			collections = new ArrayList<Collection>();
			java.util.Collection<Collection> values = collectionContainer.values();
			for (Collection collection : values) {
				collections.add(collection);
			}
	    Comparator<Collection> collectionComparator = new Comparator<Collection>() {
	      public int compare(Collection c1, Collection c2) {
	        return c1.getId().compareTo(c2.getId());
	      }
	    };
	    Collections.sort(collections, collectionComparator);
		}
		return collections;
	}

	public Collection getCollection(String collectionId) {
		Collection collection = collectionContainer.get(collectionId);
		return collection;
	}

	private void readConfFiles() throws ApplicationException {
		try {
			// liest alle Konfigurationsdateien ein
			PathExtractor pathExtractor = new PathExtractor();
			String confDir = Constants.getInstance().getCollectionConfDir();
      LOGGER.info("Reading all config files from \"" + confDir + "\"");
			List<String> configsFileList = pathExtractor.extractPathLocally(confDir);
			for (String configFileName:configsFileList) {
				String name = "";
				try {
					File configFile = new File(configFileName);
					name = configFile.getName();
					URL configFileUrl = configFile.toURI().toURL();
					Collection collection = new Collection();
					collection.setConfigFileName(configFileName);
					String collectionId = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/id/text()");
          if (collectionId == null || collectionId.trim().isEmpty()) {
            LOGGER.error("No collectionId found in: " + collection.getConfigFileName());
          } else {
						collection.setId(collectionId);
					} 
          String rdfId = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/rdfId/text()");
          if (rdfId == null || rdfId.trim().isEmpty()) {
            LOGGER.error("No rdfId found in: " + collection.getConfigFileName());
          } else {
            collection.setRdfId(rdfId);
          }
          String dbName = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/db/name/text()");
          if (dbName != null) {
            Database database = new Database();
            database.setName(dbName);
            String xmlDumpFileName = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/db/xmlDump/fileName/text()");
            if (xmlDumpFileName != null)
              database.setXmlDumpFileName(xmlDumpFileName);
            String mainResourcesTable = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/db/mainResourcesTable/name/text()");
            if (mainResourcesTable != null)
              database.setMainResourcesTable(mainResourcesTable);
            String mainResourcesTableId = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/db/mainResourcesTable/idField/text()");
            if (mainResourcesTableId != null)
              database.setMainResourcesTableId(mainResourcesTableId);
            XdmValue mainResourcesTableFields = xQueryEvaluator.evaluate(configFileUrl, "/wsp/collection/db/mainResourcesTable/field");
            XdmSequenceIterator mainResourcesTableFieldsIterator = mainResourcesTableFields.iterator();
            if (mainResourcesTableFields != null && mainResourcesTableFields.size() > 0) {
              while (mainResourcesTableFieldsIterator.hasNext()) {
                XdmItem mainResourcesTableField = mainResourcesTableFieldsIterator.next();
                String mainResourcesTableFieldStr = mainResourcesTableField.toString(); // e.g. <field><dc>title</dc><db>title</db></field>
                String dcField = xQueryEvaluator.evaluateAsString(mainResourcesTableFieldStr, "/field/dc/text()");
                String dbField = xQueryEvaluator.evaluateAsString(mainResourcesTableFieldStr, "/field/db/text()");
                if (dcField != null && dbField != null)
                  database.addField(dcField, dbField);
              }
            }
            String webIdPreStr = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/db/webId/preStr/text()");
            if (webIdPreStr != null)
              database.setWebIdPreStr(webIdPreStr);
            String webIdAfterStr = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/db/webId/afterStr/text()");
            if (webIdAfterStr != null)
              database.setWebIdAfterStr(webIdAfterStr);
            collection.setDatabase(database);
          }
          String projectRdfStr = xQueryEvaluator.evaluateAsString(inputNormdataFileUrl, NAMESPACE_DECLARATION + "/rdf:RDF/*:Description[@*:about='" + rdfId + "']");
          if (projectRdfStr == null || projectRdfStr.trim().isEmpty()) {
            LOGGER.error("Project: \"" + rdfId + "\" in: \"" + configFileUrl + "\" does not exist in: \"" + inputNormdataFileUrl + "\": Please insert this project");
          }
          String projectName = xQueryEvaluator.evaluateAsString(projectRdfStr, NAMESPACE_DECLARATION + "/rdf:Description/foaf:name/text()");
					if (projectName != null) {
						collection.setName(projectName);
					} else {
            LOGGER.error("Project: \"" + rdfId + "\" in: \"" + inputNormdataFileUrl + "\" has no name: Please provide a name for this project");
					}
          String mainLanguage = xQueryEvaluator.evaluateAsString(projectRdfStr, NAMESPACE_DECLARATION + "/rdf:Description/dc:language/text()");
          if (mainLanguage != null) {
            collection.setMainLanguage(mainLanguage);
          } else {
            LOGGER.error("Project: \"" + rdfId + "\" in: \"" + inputNormdataFileUrl + "\" has no language: Please provide a main language for this project");
          }
					// wsp/collection/metadata fields are only used in edoc-Collection
          String metadataUrlStr = xQueryEvaluator.evaluateAsStringValueJoined(configFileUrl, "/wsp/collection/metadata/url");
					if (metadataUrlStr != null) {
						String[] metadataUrls = metadataUrlStr.split(" ");
						collection.setMetadataUrls(metadataUrls);
					}
					String metadataUrlPrefix = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/metadata/urlPrefix/text()");
					if (metadataUrlPrefix != null) {
						collection.setMetadataUrlPrefix(metadataUrlPrefix);
					}
          String collectionMetadataRedundantUrlPrefix = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/metadata/redundantUrlPrefix/text()");
          if (collectionMetadataRedundantUrlPrefix != null) {
            collection.setMetadataRedundantUrlPrefix(collectionMetadataRedundantUrlPrefix);
          }
					XdmValue xmdValueDataUrls = xQueryEvaluator.evaluate(configFileUrl, "/wsp/collection/url/dataUrl");
					XdmSequenceIterator xmdValueDataUrlsIterator = xmdValueDataUrls.iterator();
					if (xmdValueDataUrls != null && xmdValueDataUrls.size() > 0) {
						ArrayList<WspUrl> collectionWspUrls = new ArrayList<WspUrl>();
						while (xmdValueDataUrlsIterator.hasNext()) {
							XdmItem xdmItemDataUrl = xmdValueDataUrlsIterator.next();
							String xdmItemDataUrlStr = xdmItemDataUrl.toString(); // e.g. <dataUrl>http://bla.de/bla.xml</dataUrl>
							String dataUrlType = xQueryEvaluator.evaluateAsString(xdmItemDataUrlStr, "string(/dataUrl/@type)");
							String dataUrl = xdmItemDataUrl.getStringValue();
							if (dataUrl != null && ! dataUrl.isEmpty()) {
                WspUrl urlKey = wspUrls.get(dataUrl);
                if (urlKey == null) {
    							WspUrl newWspUrl = new WspUrl(dataUrl);
    							newWspUrl.setCollectionId(collectionId);
    							if (dataUrlType != null && !dataUrlType.isEmpty())
    							  newWspUrl.setType(dataUrlType);
    							collectionWspUrls.add(newWspUrl);
    							wspUrls.put(dataUrl, newWspUrl);
                } else {
                  // dataUrl is a doublet
                  String wspKeyCollectionId = urlKey.getCollectionId();
                  String errorDoStr = "please remove it either in configuration \"" + wspKeyCollectionId + "\" or \"" + collectionId + "\"";
                  if (collectionId.equals(wspKeyCollectionId))
                    errorDoStr = "please remove it in configuration \"" + collectionId + "\"";
                  LOGGER.error("Doublet: url \"" + dataUrl + "\" is not added: " + errorDoStr);
                }
							}
						}
						WspUrl[] collectionWspUrlsArray = collectionWspUrls.toArray(new WspUrl[collectionWspUrls.size()]);
						collection.setDataUrls(collectionWspUrlsArray);
					}
          String projectHomepage = xQueryEvaluator.evaluateAsString(projectRdfStr, NAMESPACE_DECLARATION + "string(/rdf:Description/foaf:homepage/@rdf:resource)");
          if (projectHomepage != null) {
            collection.setWebBaseUrl(projectHomepage);
          } else {
            LOGGER.error("Project: \"" + rdfId + "\" in: \"" + inputNormdataFileUrl + "\" has no homepage: Please provide a homepage for this project");
          }
					String fieldsStr = xQueryEvaluator.evaluateAsStringValueJoined(configFileUrl, "/wsp/collection/fields/field", "###");
					ArrayList<String> fieldsArrayList = new ArrayList<String>();
					if (fieldsStr != null) {
						String[] fields = fieldsStr.split("###");
						for (int i = 0; i < fields.length; i++) {
							String field = fields[i].trim();
							if (!field.isEmpty())
								fieldsArrayList.add(field);
						}
					}
					collection.setFields(fieldsArrayList);
					Hits xQueries = (Hits) xQueryEvaluator.evaluate(configFileUrl, "/wsp/collection/xqueries/xquery", 0, 9, "hits");
					if (xQueries != null) {
						Hashtable<String, XQuery> xqueriesHashtable = new Hashtable<String, XQuery>();
						for (int i = 0; i < xQueries.getSize(); i++) {
							Hit xqueryHit = xQueries.getHits().get(i);
							String xqueryStr = xqueryHit.getContent();
							String xQueryName = xQueryEvaluator.evaluateAsStringValueJoined(xqueryStr, "xquery/name");
							String xQueryCode = xQueryEvaluator.evaluateAsStringValueJoined(xqueryStr, "xquery/code");
              String xQueryPreStr = xQueryEvaluator.evaluateAsStringValueJoined(xqueryStr, "xquery/preStr");
              String xQueryAfterStr = xQueryEvaluator.evaluateAsStringValueJoined(xqueryStr, "xquery/afterStr");
							if (xQueryName != null && xQueryCode != null) {
								XQuery xQuery = new XQuery(xQueryName, xQueryCode);
								if (xQueryPreStr != null && ! xQueryPreStr.isEmpty())
								  xQuery.setPreStr(xQueryPreStr);
                if (xQueryAfterStr != null && ! xQueryAfterStr.isEmpty())
                  xQuery.setAfterStr(xQueryAfterStr);
								xqueriesHashtable.put(xQueryName, xQuery);
							}
						}
						collection.setxQueries(xqueriesHashtable);
					}
          Hits services = (Hits) xQueryEvaluator.evaluate(configFileUrl, "/wsp/collection/services/*", 0, 9, "hits");
          if (services != null) {
            Hashtable<String, Service> servicesHashtable = new Hashtable<String, Service>();
            for (int i = 0; i < services.getSize(); i++) {
              Hit serviceHit = services.getHits().get(i);
              String serviceStr = serviceHit.getContent();
              String serviceId = xQueryEvaluator.evaluateAsString(serviceStr, "service/id/text()");
              String serviceHost = xQueryEvaluator.evaluateAsString(serviceStr, "service/host/text()");
              String serviceName = xQueryEvaluator.evaluateAsString(serviceStr, "service/name/text()");
              Hits serviceProperties = (Hits) xQueryEvaluator.evaluate(serviceStr, "service/properties/property", 0, 9, "hits");
              Hashtable<String, String> servicePropertiesHashtable = null;
              if (serviceProperties != null) {
                servicePropertiesHashtable = new Hashtable<String, String>();
                for (int j = 0; j < serviceProperties.getSize(); j++) {
                  Hit propertyHit = serviceProperties.getHits().get(j);
                  String propertyStr = propertyHit.getContent();
                  String paramName = xQueryEvaluator.evaluateAsString(propertyStr, "property/name/text()");
                  String paramValue = xQueryEvaluator.evaluateAsString(propertyStr, "property/value/text()");
                  servicePropertiesHashtable.put(paramName, paramValue);
                }
              }
              Hits serviceParameters = (Hits) xQueryEvaluator.evaluate(serviceStr, "service/parameters/param", 0, 9, "hits");
              Hashtable<String, String> serviceParametersHashtable = null;
              if (serviceParameters != null) {
                serviceParametersHashtable = new Hashtable<String, String>();
                for (int j = 0; j < serviceParameters.getSize(); j++) {
                  Hit paramHit = serviceParameters.getHits().get(j);
                  String paramStr = paramHit.getContent();
                  String paramName = xQueryEvaluator.evaluateAsString(paramStr, "param/name/text()");
                  String paramValue = xQueryEvaluator.evaluateAsString(paramStr, "param/value/text()");
                  serviceParametersHashtable.put(paramName, paramValue);
                }
              }
              Service service = new Service();
              service.setId(serviceId);
              service.setHostName(serviceHost);
              service.setName(serviceName);
              service.setProperties(servicePropertiesHashtable);
              service.setParameters(serviceParametersHashtable);
              servicesHashtable.put(serviceId, service);
            }
            collection.setServices(servicesHashtable);
          }
					String excludesStr = xQueryEvaluator.evaluateAsStringValueJoined(configFileUrl, "/wsp/collection/url/exclude");
					if (excludesStr != null) {
						collection.setExcludesStr(excludesStr);
					}
					Collection coll = collectionContainer.get(collectionId);
					if (coll != null) {
            LOGGER.error("Double collectionId \"" + collectionId + "\" (in: \"" + collection.getConfigFileName() + "\" and in \"" + coll.getConfigFileName() + "\"");
					} else {
  					collectionContainer.put(collectionId, collection);
					}
				} catch (Exception e) {
          LOGGER.error("Reading of: " + name + " failed");
          e.printStackTrace();
				}

			}
		} catch (Exception e) {
			throw new ApplicationException(e);
		}
	}
	
	public void testDataUrls() throws ApplicationException {
	  ArrayList<Collection> collections = getCollections();
    String documentsDirectory = Constants.getInstance().getDocumentsDir();
	  File dummyFile = new File(documentsDirectory + "/testDataUrls/test.bla");
	  int counter = 0;
	  Integer counterError = 0;
	  for (int i=0; i<collections.size(); i++) {
	    Collection collection = collections.get(i);
	    WspUrl[] dataUrls = collection.getDataUrls();
	    if (dataUrls != null) {
	      String collectionId = collection.getId();
	      LOGGER.info(i + ". Testing project " + collectionId + " with " + dataUrls.length + " dataUrls");
	      for (int j=0; j<dataUrls.length; j++) {
	        WspUrl wspDataUrl = dataUrls[j];
	        String dataUrlStr = wspDataUrl.getUrl();
	        if (dataUrlStr != null) {
	          counter++;
  	        copyUrlToFile(collectionId, dataUrlStr, dummyFile, counterError);
	        }
	      }
	    }
	  }
    File dummyFileDir = new File(documentsDirectory + "/testDataUrls");
    FileUtils.deleteQuietly(dummyFileDir);
    int counterOk = counter - counterError;
	  LOGGER.info("Summary: " + collections.size() + " projects were tested. " + counterError + " dataUrls have errors and " + counterOk + " dataUrls are ok.");
	}

  private void copyUrlToFile(String collectionId, String srcUrlStr, File docDestFile, Integer counterError) {
    try {
      URL srcUrl = new URL(srcUrlStr);
      FileUtils.copyURLToFile(srcUrl, docDestFile, 5000, 5000);
    } catch (Exception e) {
      counterError = counterError + 1;
      LOGGER.error(collectionId + ": dataUrl: " + srcUrlStr + " failed: " + e.getMessage());
    }
  }
  
}
