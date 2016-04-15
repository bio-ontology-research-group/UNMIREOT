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

new HTTPBuilder('http://aber-owl.net/').get(path: 'service/api/getStatuses.groovy') { resp, ontologies ->
	def possibleMireotOntologies = []

    ontologies.each { name, status ->
      if(status.status == 'classified') {
        def manager = OWLManager.createOWLOntologyManager();
        def config = new OWLOntologyLoaderConfiguration();
        config.setFollowRedirects(true);
        config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

        def ontology
        try {
          ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("http://localhost/rtestserv/ontology/"+name+"/download")), config);
        } catch(e) {
          println "Unable to obtain " + name
        }

        if(ontology && ontology.getImports().size() == 0) {

          def refCount = 0
          ontology.getClassesInSignature().each {
            onts.each { oName ->
              def m = it =~ oName
              if(m && !(m[0] in ontologies) && (m[0] != name)) {
                refCount++;
              }
            }
          }

          if(refCount > 0) {
            println name + " uses class references from " + refCount + " ontologies, but has no imports"
          }
        } 
      }
    }
}
