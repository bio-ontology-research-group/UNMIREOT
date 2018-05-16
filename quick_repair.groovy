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

// TODO : write the test to find the other problematic axioms and counts etc.

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

// Set up all the reasoner and whatnot

@Field def eConf = ReasonerConfiguration.getConfiguration()
eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "96")
eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
@Field def reasonerFactory = new ElkReasonerFactory()
@Field def rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf)

@Field def oFile = new File(args[0])
@Field def outName = args[1]
new File(args[1]).mkdir()
@Field outFile = new File(args[1]+'/fixed.owl')
@Field resFile = new File(args[1]+'/results.json')

@Field def oReasoner
@Field def SAMPLE_SIZE = 5

@Field def manager = OWLManager.createOWLOntologyManager()
@Field def df = OWLManager.getOWLDataFactory()
@Field def config = new OWLOntologyLoaderConfiguration()
@Field def noExplanationClasses = []
config.setFollowRedirects(true)

// First we will count the naughtiest axioms
def unsats = true
def removedAxioms = [:]
def lastRemovedAxiom
def runCount = 0
def unsatClasses

  // TODO: i think that the incremental reasoner mode may update the unsat
  //  counts without having to reload. will have to test that
/** 
 * 1. Load & classify ontology. Retrieve list of unsatisfiable classes. If there are none, exit.
 * 2. If there are non-leaf-node unsatisfiable classes, remove all leaf-node
 *      unsatisfiable classes, else goto 5 with the complete set of
 *      unsatisfiable classes.
 * 3. Remove from the set of non-leaf-node unsatisfiable classes all classes
 *      which have a superclass in the set.
 * 4. Group remaining classes by their number of direct asserted subclasses.
 * 5. Select the group with the most classes, and if there are more than 25,
 *      randomly sample 25 of these, else proceed with all of them.
 * 6. Get explanations, then find the most implicated axiom, and remove it
 * 7. Return to 1
 */
while(unsats) {
  runCount++
  println "ROUND ${runCount}"

  def toLoad = oFile
  if(runCount > 1) {
    toLoad = outFile
  }

  def newUnsatClasses = getUnsatisfiableClasses(toLoad)

  if(lastRemovedAxiom) { // Count how effective the last removal was
    if(!removedAxioms.containsKey(lastRemovedAxiom)) {
      removedAxioms[lastRemovedAxiom] = []
    }

    removedAxioms[lastRemovedAxiom] += unsatClasses.findAll {
      !newUnsatClasses.contains(it)
    }.collect {
      it.getIRI()
    }

    resFile.text = new JsonBuilder(removedAxioms).toPrettyString()
  }
  unsatClasses = newUnsatClasses

  println "Unsatisfiable classes: ${unsatClasses.size()}"
  if(unsatClasses.size() == 0) {
    println "Done."
    break;
  }

  def topUnsats = getTopUnsatisfiableClasses(unsatClasses)
  def naughties = findNaughties(topUnsats)
  def naughtiest = naughties.max { it.value }

  if(naughtiest) {
    def naughtiestAxiom = naughtiest.key
    def naughtiestCount = naughtiest.value

    println "Removing naughtiest axiom: ${naughtiestAxiom} with ${naughtiestCount} implications"

    removeAxiom(naughtiestAxiom)
    naughties.remove(naughtiestAxiom)
    lastRemovedAxiom = naughtiestAxiom.toString()
  } else {
    println "No justifications found in current round. Removing ${noExplanationClasses.size()} unjustifiable unsatisfiable classes."
    println noExplanationClasses
    removeAxiom(noExplanationClasses)
    lastRemovedAxiom = "Unjustifiable Unsatisfiable" 
  }

  manager.clearOntologies()
}

// Find axiom explanations for unsatisfiable classes
def findNaughties(unsatClasses) {
  def allExplanations = [:]

  unsatClasses.eachWithIndex { dClass, idx ->
    def iri = dClass.getIRI()
    allExplanations[iri] = []

    println "Processing ${iri} (${idx+1}/${unsatClasses.size()})"

    def exp = new BlackBoxExplanation(ontology, reasonerFactory, oReasoner)
    def fexp = new HSTExplanationGenerator(exp)

    def explanations = fexp.getExplanation(dClass)
    for(OWLAxiom causingAxiom : explanations) {
      allExplanations[iri] << causingAxiom.toString()
    }
  }

  def naughtyCounts = [:]
  
  allExplanations.each { cName, axioms ->
    if(axioms.size() == 0) {
      noExplanationClasses << '<'+cName+'>'
    } else {
      axioms.each { ax ->
        if(!naughtyCounts.containsKey(ax)) {
          naughtyCounts[ax] = 0
        }
        naughtyCounts[ax]++
      }
    }
  }
  
  return naughtyCounts
}

