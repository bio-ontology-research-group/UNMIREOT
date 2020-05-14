import groovy.json.*
def results = new JsonSlurper().parseText(new File("results.json").text)
def out = results.collect { k, v ->
  if(v instanceof Integer && v > 0) {
    
    "mkdir results/${k.tokenize('_')[4]}/ \n echo \"Processing ${k}\" \n groovy ../quick_repair.groovy combos/${k} results/${k.tokenize('_')[4]} > results/${k.tokenize('_')[4]}/out.log"
  }
}
out.removeAll([null])
new File('run_all.sh').text = out.join('\n')
