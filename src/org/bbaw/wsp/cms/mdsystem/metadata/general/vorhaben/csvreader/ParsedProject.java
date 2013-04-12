/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben.csvreader;

import com.googlecode.jcsv.annotations.MapToColumn;

/**
 * This POJO class will be filled by a CSVMapping object/class.
 * 
 * The information is fetched from an CSV export (e.g. from an excel).
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.03.2013
 * 
 */
public class ParsedProject {

  @MapToColumn(column = 0)
  private String creator;
  @MapToColumn(column = 1)
  private String title;
  @MapToColumn(column = 2)
  private String publisher;
  @MapToColumn(column = 3)
  private String date;
  @MapToColumn(column = 4)
  private String coverage;
  @MapToColumn(column = 5)
  private String subject;
  @MapToColumn(column = 6)
  private String extent;
  @MapToColumn(column = 7)
  private String language;
  @MapToColumn(column = 8)
  private String format;
  @MapToColumn(column = 9)
  private String type;
  @MapToColumn(column = 10)
  private String contributor;
  @MapToColumn(column = 11)
  private String method;
  @MapToColumn(column = 12)
  private String acronym;
  @MapToColumn(column = 13)
  private String state;
  @MapToColumn(column = 14)
  private String relation_part_of;
  @MapToColumn(column = 15)
  private String laufzeit;
  @MapToColumn(column = 16)
  private String subjekt;
  @MapToColumn(column = 17)
  private String cooperation;
  @MapToColumn(column = 18)
  private String mitarbeiter;
  @MapToColumn(column = 19)
  private String version;
  @MapToColumn(column = 20)
  private String coverage_geo;
  @MapToColumn(column = 21)
  private String identifier;
  @MapToColumn(column = 22)
  private String pfad;
  @MapToColumn(column = 23)
  private String kompatibel_mit;
  @MapToColumn(column = 24)
  private String relation;
  @MapToColumn(column = 25)
  private String relation_gefoerdet_von;
  @MapToColumn(column = 26)
  private String person_historisch;
  @MapToColumn(column = 27)
  private String genre;
  @MapToColumn(column = 28)
  private String personen_gruender;
  @MapToColumn(column = 29)
  private String pfad_struktur;
  @MapToColumn(column = 30)
  private String status_anett;

  /**
   * @return the creator
   */
  public String getCreator() {
    return creator;
  }

