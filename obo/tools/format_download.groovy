@Grab('org.yaml:snakeyaml:1.17')

import org.yaml.snakeyaml.Yaml

new File('../ontologies/get_ontologies.sh').text = new Yaml().load(new File('../ontologies.yml').text).ontologies.collect {
  "wget ${it.ontology_purl}"
}.join('\n')
