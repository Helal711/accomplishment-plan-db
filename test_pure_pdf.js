// Standalone pure JavaScript PDF generator for Accomplishment Plan DB
// Creates standard compliant PDF 1.4 without any external dependencies!

function escapePdfText(str) {
  if (!str) return '';
  return str.replace(/\\/g, '\\\\').replace(/\(/g, '\\(').replace(/\)/g, '\\)');
}

// Convert Unicode string (including Bengali & English) into PDF UTF-16BE hex format: <FEFF....>
function toPdfHexString(str) {
  if (!str) return '<FEFF>';
  let hex = 'FEFF';
  for (let i = 0; i < str.length; i++) {
    const code = str.charCodeAt(i);
    hex += code.toString(16).padStart(4, '0').toUpperCase();
  }
  return '<' + hex + '>';
}

function generateWebPdfBlob(month, tasks) {
  const itemsPerPage = 18;
  const totalPages = Math.max(1, Math.ceil(tasks.length / itemsPerPage));
  const pageWidth = 842;  // A4 Landscape width
  const pageHeight = 595; // A4 Landscape height
  
  const objects = [];
  function addObj(content) {
    objects.push(content);
    return objects.length;
  }

  // Pre-allocate object IDs
  // 1: Catalog
  // 2: Pages
  // 3: Font Helvetica
  // 4: Font Helvetica-Bold
  // For each page: Page Object, Content Object

  const catalogId = 1;
  const pagesId = 2;
  const fontRegularId = 3;
  const fontBoldId = 4;

  const pageObjectIds = [];
  const contentObjectIds = [];

  for (let p = 0; p < totalPages; p++) {
    pageObjectIds.push(5 + p * 2);
    contentObjectIds.push(6 + p * 2);
  }

  // Now create content streams for each page
  const colPositions = [30, 55, 230, 290, 345, 390, 435, 500, 570, 680, 750];
  const colTitles = ['#', 'Description', 'Amount', 'Mode', 'Unit', 'Priority', 'Status', 'Expected Date', 'Account', 'Code', 'Category'];

  const pagesContents = [];

  for (let p = 0; p < totalPages; p++) {
    const startIdx = p * itemsPerPage;
    const endIdx = Math.min(startIdx + itemsPerPage, tasks.length);
    const pageTasks = tasks.slice(startIdx, endIdx);

    let stream = '';

    // Color helpers:
    // RG: stroke RGB (0..1), rg: fill RGB (0..1)
    
    // Header background line
    stream += '0.117 0.533 0.898 rg\n'; // Corporate blue #1E88E5
    stream += 'BT /F2 15 Tf 30 562 Td (' + escapePdfText('Accomplishment Plan Report') + ') Tj ET\n';
    
    stream += '0.420 0.447 0.502 rg\n'; // Gray #6B7280
    stream += 'BT /F1 9.5 Tf 30 548 Td (' + escapePdfText('Master Task Accomplishments & Operational Expenses') + ') Tj ET\n';

    const printDate = new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    const metaStr = 'Month/Year: ' + month + '  |  Total Tasks: ' + tasks.length + '  |  Date: ' + printDate;
    stream += 'BT /F1 9.5 Tf 480 556 Td (' + escapePdfText(metaStr) + ') Tj ET\n';

    // Divider line below header
    stream += '0.898 0.906 0.922 RG 1 w 30 538 m 812 538 l S\n';

    // Column Headers
    let currentY = 520;
    stream += '0.122 0.161 0.216 rg\n'; // #1F2937
    colTitles.forEach((title, idx) => {
      const x = colPositions[idx];
      stream += 'BT /F2 8.5 Tf ' + x + ' ' + currentY + ' Td (' + escapePdfText(title) + ') Tj ET\n';
    });

    currentY -= 6;
    stream += '0.898 0.906 0.922 RG 1 w 30 ' + currentY + ' m 812 ' + currentY + ' l S\n';
    currentY -= 16;

    // Task rows
    if (pageTasks.length === 0) {
      stream += '0.420 0.447 0.502 rg\n';
      stream += 'BT /F1 9 Tf 30 ' + (currentY - 20) + ' Td (' + escapePdfText('No tasks recorded for this month.') + ') Tj ET\n';
    } else {
      pageTasks.forEach((task, idx) => {
        const rowNum = (startIdx + idx + 1).toString();
        const y = currentY;

        stream += '0.067 0.094 0.153 rg\n'; // Dark #111827
        stream += 'BT /F1 8.5 Tf ' + colPositions[0] + ' ' + y + ' Td (' + escapePdfText(rowNum) + ') Tj ET\n';
        stream += 'BT /F1 8.5 Tf ' + colPositions[1] + ' ' + y + ' Td (' + escapePdfText((task.description || '').slice(0, 32)) + ') Tj ET\n';
        stream += 'BT /F1 8.5 Tf ' + colPositions[2] + ' ' + y + ' Td (' + escapePdfText((task.amount || '').slice(0, 11)) + ') Tj ET\n';
        stream += 'BT /F1 8.5 Tf ' + colPositions[3] + ' ' + y + ' Td (' + escapePdfText((task.paymentMode || '').slice(0, 9)) + ') Tj ET\n';
        stream += 'BT /F1 8.5 Tf ' + colPositions[4] + ' ' + y + ' Td (' + escapePdfText((task.unit || '').slice(0, 9)) + ') Tj ET\n';
        
        // Priority color
        if (task.priority === 'High') stream += '0.863 0.149 0.149 rg\n';
        else if (task.priority === 'Med') stream += '0.851 0.467 0.024 rg\n';
        else stream += '0.086 0.639 0.290 rg\n';
        stream += 'BT /F2 8.5 Tf ' + colPositions[5] + ' ' + y + ' Td (' + escapePdfText(task.priority || '') + ') Tj ET\n';

        // Status
        stream += '0.118 0.251 0.686 rg\n'; // Status blue
        stream += 'BT /F2 8.5 Tf ' + colPositions[6] + ' ' + y + ' Td (' + escapePdfText((task.status || '').slice(0, 12)) + ') Tj ET\n';

        // Expected Date, Account, Code, Category
        stream += '0.067 0.094 0.153 rg\n';
        stream += 'BT /F1 8.5 Tf ' + colPositions[7] + ' ' + y + ' Td (' + escapePdfText((task.expectedDate || '').slice(0, 11)) + ') Tj ET\n';
        stream += 'BT /F1 8.5 Tf ' + colPositions[8] + ' ' + y + ' Td (' + escapePdfText((task.accountName || '').slice(0, 20)) + ') Tj ET\n';
        stream += 'BT /F1 8.5 Tf ' + colPositions[9] + ' ' + y + ' Td (' + escapePdfText((task.accountCode || '').slice(0, 11)) + ') Tj ET\n';
        stream += 'BT /F1 8.5 Tf ' + colPositions[10] + ' ' + y + ' Td (' + escapePdfText((task.category || '').slice(0, 14)) + ') Tj ET\n';

        // Row bottom separator line
        stream += '0.941 0.945 0.957 RG 0.5 w 30 ' + (y - 5) + ' m 812 ' + (y - 5) + ' l S\n';

        currentY -= 21;
      });
    }

    // Page Footer (repeated on every page)
    stream += '0.898 0.906 0.922 RG 1 w 30 35 m 812 35 l S\n';
    stream += '0.420 0.447 0.502 rg\n';
    stream += 'BT /F1 8.5 Tf 30 20 Td (' + escapePdfText('Accomplishment Plan DB • Confidential Internal Report') + ') Tj ET\n';
    const pageStr = 'Page ' + (p + 1) + ' of ' + totalPages;
    stream += 'BT /F1 8.5 Tf 400 20 Td (' + escapePdfText(pageStr) + ') Tj ET\n';
    stream += 'BT /F1 8.5 Tf 640 20 Td (' + escapePdfText('Developer by Md Al Helal Sarkar') + ') Tj ET\n';

    pagesContents.push(stream);
  }

  // Construct PDF Body
  const pdfParts = [];
  const xrefOffsets = [];

  function writeLine(str) {
    pdfParts.push(str + '\n');
  }

  writeLine('%PDF-1.4');
  writeLine('%\xE2\xE3\xCF\xD3');

  // Object 1: Catalog
  xrefOffsets[1] = pdfParts.join('').length;
  writeLine('1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj');

  // Object 2: Pages
  xrefOffsets[2] = pdfParts.join('').length;
  const kidsStr = pageObjectIds.map(id => id + ' 0 R').join(' ');
  writeLine('2 0 obj\n<< /Type /Pages /Kids [' + kidsStr + '] /Count ' + totalPages + ' >>\nendobj');

  // Object 3: Font Regular (Helvetica)
  xrefOffsets[3] = pdfParts.join('').length;
  writeLine('3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj');

  // Object 4: Font Bold (Helvetica-Bold)
  xrefOffsets[4] = pdfParts.join('').length;
  writeLine('4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj');

  // Add Page objects and Content objects
  for (let p = 0; p < totalPages; p++) {
    const pageId = pageObjectIds[p];
    const contentId = contentObjectIds[p];
    const contentStr = pagesContents[p];

    // Page Object
    xrefOffsets[pageId] = pdfParts.join('').length;
    writeLine(pageId + ' 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ' + pageWidth + ' ' + pageHeight + '] /Contents ' + contentId + ' 0 R /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> >>\nendobj');

    // Content Object
    xrefOffsets[contentId] = pdfParts.join('').length;
    const streamLen = Buffer.byteLength(contentStr, 'utf8');
    writeLine(contentId + ' 0 obj\n<< /Length ' + streamLen + ' >>\nstream\n' + contentStr + 'endstream\nendobj');
  }

  // Cross-reference table
  const startXref = pdfParts.join('').length;
  const totalObjectsCount = 4 + totalPages * 2;
  writeLine('xref');
  writeLine('0 ' + (totalObjectsCount + 1));
  writeLine('0000000000 65535 f ');
  for (let i = 1; i <= totalObjectsCount; i++) {
    const offset = (xrefOffsets[i] || 0).toString().padStart(10, '0');
    writeLine(offset + ' 00000 n ');
  }

  writeLine('trailer\n<< /Size ' + (totalObjectsCount + 1) + ' /Root 1 0 R >>');
  writeLine('startxref\n' + startXref);
  writeLine('%%EOF');

  return pdfParts.join('');
}

