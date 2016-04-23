# EFO Combination Results Breakdown

All ontologies, including EFO, were the most recent versions available from AberOWL at 14:00 UTC 2016-04-22.

The version of EFO used has 18,596 classes and 25,991 axioms.

Considering ELK results alone, the sum of unsatisfiable classes caused by individual imports into EFO is 2,365. When all imports are considered, the ontology is inconsistent. After removing ERO as an import, the ontology becomes consistent, with a total number of 52,540 unsatisfiable classes.

The only case, so far, in which a more expressive reasoner revealed additional unsatisfiable classes was OBI, for which there were 3 more unsatisfiable classes.

I am currently awaiting results (or not, haha) from the following experiments, which originally timed out but were deemed 'interesting' because there were unsatisfiable results revealed by ELK, so have been given extra time:

* FBbt with HermiT
* MP with FACT++
* HP with FACT++

##All

This is the combination of EFO with all ontologies below, minus IDO, because this is not loadable. 

ELK: *Inconsistent Ontology*

Loading the inconsistent ontology in Protege to discover the source of the inconsistency required the removal of ZEA as an import because of an import renaming issue Protege wasn't able to sort out. Classifying the combined ontology in Protege using ELK confirmed the inconsistency, and the explanation tool revealed 21 axioms causing inconsistency before it ran out of RAM to continue, but the theme seems to be ObsoleteProperty, sampled in the following immage:

![inconsistencies](https://alsuti.xyz/NkON5amgb.png "EFO_ALL Inconsistencies")

After removal of ERO, the ontology is consistent (this version also includes ZEA). This combined ontology has a total of 297,591 classes and 199,587 axioms.

ELK: 52,540 Unsatisfiable Classes

##ERO

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable classes

HermiT: 0 Unsatisfiable Classes

##UBERON

ELK: 650 Unsatisfiable Classes

FACT++: Unable to classify (OWL-Full)

HermiT: Unable to classify (OWL-Full)

Using http://purl.obolibrary.org/obo/uberon/basic.obo (Basic axiom version of UBERON)

ELK: 205 Unsatisfiable Classes

HermiT: 205 Unsatisfiable Classes

##CL

ELK: 191 Unsatisfiable Classes

FACT++: Reasoner Timeout

HermiT: Unable to load ontology (OWLAPI3)

##ORDO

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: 0 Unsatisfiable Classes

##CHEBI

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: 0 Unsatisfiable Classes

##BTO


ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: 0 Unsatisfiable Classes

##TO

ELK: 5 Unsatisfiable Classes

FACT++: 5 Unsatisfiable Classes

HermiT: Unable to load ontology (OWLAPI3)

##GO

ELK: 0 Unsatisfiable Classes

FACT++: Reasoner Timeout (Uncaught)

HermiT: 0 Unsatisfiable Classes

##HP

ELK: 503 Unsatisfiable Classes

FACT++: Reasoner Timeout (uncaught)

HermiT: Unable to load ontology (OWLAPI3)

##PATO

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: 0 Unsatisfiable Classes

##EO

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: 0 Unsatisfiable Classes

##PO

ELK: 150 Unsatisfiable Classes

FACT++: 150 Unsatisfiable Classes

HermiT: 150 Unsatisfiable Classes

##OBI

ELK: 138 Unsatisfiable Classes

FACT++: *141 Unsatisfiable Classes*

HermiT: *141 Unsatisfiable Classes* (note this result was achieved much quicker than via FACT++, but still very slowly)

The three extra classes which became unsatisfiable with the more expressive reasoners are: 
* http://purl.obolibrary.org/obo/OBI_0100010 
* http://purl.obolibrary.org/obo/OBI_0001946
* http://purl.obolibrary.org/obo/OBI_0001951


##DOID

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: Unable to load ontology (OWLAPI3)

##SO

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: 0 Unsatisfiable Classes

##IAO

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: 0 Unsatisfiable Classes

##MP

ELK: 728 Unsatisfiable Classes

FACT++: Reasoner Timeout (Uncaught)

HermiT: Unable to load ontology (OWLAPI3)

##MPATH

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: 0 Unsatisfiable Classes

##FBbt

ELK: 0 Unsatisfiable Classes

FACT++: Reasoner Timeout (Caught)

HermiT: Reasoner Timeout (Caught)

##ZEA

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: 0 Unsatisfiable Classes

##PR

ELK: 0 Unsatisfiable Classes

FACT++: Reasoner Timeout (Uncaught)

HermiT: Reasoner Timeout (Uncaught)

##IDO

ELK: Unable to load ontology (see full results for details)

FACT++: Unable to load ontology (see full results for details)

HermiT: Unable to load ontology (OWLAPI3)

##OGMS

ELK: 0 Unsatisfiable Classes

FACT++: 0 Unsatisfiable Classes

HermiT: Unable to load ontology (OWLAPI3)
