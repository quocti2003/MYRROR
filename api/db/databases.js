/**
 * =============================================================================
 * API: /api/db/databases
 * =============================================================================
 *
 * MỤC ĐÍCH:
 * - Liệt kê tất cả databases có trong PostgreSQL server
 * - Dùng cho dropdown "Chọn Database" trong trang DB Explorer
 *
 * CÁCH GỌI:
 * - GET https://test-beryl-five-56.vercel.app/api/db/databases
 *
 * RESPONSE:
 * {
 *   "success": true,
 *   "databases": ["mirror_product", "mirror_user", "postgres", ...]
 * }
 *
 * =============================================================================
 */

import pg from 'pg';

const { Pool } = pg;

/**
 * Tạo connection pool đến PostgreSQL
 * - Kết nối vào database mặc định "postgres" để liệt kê tất cả databases
 * - Thông tin kết nối lấy từ Vercel Environment Variables
 */
const pool = new Pool({
  host: process.env.DB_HOST || 'mirror-user-db.ch0a2gs8awvf.ap-southeast-1.rds.amazonaws.com',
  port: parseInt(process.env.DB_PORT || '5432'),
  user: process.env.DB_USER || 'mirror_user',
  password: process.env.DB_PASSWORD || '2v~]Tob:<epZzN3e:lF5zAGWbnz3',
  database: 'postgres', // Kết nối vào DB mặc định để liệt kê các DB khác
  ssl: { rejectUnauthorized: false },
  max: 5,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000,
});

export default async function handler(req, res) {
  // Cấu hình CORS - cho phép gọi API từ bất kỳ domain nào
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  // Xử lý preflight request
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  // Chỉ cho phép GET
  if (req.method !== 'GET') {
    return res.status(405).json({
      success: false,
      error: 'Chỉ hỗ trợ GET request'
    });
  }

  let client;
  try {
    // Lấy connection từ pool
    client = await pool.connect();

    /**
     * Query liệt kê tất cả databases
     * - pg_database: system catalog chứa thông tin về databases
     * - datistemplate = false: loại bỏ template databases
     * - datname != 'rdsadmin': loại bỏ database admin của AWS RDS
     */
    const query = `
      SELECT datname as name
      FROM pg_database
      WHERE datistemplate = false
        AND datname != 'rdsadmin'
      ORDER BY datname
    `;

    const result = await client.query(query);

    // Trả về danh sách databases
    return res.status(200).json({
      success: true,
      databases: result.rows.map(row => row.name)
    });

  } catch (error) {
    console.error('Database error:', error);
    return res.status(500).json({
      success: false,
      error: 'Không thể kết nối database',
      message: error.message
    });
  } finally {
    // Trả connection về pool
    if (client) client.release();
  }
}
