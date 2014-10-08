package org.bbaw.wsp.cms.dochandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
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
import org.bbaw.wsp.cms.document.DBpediaResource;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class DBpediaSpotlightHandler {
  private static DBpediaSpotlightHandler instance;
  private static int SOCKET_TIMEOUT = 200 * 1000;
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
  
  public Annotation annotate(String docId, String text, String type) throws ApplicationException {
    String testStr = "";
    NameValuePair textParam = new NameValuePair("text", text);
    NameValuePair confidenceParam = new NameValuePair("confidence", "0.99");
    NameValuePair typesParam = new NameValuePair("types", "Person"); // Person, Organisation, Place (Category ??) Einschränkung liefert evtl. zu wenige Entitäten
    NameValuePair supportParam = new NameValuePair("support", "20"); // how many incoming links are on the DBpedia-Resource 
    NameValuePair whitelistSparqlParam = new NameValuePair("sparql", "select ...");
    NameValuePair[] params = {textParam, confidenceParam};
    String spotlightAnnotationXmlStr = performPostRequest("annotate", params, "text/xml");
    List<DBpediaResource> resources = null; 
    try {
      spotlightAnnotationXmlStr = spotlightAnnotationXmlStr.replaceAll("&#[0-9];|&#1[0-9];|&#2[0-9];|&#30;|&#31;|&#32;", ""); // remove all steuerzeichen such as "&#0;"
      XdmValue xmdValueResources = xQueryEvaluator.evaluate(spotlightAnnotationXmlStr, "//Resource[not(@URI = preceding::Resource/@URI)]");  // no double resources
      if (xmdValueResources != null && xmdValueResources.size() > 0) {
        resources = new ArrayList<DBpediaResource>();
        XdmSequenceIterator xmdValueResourcesIterator = xmdValueResources.iterator();
        while (xmdValueResourcesIterator.hasNext()) {
          XdmItem xdmItemResource = xmdValueResourcesIterator.next();
          String resourceStr = xdmItemResource.toString();
          testStr = resourceStr;
          String uriStr = xQueryEvaluator.evaluateAsString(resourceStr, "string(/Resource/@URI)"); 
          String supportStr = xQueryEvaluator.evaluateAsString(resourceStr, "string(/Resource/@support)"); 
          String similarityStr = xQueryEvaluator.evaluateAsString(resourceStr, "string(/Resource/@similarityScore)"); 
          String typesStr = xQueryEvaluator.evaluateAsString(resourceStr, "string(/Resource/@types)"); 
          String uriStrEscaped = StringUtils.deresolveXmlEntities(uriStr);
          uriStrEscaped = uriStrEscaped.replaceAll("'", "&apos;");
          String frequencyStr = xQueryEvaluator.evaluateAsString(spotlightAnnotationXmlStr, "count(//Resource[@URI = '" + uriStrEscaped + "'])");
          DBpediaResource r = new DBpediaResource();
          r.setUri(uriStr);
          if (supportStr != null && ! supportStr.isEmpty())
            r.setSupport(new Integer(supportStr));
          if (similarityStr != null && ! similarityStr.isEmpty())
            r.setSimilarity(new Double(similarityStr));
          if (typesStr == null || typesStr.trim().isEmpty())
            r.setType("concept");
          else if (typesStr.contains("Person"))
            r.setType("person");
          else if (typesStr.contains("Organization") || typesStr.contains("Organisation"))
            r.setType("organization");
          else if (typesStr.contains("Place"))
            r.setType("place");
          else 
            r.setType("concept");
          r.setFrequency(new Integer(frequencyStr));
          resources.add(r);
        }
      }
    } catch (Exception e) {
      System.out.println("Error in DBpedia spotlight resource: " + docId + ": " + testStr);
    }
    Annotation annotation = null;
    if (resources != null && ! resources.isEmpty()) {
      annotation = new Annotation();
      annotation.setId(docId);
      Collections.sort(resources);
      annotation.setResources(resources);
    }
    return annotation;
  }
  
  private String performPostRequest(String serviceName, NameValuePair[] params, String outputFormat) throws ApplicationException {
    String resultStr = null;
    try {
      String portPart = ":" + PORT;
      String urlStr = "http://" + HOSTNAME + portPart + "/" + SERVICE_PATH + "/" + serviceName;
      PostMethod method = new PostMethod(urlStr);
      for (int i=0; i<params.length; i++) {
        NameValuePair param = params[i];
        method.addParameter(param);
      }
      method.addRequestHeader("Accept", outputFormat);  // values are: text/xml, application/json, text/html, application/xhtml+xml (RDFa: see http://www.w3.org/2007/08/pyRdfa/)
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
