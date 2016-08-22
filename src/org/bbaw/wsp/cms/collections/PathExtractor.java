package org.bbaw.wsp.cms.collections;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

/**
 * eXist-specific Crawler
 * extracts the paths of Documents in a http manner
 * 
 * @author marco juergens
 *
 */
public class PathExtractor {
  private static Logger LOGGER = Logger.getLogger(PathExtractor.class);
  private String rootUrlStr;
  private ArrayList<String> excludes;
  private List<String> ressourceLoc = new ArrayList<String>();;

  public PathExtractor(String rootUrlStr, ArrayList<String> excludes) {
    this.rootUrlStr = rootUrlStr;
    this.excludes = excludes;
  }

  public List<String> extract() {
    // parameter necessary, because it's recursive, thus changing the uri
    extractDocLocations(rootUrlStr);
    return this.ressourceLoc;
  }

  /**
   * recursive Method to extract the path of the resources
   * 
   * @param startUrl
   */
  private void extractDocLocations(String startUrl) {
    HttpClient client = new DefaultHttpClient();
    HttpGet httpget = new HttpGet(startUrl);
    HttpResponse resp = null;
    try {
      resp = client.execute(httpget);
    } catch (IOException e) {
      LOGGER.error("Operation failed for: " + startUrl + ". " + e.getMessage());
      e.printStackTrace();
      return;
    }
    HttpEntity entity = resp.getEntity();
    if (entity != null) {
      XMLInputFactory iFactory = XMLInputFactory.newInstance();
      XMLStreamReader reader = null;
      try {
        reader = iFactory.createXMLStreamReader(entity.getContent());
      } catch (IllegalStateException e1) {
        e1.printStackTrace();
      } catch (XMLStreamException e1) {
        e1.printStackTrace();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      try {
        while (true) {
          int event = reader.next();
          if (event == XMLStreamConstants.END_DOCUMENT) {
            reader.close();
            break;
          }
          if (event == XMLStreamConstants.START_ELEMENT) {
            String nameAttributeValue = reader.getAttributeValue(null, "name"); 
            // e.g. attr name in <exist:collection name="/db/apps/SadeRegistres/data/protokolle" created="2015-03-26T11:49:00.69+01:00" owner="admin" group="dba" permissions="rwxrwxr-x"> or
            // e.g. attr name in <exist:collection name="protokolle" created="2015-03-26T11:49:00.69+01:00" owner="admin" group="dba" permissions="rwxrwxr-x"> or
            if ((nameAttributeValue) != null) {
              if (reader.getLocalName().equals("collection") && ! (startUrl.endsWith(nameAttributeValue))) {
                String extractUrlStr = "";
                if (nameAttributeValue.startsWith("/")) {
                  extractUrlStr = startUrl + nameAttributeValue;
                } else {
                  if (! startUrl.endsWith("/")) {
                    extractUrlStr = startUrl + "/" + nameAttributeValue;
                  } else {
                    extractUrlStr = startUrl + nameAttributeValue;
                  }
                }
                boolean urlIsExcluded = isExcluded(extractUrlStr);
                if (! urlIsExcluded) {
                  client.getConnectionManager().closeExpiredConnections();
                  extractDocLocations(extractUrlStr);
                }
              }
              if (reader.getLocalName().equals("resource")) {
                String url = startUrl + "/" + nameAttributeValue;
                // e.g. attr name in <exist:resource name="0723-1763_06_16.xml" created="2015-03-26T11:49:00.694+01:00" last-modified="2015-03-26T11:49:00.694+01:00" owner="admin" group="dba" permissions="rw-rw-r--"/>
                if (startUrl.endsWith("/"))
                  url = startUrl + nameAttributeValue;
                boolean urlIsExcluded = isExcluded(url);
                if (! urlIsExcluded) {
                  try {
                    url = URLDecoder.decode(url, "utf-8"); // decodes the "%HexHex" chars into unicode chars e.g. "%20" to " "
                  } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                  }
                  ressourceLoc.add(url);
                }
              }
            }
          }
          if (event == XMLStreamConstants.ATTRIBUTE) {
            // nothing
          }
        }
      } catch (XMLStreamException e) {
        e.printStackTrace();
      }
    }
  }

  private boolean isExcluded(String urlStr) {
    boolean isExcluded = false;
    String urlStrRelative  = urlStr.replaceAll(rootUrlStr, "");
    if (excludes != null && ! excludes.isEmpty()) {
      for (int i=0; i<excludes.size(); i++) {
        String exclude = excludes.get(i);
        if (urlStrRelative.matches(exclude))
          return true;
      }
    }
    return isExcluded;
  }
}
