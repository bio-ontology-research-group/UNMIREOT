import groovy.json.*

def mireots = new JsonSlurper().parseText(new File("mireot_ontologies.json").text)
mireots.each { id, refs ->
  def command = "groovy unmireot.groovy " + id
  command.execute()
}
