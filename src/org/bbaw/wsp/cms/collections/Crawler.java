package org.bbaw.wsp.cms.collections;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import org.apache.log4j.Logger;
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
  private static Logger LOGGER = Logger.getLogger(Crawler.class);
  private String rootUrlStr;
  private Integer maxDepth;
  private ArrayList<String> excludes;
  private Hashtable<String, MetadataRecord> urlsHashtable;
  private Tika tika;

  public Crawler(String rootUrlStr, Integer maxDepth, ArrayList<String> excludes) throws ApplicationException {
    this.rootUrlStr = rootUrlStr;
    this.maxDepth = maxDepth;
    if (maxDepth == null || maxDepth == 0)
      this.maxDepth = DEFAULT_MAX_DEPTH;
    ArrayList<String> crawlerExcludes = CollectionReader.getInstance().getCrawlerExcludes();
    if (crawlerExcludes != null && excludes != null)
      excludes.addAll(crawlerExcludes);
    else if (crawlerExcludes != null && excludes == null)
      excludes = crawlerExcludes;
    this.excludes = excludes;
    this.tika = new Tika();
    this.urlsHashtable = new Hashtable<String, MetadataRecord>();
  }
  
  public ArrayList<MetadataRecord> crawl() throws ApplicationException {
    ArrayList<MetadataRecord> mdRecords = getMdRecords(rootUrlStr, 1);
    MetadataRecord rootMdRecord = getNewMdRecord(rootUrlStr);
    if (rootMdRecord != null)
      mdRecords.add(0, rootMdRecord);
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
      Document doc = null;
      try {
        doc = Jsoup.parse(startUrl, SOCKET_TIMEOUT);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (doc != null) {
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
            boolean isAllowed = isAllowed(urlStr);
            if (isAllowed) {
              MetadataRecord mdRecord = urlsHashtable.get(urlStr);
              String mimeType = null;
              if (mdRecord == null) {
                MetadataRecord newMdRecord = getNewMdRecord(urlStr);
                if (newMdRecord != null) {
                  urlsHashtable.put(urlStr, newMdRecord);
                  retMdRecords.add(newMdRecord);
                }
              } else {
                mimeType = mdRecord.getType();             
              }
              if (depth < maxDepth && isHtml(mimeType)) {
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

  private MetadataRecord getNewMdRecord(String urlStr) {
    MetadataRecord newMdRecord = null;
    URL url = null;
    try {
      url = new URL(urlStr);
      String mimeType = detect(url);
      newMdRecord = new MetadataRecord();
      newMdRecord.setWebUri(urlStr);
      newMdRecord.setType(mimeType);
    } catch (Exception e) {
      LOGGER.error("Crawler: " + url);
      e.printStackTrace();
    }
    return newMdRecord;
  }
  
  private boolean isAllowed(String urlStr) throws ApplicationException {
    boolean isAllowed = true;
    if (urlStr == null || urlStr.isEmpty())
      return false;
    try {
      URL url = null;
      try {
        url = new URL(urlStr);
      } catch (MalformedURLException e) {
        return false;
      }
      String protocol = url.getProtocol();
      if (protocol == null || (protocol != null && ! protocol.startsWith("http")))
        return false;
      URI uri = null;
      try {
        uri = new URI(urlStr);
      } catch (URISyntaxException e) {
        return false;
      }
      boolean isMultimedia = isMultimedia(urlStr);
      if (isMultimedia)
        return false;
      String fragment = uri.getFragment();  // the "#" part of the uri
      if (fragment != null)
        return false;
      String rootUrlStrWithoutProtocol = rootUrlStr.replaceAll("https*://", "");
      String urlStrWithoutProtocol = urlStr.replaceAll("https*://", "");
      if (! urlStrWithoutProtocol.startsWith(rootUrlStrWithoutProtocol))
        return false;
      if (urlStr.contains(".."))
        return false;
      String urlStrRelative = urlStrWithoutProtocol.replaceFirst(rootUrlStrWithoutProtocol, "");
      if (excludes == null)
        return true;
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
  
  private boolean isMultimedia(String urlStr) throws ApplicationException {
    if (urlStr.endsWith(".jpg") || urlStr.endsWith(".jpeg") || urlStr.endsWith(".png") || urlStr.endsWith(".gif") || urlStr.endsWith(".mp3") || urlStr.endsWith(".mov") || urlStr.endsWith(".mp4") || urlStr.endsWith(".mp4"))
      return true;
    else 
      return false;
  }
  
  private boolean isHtml(String mimeType) throws ApplicationException {
    if (mimeType != null && mimeType.contains("html"))
      return true;
    else return
      false;
  }

  private String detect(URL url) throws ApplicationException {
    String mimeType = null;
    try {
      mimeType = tika.detect(url);
    } catch (Exception e) {
      return null; // e.g. if FileNotFoundException etc. is thrown by site
    }
    return mimeType;
  }
  
}
