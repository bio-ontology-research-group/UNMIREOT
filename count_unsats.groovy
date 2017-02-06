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

def ontology
def manager = OWLManager.createOWLOntologyManager()
def config = new OWLOntologyLoaderConfiguration()
config.setFollowRedirects(true)
//config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)

try {
  def oFile = new File(args[0])
  ontology = manager
      .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(oFile.toURI())), config);
} catch(e) {
  println "Failed to load ontology"
  e.printStackTrace()
}

if(!ontology) {
  return
}

def eConf = ReasonerConfiguration.getConfiguration()
eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")
def reasonerFactory = new ElkReasonerFactory();
def rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);

def reasoner = reasonerFactory.createReasoner(ontology, rConf);
reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

println "Reasoned. Unsats: " + reasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size()
