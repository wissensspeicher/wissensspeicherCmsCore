package org.bbaw.wsp.cms.document;

import java.util.Calendar;
import java.util.Date;

public class MetadataRecord {
  private String docId;  // local id: document identifier in index system, e.g. /echo/la/Benedetti_1585.xml
  private String identifier;  // local id: identifier field in documents metadata: e.g. /echo:echo/echo:metadata/dcterms:identifier  
  private String uri;  // global id: document URI (uniform resource identifier), e.g. http://de.wikipedia.org/wiki/Ramones
  private String language;
  private String creator;  // author
  private String title;
  private String description;  // abstract etc.
  private String subject;  // subject keywords from the title or description or content or subject lists (thesaurus etc.)
  private String publisher; // publisher with place: e.g. Springer, New York
  private String type; // mime type: e.g. text/xml  // TODO ist eigentlich das Feld "format" --> zus. instance variable "format" definieren
  private String rights; // e.g. open access
  private Date date; // creation date, modification date, etc.
  private String license;  // e.g. http://echo.mpiwg-berlin.mpg.de/policy/oa_basics/declaration
  private String accessRights;  // e.g. free  
  private String projectIds; // e.g. "collection1 collection7"
  private String schemaName; // e.g. TEI, echo, html, or archimedes
  private Date lastModified;
  private int pageCount;
  private String echoId;  // document identifier in echo system: directory name, e.g. /permanent/library/163127KK  
 
  public String getDocId() {
    return docId;
  }

  public void setDocId(String docId) {
    this.docId = docId;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
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

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getProjectIds() {
    return projectIds;
  }

  public void setProjectIds(String projectIds) {
    this.projectIds = projectIds;
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

  public String getEchoId() {
    return echoId;
  }

  public void setEchoId(String echoId) {
    this.echoId = echoId;
  }

}
