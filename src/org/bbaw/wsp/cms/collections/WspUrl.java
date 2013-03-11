package org.bbaw.wsp.cms.collections;

public class WspUrl {
  private String type; // default: null (no type), or eXistDir
  private String url;  // url 
  
  public WspUrl(String url) {
    this.url = url;
  }
  
  public String getType() {
    return type;
  }

  public String getUrl() {
    return url;
  }

  public void setType(String type) {
    this.type = type;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isEXistDir() {
    boolean isEXIstDir = false;
    if (type != null && type.toLowerCase().equals("existdir"))
      isEXIstDir = true;
    return isEXIstDir;
  }
}
