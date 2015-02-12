/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;

/**
 * @author <a href="mailto:sascha.feldmann@gmx.de">Sascha Feldmann</a>
 * @since 04.02.2013
 * 
 */
public class HitGraph {

  private final URL namedGraphUrl;
  private final ArrayList<HitStatement> hitStatements;
  private final ArrayList<String> hitSubjects;


  private final HashMap<String, HashMap<String, List<String>>> hitStatementsMap;
  private double avgScore;
  private double highestScore;
  private static final Logger logger = Logger.getLogger(SparqlAdapter.class);

  public static final double DEFAULT_SCORE = 0;

  /**
   * Create a new {@link HitGraph}.
   * 
   * @param namedGraphUrl
   *          the name (an URL) of the named graph as stored in the {@link WspRdfStore}
   */
  public HitGraph(final URL namedGraphUrl) {
    this.namedGraphUrl = namedGraphUrl;
    hitStatements = new ArrayList<HitStatement>();
    hitSubjects = new ArrayList<String>();
    hitStatementsMap = new HashMap<String, HashMap<String, List<String>>>();
    avgScore = DEFAULT_SCORE;
    highestScore = DEFAULT_SCORE;
  }

  public URL getNamedGraphUrl() {
    return namedGraphUrl;
  }

  /**
   * 
   * @return the average score of all {@link HitStatement}.
   */
  public double getAvgScore() {
    return avgScore;
  }

  /**
   * 
   * @return the maximum score of all {@link HitStatement}
   */
  public double getHighestScore() {
    return highestScore;
  }

  /**
   * Add a {@link HitStatement } and subject as key. Should be done by the {@link SparqlAdapter} only.
   * 
   * @param statement a {@link HitStatement}
   * @param subject
   */
  public void addStatement(String subject, final HashMap<String, List<String>> descrMap) {
    hitStatementsMap.put(subject, descrMap);
    // calculate scores
//    calcAvgScore(statement);
//    calcHighestScore(statement);
  }
  
  /**
   * Add a {@link HitStatement}. Should be done by the {@link SparqlAdapter} only.
   * 
   * @param statement
   *          a {@link HitStatement}
   */
  public void addStatement(final HitStatement statement) {
    hitStatements.add(statement);
    // calculate scores
    calcAvgScore(statement);
    calcHighestScore(statement);
  }

  /**
   * if you need just an arraylist of strings as result
   * @param subject
   */
  public void addStatement(String subject) {
    hitSubjects.add(subject);
  }
  
  public ArrayList<String> getHitSubjects() {
    return hitSubjects;
  }
  
  private void calcHighestScore(final HitStatement statement) {
    if (highestScore == 0) {
      highestScore = statement.getScore();
    } else {
      if (statement.getScore() > highestScore) {
        highestScore = statement.getScore();
      }
    }
  }

  private void calcAvgScore(final HitStatement newStatement) {
    // calc arithmetisches Mittel
    if (avgScore == 0) {
      avgScore = newStatement.getScore();
    } else {
      final double sum = avgScore + newStatement.getScore();
      double avg = 0;
      if (sum != 0) {
        avg = sum / 2;
      }
      avgScore = avg;
    }
  }

  /**
   * Return the {@link HitStatement} of this {@link HitGraph}.
   * 
   * @return
   */
  public Collection<HitStatement> getAllHitStatements() {
    return hitStatements;
  }
  
  /**
   * Return the {@link HitStatement} of this {@link HitGraph}.
   * 
   * @return
   */
  public HashMap<String, HashMap<String, List<String>>> getAllHitStatementsAsMap() {
    return hitStatementsMap;
  }

  public HashMap<String, List<String>> getStatementBySubject(String subject){
   return hitStatementsMap.get(subject);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("HitGraph [namedGraphUrl=" + namedGraphUrl + ", hitStatementsMap= \n");
    for (Entry<String, HashMap<String, List<String>>> entry : hitStatementsMap.entrySet()) {
      if(entry.getKey() != null) {
        sb.append("  "+(entry.getKey() + " : \n"));
      }else{
        sb.append("");
      }
      sb.append("\n");
      if(entry.getValue() !=null ){
        HashMap<String, List<String>> hmRdfnode = entry.getValue();
        for (Entry<String, List<String>> entryRdf : hmRdfnode.entrySet()) {
          if(entryRdf.getValue() !=null) {
            sb.append("    "+(entryRdf.getKey() + " : " + entryRdf.getValue()));  
          }else{
            sb.append("    "+(entryRdf.getKey() + " : " + "value null"));
          }
          sb.append("\n");
        }
      }else{
      }
      sb.append("\n");
    }
//    logger.info("avgScore : "+avgScore);
//    logger.info("highestScore : "+highestScore);
    sb.append(", avgScore=" + avgScore + ", highestScore=" + highestScore + "]");
    sb.append("\n");
    return sb.toString();
  }

}
