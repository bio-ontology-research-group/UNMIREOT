@Grapes([
  @Grab(group='com.google.code.gson', module='gson', version='2.3.1'),
  @Grab(group='org.slf4j', module='slf4j-log4j12', version='1.7.10'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.1.0'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.1.0'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.1.0'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.1.0'),
  @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.2'),
  @Grab(group='org.codehaus.gpars', module='gpars', version='1.1.0'),
  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' ),
  @Grab(group='net.sourceforge.owlapi', module='jfact', version='4.0.3'),
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
import uk.ac.manchester.cs.jfact.JFactFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration
import org.semanticweb.elk.reasoner.config.*

new File('mods').eachFile() {
  it = it.toString()
  def ontologyIRI = "file:///home/aberowl/efotest/" + it
  def id = it.tokenize('.')[0].tokenize('/')[1] // lol

  println "[UNMIREOT]["+id+"] Loading ontology from " + ontologyIRI

  OWLOntology ontology
  OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
  config.setFollowRedirects(true);
  config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

  try {
    ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ontologyIRI)));
    if(id != 'EFO_MP' && id != 'EFO_OBI' && id != 'EFO_PR' && id != 'EFO_GO' && id != 'EFO_TO') {
      println "[UNMIREOT]["+id+"][FACT] Reasoning with JFact"

      OWLReasonerConfiguration rConf = new SimpleConfiguration(100000);
      
      try {
        OWLReasoner oReasoner = new JFactFactory().createReasoner(ontology, rConf);
        oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        println "[UNMIREOT]["+id+"][FACT] Unsatisfiable classes: " + oReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size()
      } catch(e) {
        println "[UNMIREOT]["+id+"][FACT] Unable to reason with FACT. Reason: " + e.getClass().getSimpleName() + ' - ' + e.getMessage()
      }
    } else {
      println "[UNMIREOT]["+id+"][FACT] Unable to reason with FACT. Reason: Uncatchable timeout."
    }

    println "[UNMIREOT]["+id+"][ELK] Reasoning with ELK"

    ReasonerConfiguration eConf = ReasonerConfiguration.getConfiguration()
    eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
    eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
    eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")

    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

    OWLReasonerConfiguration erConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);
    OWLReasoner eoReasoner = reasonerFactory.createReasoner(ontology, erConf);
    eoReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

    println "[UNMIREOT]["+id+"][ELK] Unsatisfiable classes: " + eoReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size()
  } catch(e) {
    println "[UNMIREOT]["+id+"] Unable to load ontology. Reason: " + e.getMessage().tokenize('\n')[0]
  }
}
