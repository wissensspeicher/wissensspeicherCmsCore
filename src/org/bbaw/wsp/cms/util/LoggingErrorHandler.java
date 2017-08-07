package org.bbaw.wsp.cms.util;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public class LoggingErrorHandler implements ErrorHandler, ErrorListener {
  Logger logger;

  public LoggingErrorHandler(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void error(SAXParseException e) {
    logger.error(logger.getName() + ": " + e.getSystemId() + ": line: " + e.getLineNumber() + ": " + e.getMessage());
  }

  @Override
  public void error(TransformerException e) {
    logger.error(logger.getName() + ": " + e.getMessageAndLocation());
  }

  public void error(Exception e) {
    logger.error(logger.getName() + ": " + e.getMessage());
  }

  @Override
  public void warning(SAXParseException e) {
    logger.warn(logger.getName() + ": " + e.getSystemId() + ": line: " + e.getLineNumber() + ": " + e.getMessage());
  }

  @Override
  public void warning(TransformerException e) {
    logger.warn(logger.getName() + ": " + e.getMessageAndLocation());
  }

  @Override
  public void fatalError(SAXParseException e) {
    logger.fatal(logger.getName() + ": " + e.getSystemId() + ": line: " + e.getLineNumber() + ": " + e.getMessage());
  }

  @Override
  public void fatalError(TransformerException e) {
    logger.fatal(logger.getName() + ": " + e.getMessageAndLocation());
  }
}
