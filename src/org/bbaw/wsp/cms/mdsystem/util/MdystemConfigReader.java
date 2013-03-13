package org.bbaw.wsp.cms.mdsystem.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.bbaw.wsp.cms.general.Constants;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

/**
 * This class reads fields from a config file.
 * 
 * It should be located in: config/mdsystem/mdmsystem.xml
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 28.01.2013
 * 
 */
public class MdystemConfigReader {

  private final String configFile;

  private MdsystemConfigRecord record;

  private static MdystemConfigReader instance;

  private MdystemConfigReader() {
    configFile = Constants.getInstance().getMdsystemConfFile();
    loadFromConfigFile();
  }

  public static MdystemConfigReader getInstance() {
    if (instance == null) {
      instance = new MdystemConfigReader();
    }
    return instance;
  }

  /**
   * 
   * @return the filled {@link IMdsystemConfigRecord}
   */
  public IMdsystemConfigRecord getConfig() {
    // get persisted record class or load from file
    return record;
  }

  private IMdsystemConfigRecord loadFromConfigFile() {
    record = new MdsystemConfigRecord();
    final XQueryEvaluator evaluator = new XQueryEvaluator();
    URL configFileUrl;
    try {
      configFileUrl = new URL("file:///" + new File(configFile).getAbsolutePath());
      final String eDocTemplatePath = evaluator.evaluateAsString(configFileUrl, "/mdsystem/converter/templates/eDoc/templatePath/text()");
      record.seteDocTemplatePath(eDocTemplatePath);

      final String modsStylesheetPath = evaluator.evaluateAsString(configFileUrl, "/mdsystem/converter/xslt/mods/stylesheetPath/text()");
      record.setModsStylesheetPath(modsStylesheetPath);

      final String metsStylesheetPath = evaluator.evaluateAsString(configFileUrl, "/mdsystem/converter/xslt/mets/stylesheetPath/text()");
      record.setMetsStylesheetPath(metsStylesheetPath);

      final String datasetPath = evaluator.evaluateAsString(configFileUrl, "/mdsystem/rdfmanager/tripleStore/datasetPath/text()");
      record.setDatasetPath(datasetPath);

      final String larqIndexPath = evaluator.evaluateAsString(configFileUrl, "/mdsystem/rdfmanager/tripleStore/larqIndexPath/text()");
      record.setLarqIndexPath(larqIndexPath);

      final String normdataPath = evaluator.evaluateAsString(configFileUrl, "/mdsystem/general/normdataFile/text()");
      record.setNormdataPath(normdataPath);
    } catch (final MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final ApplicationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return record;
  }

}
