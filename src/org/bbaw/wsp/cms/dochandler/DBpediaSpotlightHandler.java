package org.bbaw.wsp.cms.dochandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.collections.Collection;
import org.bbaw.wsp.cms.document.Annotation;
import org.bbaw.wsp.cms.document.DBpediaResource;
import org.bbaw.wsp.cms.general.Constants;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

/*
 * General SparQL knowledge (DBpedia SparQL interface): fetch all names of mainResources by SparQL: e.g. these 2: 
   SELECT ?name WHERE {
     {<http://de.dbpedia.org/resource/Karl_Marx> foaf:name ?name .} union
     {<http://de.dbpedia.org/resource/Friedrich_Engels> foaf:name ?name .} 
   }
 */
public class DBpediaSpotlightHandler {
  private static Logger LOGGER = Logger.getLogger(DBpediaSpotlightHandler.class);
  private static DBpediaSpotlightHandler instance;
  private static DBpediaSpotlightHandler instanceSupport;
  private static DBpediaSpotlightHandler instanceStopwords;
  private static int SOCKET_TIMEOUT = 200 * 1000;
  private static String SERVICE = "http://wspindex.bbaw.de/spotlight/rest";
  private HttpClient httpClient; 
  private XQueryEvaluator xQueryEvaluator;
  private boolean serviceAnnotateReachable = true;
  private Hashtable<String, DBpediaResource> dbPediaResources;
  private Hashtable<String, String> germanStopwords;
  private Hashtable<String, String> germanStopwordExpressions;
  private Hashtable<String, Integer> germanSupports;

