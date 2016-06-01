package org.bbaw.wsp.cms.scheduler;

import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.collections.ProjectManager;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class CmsJob implements Job {
  public static String STATUS_BEGIN = "started";
  private static Logger LOGGER = Logger.getLogger(CmsJob.class.getName());
  private JobExecutionContext currentExecutedContext;
  
  public void execute(JobExecutionContext context) throws JobExecutionException {
    this.currentExecutedContext = context;
    CmsOperation operation = getOperation();
    try {
      operation.setStatus(STATUS_BEGIN);
      String operationType = operation.getType();   
      String operationName = operation.getName();   
      ArrayList<String> parameters = operation.getParameters();   
      if (operationType.equals("ProjectManager")) {
        ProjectManager pm = ProjectManager.getInstance();
        String projectIds = parameters.get(0);  // first param are projectIds
        if (operationName.equals("update")) {
          pm.update(projectIds);
        } else if (operationName.equals("harvest")) {
          pm.harvest(projectIds);
        } else if (operationName.equals("annotate")) {
          pm.annotate(projectIds);
        } else if (operationName.equals("index")) {
          pm.index(projectIds);
        } else if (operationName.equals("delete")) {
          pm.delete(projectIds);
        }
      }
      Date startingTime = operation.getStart();
      String jobInfo = "CMS operation " + operation.toString() + ": started at: " + startingTime;
      LOGGER.info(jobInfo);
      this.currentExecutedContext = null;
    } catch (Exception e) {
      try {
        // Quartz will automatically unschedule all triggers associated with this job so that it does not run again
        CmsChainScheduler cmsChainScheduler = CmsChainScheduler.getInstance();
        cmsChainScheduler.finishOperation(operation);
        String errorMessage = e.getMessage();
        if (errorMessage == null) {
          Throwable t = e.getCause();
          if (t == null) {
            errorMessage = e.toString();
          } else {
            errorMessage = t.getMessage();
          }
        }
        operation.setErrorMessage(errorMessage);
        LOGGER.error(errorMessage);
        JobExecutionException jobExecutionException = new JobExecutionException(e);
        jobExecutionException.setUnscheduleAllTriggers(true);
        throw jobExecutionException;
      } catch (ApplicationException ex) {
        // nothing
      }
    }
  } 

  private CmsOperation getOperation() {
    CmsOperation operation = null;
    if (currentExecutedContext != null) {
      JobDetail job = currentExecutedContext.getJobDetail();
      JobDataMap parameters = job.getJobDataMap();
      operation = (CmsOperation) parameters.get("operation");
    }
    return operation;
  }

}
