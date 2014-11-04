package org.bbaw.wsp.cms.dochandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.Annotation;
import org.bbaw.wsp.cms.document.DBpediaResource;
import org.bbaw.wsp.cms.general.Constants;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class DBpediaSpotlightHandler {
  private static Logger LOGGER = Logger.getLogger(DBpediaSpotlightHandler.class);
  private static DBpediaSpotlightHandler instance;
  private static int SOCKET_TIMEOUT = 200 * 1000;
  // private static String HOSTNAME = "localhost";
  private static String HOSTNAME = "wspdev.bbaw.de";
  private static int PORT = 2222;
  private static String SERVICE_PATH = "spotlight/rest";
  private HttpClient httpClient; 
  private XQueryEvaluator xQueryEvaluator;
  private Hashtable<String, DBpediaResource> dbPediaResources;
  private int counter = 0;

  public static DBpediaSpotlightHandler getInstance() throws ApplicationException {
    if (instance == null) {
      instance = new DBpediaSpotlightHandler();
      instance.init();
    }
    return instance;
  }
  
  private void init() throws ApplicationException {
    initHttpClient();
    initDBpediaResources();
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
  
  private void initDBpediaResources() throws ApplicationException {
    dbPediaResources = new Hashtable<String, DBpediaResource>();
    initDBpediaResourceLabels();
    LOGGER.info("DBpediaSpotlightHandler initialized with: " + dbPediaResources.size() + " DBpedia resources");
  }
  
  private void initDBpediaResourceLabels() throws ApplicationException {
    String dbPediaResourcesDirName = Constants.getInstance().getExternalDocumentsDir() +  "/dbPediaResources";
    File dbPediaResourceLabelsFile = new File(dbPediaResourcesDirName + "/labels.ttl");
    try {
      Scanner s = new Scanner(dbPediaResourceLabelsFile, "utf-8");
      while (s.hasNextLine()) {
        String line = s.nextLine();
        if (! line.startsWith("#")) {
          String[] triple = line.split("> ");
          String uri = triple[0].substring(1);
          int to = triple[2].lastIndexOf("\"");
          String label = triple[2].substring(1, to);
          DBpediaResource dbPediaResource = new DBpediaResource();
          dbPediaResource.setUri(uri);
          dbPediaResource.setName(label);
          dbPediaResources.put(uri, dbPediaResource);
        }
      }
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  public Annotation annotate(String docId, String text, String confidence, int count) throws ApplicationException {
    counter++;
    if (counter == 100) {
      counter = 0;
      init(); // so that the handler has new objects and garbage collection of old objects could be done
    }
    String testStr = "";
    NameValuePair textParam = new NameValuePair("text", text);
    NameValuePair confidenceParam = new NameValuePair("confidence", confidence);
    // NameValuePair typesParam = new NameValuePair("types", "Person"); // Person, Organisation, Place (Category ??) Einschränkung liefert evtl. zu wenige Entitäten
    // NameValuePair supportParam = new NameValuePair("support", "20"); // how many incoming links are on the DBpedia-Resource 
    // NameValuePair whitelistSparqlParam = new NameValuePair("sparql", "select ...");
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
          DBpediaResource dbPediaResource = dbPediaResources.get(uriStr);
          if (dbPediaResource != null) {
            r.setName(dbPediaResource.getName());
          } else {
            int begin = uriStr.lastIndexOf("resource/") + 9;
            String name = uriStr.substring(begin);
            r.setName(name);
          }
          if (supportStr != null && ! supportStr.isEmpty())
            r.setSupport(new Integer(supportStr));
          if (similarityStr != null && ! similarityStr.isEmpty())
            r.setSimilarity(new Double(similarityStr));
          if (typesStr == null || typesStr.trim().isEmpty())
            r.setType("concept");
          else if (typesStr.contains("Person"))
            r.setType("person");
          else if (typesStr.contains("Organization") || typesStr.contains("Organisation"))
            r.setType("organisation");
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
      Collections.sort(resources, DBpediaResource.SimilarityComparatorDESC);
      ArrayList<DBpediaResource> mainResources = new ArrayList<DBpediaResource>();
      if (resources.size() <= count)
        count = resources.size();
      for (int i=0; i<count; i++) {
        mainResources.add(resources.get(i));
      }
      // TODO
      // fetch all names of mainResources by SparQL: e.g. these 2: 
      // SELECT ?name WHERE {
      //   {<http://de.dbpedia.org/resource/Karl_Marx> foaf:name ?name .} union
      //   {<http://de.dbpedia.org/resource/Friedrich_Engels> foaf:name ?name .} 
      // }
      annotation.setResources(mainResources);
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