  /**
   * @param creator
   *          the creator to set
   */
  public void setCreator(final String creator) {
    this.creator = creator;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title
   *          the title to set
   */
  public void setTitle(final String title) {
    this.title = title;
  }

  /**
   * @return the publisher
   */
  public String getPublisher() {
    return publisher;
  }

  /**
   * @param publisher
   *          the publisher to set
   */
  public void setPublisher(final String publisher) {
    this.publisher = publisher;
  }

  /**
   * @return the date
   */
  public String getDate() {
    return date;
  }

  /**
   * @param date
   *          the date to set
   */
  public void setDate(final String date) {
    this.date = date;
  }

  /**
   * @return the coverage
   */
  public String getCoverage() {
    return coverage;
  }

  /**
   * @param coverage
   *          the coverage to set
   */
  public void setCoverage(final String coverage) {
    this.coverage = coverage;
  }

  /**
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @param subject
   *          the subject to set
   */
  public void setSubject(final String subject) {
    this.subject = subject;
  }

  /**
   * @return the extent
   */
  public String getExtent() {
    return extent;
  }

  /**
   * @param extent
   *          the extent to set
   */
  public void setExtent(final String extent) {
    this.extent = extent;
  }

  /**
   * @return the language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * @param language
   *          the language to set
   */
  public void setLanguage(final String language) {
    this.language = language;
  }

  /**
   * @return the format
   */
  public String getFormat() {
    return format;
  }

  /**
   * @param format
   *          the format to set
   */
  public void setFormat(final String format) {
    this.format = format;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type
   *          the type to set
   */
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * @return the contributor
   */
  public String getContributor() {
    return contributor;
  }

  /**
   * @param contributor
   *          the contributor to set
   */
  public void setContributor(final String contributor) {
    this.contributor = contributor;
  }

  /**
   * @return the method
   */
  public String getMethod() {
    return method;
  }

  /**
   * @param method
   *          the method to set
   */
  public void setMethod(final String method) {
    this.method = method;
  }

  /**
   * @return the acronym
   */
  public String getAcronym() {
    return acronym;
  }

  /**
   * @param acronym
   *          the acronym to set
   */
  public void setAcronym(final String acronym) {
    this.acronym = acronym;
  }

  /**
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * @param state
   *          the state to set
   */
  public void setState(final String state) {
    this.state = state;
  }

  /**
   * @return the relation_part_of
   */
  public String getRelation_part_of() {
    return relation_part_of;
  }

  /**
   * @param relation_part_of
   *          the relation_part_of to set
   */
  public void setRelation_part_of(final String relation_part_of) {
    this.relation_part_of = relation_part_of;
  }

  /**
   * @return the laufzeit
   */
  public String getLaufzeit() {
    return laufzeit;
  }

  /**
   * @param laufzeit
   *          the laufzeit to set
   */
  public void setLaufzeit(final String laufzeit) {
    this.laufzeit = laufzeit;
  }

  /**
   * @return the subjekt
   */
  public String getSubjekt() {
    return subjekt;
  }

  /**
   * @param subjekt
   *          the subjekt to set
   */
  public void setSubjekt(final String subjekt) {
    this.subjekt = subjekt;
  }

  /**
   * @return the cooperation
   */
  public String getCooperation() {
    return cooperation;
  }

  /**
   * @param cooperation
   *          the cooperation to set
   */
  public void setCooperation(final String cooperation) {
    this.cooperation = cooperation;
  }

  /**
   * @return the mitarbeiter
   */
  public String getMitarbeiter() {
    return mitarbeiter;
  }

  /**
   * @param mitarbeiter
   *          the mitarbeiter to set
   */
  public void setMitarbeiter(final String mitarbeiter) {
    this.mitarbeiter = mitarbeiter;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @param version
   *          the version to set
   */
  public void setVersion(final String version) {
    this.version = version;
  }

  /**
   * @return the coverage_geo
   */
  public String getCoverage_geo() {
    return coverage_geo;
  }

  /**
   * @param coverage_geo
   *          the coverage_geo to set
   */
  public void setCoverage_geo(final String coverage_geo) {
    this.coverage_geo = coverage_geo;
  }

  /**
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * @param identifier
   *          the identifier to set
   */
  public void setIdentifier(final String identifier) {
    this.identifier = identifier;
  }

  /**
   * @return the pfad
   */
  public String getPfad() {
    return pfad;
  }

  /**
   * @param pfad
   *          the pfad to set
   */
  public void setPfad(final String pfad) {
    this.pfad = pfad;
  }

  /**
   * @return the kompatibel_mit
   */
  public String getKompatibel_mit() {
    return kompatibel_mit;
  }

  /**
   * @param kompatibel_mit
   *          the kompatibel_mit to set
   */
  public void setKompatibel_mit(final String kompatibel_mit) {
    this.kompatibel_mit = kompatibel_mit;
  }

  /**
   * @return the relation
   */
  public String getRelation() {
    return relation;
  }

  /**
   * @param relation
   *          the relation to set
   */
  public void setRelation(final String relation) {
    this.relation = relation;
  }

  /**
   * @return the relation_gefoerdet_von
   */
  public String getRelation_gefoerdet_von() {
    return relation_gefoerdet_von;
  }

  /**
   * @param relation_gefoerdet_von
   *          the relation_gefoerdet_von to set
   */
  public void setRelation_gefoerdet_von(final String relation_gefoerdet_von) {
    this.relation_gefoerdet_von = relation_gefoerdet_von;
  }

  /**
   * @return the person_historisch
   */
  public String getPerson_historisch() {
    return person_historisch;
  }

  /**
   * @param person_historisch
   *          the person_historisch to set
   */
  public void setPerson_historisch(final String person_historisch) {
    this.person_historisch = person_historisch;
  }

  /**
   * @return the genre
   */
  public String getGenre() {
    return genre;
  }

  /**
   * @param genre
   *          the genre to set
   */
  public void setGenre(final String genre) {
    this.genre = genre;
  }

  /**
   * @return the personen_gruender
   */
  public String getPersonen_gruender() {
    return personen_gruender;
  }

  /**
   * @param personen_gruender
   *          the personen_gruender to set
   */
  public void setPersonen_gruender(final String personen_gruender) {
    this.personen_gruender = personen_gruender;
  }

  /**
   * @return the pfad_struktur
   */
  public String getPfad_struktur() {
    return pfad_struktur;
  }

  /**
   * @param pfad_struktur
   *          the pfad_struktur to set
   */
  public void setPfad_struktur(final String pfad_struktur) {
    this.pfad_struktur = pfad_struktur;
  }

  /**
   * @return the status_anett
   */
  public String getStatus_anett() {
    return status_anett;
  }

  /**
   * @param status_anett
   *          the status_anett to set
   */
  public void setStatus_anett(final String status_anett) {
    this.status_anett = status_anett;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "ParsedProject [creator=" + creator + ", title=" + title + ", publisher=" + publisher + ", date=" + date + ", coverage=" + coverage + ", subject=" + subject + ", extent=" + extent + ", language=" + language + ", format=" + format + ", type=" + type + ", contributor=" + contributor + ", method=" + method + ", acronym=" + acronym + ", state=" + state + ", relation_part_of=" + relation_part_of + ", laufzeit=" + laufzeit + ", subjekt=" + subjekt + ", cooperation=" + cooperation + ", mitarbeiter=" + mitarbeiter + ", version=" + version + ", coverage_geo=" + coverage_geo + ", identifier=" + identifier + ", pfad=" + pfad + ", kompatibel_mit=" + kompatibel_mit + ", relation=" + relation + ", relation_gefoerdet_von=" + relation_gefoerdet_von + ", person_historisch=" + person_historisch + ", genre=" + genre + ", personen_gruender=" + personen_gruender + ", pfad_struktur=" + pfad_struktur + ", status_anett=" + status_anett + "]";
  }

}
