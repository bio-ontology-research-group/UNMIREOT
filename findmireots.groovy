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

def onts = []
new File('oo.txt').eachLine { line ->
  onts << line
}

def oClasses = [:]
def noImportOntologies = []
def c = 0;

new HTTPBuilder('http://aber-owl.net/').get(path: 'service/api/getStatuses.groovy') { resp, ontologies ->
	def possibleMireotOntologies = []

    ontologies.remove('GAZ')
    ontologies.each { name, status ->
      if(status.status == 'classified') {
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
          e.printStackTrace()
        }

        if(ontology) {
          if(ontology.getImports().size() == 0) {
            noImportOntologies << name
            println "[MIREOTFIND] " + name + " has no imports"
          }
          oClasses[name] = []
          ontology.getClassesInSignature(false).each {
            oClasses[name] << it.getIRI()
          }

          println "[MIREOTFIND] Found " + oClasses[name].size() + " classes in " + name
        } 
      }
    }

    println "[MIREOTFIND] Ontology count: " + ontologies.size()
    println "[MIREOTFIND] Removing ontologies which are too large"
    ontologies = ontologies.findAll { name, count -> 
      if(oClasses[name]) {
        return oClasses[name].size() < 100000 
      } else {
        return false 
      }
    }
    println "[MIREOTFIND] New ontology count: " + ontologies.size()

    println "[MIREOTFIND] Ontologies with no imports: " + noImportOntologies.size()
    println "[MIREOTFIND] Comparing classes"

    ontologies.each { name, status ->
      def uses = []
      def refCount = 0

      if(status.status == 'classified' && noImportOntologies.contains(name)) {
        println "[MIREOTFIND] Comparing classes for " + name
        if(oClasses[name]) {
          oClasses.each { oName, iris ->
            if(oName != name) {
              iris.each {
                if(oClasses[name].contains(it)) {
                  refCount++
                  if(!uses.contains(oName)) {
                    uses << oName
                  }
                }
              }
            }
          }
        }

        if(refCount > 0) {
          println "[MIREOTFIND] " + name + " has no imports and references classes " + refCount + " times from the following ontologies: " + uses
        }
      }
    }
}
