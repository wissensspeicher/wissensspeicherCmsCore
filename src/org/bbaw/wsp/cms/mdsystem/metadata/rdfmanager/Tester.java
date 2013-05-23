package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.ConceptIdentfierSearchMode;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.ConceptIdentifier;

public class Tester {

	/**
	 * TestClass
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new ConceptIdentifier().initIdentifying("Günter",
				ConceptIdentfierSearchMode.METHODE_OR);

	}

}
