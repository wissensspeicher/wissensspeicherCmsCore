package org.bbaw.wsp.cms.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.RDFSRuleReasonerFactory;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

public class ReasonerTest {

	public static void main(String[] args) {

		String file = "/home/shk2/quellen/projects/Orinoco.rdf";

		RdfHandler rdfHandler = new RdfHandler();

		String NS = "urn:x-hp-jena:eg/";

		Model tempModel = ModelFactory.createDefaultModel();
		Model rdfsModel = ModelFactory.createRDFSModel(tempModel);
		rdfsModel = rdfHandler.fillModelFromFile(rdfsModel, file);
		// Property p = rdfsModel.createProperty(NS, "p");
		// Property q = rdfsModel.createProperty(NS, "q");
		// rdfsModel.add(p, RDFS.subPropertyOf, q);
		// rdfsModel.createResource(NS + "a").addProperty(p, "fuu");

		Resource config = ModelFactory.createDefaultModel().createResource()
				.addProperty(ReasonerVocabulary.PROPsetRDFSLevel, "simple");

		Reasoner reasoner = RDFSRuleReasonerFactory.theInstance().create(config);

		reasoner.setParameter(ReasonerVocabulary.PROPsetRDFSLevel,
				ReasonerVocabulary.RDFS_SIMPLE);

		InfModel inf = ModelFactory.createInfModel(reasoner, rdfsModel);
		// Resource a = inf.getResource(NS + "a");
		// System.out.println("Statement: " + a.getProperty(q));
		// inf.write(System.out, "RDF/XML-ABBREV");
		File fileEx = new File("/home/shk2/quellen/projects/Orinoco.xml");
		try {
			inf.write(new FileOutputStream(fileEx), "RDF/XML");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
