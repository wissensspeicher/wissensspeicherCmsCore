/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.MdSystemResultType;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

/**
 * A HitStatement holds the LARQ search results of a hit statement within the RdfStore. It belongs to a {@link HitGraph}.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 08.02.2013
 * 
 */
public class HitStatement {

  protected HashMap<String, RDFNode> hitVars;
  
  protected  RDFNode subject = null;
  protected  RDFNode predicate = null;
  protected  RDFNode object = null;
  protected  RDFNode subjParent = null;
  protected  RDFNode predParent = null;
  protected  RDFNode resolvedPer = null;
  protected  RDFNode resolvedLing = null;
  protected  RDFNode resolvedTime = null;
  protected RDFNode resolvedProj = null;
  protected  RDFNode resolvedLoc = null;
  protected  RDFNode resolvedOrg = null;
  protected  RDFNode resolvedMed = null;
  protected  RDFNode resolvedEvent = null;
  protected  RDFNode projectId = null;
  protected  RDFNode typeRes = null;
  protected  RDFNode function = null;
  protected  RDFNode title = null;
  protected  RDFNode preName = null;
  protected  RDFNode lastName = null;
  protected  RDFNode email = null;
  protected  RDFNode label = null;
  protected  RDFNode languageCode = null;
  protected  RDFNode isPartOf = null;
  protected  RDFNode coordinates = null;
  protected  RDFNode dcSubject = null;
  protected  RDFNode geographicAreaCode = null;
  protected  RDFNode identifier = null;
  protected  RDFNode hasPart = null;
  protected  RDFNode nick = null;
  protected  RDFNode name = null;
  protected  RDFNode definition = null;
  protected  RDFNode topic = null;
  protected  RDFNode status = null;
  protected  RDFNode coverage = null;
  protected  RDFNode dctermsAbstract = null;
  protected  RDFNode replaces = null;
  protected  RDFNode valid = null;
  protected  RDFNode founder = null;
  protected  RDFNode contributor = null;
  protected  RDFNode fundedBy = null;
  protected  RDFNode hp = null;
  protected  RDFNode temporal = null;
  protected  double score = 0;
  private MdSystemResultType resultType = null;

