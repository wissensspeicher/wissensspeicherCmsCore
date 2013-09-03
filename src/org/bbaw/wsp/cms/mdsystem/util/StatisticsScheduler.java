package org.bbaw.wsp.cms.mdsystem.util;


public class StatisticsScheduler {

  private String totalNumberOfGraphs;
  private String totalNumberOfTriple;
  private static StatisticsScheduler scheduler;
 
  private long timeStamp;
  
  private StatisticsScheduler(){
  }
  
  public static StatisticsScheduler getInstance(){
    if(scheduler == null){
      scheduler = new StatisticsScheduler();
    }
    return scheduler;
  }
  
  public long getTimeStamp(){
    return this.timeStamp;
  }

  public void setNewTimeStamp(long currentTime){
    this.timeStamp = currentTime;
  }
  
  public String getTotalNumberOfGraphs() {
    return totalNumberOfGraphs;
  }
  
  public void setTotalNumberOfGraphs(String totalNumberOfGraphs) {
    this.totalNumberOfGraphs = totalNumberOfGraphs;
  }
  
  public String getTotalNumberOfTriple() {
    return totalNumberOfTriple;
  }
  
  public void setTotalNumberOfTriple(String totalNumberOfTriple) {
    this.totalNumberOfTriple = totalNumberOfTriple;
  }
  
}
