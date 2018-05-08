import groovy.json.*

def output = [ 'ontology\tunsatisfiable\tremoved' ]
def oResults = new JsonSlurper().parseText(new File("results.json").text)
def rmResults = new JsonSlurper().parseText(new File("rmcounts.json").text)

oResults.each { mergeId, uCount ->
  if(uCount != 'Inconsistent ontology' && uCount > 0) {
    def id = mergeId.tokenize('_')[4] 
    output << "${id}\t${uCount}\t${rmResults[id]}"
  }
}
new File('merged.tsv').text = output.join('\n')
