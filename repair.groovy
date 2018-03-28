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

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator

@Field def eConf = ReasonerConfiguration.getConfiguration()
eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "90")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
//eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")
@Field def reasonerFactory = new ElkReasonerFactory();
@Field def rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);

@Field def oFile = new File(args[0])
def oUnsatFile = args[1]
def unsatClasses = new JsonSlurper().parseText(new File(oUnsatFile).text).unsatisfiable

// First we will count the naughtiest axioms
def unsats = true
def removedAxioms = []
def runCount = 0

println "Initial unsatisfiable classes: ${unsatClasses.size()}"

while(unsats) {
  runCount++
  def naughties = findNaughties(unsatClasses)
  def naughtiest = naughties.max { it.value }
  def naughtiestAxiom = naughtiest.key
  def naughtiestCount = naughtiest.value

  println "ROUND ${runCount}"
  println "Removing naughtiest axiom: ${naughtiestAxiom} with ${naughtiestCount} implications"

  removeAxiom(naughtiestAxiom)
  removedAxioms << naughtiestAxiom
  naughties.remove(naughtiestAxiom)

  def unsatsRemaining = getUnsatisfiableClasses()
  unsatClasses.collect { cName, axioms ->
    if(!unsatsRemaining.contains(cName)) {
      return cName
    }
  }.each {
    if(it) {
      unsatClasses.remove(it)
    }
  }

  println "Unsatisfiable classes remaining: ${unsatClasses.size()}"

  unsats = unsatsClasses.size() > 0
}

def findNaughties(unsatClasses) {
  def naughtyCounts = [:]
  
  unsatClasses.each { cName, axioms ->
    axioms.each { ax ->
      if(!naughtyCounts.containsKey(ax)) {
        naughtyCounts[ax] = 0
      }
      naughtyCounts[ax]++
    }
  }
  
  return naughtyCounts
}

def removeAxiom(toRemove) {
  def manager = OWLManager.createOWLOntologyManager()
  def config = new OWLOntologyLoaderConfiguration()
  config.setFollowRedirects(true)

  println "Loading ${oFile}..."

  try {
    ontology = manager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(oFile.toURI())), config);
  } catch(e) {
    println "Failed to load ontology"
    e.printStackTrace()
    return;
  }

  // First remove from the ontology itself.
  ontology.getAxioms().each {
    if(toRemove == it.toString()) {
      manager.removeAxiom(ontology, it)

      println "Removing ${it.toString()} from main ontology"
    }
  }

  // Now we will remove the same from the imported ontologies
  ontology.getImportsDeclarations().eachWithIndex { imp, i ->
    def it = manager.getImportedOntology(imp)

    def hadToRemove = false
    it.getAxioms().each { axiom ->
      if(toRemove == axiom.toString()) {
        manager.removeAxiom(it, axiom)
        hadToRemove = true

        println "Removing ${axiom.toString()} from import"
      }
    }

    // If we had to remove an axiom from an import, then we have to create a new one
    if(hadToRemove) {
      def depFile = IRI.create(new File("dependency_${i}.owl").toURI())
      manager.saveOntology(it, depFile)

      def removeImport = new RemoveImport(ontology, imp)
      manager.applyChange(removeImport)

      def newImp = manager.getOWLDataFactory().getOWLImportsDeclaration(depFile)
      manager.applyChange(new AddImport(ontology, newImp))
    }
  }

  try {
    manager.saveOntology(ontology, IRI.create(oFile.toURI()));
    println "Saved ${oFile}"
  } catch(e) {
    println "Ontology upset, unable to save???"
  }
}

def getUnsatisfiableClasses() {
  println "Loading again to check for remaining consistencies"
  def manager = OWLManager.createOWLOntologyManager()
  def config = new OWLOntologyLoaderConfiguration()
  config.setFollowRedirects(true)

  try {
    ontology = manager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(oFile.toURI())), config);
  } catch(e) {
    println "Failed to load ontology"
    e.printStackTrace()
    return;
  }

  println "Reasoning..."
  def oReasoner
  try {
    oReasoner = reasonerFactory.createReasoner(ontology, rConf) 
    oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)
  } catch(e) {
    results.error = 'Reasoning: ' + e.getClass().getSimpleName()
    println 'Problem reasoning'
  }

  ontology.getClassesInSignature(true).collect { cl ->
    if(!oReasoner.isSatisfiable(cl)) {
      return cl.getIRI().toString() 
    }
  }
}
