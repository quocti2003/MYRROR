/**
 * =============================================================================
 * API: /api/db/tables
 * =============================================================================
 *
 * MỤC ĐÍCH:
 * - Liệt kê tất cả tables trong database đã chọn
 * - Dùng cho dropdown "Chọn Table" trong trang DB Explorer
 *
 * CÁCH GỌI:
 * - GET https://test-beryl-five-56.vercel.app/api/db/tables?db=mirror_product
 *
 * PARAMS:
 * - db: tên database cần lấy danh sách tables
 *
 * RESPONSE:
 * {
 *   "success": true,
 *   "database": "mirror_product",
 *   "tables": ["products", "categories", "orders", ...]
 * }
 *
 * =============================================================================
 */

import pg from 'pg';

const { Pool } = pg;

/**
 * Tạo connection pool động
 * - Hàm này tạo pool mới với database được chỉ định
 * - Mỗi database cần 1 pool riêng vì PostgreSQL không cho phép đổi DB sau khi connect
 */
function createPool(database) {
  return new Pool({
    host: process.env.DB_HOST || 'mirror-user-db.ch0a2gs8awvf.ap-southeast-1.rds.amazonaws.com',
    port: parseInt(process.env.DB_PORT || '5432'),
    user: process.env.DB_USER || 'mirror_user',
    password: process.env.DB_PASSWORD || '2v~]Tob:<epZzN3e:lF5zAGWbnz3',
    database: database, // Database được chọn từ query param
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

  // Lấy tên database từ query params
  const { db } = req.query;

  // Validate: bắt buộc phải có tên database
  if (!db) {
    return res.status(400).json({
      success: false,
      error: 'Thiếu tham số "db"',
      message: 'Vui lòng cung cấp tên database. Ví dụ: /api/db/tables?db=mirror_product'
    });
  }

  // Tạo pool kết nối đến database được chọn
  const pool = createPool(db);
  let client;

  try {
    client = await pool.connect();

    /**
     * Query liệt kê tất cả tables trong schema "public"
     * - information_schema.tables: chứa metadata về tables
     * - table_schema = 'public': chỉ lấy tables trong schema public (không lấy system tables)
     * - table_type = 'BASE TABLE': chỉ lấy tables thật, không lấy views
     */
    const query = `
      SELECT table_name as name
      FROM information_schema.tables
      WHERE table_schema = 'public'
        AND table_type = 'BASE TABLE'
      ORDER BY table_name
    `;

    const result = await client.query(query);

    return res.status(200).json({
      success: true,
      database: db,
      tables: result.rows.map(row => row.name)
    });

  } catch (error) {
    console.error('Database error:', error);

    // Xử lý lỗi database không tồn tại
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
    // Đóng pool sau khi dùng xong (vì mỗi request tạo pool mới)
    await pool.end();
  }
}
