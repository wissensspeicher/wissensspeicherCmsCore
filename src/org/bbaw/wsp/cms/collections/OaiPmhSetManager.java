package org.bbaw.wsp.cms.collections;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class OaiPmhSetManager {

  CollectionReader cReader;
  String source = "/opt/apache-tomcat-7.0.29/webapps/oai/WEB-INF/repository_settings_and_data/ListSets-config.xml";
  String outputDir = "/home/juergens/wspEtc/ressourcen";
  private XQueryEvaluator xQueryEvaluator;
  private ArrayList<Collection> collections;

  private static final Logger logger = Logger.getLogger(OaiPmhSetManager.class);
  
  public static void main(String[] args) throws ApplicationException {
    OaiPmhSetManager setManager = new OaiPmhSetManager();
    setManager.init();
    setManager.convert();
  }

  public OaiPmhSetManager() {
  }

  public void init(){
		try {
			this.cReader = CollectionReader.getInstance();
			this.xQueryEvaluator = new XQueryEvaluator();
			this.collections = cReader.getCollections();
		} catch (ApplicationException e) {
			e.printStackTrace();
		}
	}
  
  private void convert() throws ApplicationException{

    File outputRdfFile = new File(outputDir + "/" + "ListSets-config_TEST.xml");
    StringBuilder builder = new StringBuilder();
    try {
      for (Collection c : collections) {
      System.out.println(c.getId()); 
      
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        builder.append(System.getProperty("line.separator"));
        builder.append("<ListSets>");
        builder.append(System.getProperty("line.separator"));
        builder.append("<set>");
        builder.append(System.getProperty("line.separator"));
        builder.append("<setSpec>"+c.getId()+"</setSpec>");
        builder.append(System.getProperty("line.separator"));
        builder.append("<setDescription>");
        builder.append(System.getProperty("line.separator"));
        builder.append("<description>"+"</description>");
        builder.append(System.getProperty("line.separator"));
        builder.append("</setDescription>");
        builder.append(System.getProperty("line.separator"));
        builder.append("<virtualSearchField field=\"setSpec\">");
        builder.append(System.getProperty("line.separator"));
        builder.append("<virtualSearchTermDefinition term=\"avhbriefe\">");
        builder.append(System.getProperty("line.separator"));
        builder.append("<Query>");
        builder.append(System.getProperty("line.separator"));
        builder.append("<booleanQuery type=\"OR\">");
        builder.append(System.getProperty("line.separator"));
        builder.append("<booleanQuery type=\"OR\" excludeOrRequire=\"require\">");
        builder.append(System.getProperty("line.separator"));
        builder.append("textQuery field=\"docdir\" type=\"matchKeyword\""+/*c.getPmhDir+*/"</textQuery>");
        builder.append(System.getProperty("line.separator"));
        builder.append("</booleanQuery >");
        builder.append(System.getProperty("line.separator"));
        builder.append("</booleanQuery >");
        builder.append(System.getProperty("line.separator"));
        builder.append("</Query>");
        builder.append(System.getProperty("line.separator"));
        builder.append("</virtualSearchTermDefinition>");
        builder.append(System.getProperty("line.separator"));
        builder.append("</virtualSearchField>");
        builder.append(System.getProperty("line.separator"));
        builder.append("</set>");
        builder.append(System.getProperty("line.separator"));
        builder.append("</ListSets>");
        builder.append(System.getProperty("line.separator"));
        builder.append(System.getProperty("line.separator"));

      } 
    FileUtils.writeStringToFile(outputRdfFile, builder.toString());
    }catch (IOException e) {
      throw new ApplicationException(e);
    }
    
  }
  
  /**
   * bundles metadata to a set by Http POST
   * 
   */
  public void createSets() {
    try {
      CollectionReader collectionReader = CollectionReader.getInstance();
      ArrayList<Collection> collList = collectionReader.getCollections();
      
      for (Collection collection : collList) {
        String resultStr = null; 
        String currentSet = collection.getId();
        HttpClient client = new HttpClient();
        String params = "command=addMetadataDir&dirNickname="+currentSet+"&dirMetadataFormat=oai_dc&dirPath=/opt/wsp/data/oaiprovider/"+currentSet+"&metadataNamespace=http://www.openarchives.org/OAI/2.0/oai_dc/&metadataSchema=http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
        PostMethod postmethod = new PostMethod("http://wspdev.bbaw.de/oai/admin/metadata_dir-validate.do?"+params);
      
        int statusCode = client.executeMethod(postmethod);
        if (statusCode<  400) {
          byte[] resultBytes = postmethod.getResponseBody();
          resultStr = new String(resultBytes, "utf-8");
          logger.info("indexing oai-pmh set '"+currentSet+"'. status code : "+statusCode);
        } 
        client.executeMethod(postmethod);
        postmethod.releaseConnection();
      }
    } catch (HttpException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ApplicationException e) {
      e.printStackTrace();
    }  
    
  }
}
