package org.bbaw.wsp.cms.translator;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.memetix.mst.detect.Detect;


public class MicrosoftTranslator {
	
	private static final String key = "474A4E72DB217E37031EC190ACB4159378A6917C";
	
	//prevent instantiation
	private MicrosoftTranslator(){}
	
	
	public static String translateQuery(String queryToTranslate) throws Exception {
        // Set the API key once per JVM. It is set statically and applies to all services
        Translate.setKey(key);
        
     // From AUTO_DETECT -> German 
        System.out.println("queryToTranslate : "+queryToTranslate);
        String translatedText = Translate.execute(queryToTranslate, Language.GERMAN);
        System.out.println("queryToTranslate AUTO_DETECT -> German : " + translatedText);
        
        
        // From AUTO_DETECT -> French 
//        translatedText = Translate.execute("Bonjour", Language.FRENCH, Language.ENGLISH);
        String ranslatedText = Translate.execute(queryToTranslate, Language.FRENCH);
        System.out.println("queryToTranslate AUTO_DETECT -> French : " + ranslatedText);
       
        // English AUTO_DETECT -> Arabic
        String anslatedText = Translate.execute(queryToTranslate, Language.ARABIC);
        System.out.println("queryToTranslate AUTO_DETECT -> Arabic: " + anslatedText);
        
        return translatedText;
    }

	public static String[] translateQuery(String[] queryToTranslate) throws Exception {
        // Set the API key once per JVM. It is set statically and applies to all services
        Translate.setKey(key);
        
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
//        translatedText = Translate.execute("Bonjour", Language.FRENCH, Language.ENGLISH);
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
	
	public static String detectLanguageCode(String queryToDetect) throws Exception {
    Detect.setKey(key);
    Language detectedLanguageCode = Detect.execute(queryToDetect);
    return detectedLanguageCode.toString();
	}
	 
	public static String detectLanguageName(String queryToDetect) throws Exception {
    Detect.setKey(key);
    Language detectedLanguageCode = Detect.execute(queryToDetect);
    String detectedLanguageName = detectedLanguageCode.getName(Language.GERMAN);
    return detectedLanguageName;
	}
	
}
