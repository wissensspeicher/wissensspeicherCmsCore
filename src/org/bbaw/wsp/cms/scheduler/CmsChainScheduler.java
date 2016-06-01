package org.bbaw.wsp.cms.scheduler;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class CmsChainScheduler {
  private static CmsChainScheduler instance;
  private static String CMS_JOB = "CMS_JOB";
  private static String CMS_TRIGGER = "CMS_TRIGGER";
  private static String CMS_GROUP = "CMS_GROUP";
  private static Logger LOGGER = Logger.getLogger(CmsJob.class.getName());
  private org.quartz.Scheduler scheduler;
  private JobListener jobListener;
  private Queue<CmsOperation> operationQueue = new PriorityQueue<CmsOperation>();
  private HashMap<Integer, CmsOperation> finishedOperations = new HashMap<Integer, CmsOperation>();
  private boolean operationInProgress = false;
  private int jobOrderId = 0;
  
  public static CmsChainScheduler getInstance() throws ApplicationException {
    if (instance == null) {
      instance = new CmsChainScheduler();
      instance.init();
    }
    return instance;
  }

  public CmsOperation doOperation(CmsOperation cmsOperation) throws ApplicationException {
    jobOrderId++;
    cmsOperation.setOrderId(jobOrderId);
    queueOperation(cmsOperation);
    scheduleNextOperation();
    return cmsOperation;
  }
  
  public void finishOperation(CmsOperation cmsOperation) throws ApplicationException {
    operationInProgress = false;
    Date now = new Date();
    cmsOperation.setEnd(now);
    cmsOperation.setStatus("finished");
    int jobId = new Integer(cmsOperation.getOrderId());
    finishedOperations.put(jobId, cmsOperation);
    log(cmsOperation);
    // schedule next job if there is one
    scheduleNextOperation();
  }
  
  private void log(CmsOperation cmsOperation) {
    Date startTime = cmsOperation.getStart();
    Date endTime = cmsOperation.getEnd();
    long executionTime = -1;
    if (startTime != null && endTime != null)
      executionTime = (endTime.getTime() - startTime.getTime());
    String jobInfo = "CMS operation " + cmsOperation.toString() + ": started at: " + startTime + " and ended at: " + endTime + " (needed time: " + executionTime + " ms)";
    LOGGER.info(jobInfo);
  }
  
  public synchronized void scheduleNextOperation() throws ApplicationException {
    if (isOperationInProgress()) {
      // nothing, operation has to wait
    } else {
      CmsOperation cmsOperation = operationQueue.poll();
      if (cmsOperation == null) {
        // if queue is empty then do nothing (there are no more operations to execute)
      } else {
        Date now = new Date();
        operationInProgress = true;
        cmsOperation.setStart(now);
        scheduleJob(cmsOperation, now);
      }
    }
  }
  
  public ArrayList<CmsOperation> getOperations() throws ApplicationException {
    ArrayList<CmsOperation> operations = new ArrayList<CmsOperation>();
    try {
      // first: all finished jobs
      Collection<CmsOperation> finishedOperationsValues = finishedOperations.values();
      operations.addAll(finishedOperationsValues);
      // second: all currently executed jobs
      if (operationInProgress) {
        List<JobExecutionContext> currentJobs = (List<JobExecutionContext>) scheduler.getCurrentlyExecutingJobs();
        Iterator<JobExecutionContext> iter = currentJobs.iterator();
        while (iter.hasNext()) {
          JobExecutionContext jobExecutionContext = iter.next();
          CmsOperation operation = getOperation(jobExecutionContext);
          if (operation != null) {
            operations.add(operation);
          }
        }
      }
      // third: all queued jobs
      Iterator<CmsOperation> iter = operationQueue.iterator();
      while (iter.hasNext()) {
        CmsOperation operation = iter.next();
        operations.add(operation);
      }
    } catch (SchedulerException e) {
      LOGGER.error(e.getMessage());
      throw new ApplicationException(e);
    }
    return operations;
  }
    
  public CmsOperation getOperation(int jobId) throws ApplicationException {
    CmsOperation operation = null;
    try {
      // first try: looks into currently executing jobs
      if (operationInProgress) {
        List<JobExecutionContext> currentJobs = (List<JobExecutionContext>) scheduler.getCurrentlyExecutingJobs();
        Iterator<JobExecutionContext> iter = currentJobs.iterator();
        while (iter.hasNext()) {
          JobExecutionContext jobExecutionContext = iter.next();
          operation = getOperation(jobExecutionContext);
          if (operation != null) {
            int operationOrderId = operation.getOrderId();
            if (jobId == operationOrderId)
              return operation;
          }
        }
      }
      // second try: look into finished jobs
      operation = finishedOperations.get(new Integer(jobId));
      if (operation != null) {
        return operation;
      }
      // third try: look into queued jobs
      Iterator<CmsOperation> iter = operationQueue.iterator();
      while (iter.hasNext()) {
        operation = iter.next();
        if (operation.getOrderId() == jobId)
          return operation;
      }
    } catch (SchedulerException e) {
      LOGGER.error(e.getMessage());
      throw new ApplicationException(e);
    }
    // if not found return null
    return null;
  }
  
  public CmsOperation getOperation(JobExecutionContext jobExecutionContext) {
    CmsOperation operation = null;
    if (jobExecutionContext != null) {
      JobDetail job = jobExecutionContext.getJobDetail();
      JobDataMap parameters = job.getJobDataMap();
      operation = (CmsOperation) parameters.get("operation");
    }
    return operation;
  }
  
  private void queueOperation(CmsOperation operation) {
    int operationsBefore = operationQueue.size();
    if (operationsBefore == 0)
      operation.setStatus("waiting in operation queue");
    else 
      operation.setStatus("waiting in operation queue: " + operationsBefore + " operations heve to be executed before this operation");
    operationQueue.offer(operation);
  }
  
  private synchronized boolean isOperationInProgress() {
    return operationInProgress;  
  }
  
  private void scheduleJob(CmsOperation operation, Date fireTime) throws ApplicationException {
    try {
      int jobId = operation.getOrderId();
      String jobName = CMS_JOB + "-id-" + jobId + "-timeId-" + fireTime;
      JobDetail job = new JobDetail(jobName, CMS_GROUP, CmsJob.class);
      JobDataMap parameters = new JobDataMap();
      parameters.put("operation", operation);
      job.setJobDataMap(parameters);
      job.addJobListener(jobListener.getName());        
      String triggerName = CMS_TRIGGER + "-id-" + jobId + "-timeId-" + fireTime;
      Trigger trigger = new SimpleTrigger(triggerName, CMS_GROUP, fireTime);
      scheduler.scheduleJob(job, trigger);
      String jobInfo = "Schedule operation: " + operation.toString() + ": done at: " + fireTime.toString();
      LOGGER.info(jobInfo);
    } catch (SchedulerException e) {
      LOGGER.error(e.getMessage());
      throw new ApplicationException(e);
    }
  }
  
  private void init() throws ApplicationException {
    try {
      if (scheduler == null) {
        String quartzPath = getQuartzPath();
        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory(quartzPath);
        scheduler = schedulerFactory.getScheduler();
        jobListener = new CmsChainSchedulerListener();
        scheduler.addJobListener(jobListener);
        scheduler.start();
        LOGGER.info("Started Quartz scheduler factory: " + quartzPath);
      } 
    } catch (SchedulerException e) {
      LOGGER.error(e.getMessage());
      throw new ApplicationException(e);
    }
  }
  
  public void end() throws ApplicationException {
    try {
      if (scheduler != null) {
        scheduler.shutdown();
      }
      String quartzPath = getQuartzPath();
      LOGGER.info("Ended Quartz scheduler factory: " + quartzPath);
    } catch (SchedulerException e) {
      LOGGER.error(e.getMessage());
      throw new ApplicationException(e);
    }
  }

  private String getQuartzPath() {
    URL quartzUrl = CmsChainScheduler.class.getResource("quartz.properties");
    String quartzPath = quartzUrl.getPath();
    if (quartzPath.indexOf(".jar!") != -1) {
      int beginIndex = quartzPath.indexOf(".jar!") + 6;
      quartzPath = quartzPath.substring(beginIndex);
    }
    return quartzPath;    
  }
}