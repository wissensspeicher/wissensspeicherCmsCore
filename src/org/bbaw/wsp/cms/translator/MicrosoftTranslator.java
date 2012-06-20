package org.bbaw.wsp.cms.translator;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.memetix.mst.detect.Detect;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class MicrosoftTranslator {
	private static final String KEY = "474A4E72DB217E37031EC190ACB4159378A6917C";  // free key to use with Microsoft server
	private static String[] DEST_LANGUAGES = {"eng", "ger", "fra"};

	private MicrosoftTranslator() {
	  // nothing: prevent instantiation
	}
	
	public static String[] getDestLanguages() {
    return DEST_LANGUAGES;
  }

  public static String translate(String query, String toLanguageStr) throws ApplicationException {
	  if (toLanguageStr == null)
	    throw new ApplicationException("MicrosoftTranslator: toLanguageStr is null");
	  String translation = null;
	  try {
      Translate.setKey(KEY);  // Set the API key once per JVM. It is set statically and applies to all services
      String langId = de.mpg.mpiwg.berlin.mpdl.lt.general.Language.getInstance().getLanguageId(toLanguageStr);  // e.g. "de" is delivered from "deu"
      Language toLanguage = Language.fromString(langId);
      translation = Translate.execute(query, toLanguage);
	  } catch (Exception e) {
	    throw new ApplicationException(e);
	  }
    return translation;
  }

  public static String translate(String query, String fromLanguageStr, String toLanguageStr) throws ApplicationException {
    if (fromLanguageStr == null)
      throw new ApplicationException("MicrosoftTranslator: toLanguageStr is null");
    if (toLanguageStr == null)
      throw new ApplicationException("MicrosoftTranslator: fromLanguageStr is null");
    String translation = null;
    try {
      Translate.setKey(KEY);
      String fromLangId = de.mpg.mpiwg.berlin.mpdl.lt.general.Language.getInstance().getLanguageId(fromLanguageStr);  // e.g. "de" is delivered from "deu"
      String toLangId = de.mpg.mpiwg.berlin.mpdl.lt.general.Language.getInstance().getLanguageId(toLanguageStr);  // e.g. "de" is delivered from "deu"
      Language fromLanguage = Language.fromString(fromLangId);
      Language toLanguage = Language.fromString(toLangId);
      translation = Translate.execute(query, fromLanguage, toLanguage);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return translation;
  }

  public static String[] translate(String[] query, String fromLanguageStr, String toLanguageStr) throws ApplicationException {
    if (fromLanguageStr == null)
      throw new ApplicationException("MicrosoftTranslator: toLanguageStr is null");
    if (toLanguageStr == null)
      throw new ApplicationException("MicrosoftTranslator: fromLanguageStr is null");
    String[] translation = null;
    try {
      Translate.setKey(KEY);  
      String fromLangId = de.mpg.mpiwg.berlin.mpdl.lt.general.Language.getInstance().getLanguageId(fromLanguageStr);  // e.g. "de" is delivered from "deu"
      String toLangId = de.mpg.mpiwg.berlin.mpdl.lt.general.Language.getInstance().getLanguageId(toLanguageStr);  // e.g. "de" is delivered from "deu"
      Language fromLanguage = Language.fromString(fromLangId);
      Language toLanguage = Language.fromString(toLangId);
      translation = Translate.execute(query, fromLanguage, toLanguage);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return translation;
  }

  public static String detectLanguageCode(String query) throws ApplicationException {
	  String langCode = null;
	  try {
      Detect.setKey(KEY);
      Language detectedLanguageCode = Detect.execute(query);
      langCode = detectedLanguageCode.toString();
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
	  return langCode;
	}
	 
	public static String detectLanguageName(String query) throws ApplicationException {
    String langName = null;
    try {
      Detect.setKey(KEY);
      Language detectedLanguage = Detect.execute(query);
      langName = detectedLanguage.getName(Language.GERMAN);
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
    return langName;
	}
	
	
	// TODO
  public static String[] translateQuery(String[] queryToTranslate) throws Exception {
    // Set the API key once per JVM. It is set statically and applies to all services
    Translate.setKey(KEY);
    
 // From AUTO_DETECT -> German 
    System.out.println("queryToTranslate : "+queryToTranslate);
    String[] translatedText = Translate.execute(queryToTranslate, Language.GERMAN);
    System.out.println("queryToTranslate AUTO_DETECT -> German : ");
    for (String string : translatedText) {
      System.out.print(string);
      System.out.print(" ");
}
    System.out.println();
    
    
    // From AUTO_DETECT -> French 
//    translatedText = Translate.execute("Bonjour", Language.FRENCH, Language.ENGLISH);
    String[] ranslatedText = Translate.execute(queryToTranslate, Language.FRENCH);
    System.out.println("queryToTranslate AUTO_DETECT -> French : ");
    for (String string : ranslatedText) {
      System.out.print(string);
      System.out.print(" ");
}
    System.out.println();
   
    // English AUTO_DETECT -> Arabic
    String[] anslatedText = Translate.execute(queryToTranslate, Language.ARABIC);
    System.out.println("queryToTranslate AUTO_DETECT -> Arabic : ");
    for (String string : anslatedText) {
      System.out.print(string);
      System.out.print(" ");
}
    System.out.println();
    return translatedText;
}

}
