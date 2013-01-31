package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools;

/**
 * This (helper-)enum encapsules special sparql commands and returns the
 * commands as String for often required commands.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 12.11.2012
 * 
 *       Last change: 15.11.2012 -> select queries added
 */
public enum SparqlCommandBuilder {
  CLEAR_DATASET, CLEAR_GRAPH, CLEAR_DEFAULT, SELECT_DEFAULT, SELECT_NAMED, SELECT_USING_INDEX, SELECT_NAMED_USING_INDEX;

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
  public String getSelectQueryString(final String toSelect, final String graphName, final String graphPattern) {
    switch (this) {
    case SELECT_DEFAULT:
      return "SELECT " + toSelect + " { " + graphPattern + "}";
    case SELECT_NAMED:
      return "SELECT " + toSelect + " FROM NAMED <" + graphName + "> { " + graphPattern + "}";
    case SELECT_USING_INDEX:
      return "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" + "SELECT " + toSelect + " { ?s pf:textMatch '" + graphPattern + "' }";
    case SELECT_NAMED_USING_INDEX:
      return "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" + "SELECT " + toSelect + " FROM NAMED <" + graphName + "> { ?s pf:textMatch '" + graphPattern + "' }";
    default:
      return "";
    }
  }
}
