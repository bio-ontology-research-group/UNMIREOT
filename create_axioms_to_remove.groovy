import groovy.json.*

def toRemove = [:]

new File('jsons/').eachFile { f ->
  def combinations = new JsonSlurper().parseText(f.text)

  combinations.each { cName, unsats ->
    def naughtyCounts = [:]

    unsats.each { cIRI, axioms ->
      axioms.each { axiom ->
        if(!naughtyCounts.containsKey(axiom)) {
          naughtyCounts[axiom] = 0
        }
        naughtyCounts[axiom]++
      }
    }

    new File('naughtycounts/'+cName+'.json').text = new JsonBuilder(naughtyCounts).toPrettyString()

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
        if(naughtiestInvolvedAxiom != null) { // This happens in the case of it being owl#Nothing, in which case there are no explanations
          toRemove[cName] << naughtiestInvolvedAxiom
        }
      }
    }
  }
}

new File('axioms_to_remove.json').text = new JsonBuilder(toRemove).toPrettyString()
