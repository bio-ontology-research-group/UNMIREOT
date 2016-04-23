def elkobi = []
new File('elk_obi.out').eachLine { line ->
  elkobi << line
}

def hermitobi = []
new File('hermit_obi.out').eachLine { line ->
  hermitobi << line
}

hermitobi.each {
  if(!elkobi.contains(it)) {
    println it
  }
}
