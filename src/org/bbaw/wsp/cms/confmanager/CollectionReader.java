package org.bbaw.wsp.cms.confmanager;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.harvester.PathExtractor;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class CollectionReader {
  private Collection collection;
  private HashMap<String, Collection> collectionContainer;
  private static CollectionReader collectionReader;

  private CollectionReader() throws ApplicationException {
    collectionContainer = new HashMap<String, Collection>();
    readConfFiles();
  }

  public static CollectionReader getInstance() throws ApplicationException {
    if (collectionReader == null)
      collectionReader = new CollectionReader();
    return collectionReader;
  }

  private void readConfFiles() throws ApplicationException {
    try {
      // get conf files
      PathExtractor ext = new PathExtractor();
      List<String> configsList = ext.extractPathLocally(Constants.getInstance().getConfDir());
      File configFile = null;
      for (String configXml : configsList) {
        configFile = new File(configXml);
        XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
        URL srcUrl = configFile.toURI().toURL();
        collection = new Collection();
        String collectionId = xQueryEvaluator.evaluateAsString(srcUrl, "/wsp/collection/id/text()");
        if(collectionId != null) {
          collection.setId(collectionId);
        }
        String mainLanguage = xQueryEvaluator.evaluateAsString(srcUrl, "/wsp/collection/mainLanguage/text()");
        if(mainLanguage != null) {
          collection.setMainLanguage(mainLanguage);
        }
        String collectionName = xQueryEvaluator.evaluateAsString(srcUrl, "/wsp/collection/name/text()");
        if(collectionName != null) {
          collection.setName(collectionName);
        }
        String collectionDataUrl = xQueryEvaluator.evaluateAsString(srcUrl, "/wsp/collection/url/dataUrl/text()");
        if(collectionDataUrl != null) {
          collection.setDataUrl(collectionDataUrl);
        }
        String fieldsStr = xQueryEvaluator.evaluateAsStringValueJoined(srcUrl, "/wsp/collection/fields/field", "###");
        ArrayList<String> fieldsArrayList = new ArrayList<String>();
        if(fieldsStr != null) {
          String[] fields = fieldsStr.split("###");
          for (int i=0; i<fields.length; i++) {
            String field = fields[i].trim();
            if (! field.isEmpty())
              fieldsArrayList.add(field);
          }
        }
        collection.setFields(fieldsArrayList);
        collectionContainer.put(collection.getId(), collection);
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  public Collection getCollection(String collectionId) {
    Collection collection = collectionContainer.get(collectionId);
    return collection;
  }

}
