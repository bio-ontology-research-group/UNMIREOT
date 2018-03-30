@Grapes([
  @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='5.1.4'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='5.1.4'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='5.1.4'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='5.1.4'),
  @Grab(group='org.apache.commons', module='commons-rdf-api', version='0.5.0'),
  @Grab(group='org.slf4j', module='slf4j-log4j12', version='1.7.10'),
  @Grab('com.xlson.groovycsv:groovycsv:1.1'),

  @GrabResolver(name='sonatype-nexus-snapshots', root='https://oss.sonatype.org/content/repositories/snapshots/'),
  @Grab(group='org.semanticweb.elk', module='elk-owlapi5', version='0.5.0-SNAPSHOT'),

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

def COMBO_PREFIX = 'http://reality.rehab/ontology/'
def COMBO_FOLDER = 'combos/'

def pFile = new File(args[0])
def pName = pFile.getName().tokenize('.')[0]
def oFolder = new File(args[1])
def unsatCounts = new JsonSlurper().parseText(new File("results.json").text)

def manager = OWLManager.createOWLOntologyManager()
def config = new OWLOntologyLoaderConfiguration()
config.setFollowRedirects(true)

def total = oFolder.list().size()
def idx = 0

/*println "Loading master ontology"
def masterOntology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(pFile), config)

oFolder.eachFile { oFile ->
  idx++
  println "Processing ${oFile.getName()} (${idx}/${total})"

  try {
    def importDeclaration = manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(oFile))
    def removeDeclaration = manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/bfo.owl"))
    def newOntologyID = new OWLOntologyID(IRI.create(COMBO_PREFIX+"${pName}_${oFile.getName()}"))

    manager.applyChange(new AddImport(masterOntology, importDeclaration))
    manager.applyChange(new SetOntologyID(masterOntology, newOntologyID))

    def fileFormatted = new File("${COMBO_FOLDER}${pName}_${oFile.getName()}")
    manager.saveOntology(masterOntology, IRI.create(fileFormatted.toURI()))

    manager.applyChange(new RemoveImport(masterOntology, importDeclaration)) 

    println "Saved ${fileFormatted.getName()}"
  } catch(e) {
    println "Error: ${e.getMessage()}"
    unsatCounts[oFile.getName()] = e.getMessage()
  }
}

manager.clearOntologies()*/

def eConf = ReasonerConfiguration.getConfiguration()
eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "24")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
def reasonerFactory = new ElkReasonerFactory();
def rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);

def cFolder = new File(COMBO_FOLDER)
idx = 0
total = cFolder.list().size()

println "Checking combos for unsats..."

new File(COMBO_FOLDER).eachFile { oFile ->
  idx++

  if(unsatCounts.containsKey(oFile.getName())) {
    println "Skipping already done ${oFile.getName()} (${idx}/${total})"
    return;
  }

  println "Classifying ${oFile.getName()} (${idx}/${total})"

  def ontology
  try {
    ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(oFile), config)
  } catch(e) {
    unsatCounts[oFile.getName()] = e.getMessage()
    println '  Problem loading: ' + e.getMessage()
  }

  if(ontology) {
    def oReasoner
    try {
      oReasoner = reasonerFactory.createReasoner(ontology, rConf) 
      oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)
    } catch(e) {
      unsatCounts[oFile.getName()] = e.getMessage()
      println '  Problem reasoning: ' + e.getMessage()
    }

    if(oReasoner && oReasoner.isConsistent()) {
      def unsats = 0

      ontology.getClassesInSignature(true).collect { cl ->
        if(!oReasoner.isSatisfiable(cl)) {
          unsats++
        }
      }
      
      println "  Found ${unsats} unsatisfiable classes"
      unsatCounts[oFile.getName()] = unsats
    }
  }

  new File('results.json').text = new JsonBuilder(unsatCounts).toPrettyString()
  manager.clearOntologies()
}
