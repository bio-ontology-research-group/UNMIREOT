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
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.AddImport
import groovyx.net.http.HTTPBuilder
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration
import org.semanticweb.elk.reasoner.config.*
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.owllink.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.search.*;
import org.semanticweb.owlapi.manchestersyntax.renderer.*;
import org.semanticweb.owlapi.reasoner.structural.*
import groovy.json.*

def oClasses = [:]
def noImportOntologies = []
def c = 0;

new HTTPBuilder('http://localhost/rtestserv/').get(path: 'service/api/getStatuses.groovy') { resp, ontologies ->
	def possibleMireotOntologies = []

    ontologies.remove('GAZ')
    def r = 0;
    ontologies.each { name, status ->
    if(r>20){
    return}
    r++

      def manager = OWLManager.createOWLOntologyManager();
      def config = new OWLOntologyLoaderConfiguration();
      config.setFollowRedirects(false);
      config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

      println "[MIREOTFIND] Loading " + name
      def ontology
      try {
        ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("http://localhost/rtestserv/ontology/"+name+"/download")), config);
      } catch(e) {
        println "[MIREOTFIND] Unable to load " + name
      }

      if(ontology) {
        if(ontology && ontology.getImports().size() == 0) {
          noImportOntologies << name
          println "[MIREOTFIND] " + name + " has no imports"
        }
        oClasses[name] = []
        ontology.getClassesInSignature(false).each {
          oClasses[name] << it.getIRI().toString()
        }
        println "[MIREOTFIND] Found " + oClasses[name].size() + " classes in " + name
      } 
    }

    println "[MIREOTFIND] New ontology count: " + ontologies.size()

    println "[MIREOTFIND] Ontologies with no imports: " + noImportOntologies.size()
    println "[MIREOTFIND] Comparing classes"

    def patterns = new JsonSlurper().parseText(new File("ontology_iri_patterns.json").text)
    def uses = [:]

    //oClasses is a map of ontologies, the value of which is a list of their iris

    oClasses.each { name, classIRIs ->
      uses[name] = []
      refCount = 0

      classIRIs.each { iri ->
        patterns.each { oName, classIRIPattern ->
          
          if(iri.contains(classIRIPattern) && name != oName) {
              uses[name] << oName
              refCount++
        }

        uses[name] = uses[name].unique { a, b -> a <=> b }

        
      }
        
    }

    
if(refCount > 0) {
      println "[MIREOTFIND] " + name + " has no imports and references classes " + refCount + " times from: " + uses[name]
    } else {
      println "[MIREOTFIND] " + name + " has no imports but defines all its own classes"
    }
    }
  }

