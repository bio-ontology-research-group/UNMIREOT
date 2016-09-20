@Grapes([
  @Grab(group='com.google.code.gson', module='gson', version='2.3.1'),
  @Grab(group='org.slf4j', module='slf4j-log4j12', version='1.7.10'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.1.0'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.1.0'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.1.0'),
  @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.1.0'),
  @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.2'),
  @Grab(group='org.codehaus.gpars', module='gpars', version='1.1.0'),
  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' ),
  @Grab(group='net.sourceforge.owlapi', module='jfact', version='4.0.3'),
  @GrabConfig(systemClassLoader=true)
])
 
import org.semanticweb.owlapi.io.* 
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.AddImport
import groovyx.net.http.HTTPBuilder
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
import uk.ac.manchester.cs.jfact.JFactFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration
import org.semanticweb.elk.reasoner.config.*

def ontologyIRI = args[0]

// Load input ontology

println "[UNMIREOT] Loading ontology from " + ontologyIRI

OWLOntology ontology
OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
config.setFollowRedirects(true);
config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

ontology = manager.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create(ontologyIRI)));

println "[UNMIREOT] Reasoning with JFact"

OWLReasonerConfiguration rConf = new SimpleConfiguration();

OWLReasoner oReasoner = new JFactFactory().createReasoner(ontology, rConf);
oReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
def unsatisfiable = oReasoner.getEquivalentClasses(manager.getOWLDataFactory().getOWLNothing()).getEntitiesMinusBottom()
println "[UNMIREOT] Unsatisfiable classes: " + unsatisfiable.size()
unsatisfiable.each {
  println it
}
