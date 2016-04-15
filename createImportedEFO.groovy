@Grapes([
          @Grab(group='com.google.code.gson', module='gson', version='2.3.1'),

          @Grab(group='org.slf4j', module='slf4j-log4j12', version='1.7.10'),
	  @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.1.0'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.1.0'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.1.0'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.1.0'),

          @Grab(group='org.codehaus.gpars', module='gpars', version='1.1.0'),
	  @GrabConfig(systemClassLoader=true)
	])
 

import org.semanticweb.owlapi.io.* 
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.AddImport


import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration
import org.semanticweb.elk.reasoner.config.*
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.owllink.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.search.*;
import org.semanticweb.owlapi.manchestersyntax.renderer.*;
import org.semanticweb.owlapi.reasoner.structural.*


OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
config.setFollowRedirects(true);
config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("file:///home/reality/Projects/efotest/EFO.ont")), config);

// Add GO
/*
OWLImportsDeclaration importDeclaration=manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://aber-owl.net/onts/GO_36.ont"));
manager.applyChange(new AddImport(ontology, importDeclaration));

OWLImportsDeclaration importDeclaration2=manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://aber-owl.net/onts/BFO_55.ont"));
manager.applyChange(new AddImport(ontology, importDeclaration2));
*/
//OWLImportsDeclaration importDeclaration3=manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://aber-owl.net/onts/CL_54.ont"));
//manager.applyChange(new AddImport(ontology, importDeclaration3));
// 
//you can list all the URI's (getClassesInSignature(true)) and cut off the last part, then sort them, to see what ontologies they use

OWLImportsDeclaration importDeclaration3=manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://aber-owl.net/onts/UBERON_33.ont"));
manager.applyChange(new AddImport(ontology, importDeclaration3));

File fileformated = new File("efowithimports.owl");
manager.saveOntology(ontology, IRI.create(fileformated.toURI()));