  /**
   * Create a new HitStatement to represent results of the {@link SparqlAdapter}
   * 
   */
  public HitStatement(final RDFNode subject, final RDFNode predicate, final RDFNode object, final double score, final RDFNode subjParent, 
      final RDFNode predParent, final RDFNode resolvedPer, final RDFNode resolvedLing, final RDFNode resolvedTime, final RDFNode resolvedProj, 
      final RDFNode resolvedLoc, final RDFNode resolvedOrg, final RDFNode resolvedMed, final RDFNode resolvedEvent,
      final RDFNode projectId, final RDFNode typeRes,final RDFNode function, final RDFNode title, final RDFNode preName, final RDFNode lastName, 
      final RDFNode email, final RDFNode label, final RDFNode languageCode, final RDFNode isPartOf, final RDFNode coordinates, final RDFNode dcSubject,
      final RDFNode geographicAreaCode, final RDFNode identifier, final RDFNode hasPart, final RDFNode nick, final RDFNode name, 
      final RDFNode definition, final RDFNode topic, final RDFNode status, final RDFNode coverage, final RDFNode dctermsAbstract, 
      final RDFNode replaces, final RDFNode valid, final RDFNode founder, final RDFNode contributor, final RDFNode fundedBy, 
      final RDFNode hp, final RDFNode temporal
      ) {
    
    hitVars = new HashMap<String, RDFNode>();

    this.subject = removeSparqlBrackets(subject);
    this.predicate = removeSparqlBrackets(predicate);
    this.object = removeSparqlBrackets(object);
    this.score = score;
    this.subjParent = removeSparqlBrackets(subjParent);
    this.predParent = removeSparqlBrackets(predParent);
    this.resolvedPer = removeSparqlBrackets(resolvedPer);
    this.resolvedLing = removeSparqlBrackets(resolvedLing);
    this.resolvedTime = removeSparqlBrackets(resolvedTime);
    this.resolvedProj = removeSparqlBrackets(resolvedProj);
    this.resolvedLoc = removeSparqlBrackets(resolvedLoc);
    this.resolvedOrg = removeSparqlBrackets(resolvedOrg);
    this.resolvedMed = removeSparqlBrackets(resolvedMed);
    this.resolvedEvent = removeSparqlBrackets(resolvedEvent);
    this.projectId = removeSparqlBrackets(projectId);
    this.typeRes = removeSparqlBrackets(typeRes);
    this.function = removeSparqlBrackets(function);
    this.title = removeSparqlBrackets(title);
    this.preName = removeSparqlBrackets(preName);
    this.lastName = removeSparqlBrackets(lastName);
    this.email = removeSparqlBrackets(email);
    this.label = removeSparqlBrackets(label);
    this.languageCode = removeSparqlBrackets(languageCode);
    this.isPartOf = removeSparqlBrackets(isPartOf);
    this.coordinates = removeSparqlBrackets(coordinates);
    this.dcSubject = removeSparqlBrackets(dcSubject);
    this.geographicAreaCode = removeSparqlBrackets(geographicAreaCode);
    this.identifier = removeSparqlBrackets(identifier);
    this.hasPart = removeSparqlBrackets(hasPart);
    this.nick = removeSparqlBrackets(nick);
    this.name = removeSparqlBrackets(name);
    this.definition = removeSparqlBrackets(definition);
    this.topic = removeSparqlBrackets(topic);
    this.status = removeSparqlBrackets(status);
    this.coverage = removeSparqlBrackets(coverage);
    this.dctermsAbstract = removeSparqlBrackets(dctermsAbstract);
    this.replaces = removeSparqlBrackets(replaces);
    this.valid = removeSparqlBrackets(valid);
    this.founder = removeSparqlBrackets(founder);
    this.contributor = removeSparqlBrackets(contributor);
    this.fundedBy = removeSparqlBrackets(fundedBy);
    this.hp = removeSparqlBrackets(hp);
    this.temporal = removeSparqlBrackets(temporal);
    
    hitVars.put("subject", removeSparqlBrackets(subject));
    hitVars.put("predicate", removeSparqlBrackets(predicate));
    hitVars.put("object", removeSparqlBrackets(object));
    hitVars.put("subjParent", removeSparqlBrackets(subjParent));
    hitVars.put("predParent", removeSparqlBrackets(predParent));
    hitVars.put("resolvedPer", removeSparqlBrackets(resolvedPer));
    hitVars.put("resolvedLing", removeSparqlBrackets(resolvedLing));
    hitVars.put("resolvedTime", removeSparqlBrackets(resolvedTime));
    hitVars.put("resolvedProj", removeSparqlBrackets(resolvedProj));
    hitVars.put("resolvedLoc", removeSparqlBrackets(resolvedLoc));
    hitVars.put("resolvedOrg", removeSparqlBrackets(resolvedOrg));
    hitVars.put("resolvedMed", removeSparqlBrackets(resolvedMed));
    hitVars.put("resolvedEvent", removeSparqlBrackets(resolvedEvent));
    hitVars.put("projectId", removeSparqlBrackets(projectId));
    hitVars.put("typeRes", removeSparqlBrackets(typeRes));
    hitVars.put("function", removeSparqlBrackets(function));
    hitVars.put("title", removeSparqlBrackets(title));
    hitVars.put("preName", removeSparqlBrackets(preName));
    hitVars.put("lastName", removeSparqlBrackets(lastName));
    hitVars.put("email", removeSparqlBrackets(email));
    hitVars.put("label", removeSparqlBrackets(label));
    hitVars.put("languageCode", removeSparqlBrackets(languageCode));
    hitVars.put("isPartOf", removeSparqlBrackets(isPartOf));
    hitVars.put("coordinates", removeSparqlBrackets(coordinates));
    hitVars.put("dcSubject", removeSparqlBrackets(dcSubject));
    hitVars.put("geographicAreaCode", removeSparqlBrackets(geographicAreaCode));
    hitVars.put("identifier", removeSparqlBrackets(identifier));
    hitVars.put("hasPart", removeSparqlBrackets(hasPart));
    hitVars.put("nick", removeSparqlBrackets(nick));
    hitVars.put("name", removeSparqlBrackets(name));
    hitVars.put("definition", removeSparqlBrackets(definition));
    hitVars.put("topic", removeSparqlBrackets(topic));
    hitVars.put("status", removeSparqlBrackets(status));
    hitVars.put("coverage", removeSparqlBrackets(coverage));
    hitVars.put("dctermsAbstract", removeSparqlBrackets(dctermsAbstract));
    hitVars.put("replaces", removeSparqlBrackets(replaces));
    hitVars.put("valid", removeSparqlBrackets(valid));
    hitVars.put("founder", removeSparqlBrackets(founder));
    hitVars.put("contributor", removeSparqlBrackets(contributor));
    hitVars.put("fundedBy", removeSparqlBrackets(fundedBy));
    hitVars.put("hp", removeSparqlBrackets(hp));
    hitVars.put("temporal", removeSparqlBrackets(temporal));
    
    //
    this.score = score;
  }

