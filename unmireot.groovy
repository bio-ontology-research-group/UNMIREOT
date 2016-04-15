@Grapes([
          @Grab(group='com.google.code.gson', module='gson', version='2.3.1'),
          @Grab(group='org.slf4j', module='slf4j-log4j12', version='1.7.10'),
          @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.1.0'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.1.0'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.1.0'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.1.0'),
          @Grab(group='org.codehaus.gpars', module='gpars', version='1.1.0'),
          @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' ),
	  @GrabConfig(systemClassLoader=true)
	])
 
import org.semanticweb.owlapi.io.* 
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.AddImport
import groovyx.net.http.HTTPBuilder
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

def ontologyIRI = args[0]

// Load input ontology

println "[UNMIREOT] Loading ontology from " + ontologyIRI

OWLOntology ontology
OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();

config.setFollowRedirects(true);
config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ontologyIRI)), config);

println "[UNMIREOT] Finding missing ontology imports"

def onts = []
new File('oo.txt').eachLine { line ->
  onts << line
}

def mireotOntologies = []

// get the things from the uris
ontology.getClassesInSignature().each {
  onts.each { oName ->
    def m = it =~ oName
    if(m && !(m[0] in mireotOntologies)) {
      mireotOntologies << m[0]
    }
  }
}

println "[UNMIREOT] The following ontologies are referenced: " + mireotOntologies

new HTTPBuilder('http://aber-owl.net/').get(path: 'service/api/getStatuses.groovy') { resp, ontologies ->

  mireotOntologies = mireotOntologies.findAll {
    return ontologies[it].status == 'classified';
  }

  println "[UNMIREOT] The following ontologies are referenced after removing dead ontologies: " + mireotOntologies

  println "[UNMIREOT] Creating new ontology with imports"

  def added = []

  mireotOntologies.each {
    println "[UNMIREOT] Adding " + it + " to ontology"
 
    OWLOntology modOntology
    OWLOntologyManager modManager = OWLManager.createOWLOntologyManager();
    modOntology = modManager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ontologyIRI)), config);

    OWLImportsDeclaration importDeclaration = modManager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://localhost/rtestserv/ontology/"+it+"/download"));
    manager.applyChange(new AddImport(modOntology, importDeclaration));

    File fileFormated = new File("unmireot_test.ontology");
    manager.saveOntology(modOntology, IRI.create(fileFormated.toURI()));

    // Load and reason the new ontology

    added << it
    println "[UNMIREOT] Loading new ontology with " + it

    def newOntology
    def newManager
    try {
      newManager = OWLManager.createOWLOntologyManager();
      newOntology = newManager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("file:///home/aberowl/efotest/unmireot_test.ontology")), config);

      ReasonerConfiguration eConf = ReasonerConfiguration.getConfiguration()
      eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
      eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
      eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")

      OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

      OWLReasonerConfiguration rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);
      OWLReasoner oReasoner = reasonerFactory.createReasoner(newOntology, rConf);
      oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

      println "[UNMIREOT] Unsatisfiable with " + it + " classes: " + oReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size()
  for (OWLClass cl : newOntology.getClassesInSignature(true)) {
    if(!oReasoner.isSatisfiable(cl)) {
      System.out.println("[UNMIREOT] Unsatisfiable class: " + cl.getIRI())
    }
  }

    } catch(e) {
      println "[UNMIREOT] Unable to load ontology with " + it
      println "[UNMIREOT] Removing "  + it + " from imports"
      added.remove(it)
    }
  }

  // Modify the original ontology, adding all of the valid imports
  println "[UNMIREOT] Adding all available ontologies as imports"
  added.each {
    OWLImportsDeclaration importDeclaration = manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://aber-owl.net/ontology/"+it+"/download"));
    manager.applyChange(new AddImport(ontology, importDeclaration));
  }

  File fileFormated = new File("unmireot_test.ontology");
  manager.saveOntology(ontology, IRI.create(fileFormated.toURI()));

  println "[UNMIREOT] Loading new ontology with imports"

  def newManager = OWLManager.createOWLOntologyManager();
  def newOntology = newManager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("file:///home/aberowl/efotest/unmireot_test.ontology")), config);

  println "[UNMIREOT] Reasoning new ontology"
  ReasonerConfiguration eConf = ReasonerConfiguration.getConfiguration()
  eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
  eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
  eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")

  OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

  OWLReasonerConfiguration rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);
  OWLReasoner oReasoner = reasonerFactory.createReasoner(newOntology, rConf);
  oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

  println "[UNMIREOT] Unsatisfiable classes: " + oReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size()
  for (OWLClass cl : newOntology.getClassesInSignature()) {
    if(!oReasoner.isSatisfiable(cl)) {
      System.out.println("Unsatisfiable: " + cl.getIRI())
    }
  }
}
