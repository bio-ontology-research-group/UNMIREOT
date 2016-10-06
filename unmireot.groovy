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
import groovy.json.*

def mireots = new JsonSlurper().parseText(new File("mireot_ontologies.json").text)
mireots.each { id, refs ->
  runUNMIREOT(id)
}

def runUNMIREOT(ontologyID) {
  def currentDir = new File(".").getAbsolutePath()
  currentDir = currentDir.subSequence(0, currentDir.length() - 1)
  def results = [:]
  def added = []
  def mireots = new JsonSlurper().parseText(new File("mireot_ontologies.json").text)

  // Reasoner configuration
  def eConf = ReasonerConfiguration.getConfiguration()
  eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
  eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
  eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")
  def reasonerFactory = new ElkReasonerFactory();
  def rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);

  // Check it is a MIREOT ontology
  if(!mireots.containsKey(ontologyID)) {
    println "This is not a MIREOT ontology, probably - take a look at mireot_ontologies.json. If it really is a MIREOT ontology, you can add it and its references there."
    System.exit(1)
  }

  // Load input ontology
  println "[UNMIREOT] Loading ontology " + ontologyID
  def manager = OWLManager.createOWLOntologyManager();
  def config = new OWLOntologyLoaderConfiguration();
  config.setFollowRedirects(true);
  config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
  def originalOntology

  def ooFile = new File("temp/unmireot_test_"+ontologyID+".ontology")
  try {
    if(!ooFile.exists()) { // create and save a new ontology
      println "[UNMIREOT] No cache, downloading " + ontologyID
      originalOntology = manager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("http://aber-owl.net/ontology/"+ontologyID+"/download")), config);
      manager.saveOntology(originalOntology, IRI.create(ooFile.toURI()));
    } else {
      println "[UNMIREOT] Got cache, loading " + ontologyID + " from cache"
      originalOntology = manager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ooFile.toURI())), config);
    }
  } catch(e) {
    println "This ontology ID is not on AberOWL, or the cache, or AberOWL is down. To identify which, see http://aber-owl.net/ontology/"
  }

  mireots[ontologyID].each { referenceID ->
    println "[UNMIREOT] Testing ontology with " + referenceID + " import."

    def combinationPath = "temp/unmireot_test_" + ontologyID + "_and_" + referenceID + ".ontology"

    if(!new File(combinationPath).exists()) { // create and save a new ontology
      println "creating new combiantion"
      def importIRI = IRI.create("http://aber-owl.net/ontology/"+referenceID+"/download")
      OWLImportsDeclaration importDeclaration = manager.getOWLDataFactory()
        .getOWLImportsDeclaration(importIRI);
      manager.applyChange(new AddImport(originalOntology, importDeclaration));

      def fileFormated = new File("temp/unmireot_test_"+oName+"_and_"+it+".ontology");
      manager.saveOntology(ontology, IRI.create(fileFormated.toURI()));

      manager.applyChange(new RemoveImport(originalOntology, importDeclaration));
    }

    def cOntology
    def cManager = OWLManager.createOWLOntologyManager()
    println new IRIDocumentSource(IRI.create("file://"+currentDir+combinationPath))
    try {
      cOntology = cManager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("file://"+currentDir+combinationPath)), config);

      def oReasoner = reasonerFactory.createReasoner(cOntology, rConf);
      oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

      println "[UNMIREOT] Reasoned " + ontologyID + " and " + referenceID + ", resulting in " + oReasoner.getEquivalentClasses(cManager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size() + " unsatisfiable classes:"
      results[referenceID] = [ 'count': oReasoner.getEquivalentClasses(cManager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size() ]
      results[referenceID]['classes'] = []

      for(OWLClass cl : cOntology.getClassesInSignature(true)) {
        if(!oReasoner.isSatisfiable(cl)) {
          def iri = cl.getIRI().toString() 
          System.out.println("Unsatisfiable (" + results[referenceID]['classes'].size() + "): " + iri)
          results[referenceID]['classes'] << iri
        }
      }
    } catch(org.semanticweb.owlapi.io.UnparsableOntologyException e) {
      println "[UNMIREOT] Unable to parse ontology with " + referenceID
      results[referenceID] = e.getClass().getSimpleName()
    } catch(org.semanticweb.owlapi.model.UnloadableImportException e) {
      println "[UNMIREOT] Unable to load ontology (unloadable import) with " + referenceID
      results[referenceID] = e.getClass().getSimpleName()
      e.printStackTrace()
    } catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException e) {
      println "[UNMIREOT] Unable to reason ontology with " + referenceID
      results[referenceID] = e.getClass().getSimpleName()
    } catch(e) {
      results[referenceID] = e.getClass().getSimpleName()
      println e.getClass().getSimpleName() 
      e.printStackTrace()
    }
  }

  def oFile = "results/"+ontologyID+".json"
  println "[UNMIREOT] Done, saving results to " + oFile
  new File(oFile).text = new JsonBuilder(results).toPrettyString()
}
  /*
  def fixUnsatisfiability() {
    BlackBoxExplanation exp = new BlackBoxExplanation(ncOntology, reasonerFactory, oReasoner);
    HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp);

    Set<Set<OWLAxiom>> explanations=multExplanator.getExplanations(cl);
    for(Set<OWLAxiom> explanation : explanations) {
        System.out.println("------------------");
        System.out.println("Axioms causing the unsatisfiability: ");
        for (OWLAxiom causingAxiom : explanation) {
          System.out.println(causingAxiom);
        }
        System.out.println("------------------");
    }
  }
*/ 
