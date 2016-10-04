package org.bbaw.wsp.cms.document;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

import org.bbaw.wsp.cms.collections.ProjectReader;
import org.jsoup.nodes.Element;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;

public class MetadataRecord implements Cloneable, Serializable {
  private static final long serialVersionUID = 4711L;
  private static DateFormat US_DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
  private int id = -1; // local id: auto incremented integer value
  private String docId; // local id: resource identifier in index system, e.g. "/edoc/files/1155/08_assmann.pdf"
  private String identifier; // local id: identifier field in resource metadata: e.g. <meta name="DC.identifier" content="47114711">
  private String uri; // global id: resource URI (uniform resource identifier), download URL: e.g. http://www.deutschestextarchiv.de/book/download_xml/albertinus_landtstoertzer01_1615
  private String webUri; // global web url, e.g. "http://telota.bbaw.de/ig/IG I3 501" or "http://edoc.bbaw.de/volltexte/2011/1769/" or "http://www.deutschestextarchiv.de/albertinus_landtstoertzer01_1615"
  private String organizationRdfId; // rdf id of organization (e.g.: http://wissensspeicher.bbaw.de/rdf/normdata/BBAW)
  private String projectId; // project id, e.g. "edoc" or "ig"
  private String projectRdfId; // project rdf id
  private String collectionRdfId; // collection rdf id
  private String databaseRdfId; // database rdf id, e.g. http://telota.bbaw.de:8085/exist/rest/db/AvHBriefedition
  private String schemaName; // schema name of resource, e.g. TEI, html, quran etc.
  private String language; // language: ISO 639-3 with 3 characters, e.g. "ger" or "eng"
  private String creator; // author(s) of resource
  private String creatorDetails; // author(s) details (xml format) 
  private String title; // main title of resource
  private String alternativeTitle; // alternative title(s) of resource
  private String publisher; // publisher with place: e.g. Springer, New York
  private Date date; // publication date, e.g. 1958
  private String description; // description, abstract etc.
  private String subject; // free subject keywords (separated by "###", ";" or ","
  private String subjectControlled; // subjects controlled keywords (separated by "###")
  private String subjectControlledDetails; // subjects controlled (persons, places, concepts of dbPedia, gnd or ddc) (xml format)
  private String contributor; // additional contributors of the resource such as translators, layouters, designers. These could be persons or organizations.
  private String coverage; // spatial and temporal coverages, e.g. "13th century" or "Grand Canyon, Arizona"
  private String ddc; // subject keywords of DDC (Dewey Decimal Classification), e.g. "Philosophie" 
  private String swd; // subject keywords of SWD (Schlagwortnormdatei), normally separated by commas
  private String entities; // main entities in resource (separated by "###")
  private String entitiesDetails; // main entities details in resource (xml format)
  private String persons; // main person names in resource (separated by "###")
  private String personsDetails; // main persons details in resource (xml format)
  private String places; // main place names in resource (separated by "###")
  private String type; // mime type: e.g. text/xml
  private String rights; // rights string, e.g. open access
  private String license; // license string, e.g. "CC-BY-SA"
  private String accessRights; // access rights string, e.g. free
  private Date lastModified; // last modification date in index system
  private String encoding; // charset such as "utf-8" or "iso-8859-1"
  private int pageCount; // number of pages
  private String extent; // extent string
  private String urn; // uniform resource name, e.g. the KOBV urn, e.g. urn:nbn:de:kobv:b4360-10020
  private String edocCollection;  // edoc collection. e.g. "BBAW / Schriftenreihen / Berichte und Abhandlungen / Berichte und Abhandlungen - Band 10", in case of multi values: separated by semicolon
  private String documentType; // e.g. the KOBV "Dokumentenart"
  private String isbn; // isbn, e.g. the KOBV ISBN
  private Date creationDate; // e.g. the KOBV "Erstellungsjahr"
  private Date publishingDate; // e.g. the KOBV "Publikationsdatum"
  private String inPublication; // e.g. the KOBV publication (in: ...)
  private String tokenOrig; // original fulltext tokens of resource
  private String tokenReg; // regularized fulltext tokens of resource
  private String tokenNorm; // normalized fulltext tokens of resource
  private String tokenMorph; // morphological fulltext tokens of resource
  private String contentXml; // original xml content of the resources file
  private String content; // original text content of the resources file (without xml tags)
  private Hashtable<String, XQuery> xQueries; // dynamic xQueries of xml resources (with name, code, result)
  private String realDocUrl; // e.g. the URL to the eDoc (not the index.html)
  private String system; // "crawl", "eXist" or "dbRecord" (for dbType: mysql, postgres and for dbName jdg) or "oaiRecord" (for oai databases: dta, edoc) 
  
