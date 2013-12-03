package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools;

import java.net.URL;

/**
 * This (helper-)enum encapsules special sparql commands and returns the commands as String for often required commands.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 12.11.2012
 * 
 *       Last change: 15.11.2012 -> select queries added
 */
public enum SparqlCommandBuilder {
  CLEAR_DATASET, CLEAR_GRAPH, CLEAR_DEFAULT, SELECT_DEFAULT, SELECT_NAMED, SELECT_USING_INDEX, SELECT_NAMED_USING_INDEX, SELECT_USING_INDEX_CONTAINING_NAMED_GRAPH, SELECT_CONTAINING_NAMED_GRAPH, SELECT_ALL_PROJECT_INFO_FOR_GIVEN_ID, SELECT_ALL_PROJECT_INFO_AND_RESOlVE_FOR_GIVEN_ID;

  /**
   * @return the spaql command as {@link String}.
   */
  public String getUpdateCommandString() {
    switch (this) {
    case CLEAR_DATASET:
      return "CLEAR ALL";
    case CLEAR_GRAPH:
      return "CLEAR GRAPH ";
    case CLEAR_DEFAULT:
      return "CLEAR DEFAULT";
    default:
      return "";
    }
  }

  /**
   * @return the sparql query command as {@link String}
   */
  public String getSelectQueryString(final String toSelect, final URL graphName, final String graphPattern, final String projectId, final String projectUri) {
    switch (this) {
    case SELECT_DEFAULT:
      return "SELECT " + toSelect + " { " + graphPattern + "}";
    case SELECT_NAMED:
      return "SELECT " + toSelect + " FROM NAMED <" + graphName + "> { " + graphPattern + "}";
    case SELECT_USING_INDEX:
      // return "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" +
      // "SELECT " + toSelect + " { GRAPH ?g { ?o pf:textMatch '" + graphPattern
      // + "'. ?s ?p ?o. } }";
      return "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" + "SELECT DISTINCT " + toSelect + " { (?lit ?score )  pf:textMatch '" + graphPattern + "'. ?s ?p ?lit\n OPTIONAL {?sParent ?pParent ?s\n FILTER (isBlank(?s))}}";
    case SELECT_NAMED_USING_INDEX:
      return "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" + "SELECT DISTINCT " + toSelect + " FROM NAMED <" + graphName + "> { (?lit ?score ) pf:textMatch '" + graphPattern + "'.?doc ?p ?lit\n }";
    case SELECT_USING_INDEX_CONTAINING_NAMED_GRAPH:
      return "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" + "SELECT DISTINCT " + toSelect + " {  (?lit ?score ) pf:textMatch '" + graphPattern + "'. GRAPH ?g { ?s ?p ?lit}\n OPTIONAL {?sParent ?pParent ?s\n FILTER (isBlank(?s))}}";
    case SELECT_CONTAINING_NAMED_GRAPH:
      return "SELECT DISTINCT " + toSelect + " {  GRAPH ?g { " + graphPattern + " }}";
      // return "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" +
      // "SELECT " + toSelect + " {  ?o pf:textMatch '" + graphPattern +
      // "'. GRAPH ?g { ?s ?p ?o}  }";
    case SELECT_ALL_PROJECT_INFO_FOR_GIVEN_ID:
      return "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n prefix foaf: <http://xmlns.com/foaf/0.1/> \n" + "SELECT " + toSelect + " { ?sbjct  pf:textMatch '"+ projectId +"'.\n ?s foaf:nick ?sbjct. \n ?s ?p ?o.\n } ";
    case SELECT_ALL_PROJECT_INFO_AND_RESOlVE_FOR_GIVEN_ID:
      return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n " 
    + "PREFIX dc:  <http://purl.org/dc/elements/1.1/>\n" 
      + "PREFIX dcterms: <http://purl.org/dc/terms/>\n" 
    + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" 
      + "PREFIX dc:  <http://purl.org/dc/elements/1.1/>\n" 
    + "PREFIX gnd: <http://d-nb.info/standards/elementset/gnd#>\n" 
      + "SELECT DISTINCT ?p ?o ?resolvedPer ?resolvedLing  ?resolvedTime ?resolvedProj ?resolvedLoc ?resolvedOrg ?resolvedMed ?resolvedEvnt  { " 
    + "<" + projectUri + ">" + " ?p ?o .\n" 
    + "OPTIONAL { ?o rdf:type foaf:Person.\n" 
      + "?o ?resolvedPredPer ?resolvedPer }\n" 
      + "OPTIONAL { ?o rdf:type foaf:Project.\n" 
      + "  ?o ?resolvedProj ?resolvedProj } \n " 
    + "OPTIONAL { ?o rdf:type dcterms:Location.\n" 
      + "  ?o ?resolvedPredLoc ?resolvedLoc }\n" 
      + "OPTIONAL { ?o rdf:type foaf:Organization.\n" 
      + "  ?o ?resolvedPredOrg ?resolvedOrg }\n"
    + "OPTIONAL { ?o rdf:type dcterms:LinguisticSystem.\n" 
      + "  ?o ?resolvedPredLin ?resolvedLing }\n" 
      + "OPTIONAL { ?o rdf:type dcterms:MediaType.\n" 
      + "  ?o ?resolvedPredMed ?resolvedMed }\n" 
    + "OPTIONAL { ?o rdf:type dcterms:PeriodOfTime.\n" 
      + "  ?o ?resolvedPredTime ?resolvedTime }\n" 
      + "OPTIONAL { ?o rdf:type gnd:ConferenceOrEvent.\n" 
      + " ?o ?resolvedPredEvnt ?resolvedEvnt } \n}";

    default:
      return "";
    }
  }
}
