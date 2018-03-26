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


def rFile = new File('results.json')
def results = [ 'error': false, 'unsatisfiable': [:] ]
if(rFile.exists()) {
  results = new JsonSlurper().parseText(rFile.text)
}

def manager = OWLManager.createOWLOntologyManager()

def config = new OWLOntologyLoaderConfiguration()
config.setFollowRedirects(true)
config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)

def ontology 
def ooFile = new File(args[0])
println "Loading ${ooFile}"
try {
  ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ooFile.toURI())), config) 
} catch(e) {
  results.error = 'Loading Error: ' + e.getClass().getSimpleName()
  e.printStackTrace()
  println '[UNMIREOT] Problem loading ontology'
}
println 'done loading'

if(ontology) {
 def oReasoner
  try {
    oReasoner = reasonerFactory.createReasoner(ontology, rConf) 
    oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)
  } catch(e) {
    results.error = 'Reasoning: ' + e.getClass().getSimpleName()
    println '[UNMIREOT] Problem reasoning'
  }

  if(!results.error) {
    def uCount = 0
    ontology.getClassesInSignature(true).each { cl ->
      if(!oReasoner.isSatisfiable(cl)) {
        uCount++
      }
    }

    println "Total unsatisfiable: " + uCount
    def uDone = 0

    ontology.getClassesInSignature(true).each { cl ->
      if(!oReasoner.isSatisfiable(cl)) {
        def iri = cl.getIRI().toString() 
        if(results.unsatisfiable.containsKey(iri)) {
          println "Skipping already done: ${iri} (${uDone+1}/${uCount})"
          uCount++
          return;
        }
        results.unsatisfiable[iri] = []

        println "Processing ${iri} (${uDone+1}/${uCount})"

        BlackBoxExplanation exp = new BlackBoxExplanation(ontology, reasonerFactory, oReasoner)

        Set<OWLAxiom> explanations = exp.getExplanation(cl)
        for(OWLAxiom causingAxiom : explanations) {
          results.unsatisfiable[iri] << causingAxiom.toString()
        }

        new File('results.json').text = new JsonBuilder(results).toPrettyString()
        uDone++
      }
    }
  }
} else {
  println '[UNMIREOT] Problem loading ' + id
}

new File('results.json').text = new JsonBuilder(results).toPrettyString()
println '[UNMIREOT] Saved ' + id
