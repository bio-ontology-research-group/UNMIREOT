import groovy.json.*

def inc = 0
def unload = 0
def unsat = 0
def problemOs = 0
def totalMireots = 0

new File('results').eachFile { file ->
  totalMireots++
  println ''
  println '## ' + file.getName()
  def results = new JsonSlurper().parseText(file.text)
  def problematic = false

  results.each { ont, res ->
     
    if(res == 'Inconsistent') {
      println ont + ': Inconsistent'
      println ''
      inc++
      problematic = true
    } else if(res == 'Unloadable') {
      println ont + ': Unloadable'
      println ''
      unload++
      problematic = true
    } else if(res.count != 0) {
      println ont + ': ' + res.count + ' unsatisfiable classes.'
      println ''
      unsat += res.count
      problematic = true
    }
  }

  if(problematic == true) {
    problemOs++
  } else {
      println 'No issues detected.'
  }
}

println '## Overall results'

println 'Total MIREOT ontologies: ' + totalMireots
      println ''
println 'Total MIREOT ontologies with some \'problem\' concerning its references: ' + problemOs
      println ''

println 'Inconsistent ontology combinations: ' + inc 
      println ''
println 'Unloadable ontology combinations: ' + unload
      println ''
println 'Unsatisfiable class total: ' + unsat
      println ''
