# UNMIREOT

Tools for combining ontologies with their imports, and removing problematic axioms until they are consistent.

## Running the tool

To run the tool to combine an ontology with its imports closure, and check for unsatisfiable classes:

$ groovy merge_imports.groovy ontology.owl

To run the algorithm to diagnose and make an ontology consistent:

$ groovy quick_repair.groovy ontology.owl output_dir/

## Experiment results

The result of the analysis over OBO Foundry can be found in obo/. In obo/results.json you can find a list of the initial combination results for each ontology. In obo/results/, you can find a directory for each consistent OBO Foundry ontology-core combination with unsatisfiable classes.

You can also find the files used to run the experiment there.