  public HitStatement(final RDFNode subject, final RDFNode predicate, final RDFNode object, 
      final RDFNode projectId, final RDFNode typeRes,final RDFNode function, final RDFNode title, final RDFNode preName, final RDFNode lastName, 
      final RDFNode email, final RDFNode label, final RDFNode languageCode, final RDFNode isPartOf, final RDFNode coordinates, final RDFNode dcSubject,
      final RDFNode geographicAreaCode, final RDFNode identifier, final RDFNode hasPart, final RDFNode nick, final RDFNode name, 
      final RDFNode definition, final RDFNode topic, final RDFNode status, final RDFNode coverage, final RDFNode dctermsAbstract, 
      final RDFNode replaces, final RDFNode valid, final RDFNode founder, final RDFNode contributor, final RDFNode fundedBy, 
      final RDFNode hp, final RDFNode temporal
      ) {

    hitVars = new HashMap<String, RDFNode>();
    
    this.subject = removeSparqlBrackets(subject);
    this.predicate = removeSparqlBrackets(predicate);
    this.object = removeSparqlBrackets(object);
    this.projectId = removeSparqlBrackets(projectId);
    this.typeRes = removeSparqlBrackets(typeRes);
    this.function = removeSparqlBrackets(function);
    this.title = removeSparqlBrackets(title);
    this.preName = removeSparqlBrackets(preName);
    this.lastName = removeSparqlBrackets(lastName);
    this.email = removeSparqlBrackets(email);
    this.label = removeSparqlBrackets(label);
    this.languageCode = removeSparqlBrackets(languageCode);
    this.isPartOf = removeSparqlBrackets(isPartOf);
    this.coordinates = removeSparqlBrackets(coordinates);
    this.dcSubject = removeSparqlBrackets(dcSubject);
    this.geographicAreaCode = removeSparqlBrackets(geographicAreaCode);
    this.identifier = removeSparqlBrackets(identifier);
    this.hasPart = removeSparqlBrackets(hasPart);
    this.nick = removeSparqlBrackets(nick);
    this.name = removeSparqlBrackets(name);
    this.definition = removeSparqlBrackets(definition);
    this.topic = removeSparqlBrackets(topic);
    this.status = removeSparqlBrackets(status);
    this.coverage = removeSparqlBrackets(coverage);
    this.dctermsAbstract = removeSparqlBrackets(dctermsAbstract);
    this.replaces = removeSparqlBrackets(replaces);
    this.valid = removeSparqlBrackets(valid);
    this.founder = removeSparqlBrackets(founder);
    this.contributor = removeSparqlBrackets(contributor);
    this.fundedBy = removeSparqlBrackets(fundedBy);
    this.hp = removeSparqlBrackets(hp);
    this.temporal = removeSparqlBrackets(temporal);
    

    hitVars.put("subject", removeSparqlBrackets(subject));
    hitVars.put("predicate", removeSparqlBrackets(predicate));
    hitVars.put("object", removeSparqlBrackets(object));
    hitVars.put("subjParent", removeSparqlBrackets(subjParent));
    hitVars.put("predParent", removeSparqlBrackets(predParent));
    hitVars.put("resolvedPer", removeSparqlBrackets(resolvedPer));
    hitVars.put("resolvedLing", removeSparqlBrackets(resolvedLing));
    hitVars.put("resolvedTime", removeSparqlBrackets(resolvedTime));
    hitVars.put("resolvedProj", removeSparqlBrackets(resolvedProj));
    hitVars.put("resolvedLoc", removeSparqlBrackets(resolvedLoc));
    hitVars.put("resolvedOrg", removeSparqlBrackets(resolvedOrg));
    hitVars.put("resolvedMed", removeSparqlBrackets(resolvedMed));
    hitVars.put("resolvedEvent", removeSparqlBrackets(resolvedEvent));
    hitVars.put("projectId", removeSparqlBrackets(projectId));
    hitVars.put("typeRes", removeSparqlBrackets(typeRes));
    hitVars.put("function", removeSparqlBrackets(function));
    hitVars.put("title", removeSparqlBrackets(title));
    hitVars.put("preName", removeSparqlBrackets(preName));
    hitVars.put("lastName", removeSparqlBrackets(lastName));
    hitVars.put("email", removeSparqlBrackets(email));
    hitVars.put("label", removeSparqlBrackets(label));
    hitVars.put("languageCode", removeSparqlBrackets(languageCode));
    hitVars.put("isPartOf", removeSparqlBrackets(isPartOf));
    hitVars.put("coordinates", removeSparqlBrackets(coordinates));
    hitVars.put("dcSubject", removeSparqlBrackets(dcSubject));
    hitVars.put("geographicAreaCode", removeSparqlBrackets(geographicAreaCode));
    hitVars.put("identifier", removeSparqlBrackets(identifier));
    hitVars.put("hasPart", removeSparqlBrackets(hasPart));
    hitVars.put("nick", removeSparqlBrackets(nick));
    hitVars.put("name", removeSparqlBrackets(name));
    hitVars.put("definition", removeSparqlBrackets(definition));
    hitVars.put("topic", removeSparqlBrackets(topic));
    hitVars.put("status", removeSparqlBrackets(status));
    hitVars.put("coverage", removeSparqlBrackets(coverage));
    hitVars.put("dctermsAbstract", removeSparqlBrackets(dctermsAbstract));
    hitVars.put("replaces", removeSparqlBrackets(replaces));
    hitVars.put("valid", removeSparqlBrackets(valid));
    hitVars.put("founder", removeSparqlBrackets(founder));
    hitVars.put("contributor", removeSparqlBrackets(contributor));
    hitVars.put("fundedBy", removeSparqlBrackets(fundedBy));
    hitVars.put("hp", removeSparqlBrackets(hp));
    hitVars.put("temporal", removeSparqlBrackets(temporal));
  }
  
