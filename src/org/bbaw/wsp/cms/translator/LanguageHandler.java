package org.bbaw.wsp.cms.translator;

import java.util.ArrayList;
import java.util.Hashtable;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.dict.db.LexHandler;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.Form;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.Lemma;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.MorphologyCache;
import de.mpg.mpiwg.berlin.mpdl.lt.text.norm.Normalizer;

public class LanguageHandler {
	private static String[] SUPPORTED_LANGUAGES = {"deu", "eng", "fra", "lat", "grc"};
	private static String[] SUPPORTED_TRANSLATION_LANGUAGES = {"deu", "eng", "fra"};  // TODO
	private MorphologyCache morphologyCache;
	private Hashtable<String, String> translatedTerms;
	
  public LanguageHandler() throws ApplicationException {
    init();
  }

  private void init() throws ApplicationException {
	  morphologyCache = MorphologyCache.getInstance();
	  translatedTerms = new Hashtable<String, String>();
	}
	
  public String detectLanguage(String formName) throws ApplicationException {
	  try {
	    for (int i=0; i<SUPPORTED_LANGUAGES.length; i++) {
	      String language = SUPPORTED_LANGUAGES[i];
  	    ArrayList<Lemma> lemmas = morphologyCache.getLemmasByFormName(language, formName, Normalizer.DISPLAY);
        if (lemmas != null && ! lemmas.isEmpty())
          return language;
	    }
    } catch (Exception e) {
      throw new ApplicationException(e);
    }
	  return null; // if no lemma is found, no language is detected
	}
	
  public void translate(ArrayList<String> terms, String language) throws ApplicationException {
    String fromLanguage = language;
    for (int i=0; i<terms.size(); i++) {
      String term = terms.get(i);
      if (language == null || language.isEmpty()) {
        String detectedLang = detectLanguage(term);
        if (detectedLang != null)
          fromLanguage = detectedLang;
        else
          fromLanguage = "eng";  // default from language
      }
      ArrayList<String> forms = new ArrayList<String>();
      LexHandler lexHandler = LexHandler.getInstance();
      ArrayList<Lemma> lemmas = lexHandler.getLemmas(term, "form", fromLanguage, Normalizer.DICTIONARY, true);
      if (lemmas != null) {
        for (int j=0; j<lemmas.size(); j++) {
          Lemma lemma = lemmas.get(j);
          ArrayList<Form> lemmaForms = lemma.getFormsList();
          for (int k=0; k<lemmaForms.size(); k++) {
            Form form = lemmaForms.get(k);
            String formName = form.getFormName();
            forms.add(formName);
          } 
        }
      }
      translate(forms, fromLanguage, SUPPORTED_TRANSLATION_LANGUAGES);
    }
  }
  
  public ArrayList<String> getTranslations(ArrayList<String> terms, String fromLanguage) {
    ArrayList<String> translations = new ArrayList<String>();
    for (int i=0; i<terms.size(); i++) {
      String term = terms.get(i);
      for (int j=0; j<SUPPORTED_TRANSLATION_LANGUAGES.length; j++) {
        String toLanguage = SUPPORTED_TRANSLATION_LANGUAGES[j];
        String translation = getTranslation(term, fromLanguage, toLanguage);
        if (translation != null)
          translations.add(translation);
      }
    }
    return translations;
  }
  
  private String getTranslation(String term, String fromLanguage, String toLanguage) {
    String key = term + "###" + fromLanguage + "###" + toLanguage;
    return translatedTerms.get(key);
  }
  
  private void translate(ArrayList<String> terms, String fromLanguage, String[] toLanguages) throws ApplicationException {
    for (int i=0; i<toLanguages.length; i++) {
      String toLanguage = toLanguages[i];
      String[] termsArrayEmpty = new String[terms.size()];
      String[] termsArray = terms.toArray(termsArrayEmpty);
      String[] translatedTermsArray = null;
      if (fromLanguage != null)
        translatedTermsArray = MicrosoftTranslator.translate(termsArray, fromLanguage, toLanguage);
      if (translatedTermsArray != null) {
        for (int j=0; j<translatedTermsArray.length; j++) {
          try {
            String term = terms.get(j);
            String translation = translatedTermsArray[j];
            String key = term + "###" + fromLanguage + "###" + toLanguage;
            translatedTerms.put(key, translation);  // e.g.: key: hauses###deu###eng ; value: houses
          } catch (ArrayIndexOutOfBoundsException e) {
            // nothing: this would be an error in the Translation API and should normally not happen
          }
        }
      }
    }
  }
}
