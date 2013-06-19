package org.bbaw.wsp.cms.collections;

import java.util.Hashtable;

public class Database {
  private String name;
  private String xmlDumpFileName;
  private String mainResourcesTable;
  private String mainResourcesTableId;
  private String webIdPreStr;
  private String webIdAfterStr;
  private Hashtable<String, String> dcField2dbField = new Hashtable<String, String>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public String getXmlDumpFileName() {
    return xmlDumpFileName;
  }

  public void setXmlDumpFileName(String xmlDumpFileName) {
    this.xmlDumpFileName = xmlDumpFileName;
  }

  public String getMainResourcesTable() {
    return mainResourcesTable;
  }
  
  public void setMainResourcesTable(String mainResourcesTable) {
    this.mainResourcesTable = mainResourcesTable;
  }

  public String getMainResourcesTableId() {
    return mainResourcesTableId;
  }

  public void setMainResourcesTableId(String mainResourcesTableId) {
    this.mainResourcesTableId = mainResourcesTableId;
  }

  public String getWebIdPreStr() {
    return webIdPreStr;
  }

  public void setWebIdPreStr(String webIdPreStr) {
    this.webIdPreStr = webIdPreStr;
  }

  public String getWebIdAfterStr() {
    return webIdAfterStr;
  }

  public void setWebIdAfterStr(String webIdAfterStr) {
    this.webIdAfterStr = webIdAfterStr;
  }

  public void addField(String dcField, String dbField) {
    dcField2dbField.put(dcField, dbField);
  }
  
  public String getDbField(String dcField) {
    return dcField2dbField.get(dcField);
  }
}