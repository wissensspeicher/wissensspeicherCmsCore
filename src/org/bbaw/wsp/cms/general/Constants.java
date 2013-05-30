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

  public String getDocumentsDir() {
    if (properties != null) {
      return properties.getProperty("documentsDir");
    } else {
      return "no properties file";
    }
  }

  public String getLuceneDocumentsDir() {
    if (properties != null) {
      return properties.getProperty("luceneDocumentsDir");
    } else {
      return "no properties file";
    }
  }

  public String getLuceneNodesDir() {
    if (properties != null) {
      return properties.getProperty("luceneNodesDir");
    } else {
      return "no properties file";
    }
  }

  public String getLuceneTaxonomyDir() {
    if (properties != null) {
      return properties.getProperty("luceneTaxonomyDir");
    } else {
      return "no properties file";
    }
  }

  public String getCollectionConfDir() {
    return configDirectory + "/collections";
  }

  /**
   * @return the dir to the mdsystem.xml
   */
  public String getMdsystemConfDir() {
    return configDirectory + "/mdsystem";
  }

  /**
   * @return the path to the mdsystem.xml
   */
  public String getMdsystemConfFile() {
    return getMdsystemConfDir() + "/mdsystem.xml";
  }

  /**
   * @return the path to the mdsystem.xml
   */
  public String getMdsystemNormdataFile() {
    return getMdsystemConfDir() + "/wsp.normdata.rdf_V25_04_13";
  }

  /**
   * @return the path to the rdfStoreRdfDir
   */
  public String getRdfStoreMdDir() {
    if (properties != null) {
      return properties.getProperty("rdfStoreRdfDir");
    } else {
      return "no properties file";
    }
  }

  /**
   * @return the path to the rdfStoreLarqDir
   */
  public String getRdfStoreLarqDir() {
    if (properties != null) {
      return properties.getProperty("rdfStoreLarqDir");
    } else {
      return "no properties file";
    }
  }

}
