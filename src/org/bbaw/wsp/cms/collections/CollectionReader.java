package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

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
	private HashMap<String, Collection> collectionContainer;  // key is collectionId string and value is collection
  private HashMap<String, WspUrl> wspUrls;  // key is wspUrl string and value is wspUrl 
	
	private CollectionReader() throws ApplicationException {
		collectionContainer = new HashMap<String, Collection>();
		wspUrls = new HashMap<String, WspUrl>();
		readConfFiles();
	}

	public static CollectionReader getInstance() throws ApplicationException {
		if (collectionReader == null)
			collectionReader = new CollectionReader();
		return collectionReader;
	}

	public ArrayList<Collection> getCollections() {
		ArrayList<Collection> collections = null;
		if (collectionContainer != null) {
			collections = new ArrayList<Collection>();
			java.util.Collection<Collection> values = collectionContainer.values();
			for (Collection collection : values) {
				collections.add(collection);
			}
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
					XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
					URL configFileUrl = configFile.toURI().toURL();
					Collection collection = new Collection();
					collection.setConfigFileName(configFileName);
					String update = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/update/text()");
					if (update != null) {
						if (update.equals("true"))
							collection.setUpdateNecessary(true);
					}
					String collectionId = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/id/text()");
					if (collectionId != null) {
						collection.setId(collectionId);
					}
          String rdfId = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/rdfId/text()");
          if (rdfId != null) {
            collection.setRdfId(rdfId);
          }
          String dbName = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/db/name/text()");
          if (dbName != null) {
            Database database = new Database();
            database.setName(dbName);
            String xmlDumpFileName = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/db/xmlDumpFileName/text()");
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
					String collectionName = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/name/text()");
					if (collectionName != null) {
						collection.setName(collectionName);
					}
          String mainLanguage = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/mainLanguage/text()");
          if (mainLanguage != null) {
            collection.setMainLanguage(mainLanguage);
          }
					String metadataUrlStr = xQueryEvaluator.evaluateAsStringValueJoined(configFileUrl, "/wsp/collection/metadata/url");
					if (metadataUrlStr != null) {
						String[] metadataUrls = metadataUrlStr.split(" ");
						collection.setMetadataUrls(metadataUrls);
					}
					String metadataUrlPrefix = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/metadata/urlPrefix/text()");
					if (metadataUrlPrefix != null) {
						collection.setMetadataUrlPrefix(metadataUrlPrefix);
					}
					String metadataUrlType = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/metadata/urlType/text()");
					if (metadataUrlType != null) {
						collection.setMetadataUrlType(metadataUrlType);
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
					String webBaseUrl = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/url/webBaseUrl/text()");
					if (webBaseUrl != null) {
						collection.setWebBaseUrl(webBaseUrl);
					}
					String collectionDataUrlPrefix = xQueryEvaluator.evaluateAsString(configFileUrl, "/wsp/collection/url/dataUrlPrefix/text()");
					if (collectionDataUrlPrefix != null) {
						collection.setDataUrlPrefix(collectionDataUrlPrefix);
					}
					String formatsStr = xQueryEvaluator.evaluateAsStringValueJoined(configFileUrl, "/wsp/collection/formats/format", "###");
					ArrayList<String> formatsArrayList = new ArrayList<String>();
					if (formatsStr != null) {
						String[] formats = formatsStr.split("###");
						for (int i = 0; i < formats.length; i++) {
							String format = formats[i].trim().toLowerCase();
							if (!format.isEmpty())
								formatsArrayList.add(format);
						}
					}
					collection.setFormats(formatsArrayList);
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

}
