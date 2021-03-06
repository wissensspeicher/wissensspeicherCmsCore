package org.bbaw.wsp.cms.collections;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Crawler {
  private static int SOCKET_TIMEOUT = 10 * 1000;
  private static Integer DEFAULT_MAX_DEPTH = 3;
  private static Logger LOGGER = Logger.getLogger(Crawler.class);
  private String rootUrlStr;  // e.g. http://schleiermacher-in-berlin.bbaw.de
  private String rootUrlPathStr; // if rootUrl has a fileName part (e.g. http://schleiermacher-in-berlin.bbaw.de/index.html) then rootUrlPathStr is the path without the filename (e.g. http://schleiermacher-in-berlin.bbaw.de) 
  private String redirectRootUrlStr;
  private Integer maxDepth;
  private ArrayList<String> excludes;
  private ArrayList<String> includes;
  private Hashtable<String, MetadataRecord> urlsHashtable;
  private Tika tika;

  public Crawler(String rootUrlStr, Integer maxDepth, ArrayList<String> excludes, ArrayList<String> includes) throws ApplicationException {
    this.rootUrlStr = rootUrlStr;
    this.maxDepth = maxDepth;
    if (maxDepth == null || maxDepth == 0)
      this.maxDepth = DEFAULT_MAX_DEPTH;
    ArrayList<String> crawlerExcludes = ProjectReader.getInstance().getGlobalExcludes();
    if (crawlerExcludes != null && excludes != null)
      excludes.addAll(crawlerExcludes);
    else if (crawlerExcludes != null && excludes == null)
      excludes = crawlerExcludes;
    this.excludes = excludes;
    this.includes = includes;
    this.tika = new Tika();
    this.urlsHashtable = new Hashtable<String, MetadataRecord>();
  }
  
  public ArrayList<MetadataRecord> crawl() throws ApplicationException {
    rootUrlPathStr = getUrlPath(rootUrlStr); // e.g. "http://www.bbaw.de/forschung/moritz/biblio/index.html" delivers "http://www.bbaw.de/forschung/moritz/biblio/"
    redirectRootUrlStr = getRedirectUrl(rootUrlStr);  // e.g. redirects from "http://kant.bbaw.de/die-drei-kritiken" to "http://kant.bbaw.de/abteilung-i/die-drei-kritiken" or from "http://iffland.bbaw.de" to "http://iffland.bbaw.de/index.xql"
    if (redirectRootUrlStr == null)
      redirectRootUrlStr = rootUrlStr;
    getMdRecords(redirectRootUrlStr, 1);
    ArrayList<MetadataRecord> mdRecords = new ArrayList<MetadataRecord>(urlsHashtable.values());
    MetadataRecord rootMdRecord = getNewMdRecord(null, redirectRootUrlStr);
    if (rootMdRecord != null) {
      MetadataRecord mdRecord = urlsHashtable.get(redirectRootUrlStr);
      if (mdRecord == null) {
        urlsHashtable.put(redirectRootUrlStr, rootMdRecord);
        mdRecords.add(0, rootMdRecord);
      }
    }
    Comparator<MetadataRecord> mdRecordComparator = new Comparator<MetadataRecord>() {
      public int compare(MetadataRecord m1, MetadataRecord m2) {
        return m1.getWebUri().compareTo(m2.getWebUri());
      }
    };
    Collections.sort(mdRecords, mdRecordComparator);
    return mdRecords;
  }

  private String getRedirectUrl(String url) throws ApplicationException {
    String redirectUrlStr = null;
    try {
      Response response = Jsoup.connect(url).timeout(SOCKET_TIMEOUT).followRedirects(true).execute();
      URL redirectUrl = response.url();
      String redirectUrlStrTmp = redirectUrl.toString();
      if (! redirectUrlStrTmp.equals(url))
        redirectUrlStr = redirectUrlStrTmp;
    } catch (Exception e) {
      // nothing
    }
    return redirectUrlStr;
  }
  
  private void getMdRecords(String startUrlStr, Integer depth) throws ApplicationException {
    try {
      URL startUrl = new URL(startUrlStr);
      Document doc = null;
      try {
        doc = Jsoup.parse(startUrl, SOCKET_TIMEOUT);
      } catch (Exception e) {
        LOGGER.error("Crawler: " + startUrl + " couldn't be crawled: " + e.getMessage());
        e.printStackTrace();
      }
      if (doc != null) {
        Elements links = doc.select("a[href], area[href]");
        if (links != null) {
          ArrayList<MetadataRecord> allowedResourceMdRecords = new ArrayList<MetadataRecord>();
          for (Element link : links) {
            String urlStr = link.attr("abs:href");
            if (urlStr.endsWith("index.html"))
              urlStr = urlStr.substring(0, urlStr.length() - 10);
            if (urlStr.endsWith("index.htm"))
              urlStr = urlStr.substring(0, urlStr.length() - 9);
            if (urlStr.endsWith("/"))
              urlStr = urlStr.substring(0, urlStr.length() - 1);
            MetadataRecord mdRecord = urlsHashtable.get(urlStr);
            if (mdRecord == null) {
              boolean isAllowed = isAllowed(urlStr);
              if (isAllowed) {
                MetadataRecord newMdRecord = getNewMdRecord(startUrlStr, urlStr);
                if (newMdRecord != null) {
                  urlsHashtable.put(urlStr, newMdRecord);
                  String mimeType = newMdRecord.getType();
                  if (mimeType != null && isHtml(mimeType))
                    allowedResourceMdRecords.add(newMdRecord);
                }
              }
            }
          }
          if (depth < maxDepth) {
            for (int i=0; i<allowedResourceMdRecords.size(); i++) {
              MetadataRecord mdRecord = allowedResourceMdRecords.get(i);
              String urlStr = mdRecord.getWebUri();
              getMdRecords(urlStr, depth + 1);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
  }

  private MetadataRecord getNewMdRecord(String startUrlStr, String urlStr) {
    MetadataRecord newMdRecord = null;
    URL url = null;
    try {
      url = new URL(urlStr);
      String mimeType = detect(startUrlStr, url);
      if (mimeType == null)
        return null;
      if (isBinaryMimeType(mimeType))
        return null;
      newMdRecord = new MetadataRecord();
      newMdRecord.setWebUri(urlStr);
      newMdRecord.setType(mimeType);
    } catch (Exception e) {
      LOGGER.error("Crawler: detection of link: " + urlStr + " on page: " + startUrlStr + " is not possible");
      e.printStackTrace();
    }
    return newMdRecord;
  }
  
  static public String getUrlPath(String urlStr) throws ApplicationException {
    String urlPath = urlStr;
    try {
      URL url = new URL(urlStr);
      String urlFile = url.getPath(); // only path (without query part)
      String urlQuery = url.getQuery();
      String fileExtension = FilenameUtils.getExtension(urlFile);
      if (! fileExtension.isEmpty() || urlQuery != null)
        urlPath = url.getProtocol() + "://" + url.getHost() + FilenameUtils.getFullPath(urlFile);
    } catch (MalformedURLException e) {
      LOGGER.error("Crawler: Crawl Database Id: " + urlStr + " is malformed");
      return null;
    }
    return urlPath;
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
      boolean isBinary = isBinary(urlStr);
      if (isBinary)
        return false;
      String fragment = uri.getFragment();  // the "#" part of the uri
      if (fragment != null)
        return false;
      String rootUrlStrWithoutProtocol = rootUrlStr.replaceAll("https*://", "");
      if (rootUrlStrWithoutProtocol.endsWith("/"))
        rootUrlStrWithoutProtocol = rootUrlStrWithoutProtocol.substring(0, rootUrlStrWithoutProtocol.length() - 1);
      String rootUrlPathStrWithoutProtocol = rootUrlPathStr.replaceAll("https*://", "");
      String redirectRootUrlStrWithoutProtocol = redirectRootUrlStr.replaceAll("https*://", "");
      String urlStrWithoutProtocol = urlStr.replaceAll("https*://", "");
      String urlStrRelative = urlStrWithoutProtocol.replaceFirst(rootUrlStrWithoutProtocol, "");
      // includes are stronger than excludes 
      if (includes != null) {
        for (int i=0;i<includes.size();i++) {
          String include = includes.get(i);
          if (urlStr.matches(include) || urlStrRelative.matches(include)) // full (in other url domains) and relative matches
            return true;
        }
      }
      if (! urlStrWithoutProtocol.startsWith(rootUrlStrWithoutProtocol) && ! urlStrWithoutProtocol.startsWith(redirectRootUrlStrWithoutProtocol) && ! urlStrWithoutProtocol.startsWith(rootUrlPathStrWithoutProtocol))
        return false;
      if (urlStr.contains(".."))
        return false;
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
  
  private boolean isBinary(String urlStr) throws ApplicationException {
    String urlS = urlStr.toLowerCase();
    if (urlS.endsWith(".bin") || urlS.endsWith(".bmp") || urlS.endsWith(".cdr") || urlS.endsWith(".com") || urlS.endsWith(".dat") || urlS.endsWith(".eps") 
        || urlS.endsWith(".exe") || urlS.endsWith(".gif") || urlS.endsWith(".gz") || urlS.endsWith(".jpg") || urlS.endsWith(".jpeg") || urlS.endsWith(".mdb") 
        || urlS.endsWith(".mid") || urlS.endsWith(".mp3") || urlS.endsWith(".mp4") || urlS.endsWith(".mov") || urlS.endsWith(".mpg") || urlS.endsWith(".ogg") 
        || urlS.endsWith(".png") || urlS.endsWith(".ppt") || urlS.endsWith(".swf") || urlS.endsWith(".sys") || urlS.endsWith(".tif") || urlS.endsWith(".wav") 
        || urlS.endsWith(".wmf") || urlS.endsWith(".zip"))
      return true;
    else 
      return false;
  }
  
  private boolean isBinaryMimeType(String mimeTypeStr) throws ApplicationException {
    String mimeTypeStrL = mimeTypeStr.toLowerCase();
    if (mimeTypeStrL.contains("application/ogg") || mimeTypeStrL.contains("gif") || mimeTypeStrL.contains("jpeg") ||  mimeTypeStrL.contains("mp3") || mimeTypeStrL.contains("mpeg") 
        || mimeTypeStrL.contains("png") || mimeTypeStrL.contains("powerpoint") || mimeTypeStrL.contains("zip"))
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

  private String detect(String startUrlStr, URL url) throws ApplicationException {
    String mimeType = null;
    try {
      URLConnection connection = url.openConnection();
      connection.setConnectTimeout(SOCKET_TIMEOUT);
      connection.setReadTimeout(SOCKET_TIMEOUT);
      mimeType = connection.getContentType();
      if (mimeType != null && mimeType.trim().isEmpty())
        return null;
      if (mimeType != null && mimeType.contains(";"))
        mimeType = mimeType.substring(0, mimeType.indexOf(";"));
    } catch (IOException e) {
      LOGGER.error("Crawler: detection of link: " + url + " on page: " + startUrlStr + " is not possible");
      return null; // e.g. if FileNotFoundException etc. is thrown by site
    }
    return mimeType;
  }

  /**
   * has no timeout: so URLConnection is used instead
   * @param startUrlStr
   * @param url
   * @return
   * @throws ApplicationException
   */
  private String detectByTika(String startUrlStr, URL url) throws ApplicationException {
    String mimeType = null;
    try {
      mimeType = tika.detect(url);
      if (mimeType != null && mimeType.contains(";"))
        mimeType = mimeType.replaceAll(";.*", "");
    } catch (Exception e) {
      LOGGER.error("Crawler: detection of link: " + url + " on page: " + startUrlStr + " is not possible");
      return null; // e.g. if FileNotFoundException etc. is thrown by site
    }
    return mimeType;
  }
  
  /**
   * is a little bit slower than tika and URLConnection: so URLConnection is used instead
   * @param startUrlStr
   * @param urlStr
   * @return mime type
   * @throws ApplicationException
   */
  private String detectByJsoup(String startUrlStr, String urlStr) throws ApplicationException {
    String mimeType = null;
    try {
      Response resp = Jsoup.connect(urlStr).timeout(SOCKET_TIMEOUT).execute();
      mimeType = resp.contentType(); 
      if (mimeType != null && mimeType.contains(";"))
        mimeType = mimeType.substring(0, mimeType.indexOf(";"));
    } catch (Exception e) {
      LOGGER.error("Crawler: detection of link: " + urlStr + " on page: " + startUrlStr + " is not possible");
      return null; 
    }
    return mimeType;
  }
  
}
