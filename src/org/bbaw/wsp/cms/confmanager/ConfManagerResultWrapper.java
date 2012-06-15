package org.bbaw.wsp.cms.confmanager;

import java.util.ArrayList;
import java.util.List;

public class ConfManagerResultWrapper {

  private List<String> collectionUrls;
  private String collectionId;
  private String collectionName;
  private String collectionDataUrl;
  private String mainLanguage;
  private ArrayList<String> fields;
  private List<String> formats;
  private List<String> excludeField;

  
  public ConfManagerResultWrapper(){
    collectionUrls = new ArrayList<String>();
    collectionId = "";
    collectionName = "";
    collectionDataUrl = "";
    mainLanguage = "";
    fields = new ArrayList<String>();
    formats = new ArrayList<String>();
    excludeField = new ArrayList<String>();
  }
  
  public List<String> getCollectionUrls() {
    return collectionUrls;
  }

  public String getCollectionId() {
    return collectionId;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public void setCollectionId(String collectionId){
    this.collectionId = collectionId;
  }
  
  public void setCollectionUrls(List<String> collectionUrls){
    this.collectionUrls = collectionUrls;
  }

  public void setCollectionName(String collectionName) {
    this.collectionName = collectionName;
  }

  public String getCollectiondataUrl() {
    return collectionDataUrl;
  }

  public void setCollectionDataUrl(String collectiondataUrl) {
    this.collectionDataUrl = collectiondataUrl;
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
