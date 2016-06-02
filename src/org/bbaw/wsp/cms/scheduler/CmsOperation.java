package org.bbaw.wsp.cms.scheduler;

import java.util.ArrayList;
import java.util.Date;

public class CmsOperation implements Comparable<CmsOperation> {
  private int id;
  private Date start;
  private Date end;
  private String name;
  private String status;
  private String errorMessage;
  private String type; // operation type e.g. "ProjectManager"
  private ArrayList<String> parameters;
  
  public CmsOperation(String type, String name, ArrayList<String> parameters) {
    this.type = type;
    this.name = name;
    this.parameters = parameters;
  }

  public int compareTo(CmsOperation op) {
    Integer opOrderId = new Integer(op.id);
    Integer thisOrderId = new Integer(id);
    return thisOrderId.compareTo(opOrderId);
  }
  
  public boolean isFinished() {
    if (status != null && status.equals("finished"))
      return true;
    else 
      return false;
  }
  
  public boolean isError() {
    if (errorMessage != null && errorMessage.length() > 0)
      return true;
    else 
      return false;
  }
  
  public int getOrderId() {
    return id;
  }

  public void setOrderId(int orderId) {
    this.id = orderId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getStart() {
    return start;
  }

  public void setStart(Date start) {
    this.start = start;
  }

  public Date getEnd() {
    return end;
  }

  public void setEnd(Date end) {
    this.end = end;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public ArrayList<String> getParameters() {
    return parameters;
  }

  public void setParameters(ArrayList<String> parameters) {
    this.parameters = parameters;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String toString() {
    String retStr = "job: " + id + ": " + name;
    if (parameters != null)
      retStr = "job: " + id + ": " + name + "(" + parameters.toString() + ")";
    return retStr;
  }
  
}
