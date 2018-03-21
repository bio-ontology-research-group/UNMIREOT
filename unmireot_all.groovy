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
import org.semanticweb.owlapi.util.*
import org.semanticweb.owlapi.apibinding.*
import org.semanticweb.owlapi.reasoner.*

import org.semanticweb.elk.owlapi.*
import org.semanticweb.elk.reasoner.config.*

import java.util.concurrent.*
import groovy.json.*
import groovyx.gpars.*
import groovy.transform.Field

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;

@Field def currentDir = new File('.').getAbsolutePath()
currentDir.subSequence(0, currentDir.length() - 1)

// Reasoner configuration
@Field def eConf = ReasonerConfiguration.getConfiguration()
eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "24")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")
@Field def reasonerFactory = new ElkReasonerFactory();
@Field def rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);

def mireots = new JsonSlurper().parseText(new File("mireot_ontologies.json").text)

def i = 1
mireots.each { id, refs ->
  println '[UNMIREOT][' + i + '/' + mireots.size() + '] Processing ' + id 
  if(refs instanceof String) {
    println 'skip because noload'
    return
  }
  def oFile = new File('results/' + id + '.json')
  if(!oFile.exists()) {
    println '[UNMIREOT] Running'
    runUNMIREOT(id)
  } else {
    def v = new JsonSlurper().parseText(oFile.text)
    if(v.error != false) {
      println '[UNMIREOT] Running'
      runUNMIREOT(id)
    } else {

      println 'skip'
    }
  }
  i++
}

def runUNMIREOT(id) {
  def results = [ 'error': false, 'unsatisfiable': [:] ]
  def manager = OWLManager.createOWLOntologyManager()
  def config = new OWLOntologyLoaderConfiguration()
  config.setFollowRedirects(true)
  config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
  
  def ontology 
  def ooFile = new File('ontologies_merged/' + id + '_all.ontology')
  try {
    ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ooFile.toURI())), config) 
  } catch(e) {
    results.error = 'Loading Error: ' + e.getClass().getSimpleName()
    e.printStackTrace()
    println '[UNMIREOT] Problem loading ' + id
  }

  if(ontology) {
   def oReasoner
    try {
      oReasoner = reasonerFactory.createReasoner(ontology, rConf) 
      oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)
    } catch(e) {
      results.error = 'Reasoning: ' + e.getClass().getSimpleName()
      println '[UNMIREOT] Problem reasoning ' + id
    }

    if(!results.error) {
      ontology.getClassesInSignature(true).eachParallel { cl ->
        if(!oReasoner.isSatisfiable(cl)) {
          def iri = cl.getIRI().toString() 
          results.unsatisfiable[iri] = []
          println 'procing ' + iri

          BlackBoxExplanation exp = new BlackBoxExplanation(ontology, reasonerFactory, oReasoner)

          Set<OWLAxiom> explanations = exp.getExplanation(cl)
          for(OWLAxiom causingAxiom : explanations) {
            results.unsatisfiable[iri] << causingAxiom.toString()
          }
        }
      }
    }
  } else {
    println '[UNMIREOT] Problem loading ' + id
  }

  def oFile = 'results/' + id + '.json'
  new File(oFile).text = new JsonBuilder(results).toPrettyString()
  println '[UNMIREOT] Saved ' + id
}
