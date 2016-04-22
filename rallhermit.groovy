@Grapes([
          @Grab(group='com.google.code.gson', module='gson', version='2.3.1'),
          @Grab(group='org.slf4j', module='slf4j-log4j12', version='1.7.10'),
          @Grab(group='com.hermit-reasoner', module='org.semanticweb.hermit', version='1.3.8.4'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='3.4.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='3.4.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='3.4.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='3.4.3'),
          @Grab(group='org.codehaus.gpars', module='gpars', version='1.1.0'),
          @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' ),
	  @GrabConfig(systemClassLoader=true)
	])
 
import org.semanticweb.owlapi.io.* 
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.AddImport
import groovyx.net.http.HTTPBuilder
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
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
def done = []
new File('hermit_done.txt').eachLine { line ->
  done << line
}
new File('mods').eachFile() {
  it = it.toString()
  def ontologyIRI = "file:///home/aberowl/efotest/" + it
  def id = it.tokenize('.')[0].tokenize('/')[1] // lol

  if(done.contains(id)) {
    return
  }
  // Load input ontology

  println "[UNMIREOT] Loading ontology from " + ontologyIRI

  try {
    OWLOntology ontology
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();

    ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ontologyIRI)));

    println "[UNMIREOT][HermiT] Reasoning with HermiT"
    OWLReasonerConfiguration rConf = new SimpleConfiguration(100000);
    OWLReasoner oReasoner = new Reasoner.ReasonerFactory().createReasoner(ontology, rConf);
    oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)

    println "[UNMIREOT]["+id+"][HermiT] Unsatisfiable classes: " + oReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size()
  } catch(e) {
    println "[UNMIREOT]["+id+"] Unable to load ontology. Reason: " + e.getMessage().tokenize('\n')[0]
  }
}
