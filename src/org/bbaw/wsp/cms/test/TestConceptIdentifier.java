package org.bbaw.wsp.cms.test;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.ConceptIdentfierSearchMode;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.ConceptIdentifier;

public class TestConceptIdentifier {

	/**
	 * TestClass
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new ConceptIdentifier().initIdentifying("Humboldt nix hallo",
				ConceptIdentfierSearchMode.METHODE_OR);

	}

}