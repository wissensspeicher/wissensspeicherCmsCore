package org.bbaw.wsp.cms.document;

import java.util.ArrayList;

import org.apache.lucene.search.Query;

public class Hits {
  private ArrayList<Document> hits;
  private float maxScore;  // maximum score of all hits 
  private Query query;
  private int from;
  private int to;
  private int size = 0;
  private int sizeTotalDocuments;
  private int sizeTotalTerms;
  private long elapsedTime;
  private Facets facets;
  private ArrayList<GroupDocuments> groupByHits;
  
  public Hits(ArrayList<Document> hits, int from, int to) {
    this.hits = hits;
    this.from = from;
    this.to = to;
  }

  public Float getMaxScore() {
    return maxScore;
  }

  public void setMaxScore(Float maxScore) {
    this.maxScore = maxScore;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getSizeTotalDocuments() {
    return sizeTotalDocuments;
  }

  public void setSizeTotalDocuments(int sizeTotalDocuments) {
    this.sizeTotalDocuments = sizeTotalDocuments;
  }

  public int getSizeTotalTerms() {
    return sizeTotalTerms;
  }

  public void setSizeTotalTerms(int sizeTotalTerms) {
    this.sizeTotalTerms = sizeTotalTerms;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public void setElapsedTime(long elapsedTime) {
    this.elapsedTime = elapsedTime;
  }

  public Query getQuery() {
    return query;
  }

  public void setQuery(Query query) {
    this.query = query;
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

  public Facets getFacets() {
    return facets;
  }

  public void setFacets(Facets facets) {
    this.facets = facets;
  }

  public ArrayList<GroupDocuments> getGroupByHits() {
    return groupByHits;
  }

  public void setGroupByHits(ArrayList<GroupDocuments> groupByHits) {
    this.groupByHits = groupByHits;
  }
}
