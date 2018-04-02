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
def COMBO_FOLDER = 'core/'
def PURL = "http://purl.obolibrary.org/obo/"

def pFile = new File(args[0])
def pName = pFile.getName().tokenize('.')[0]

def manager = OWLManager.createOWLOntologyManager()
def config = new OWLOntologyLoaderConfiguration()
config.setFollowRedirects(true)

try {
  def childOntology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(pFile), config)
  def newOntologyID = IRI.create(COMBO_PREFIX+"${pName}_merged")
  def fileFormatted = new File("${COMBO_FOLDER}${pName}_merged.owl")

  def iDecs = childOntology.getImportsDeclarations()
  iDecs.each { imp ->
    def it = manager.getImportedOntology(imp)
    manager.applyChange(new RemoveImport(childOntology, imp)) 
    //manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(imp.getIRI()), config)
  }

  def newOntology = new OWLOntologyMerger(manager).createMergedOntology(manager, newOntologyID)

  manager.saveOntology(newOntology, IRI.create(fileFormatted.toURI()))

  println "Saved ${fileFormatted.getName()}"
} catch(e) {
  println "Error: ${e.getMessage()}"
}
