package org.bbaw.wsp.cms.mdsystem.util;

import java.util.TimerTask;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.MdSystemQueryHandler;


public class StatisticsScheduler extends TimerTask {

  private String totalNumberOfGraphs;
  private String totalNumberOfTriple;
  
  @Override
  public void run() {

    MdSystemQueryHandler mdQueryHandler = MdSystemQueryHandler.getInstance();
    
    this.totalNumberOfTriple = mdQueryHandler.getTripleCount().toString();
    this.totalNumberOfGraphs = mdQueryHandler.getNumberOfGraphs();
  }

  public String getTotalNumberOfGraphs() {
    return totalNumberOfGraphs;
  }

  public String getTotalNumberOfTriple() {
    return totalNumberOfTriple;
  }
  
}
