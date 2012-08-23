package org.bbaw.wsp.cms.confmanager;

import java.util.ArrayList;
import java.util.List;

public class Collection {
  private List<String> urls;
  private String id;
  private String name;
  private String dataUrl;
  private String mainLanguage;
  private ArrayList<String> fields;
  private List<String> formats;
  private List<String> excludeField;
  
  public Collection(){
    urls = new ArrayList<String>();
    id = "";
    name = "";
    dataUrl = "";
    mainLanguage = "";
    fields = new ArrayList<String>();
    formats = new ArrayList<String>();
    excludeField = new ArrayList<String>();
  }
  
  public List<String> getUrls() {
    return urls;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  public void setUrls(List<String> urls){
    this.urls = urls;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCollectiondataUrl() {
    return dataUrl;
  }

  public void setDataUrl(String dataUrl) {
    this.dataUrl = dataUrl;
  }

  public List<String> getFormats() {
    return formats;
  }

  public void setFormats(List<String> formats) {
    this.formats = formats;
  }

  public List<String> getExcludeField() {
    return excludeField;
  }

  public void setExcludeField(List<String> excludeField) {
    this.excludeField = excludeField;
  }
  
  public String getMainLanguage() {
    return mainLanguage;
  }

  public void setMainLanguage(String mainLanguage) {
    this.mainLanguage = mainLanguage;
  }

  public ArrayList<String> getFields() {
    return fields;
  }

  public void setFields(ArrayList<String> fields) {
    this.fields = fields;
  }
}
