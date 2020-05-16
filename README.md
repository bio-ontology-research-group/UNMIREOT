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

The result of the analysis over OBO Foundry can be found in obo/. In obo/results.json you can find a list of the initial combination results for each ontology. In *obo/results/*, you can find a directory for each consistent OBO Foundry ontology-core combination with unsatisfiable classes.

You can also find the files used to run the experiment there. You will have to obtain your own ontology files, though. The *ontologies.yml* file downloaded from the OBO Foundry at the time of the experiment is provided, but the download links will give you newer versions of those ontologies than the ones analysed in the original experiment. Because it's relatively small, the repaired OBO Foundry meta-ontology is provided in *obo/core/obofoundry_core_fixed.owl*. 

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

Be advised, that to completely recreate the experiment, for all OBO ontologies, will take a long time and a lot of computational power. When it's done, there will be an *obo/results* directory, with subdirectories for each combination, including the output of the script, axioms removed, and a coherent version of the ontology. You can then collate all the results to view overall axiom counts etc with:

```
groovy collate_results.groovy
```