// Test with 65 tasks
const testTasks = [];
for (let i = 1; i <= 65; i++) {
  testTasks.push({
    taskNumber: i,
    description: 'Electric maintenance and repair job #' + i,
    amount: (1500 * i) + ' TK',
    paymentMode: i % 2 === 0 ? 'Bank' : 'Cash',
    unit: 'Unit-1',
    priority: i % 3 === 0 ? 'High' : (i % 3 === 1 ? 'Med' : 'Low'),
    status: i % 2 === 0 ? 'Approved' : 'Pending',
    expectedDate: '15 Nov 2026',
    accountName: 'Maintenance ACC',
    accountCode: 'ACC-EL' + (100 + i),
    category: 'Operational',
    remarks: 'Routine inspection completed'
  });
}

const pdfOutput = generateWebPdfBlob('Nov-2026', testTasks);
const fs = require('fs');
fs.writeFileSync('test_output.pdf', pdfOutput, 'utf8');
console.log('PDF Generated successfully! Size:', pdfOutput.length, 'bytes');

// Check with pdfinfo or basic validation
console.log('PDF starts with %PDF-1.4:', pdfOutput.startsWith('%PDF-1.4'));
console.log('PDF ends with %%EOF:', pdfOutput.trim().endsWith('%%EOF'));
