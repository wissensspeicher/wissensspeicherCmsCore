package org.bbaw.wsp.cms.document;

import java.util.ArrayList;

public class GroupHits {
  private ArrayList<GroupDocuments> groupDocuments;
  private int sizeTotalGroups;
  
  public ArrayList<GroupDocuments> getGroupDocuments() {
    return groupDocuments;
  }
  public void setGroupDocuments(ArrayList<GroupDocuments> groupDocuments) {
    this.groupDocuments = groupDocuments;
  }
  public int getSizeTotalGroups() {
    return sizeTotalGroups;
  }
  public void setSizeTotalGroups(int sizeTotalGroups) {
    this.sizeTotalGroups = sizeTotalGroups;
  }

  
}
