// Simulate the browser environment in Node.js to test handleExportPDF execution
const fs = require('fs');

// Read ExportModal.tsx
const code = fs.readFileSync('src/components/ExportModal.tsx', 'utf8');

// Find handleExportPDF
const start = code.indexOf('const handleExportPDF = () => {');
const end = code.indexOf('return (', start);
console.log('handleExportPDF found, length:', end - start);
