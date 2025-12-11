/**
 * =============================================================================
 * API: /api/db/data
 * =============================================================================
 *
 * MỤC ĐÍCH:
 * - Lấy dữ liệu từ table với các columns đã chọn
 * - Dùng cho Preview và Export trong trang DB Explorer
 *
 * CÁCH GỌI:
 * - GET https://test-beryl-five-56.vercel.app/api/db/data?db=mirror_product&table=products&columns=id,sku,name,price
 * - GET https://test-beryl-five-56.vercel.app/api/db/data?db=mirror_product&table=products&columns=*  (lấy tất cả)
 * - GET ...&limit=10  (giới hạn số rows cho preview)
 *
 * PARAMS:
 * - db: tên database
 * - table: tên table
 * - columns: danh sách columns cách nhau bằng dấu phẩy, hoặc "*" để lấy tất cả
 * - limit (optional): giới hạn số rows trả về (mặc định: không giới hạn)
 *
 * RESPONSE:
 * {
 *   "success": true,
 *   "database": "mirror_product",
 *   "table": "products",
 *   "columns": ["id", "sku", "name", "price"],
 *   "rowCount": 156,
 *   "data": [
 *     { "id": 1, "sku": "RING-001", "name": "Diamond Ring", "price": 15000000 },
 *     { "id": 2, "sku": "NECK-002", "name": "Gold Necklace", "price": 25000000 },
 *     ...
 *   ]
 * }
 *
 * =============================================================================
 */

import pg from 'pg';

const { Pool } = pg;

/**
 * Tạo connection pool với database được chỉ định
 */
function createPool(database) {
  return new Pool({
    host: process.env.DB_HOST || 'mirror-user-db.ch0a2gs8awvf.ap-southeast-1.rds.amazonaws.com',
    port: parseInt(process.env.DB_PORT || '5432'),
    user: process.env.DB_USER || 'mirror_user',
    password: process.env.DB_PASSWORD || '2v~]Tob:<epZzN3e:lF5zAGWbnz3',
    database: database,
    ssl: { rejectUnauthorized: false },
    max: 5,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 10000,
  });
}

/**
 * Validate tên column để tránh SQL injection
 * - Chỉ cho phép chữ cái, số, underscore
 * - Column name phải bắt đầu bằng chữ cái hoặc underscore
 */
function isValidColumnName(name) {
  return /^[a-zA-Z_][a-zA-Z0-9_]*$/.test(name);
}

export default async function handler(req, res) {
  // Cấu hình CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'GET') {
    return res.status(405).json({
      success: false,
      error: 'Chỉ hỗ trợ GET request'
    });
  }

  // Lấy params từ query
  const { db, table, columns, limit } = req.query;

  // Validate params bắt buộc
  if (!db || !table) {
    return res.status(400).json({
      success: false,
      error: 'Thiếu tham số',
      message: 'Cần cung cấp "db" và "table". Ví dụ: /api/db/data?db=mirror_product&table=products'
    });
  }

  // Validate table name để tránh SQL injection
  if (!isValidColumnName(table)) {
    return res.status(400).json({
      success: false,
      error: 'Tên table không hợp lệ',
      message: 'Tên table chỉ được chứa chữ cái, số và underscore'
    });
  }

  // Xử lý columns
  let selectedColumns = '*';
  let columnList = [];

  if (columns && columns !== '*') {
    // Parse danh sách columns từ query string
    columnList = columns.split(',').map(c => c.trim());

    // Validate từng column name
    for (const col of columnList) {
      if (!isValidColumnName(col)) {
        return res.status(400).json({
          success: false,
          error: 'Tên column không hợp lệ',
          message: `Column "${col}" không hợp lệ. Chỉ được chứa chữ cái, số và underscore`
        });
      }
    }

    // Wrap column names trong double quotes để handle reserved words
    selectedColumns = columnList.map(c => `"${c}"`).join(', ');
  }

  // Validate limit nếu có
  let limitClause = '';
  if (limit) {
    const limitNum = parseInt(limit);
    if (isNaN(limitNum) || limitNum < 1) {
      return res.status(400).json({
        success: false,
        error: 'Limit không hợp lệ',
        message: 'Limit phải là số nguyên dương'
      });
    }
    limitClause = ` LIMIT ${limitNum}`;
  }

  const pool = createPool(db);
  let client;

  try {
    client = await pool.connect();

    // Tìm primary key hoặc column đầu tiên để ORDER BY (giữ thứ tự như PostgreSQL)
    let orderByColumn = null;
    try {
      const pkQuery = `
        SELECT a.attname as column_name
        FROM pg_index i
        JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey)
        WHERE i.indrelid = '"${table}"'::regclass AND i.indisprimary
        LIMIT 1
      `;
      const pkResult = await client.query(pkQuery);
      if (pkResult.rows.length > 0) {
        orderByColumn = pkResult.rows[0].column_name;
      }
    } catch (e) {
      // Nếu không tìm được PK, bỏ qua
    }

    // Nếu không có PK, thử dùng column 'id' hoặc column đầu tiên
    if (!orderByColumn) {
      if (columnList.length > 0 && columnList.includes('id')) {
        orderByColumn = 'id';
      } else if (columnList.length > 0) {
        orderByColumn = columnList[0];
      }
    }

    // Query lấy data từ table
    // Table name được wrap trong double quotes để handle reserved words
    let orderClause = orderByColumn ? ` ORDER BY "${orderByColumn}" ASC` : '';
    const query = `SELECT ${selectedColumns} FROM "${table}"${orderClause}${limitClause}`;

    const result = await client.query(query);

    // Lấy tên columns từ kết quả (trường hợp dùng SELECT *)
    const resultColumns = result.fields.map(f => f.name);

    return res.status(200).json({
      success: true,
      database: db,
      table: table,
      columns: columnList.length > 0 ? columnList : resultColumns,
      rowCount: result.rowCount,
      data: result.rows
    });

  } catch (error) {
    console.error('Database error:', error);

    // Xử lý các lỗi cụ thể
    if (error.code === '3D000') {
      return res.status(404).json({
        success: false,
        error: 'Database không tồn tại',
        message: `Không tìm thấy database: ${db}`
      });
    }

    if (error.code === '42P01') {
      return res.status(404).json({
        success: false,
        error: 'Table không tồn tại',
        message: `Không tìm thấy table: ${table}`
      });
    }

    if (error.code === '42703') {
      return res.status(400).json({
        success: false,
        error: 'Column không tồn tại',
        message: error.message
      });
    }

    return res.status(500).json({
      success: false,
      error: 'Không thể lấy dữ liệu',
      message: error.message
    });
  } finally {
    if (client) client.release();
    await pool.end();
  }
}
