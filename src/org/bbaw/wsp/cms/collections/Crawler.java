package org.bbaw.wsp.cms.collections;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import org.apache.tika.Tika;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Crawler {
  private static int SOCKET_TIMEOUT = 10 * 1000;
  private static Integer DEFAULT_MAX_DEPTH = 3;
  private String rootUrlStr;
  private Integer maxDepth;
  private ArrayList<String> excludes;
  private Hashtable<String, MetadataRecord> urlsHashtable;
  private Tika tika;

  public Crawler(String rootUrlStr, Integer maxDepth, ArrayList<String> excludes) throws ApplicationException {
    this.rootUrlStr = rootUrlStr;
    this.maxDepth = maxDepth;
    if (maxDepth == null)
      this.maxDepth = DEFAULT_MAX_DEPTH;
    this.excludes = excludes;
    this.tika = new Tika();
    this.urlsHashtable = new Hashtable<String, MetadataRecord>();
  }
  
  public ArrayList<MetadataRecord> crawl() throws ApplicationException {
    ArrayList<MetadataRecord> mdRecords = getMdRecords(rootUrlStr, 1);
    /*
    if (urlsHashtable.get(rootUrlStr) == null) {
      MetadataRecord mdRecord = new MetadataRecord();
      mdRecord.setWebUri(rootUrlStr);
      urlsHashtable.put(rootUrlStr, mdRecord);
      mdRecords.add(mdRecord);
    }
    */
    Comparator<MetadataRecord> mdRecordComparator = new Comparator<MetadataRecord>() {
      public int compare(MetadataRecord m1, MetadataRecord m2) {
        return m1.getWebUri().compareTo(m2.getWebUri());
      }
    };
    Collections.sort(mdRecords, mdRecordComparator);
    return mdRecords;
  }

  private ArrayList<MetadataRecord> getMdRecords(String startUrlStr, Integer depth) throws ApplicationException {
    ArrayList<MetadataRecord> retMdRecords = new ArrayList<MetadataRecord>();
    try {
      URL startUrl = new URL(startUrlStr);
      Document doc = Jsoup.parse(startUrl, SOCKET_TIMEOUT);
      Elements links = doc.select("a[href], area[href]");
      if (links != null) {
        for (Element link : links) {
          String urlStr = link.attr("abs:href");
          if (urlStr.endsWith("index.html"))
            urlStr = urlStr.substring(0, urlStr.length() - 10);
          if (urlStr.endsWith("index.htm"))
            urlStr = urlStr.substring(0, urlStr.length() - 9);
          if (urlStr.endsWith("/"))
            urlStr = urlStr.substring(0, urlStr.length() - 1);
          URL url = new URL(urlStr);
          boolean isAllowed = isAllowed(urlStr);
          if (isAllowed) {
            if (urlsHashtable.get(urlStr) == null) {
              MetadataRecord mdRecord = new MetadataRecord();
              mdRecord.setWebUri(urlStr);
              urlsHashtable.put(urlStr, mdRecord);
              retMdRecords.add(mdRecord);
              if (depth < maxDepth && isHtml(url)) {
                ArrayList<MetadataRecord> mdRecords = getMdRecords(urlStr, depth + 1);
                retMdRecords.addAll(mdRecords);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return retMdRecords;
  }
  
  private boolean isAllowed(String urlStr) throws ApplicationException {
    boolean isAllowed = true;
    try {
      if (excludes == null)
        return true;
      URL url = new URL(urlStr);
      String protocol = url.getProtocol();
      if (protocol == null || (protocol != null && ! protocol.startsWith("http")))
        return false;
      URI uri = new URI(urlStr);
      String fragment = uri.getFragment();  // the "#" part of the uri
      if (fragment != null)
        return false;
      if (! urlStr.startsWith(rootUrlStr))
        return false;
      String urlStrRelative = urlStr.replaceFirst(rootUrlStr, "");
      for (int i=0;i<excludes.size();i++) {
        String exclude = excludes.get(i);
        if (urlStrRelative.matches(exclude))
          return false;
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return isAllowed;
  }
  
  private boolean isHtml(URL url) throws ApplicationException {
    boolean retValue = false;
    try {
      String mimeType = tika.detect(url);
      if (mimeType.contains("html"))
        return true;
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return retValue;
  }
}
