/**
 * 
 */
package org.bbaw.wsp.cms.dochandler.parser.text.parser.mapper;

import org.bbaw.wsp.cms.document.MetadataRecord;

/**
 * This is a small class. Objects represent a institut mapping configuration (Institut metadata field by the eDoc index.html files) that are put into the {@link MetadataRecord}. *
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 11.04.2013
 * 
 */
public class InstitutMapping {

  private final String institut;
  private final String configId;

  /**
   * Create a new mapping value. Those values will replace another one (as defined in the {@link EdocFieldValueMapper}.
   * 
   * @param institut
   * @param configId
   */
  public InstitutMapping(final String institut, final String configId) {
    this.institut = institut;
    this.configId = configId;
  }

  /**
   * 
   * @return the new value of the metadata field "Institut"
   */
  public String getInstitut() {
    return institut;
  }

  /**
   * @return the matching value for the configuration file (its id).
   */
  public String getConfigId() {
    return configId;
  }

}
