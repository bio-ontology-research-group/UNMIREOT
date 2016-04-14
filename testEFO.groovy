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

/*OWLImportsDeclaration importDeclaration=manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://aber-owl.net/onts/GO_36.ont"));
manager.applyChange(new AddImport(ontology, importDeclaration));*/

manager.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://purl.obolibrary.org/obo/go.owl"),
       IRI.create("http://aber-owl.net/onts/GO_36.ont")));

// Reason

ReasonerConfiguration eConf = ReasonerConfiguration.getConfiguration()
eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")

OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

OWLReasonerConfiguration rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);
OWLReasoner oReasoner = reasonerFactory.createReasoner(ontology, rConf);
oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

println oReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size()
