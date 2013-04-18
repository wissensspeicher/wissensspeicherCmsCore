package org.bbaw.wsp.cms.document;

public class XQuery {
  private String name;
  private String code;
  private String result;
  private String preStr;
  private String afterStr;
  
  public XQuery(String name, String code) {
    this.name = name;
    this.code = code;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getCode() {
    return code;
  }
  public void setCode(String code) {
    this.code = code;
  }
  public String getPreStr() {
    return preStr;
  }

  public void setPreStr(String preStr) {
    this.preStr = preStr;
  }

  public String getAfterStr() {
    return afterStr;
  }

  public void setAfterStr(String afterStr) {
    this.afterStr = afterStr;
  }

  public String getResult() {
    return result;
  }
  public void setResult(String result) {
    this.result = result;
  }
  
}
