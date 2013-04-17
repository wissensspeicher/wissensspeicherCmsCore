package org.bbaw.wsp.cms.dochandler.parser.text.parser.mapper;

import org.bbaw.wsp.cms.document.MetadataRecord;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * A FieldMapper instance offers a functionality to map the value of a parsed metadata field to our {@link MetadataRecord} field.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 11.04.2013
 * 
 */
public interface IFieldValueMapper {

  /**
   * Map the value of a parsed field. Execute some algorithm to replace the old parsed value and insert the new value in the {@link MetadataRecord}.
   * 
   * @param parsedFieldName
   *          the name of the parsed field (one metadata field, e.g. "Institut")
   * @param parsedFieldValue
   *          the value of the parsed metadata field
   * @return {@link InstitutMapping} with the new values
   * @throws ApplicationException
   *           if the field couln't be mapped
   */
  InstitutMapping mapField(final String parsedFieldName, final String parsedFieldValue) throws ApplicationException;

}
