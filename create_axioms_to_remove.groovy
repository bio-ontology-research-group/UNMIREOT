import groovy.json.*

def toRemove = [:]

new File('jsons/').eachFile { f ->
  def combinations = new JsonSlurper().parseText(f.text)
  def naughtyCounts = [:]
  println f.toString()

  combinations.each { cName, unsats ->
    unsats.each { cIRI, axioms ->
      axioms.each { axiom ->
        if(!naughtyCounts.containsKey(axiom)) {
          naughtyCounts[axiom] = 0
        }
        naughtyCounts[axiom]++
      }
    }

    toRemove[cName] = []
    unsats.each { cIRI, axioms ->
      if(!axioms.any { toRemove[cName].contains(it) }) {
        def naughtiestInvolvedAxiom
        def naughtiestInvolvedScore = 0
        axioms.each {
          if(naughtyCounts[it] > naughtiestInvolvedScore) {
            naughtiestInvolvedAxiom = it
            naughtiestInvolvedScore = naughtyCounts[it]
          }
        }
        toRemove[cName] << naughtiestInvolvedAxiom
      }
    }
  }
}

new File('axioms_to_remove.json').text = new JsonBuilder(toRemove).toPrettyString()
