/**
 * =============================================================================
 * API: /api/db/columns
 * =============================================================================
 *
 * MỤC ĐÍCH:
 * - Liệt kê tất cả columns của table đã chọn
 * - Dùng cho checkbox list "Chọn Columns" trong trang DB Explorer
 *
 * CÁCH GỌI:
 * - GET https://test-beryl-five-56.vercel.app/api/db/columns?db=mirror_product&table=products
 *
 * PARAMS:
 * - db: tên database
 * - table: tên table
 *
 * RESPONSE:
 * {
 *   "success": true,
 *   "database": "mirror_product",
 *   "table": "products",
 *   "columns": [
 *     { "name": "id", "type": "integer", "nullable": false },
 *     { "name": "sku", "type": "character varying", "nullable": false },
 *     { "name": "name", "type": "character varying", "nullable": true },
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
  const { db, table } = req.query;

  // Validate params
  if (!db || !table) {
    return res.status(400).json({
      success: false,
      error: 'Thiếu tham số',
      message: 'Cần cung cấp cả "db" và "table". Ví dụ: /api/db/columns?db=mirror_product&table=products'
    });
  }

  const pool = createPool(db);
  let client;

  try {
    client = await pool.connect();

    /**
     * Query lấy thông tin columns của table
     * - information_schema.columns: chứa metadata về columns
     * - Lấy: tên cột, kiểu dữ liệu, có cho phép null không
     */
    const query = `
      SELECT
        column_name as name,
        data_type as type,
        is_nullable = 'YES' as nullable,
        column_default as default_value,
        ordinal_position as position
      FROM information_schema.columns
      WHERE table_schema = 'public'
        AND table_name = $1
      ORDER BY ordinal_position
    `;

    const result = await client.query(query, [table]);

    // Kiểm tra table có tồn tại không
    if (result.rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: 'Table không tồn tại',
        message: `Không tìm thấy table: ${table} trong database: ${db}`
      });
    }

    return res.status(200).json({
      success: true,
      database: db,
      table: table,
      columns: result.rows
    });

  } catch (error) {
    console.error('Database error:', error);

    if (error.code === '3D000') {
      return res.status(404).json({
        success: false,
        error: 'Database không tồn tại',
        message: `Không tìm thấy database: ${db}`
      });
    }

    return res.status(500).json({
      success: false,
      error: 'Không thể kết nối database',
      message: error.message
    });
  } finally {
    if (client) client.release();
    await pool.end();
  }
}
