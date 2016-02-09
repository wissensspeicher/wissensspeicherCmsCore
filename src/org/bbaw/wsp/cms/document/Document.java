package org.bbaw.wsp.cms.document;

import java.util.ArrayList;

import org.apache.lucene.index.IndexableField;

public class Document {
  private org.apache.lucene.document.Document document;
  private ArrayList<String> hitFragments;
  private String firstHitPageNumber;  // page number of first hit
  
  public Document(org.apache.lucene.document.Document luceneDocument) {
    this.document = luceneDocument;
  }

  public org.apache.lucene.document.Document getDocument() {
    return document;
  }

  public IndexableField getField(String field) {
    if (document != null)
      return document.getField(field);
    else 
      return null;
  }
  
  public void setDocument(org.apache.lucene.document.Document document) {
    this.document = document;
  }

  public ArrayList<String> getHitFragments() {
    return hitFragments;
  }

  public void setHitFragments(ArrayList<String> hitFragments) {
    this.hitFragments = hitFragments;
  }

  public String getFirstHitPageNumber() {
    return firstHitPageNumber;
  }

  public void setFirstHitPageNumber(String firstHitPageNumber) {
    this.firstHitPageNumber = firstHitPageNumber;
  }


}
