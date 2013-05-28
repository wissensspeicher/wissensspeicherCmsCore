/**
 * 
 */
package org.bbaw.wsp.cms.dochandler.parser.text.parser.mapper;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

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
   * Create a new {@link EdocFieldValueMapper} instance.
   */
  public EdocFieldValueMapper() {
    initialize();
  }

  private void initialize() {
    logger = Logger.getLogger(EdocFieldValueMapper.class);
  }

  /**
   * Fill the institut map with the replacing values once.
   * 
   * @return a map in the form (key: the value to be replaced; value: InstitutMapping object which contains the replacing values)
   */
  private static Map<String, InstitutMapping> fillInstitutMap() {
    final Map<String, InstitutMapping> mapping = new HashMap<String, InstitutMapping>();
    // without concrete institute: map to root node "edoc"
    mapping.put("Berlin-Brandenburgische Akademie der Wissenschaften", new InstitutMapping("Akademiebibliothek: Elektronischer Dokumentenserver (EDOC)", "edoc"));
    mapping.put("Veröffentlichungen von Akademiemitgliedern", new InstitutMapping("Akademiebibliothek: Elektronischer Dokumentenserver (EDOC)", "edoc"));
    mapping.put("Veröffentlichungen von Akademiemitarbeitern", new InstitutMapping("Akademiebibliothek: Elektronischer Dokumentenserver (EDOC)", "edoc"));
    mapping.put("Veröffentlichungen der Vorgängerinstitutionen der Berlin-Brandenburgischen Akademie der Wissenschaften", new InstitutMapping("Akademiebibliothek: Elektronischer Dokumentenserver (EDOC)", "edoc"));
    mapping.put("Veröffentlichungen externer Institutionen", new InstitutMapping("Akademiebibliothek: Elektronischer Dokumentenserver (EDOC)", "edoc"));
    // mappings to institutes
    // institutmapping changed to "Psychologisches Denken und psychologische Praxis" to match name in the wsp.normdata.rdf
    mapping.put("Interdisziplinäre Arbeitsgruppe Psychologisches Denken und psychologische Praxis", new InstitutMapping("Psychologisches Denken und psychologische Praxis", "pd"));
    mapping.put("Akademienvorhaben Altägyptisches Wörterbuch", new InstitutMapping("Altägyptisches Wörterbuch", "aaew"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Berliner Akademiegeschichte im 19. und 20. Jahrhundert", new InstitutMapping("Berliner Akademiegeschichte im 19. und 20. Jahrhundert", "bag"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Wissenschaftliche Politikberatung in der Demokratie", new InstitutMapping("Wissenschaftliche Politikberatung in der Demokratie", "wpd"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Die Herausforderung durch das Fremde", new InstitutMapping("Die Herausforderung durch das Fremde", "fremde"));
    mapping.put("Initiative Wissen für Entscheidungsprozesse", new InstitutMapping("Wissen für Entscheidungsprozesse - Forschung zum Verhältnis von Wissenschaft, Politik und Gesellschaft", "wfe"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Gentechnologiebericht", new InstitutMapping("Gentechnologiebericht", "gen"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Wissenschaften und Wiedervereinigung", new InstitutMapping("Wissenschaft und Wiedervereinigung", "ww"));
    mapping.put("Akademienvorhaben Leibniz-Edition Berlin", new InstitutMapping("Leibniz-Edition Berlin", "leibber"));
    mapping.put("Akademienvorhaben Deutsches Wörterbuch von Jacob Grimm und Wilhelm Grimm", new InstitutMapping("Deutsches Wörterbuch von Jacob Grimm und Wilhelm Grimm", "dwb"));
    mapping.put("Akademienvorhaben Census of Antique Works of Art and Architecture Known in the Renaissance", new InstitutMapping("Census of Antique Works of Art and Architecture Known in the Renaissance", "census"));
    mapping.put("Akademienvorhaben Protokolle des Preußischen Staatsministeriums Acta Borussica", new InstitutMapping("Protokolle des Preußischen Staatsministeriums Acta Borussica", "ab"));
    mapping.put("Akademienvorhaben Berliner Klassik", new InstitutMapping("Berliner Klassik", "bk"));
    mapping.put("Akademienvorhaben Alexander-von-Humboldt-Forschung", new InstitutMapping("Alexander-von-Humboldt-Forschungsstelle", "avh"));
    mapping.put("Akademienvorhaben Leibniz-Edition Potsdam", new InstitutMapping("Leibniz-Edition Potsdam", "leibpots"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Globaler Wandel", new InstitutMapping("Globaler Wandel - Regionale Entwicklung", "globw"));
    mapping.put("Akademienvorhaben Jahresberichte für deutsche Geschichte", new InstitutMapping("Jahresberichte für deutsche Geschichte", "jdg"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Die Welt als Bild", new InstitutMapping("Die Welt als Bild", "wab"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Optionen zukünftiger industrieller Produktionssysteme", new InstitutMapping("Optionen zukünftiger industrieller Produktionssysteme", "ops"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Frauen in Akademie und Wissenschaft", new InstitutMapping("Frauen in Akademie und Wissenschaft", "frauen"));
    mapping.put("Akademienvorhaben Corpus Coranicum", new InstitutMapping("Corpus Coranicum", "coranicum"));
    mapping.put("Initiative Telota", new InstitutMapping("TELOTA", "telota"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Gemeinwohl und Gemeinsinn", new InstitutMapping("Gemeinwohl und Gemeinsinn", "gg"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Bildkulturen", new InstitutMapping("Bildkulturen - Perspektiven verschiedener Bildkulturen", "bild"));
    mapping.put("Akademienvorhaben Monumenta Germaniae Historica", new InstitutMapping("Monumenta Germaniae Historica", "mgh"));
    mapping.put("Interdisziplinäre Arbeitsgruppe LandInnovation", new InstitutMapping("Globaler Wandel- Regionale Entwicklung", "globw"));
    mapping.put("Akademienvorhaben Marx-Engels-Gesamtausgabe", new InstitutMapping("Marx-Engels-Gesamtausgabe", "mega"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Strukturbildung und Innovation", new InstitutMapping("Studiengruppe Strukturbildung und Innovation", "sui"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Sprache des Rechts, Vermitteln, Verstehen, Verwechseln", new InstitutMapping("Sprache des Rechts, Vermitteln, Verstehen, Verwechseln", "srvvv"));
    mapping.put("Akademienvorhaben Die Griechischen Christlichen Schriftsteller", new InstitutMapping("Die Griechischen Christlichen Schriftsteller", "gcs"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Gesundheitsstandards", new InstitutMapping("Gesundheitsstandards", "gs"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Humanprojekt", new InstitutMapping("Humanprojekt", "hum"));
    mapping.put("Akademienvorhaben Turfanforschung", new InstitutMapping("Turfanforschung", "turfan"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Gegenworte - Hefte für den Disput über Wissen", new InstitutMapping("Gegenworte - Hefte für den Disput über Wissen", "gw"));
    mapping.put("Interdisziplinäre Arbeitsgruppe Strategien zur Abfallenergieverwertung", new InstitutMapping("Strategien zur Abfallenergieverwertung", "sza"));
    return mapping;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.dochandler.parser.text.parser.mapper.IFieldValueMapper#mapField(java.lang.String, java.lang.String, org.bbaw.wsp.cms.document.MetadataRecord)
   */
  @Override
  public InstitutMapping mapField(final String parsedFieldName, final String parsedFieldValue) throws ApplicationException {
    switch (parsedFieldName) {
    case EDOC_FIELD_INSTITUT:
      return mapInstitut(parsedFieldValue);
    }
    return null;
  }

  /**
   * Map the edoc "institut" to our projects / vorhabens ' names.
   * 
   * @param parsedFieldValue
   * @param mdRecord
   * @return {@link InstitutMapping} the new values.
   */
  private InstitutMapping mapInstitut(final String parsedFieldValue) {
    final InstitutMapping mapping = institutMap.get(parsedFieldValue);
    if (mapping != null) {
      final String newValue = mapping.getInstitut();
      final String newCollection = mapping.getConfigId();

      final InstitutMapping newValues = new InstitutMapping(newValue, newCollection);

      return newValues;
    } else {
      logger.error("Cannot replace the value of eDoc's \"Institut\" metadata field. The value to be replaced: " + parsedFieldValue);
      return null;
    }
  }
}
