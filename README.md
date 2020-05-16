# UNMIREOT

Tools to identify, diagnose, and semi-automatically repair hidden contradictions in biomedical ontologies.

## Running the tool

To run the tool to combine an ontology with its imports closure (so, if you want to combine two or more ontologies, simply add an additional import declaration to each, and then run this script on it):

```bash
$ groovy merge_imports.groovy ontology.owl
```

To check an ontology for unsatisfiable classes, and to diagnose causes of those unsatisfiable classes:

```bash
$ groovy quick_repair.groovy ontology.owl output_dir/
```

*explain.groovy* contains the naive algorithm, but you probably don't want to use that.

## Experimental results

The result of the analysis over OBO Foundry can be found in obo/.

* *obo/results.json* contains a list of OBO ontologies, and the results of an attempt to combine it with the repaired OBO Foundry meta-ontology. Either contains an inconsistency message, a loading error, or the number of unsatisfiable classes upon successful combination.
* *obo/results/* one subdirectory for each OBO ontology+OBO core combination with unsatisfiable classes, containing:
  * *out.log* The output of the quick_repair script
  * Each axiom removed, and which classes doing so made satisfiable
* *obo/axioms.json* All of the axioms removed for all OBO ontologies, and which classes they made satisfiable.
* *obo/axiom_counts.json* Each of the axioms removed from the OBO ontologies, and how many classes it made satisfiable. Interestingly, some of these are zero - that's because every class it affected had another indepdendent cause for its unsatisfiability.
* *obo/rmcounts.json* Each OBO ontology, and the number of axioms that had to be removed from it to made all of its classes unsatisfiable (UPHENO 37!!)
* *obo/classes.json* Every unsatisfiable class found, and which ontologies it was unsatisfiable in.

### Recreating the experiment

You can also find the files used to run the experiment there. You will have to obtain your own ontology files, though. The *ontologies.yml* file downloaded from the OBO Foundry at the time of the experiment is provided, but the download links will give you newer versions of those ontologies than the ones analysed in the original experiment, so the results may differ somewhat. To create a simple bash script to download all of the ontology files in the *ontologies.yml* file, you can run:

```bash
groovy tools/format_download.groovy
cd ontologies
chmod a+x get_ontologies.sh
./get_ontologies.sh
```

Because it's relatively small, the repaired OBO Foundry meta-ontology is provided in *obo/core/obofoundry_core_fixed.owl*. 

If you want to recreate the experiment, note that it will overwrite the given result files. First, you will have to fill *obo/ontologies/* with the ontologies you're interested in, then run the following:

```bash
cd obo/
groovy combine_each_folder.groovy core/obofoundry_core_fixed.owl ontologies/
```

This will combine each ontologies in *obo/ontologies* with the OBO Foundry meta-ontology, and run a satisfiability check on each of them. The results, including which ontologies were unloadable, inconsistent, and numbers of unsatisfiable classes, will be recorded in *obo/results.json*. The combined ontologies will be stored in *obo/combos*. You can then run the quick repair algorithm on them individually, or create a simple bash script to do so for you:

```bash
groovy make_bash.groovy
chmod a+x run_all.sh
./run_all.sh
```

Be advised, that to completely recreate the experiment, for all OBO ontologies, will take a long time and a lot of computational power. When it's done, there will be an *obo/results* directory, with subdirectories for each combination, including the output of the script, axioms removed, and a coherent version of the ontology. You can then collate all the results to generate the *axioms.json*, *axiom_counts.json*, *rmcounts.json*, and *classes.json* results files, and view overall axiom counts etc with:

```
groovy collate_results.groovy
```

You can also run the *create_association.groovy* file, to create a TSV showing the relationship between the number of unsatisfiable classes in each OBO ontology and the number of axioms that had to be removed from it to repair them all.
