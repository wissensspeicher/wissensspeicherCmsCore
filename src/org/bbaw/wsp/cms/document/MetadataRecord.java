package org.bbaw.wsp.cms.document;

import java.util.Calendar;
import java.util.Date;

public class MetadataRecord {
  private String docId;  // document identifier in index system, e.g. /echo/la/Benedetti_1585.xml
  private String identifier;  // document identifier in documents metadata: e.g. /echo:echo/echo:metadata/dcterms:identifier  
  private String echoId;  // document identifier in echo system: directory name, e.g. /permanent/library/163127KK  
  private String language;
  private String creator;  // author
  private String title;
  private String description;
  private String publisher; // publisher with place: e.g. Springer, New York
  private String type; // mime type: e.g. text/xml  // TODO ist eigentlich das Feld "format" --> zus. instance variable "format" definieren
  private String rights; // e.g. open access
  private Date date; // creation date, modification date, etc.
  private String license;  // e.g. http://echo.mpiwg-berlin.mpg.de/policy/oa_basics/declaration
  private String accessRights;  // e.g. free  
  private String schemaName; // e.g. TEI, echo, html, or archimedes
  private Date lastModified;
  private int pageCount;
  private String figuresXmlStr; // echo schema: list of figure elements as xml string 
  private String notesXmlStr;   // echo schema: list of note elements as xml string
  private String tocXmlStr;     // echo schema: table of content elements as xml string
 
  public MetadataRecord() {
    
  }
  
  public MetadataRecord(String identifier, String language, String creator, String title, String description, String publisher, String type, String rights, Date date) {
    this.identifier = identifier;
    this.language = language;
    this.creator = creator;
    this.title = title;
    this.description = description;
    this.publisher = publisher;
    this.type = type;
    this.rights = rights;
    this.date = date;
  }
  
  public String getDocId() {
    return docId;
  }

  public void setDocId(String docId) {
    this.docId = docId;
  }

  public String getEchoId() {
    return echoId;
  }

  public void setEchoId(String echoId) {
    this.echoId = echoId;
  }

  public String getRights() {
    return rights;
  }

  public void setRights(String rights) {
    this.rights = rights;
  }

  public int getPageCount() {
    return pageCount;
  }

  public void setPageCount(int pageCount) {
    this.pageCount = pageCount;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public String getAccessRights() {
    return accessRights;
  }

  public void setAccessRights(String accessRights) {
    this.accessRights = accessRights;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getYear() {
    String year = null;
    if (date != null) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      int iYear = cal.get(Calendar.YEAR);
      year = "" + iYear;
    }
    return year;
  }
  
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getPublisher() {
    return publisher;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public String getFiguresXmlStr() {
    return figuresXmlStr;
  }

  public void setFiguresXmlStr(String figuresXmlStr) {
    this.figuresXmlStr = figuresXmlStr;
  }

  public String getNotesXmlStr() {
    return notesXmlStr;
  }

  public void setNotesXmlStr(String notesXmlStr) {
    this.notesXmlStr = notesXmlStr;
  }

  public String getTocXmlStr() {
    return tocXmlStr;
  }

  public void setTocXmlStr(String tocXmlStr) {
    this.tocXmlStr = tocXmlStr;
  }

  
}
