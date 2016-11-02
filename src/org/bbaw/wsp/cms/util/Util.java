package org.bbaw.wsp.cms.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class Util {

  public String toYearStr(String inputStr) {
    String retYearStr = inputStr.trim();
    int index = inputStr.indexOf("-");
    if (index > 0) {
      retYearStr = inputStr.substring(0, index);
      retYearStr = retYearStr.trim();
    }
    return retYearStr;
  }

  public Date toDate(String xsDateStr) throws ApplicationException {
    Date retDate = null;
    if (xsDateStr == null || xsDateStr.startsWith("0000-"))
      return null;
    try {
      if (xsDateStr.matches("[0-9][0-9][0-9][0-9]"))
        xsDateStr = xsDateStr + "-01-01T00:00:00.000Z";
      else if (xsDateStr.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"))
        xsDateStr = xsDateStr + "T00:00:00.000Z";
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      retDate = dateFormat.parse(xsDateStr);
    } catch (ParseException e) {
      try {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        retDate = dateFormat.parse(xsDateStr);
      } catch (ParseException e2) {
        throw new ApplicationException(e2);
      }
    }
    return retDate;
  }

  public String toXsDate(Date date) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    String xsDateStr = dateFormat.format(date);
    return xsDateStr;
  }
  
  public Double getSecondWithMillisecondsBetween(Date begin, Date end) {
    long beginMS = begin.getTime();
    long endMS = end.getTime();
    long elapsedSeconds = (endMS - beginMS) / 1000;
    long elapsedMilliSecondsAfterSeconds1 = (endMS - beginMS) - (elapsedSeconds * 1000);
    Double seconds = new Double(elapsedSeconds + "." + elapsedMilliSecondsAfterSeconds1); 
    return seconds;
  }
 
}
