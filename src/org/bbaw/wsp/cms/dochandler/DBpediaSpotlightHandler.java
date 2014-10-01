package org.bbaw.wsp.cms.dochandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class DBpediaSpotlightHandler {
  private static DBpediaSpotlightHandler instance;
  private static int SOCKET_TIMEOUT = 100 * 1000;
  private static String HOSTNAME = "localhost";
  private static int PORT = 2222;
  private static String SERVICE_PATH = "spotlight/rest";
  private HttpClient httpClient; 

  public static DBpediaSpotlightHandler getInstance() throws ApplicationException {
    if (instance == null) {
      instance = new DBpediaSpotlightHandler();
      instance.init();
    }
    return instance;
  }
  
  private void init() throws ApplicationException {
    initHttpClient();
  }
  
  private void initHttpClient() {
    httpClient = new HttpClient();
    // timeout seems to work
    httpClient.getParams().setParameter("http.socket.timeout", SOCKET_TIMEOUT);
    httpClient.getParams().setParameter("http.connection.timeout", SOCKET_TIMEOUT);
    httpClient.getParams().setParameter("http.connection-manager.timeout", new Long(SOCKET_TIMEOUT));
    httpClient.getParams().setParameter("http.protocol.head-body-timeout", SOCKET_TIMEOUT);
  }
  
  public String annotate(String text) throws ApplicationException {
    NameValuePair textParam = new NameValuePair("text", text);
    NameValuePair confidenceParam = new NameValuePair("confidence", "0.5");
    NameValuePair supportParam = new NameValuePair("support", "20");
    NameValuePair whitelistSparqlParam = new NameValuePair("sparql", "select ...");
    NameValuePair[] params = {textParam, confidenceParam};
    String dbPediaKnowledgeXmlStr = performPostRequest("annotate", params);
    return dbPediaKnowledgeXmlStr;
  }
  
  private String performPostRequest(String serviceName, NameValuePair[] params) throws ApplicationException {
    String resultStr = null;
    try {
      String portPart = ":" + PORT;
      String urlStr = "http://" + HOSTNAME + portPart + "/" + SERVICE_PATH + "/" + serviceName;
      PostMethod method = new PostMethod(urlStr);
      for (int i=0; i<params.length; i++) {
        NameValuePair param = params[i];
        method.addParameter(param);
      }
      method.addRequestHeader("Accept", "text/xml");  // so output is xml; other values are: application/json, text/html, application/xhtml+xml (RDFa: see http://www.w3.org/2007/08/pyRdfa/)
      httpClient.executeMethod(method);
      InputStream is = method.getResponseBodyAsStream();
      BufferedInputStream in = new BufferedInputStream(is);
      resultStr = IOUtils.toString(in, "utf-8");
      in.close();
      method.releaseConnection();
    } catch (HttpException e) {
      throw new ApplicationException(e);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return resultStr;
  }

}
