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

import java.util.concurrent.*
import groovy.json.*
import groovyx.gpars.*
import groovy.transform.Field

@Field def axiomsToRemove = new JsonSlurper().parseText(new File('axioms_to_remove.json').text)

axiomsToRemove.each { id, a ->
  println "Processing " + id

  def newOntologyFile = new File("removed/unmireot_test_removed_"+id+".ontology")
  if(!newOntologyFile.exists()) {
    removeAxioms(id)
  } else {
    println "skipping because already exists"
  }
}

def removeAxioms(id) {
  def ontology
  def manager = OWLManager.createOWLOntologyManager()
  def config = new OWLOntologyLoaderConfiguration()
  config.setFollowRedirects(true)
  //config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)

  try {
    def oFile = new File("temp/unmireot_test_"+id.replaceFirst(/_/,"_and_")+".ontology")
    ontology = manager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(oFile.toURI())), config);
  } catch(e) {
    println "Failed to load ontology"
    e.printStackTrace()
  }

  if(!ontology) {
    return
  }

  def totalRemoved = 0

  ontology.getAxioms().each {
    if(axiomsToRemove[id].contains(it.toString())) {
      manager.removeAxiom(ontology, it)
      totalRemoved++

      println "Removing " + it.toString()
    }
  }

  ontology.getImportsDeclarations().eachWithIndex { imp, i ->
    def it = manager.getImportedOntology(imp)

    def hadToRemove = false
    it.getAxioms().each { axiom ->
      if(axiomsToRemove[id].contains(axiom.toString())) {
        manager.removeAxiom(it, axiom)
        hadToRemove = true
        totalRemoved++

        println "Removing " + axiom.toString()
      }
    }

    if(hadToRemove) { // iterator might not be best fname but it may not necessary be combo
      def depFile = IRI.create(new File("removed/dependency_"+id+"_"+i+".ontology").toURI())
      manager.saveOntology(it, depFile)

      def removeImport = new RemoveImport(ontology, imp)
      manager.applyChange(removeImport)

      def newImp = manager.getOWLDataFactory().getOWLImportsDeclaration(depFile)
      manager.applyChange(new AddImport(ontology, newImp))
    }
  }

  if(totalRemoved >= axiomsToRemove[id].size()) {
    try {
      def newOntologyFile = new File("removed/unmireot_test_removed_"+id+".ontology")
      manager.saveOntology(ontology, IRI.create(newOntologyFile.toURI()));
      println "Saved " + newOntologyFile 
    } catch(e) {
      println "Ontology upset, unable to save???"
    }
  } else {
    println "Removed axioms less than what was necessary. Some issue there. Not saving."
  }
}
