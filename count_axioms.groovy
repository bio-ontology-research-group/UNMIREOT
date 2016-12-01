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
import java.io.PrintWriter
import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;
import groovy.transform.TimedInterrupt
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import groovy.json.*

class CountAxioms {
  def aCounts = new JsonSlurper().parseText(new File("acount.json").text)
  def reasonerFactory = new ElkReasonerFactory();

  def CountAxioms() {
    def currentDir = new File(".").getAbsolutePath()
    currentDir = currentDir.subSequence(0, currentDir.length() - 1)
    def eConf = ReasonerConfiguration.getConfiguration()
    eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
    eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
    eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")
    def rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);

    def mireots = new JsonSlurper().parseText(new File("mireot_ontologies.json").text)
    mireots.each { id, refs ->
      def res = new JsonSlurper().parseText(new File("results/"+id+".json").text)

      res.each { comb, results ->
        if(!(results instanceof String) && results['count'] > 0) { // there are some unsats
          def manager = OWLManager.createOWLOntologyManager()
          def config = new OWLOntologyLoaderConfiguration();
          config.setFollowRedirects(true);
          config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

          def combinationPath = "temp/unmireot_test_" + id + "_and_" + comb + ".ontology"

          try {
            if((aCounts[id + '_' + comb] && aCounts[id + '_' + comb].size() > 0) || aCounts.dead_combos.contains(id + '_' + comb)) {
              println "skipping " + id + " and " + comb
              return
            }

            def ontology = manager
              .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("file://"+currentDir+combinationPath)), config);

            def oReasoner = reasonerFactory.createReasoner(ontology, rConf);
            oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

            println "[UNMIREOT] Reasoned " + id + " and " + comb + ", resulting in " + oReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size() + " unsatisfiable classes:"
            aCounts[id + '_' + comb] = [:]
            findExplanations(ontology, oReasoner, id, comb)
          } catch(org.semanticweb.owlapi.io.UnparsableOntologyException e) {
            println "[UNMIREOT] Unable to parse ontology with " + comb
          } catch(org.semanticweb.owlapi.model.UnloadableImportException e) {
            println "[UNMIREOT] Unable to load ontology (unloadable import) with " + comb
            e.printStackTrace()
          } catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException e) {
            println "[UNMIREOT] Unable to reason ontology with " + comb
          } catch(e) {
            println "skipping " + id + '_' + comb
            aCounts.dead_combos << id + '_' + comb
            new File('acount.json').text = new JsonBuilder(aCounts).toPrettyString()
            println e.getClass().getSimpleName()
          }
        }
      }
    }
  }

  def findExplanations(ontology, oReasoner, id, comb) {
    for(OWLClass cl : ontology.getClassesInSignature(true)) {
      if(!oReasoner.isSatisfiable(cl)) {
        def iri = cl.getIRI().toString() 
        if(aCounts[id + '_' + comb].containsKey(iri)) {
          println "skipping iri " + iri
          continue;
        }
        aCounts[id + '_' + comb][iri] = []
        println 'procing ' + iri

        new File('acount.json').text = new JsonBuilder(aCounts).toPrettyString()

        BlackBoxExplanation exp = new BlackBoxExplanation(ontology, reasonerFactory, oReasoner)
        HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp)

        try {
          def t = new Thread(new getExplanations(['multExplanator':multExplanator,'cl':cl,'comb':comb,'id':id,'iri':iri]))
          def startTime = System.currentTimeMillis()
          def endTime = startTime + (60000L*2)
          t.start()

          while(System.currentTimeMillis() < endTime) {
              try {
                Thread.sleep(500L);  // Sleep 1/2 second
              } catch (InterruptedException e) {
                println "already done?"
              }
          }

          t.interrupt()
          t.join()

          aCounts = new JsonSlurper().parseText(new File("acount.json").text) // Load any new changes....
        } catch(java.util.concurrent.TimeoutException e) {
          println e.getClass().getSimpleName()
          new File('acount.json').text = new JsonBuilder(aCounts).toPrettyString()
          e.printStackTrace()
        }
      }
    }
  }
}

class getExplanations implements Runnable {
  def multExplanator
  def cl
  def iri
  def id
  def comb

  @Override
  void run() {
    Set<Set<OWLAxiom>> explanations=multExplanator.getExplanations(cl);
    def aCounts = new JsonSlurper().parseText(new File("acount.json").text)
    for(Set<OWLAxiom> explanation : explanations) {
      for (OWLAxiom causingAxiom : explanation) {
        aCounts[id + '_' + comb][iri] << causingAxiom.toString()
      }
    }
    new File('acount.json').text = new JsonBuilder(aCounts).toPrettyString()
  }
}

new CountAxioms()
