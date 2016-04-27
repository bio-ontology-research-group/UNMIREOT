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
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration
import org.semanticweb.elk.reasoner.config.*

def ontologyIRI = args[0]

// Load input ontology

println "[UNMIREOT] Loading ontology from " + ontologyIRI

  OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
  config.setFollowRedirects(true);
  config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

  def ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ontologyIRI)));

    println "[UNMIREOT][ELK] Total classes: " + ontology.getClassesInSignature(true).size()
    println "[UNMIREOT][ELK] Total axioms: " + ontology.getAxiomCount()

  println "[UNMIREOT] Reasoning with ELK"

    ReasonerConfiguration eConf = ReasonerConfiguration.getConfiguration()
    eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
    eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
    eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")

    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

    OWLReasonerConfiguration erConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);
    OWLReasoner eoReasoner = reasonerFactory.createReasoner(ontology, erConf);
    eoReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
    def unsatisfiable = eoReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom()
    println "[UNMIREOT][ELK] Unsatisfiable classes: " + unsatisfiable.size()

    unsatisfiable.each {
      println it.getIRI()
      for(OWLAnnotation a : EntitySearcher.getAnnotations(it, ontology, manager.getOWLDataFactory().getRDFSLabel())) {
        def v = a.getLiteral()
        if(v instanceof OWLLiteral) {
          println "  Label: " + v.getLiteral()
        }
      }
    }
