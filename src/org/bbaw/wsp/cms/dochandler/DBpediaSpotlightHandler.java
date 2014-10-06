package org.bbaw.wsp.cms.dochandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.bbaw.wsp.cms.document.Annotation;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class DBpediaSpotlightHandler {
  private static DBpediaSpotlightHandler instance;
  private static int SOCKET_TIMEOUT = 100 * 1000;
  private static String HOSTNAME = "localhost";
  private static int PORT = 2222;
  private static String SERVICE_PATH = "spotlight/rest";
  private HttpClient httpClient; 
  private XQueryEvaluator xQueryEvaluator;

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
    xQueryEvaluator = new XQueryEvaluator();
  }
  
  public Annotation annotate(String text, String type) throws ApplicationException {
    NameValuePair textParam = new NameValuePair("text", text);
    NameValuePair confidenceParam = new NameValuePair("confidence", "0.99");
    NameValuePair typesParam = new NameValuePair("types", "Person"); // Person, Organisation, Place (Category ??) Einschränkung liefert evtl. zu wenige Entitäten
    NameValuePair supportParam = new NameValuePair("support", "20"); // how many incoming links are on the DBpedia-Resource 
    NameValuePair whitelistSparqlParam = new NameValuePair("sparql", "select ...");
    NameValuePair[] params = {textParam, confidenceParam};
    String spotlightAnnotationXmlStr = performPostRequest("annotate", params);
    List<String> resources = null; 
    XdmValue xmdValueResourceUris = xQueryEvaluator.evaluate(spotlightAnnotationXmlStr, "//Resource[not(@URI = preceding::Resource/@URI)]/@URI");
    if (xmdValueResourceUris != null && xmdValueResourceUris.size() > 0) {
      resources = new ArrayList<String>();
      XdmSequenceIterator xmdValueAuthorsIterator = xmdValueResourceUris.iterator();
      while (xmdValueAuthorsIterator.hasNext()) {
        XdmItem xdmItemResourceUri = xmdValueAuthorsIterator.next();
        String uriStr = xdmItemResourceUri.getStringValue();
        int begin = uriStr.lastIndexOf("resource/")+ 9;
        String resourceName = uriStr.substring(begin);
        resources.add(resourceName);
      }
    }
    Annotation annotation = new Annotation();
    annotation.setResources(resources);
    annotation.setSpotlightAnnotationXmlStr(spotlightAnnotationXmlStr);
    return annotation;
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