  private RDFNode removeSparqlBrackets(final RDFNode rdfnode) {
    final RDFNode resultingNode;
    if (rdfnode != null && rdfnode.isResource()) {
      String uri = rdfnode.asResource().getURI();
      if (uri != null) {
        uri = uri.replaceFirst("\\<", "");
        uri = uri.replaceFirst("\\>", "");
        resultingNode = new ResourceImpl(uri);
        return resultingNode;
      }
    }
    return rdfnode;
  }

  /**
   * @return the subject
   */
  public RDFNode getSubject() {
    return subject;
  }

  /**
   * @return the predicate
   */
  public RDFNode getPredicate() {
    return predicate;
  }

  /**
   * @return the object
   */
  public RDFNode getObject() {
    return object;
  }

  /**
   * @return the score
   */
  public double getScore() {
    return score;
  }

  /**
   * @return the subject
   */
  public RDFNode getSubjParent() {
    return subjParent;
  }

  /**
   * @return the predicate
   */
  public RDFNode getPredParent() {
    return predParent;
  }
  /**
   * @return the resolved Person
   */
  public RDFNode getResolvedPer() {
    return resolvedPer;
  }
  /**
   * @return the resolved Linguistic System
   */
  public RDFNode getResolvedLing() {
    return resolvedLing;
  }
  /**
   * @return the resolved Time
   */
  public RDFNode getResolvedTime() {
    return resolvedTime;
  }
  /**
   * @return the resolved Project
   */
  public RDFNode getResolvedProj() {
    return resolvedProj;
  }
  /**
   * @return the resolved Location
   */
  public RDFNode getResolvedLoc() {
    return resolvedLoc;
  }
  /**
   * @return the resolved Organization
   */
  public RDFNode getResolvedOrg() {
    return resolvedOrg;
  }
  /**
   * @return the resolved MediaType
   */
  public RDFNode getResolvedMed() {
    return resolvedMed;
  }
  /**
   * @return the resolved Event
   */
  public RDFNode getResolvedEvent() {
    return resolvedEvent;
  }
  
  public RDFNode getProjectId() {
    return projectId;
  }

  public RDFNode getTypeRes() {
    return typeRes;
  }

  public RDFNode getFunction() {
    return function;
  }

  public RDFNode getTitle() {
    return title;
  }

  public RDFNode getPreName() {
    return preName;
  }

  public RDFNode getLastName() {
    return lastName;
  }

  public RDFNode getEmail() {
    return email;
  }

  public RDFNode getLabel() {
    return label;
  }

  public RDFNode getLanguageCode() {
    return languageCode;
  }

  public RDFNode getIsPartOf() {
    return isPartOf;
  }

  public RDFNode getCoordinates() {
    return coordinates;
  }

  public RDFNode getDcSubject() {
    return dcSubject;
  }

  public RDFNode getGeographicAreaCode() {
    return geographicAreaCode;
  }

  public RDFNode getIdentifier() {
    return identifier;
  }

  public RDFNode getHasPart() {
    return hasPart;
  }

  public RDFNode getNick() {
    return nick;
  }

  public RDFNode getName() {
    return name;
  }

