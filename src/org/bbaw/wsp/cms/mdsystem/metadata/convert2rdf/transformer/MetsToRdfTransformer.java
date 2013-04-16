/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer;

import org.bbaw.wsp.cms.mdsystem.util.MdSystemConfigReader;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * Instances of this (singleton) class transform mets files to OAI/ORE files.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 17.01.13
 * 
 */
public class MetsToRdfTransformer extends ToRdfTransformer implements
		IXsltTransformable {
	/**
	 * The standard XSLT template for mets to rdf transformations. Use
	 * setXslInput() if you prefer another template.
	 */
	public static final String PATH_XSLT_TEMPLATE = MdSystemConfigReader
			.getInstance().getConfig().getMetsStylesheetPath();

	private static MetsToRdfTransformer instance;

	private MetsToRdfTransformer() throws ApplicationException {
		super(ToRdfTransformer.MODE_XSLT); // this is an XSLT transformer
		setXslInput(PATH_XSLT_TEMPLATE);
	}

	/**
	 * 
	 * @return the only existing instance.
	 * @throws ApplicationException
	 *           if the mode wasn't specified correctly.
	 */
	public static MetsToRdfTransformer getInstance() throws ApplicationException {
		if (instance == null) {
			return new MetsToRdfTransformer();
		}
		return instance;
	}

	/**
	 * Set the aggregation id. This will be set to the xslt transformation.
	 * 
	 * @param id
	 */
	public void setAggregationId(final int id) {
		super.aggrId = id;
	}

}
