/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.util;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;

/**
 * API interface for all accesses on the configuration file.
 * 
 * @author Sascha Feldmann (<a
 *         href="mailto:wsp-shk1@bbaw.de">wsp-shk1@bbaw.de</a>)
 * @date 28.01.2013
 * 
 */
public interface IMdsystemConfigRecord {
  /**
   * Used by the EdocToRdfTransformer
   * 
   * @return the path to the eDoc template.
   */
  public String geteDocTemplatePath();

  /**
   * Used by the ModsToRdfTransformer
   * 
   * @return the path to the mods to OAI/ORE XSLT stylesheet.
   */
  public String getModsStylesheetPath();

  /**
   * Used by the MetsToRdfTransformer
   * 
   * @return the path to the mets to OAI/ORE XSLT stylesheet.
   */
  public String getMetsStylesheetPath();

  /**
   * Used by the {@link JenaMain}
   * 
   * @return the path to the dataset.
   */
  public String getDatasetPath();

  /**
   * Used by the {@link WspRdfStore}
   * 
   * @return the path to the larq index.
   */
  public String getLarqIndexPath();
}