  public static void main(String[] args) throws ApplicationException {
    try {
      DBpediaSpotlightHandler dbPediaSpotlightHandler = DBpediaSpotlightHandler.getSupportInstance();
      dbPediaSpotlightHandler.writeDBpediaSupports();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
      
  public static DBpediaSpotlightHandler getInstance() throws ApplicationException {
    if (instance == null) {
      instance = new DBpediaSpotlightHandler();
      instance.init();
    }
    return instance;
  }
  
  public static DBpediaSpotlightHandler getSupportInstance() throws ApplicationException {
    if (instanceSupport == null) {
      instanceSupport = new DBpediaSpotlightHandler();
      instanceSupport.initDBpediaSupports();
    }
    return instanceSupport;
  }
  
  public static DBpediaSpotlightHandler getStopwordsInstance() throws ApplicationException {
    if (instanceStopwords == null) {
      instanceStopwords = new DBpediaSpotlightHandler();
      instanceStopwords.initStopwords();
    }
    return instanceStopwords;
  }
  
  private void init() throws ApplicationException {
    String dbPediaSpotlightService = Constants.getInstance().getDBpediaSpotlightService();
    if (dbPediaSpotlightService != null)
      SERVICE = dbPediaSpotlightService;
    initHttpClient();
    testCall();
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
  
  private void testCall() throws ApplicationException {
    HttpClient httpClient = new HttpClient();
    int smallSocketTimeout = 2000;
    // timeout seems to work
    httpClient.getParams().setParameter("http.socket.timeout", smallSocketTimeout);
    httpClient.getParams().setParameter("http.connection.timeout", smallSocketTimeout);
    httpClient.getParams().setParameter("http.connection-manager.timeout", new Long(smallSocketTimeout));
    httpClient.getParams().setParameter("http.protocol.head-body-timeout", smallSocketTimeout);
    NameValuePair textParam = new NameValuePair("text", "test");
    NameValuePair confidenceParam = new NameValuePair("confidence", "0.99");
    NameValuePair[] params = {textParam, confidenceParam};
    try {
      performPostRequest(httpClient, "annotate", params, "text/xml");
    } catch (Exception e) {
      serviceAnnotateReachable = false;
      String urlStr = SERVICE + "/" + "annotate";
      LOGGER.info("DBpediaSpotlightHandler: Service: " + urlStr + " not reachable");
    }
  }

  private void initDBpediaResources() throws ApplicationException {
    dbPediaResources = new Hashtable<String, DBpediaResource>();
    germanStopwords = new Hashtable<String, String>();
    germanStopwordExpressions = new Hashtable<String, String>();
    initStopwords();
    initDBpediaResourceLabels();
    LOGGER.info("DBpediaSpotlightHandler initialized with: " + dbPediaResources.size() + " DBpedia resources");
  }
  
  private void initStopwords() throws ApplicationException {
    String dbPediaSpotlightDirName = Constants.getInstance().getExternalDataDbPediaSpotlightDir();
    File spotlightStopwordFileGerman = new File(dbPediaSpotlightDirName + "/stopwords-de.txt");
    if (! spotlightStopwordFileGerman.exists()) {
      LOGGER.info("DBpediaSpotlightHandler could not be initialized. File: " + spotlightStopwordFileGerman.getAbsolutePath() + " does not exist");
      return;
    }
    germanStopwords = new Hashtable<String, String>();
    germanStopwordExpressions = new Hashtable<String, String>();
    try {
      Scanner s = new Scanner(spotlightStopwordFileGerman, "utf-8");
      while (s.hasNextLine()) {
        String stopword = s.nextLine().trim();
        if (stopword != null && ! stopword.isEmpty()) {
          if (stopword.endsWith("*"))
            germanStopwordExpressions.put(stopword, stopword);
          else
            germanStopwords.put(stopword, stopword);
        }
      }
      s.close();
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  private void initDBpediaSupports() throws ApplicationException {
    xQueryEvaluator = new XQueryEvaluator();
    germanSupports = new Hashtable<String, Integer>();
    String dbPediaSpotlightDirName = Constants.getInstance().getExternalDataDbPediaSpotlightDir();
    File spotlightSupportFileGerman = new File(dbPediaSpotlightDirName + "/supports-de.txt");
    if (! spotlightSupportFileGerman.exists()) {
      LOGGER.info("DBpediaSpotlightHandler could not be initialized. File: " + spotlightSupportFileGerman.getAbsolutePath() + " does not exist");
      return;
    }
    try {
      Scanner s = new Scanner(spotlightSupportFileGerman, "utf-8");
      while (s.hasNextLine()) {
        String line = s.nextLine().trim();
        if (line != null && ! line.isEmpty()) {
          String[] supportPair = line.split(" ");
          String uri = supportPair[0];
          Integer support = new Integer(supportPair[1]);
          germanSupports.put(uri, support);
        }
      }
      s.close();
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  private void writeDBpediaSupports() throws ApplicationException {
    germanSupports = new Hashtable<String, Integer>();
    String dbPediaSpotlightDirName = Constants.getInstance().getExternalDataDbPediaSpotlightDir();
    File spotlightSupportFileGerman = new File(dbPediaSpotlightDirName + "/supports-de.txt");
    StringBuilder supportStrBuilder = new StringBuilder();
    try {
      String rdfFileFilterStr = "*.rdf"; 
      FileFilter rdfFileFilter = new WildcardFileFilter(rdfFileFilterStr);
      File dbPediaSpotlightDir = new File(dbPediaSpotlightDirName);
      File[] files = dbPediaSpotlightDir.listFiles(rdfFileFilter);
      if (files != null && files.length > 0) {
        for (int i = 0; i < files.length; i++) {
          File projectDBpediaSpotlightFile = files[i];
          URL dbPediaSpotlightFileUrl = projectDBpediaSpotlightFile.toURI().toURL();
          XdmValue xmdValueEntities = xQueryEvaluator.evaluate(dbPediaSpotlightFileUrl, "//*:relation/*:Description");
          XdmSequenceIterator xmdValueEntitiesIterator = xmdValueEntities.iterator();
          if (xmdValueEntities != null && xmdValueEntities.size() > 0) {
            while (xmdValueEntitiesIterator.hasNext()) {
              XdmItem xdmItemEntity = xmdValueEntitiesIterator.next();
              String xdmItemEntityStr = xdmItemEntity.toString();
              String uri = xQueryEvaluator.evaluateAsString(xdmItemEntityStr, "string(/*:Description/@*:about)");
              if (! germanSupports.containsKey(uri)) { 
                String supportStr = xQueryEvaluator.evaluateAsString(xdmItemEntityStr, "/*:Description/*:support/text()");
                Integer support = new Integer(supportStr);
                supportStrBuilder.append(uri + " " + supportStr + "\n");
                germanSupports.put(uri, support);
              }
            }
          }
        }
      }
      FileUtils.writeStringToFile(spotlightSupportFileGerman, supportStrBuilder.toString(), "utf-8");
      LOGGER.info("DBpedia Spotlight support file: \"" + spotlightSupportFileGerman + "\" sucessfully created");
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }
  
  public Integer getSupport(String uri) {
    return germanSupports.get(uri);
  }
  
  private void initDBpediaResourceLabels() throws ApplicationException {
    String dbPediaResourcesDirName = Constants.getInstance().getExternalDataDbPediaDir();
    File dbPediaResourceLabelsFile = new File(dbPediaResourcesDirName + "/labels.ttl");
    if (! dbPediaResourceLabelsFile.exists()) {
      LOGGER.info("DBpediaSpotlightHandler could not be initialized. File: " + dbPediaResourceLabelsFile.getAbsolutePath() + " does not exist");
      return;
    }
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
      // persondata: get surname and givenName of all person resources
      File dbPediaResourcePersondataFile = new File(dbPediaResourcesDirName + "/persondata-names.ttl");
      Scanner s2 = new Scanner(dbPediaResourcePersondataFile, "utf-8");
      while (s2.hasNextLine()) {
        String line = s2.nextLine();
        if (! line.startsWith("#")) {
          String[] triple = line.split("> ");
          String fieldName = triple[1].substring(1);
          if (fieldName.endsWith("surname")) {
            String uri = triple[0].substring(1);
            int to = triple[2].lastIndexOf("\"");
            String surname = triple[2].substring(1, to);
            DBpediaResource personResource = dbPediaResources.get(uri);
            if (personResource != null) {
              personResource.setName(surname);
              personResource.setType("person");
            }
          } else if (fieldName.endsWith("givenName")) {
            String uri = triple[0].substring(1);
            int to = triple[2].lastIndexOf("\"");
            String givenName = triple[2].substring(1, to);
            DBpediaResource personResource = dbPediaResources.get(uri);
            if (personResource != null) {
              String name = personResource.getName() + ", " + givenName;
              personResource.setName(name);
              personResource.setType("person");
            }
          }
        }
      }
      File dbPediaResourceGndFile = new File(dbPediaResourcesDirName + "/mappingbased-properties-individualisedGnd.ttl");
      Scanner s3 = new Scanner(dbPediaResourceGndFile, "utf-8");
      while (s3.hasNextLine()) {
        String line = s3.nextLine();
        if (! line.startsWith("#")) {
          String[] triple = line.split("> ");
          String uri = triple[0].substring(1);
          int to = triple[2].lastIndexOf("\"");
          String gnd = triple[2].substring(1, to);
          DBpediaResource dbpediaResource = dbPediaResources.get(uri);
          if (dbpediaResource != null) {
            dbpediaResource.setGnd(gnd);
          }
        }
      }
      s.close();
      s2.close();
      s3.close();
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  public Annotation annotate(Collection collection, String docId, String textInput, String confidence, int count) throws ApplicationException {
    if (! serviceAnnotateReachable) {
      String urlStr = SERVICE + "/" + "annotate";
      LOGGER.info("DBpediaSpotlightHandler: Service: " + urlStr + " not reachable");
      return null;
    }
    String testStr = "";
    String text = textInput.replaceAll("-[ \n]+|&#[0-9];|&#1[0-9];|&#2[0-9];|&#30;|&#31;|&#32;|\uffff", ""); // entferne Bindestriche vor Blank/Zeilenende und Steuerzeichen wie "&#0;"
    text = text.replaceAll("\n", " "); // new lines durch blank ersetzen (da sonst das Steuerzeichen &#10; erzeugt wird)
    NameValuePair textParam = new NameValuePair("text", text);
    NameValuePair confidenceParam = new NameValuePair("confidence", confidence);
    // NameValuePair spotterParam = new NameValuePair("spotter", "Default"); // other values: NESpotter, ...
    // NameValuePair typesParam = new NameValuePair("types", "Person"); // Person, Organisation, Place (Category ??) Einschränkung liefert evtl. zu wenige Entitäten
    // NameValuePair supportParam = new NameValuePair("support", "20"); // how many incoming links are on the DBpedia-Resource 
    // NameValuePair whitelistSparqlParam = new NameValuePair("sparql", "select ...");
    NameValuePair[] params = {textParam, confidenceParam};
    String spotlightAnnotationXmlStr = performPostRequest("annotate", params, "text/xml");
    spotlightAnnotationXmlStr = spotlightAnnotationXmlStr.replaceAll("&#[0-9];", "XXXX"); // ersetze Steuerzeichen durch X (dann bleibt die Länge erhalten)
    spotlightAnnotationXmlStr = spotlightAnnotationXmlStr.replaceAll("&#1[0-9];|&#2[0-9];|&#30;|&#31;|&#32;", "XXXXX"); // ersetze Steuerzeichen durch X (dann bleibt die Länge erhalten)
    List<DBpediaResource> resources = null; 
    try {
      XdmValue xmdValueResources = xQueryEvaluator.evaluate(spotlightAnnotationXmlStr, "//Resource[not(@URI = preceding::Resource/@URI)]");  // no double resources
      String annotationText = xQueryEvaluator.evaluateAsString(spotlightAnnotationXmlStr, "string(/Annotation/@text)"); 
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
          String surfaceFormStr = xQueryEvaluator.evaluateAsString(resourceStr, "string(/Resource/@surfaceForm)"); 
          int offset = Integer.valueOf(xQueryEvaluator.evaluateAsString(resourceStr, "string(/Resource/@offset)"));
          String surfaceFormContextStr = null;
          if (offset < annotationText.length()) {
            int leftContextBegin = offset - 20;
            if (leftContextBegin < 0)
              leftContextBegin = 0;
            int rightContextEnd = offset + surfaceFormStr.length() + 20;
            if (rightContextEnd > annotationText.length())
              rightContextEnd = annotationText.length();
            String surfaceFormLeftContextStr = annotationText.substring(leftContextBegin, offset); 
            String surfaceFormRightContextStr = annotationText.substring(offset + surfaceFormStr.length(), rightContextEnd); 
            surfaceFormContextStr = "(...) " + surfaceFormLeftContextStr + "||" + surfaceFormStr + "||" + surfaceFormRightContextStr + " (...)"; 
          } 
          String uriStrEscaped = StringUtils.deresolveXmlEntities(uriStr);
          uriStrEscaped = uriStrEscaped.replaceAll("'", "&apos;");
          String frequencyStr = xQueryEvaluator.evaluateAsString(spotlightAnnotationXmlStr, "count(//Resource[@URI = '" + uriStrEscaped + "'])");
          DBpediaResource r = new DBpediaResource();
          r.setUri(uriStr);
          DBpediaResource dbPediaResource = dbPediaResources.get(uriStr);
          if (dbPediaResource != null) {
            r.setName(dbPediaResource.getName());
            r.setType(dbPediaResource.getType());
            r.setGnd(dbPediaResource.getGnd());
          } else {
            int begin = uriStr.lastIndexOf("resource/") + 9;
            if (begin != -1) {
              String name = uriStr.substring(begin);
              name = name.replaceAll("_", " ");
              r.setName(name);
            }
          }
          boolean isProper = isProper(surfaceFormStr) && isProperUri(uriStr);
          if (supportStr != null && ! supportStr.isEmpty())
            r.setSupport(new Integer(supportStr));
          if (similarityStr != null && ! similarityStr.isEmpty())
            r.setSimilarity(new Double(similarityStr));
          String coverage = collection.getCoverage();
          if (r.getType() == null) {
            boolean isPresentOrganisationType = 
                typesStr.contains("DBpedia:Band") || typesStr.contains("Schema:MusicGroup") || 
                typesStr.contains("DBpedia:Broadcaster") || typesStr.contains("DBpedia:Company") ||
                typesStr.contains("DBpedia:SoccerClub") || typesStr.contains("DBpedia:PoliticalParty");
            if (typesStr == null || typesStr.trim().isEmpty()) {
              r.setType("concept");
            } else if (typesStr.contains("Person") && ! isPresentOrganisationType && ! typesStr.contains("SocialPerson") && ! typesStr.contains("Organization")) {
              r.setType("person");
            } else if (isPresentOrganisationType) {
              r.setType("organisation");
              if (coverage != null && ! coverage.contains("Gegenwart")) // only add music bands and companies, if project is in "Gegenwart"
                isProper = false;
            } else if (typesStr.contains("Organization") || typesStr.contains("Organisation")) {
              r.setType("organisation");
            } else if (typesStr.contains("Place")) {
              r.setType("place");
            } else {
              r.setType("concept");
            }
          }
          r.setFrequency(new Integer(frequencyStr));
          r.setContext(surfaceFormContextStr);
          if (isProper)
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
      annotation.setResources(mainResources);
    }
    return annotation;
  }
  
  public boolean isProper(String surfaceForm) {
    boolean isProper = true;
    if (surfaceForm.length() <= 2)
      isProper = false;
    else if (surfaceForm.contains("-") || surfaceForm.contains(".."))
      isProper = false;
    else if (germanStopwords.get(surfaceForm) != null)
      isProper = false;
    return isProper;
  }
  
  public boolean isProperUri(String dbPediaUri) {
    if (germanStopwords.get(dbPediaUri) != null)
      return false;
    boolean isProper = true;
    Enumeration<String> stopwordExpressions = germanStopwordExpressions.keys();
    String stopwordExpressionOR = "";
    while (stopwordExpressions.hasMoreElements()) {
      String stopwordExpression = stopwordExpressions.nextElement();
      stopwordExpressionOR = stopwordExpressionOR + stopwordExpression + "|";
    }
    if (! stopwordExpressionOR.isEmpty() && dbPediaUri.matches(stopwordExpressionOR)) {
      isProper = false;
    }
    return isProper;
  }

  private String performPostRequest(String serviceName, NameValuePair[] params, String outputFormat) throws ApplicationException {
    String resultStr = null;
    String urlStr = SERVICE + "/" + serviceName;
    try {
      PostMethod method = new PostMethod(urlStr);
      method.getParams().setContentCharset("utf-8");
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
      LOGGER.info("DBpediaSpotlightHandler: Service annotate: " + urlStr + " not reachable");
      throw new ApplicationException(e);
    } catch (IOException e) {
      LOGGER.info("DBpediaSpotlightHandler: Service annotate: " + urlStr + " not reachable");
      throw new ApplicationException(e);
    } catch (Exception e) {
      LOGGER.info("DBpediaSpotlightHandler: Service annotate: " + urlStr + " not reachable");
      throw new ApplicationException(e);
    }
    return resultStr;
  }

  private String performPostRequest(HttpClient httpClient, String serviceName, NameValuePair[] params, String outputFormat) throws ApplicationException {
    String resultStr = null;
    String urlStr = SERVICE + "/" + serviceName;
    try {
      PostMethod method = new PostMethod(urlStr);
      method.getParams().setContentCharset("utf-8");
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
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return resultStr;
  }
}
