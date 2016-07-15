import groovy.json.*

def inc = 0
def unload = 0
def unsat = 0
def problemOs = 0
def totalMireots = 0

new File('results').eachFile { file ->
  totalMireots++
  println '## ' + file.getName()
  def results = new JsonSlurper().parseText(file.text)
  def problematic = false

  results.each { ont, res ->
     
    if(res == 'Inconsistent') {
      println ont + ': Inconsistent'
      inc++
      problematic = true
    } else if(res == 'Unloadable') {
      println ont + ': Unloadable'
      unload++
      problematic = true
    } else if(res.count != 0) {
      println ont + ': ' + res.count + ' unsatisfiable classes.'
      unsat += res.count
      problematic = true
    }
  }

  if(problematic == true) {
    problemOs++
  }
}

println '## Overall results'

println 'Total MIREOT ontologies: ' + totalMireots
println 'Total MIREOT ontologies with some \'problem\' concerning its references: ' + problemOs

println 'Inconsistent ontology combinations: ' + inc 
println 'Unloadable ontology combinations: ' + unload
println 'Unsatisfiable class total: ' + unsat