  public RDFNode getDefinition() {
    return definition;
  }

  public RDFNode getTopic() {
    return topic;
  }

  public RDFNode getStatus() {
    return status;
  }

  public RDFNode getCoverage() {
    return coverage;
  }

  public RDFNode getDctermsAbstract() {
    return dctermsAbstract;
  }

  public RDFNode getReplaces() {
    return replaces;
  }

  public RDFNode getValid() {
    return valid;
  }

  public RDFNode getFounder() {
    return founder;
  }

  public RDFNode getContributor() {
    return contributor;
  }

  public RDFNode getFundedBy() {
    return fundedBy;
  }

  public RDFNode getHp() {
    return hp;
  }

  public RDFNode getTemporal() {
    return temporal;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb =  new StringBuilder();
    sb.append("HitStatement [\n");
    
    for (Entry<String, RDFNode> entry : hitVars.entrySet()) {
      if(entry.getValue() != null){
        String s = entry.getKey() + ": " + entry.getValue();
        sb.append(s);
        sb.append(", | \n");
      }
    }
    sb.append("]\n");
    return sb.toString();
//    return "HitStatement [subject=" + subject + ", predicate=" + predicate + ", object=" + object + ", subjParent=" + subjParent + ", "
//        + "predParent=" + predParent + ", score=" + score + ", resultType=" + resultType + ", resolvedEvent=" + resolvedEvent + ", "
//        + "resolved Ling=" + resolvedLing + ", resolved Location=" + resolvedLoc + ", resolved Media Type=" + resolvedMed + ", "
//        + "resolved Organization=" + resolvedOrg + ", resolved Person=" + resolvedPer + ", resolved Project=" + resolvedProj + ", "
//        + "resolved Period Of Time=" + resolvedTime + ", resolved projectId=" + projectId + ", resolved typeRes=" + typeRes + ""
//        + ", resolved function=" + function + ", resolved title=" + title + ", resolved preName=" + preName + ""
//        + ", resolved lastName=" + lastName + ", resolved email=" + email + ", resolved label=" + label + ""
//        + ", resolved languageCode=" + languageCode + ", resolved isPartOf=" + isPartOf + ", resolved coordinates=" + coordinates + ""
//        + ", resolved dcSubject=" + dcSubject + ", resolved geographicAreaCode=" + geographicAreaCode + ", resolved identifier=" + identifier + ""
//        + ", resolved hasPart=" + hasPart + ", resolved nick=" + nick + ", resolved name=" + name + ", resolved definition=" + definition + ""
//        + ", resolved topic=" + topic + ", resolved status=" + status + ", resolved coverage=" + coverage + ""
//        + ", resolved dctermsAbstract=" + dctermsAbstract + ""
//        + ", resolved replaces=" + replaces + ", resolved valid=" + valid + ", resolved founder=" + founder + ""
//        + ", resolved contributor=" + contributor + ", resolved fundedBy=" + fundedBy + ""
//        + ", resolved hp=" + hp + ", resolved temporal=" + temporal + ""
//        + "]";
  }
 
  
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((object == null) ? 0 : object.hashCode());
    result = prime * result + ((predParent == null) ? 0 : predParent.hashCode());
    result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
    long temp;
    temp = Double.doubleToLongBits(score);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((subjParent == null) ? 0 : subjParent.hashCode());
    result = prime * result + ((subject == null) ? 0 : subject.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final HitStatement other = (HitStatement) obj;
    if (object == null) {
      if (other.object != null) {
        return false;
      }
    } else if (!object.equals(other.object)) {
      return false;
    }
    if (predParent == null) {
      if (other.predParent != null) {
        return false;
      }
    } else if (!predParent.equals(other.predParent)) {
      return false;
    }
    if (predicate == null) {
      if (other.predicate != null) {
        return false;
      }
    } else if (!predicate.equals(other.predicate)) {
      return false;
    }
    if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score)) {
      return false;
    }
    if (subjParent == null) {
      if (other.subjParent != null) {
        return false;
      }
    } else if (!subjParent.equals(other.subjParent)) {
      return false;
    }
    if (subject == null) {
      if (other.subject != null) {
        return false;
      }
    } else if (!subject.equals(other.subject)) {
      return false;
    }
    return true;
  }

  /**
   * Set the result type so a client can differ the way, the result was fetched.
   * 
   * @param resultType
   */
  public void setResultType(final MdSystemResultType resultType) {
    this.resultType = resultType;

  }

  /**
   * @return the resultType
   */
  public final MdSystemResultType getResultType() {
    return resultType;
  }

}
