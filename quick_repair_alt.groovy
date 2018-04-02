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

// todo remove the things from the import statements in the imports that we
// already have in the main one. because they're all importing bfo again 

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
eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "24")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
@Field def reasonerFactory = new ElkReasonerFactory();
@Field def rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);

@Field def oFile = new File(args[0])
@Field def outFile = new File(args[1])
@Field def oReasoner

@Field def manager = OWLManager.createOWLOntologyManager()
@Field def df = OWLManager.getOWLDataFactory()
@Field def config = new OWLOntologyLoaderConfiguration()
config.setFollowRedirects(true)

// First we will count the naughtiest axioms
def unsats = true
def removedAxioms = []
def runCount = 0

/** 
 * 1. Get all unsatisfiable classes
 * 2. Remove all leaf-node unsatisfiable classes
 * 3. Remove from this set all unsatisfiable classes which have a superclass in
 *      the set
 * 4. Find and then remove most implicated axiom in unsatisfiability justifications
 *      for these
 * 5. If unsatisfiable classes remain, return to 1, otherwise save and exit
 *      because you've just fixed the ontology boiiiiiiiiiiiiiiiiiiiiiii
 */
while(unsats) {
  runCount++
  println "ROUND ${runCount}"

  manager.clearOntologies()

  def toLoad = oFile
  if(runCount > 1) {
    toLoad = outFile
  }

  def unsatClasses = getUnsatisfiableClasses(toLoad)
  println "Unsatisfiable classes: ${unsatClasses.size()}"

  def topUnsats = getTopUnsatisfiableClasses(unsatClasses)
  def naughties = findNaughties(topUnsats)
  def naughtiest = naughties.max { it.value }
  def naughtiestAxiom = naughtiest.key
  def naughtiestCount = naughtiest.value

  println "Removing naughtiest axiom: ${naughtiestAxiom} with ${naughtiestCount} implications"

  removeAxiom(naughtiestAxiom)
  removedAxioms << naughtiestAxiom
  naughties.remove(naughtiestAxiom)

  // TODO: i think that the incremental reasoner mode may update the unsat
  //  counts without having to reload. will have to test that

  manager.clearOntologies()
  def unsatsRemaining = getUnsatisfiableClasses(outFile)
  println "Unsatisfiable classes remaining: ${unsatsRemaining.size()}"

  unsats = unsatsRemaining.size() > 0
}

// Find axiom explanations for unsatisfiable classes
def findNaughties(unsatClasses) {
  def allExplanations = [:]

  unsatClasses.eachWithIndex { dClass, idx ->
    def iri = dClass.getIRI()
    allExplanations[iri] = []

    println "Processing ${iri} (${idx+1}/${unsatClasses.size()})"

    def exp = new BlackBoxExplanation(ontology, reasonerFactory, oReasoner)
    def fexp = new HSTExplanationGenerator(exp);

    def explanations = fexp.getExplanation(dClass)
    for(OWLAxiom causingAxiom : explanations) {
      allExplanations[iri] << causingAxiom.toString()
    }
  }

  def naughtyCounts = [:]
  
  allExplanations.each { cName, axioms ->
    axioms.each { ax ->
      if(!naughtyCounts.containsKey(ax)) {
        naughtyCounts[ax] = 0
      }
      naughtyCounts[ax]++
    }
  }
  
  return naughtyCounts
}

// remove given axiom from ontology
def removeAxiom(toRemove) {
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

        println "Removing ${axiom.toString()} from import ${it.getOntologyID().getOntologyIRI()}"
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
    manager.saveOntology(ontology, IRI.create(outFile.toURI()));
    println "Saved ${oFile}"
  } catch(e) {
    println "Ontology upset, unable to save???"
  }
}

// load the ontology and get an unsat count
def getUnsatisfiableClasses(toLoad) {
  println "Loading ontology... ${toLoad.toURI()}" // TODO this should probably be in its own funxion

  try {
    ontology = manager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(toLoad.toURI())), config);
  } catch(e) {
    println "Failed to load ontology"
    e.printStackTrace()
    return;
  }

  println "Reasoning..."
  try {
    oReasoner = reasonerFactory.createReasoner(ontology, rConf) 
    oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)
  } catch(e) {
    results.error = 'Reasoning: ' + e.getClass().getSimpleName()
    println 'Problem reasoning'
  }

  println "Getting unsats..."
  def unsatisfiableClasses = ontology.getClassesInSignature(true).collect { cl ->
    if(!oReasoner.isSatisfiable(cl)) {
      return cl
    }
  }
  unsatisfiableClasses.removeAll([null])

  println "Found ${unsatisfiableClasses.size()}"

  return unsatisfiableClasses
}

// Get the top unsatisfiable classes
// So, we have to use the structural subclass axioms here because obviously if
//  they are unsatisfiable then we lose the proper structure if we start
//  querying with the reasoner, which is a pain in the arse.
def getTopUnsatisfiableClasses(unsatisfiableClasses) {
  println "Getting top unsats..."

  def hCount = 0
  def highest
  def allOnts = [ontology] + ontology.getImports()
  unsatisfiableClasses.each { dClass ->
    if(dClass.isBottomEntity()) {
      return;
    }

    allOnts.collect { o ->
      o.getSubClassAxiomsForSuperClass(dClass).size()
    }.each { scCount ->
      if(scCount > 0) { // collect all non-leaf node unsat classes
        if(scCount > hCount) {
          highest = dClass
          hCount = scCount
        }
      }
    }
  }

  println "Found highest class ${highest} with sc-count ${hCount}"

  
  def set = []
  allOnts.each { o -> 
    o.getSubClassAxiomsForSuperClass(highest).each { a ->
      a = a.getSubClass()
      if(a.isOWLClass()) {
        set << a
      }
    }
  }

  println "Obtained ${set.size()} direct subclasses of our highest class."

  if(set.size() > 50) {
    println "Looking at the first 25..."
    set = set.subList(0, 25)
  }

  // TODO if this set is empty, simply return all of the unsats! (since there may be the case that all the unsats are leaf nodes). thinking about it, we will probably want to look for ontology friend groups in this case too

  return set
}