package org.bbaw.wsp.cms.mdsystem.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class WspJsonEncoder {

  private static WspJsonEncoder wspJsonEncoder;
  private JSONObject jsonObj;
  
  public WspJsonEncoder(){
      jsonObj = new JSONObject();
  }
  
  public void putStrings(String key, String value){
      jsonObj.put(key, value);
  }
  
  public void putJsonObj(String mapName, JSONArray array){
      jsonObj.put(mapName, array);
  }
  
  public String encode(){
      String jsonText = JSONValue.toJSONString(jsonObj);
      System.out.print(jsonText);
      return jsonText;
  }
  
  public static WspJsonEncoder getInstance(){
      if(wspJsonEncoder == null){
          wspJsonEncoder = new WspJsonEncoder();
      }
      return wspJsonEncoder;
  }
  
  public JSONObject getJsonObject(){
      return jsonObj;
  }
  
  public void clear(){
      jsonObj = new JSONObject();;
  }
}
