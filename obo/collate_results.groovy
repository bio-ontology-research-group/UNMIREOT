import groovy.json.*

def removalCounts = [:]
def cResults = [:]
def aResults = [:]
def totalUnsatisfiable = 0
def uniqueUnsatisfiable = 0

new File('results/').eachFile { oFolder ->
  def oName = oFolder.getName()
  def rFile = new File("results/${oName}/results.json")
  if(!rFile.exists()) {
    return;
  }
  def oResults = new JsonSlurper().parseText(rFile.text)

  removalCounts[oName] = 0
  oResults.each { axiom, classes ->
    removalCounts[oName]++
    if(!aResults.containsKey(axiom)) {
      aResults[axiom] = []
    }
    aResults[axiom] += classes
    aResults[axiom].unique()

    classes.each { oClass ->
      if(!cResults.containsKey(oClass)) {
        cResults[oClass] = []
        uniqueUnsatisfiable++
      }
      if(!cResults[oClass].contains(oName)) {
        cResults[oClass] << oName
      }

      totalUnsatisfiable++
    }
  }
}

aCounts = [:]
def topAxioms = [:]
aResults.each {
  aCounts[it.key] = it.value.size()
}

new File('axioms.json').text = new JsonBuilder(aResults).toPrettyString()
new File('axiom_counts.json').text = new JsonBuilder(aCounts).toPrettyString()
new File('classes.json').text = new JsonBuilder(cResults).toPrettyString()
new File('rmcounts.json').text = new JsonBuilder(removalCounts).toPrettyString()

println "Total unsatisfiable classes: ${totalUnsatisfiable}"
println "Unique unsatisfiable classes: ${uniqueUnsatisfiable}"

println "Total implicated axioms: ${aResults.size()}"

println aCounts.sort { it.value }
