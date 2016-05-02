package org.bbaw.wsp.cms.collections;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.tika.Tika;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Crawler {
  private static Crawler crawler;
  private static int SOCKET_TIMEOUT = 10 * 1000;
  private Tika tika;

  public static Crawler getInstance() throws ApplicationException {
    if(crawler == null) {
      crawler = new Crawler();
      crawler.init();
    }
    return crawler;
  }
  
  private void init() throws ApplicationException {
    tika = new Tika();
  }
  
  public ArrayList<String> crawl(String startUrlStr, Integer maxDepth, ArrayList<String> excludes) throws ApplicationException {
    Hashtable<String, String> urlsHashtable = new Hashtable<String, String>();
    ArrayList<String> allUrls = getUrls(startUrlStr, 1, maxDepth);
    allUrls.add(startUrlStr);
    for (String urlStr : allUrls) {
      if (urlsHashtable.get(urlStr) == null) {
        String urlStrRelative = urlStr.replaceFirst(startUrlStr, "");
        if (isAllowed(urlStrRelative, excludes))
          urlsHashtable.put(urlStr, urlStr);
      }
    }
    ArrayList<String> urls = new ArrayList<String>(urlsHashtable.values());
    urls.sort(null);
    return urls;
  }

  private ArrayList<String> getUrls(String startUrlStr, Integer depth, Integer maxDepth) throws ApplicationException {
    ArrayList<String> retUrls = new ArrayList<String>();
    try {
      URL startUrl = new URL(startUrlStr);
      Document doc = Jsoup.parse(startUrl, SOCKET_TIMEOUT);
      Elements links = doc.select("a[href]");
      if (links != null) {
        for (Element link : links) {
          String urlStr = link.attr("abs:href");
          if (urlStr.endsWith("/"))
            urlStr = urlStr.substring(0, urlStr.length() - 1);
          URL url = new URL(urlStr);
          URI uri = new URI(urlStr);
          String fragment = uri.getFragment();  // the "#" part of the uri
          String urlQuery = url.getQuery();
          if (url.getProtocol().startsWith("http") && fragment == null && urlQuery == null && urlStr.startsWith(startUrlStr)) {
            if (depth < maxDepth && isHtml(url)) {
              retUrls.add(urlStr);
              ArrayList<String> urls = getUrls(urlStr, depth + 1, maxDepth);
              retUrls.addAll(urls);
            } else {
              retUrls.add(urlStr);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return retUrls;
  }
  
  private boolean isAllowed(String urlStr, ArrayList<String> excludes) {
    boolean isAllowed = true;
    if (excludes == null)
      return true;
    for (int i=0;i<excludes.size();i++) {
      String exclude = excludes.get(i);
      if (urlStr.matches(exclude))
        return false;
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
