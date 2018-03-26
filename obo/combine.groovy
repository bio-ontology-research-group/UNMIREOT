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

import java.util.concurrent.*
import groovy.json.*
import groovyx.gpars.*
import groovy.transform.Field

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;

@Field def currentDir = new File('.').getAbsolutePath()
currentDir.subSequence(0, currentDir.length() - 1)

// Loader & Reasoner configuration
def oList = new File(args[0]).text.split('\n')

def manager = OWLManager.createOWLOntologyManager()
def config = new OWLOntologyLoaderConfiguration()
config.setFollowRedirects(true)

// So, we will take the first ontology, and then add imports for all the others to this.

def PREFIX = "http://purl.obolibrary.org/obo/"
def masterOntology = manager.loadOntologyFromOntologyDocument(
      new IRIDocumentSource(IRI.create(PREFIX + oList[0] + '.owl')), config)

oList.eachWithIndex { o, i ->
  if(i == 0) {
    return;
  }

  def path = PREFIX + o + '.owl'
  def importDeclaration = manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(path))
  manager.applyChange(new AddImport(masterOntology, importDeclaration))

  println "Added ${path} import"
}

// Then just save it into combontology.owl

def fileFormated = new File("combontology.owl");
manager.saveOntology(masterOntology, IRI.create(fileFormated.toURI()));
println "Saved combontology.owl"
