package org.bbaw.wsp.cms.scheduler;

import org.apache.log4j.Logger;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;


public class CmsChainSchedulerListener implements JobListener {
  private static Logger LOGGER = Logger.getLogger(CmsJob.class.getName());
  
  public String getName() {
    return "CmsJobChainingListener";
  }

  public void jobToBeExecuted(JobExecutionContext inContext) {
  }

  public void jobExecutionVetoed(JobExecutionContext inContext) {
    String message = "Quartz: JobChainingListener: Job execution was vetoed.";
    LOGGER.debug(message);
  }

  public void jobWasExecuted(JobExecutionContext inContext, JobExecutionException inException) {
    // after finishing his job it tries to schedule the next operation (if there is one in the queue)
    CmsOperation operation = null;
    try {
      CmsChainScheduler cmsChainScheduler = CmsChainScheduler.getInstance();
      operation = getOperation(inContext);
      cmsChainScheduler.finishOperation(operation);
    } catch (ApplicationException e) {
      if (operation != null) {
        operation.setErrorMessage(e.getMessage());
      }
      LOGGER.error(e.getMessage());
    }
  }

  private CmsOperation getOperation(JobExecutionContext context) {
    CmsOperation operation = null;
    if (context != null) {
      JobDetail job = context.getJobDetail();
      JobDataMap parameters = job.getJobDataMap();
      operation = (CmsOperation) parameters.get("operation");
    }
    return operation;
  }
}
