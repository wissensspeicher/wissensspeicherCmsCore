package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
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
  private HashMap<String, Collection> projectRdfId2collection;  // key is projectRdfId string and value is collection
  private HashMap<String, Person> persons;   
	
	private CollectionReader() throws ApplicationException {
		collectionContainer = new HashMap<String, Collection>();
		projectRdfId2collection = new HashMap<String, Collection>();
		init();
		readConfFiles();
		readNormdataFile();
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

	/**
	 * Delivers collections from that startingCollectionId onwards till endingCollectionId alphabetically
	 * @param startingCollectionId
	 * @param endingCollectionId
	 * @return ArrayList<Collection>
	 */
  public ArrayList<Collection> getCollections(String startingCollectionId, String endingCollectionId) {
    ArrayList<Collection> retCollections = new ArrayList<Collection>();
    boolean start = false;
    boolean end = false;
    ArrayList<Collection> collections = collectionReader.getCollections();
    for (Collection collection : collections) {
      String collId = collection.getId();
      if (startingCollectionId.equals(collId))
        start = true;
      if (start && ! end)
        retCollections.add(collection);
      if (endingCollectionId.equals(collId)) {
        end = true;
        break;
      }
    }
    return retCollections;
  }

  public ArrayList<Collection> getCollections(String[] collectionIds) {
    ArrayList<Collection> retCollections = new ArrayList<Collection>();
    for (int i=0; i<collectionIds.length; i++) {
      String collId = collectionIds[i];
      Collection c = getCollection(collId);
      retCollections.add(c);
    }
    return retCollections;
  }

  public Collection getCollection(String collectionId) {
		Collection collection = collectionContainer.get(collectionId);
		return collection;
	}

  public Collection getCollectionByProjectRdfId(String projectRdfId) {
    Collection collection = projectRdfId2collection.get(projectRdfId);
    return collection;
  }

	public Person getPerson(String aboutId) {
	  return persons.get(aboutId);
	}
	
	private void readNormdataFile() {
    try {
      persons = new HashMap<String, Person>();
      String foafPerson = "http://xmlns.com/foaf/0.1/Person";
      XdmValue xmdValues = xQueryEvaluator.evaluate(inputNormdataFileUrl, NAMESPACE_DECLARATION + "/rdf:RDF/*:Description[rdf:type/@rdf:resource='" + foafPerson + "']");
      XdmSequenceIterator xmdValuesIterator = xmdValues.iterator();
      if (xmdValues != null && xmdValues.size() > 0) {
        while (xmdValuesIterator.hasNext()) {
          Person person = new Person();
          XdmItem xdmItem = xmdValuesIterator.next();
          String xdmItemStr = xdmItem.toString();
          String surname = xQueryEvaluator.evaluateAsString(xdmItemStr, "/*:Description/*:familyName/text()");
          String forename = xQueryEvaluator.evaluateAsString(xdmItemStr, "/*:Description/*:givenName/text()");
          if (surname != null && ! surname.isEmpty()) {
            person.setSurname(surname);
            person.setName(surname);
          }
          if (forename != null && ! forename.isEmpty()) {
            person.setForename(forename);
            if (person.getSurname() != null) 
              person.setName(person.getSurname() + ", " + forename);
          }
          String aboutId = xQueryEvaluator.evaluateAsString(xdmItemStr, "string(/*:Description/@*:about)");
          persons.put(aboutId, person);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Reading of: " + inputNormdataFileUrl + " failed");
      e.printStackTrace();
    }
	}
	
  private void readConfFiles() throws ApplicationException {
    try {
      // read info from project rdf file        
      String metadataRdfDirStr = Constants.getInstance().getMetadataDir() + "/resources";
      File metadataRdfDir = new File(metadataRdfDirStr);
      ArrayList<File> metadataRdfFiles = new ArrayList<File>(FileUtils.listFiles(metadataRdfDir, null, false));
      Collections.sort(metadataRdfFiles);
      Iterator<File> filesIter = metadataRdfFiles.iterator();
      while (filesIter.hasNext()) {
        File metadataRdfFile = filesIter.next();
        String metadataRdfFileName = metadataRdfFile.getName().toLowerCase();
        Collection collection = new Collection();
        String collectionId = metadataRdfFileName.substring(0, metadataRdfFileName.lastIndexOf("."));
        collection.setId(collectionId);
        Date metadataRdfFileLastModified = new Date(metadataRdfFile.lastModified());
        collection.setLastModified(metadataRdfFileLastModified);
        URL metadataRdfFileUrl = metadataRdfFile.toURI().toURL();
        String projectRdfStr = xQueryEvaluator.evaluateAsString(metadataRdfFileUrl, NAMESPACE_DECLARATION + "/rdf:RDF/rdf:Description[rdf:type/@rdf:resource = 'http://xmlns.com/foaf/0.1/Project']");
        if (projectRdfStr == null || projectRdfStr.trim().isEmpty()) {
          projectRdfStr = xQueryEvaluator.evaluateAsString(inputNormdataFileUrl, NAMESPACE_DECLARATION + "/rdf:RDF/*:Description[foaf:nick/text() ='" + collectionId + "']");  // TODO entfernen, wenn alle RDF-Dateien eine Projektbeschreibung haben
        } 
        String rdfId = xQueryEvaluator.evaluateAsString(projectRdfStr, NAMESPACE_DECLARATION + "string(/rdf:Description/@rdf:about)");
        if (rdfId == null || rdfId.trim().isEmpty())
          LOGGER.error("No project rdf id found in: " + metadataRdfFileName + " or in: " + inputNormdataFileUrl);
        else 
          collection.setRdfId(rdfId);
        String projectName = xQueryEvaluator.evaluateAsString(projectRdfStr, NAMESPACE_DECLARATION + "/rdf:Description/foaf:name/text()");
        if (projectName != null) {
          collection.setName(projectName);
        } else {
          LOGGER.error("Project: \"" + rdfId + "\" has no name: Please provide a name for this project");
        }
        String mainLanguage = xQueryEvaluator.evaluateAsString(projectRdfStr, NAMESPACE_DECLARATION + "/rdf:Description/dc:language/text()");
        if (mainLanguage != null) {
          collection.setMainLanguage(mainLanguage);
        } else {
          LOGGER.error("Project: \"" + rdfId + "\" has no language: Please provide a main language for this project");
        }
        String coverage = xQueryEvaluator.evaluateAsString(projectRdfStr, NAMESPACE_DECLARATION + "string-join(/rdf:Description/dc:coverage/@rdf:resource, ' ')");
        if (coverage != null && ! coverage.isEmpty()) {
          collection.setCoverage(coverage);
        } 
        String projectHomepage = xQueryEvaluator.evaluateAsString(projectRdfStr, NAMESPACE_DECLARATION + "string(/rdf:Description/foaf:homepage/@rdf:resource)");
        if (projectHomepage != null) {
          collection.setWebBaseUrl(projectHomepage);
        } else {
          LOGGER.error("Project: \"" + rdfId + "\" has no homepage: Please provide a homepage for this project");
        }
        // read additional info from xml config file        
        String configFileName = Constants.getInstance().getMetadataDir() + "/resources-addinfo/" + collectionId + ".xml";
        File configFile = new File(configFileName);
        if (configFile.exists()) {
          collection.setConfigFileName(configFileName);
          readConfFile(collection);
        }
        // add collection
        Collection coll = collectionContainer.get(collectionId);
        if (coll != null) {
          LOGGER.error("Double collectionId \"" + collectionId + "\" (in: \"" + collection.getConfigFileName() + "\" and in \"" + coll.getConfigFileName() + "\"");
        } else {
          collectionContainer.put(collectionId, collection);
          projectRdfId2collection.put(rdfId, collection);
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
	private void readConfFile(Collection collection) {
	  try {
      File configFile = new File(collection.getConfigFileName());
      URL configFileUrl = configFile.toURI().toURL();
      // read db infos
      XdmValue xmdValueDBs = xQueryEvaluator.evaluate(configFileUrl, "/wsp/collection/db");
      XdmSequenceIterator xmdValueDBsIterator = xmdValueDBs.iterator();
      if (xmdValueDBs != null && xmdValueDBs.size() > 0) {
        ArrayList<Database> collectionDBs = new ArrayList<Database>();
        while (xmdValueDBsIterator.hasNext()) {
          Database db = new Database();
          XdmItem xdmItemDB = xmdValueDBsIterator.next();
          String xdmItemDBStr = xdmItemDB.toString(); 
          String dbType = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/type/text()");  // e.g. "mysql" or "postgres" or "oai"
          db.setType(dbType);
          String dbName = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/name/text()");
          db.setName(dbName);
          String dbUrl = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/url/text()");
          db.setUrl(dbUrl);
          String language = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/language/text()");
          db.setLanguage(language);
          String jdbcHost = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/jdbc/host/text()");
          if (jdbcHost != null) {
            JdbcConnection jdbcConn = new JdbcConnection();
            jdbcConn.setType(dbType);
            jdbcConn.setHost(jdbcHost);
            String jdbcPort = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/jdbc/port/text()");
            jdbcConn.setPort(jdbcPort);
            String jdbcDb = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/jdbc/db/text()");
            jdbcConn.setDb(jdbcDb);
            String jdbcUser = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/jdbc/user/text()");
            jdbcConn.setUser(jdbcUser);
            String jdbcPw = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/jdbc/pw/text()");
            jdbcConn.setPw(jdbcPw);
            db.setJdbcConnection(jdbcConn);
            String sql = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/sql/text()");
            db.setSql(sql);
          }
          String oaiSet = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/set/text()");
          if (oaiSet != null)
            db.setOaiSet(oaiSet);
          String mainResourcesTable = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/mainResourcesTable/name/text()");
          if (mainResourcesTable != null)
            db.setMainResourcesTable(mainResourcesTable);
          String mainResourcesTableId = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/mainResourcesTable/idField/text()");
          if (mainResourcesTableId != null)
            db.setMainResourcesTableId(mainResourcesTableId);
          XdmValue mainResourcesTableFields = xQueryEvaluator.evaluate(xdmItemDBStr, "/db/mainResourcesTable/field");
          XdmSequenceIterator mainResourcesTableFieldsIterator = mainResourcesTableFields.iterator();
          if (mainResourcesTableFields != null && mainResourcesTableFields.size() > 0) {
            while (mainResourcesTableFieldsIterator.hasNext()) {
              XdmItem mainResourcesTableField = mainResourcesTableFieldsIterator.next();
              String mainResourcesTableFieldStr = mainResourcesTableField.toString(); // e.g. <field><dc>title</dc><db>title</db></field>
              String dcField = xQueryEvaluator.evaluateAsString(mainResourcesTableFieldStr, "/field/dc/text()");
              String dbField = xQueryEvaluator.evaluateAsString(mainResourcesTableFieldStr, "/field/db/text()");
              if (dcField != null && dbField != null)
                db.addField(dcField, dbField);
            }
          }
          String webIdPreStr = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/webId/preStr/text()");
          if (webIdPreStr != null)
            db.setWebIdPreStr(webIdPreStr);
          String webIdAfterStr = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/webId/afterStr/text()");
          if (webIdAfterStr != null)
            db.setWebIdAfterStr(webIdAfterStr);
          // read excludes (e.g. used in eXist resources, to eliminate these sub directories)
          XdmValue xmdValueExcludes = xQueryEvaluator.evaluate(xdmItemDBStr, "/db/excludes/exclude");
          XdmSequenceIterator xmdValueExcludesIterator = xmdValueExcludes.iterator();
          if (xmdValueExcludes != null && xmdValueExcludes.size() > 0) {
            ArrayList<String> excludes = new ArrayList<String>();
            while (xmdValueExcludesIterator.hasNext()) {
              XdmItem xdmItemExclude = xmdValueExcludesIterator.next();
              String xdmItemExcludeStr = xdmItemExclude.getStringValue().trim();
              if (xdmItemExcludeStr != null && ! xdmItemExcludeStr.isEmpty())
                excludes.add(xdmItemExcludeStr);
            }
            db.setExcludes(excludes);
          }
          String depthStr = xQueryEvaluator.evaluateAsString(xdmItemDBStr, "/db/depth/text()");
          if (depthStr != null && ! depthStr.isEmpty()) {
            Integer depth = Integer.parseInt(depthStr);
            db.setDepth(depth);
          } 
          // read xqueries (used for building dynamically a webUri of a record; only used in xml records
          Hits xQueries = (Hits) xQueryEvaluator.evaluate(xdmItemDBStr, "/db/xqueries/xquery", 0, 9, "hits");
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
            db.setxQueries(xqueriesHashtable);
          }
          collectionDBs.add(db);
        }
        collection.setDatabases(collectionDBs);
      }
      // read services (for dynamically building queries to single project documents, e.g. in dta)
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
	  } catch (Exception e) {
      LOGGER.error("Reading of: " + collection.getConfigFileName() + " failed");
      e.printStackTrace();
    }
	}
	
	public void testDataUrls() throws ApplicationException {
	  ArrayList<Collection> collections = getCollections();
    String harvestDirectory = Constants.getInstance().getHarvestDir();
	  File dummyFile = new File(harvestDirectory + "/testDataUrls/test.bla");
	  int counter = 0;
	  Integer counterError = 0;
	  for (int i=0; i<collections.size(); i++) {
	    Collection collection = collections.get(i);
	    ArrayList<MetadataRecord> mdRecords = MetadataHandler.getInstance().getMetadataRecords(collection, true);
	    if (mdRecords != null) {
	      String collectionId = collection.getId();
	      LOGGER.info(i + ". Testing project " + collectionId + " with " + mdRecords.size() + " urls");
	      for (int j=0; j<mdRecords.size(); j++) {
	        MetadataRecord mdRecord = mdRecords.get(j);
	        String url = mdRecord.getWebUri();
	        if (url != null) {
	          counter++;
  	        copyUrlToFile(collectionId, url, dummyFile, counterError);
	        }
	      }
	    }
	  }
    File dummyFileDir = new File(harvestDirectory + "/testDataUrls");
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
