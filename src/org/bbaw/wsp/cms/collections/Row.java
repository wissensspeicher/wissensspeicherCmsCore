package org.bbaw.wsp.cms.collections;

import java.util.Hashtable;

public class Row {
  private Hashtable<String, String> fields;
  
  public void addField(String fieldName, String fieldValue) {
    if (fields == null)
      fields = new Hashtable<String, String>();
    String val = fields.get(fieldName);
    if (val == null)
      fields.put(fieldName, fieldValue);
  }
  
  public String getFieldValue(String fieldName) {
    if (fields == null)
      return null;
    else return fields.get(fieldName);
  }
  
}
