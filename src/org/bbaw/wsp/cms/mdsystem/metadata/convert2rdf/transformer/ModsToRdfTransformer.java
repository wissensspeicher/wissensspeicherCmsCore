/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer;

import org.bbaw.wsp.cms.mdsystem.util.MdSystemConfigReader;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * Instances of this (singleton) class transform mods files of the old WSP to OAI/ORE files.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 08.10.12
 * 
 */
public class ModsToRdfTransformer extends ToRdfTransformer implements IXsltTransformable {
  /**
   * The standard XSLT template for mods to rdf transformations. Use setXslInput() if you prefer another template.
   */
  public static final String PATH_XSLT_TEMPLATE = MdSystemConfigReader.getInstance().getConfig().getModsStylesheetPath();

  private static ModsToRdfTransformer instance;

  private ModsToRdfTransformer() throws ApplicationException {
    super(ToRdfTransformer.MODE_XSLT); // this is an XSLT transformer
    setXslInput(PATH_XSLT_TEMPLATE);
  }

  /**
   * 
   * @return the only existing instance.
   * @throws ApplicationException
   *           if the mode wasn't specified correctly.
   */
  public static ModsToRdfTransformer getInstance() throws ApplicationException {
    if (instance == null) {
      return new ModsToRdfTransformer();
    }
    return instance;
  }

  /**
   * Set the aggregation id. This will be set to the xslt transformation. It will cause an id for the aggregation's uri.
   * 
   * @param id
   */
  public void setAggregationId(final int id) {
    super.aggrId = id;
  }

}
