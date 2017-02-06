import groovy.json.*

def counts = new JsonSlurper().parseText(new File('all_axiom_counts.json').text)

println counts.sort { it.value }

