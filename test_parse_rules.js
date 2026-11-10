const fs = require('fs');

console.log("Analyzing firestore.rules...");
const rules = fs.readFileSync('firestore.rules', 'utf8');

// Check all match statements
const matches = [...rules.matchAll(/match\s+([^\s{]+)/g)].map(m => m[1]);
console.log("Matched paths in rules:", matches);

// Check if Month_Counters is covered
const hasMonthCounters = matches.includes('/Month_Counters/{counterId}');
console.log("Has /Month_Counters/{counterId} in rules file:", hasMonthCounters);