  private static Hashtable<String, String> notOutputFields = new Hashtable<String, String>();
  static {
    notOutputFields.put("tokenOrig", "tokenOrig");
    notOutputFields.put("tokenReg", "tokenReg");
    notOutputFields.put("tokenNorm", "tokenNorm");
    notOutputFields.put("tokenMorph", "tokenMorph");
    notOutputFields.put("contentXml", "contentXml");
    notOutputFields.put("content", "content");
    notOutputFields.put("xQueries", "xQueries");
    notOutputFields.put("notOutputFields", "notOutputFields");
    notOutputFields.put("serialVersionUID", "serialVersionUID");
    notOutputFields.put("US_DATE_FORMAT", "US_DATE_FORMAT");
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getRealDocUrl() {
    return realDocUrl;
  }

  public void setRealDocUrl(String realDocUrl) {
    this.realDocUrl = realDocUrl;
  }

  public String getSystem() {
    return system;
  }

  public void setSystem(String system) {
    this.system = system;
  }

  public String getDatabaseRdfId() {
    return databaseRdfId;
  }

  public void setDatabaseRdfId(String databaseRdfId) {
    this.databaseRdfId = databaseRdfId;
  }

  public boolean isDirectory() {
    if (system != null && system.equals("directory"))
      return true;
    else 
      return false;
  }

  public boolean isDbRecord() {
    if (system != null && system.equals("dbRecord"))
      return true;
    else 
      return false;
  }

  public boolean isRecord() {
    if (system != null && system.contains("Record"))
      return true;
    else 
      return false;
  }

  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  public String getIsbn() {
    return isbn;
  }

  public void setIsbn(String isbn) {
    this.isbn = isbn;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getPublishingDate() {
    return publishingDate;
  }

  public void setPublishingDate(Date publishingDate) {
    this.publishingDate = publishingDate;
  }

  public String getEdocCollection() {
    return edocCollection;
  }

  public void setEdocCollection(String edocCollection) {
    this.edocCollection = edocCollection;
  }

  public String getSwd() {
    return swd;
  }

  public void setSwd(String swd) {
    this.swd = swd;
  }

  public String getDdc() {
    return ddc;
  }

  public void setDdc(String ddc) {
    this.ddc = ddc;
  }

  public String getUrn() {
    return urn;
  }

  public void setUrn(String urn) {
    this.urn = urn;
  }

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

  public String getExtent() {
    return extent;
  }

  public void setExtent(String extent) {
    this.extent = extent;
  }

  public String getContributor() {
    return contributor;
  }

  public void setContributor(String contributor) {
    this.contributor = contributor;
  }

  public String getCoverage() {
    return coverage;
  }

  public void setCoverage(String coverage) {
    this.coverage = coverage;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
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

  public String getCreatorDetails() {
    return creatorDetails;
  }

  public void setCreatorDetails(String creatorDetails) {
    this.creatorDetails = creatorDetails;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAlternativeTitle() {
    return alternativeTitle;
  }

  public void setAlternativeTitle(String alternativeTitle) {
    this.alternativeTitle = alternativeTitle;
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

  public String getSubjectControlled() {
    return subjectControlled;
  }

  public void setSubjectControlled(String subjectControlled) {
    this.subjectControlled = subjectControlled;
  }

  public String getSubjectControlledDetails() {
    return subjectControlledDetails;
  }

  public void setSubjectControlledDetails(String subjectControlledDetails) {
    this.subjectControlledDetails = subjectControlledDetails;
  }

  public String getOrganizationRdfId() {
    return organizationRdfId;
  }

  public void setOrganizationRdfId(String organizationRdfId) {
    this.organizationRdfId = organizationRdfId;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getProjectRdfId() {
    return projectRdfId;
  }

  public void setProjectRdfId(String projectRdfId) {
    this.projectRdfId = projectRdfId;
  }

  public String getCollectionRdfId() {
    return collectionRdfId;
  }

  public void setCollectionRdfId(String collectionRdfId) {
    this.collectionRdfId = collectionRdfId;
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

  public String getTokenOrig() {
    return tokenOrig;
  }

  public void setTokenOrig(String tokenOrig) {
    this.tokenOrig = tokenOrig;
  }

  public String getTokenReg() {
    return tokenReg;
  }

  public void setTokenReg(String tokenReg) {
    this.tokenReg = tokenReg;
  }

  public String getTokenNorm() {
    return tokenNorm;
  }

  public void setTokenNorm(String tokenNorm) {
    this.tokenNorm = tokenNorm;
  }

  public String getTokenMorph() {
    return tokenMorph;
  }

  public void setTokenMorph(String tokenMorph) {
    this.tokenMorph = tokenMorph;
  }

  public String getContentXml() {
    return contentXml;
  }

  public void setContentXml(String contentXml) {
    this.contentXml = contentXml;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
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

  public String getEntities() {
    return entities;
  }

  public void setEntities(String entities) {
    this.entities = entities;
  }

  public String getEntitiesDetails() {
    return entitiesDetails;
  }

  public void setEntitiesDetails(String entitiesDetails) {
    this.entitiesDetails = entitiesDetails;
  }

  public String getPersons() {
    return persons;
  }

  public void setPersons(String persons) {
    this.persons = persons;
  }

  public String getPersonsDetails() {
    return personsDetails;
  }

  public void setPersonsDetails(String personsDetails) {
    this.personsDetails = personsDetails;
  }

  public String getPlaces() {
    return places;
  }

  public void setPlaces(String places) {
    this.places = places;
  }

  public String getWebUri() {
    return webUri;
  }

  public void setWebUri(String webUri) {
    this.webUri = webUri;
  }

  public Hashtable<String, XQuery> getxQueries() {
    return xQueries;
  }

  public void setxQueries(Hashtable<String, XQuery> xQueries) {
    this.xQueries = xQueries;
  }

  public String getInPublication() {
    return inPublication;
  }

  public void setInPublication(String inPublication) {
    this.inPublication = inPublication;
  }

  public String toString() {
    return "MetadataRecord [docId=" + docId + ", identifier=" + identifier + ", uri=" + uri + ", webUri=" + webUri + ", projectId=" + projectId + ", schemaName=" + schemaName + ", language=" + language + ", creator=" + creator + ", title=" + title + ", publisher=" + publisher + ", date=" + date + ", description=" + description + ", subject=" + subject + ", contributor=" + contributor + ", coverage=" + coverage + ", ddc=" + ddc + ", swd=" + swd + ", persons=" + persons + ", places=" + places + ", type=" + type + ", rights=" + rights + ", license=" + license + ", accessRights=" + accessRights + ", lastModified=" + lastModified + ", encoding=" + encoding + ", pageCount=" + pageCount + ", urn=" + urn + ", documentType=" + documentType + ", isbn=" + isbn + ", creationDate=" + creationDate + ", publishingDate=" + publishingDate + ", inPublication=" + inPublication + ", tokenOrig=" + tokenOrig + ", tokenReg=" + tokenReg + ", tokenNorm=" + tokenNorm + ", tokenMorph=" + tokenMorph + ", contentXml=" + contentXml + ", content=" + content + ", xQueries=" + xQueries + ", realDocUrl=" + realDocUrl + "]";
  }
  
  public static MetadataRecord fromRecordElement(Element record) throws ApplicationException {
    MetadataRecord mdRecord = new MetadataRecord();
    try {
      Field[] fields = MetadataRecord.class.getDeclaredFields();
      for (int i=0; i<fields.length; i++) {
        Field field = fields[i];
        String fieldName = field.getName();
        if (notOutputFields.get(fieldName) == null) {
          Element fieldElem = record.select(fieldName).first();
          if (fieldElem != null) {
            Object fieldValue = fieldElem.text();
            Class<? extends Object> typeClass = field.getType();
            if (typeClass.getName().equals("java.lang.String")) {
              fieldValue = StringUtils.resolveXmlEntities((String) fieldValue);
            } else if (typeClass.getName().equals("int")) {
              fieldValue = Integer.valueOf((String) fieldValue);
            } else if (typeClass.getName().equals("java.util.Date")) {
              fieldValue = US_DATE_FORMAT.parse((String) fieldValue);
            }
            field.set(mdRecord, fieldValue);
          }
        }
      }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return mdRecord;
  }
  
  public String toXmlStr() throws ApplicationException {
    StringBuilder xmlStrBuilder = new StringBuilder();
    try {
      xmlStrBuilder.append("<record>\n");
      Field[] fields = MetadataRecord.class.getDeclaredFields();
      for (int i=0; i<fields.length; i++) {
        Field field = fields[i];
        String fieldName = field.getName();
        if (notOutputFields.get(fieldName) == null) {
          Object fieldValue = field.get(this);
          if (fieldValue != null) {
            String fieldValueStr = fieldValue.toString();
            if (! fieldValueStr.isEmpty()) {
              String fieldValueStrEscaped = StringUtils.deresolveXmlEntities(fieldValueStr);
              xmlStrBuilder.append("  <" + fieldName + ">" + fieldValueStrEscaped + "</" + fieldName + ">\n");
            }
          }
        }
      }
      xmlStrBuilder.append("</record>\n");
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return xmlStrBuilder.toString();
  }
  
  public String toRdfStr() throws ApplicationException {
    StringBuilder rdfStrBuilder = new StringBuilder();
    String uriStrEscaped = StringUtils.deresolveXmlEntities(uri);
    rdfStrBuilder.append("<rdf:Description rdf:about=\"" + uriStrEscaped + "\">\n");
    if (system != null)
      rdfStrBuilder.append("  <dcterms:type>" + system + "</dcterms:type>\n");
    rdfStrBuilder.append("  <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
    String projectRdfId = ProjectReader.getInstance().getProject(projectId).getRdfId();
    rdfStrBuilder.append("  <dcterms:isPartOf rdf:resource=\"" + projectRdfId + "\"/>\n");
    rdfStrBuilder.append("  <dcterms:identifier>" + uriStrEscaped + "</dcterms:identifier>\n");
    if (webUri != null) {
      String webUrlStrEscaped = StringUtils.deresolveXmlEntities(webUri);
      rdfStrBuilder.append("  <dcterms:hasFormat rdf:resource=\"" + webUrlStrEscaped + "\"/>\n");
    }
    if (type != null)
      rdfStrBuilder.append("  <dcterms:format>" + type + "</dcterms:format>\n");
    if (creator != null) {
      String creatorStrEscaped = StringUtils.deresolveXmlEntities(creator);
      rdfStrBuilder.append("  <dcterms:creator>" + creatorStrEscaped + "</dcterms:creator>\n");
    }
    if (title != null) {
      String titleStrEscaped = StringUtils.deresolveXmlEntities(title);
      rdfStrBuilder.append("  <dcterms:title>" + titleStrEscaped + "</dcterms:title>\n");
    }
    if (date != null) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      int year = cal.get(Calendar.YEAR);
      rdfStrBuilder.append("  <dcterms:date>" + year + "</dcterms:date>\n");
    }
    rdfStrBuilder.append("  <dcterms:language>" + language + "</dcterms:language>\n");
    if (subject != null) {
      String subjectStrEscaped = StringUtils.deresolveXmlEntities(subject);
      rdfStrBuilder.append("  <dcterms:subject>" + subjectStrEscaped + "</dcterms:subject>\n");
    }
    if (description != null) {
      String descriptionStrEscaped = StringUtils.deresolveXmlEntities(description);
      rdfStrBuilder.append("  <dcterms:abstract>" + descriptionStrEscaped + "</dcterms:abstract>\n");
    }
    // TODO further fields / if more than one creator, title, subject
    rdfStrBuilder.append("</rdf:Description>\n");
    return rdfStrBuilder.toString();
  }
  
  public void setAllNull() {
    entitiesDetails = null;
    tokenOrig = null; 
    tokenReg = null; 
    tokenNorm = null; 
    tokenMorph = null; 
    contentXml = null; 
    content = null; 
  }
  
  public boolean isEdocRecord() {
    boolean isEdocRecord = false;
    if (docId != null && docId.startsWith("/edoc"))
      isEdocRecord = true;
    return isEdocRecord;
  }
  
  public MetadataRecord clone() {
    MetadataRecord mdRecord = null;
    try {
      mdRecord = (MetadataRecord) super.clone();
    } catch (CloneNotSupportedException e) {
      // nothing
    }
    return mdRecord;
  }
  
}