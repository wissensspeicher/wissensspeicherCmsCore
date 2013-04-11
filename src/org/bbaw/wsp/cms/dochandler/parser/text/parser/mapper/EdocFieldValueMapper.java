/**
 * 
 */
package org.bbaw.wsp.cms.dochandler.parser.text.parser.mapper;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.document.MetadataRecord;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This special {@link IFieldValueMapper} maps the values of metadata fields from the edoc index files.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 11.04.2013
 * 
 */
public class EdocFieldValueMapper implements IFieldValueMapper {
  public static final String EDOC_FIELD_INSTITUT = "Institut";
  private static Map<String, InstitutMapping> institutMap = fillInstitutMap();
  private static Logger logger;

  /**
   * Fill the institut map with the replacing values once.
   * 
   * @return a map in the form (key: the value to be replaced; value: InstitutMapping object which contains the replacing values)
   */
  private static Map<String, InstitutMapping> fillInstitutMap() {
    final Map<String, InstitutMapping> mapping = new HashMap<String, InstitutMapping>();
    mapping.put("Interdisziplin�re Arbeitsgruppe Psychologisches Denken und psychologische Praxis", new InstitutMapping("Interdisziplinäre Arbeitsgruppe Psychologisches Denken und psychologische Praxis", "pd"));
    mapping.put("Akademienvorhaben Alt�gyptisches W�rterbuch", new InstitutMapping("Alt�gyptisches W�rterbuch", "aaew"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Berliner Akademiegeschichte im 19. und 20. Jahrhundert", new InstitutMapping("Berliner Akademiegeschichte im 19. und 20. Jahrhundert", "bag"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Wissenschaftliche Politikberatung in der Demokratie", new InstitutMapping("Wissenschaftliche Politikberatung in der Demokratie", "wpd"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Die Herausforderung durch das Fremde", new InstitutMapping("Die Herausforderung durch das Fremde", "fremde"));
    mapping.put("Initiative Wissen f�r Entscheidungsprozesse", new InstitutMapping("Wissen f�r Entscheidungsprozesse � Forschung zum Verh�ltnis von Wissenchaft, Politik und Gesellschaft", "wfe"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Gentechnologiebericht", new InstitutMapping("Gentechnologiebericht", "gen"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Wissenschaften und Wiedervereinigung", new InstitutMapping("Wissenschaften und Wiedervereinigung", "ww"));
    mapping.put("Akademienvorhaben Leibniz-Edition Berlin", new InstitutMapping("Leibniz-Edition Berlin", "leibber"));
    mapping.put("Akademienvorhaben Deutsches W�rterbuch von Jacob Grimm und Wilhelm Grimm", new InstitutMapping("Deutsches W�rterbuch von Jacob Grimm und Wilhelm Grimm", "dwb"));
    mapping.put("Akademienvorhaben Census of Antique Works of Art and Architecture Known in the Renaissance", new InstitutMapping("Census of Antique Works of Art and Architecture Known in the Renaissance", "census"));
    mapping.put("Akademienvorhaben Protokolle des Preu�ischen Staatsministeriums Acta Borussica", new InstitutMapping("Protokolle des Preu�ischen Staatsministeriums Acta Borussica", "ab"));
    mapping.put("Akademienvorhaben Berliner Klassik", new InstitutMapping("Berliner Klassik", "bk"));
    mapping.put("Akademienvorhaben Alexander-von-Humboldt-Forschung", new InstitutMapping("Alexander-von Humboldt-Forschungsstelle", "avh"));
    mapping.put("Akademienvorhaben Leibniz-Edition Potsdam", new InstitutMapping("Leibniz-Edition Potsdam", "leibpots"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Globaler Wandel", new InstitutMapping("Globaler Wandel- Regionale Entwicklung", "globw"));
    mapping.put("Akademienvorhaben Jahresberichte f�r deutsche Geschichte", new InstitutMapping("Jahresberichte f�r deutsche Geschichte", "jdg"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Die Welt als Bild", new InstitutMapping("Die Welt als Bild", "wab"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Optionen zuk�nftiger industrieller Produktionssysteme", new InstitutMapping("Optionen zuk�nftiger industrieller Produktionssysteme", "ops"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Frauen in Akademie und Wissenschaft", new InstitutMapping("Frauen in Akademie und Wissenschaft", "frauen"));
    mapping.put("Akademienvorhaben Corpus Coranicum", new InstitutMapping("Corpus Coranicum", "coranicum"));
    mapping.put("Initiative Telota", new InstitutMapping("Telota", "telota"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Gemeinwohl und Gemeinsinn", new InstitutMapping("Gemeinwohl und Gemeinsinn", "gg"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Bildkulturen", new InstitutMapping("Bildkulturen", "bild"));
    mapping.put("Akademienvorhaben Monumenta Germaniae Historica", new InstitutMapping("Monumenta Germaniae Historica-Constitutiones", "mgh"));
    mapping.put("Interdisziplin�re Arbeitsgruppe LandInnovation", new InstitutMapping("Globaler Wandel- Regionale Entwicklung", "globw"));
    mapping.put("Akademienvorhaben Marx-Engels-Gesamtausgabe", new InstitutMapping("Marx-Engels-Gesamtausgabe", "mega"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Strukturbildung und Innovation", new InstitutMapping("Studiengruppe Strukturbildung und Innovation", "sui"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Sprache des Rechts, Vermitteln, Verstehen, Verwechseln", new InstitutMapping("Sprache des Rechts, Vermitteln, Verstehen, Verwechseln", "srvvv"));
    mapping.put("Akademienvorhaben Die Griechischen Christlichen Schriftsteller", new InstitutMapping("Die Griechischen Christlichen Schriftsteller", "gcs"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Gesundheitsstandards", new InstitutMapping("Gesundheitsstandards", "gs"));
    mapping.put("Interdisziplin�re Arbeitsgruppe Humanprojekt", new InstitutMapping("Humanprojekt", "hum"));
    mapping.put("Akademienvorhaben Turfanforschung", new InstitutMapping("Turfanforschung", "turfan"));
    return mapping;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.dochandler.parser.text.parser.mapper.IFieldValueMapper#mapField(java.lang.String, java.lang.String, org.bbaw.wsp.cms.document.MetadataRecord)
   */
  @Override
  public void mapField(final String parsedFieldName, final String parsedFieldValue, final MetadataRecord mdRecord) throws ApplicationException {
    switch (parsedFieldName) {
    case EDOC_FIELD_INSTITUT:
      mapInstitut(parsedFieldValue, mdRecord);
      break;
    }
  }

  /**
   * Map the edoc "institut" to our projects / vorhabens ' names.
   * 
   * @param parsedFieldValue
   * @param mdRecord
   */
  private void mapInstitut(final String parsedFieldValue, final MetadataRecord mdRecord) {
    final InstitutMapping mapping = institutMap.get(parsedFieldValue);
    if (mapping != null) {
      final String newValue = mapping.getInstitut();
      final String newCollection = mapping.getConfigId();
      String collectionsExisting = newCollection;
      if (mdRecord.getCollectionNames() != null) {
        collectionsExisting += ";" + mdRecord.getCollectionNames();
      }
      mdRecord.setPublisher(newValue);
      mdRecord.setCollectionNames(collectionsExisting);
    } else {
      System.err.println("Canno't replace the value of eDoc's \"Institut\" metadata field. The value to be replaced: " + parsedFieldValue);
    }
  }
}
