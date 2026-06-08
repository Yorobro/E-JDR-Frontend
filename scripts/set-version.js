const fs = require('fs');
const path = require('path');

const newVersion = process.argv[2];
if (!newVersion) {
  console.error('Usage: node set-version.js <version>');
  process.exit(1);
}

const filePath = path.resolve(__dirname, '..', 'gradle.properties');
let content = fs.readFileSync(filePath, 'utf8');
if (/^version=.*/m.test(content)) {
  content = content.replace(/^version=.*/m, `version=${newVersion}`);
} else {
  content = `version=${newVersion}\n` + content;
}
fs.writeFileSync(filePath, content, 'utf8');
console.log(`Updated gradle.properties to version ${newVersion}`);
