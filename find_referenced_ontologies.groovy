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

@Field def currentDir = new File('.').getAbsolutePath()
currentDir.subSequence(0, currentDir.length() - 1)

def pats = new JsonSlurper().parseText(new File("iri_patterns.json").text)
def results = new JsonSlurper().parseText(new File("mireot_ontologies.json").text)

new File('temp/').eachFile { f ->
  new File('mireot_ontologies.json').text = new JsonBuilder(results).toPrettyString()
  def id = f.getName().split('_').last().replace('.ontology','')

  if(results.containsKey(id)) {
    println 'skip ' + id
    return;
  }

  def manager = OWLManager.createOWLOntologyManager()
  def config = new OWLOntologyLoaderConfiguration()
  config.setFollowRedirects(true)
  //config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
  
  def ontology 
  try {
    ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(f.toURI())), config) 
  } catch(e) {
    results[id] = 'Loading Error: ' + e.getClass().getSimpleName()
    e.printStackTrace()
    println '[UNMIREOT] Problem loading ' + id
  }

  if(ontology && ontology.getImports().size() == 0) {
    results[id] = []
    ontology.getClassesInSignature(true).each {
      pats.each { n, p ->
        def os = it.getIRI().toString()
        if(n != id && !results[id].contains(n) && (os.indexOf(p) != -1 || os.indexOf('/'+n+'_') != -1 || os.indexOf(';'+n+'_') != -1)) {
          results[id] << n
        }
      }
    }


    if(results[id].size() > 0) {
      println id + ': ' + results[id].join(', ')

      results[id].each {
        def importDeclaration = manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("file:/home/aberowl/UNMIREOT/temp/unmireot_test_"+it+".ontology"));
        manager.applyChange(new AddImport(ontology, importDeclaration));

        try {
          File fileFormated = new File("tempall/"+id+"_all.ontology");
          manager.saveOntology(ontology, IRI.create(fileFormated.toURI()));
        } catch(e) {
          println 'problems'
        }
      }
    }

  }
}

new File('mireot_ontologies.json').text = new JsonBuilder(results).toPrettyString()
