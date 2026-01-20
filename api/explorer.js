/**
 * =============================================================================
 * API: DB Explorer Page
 * =============================================================================
 * Trả về trang HTML tĩnh cho DB Explorer
 * URL: /api/explorer
 * =============================================================================
 */

export default function handler(req, res) {
  const html = `<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DB Explorer - Export CSV/XLSX</title>
  <script src="https://cdn.jsdelivr.net/npm/xlsx@0.18.5/dist/xlsx.full.min.js"></script>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: linear-gradient(135deg, #0a0a0a 0%, #1a1a2e 100%);
      color: #fff;
      min-height: 100vh;
      padding: 2rem;
    }
    .container { max-width: 1200px; margin: 0 auto; }
    h1 {
      text-align: center;
      margin-bottom: 2rem;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    .section {
      background: #1a1a1a;
      border-radius: 12px;
      padding: 1.5rem;
      margin-bottom: 1.5rem;
    }
    .section h2 {
      font-size: 1rem;
      color: #888;
      margin-bottom: 1rem;
      text-transform: uppercase;
      letter-spacing: 1px;
    }
    .row { display: flex; gap: 1rem; flex-wrap: wrap; align-items: flex-end; }
    .form-group { flex: 1; min-width: 200px; }
    label { display: block; margin-bottom: 0.5rem; color: #aaa; font-size: 0.9rem; }
    select, input, button {
      width: 100%;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      border: 1px solid #333;
      background: #0a0a0a;
      color: #fff;
      font-size: 1rem;
    }
    select:focus, input:focus { outline: none; border-color: #667eea; }
    button {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      cursor: pointer;
      font-weight: 600;
      transition: transform 0.2s, opacity 0.2s;
    }
    button:hover { transform: translateY(-2px); opacity: 0.9; }
    button:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
    .btn-secondary {
      background: transparent;
      border: 1px solid #667eea;
      color: #667eea;
    }
    .columns-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: 0.5rem;
      max-height: 300px;
      overflow-y: auto;
      padding: 0.5rem;
      background: #0a0a0a;
      border-radius: 8px;
    }
    .column-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem;
      background: #1a1a1a;
      border-radius: 6px;
      cursor: pointer;
    }
    .column-item:hover { background: #252525; }
    .column-item input[type="checkbox"] { width: auto; }
    .column-item .type { font-size: 0.75rem; color: #666; margin-left: auto; }
    .computed-list { margin-top: 1rem; }
    .computed-item {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.75rem;
      background: #0a0a0a;
      border-radius: 8px;
      margin-bottom: 0.5rem;
    }
    .computed-item span { flex: 1; font-family: monospace; color: #4ade80; }
    .btn-remove {
      background: #ff4444;
      padding: 0.25rem 0.75rem;
      font-size: 0.8rem;
    }
    .preview-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.9rem;
    }
    .preview-table th, .preview-table td {
      padding: 0.75rem;
      text-align: left;
      border-bottom: 1px solid #333;
    }
    .preview-table th { background: #252525; color: #667eea; font-weight: 600; }
    .preview-table tr:hover td { background: #1f1f1f; }
    .table-wrapper { overflow-x: auto; max-height: 400px; overflow-y: auto; }
    .export-buttons { display: flex; gap: 1rem; margin-top: 1rem; }
    .export-buttons button { flex: 1; }
    .status { padding: 1rem; border-radius: 8px; margin-top: 1rem; }
    .status.loading { background: rgba(102, 126, 234, 0.2); color: #667eea; }
    .status.error { background: rgba(255, 68, 68, 0.2); color: #ff6b6b; }
    .status.success { background: rgba(74, 222, 128, 0.2); color: #4ade80; }
    .info { font-size: 0.85rem; color: #666; margin-top: 0.5rem; }
  </style>
</head>
<body>
  <div class="container">
    <h1>DB Explorer - Export CSV/XLSX</h1>

    <div class="section">
      <h2>Step 1: Chon Database</h2>
      <div class="row">
        <div class="form-group">
          <label>Database</label>
          <select id="dbSelect" onchange="loadTables()">
            <option value="">-- Chon database --</option>
          </select>
        </div>
        <div class="form-group" style="flex: 0 0 auto;">
          <label>&nbsp;</label>
          <button onclick="loadDatabases()" class="btn-secondary">Refresh</button>
        </div>
      </div>
    </div>

    <div class="section">
      <h2>Step 2: Chon Table</h2>
      <div class="row">
        <div class="form-group">
          <label>Table</label>
          <select id="tableSelect" onchange="loadColumns()" disabled>
            <option value="">-- Chon table --</option>
          </select>
        </div>
      </div>
    </div>

    <div class="section">
      <h2>Step 3: Chon Columns</h2>
      <div class="row" style="margin-bottom: 1rem;">
        <button onclick="selectAllColumns()" class="btn-secondary" style="width: auto; padding: 0.5rem 1rem;">Chon tat ca</button>
        <button onclick="deselectAllColumns()" class="btn-secondary" style="width: auto; padding: 0.5rem 1rem;">Bo chon tat ca</button>
      </div>
      <div class="columns-grid" id="columnsGrid">
        <p style="color: #666; padding: 1rem;">Chon database va table truoc</p>
      </div>
    </div>

    <div class="section">
      <h2>Step 4: Computed Columns (Tuy chon)</h2>
      <p class="info">Them cot tinh toan, vi du: qr_url = "https://test-beryl-five-56.vercel.app/api/p?sku=" + sku</p>
      <div class="row" style="margin-top: 1rem;">
        <div class="form-group">
          <label>Ten cot moi</label>
          <input type="text" id="computedName" placeholder="vd: qr_url">
        </div>
        <div class="form-group">
          <label>Prefix (chuoi dau)</label>
          <input type="text" id="computedPrefix" placeholder="vd: https://test-beryl-five-56.vercel.app/api/p?sku=">
        </div>
        <div class="form-group">
          <label>Cot nguon</label>
          <select id="computedSource">
            <option value="">-- Chon cot --</option>
          </select>
        </div>
        <div class="form-group" style="flex: 0 0 auto;">
          <label>&nbsp;</label>
          <button onclick="addComputedColumn()">+ Them</button>
        </div>
      </div>
      <div class="computed-list" id="computedList"></div>
    </div>

    <div class="section">
      <h2>Step 5: Preview & Export</h2>
      <div class="row">
        <div class="form-group">
          <label>Gioi han dong preview</label>
          <input type="number" id="limitInput" value="10" min="1" max="1000">
        </div>
        <div class="form-group" style="flex: 0 0 auto;">
          <label>&nbsp;</label>
          <button onclick="loadPreview()">Load Preview</button>
        </div>
      </div>
      <div id="status"></div>
      <div class="table-wrapper" id="previewWrapper" style="margin-top: 1rem; display: none;">
        <table class="preview-table" id="previewTable">
          <thead id="previewHead"></thead>
          <tbody id="previewBody"></tbody>
        </table>
      </div>
      <div class="export-buttons">
        <button onclick="exportData('csv')" id="btnExportCSV" disabled>Export CSV</button>
        <button onclick="exportData('xlsx')" id="btnExportXLSX" disabled>Export XLSX</button>
      </div>
    </div>
  </div>

  <script>
    var currentData = [];
    var computedColumns = [];
    var allColumns = [];

    document.addEventListener('DOMContentLoaded', loadDatabases);

    function showStatus(type, message) {
      var status = document.getElementById('status');
      status.className = 'status ' + type;
      status.textContent = message;
      status.style.display = 'block';
    }

    async function loadDatabases() {
      showStatus('loading', 'Dang tai danh sach databases...');
      try {
        var res = await fetch('/api/db/databases');
        var data = await res.json();
        console.log('databases response:', data);
        if (data.success && data.databases) {
          var select = document.getElementById('dbSelect');
          select.innerHTML = '<option value="">-- Chon database --</option>';
          data.databases.forEach(function(dbName) {
            var opt = document.createElement('option');
            opt.value = dbName;
            opt.textContent = dbName;
            select.appendChild(opt);
          });
          showStatus('success', 'Tim thay ' + data.databases.length + ' databases');
        } else {
          showStatus('error', data.error || 'Khong lay duoc databases');
        }
      } catch (err) {
        console.error('loadDatabases error:', err);
        showStatus('error', 'Loi ket noi: ' + err.message);
      }
    }

    async function loadTables() {
      var db = document.getElementById('dbSelect').value;
      var tableSelect = document.getElementById('tableSelect');
      if (!db) {
        tableSelect.disabled = true;
        tableSelect.innerHTML = '<option value="">-- Chon table --</option>';
        return;
      }
      showStatus('loading', 'Dang tai danh sach tables...');
      try {
        var res = await fetch('/api/db/tables?db=' + encodeURIComponent(db));
        var data = await res.json();
        console.log('tables response:', data);
        if (data.success && data.tables) {
          tableSelect.disabled = false;
          tableSelect.innerHTML = '<option value="">-- Chon table --</option>';
          data.tables.forEach(function(tbl) {
            var opt = document.createElement('option');
            opt.value = tbl;
            opt.textContent = tbl;
            tableSelect.appendChild(opt);
          });
          showStatus('success', 'Tim thay ' + data.tables.length + ' tables trong ' + db);
        } else {
          showStatus('error', data.error || 'Khong lay duoc tables');
        }
      } catch (err) {
        console.error('loadTables error:', err);
        showStatus('error', 'Loi ket noi: ' + err.message);
      }
    }

    async function loadColumns() {
      var db = document.getElementById('dbSelect').value;
      var table = document.getElementById('tableSelect').value;
      var grid = document.getElementById('columnsGrid');
      var sourceSelect = document.getElementById('computedSource');
      if (!db || !table) {
        grid.innerHTML = '<p style="color: #666; padding: 1rem;">Chon database va table truoc</p>';
        return;
      }
      showStatus('loading', 'Dang tai danh sach columns...');
      try {
        var res = await fetch('/api/db/columns?db=' + encodeURIComponent(db) + '&table=' + encodeURIComponent(table));
        var data = await res.json();
        console.log('columns response:', data);
        if (data.success && data.columns) {
          allColumns = data.columns;
          grid.innerHTML = '';
          sourceSelect.innerHTML = '<option value="">-- Chon cot --</option>';
          data.columns.forEach(function(col) {
            var label = document.createElement('label');
            label.className = 'column-item';
            label.innerHTML = '<input type="checkbox" value="' + col.name + '" checked><span>' + col.name + '</span><span class="type">' + col.type + '</span>';
            grid.appendChild(label);
            var opt = document.createElement('option');
            opt.value = col.name;
            opt.textContent = col.name;
            sourceSelect.appendChild(opt);
          });
          showStatus('success', 'Tim thay ' + data.columns.length + ' columns trong ' + table);
        } else {
          showStatus('error', data.error || 'Khong lay duoc columns');
        }
      } catch (err) {
        console.error('loadColumns error:', err);
        showStatus('error', 'Loi ket noi: ' + err.message);
      }
    }

    function selectAllColumns() {
      document.querySelectorAll('#columnsGrid input[type="checkbox"]').forEach(function(cb) { cb.checked = true; });
    }

    function deselectAllColumns() {
      document.querySelectorAll('#columnsGrid input[type="checkbox"]').forEach(function(cb) { cb.checked = false; });
    }

    function addComputedColumn() {
      var name = document.getElementById('computedName').value.trim();
      var prefix = document.getElementById('computedPrefix').value;
      var source = document.getElementById('computedSource').value;
      if (!name || !source) {
        alert('Vui long nhap ten cot va chon cot nguon');
        return;
      }
      computedColumns.push({ name: name, prefix: prefix, source: source });
      renderComputedColumns();
      document.getElementById('computedName').value = '';
      document.getElementById('computedPrefix').value = '';
      document.getElementById('computedSource').value = '';
    }

    function removeComputedColumn(index) {
      computedColumns.splice(index, 1);
      renderComputedColumns();
    }

    function renderComputedColumns() {
      var list = document.getElementById('computedList');
      if (computedColumns.length === 0) {
        list.innerHTML = '';
        return;
      }
      list.innerHTML = computedColumns.map(function(col, i) {
        return '<div class="computed-item"><span>' + col.name + ' = "' + col.prefix + '" + ' + col.source + '</span><button class="btn-remove" onclick="removeComputedColumn(' + i + ')">Xoa</button></div>';
      }).join('');
    }

    async function loadPreview() {
      var db = document.getElementById('dbSelect').value;
      var table = document.getElementById('tableSelect').value;
      var limit = document.getElementById('limitInput').value || 10;
      if (!db || !table) {
        showStatus('error', 'Vui long chon database va table');
        return;
      }
      var selectedColumns = [];
      document.querySelectorAll('#columnsGrid input[type="checkbox"]:checked').forEach(function(cb) {
        selectedColumns.push(cb.value);
      });
      if (selectedColumns.length === 0) {
        showStatus('error', 'Vui long chon it nhat 1 column');
        return;
      }
      showStatus('loading', 'Dang tai du lieu...');
      try {
        var url = '/api/db/data?db=' + encodeURIComponent(db) + '&table=' + encodeURIComponent(table) + '&columns=' + selectedColumns.join(',') + '&limit=' + limit;
        var res = await fetch(url);
        var data = await res.json();
        console.log('data response:', data);
        if (data.success && data.data) {
          currentData = data.data.map(function(row) {
            var newRow = Object.assign({}, row);
            computedColumns.forEach(function(comp) {
              newRow[comp.name] = (comp.prefix || '') + (row[comp.source] || '');
            });
            return newRow;
          });
          renderPreview(selectedColumns);
          showStatus('success', 'Da tai ' + currentData.length + ' dong du lieu');
          document.getElementById('btnExportCSV').disabled = false;
          document.getElementById('btnExportXLSX').disabled = false;
        } else {
          showStatus('error', data.error || 'Khong lay duoc data');
        }
      } catch (err) {
        console.error('loadPreview error:', err);
        showStatus('error', 'Loi ket noi: ' + err.message);
      }
    }

    function renderPreview(selectedColumns) {
      var wrapper = document.getElementById('previewWrapper');
      var head = document.getElementById('previewHead');
      var body = document.getElementById('previewBody');
      var allCols = selectedColumns.concat(computedColumns.map(function(c) { return c.name; }));
      head.innerHTML = '<tr>' + allCols.map(function(col) { return '<th>' + col + '</th>'; }).join('') + '</tr>';
      body.innerHTML = currentData.map(function(row) {
        return '<tr>' + allCols.map(function(col) { return '<td>' + (row[col] != null ? row[col] : '') + '</td>'; }).join('') + '</tr>';
      }).join('');
      wrapper.style.display = 'block';
    }

    async function exportData(format) {
      var db = document.getElementById('dbSelect').value;
      var table = document.getElementById('tableSelect').value;
      if (!db || !table) {
        alert('Vui long chon database va table truoc');
        return;
      }
      var selectedColumns = [];
      document.querySelectorAll('#columnsGrid input[type="checkbox"]:checked').forEach(function(cb) {
        selectedColumns.push(cb.value);
      });
      if (selectedColumns.length === 0) {
        alert('Vui long chon it nhat 1 column');
        return;
      }

      showStatus('loading', 'Dang tai TAT CA du lieu de export...');
      console.log('Export - Computed columns:', computedColumns);
      console.log('Export - Selected columns:', selectedColumns);

      try {
        // Goi API KHONG CO LIMIT de lay tat ca rows
        var url = '/api/db/data?db=' + encodeURIComponent(db) + '&table=' + encodeURIComponent(table) + '&columns=' + selectedColumns.join(',');
        var res = await fetch(url);
        var data = await res.json();

        if (data.success && data.data) {
          // Apply computed columns
          var exportData = data.data.map(function(row) {
            var newRow = Object.assign({}, row);
            computedColumns.forEach(function(comp) {
              newRow[comp.name] = (comp.prefix || '') + (row[comp.source] || '');
            });
            return newRow;
          });

          var filename = table + '_export_' + new Date().toISOString().slice(0,10);
          showStatus('success', 'Da tai ' + exportData.length + ' dong. Dang xuat file...');

          if (format === 'csv') {
            doExportCSV(exportData, selectedColumns, filename);
          } else {
            doExportXLSX(exportData, selectedColumns, filename);
          }
        } else {
          showStatus('error', data.error || 'Khong lay duoc data');
        }
      } catch (err) {
        console.error('exportData error:', err);
        showStatus('error', 'Loi ket noi: ' + err.message);
      }
    }

    function doExportCSV(dataToExport, selectedColumns, filename) {
      var allCols = selectedColumns.concat(computedColumns.map(function(c) { return c.name; }));
      var csv = allCols.join(',') + '\\n';
      dataToExport.forEach(function(row) {
        csv += allCols.map(function(col) {
          var val = row[col] != null ? String(row[col]) : '';
          if (val.indexOf(',') >= 0 || val.indexOf('"') >= 0 || val.indexOf('\\n') >= 0) {
            val = '"' + val.replace(/"/g, '""') + '"';
          }
          return val;
        }).join(',') + '\\n';
      });
      downloadFile(csv, filename + '.csv', 'text/csv;charset=utf-8;');
      showStatus('success', 'Da xuat ' + dataToExport.length + ' dong ra file ' + filename + '.csv');
    }

    function doExportXLSX(dataToExport, selectedColumns, filename) {
      var allCols = selectedColumns.concat(computedColumns.map(function(c) { return c.name; }));

      // Tao array of arrays thay vi json_to_sheet de dam bao thu tu cot dung
      var aoa = [];
      // Header row
      aoa.push(allCols);
      // Data rows
      dataToExport.forEach(function(row) {
        var rowData = allCols.map(function(col) {
          return row[col] != null ? row[col] : '';
        });
        aoa.push(rowData);
      });

      var ws = XLSX.utils.aoa_to_sheet(aoa);
      var wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, 'Data');
      XLSX.writeFile(wb, filename + '.xlsx');
      showStatus('success', 'Da xuat ' + dataToExport.length + ' dong ra file ' + filename + '.xlsx');
    }

    function downloadFile(content, filename, mimeType) {
      var blob = new Blob([content], { type: mimeType });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }
  </script>
</body>
</html>`;

  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.status(200).send(html);
}
