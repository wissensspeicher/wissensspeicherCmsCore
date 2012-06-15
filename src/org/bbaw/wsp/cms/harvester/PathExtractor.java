package org.bbaw.wsp.cms.harvester;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

public class PathExtractor {

  private List<String> ressourceLoc;
  PrintWriter out;

  public PathExtractor() {

  }

  public List<String> initExtractor(String startingUri) {

    ressourceLoc = new ArrayList<String>();
    // parameter necessery, because it's recursive, thus changing the uri
    extractDocLocations(startingUri);
    System.out.println("extracing resource locations done.");
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
      e.printStackTrace();
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
            if ((reader.getAttributeValue(null, "name")) != null) {
              if (!(reader.getAttributeValue(null, "name")).contains(".") && !(startUrl.endsWith(reader.getAttributeValue(null, "name")))) {
                if (reader.getAttributeValue(null, "name").startsWith("/")) {
                  client.getConnectionManager().closeExpiredConnections();
                  extractDocLocations(startUrl + reader.getAttributeValue(null, "name"));
                } else {
                  client.getConnectionManager().closeExpiredConnections();
                  if (!startUrl.endsWith("/")) {
                    extractDocLocations(startUrl + "/" + reader.getAttributeValue(null, "name"));
                  } else {
                    extractDocLocations(startUrl + reader.getAttributeValue(null, "name"));
                  }
                }
              }
              if (reader.getAttributeValue(null, "name").endsWith(".xml")) {
                if (!startUrl.endsWith("/")) {
                  ressourceLoc.add(startUrl + "/" + reader.getAttributeValue(null, "name"));
                } else {
                  ressourceLoc.add(startUrl + reader.getAttributeValue(null, "name"));
                }
              }
            }
          }
          if (event == XMLStreamConstants.ATTRIBUTE) {
            // System.out.println("tss "+reader.getLocalName());
          }
        }
      } catch (XMLStreamException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * extrahiert ebenso wie extractDocLocations(String startUri) Pfade, tut dies
   * aber local und nicht über HTTP
   * 
   * @return
   */
  public List<String> extractPathLocally(String startUrl) {
    List<String> pathList = new ArrayList<String>();

    // home verzeichnis pfad über system variable
    // String loc = System.getenv("HOME")+"/wsp/configs";
    // out.println("hom variable + conf datei : "+loc);
    File f = new File(startUrl);
    // out.println("readable : "+Boolean.toString(f.canRead()));
    // out.println("readable : "+f.isDirectory());
    if (f.isDirectory()) {
      File[] filelist = f.listFiles();
      for (File file : filelist) {
        if (file.getName().toLowerCase().contains("config")) {
          if (!startUrl.endsWith("/")) {
            pathList.add(startUrl + "/" + file.getName());
          } else {
            pathList.add(startUrl + file.getName());
          }
        }
      }
    }
    return pathList;
  }

}
