package org.bbaw.wsp.cms.general;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

public class Constants {
  private static Constants instance;
  private static Logger LOGGER = Logger.getLogger(Constants.class);
  private String applicationDirectory;
  private String configDirectory;
  private Properties properties;
  private File coreConstantsPropFile;

  public File getPropConstantFile() {
    return coreConstantsPropFile;
  }

  public static Constants getInstance() {
    if (instance == null) {
      instance = new Constants();
      instance.init();
    }
    return instance;
  }

  public Properties getProperties() {
    return properties;
  }

  public static Constants getInstance(final String applicationDirectory) {
    if (instance == null) {
      instance = new Constants();
      instance.applicationDirectory = applicationDirectory;
      instance.init();
    }
    return instance;
  }

  private void init() {
    File configDir = new File("./config");
    if (applicationDirectory != null) {
      configDir = new File(applicationDirectory + "/config");
    }
    configDirectory = configDir.getAbsolutePath();
    if (configDir.exists()) {
      coreConstantsPropFile = new File(configDirectory + "/core/constants.properties");
      if (coreConstantsPropFile.exists()) {
        try {
          final FileInputStream in = new FileInputStream(coreConstantsPropFile);
          properties = new Properties();
          properties.load(in);
          LOGGER.info("CMS core property file: " + coreConstantsPropFile.getAbsolutePath() + " loaded");
        } catch (final IOException e) {
          e.printStackTrace();
        }
      } else {
        LOGGER.info("CMS core property file: " + coreConstantsPropFile.getAbsolutePath() + " not found");
      }
    } else {
      LOGGER.info("Application configuration directory: " + configDirectory + " not found");
    }
    // a little hack: this is done so that https urls could be fetched (e.g. by
    // FileUtils.copyURLToFile) without certificate error
    final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      @Override
      public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
        // No need to implement.
      }

      @Override
      public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
        // No need to implement.
      }
    } };
    try {
      // Install the all-trusting trust manager
      final SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  public String getDataDir() {
    if (properties != null) {
      return properties.getProperty("dataDir");
    } else {
      return "no properties file";
    }
  }

  public String getHarvestDir() {
    if (properties != null) {
      return properties.getProperty("harvestDir");
    } else {
      return "no properties file";
    }
  }

  public String getExternalDataDir() {
    if (properties != null) {
      return properties.getProperty("externalDataDir");
    } else {
      return "no properties file";
    }
  }

  public String getExternalDataDbPediaDir() {
    if (properties != null) {
      return properties.getProperty("externalDataDbPediaDir");
    } else {
      return "no properties file";
    }
  }

  public String getExternalDataDbPediaSpotlightDir() {
    if (properties != null) {
      return properties.getProperty("externalDataDbPediaSpotlightDir");
    } else {
      return "no properties file";
    }
  }

  public String getExternalDataDbDumpsDir() {
    if (properties != null) {
      return properties.getProperty("externalDataDbDumpsDir");
    } else {
      return "no properties file";
    }
  }

  public String getExternalDataResourcesDir() {
    if (properties != null) {
      return properties.getProperty("externalDataResourcesDir");
    } else {
      return "no properties file";
    }
  }

  public String getIndexDir() {
    if (properties != null) {
      return properties.getProperty("indexDir");
    } else {
      return "no properties file";
    }
  }

  public String getAnnotationsDir() {
    if (properties != null) {
      return properties.getProperty("annotationsDir");
    } else {
      return "no properties file";
    }
  }

  public String getLuceneDocumentsDir() {
    return getIndexDir() + "/documents";
  }

  public String getLuceneNodesDir() {
    return getIndexDir() + "/nodes";
  }

  public String getLuceneTaxonomyDir() {
    return getIndexDir() + "/taxonomy";
  }

  public String getMetadataDir() {
    if (properties != null) {
      return properties.getProperty("metadataDir");
    } else {
      return "no properties file";
    }
  }

  public String getUserMetadataDir() {
    if (properties != null) {
      return properties.getProperty("userMetadataDir");
    } else {
      return "no properties file";
    }
  }

  public String getCollectionConfDir() {
    return getMetadataDir() + "/resources-addinfo";
  }

  /**
   * @return the path to the mdsystem.xml
   */
  public String getMdsystemConfFile() {
    return getMetadataDir() + "/mdsystem.xml";
  }

  /**
   * @return the path to the wsp.normdata.xml
   */
  public String getMdsystemNormdataFile() {
    return getMetadataDir() + "/wsp.normdata.rdf";
  }

  public String getRdfStoreDir() {
    if (properties != null) {
      return properties.getProperty("rdfStoreDir");
    } else {
      return "no properties file";
    }
  }

  public String getRdfStoreMdDir() {
    return getRdfStoreDir() + "/rdf";
  }

  public String getRdfStoreLarqDir() {
    return getRdfStoreDir() + "/larq";
  }

  public String getOaiDir() {
    if (properties != null) {
      return properties.getProperty("oaiDir");
    } else {
      return "no properties file";
    }
  }

  public String getDBpediaSpotlightService() {
    String dbPediaSpotlightService = null;
    if (properties != null) {
      String dbPediaSpotlightServiceStr = properties.getProperty("dbPediaSpotlightService");
      if (dbPediaSpotlightServiceStr != null)
        dbPediaSpotlightService = dbPediaSpotlightServiceStr;
    } 
    return dbPediaSpotlightService;
  }

}
