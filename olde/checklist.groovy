@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )
import groovyx.net.http.HTTPBuilder

def l = ["FO", "ERO", "UBERON", "CL", "ORDO", "CHEBI", "BT", "BTO", "TO", "GO", "HP", "ATO", "PATO", "EO", "PO", "OBI", "DOID", "SO", "AO", "IAO", "MP", "MPATH", "FBbt", "ZEA", "PR", "IDO", "MS", "OGMS", "UO" ]
new HTTPBuilder('http://aber-owl.net/').get(path: 'service/api/getStatuses.groovy') { resp, ontologies ->
  println l.findAll { ontologies[it].status != 'classified' }
}
