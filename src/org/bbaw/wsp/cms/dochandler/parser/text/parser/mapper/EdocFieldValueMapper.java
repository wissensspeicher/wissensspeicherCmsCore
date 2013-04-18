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
      logger.error("Canno't replace the value of eDoc's \"Institut\" metadata field. The value to be replaced: " + parsedFieldValue);
      return null;
    }
  }
}
