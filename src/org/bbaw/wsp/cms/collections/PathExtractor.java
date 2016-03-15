package org.bbaw.wsp.cms.collections;

import java.io.File;
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
  private List<String> ressourceLoc;
  private String excludes;

  public PathExtractor() {

  }

  public List<String> initExtractor(String startingUri, String excludes) {
    this.excludes = excludes;
    ressourceLoc = new ArrayList<String>();
    // parameter necessary, because it's recursive, thus changing the uri
    extractDocLocations(startingUri);
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
            if ((nameAttributeValue) != null) {
              if (reader.getLocalName().equals("collection") && !(startUrl.endsWith(nameAttributeValue))) {
                if (! isNameExcluded(nameAttributeValue.toLowerCase())) {
                  if (nameAttributeValue.startsWith("/")) {
                    client.getConnectionManager().closeExpiredConnections();
                      extractDocLocations(startUrl + nameAttributeValue);
                  } else {
                    client.getConnectionManager().closeExpiredConnections();
                    if (! startUrl.endsWith("/")) {
                      extractDocLocations(startUrl + "/" + nameAttributeValue);
                    } else {
                      extractDocLocations(startUrl + nameAttributeValue);
                    }
                  }
                }
              }
              if (reader.getLocalName().equals("resource")) {
                String url = startUrl + "/" + nameAttributeValue;
                if (startUrl.endsWith("/"))
                  url = startUrl + nameAttributeValue;
                boolean startUrlIsExcluded = isExcluded(url); // if exclude contains a full file name e.g. verzeichnisse/personenkorrektur.xml
                if (! startUrlIsExcluded) {
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

  private boolean isExcluded(String url) {
    boolean isExcluded = false;
    if (excludes != null && ! excludes.isEmpty() && url != null) {
      String[] exludeArrayStr = excludes.split(" ");
      for (int i=0; i<exludeArrayStr.length; i++) {
        String exclude = exludeArrayStr[i];
        if (url.endsWith(exclude))
          return true;
      }
    }
    return isExcluded;
  }
  
  private boolean isNameExcluded(String name) {
    boolean isExcluded = false;
    if (excludes != null && name != null) {
      String[] exludeArrayStr = excludes.split(" ");
      for (int i=0; i<exludeArrayStr.length; i++) {
        String exclude = exludeArrayStr[i];
        if (name.equals(exclude))
          return true;
      }
    }
    return isExcluded;
  }
}
