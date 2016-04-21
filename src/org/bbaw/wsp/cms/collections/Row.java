package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;
import java.util.Hashtable;

public class Row {
  private Hashtable<String, ArrayList<String>> fields;
  
  public void addField(String fieldName, String fieldValue) {
    if (fields == null) {
      fields = new Hashtable<String, ArrayList<String>>();
    }
    ArrayList<String> values = fields.get(fieldName);
    if (values == null) {
      values = new ArrayList<String>();
    }
    values.add(fieldValue);
    fields.put(fieldName, values);
  }
  
  public ArrayList<String> getFieldValues(String fieldName) {
    if (fields == null)
      return null;
    else return fields.get(fieldName);
  }
  
  public String getFirstFieldValue(String fieldName) {
    if (fields == null)
      return null;
    ArrayList<String> fieldValues = fields.get(fieldName);
    if (fieldValues == null)
      return null;
    else 
      return fieldValues.get(0);
  }
}
