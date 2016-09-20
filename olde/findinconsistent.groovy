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

def ontologyIRI = args[0]
def consistentOntologyFound = false
def imports = ["http://aber-owl.net/ontology/MP/download",
"http://aber-owl.net/ontology/MPATH/download",
"http://aber-owl.net/ontology/ERO/download",
"http://aber-owl.net/ontology/OGMS/download",
"http://aber-owl.net/ontology/ATO/download",
"http://aber-owl.net/ontology/BT/download",
"http://aber-owl.net/ontology/MS/download",
"http://aber-owl.net/ontology/PR/download",
"http://aber-owl.net/ontology/FBbt/download",
"http://aber-owl.net/ontology/IAO/download",
"http://aber-owl.net/ontology/EO/download",
"http://aber-owl.net/ontology/FO/download",
"http://aber-owl.net/ontology/PATO/download",
"http://aber-owl.net/ontology/GO/download",
"http://aber-owl.net/ontology/SO/download",
"http://aber-owl.net/ontology/DOID/download",
"http://aber-owl.net/ontology/BTO/download",
"http://aber-owl.net/ontology/HP/download",
"http://aber-owl.net/ontology/ZEA/download",
"http://aber-owl.net/ontology/ORDO/download",
"http://aber-owl.net/ontology/CL/download",
"http://aber-owl.net/ontology/PO/download",
"http://aber-owl.net/ontology/OBI/download",
"http://aber-owl.net/ontology/CHEBI/download",
"http://aber-owl.net/ontology/UBERON/download"]

while(!consistentOntologyFound) {
  consistentOntologyFound = true // not really, you know what i mean

  println "[UNMIREOT] Loading ontology from " + ontologyIRI

  OWLOntology ontology
  OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
  OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();

  config.setFollowRedirects(true);
  config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
  ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ontologyIRI)), config);

  def success = true;
  def oReasoner
  try {
    ReasonerConfiguration eConf = ReasonerConfiguration.getConfiguration()
    eConf.setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS, "24")
    eConf.setParameter(ReasonerConfiguration.INCREMENTAL_MODE_ALLOWED, "true")
    eConf.setParameter(ReasonerConfiguration.INCREMENTAL_TAXONOMY, "true")

    OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();

    OWLReasonerConfiguration rConf = new ElkReasonerConfiguration(ElkReasonerConfiguration.getDefaultOwlReasonerConfiguration(new NullReasonerProgressMonitor()), eConf);

    println "[UNMIREOT] Reasoning ontology with " + ontology.getImports().size() + " imports."
    oReasoner = reasonerFactory.createReasoner(ontology, rConf);
    oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
  } catch(e) {
    println ontology.getDirectImports()[0].getOntologyID()
    println "[UNMIREOT] Removing " + imports[0]

    OWLImportsDeclaration importDeclaration = manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(imports[0]));
    manager.applyChange(new RemoveImport(ontology, importDeclaration));

    File fileFormated = new File("unmireot_test.ontology");
    manager.saveOntology(ontology, IRI.create(fileFormated.toURI()));

    imports.remove(0)
    consistentOntologyFound = false
  }
println "[UNMIREOT] Unsatisfiable classes: " + oReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom().size()
}

println "Success!"
