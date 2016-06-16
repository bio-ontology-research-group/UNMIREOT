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

new HTTPBuilder('http://aber-owl.net/').get(path: 'service/api/getStatuses.groovy') { resp, ontologies ->
	def nboabo = []

    ontologies.each { name, status ->
      def manager = OWLManager.createOWLOntologyManager();
      def config = new OWLOntologyLoaderConfiguration();
      config.setFollowRedirects(false);
      config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

      def ontology
      try {
        println "[BOFIND] Loading " + name
        ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("http://aber-owl.net/ontology/"+name+"/download")), config);
      } catch(e) {
        println "[BOFIND] Unable to load " + name
        e.printStackTrace()
      }

      if(ontology) {
        ontology.getClassesInSignature(false).each {
          if(it.getIRI().toString().contains('ABO_')) {
            nboabo << name
            println '[BOFIND] ' + name + ' uses ABO'
          } else if (it.getIRI().toString().contains('NBO_')) {
            println '[BOFIND] ' + name + ' uses NBO'
            nboabo << name
          }
        }
      } 
    }
}
