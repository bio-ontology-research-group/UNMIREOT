import groovy.json.*

def mostCommon = [:]
def total = 0
def distinct = 0

new File('naughtycounts/').eachFile { f ->
  def counts = new JsonSlurper().parseText(f.text)
  counts.each { k, v ->
    if(!mostCommon.containsKey(k)) {
      mostCommon[k] = 0;
      distinct++
    }
    mostCommon[k] += v
    total+=v
  }
}

println total
println distinct
new File('all_axiom_counts.json').text = new JsonBuilder(mostCommon).toPrettyString()
