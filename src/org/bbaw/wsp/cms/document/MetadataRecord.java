package org.bbaw.wsp.cms.document;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

import org.bbaw.wsp.cms.collections.CollectionReader;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class MetadataRecord implements Cloneable, Serializable {
  private static final long serialVersionUID = 4711L;
  private static DateFormat US_DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
  private int id = -1; // local id: auto incremented integer value (one higher than the last created integer value in lucene field "id", e.g. 1
  private String docId; // local id: resource identifier in index system, e.g. "/edoc/2011/1591/pdf/08_VI.Dokumente.pdf"
  private String identifier; // local id: identifier field in resource metadata: e.g. <meta name="DC.identifier" content="47114711">
  private String uri; // global id: resource URI (uniform resource identifier), download URL: e.g. http://www.deutschestextarchiv.de/book/download_xml/albertinus_landtstoertzer01_1615
  private String webUri; // global web url, e.g. "http://telota.bbaw.de/ig/IG I3 501" or "http://edoc.bbaw.de/volltexte/2011/1769/" or "http://www.deutschestextarchiv.de/albertinus_landtstoertzer01_1615"
  private String collectionNames; // project name(s), e.g. "edoc" or "ig", at this time only exactly one collection name is supported
  private String schemaName; // schema name of resource, e.g. TEI, html, quran etc.
  private String language; // language: ISO 639-3 with 3 characters, e.g. "ger" or "eng"
  private String creator; // author(s) of resource
  private String creatorDetails; // author(s) details (xml format) 
  private String title; // title(s) of resource
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
  private String system; // e.g. eXistDir or httpDir
  
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

  public void setRealDocUrl(final String realDocUrl) {
    this.realDocUrl = realDocUrl;
  }

  public String getSystem() {
    return system;
  }

  public void setSystem(String system) {
    this.system = system;
  }

  public boolean isEXistDir() {
    if (system != null && system.equals("eXistDir"))
      return true;
    else 
      return false;
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

  public void setDocumentType(final String documentType) {
    this.documentType = documentType;
  }

  public String getIsbn() {
    return isbn;
  }

  public void setIsbn(final String isbn) {
    this.isbn = isbn;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(final Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getPublishingDate() {
    return publishingDate;
  }

  public void setPublishingDate(final Date publishingDate) {
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

  public void setSwd(final String swd) {
    this.swd = swd;
  }

  public String getDdc() {
    return ddc;
  }

  public void setDdc(final String ddc) {
    this.ddc = ddc;
  }

  public String getUrn() {
    return urn;
  }

  public void setUrn(final String urn) {
    this.urn = urn;
  }

  public String getDocId() {
    return docId;
  }

  public void setDocId(final String docId) {
    this.docId = docId;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(final String uri) {
    this.uri = uri;
  }

  public String getRights() {
    return rights;
  }

  public void setRights(final String rights) {
    this.rights = rights;
  }

  public int getPageCount() {
    return pageCount;
  }

  public void setPageCount(final int pageCount) {
    this.pageCount = pageCount;
  }

  public String getContributor() {
    return contributor;
  }

  public void setContributor(final String contributor) {
    this.contributor = contributor;
  }

  public String getCoverage() {
    return coverage;
  }

  public void setCoverage(final String coverage) {
    this.coverage = coverage;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(final String encoding) {
    this.encoding = encoding;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(final String license) {
    this.license = license;
  }

  public String getAccessRights() {
    return accessRights;
  }

  public void setAccessRights(final String accessRights) {
    this.accessRights = accessRights;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(final String creator) {
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

  public void setTitle(final String title) {
    this.title = title;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(final Date date) {
    this.date = date;
  }

  public String getYear() {
    String year = null;
    if (date != null) {
      final Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      final int iYear = cal.get(Calendar.YEAR);
      year = "" + iYear;
    }
    return year;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(final String subject) {
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

  public String getCollectionNames() {
    return collectionNames;
  }

  public void setCollectionNames(final String collectionNames) {
    this.collectionNames = collectionNames;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(final String identifier) {
    this.identifier = identifier;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(final String language) {
    this.language = language;
  }

  public String getPublisher() {
    return publisher;
  }

  public void setPublisher(final String publisher) {
    this.publisher = publisher;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getTokenOrig() {
    return tokenOrig;
  }

  public void setTokenOrig(final String tokenOrig) {
    this.tokenOrig = tokenOrig;
  }

  public String getTokenReg() {
    return tokenReg;
  }

  public void setTokenReg(final String tokenReg) {
    this.tokenReg = tokenReg;
  }

  public String getTokenNorm() {
    return tokenNorm;
  }

  public void setTokenNorm(final String tokenNorm) {
    this.tokenNorm = tokenNorm;
  }

  public String getTokenMorph() {
    return tokenMorph;
  }

  public void setTokenMorph(final String tokenMorph) {
    this.tokenMorph = tokenMorph;
  }

  public String getContentXml() {
    return contentXml;
  }

  public void setContentXml(final String contentXml) {
    this.contentXml = contentXml;
  }

  public String getContent() {
    return content;
  }

  public void setContent(final String content) {
    this.content = content;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public void setSchemaName(final String schemaName) {
    this.schemaName = schemaName;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(final Date lastModified) {
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

  public void setPersons(final String persons) {
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

  public void setPlaces(final String places) {
    this.places = places;
  }

  public String getWebUri() {
    return webUri;
  }

  public void setWebUri(final String webUri) {
    this.webUri = webUri;
  }

  public Hashtable<String, XQuery> getxQueries() {
    return xQueries;
  }

  public void setxQueries(final Hashtable<String, XQuery> xQueries) {
    this.xQueries = xQueries;
  }

  public String getInPublication() {
    return inPublication;
  }

  public void setInPublication(final String inPublication) {
    this.inPublication = inPublication;
  }

  public String toString() {
    return "MetadataRecord [docId=" + docId + ", identifier=" + identifier + ", uri=" + uri + ", webUri=" + webUri + ", collectionNames=" + collectionNames + ", schemaName=" + schemaName + ", language=" + language + ", creator=" + creator + ", title=" + title + ", publisher=" + publisher + ", date=" + date + ", description=" + description + ", subject=" + subject + ", contributor=" + contributor + ", coverage=" + coverage + ", ddc=" + ddc + ", swd=" + swd + ", persons=" + persons + ", places=" + places + ", type=" + type + ", rights=" + rights + ", license=" + license + ", accessRights=" + accessRights + ", lastModified=" + lastModified + ", encoding=" + encoding + ", pageCount=" + pageCount + ", urn=" + urn + ", documentType=" + documentType + ", isbn=" + isbn + ", creationDate=" + creationDate + ", publishingDate=" + publishingDate + ", inPublication=" + inPublication + ", tokenOrig=" + tokenOrig + ", tokenReg=" + tokenReg + ", tokenNorm=" + tokenNorm + ", tokenMorph=" + tokenMorph + ", contentXml=" + contentXml + ", content=" + content + ", xQueries=" + xQueries + ", realDocUrl=" + realDocUrl + "]";
  }
  
  public static MetadataRecord fromRecordXmlStr(XQueryEvaluator xQueryEvaluator, String recordXmlString) throws ApplicationException {
    MetadataRecord mdRecord = new MetadataRecord();
    try {
      Field[] fields = MetadataRecord.class.getDeclaredFields();
      for (int i=0; i<fields.length; i++) {
        Field field = fields[i];
        String fieldName = field.getName();
        if (notOutputFields.get(fieldName) == null) {
          Object fieldValue = xQueryEvaluator.evaluateAsString(recordXmlString, "/record/" + fieldName + "/text()");
          if (fieldValue != null) {
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
    rdfStrBuilder.append("  <rdf:Description rdf:about=\"" + uriStrEscaped + "\">\n");
    rdfStrBuilder.append("    <rdf:type rdf:resource=\"http://purl.org/dc/terms/BibliographicResource\"/>\n");
    String projectRdfId = CollectionReader.getInstance().getCollection(collectionNames).getRdfId();
    rdfStrBuilder.append("    <dcterms:isPartOf rdf:resource=\"" + projectRdfId + "\"/>\n");
    rdfStrBuilder.append("    <dcterms:identifier>" + uriStrEscaped + "</dcterms:identifier>\n");
    if (webUri != null) {
      String webUrlStrEscaped = StringUtils.deresolveXmlEntities(webUri);
      rdfStrBuilder.append("    <edm:isShownAt rdf:resource=\"" + webUrlStrEscaped + "\"/>\n");
    }
    if (system != null)
      rdfStrBuilder.append("    <dc:type>" + system + "</dc:type>\n");
    if (creator != null) {
      String creatorStrEscaped = StringUtils.deresolveXmlEntities(creator);
      rdfStrBuilder.append("    <dcterms:creator>" + creatorStrEscaped + "</dcterms:creator>\n");
    }
    if (title != null) {
      String titleStrEscaped = StringUtils.deresolveXmlEntities(title);
      rdfStrBuilder.append("    <dcterms:title>" + titleStrEscaped + "</dcterms:title>\n");
    }
    if (date != null) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      int year = cal.get(Calendar.YEAR);
      rdfStrBuilder.append("    <dcterms:date>" + year + "</dcterms:date>\n");
    }
    rdfStrBuilder.append("    <dc:language>" + language + "</dc:language>\n");
    if (type != null)
      rdfStrBuilder.append("    <dc:format>" + type + "</dc:format>\n");
    if (subject != null) {
      String subjectStrEscaped = StringUtils.deresolveXmlEntities(subject);
      rdfStrBuilder.append("    <dc:subject>" + subjectStrEscaped + "</dc:subject>\n");
    }
    if (description != null) {
      String descriptionStrEscaped = StringUtils.deresolveXmlEntities(description);
      rdfStrBuilder.append("    <dcterms:abstract>" + descriptionStrEscaped + "</dcterms:abstract>\n");
    }
    // TODO further fields / if more than one creator, title, subject
    rdfStrBuilder.append("  </rdf:Description>\n");
    return rdfStrBuilder.toString();
  }
  
  public void setAllNull() {
    persons = null;
    places = null;
    entities = null;
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