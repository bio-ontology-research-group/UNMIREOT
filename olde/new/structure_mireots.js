var fs = require('fs');
    _ = require('underscore')._;

var mireots = fs.readFileSync('mireot_ontologies.txt').toString().split("\n");

var results = {};

_.each(mireots, function(line, i) {
  console.log(i);
  var res = line.match(/FIND\] ([^ ]+) .*?\[(.+)\]/);
  if(line) {
    results[res[1]] = res[2].split(', ');
  } else {
    console.log(line);
  }
});

fs.writeFileSync('mireot_results.json', JSON.stringify(results, null, 2));

