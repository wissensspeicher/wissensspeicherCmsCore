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
  private Collection cmrw;
  private HashMap<String, Collection> wrapperContainer;
  private static CollectionReader collectionReader;

  private CollectionReader() throws ApplicationException {
    wrapperContainer = new HashMap<String, Collection>();
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
        cmrw = new Collection();
        String collectionId = xQueryEvaluator.evaluateAsString(srcUrl, "/wsp/collection/collectionId/text()");
        if(collectionId != null) {
          cmrw.setCollectionId(collectionId);
        }
        String mainLanguage = xQueryEvaluator.evaluateAsString(srcUrl, "/wsp/collection/mainLanguage/text()");
        if(mainLanguage != null) {
          cmrw.setMainLanguage(mainLanguage);
        }
        String collectionName = xQueryEvaluator.evaluateAsString(srcUrl, "/wsp/collection/name/text()");
        if(collectionName != null) {
          cmrw.setCollectionName(collectionName);
        }
        String collectionDataUrl = xQueryEvaluator.evaluateAsString(srcUrl, "/wsp/collection/collectionDataUrl/text()");
        if(collectionDataUrl != null) {
          cmrw.setCollectionDataUrl(collectionDataUrl);
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
        cmrw.setFields(fieldsArrayList);
        String[] registerFieldNames = {"persName", "placeName", "zoolName"};
        for (int i=0; i<registerFieldNames.length; i++) {
          String registerFieldName = registerFieldNames[i];
          String registerDocIdsStr = xQueryEvaluator.evaluateAsStringValueJoined(srcUrl, "/wsp/collection/registers/register[@name = '" + registerFieldName + "']", "###");
          ArrayList<String> registerDocIdsArrayList = new ArrayList<String>();
          if(registerDocIdsStr != null) {
            String[] registerDocIds = registerDocIdsStr.split("###");
            for (int j=0; j<registerDocIds.length; j++) {
              String registerDocId = registerDocIds[j].trim();
              if (! registerDocId.isEmpty())
                registerDocIdsArrayList.add(registerDocId);
            }
            cmrw.setRegister(registerFieldName, registerDocIdsArrayList);
          }
        }
        wrapperContainer.put(cmrw.getCollectionId(), cmrw);
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  public Collection getResultWrapper(String collectionId) {
    Collection cmrw = wrapperContainer.get(collectionId);
    return cmrw;
  }

}
