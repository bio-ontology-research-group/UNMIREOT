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

  println "[CREATEUNMIREOT] Creating new ontology with imports"

  def added = []

  mireotOntologies.each {
    println "[CREATEUNMIREOT] Adding " + it + " to ontology"
 
    OWLOntology modOntology
    OWLOntologyManager modManager = OWLManager.createOWLOntologyManager();
    modOntology = modManager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ontologyIRI)), config);

    OWLImportsDeclaration importDeclaration = modManager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://aber-owl.net/ontology/"+it+"/download"));
    manager.applyChange(new AddImport(modOntology, importDeclaration));
    manager.applyChange(new AddImport(ontology, importDeclaration));

    File fileFormated = new File("mods/EFO_"+it+".ontology");
    manager.saveOntology(modOntology, IRI.create(fileFormated.toURI()));
    println "[CREATEUNMIREOT] Saved EFO with " + it
  }

  File fileFormated = new File("mods/EFO_all.ontology");
  manager.saveOntology(ontology, IRI.create(fileFormated.toURI()));
  println "[CREATEUNMIREOT] Saved EFO with all"
}
