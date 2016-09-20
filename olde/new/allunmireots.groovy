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
//import org.semanticweb.owl.explanation.api.Explanation;
//import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
//import org.semanticweb.owl.explanation.api.ExplanationManager;
import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;
import groovy.json.*

def mireots = new JsonSlurper().parseText(new File("mireot_results.json").text)
def c = 0
mireots.each { name, imports ->
  try {
    def otherResult = new JsonSlurper().parseText(new File("results/"+name+'.json').text)
    def icons = otherResult.findAll { oName, res -> res == 'Inconsistent' }

    if(icons.size() > 1) {
      println "re-running UNMIREOT on " + name + " because everything got fokt last time for some reason"
      println "Running UNMIREOT on " + name
      def results = run(name)
      println "Saving results for " + name
      new File("results/"+name+'.json').text = new JsonBuilder(results).toPrettyString()
    } else {
      println "skipping " + name + " because it's already done"
    }
  } catch(FileNotFoundException e) {
    println "Running UNMIREOT on " + name
    def results = run(name)
    println "Saving results for " + name
    new File("results/"+name+'.json').text = new JsonBuilder(results).toPrettyString()
  }
}

def run(oName) {
  def results = [:]

  // Load input ontology
  println "[UNMIREOT] Loading ontology from mirot index"

  def mireots = new JsonSlurper().parseText(new File("mireot_results.json").text)

  if(mireots[oName]) {
    println "[UNMIREOT] " + oName + " has no imports, and references ontologies from " + mireots[oName].size() + " other ontologies."
  } else {
    println "[UNMIREOT] " + oName + " doesn't seem to be a MIREOT ontology."
  }

  println "[UNMIREOT] Creating new ontology with imports"

  def added = []
  def config = new OWLOntologyLoaderConfiguration();
  config.setFollowRedirects(true);
  config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

  mireots[oName].each {
    println "[UNMIREOT] Adding " + it + " to ontology"

    def ontology
    def manager
    try {
      manager = OWLManager.createOWLOntologyManager();
      ontology = manager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("http://aber-owl.net/ontology/"+oName+"/download")), config);

      OWLImportsDeclaration importDeclaration = manager.getOWLDataFactory()
        .getOWLImportsDeclaration(IRI.create("http://aber-owl.net/ontology/"+it+"/download"));
      manager.applyChange(new AddImport(ontology, importDeclaration));

      def fileFormated = new File("temp/unmireot_test_"+oName+"_and_"+it+".ontology");
      manager.saveOntology(ontology, IRI.create(fileFormated.toURI()));

      added << it
      println "[UNMIREOT] Loaded new " + oName + " with " + it
    } catch(err) {
      println "[UNMIREOT] Unable to load " + it + " with " + oName 
      println "[UNMIREOT] Removing "  + it + " from imports"
      err.printStackTrace();

      added.remove(it)
      results[it] = err.getClass().getSimpleName() 
    }

  if(results[it] != "Unloadable") {
    // Load and reason the new ontology
    def newOntology
    def newManager
    try {
      newManager = OWLManager.createOWLOntologyManager();
      newOntology = newManager
        .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("file:///home/aberowl/UNMIREOT/new/temp/unmireot_test_"+oName+"_and_"+it+".ontology")), config);

      ReasonerConfiguration eConf = ReasonerConfiguration.getConfiguration()
      eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
      eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
      eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")

      OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

      OWLReasonerConfiguration rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);
      OWLReasoner oReasoner = reasonerFactory.createReasoner(newOntology, rConf);
      oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

      println "[UNMIREOT] Reasoned " + oName + " and " + it + ", resulting in " + oReasoner.getEquivalentClasses(newManager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size() + " unsatisfiable classes"
      results[it] = [ 'count': oReasoner.getEquivalentClasses(newManager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size() ]
      results[it]['classes'] = []
      for(OWLClass cl : newOntology.getClassesInSignature(true)) {
        if(!oReasoner.isSatisfiable(cl)) {
          def iri = cl.getIRI().toString() 
          System.out.println("Unsatisfiable: " + iri)
          results[it]['classes'] << iri

          BlackBoxExplanation exp=new BlackBoxExplanation(newOntology, reasonerFactory, oReasoner);
          HSTExplanationGenerator multExplanator=new HSTExplanationGenerator(exp);

          // Now we can get explanations for the unsatisfiability. 
          Set<Set<OWLAxiom>> explanations=multExplanator.getExplanations(cl);
          // Let us print them. Each explanation is one possible set of axioms that cause the 
          // unsatisfiability. 
          for(Set<OWLAxiom> explanation : explanations) {
              System.out.println("------------------");
              System.out.println("Axioms causing the unsatisfiability: ");
              for (OWLAxiom causingAxiom : explanation) {
                System.out.println(causingAxiom);
              }
              System.out.println("------------------");
          }
        }
      }
    } catch(org.semanticweb.owlapi.io.UnparsableOntologyException e) {
      println "[UNMIREOT] Unable to parse ontology with " + it
      println "[UNMIREOT] Removing "  + it + " from imports"
      added.remove(it)
      results[it] = e.getClass().getSimpleName() 
      e.printStackTrace()
    } catch(org.semanticweb.owlapi.model.UnloadableImportException e) {
      println "[UNMIREOT] Unable to unloadable ontology with " + it
      println "[UNMIREOT] Removing "  + it + " from imports"
      added.remove(it)
      results[it] = e.getClass().getSimpleName() 
    } catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException e) {
      println "[UNMIREOT] Unable to reason ontology with " + it
      println "[UNMIREOT] Removing "  + it + " from imports"
      added.remove(it)
      results[it] = e.getClass().getSimpleName() 
    } catch(e) {
      results[it] = e.getClass().getSimpleName() 
    }
  }
}
  println "[UNMIREOT] Finished checking all ontologies individually"

  // Modify the original ontology, adding all of the valid imports
  println "[UNMIREOT] Adding all available ontologies as imports"
  try {
  def newManager = OWLManager.createOWLOntologyManager();
  def ontology = newManager
    .loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("http://aber-owl.net/ontology/"+oName+"/download")), config);

  added.each {
    try {
    OWLImportsDeclaration importDeclaration = newManager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create("http://aber-owl.net/ontology/"+it+"/download"));
    newManager.applyChange(new AddImport(ontology, importDeclaration));
    println "[UNMIREOT] Adding " + it
    }catch(e) {}
  }

  File fileFormated = new File("temp/unmireot_test_"+oName+"_all.ontology");
  newManager.saveOntology(ontology, IRI.create(fileFormated.toURI()));

  println "[UNMIREOT] Loading new ontology with imports"

  def newestManager = OWLManager.createOWLOntologyManager();
  def newOntology = newestManager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("file:///home/aberowl/UNMIREOT/new/temp/unmireot_test_"+oName+"_all.ontology")), config);

  println "[UNMIREOT] Reasoning new ontology"
  ReasonerConfiguration eConf = ReasonerConfiguration.getConfiguration()
  eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "8")
  eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
  eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")

  OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

  OWLReasonerConfiguration rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);
  OWLReasoner oReasoner = reasonerFactory.createReasoner(newOntology, rConf);
  oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

  println "[UNMIREOT] Unsatisfiable classes: " + oReasoner.getEquivalentClasses(newManager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size()
  results['ALL'] = [ 'count': oReasoner.getEquivalentClasses(newManager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size() ]
  results['ALL']['classes'] = []
  for (OWLClass cl : newOntology.getClassesInSignature(true)) {
    if(!oReasoner.isSatisfiable(cl)) {
      System.out.println("Unsatisfiable: " + cl.getIRI())
      results['ALL']['classes'] << cl.getIRI().toString()
    }
  }
  

  } catch(e) {
    e.printStackTrace()
    results['ALL'] = e.getClass().getSimpleName() 
  }

  return results
}
