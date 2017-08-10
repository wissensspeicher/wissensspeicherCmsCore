package org.bbaw.wsp.cms.util;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
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
    String columnNumberStr = "";
    int columnNumber = e.getColumnNumber();
    if (columnNumber > 0)
      columnNumberStr = ", column: " + columnNumber;
    logger.error(logger.getName() + ": " + e.getSystemId() + ": line: " + e.getLineNumber() + columnNumberStr + ": " + e.getMessage());
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
    String columnNumberStr = "";
    int columnNumber = e.getColumnNumber();
    if (columnNumber > 0)
      columnNumberStr = ", column: " + columnNumber;
    logger.warn(logger.getName() + ": " + e.getSystemId() + ": line: " + e.getLineNumber() + columnNumberStr + ": " + e.getMessage());
  }

  @Override
  public void warning(TransformerException e) {
    logger.warn(logger.getName() + ": " + e.getMessageAndLocation());
  }

  @Override
  public void fatalError(SAXParseException e) {
    String columnNumberStr = "";
    int columnNumber = e.getColumnNumber();
    if (columnNumber > 0)
      columnNumberStr = ", column: " + columnNumber;
    logger.fatal(logger.getName() + ": " + e.getSystemId() + ": line: " + e.getLineNumber() + columnNumberStr + ": " + e.getMessage());
  }

  @Override
  public void fatalError(TransformerException e) {
    SourceLocator locator = e.getLocator();
    if (locator != null) {
      String errorMessage = e.getMessage();
      if (e.getException() != null)
        errorMessage = errorMessage + ": " + e.getException().getMessage();
      String columnNumberStr = "";
      int columnNumber = locator.getColumnNumber();
      if (columnNumber > 0)
        columnNumberStr = ", column: " + columnNumber;
      logger.fatal(logger.getName() + ": " + locator.getSystemId() + ": line: " + locator.getLineNumber() + columnNumberStr + ": " + errorMessage);
    } else {
      logger.fatal(logger.getName() + ": " + e.getMessageAndLocation());
    }
  }
}