// remove given axiom from ontology
def removeAxiom(toRemove) {
  if(!(toRemove instanceof Collection)) {
    toRemove = [ toRemove ]
  }

  ontology.getAxioms().each {
    if(toRemove.contains(it.toString()) || it.getClassesInSignature().any { c -> toRemove.contains(c.getIRI().toString()) }) {
      manager.removeAxiom(ontology, it)  
      println "Removing ${it.toString()} from main ontology"
    }
  }

  // Now we will remove the same from the imported ontologies
  ontology.getImportsDeclarations().eachWithIndex { imp, i ->
    def it = manager.getImportedOntology(imp)

    def hadToRemove = false
    it.getAxioms().each { axiom ->
      if(toRemove.contains(axiom.toString()) || axiom.getClassesInSignature().any { c -> toRemove.contains(c.getIRI().toString()) }) {
        manager.removeAxiom(it, axiom)
        hadToRemove = true

        println "Removing ${axiom.toString()} from import ${it.getOntologyID().getOntologyIRI()}"
      }
    }

    // If we had to remove an axiom from an import, then we have to create a new one
    if(hadToRemove) {
      def depFile = IRI.create(new File("${args[1]}/dependency_${i}.owl").toURI())
      manager.saveOntology(it, depFile)

      def removeImport = new RemoveImport(ontology, imp)
      manager.applyChange(removeImport)

      def newImp = manager.getOWLDataFactory().getOWLImportsDeclaration(depFile)
      manager.applyChange(new AddImport(ontology, newImp))
    }
  }

  try {
    manager.saveOntology(ontology, IRI.create(outFile.toURI()))
    println "Saved ${outFile}"
  } catch(e) {
    println "Ontology upset, unable to save???"
  }
}

// load the ontology and get an unsat count
def getUnsatisfiableClasses(toLoad) {
  println "Loading ontology... ${toLoad.toURI()}" // TODO this should probably be in its own funxion

  try {
    ontology = manager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(toLoad.toURI())), config)
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
    if(!cl.isBottomEntity() && !oReasoner.isSatisfiable(cl)) {
      return cl
    }
  }
  unsatisfiableClasses.removeAll([null])

  return unsatisfiableClasses
}

// Get the top unsatisfiable classes
// So, we have to use the structural subclass axioms here because obviously if
//  they are unsatisfiable then we lose the proper structure if we start
//  querying with the reasoner, which is a pain in the arse.
def getTopUnsatisfiableClasses(unsatisfiableClasses) {
  println "Getting top unsats..."

  def highest = []
  def subCount = 0
  def allDepths = [:]
  def allOnts = [ontology] + ontology.getImports()

  if(unsatisfiableClasses.size() <= SAMPLE_SIZE) {
    println "Less than ${SAMPLE_SIZE} unsatisfiable classes, looking at them all!"
    return unsatisfiableClasses
  }

  unsatisfiableClasses.each { dClass ->
    allOnts.collect { o ->
      o.getSubClassAxiomsForSuperClass(dClass).size()
    }.each { scCount ->
      if(scCount > 0) { // collect all non-leaf node unsat classes
        highest << dClass

        if(!allDepths.containsKey(scCount)) {
          allDepths[scCount] = []
        }
        allDepths[scCount] << dClass

        if(scCount > subCount) {
          subCount = scCount
        }
      }
    }
  }

  println "Found ${highest.size()} non-leaf unsats with the highest @ ${subCount} out of a total ${unsatisfiableClasses.size()} unsats"

  if(highest.size() == 0) {
    println "In this case, we will simply have to look at all ${unsatisfiableClasses.size()} of the classes!"
    //return unsatisfiableClasses
    highest = unsatisfiableClasses
  }

  def toRemove = []
  highest.each { dClass ->
    allOnts.each { o ->
      def removableSub = o.getSubClassAxiomsForSuperClass(dClass).find { highest.contains(it.getSubClass()) }
      if(removableSub && !toRemove.contains(removableSub.getSubClass())) {
        toRemove << removableSub.getSubClass()
      }
    }
  }

  println "Removing a further ${toRemove.size()} classes with superclasses in the group"

  highest.removeAll(toRemove)
  allDepths.each { k, v ->
    v.removeAll(toRemove)
  }

  println "Now looking at ${highest.size()} unsats out of a total ${unsatisfiableClasses.size()}"

  def friends = allDepths.max { it.value.size() }
  
  if(friends && friends.value.size() < highest.size()) {
    println "Found happy ontology friend group of ${friends.value.size()} classes with sc-count ${friends.key} (well I suppose strictly they can't be all that happy given their instances cannot possibly exist :(. Now, we will fix that!"
    highest = friends.value
  }

  println "Pared ${unsatisfiableClasses.size()} unsatisfiable classes down to ${highest.size()} to justify for this round"

  if(highest.size() > SAMPLE_SIZE) {
    println "There are more than ${SAMPLE_SIZE} classes, so we will take a random sample of ${SAMPLE_SIZE} from our subset."
    def random = new Random()
    highest = (1..SAMPLE_SIZE).collect {
      highest[random.nextInt(highest.size())]
    }
  }

  return highest
}
