package org.bbaw.wsp.cms.confmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Collection {
  private List<String> urls;
  private String id;
  private String name;
  private String dataUrl;
  private String mainLanguage;
  private ArrayList<String> fields;
  private List<String> formats;
  private List<String> excludeField;
  private HashMap<String, ArrayList<String>> registers;
  
  public Collection(){
    urls = new ArrayList<String>();
    id = "";
    name = "";
    dataUrl = "";
    mainLanguage = "";
    fields = new ArrayList<String>();
    formats = new ArrayList<String>();
    excludeField = new ArrayList<String>();
    registers = new HashMap<String, ArrayList<String>>();
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

  public void setRegister(String registerName, ArrayList<String> entries) {
    registers.put(registerName, entries);
  }
  
  public ArrayList<String> getRegisterEntries(String registerName) {
    return registers.get(registerName);
  }

  public String getRegisterLink(String registerName, String registerValue) throws ApplicationException {
    String retRegLink = null;
    ArrayList<String> regs = registers.get(registerName);
    String reg = null;
    if (regs != null && regs.size() >= 1) {
      reg = regs.get(0);  // get first one
      String valueQuery = "";
      if (registerValue != null && ! registerValue.isEmpty()) {
        valueQuery = valueQuery + "+tokenOrig:(";
        String[] registerValueTerms = registerValue.split(" ");
        for (int i=0; i<registerValueTerms.length; i++) {
          String term = registerValueTerms[i];
          valueQuery = valueQuery + " +" + term;
        }
        valueQuery = valueQuery + ")";
      }
      String query = "+elementName:" + registerName + " " + valueQuery;
      try {
        query = URIUtil.encodePath(query, "utf-8");
      } catch (URIException e) {
        throw new ApplicationException(e);
      }
      retRegLink = "/wspCmsWebApp/query/QueryDocument?docId=" + reg + "&query=" + query;
    }
    return retRegLink;
  }

}
