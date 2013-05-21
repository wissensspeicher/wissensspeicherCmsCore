package org.bbaw.wsp.cms.test;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.RDFSRuleReasonerFactory;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

public class ReasonerTest {

	public static void main(String[] args) {
		String NS = "urn:x-hp-jena:eg/";

		Model tempModel = ModelFactory.createDefaultModel();
		Model rdfsModel = ModelFactory.createRDFSModel(tempModel);
		Property p = rdfsModel.createProperty(NS, "p");
		Property q = rdfsModel.createProperty(NS, "q");
		rdfsModel.add(p, RDFS.subPropertyOf, q);
		rdfsModel.createResource(NS + "a").addProperty(p, "fuu");

		Resource config = ModelFactory.createDefaultModel().createResource()
				.addProperty(ReasonerVocabulary.PROPsetRDFSLevel, "simple");

		Reasoner reasoner = RDFSRuleReasonerFactory.theInstance().create(config);

		reasoner.setParameter(ReasonerVocabulary.PROPsetRDFSLevel,
				ReasonerVocabulary.RDFS_SIMPLE);

		InfModel inf = ModelFactory.createInfModel(reasoner, rdfsModel);
		Resource a = inf.getResource(NS + "a");
		System.out.println("Statement: " + a.getProperty(q));

	}
}
