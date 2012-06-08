package org.bbaw.wsp.cms.document;

import java.util.ArrayList;

public class Hits {
  private ArrayList<Document> hits;
  private int from;
  private int to;
  private int size = 0;
  
  public Hits(ArrayList<Document> hits, int from, int to) {
    this.hits = hits;
    this.from = from;
    this.to = to;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public ArrayList<Document> getHits() {
    return hits;
  }

  public void setHits(ArrayList<Document> hits) {
    this.hits = hits;
  }

  public int getFrom() {
    return from;
  }

  public void setFrom(int from) {
    this.from = from;
  }

  public int getTo() {
    return to;
  }

  public void setTo(int to) {
    this.to = to;
  }


}
