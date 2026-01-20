/**
 * EventAdminPage - Admin control panel for event management
 */
import React, { useEffect, useState } from 'react';
import ExcelJS from 'exceljs';
import { fetchUserStats, fetchShapeStats, fetchAllUsers } from '@services/event/eventApi';
import { broadcastStatsUpdate } from '@services/event/ably';
import { ADMIN_PASSWORD } from '@/constants/eventConstants';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import Logo from '@components/event/ui/Logo';

import '@styles/event.css';

// Shape display names (supports both legacy names and h1-h7 IDs)
const SHAPE_NAMES = {
  // Legacy names
  heart: 'Heart Shape',
  oval: 'Oval Shape',
  round: 'Round Shape',
  pear: 'Pear Shape',
  asscher: 'Asscher Shape',
  emerald: 'Emerald Shape',
  marquise: 'Marquise Shape',
  // New IDs (h1-h7)
  h1: 'Heart Shape',
  h2: 'Oval Shape',
  h3: 'Round Shape',
  h4: 'Pear Shape',
  h5: 'Asscher Shape',
  h6: 'Emerald Shape',
  h7: 'Marquise Shape',
};

// Shape images mapping (supports both legacy names and h1-h7 IDs)
const SHAPE_IMAGES = {
  // Legacy names
  heart: 'mirror_DMM/HEART-01.webp',
  oval: 'mirror_DMM/HEART-02.webp',
  round: 'mirror_DMM/HEART-03.webp',
  pear: 'mirror_DMM/HEART-04.webp',
  asscher: 'mirror_DMM/HEART-05.webp',
  emerald: 'mirror_DMM/HEART-06.webp',
  marquise: 'mirror_DMM/HEART-07.webp',
  // New IDs (h1-h7)
  h1: 'mirror_DMM/HEART-01.webp',
  h2: 'mirror_DMM/HEART-02.webp',
  h3: 'mirror_DMM/HEART-03.webp',
  h4: 'mirror_DMM/HEART-04.webp',
  h5: 'mirror_DMM/HEART-05.webp',
  h6: 'mirror_DMM/HEART-06.webp',
  h7: 'mirror_DMM/HEART-07.webp',
};

const EventAdminPage = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [password, setPassword] = useState('');
  const [stats, setStats] = useState(null);
  const [shapeStats, setShapeStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [isExporting, setIsExporting] = useState(false);

  // Load data on auth
  useEffect(() => {
    if (isAuthenticated) {
      loadData();
      // Refresh every 30 seconds
      const interval = setInterval(loadData, 30000);
      return () => clearInterval(interval);
    }
  }, [isAuthenticated]);

  const loadData = async () => {
    const [statsData, shapeData, usersData] = await Promise.all([
      fetchUserStats(),
      fetchShapeStats(),
      fetchAllUsers(),
    ]);
    setStats(statsData);
    setShapeStats(shapeData);
    setUsers(usersData?.users || []);

    // Broadcast stats update
    if (statsData) {
      broadcastStatsUpdate({
        totalUsers: statsData.totalUsers,
      });
    }
  };

  const handleLogin = (e) => {
    e.preventDefault();
    if (password === ADMIN_PASSWORD) {
      setIsAuthenticated(true);
    } else {
      alert('Mật khẩu không đúng');
    }
  };

  const exportToExcel = async () => {
    if (!stats || !shapeStats) {
      alert('Chưa có dữ liệu để xuất');
      return;
    }

    setIsExporting(true);

    try {
      // Fetch all users for export
      const usersData = await fetchAllUsers();

      // Create workbook
      const workbook = new ExcelJS.Workbook();
      workbook.creator = 'DMM Event Admin';
      workbook.created = new Date();

      // Style constants
      const headerFill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'FF4472C4' },
      };
      const headerFont = {
        bold: true,
        color: { argb: 'FFFFFFFF' },
        size: 12,
      };
      const titleFont = {
        bold: true,
        size: 14,
        color: { argb: 'FF4472C4' },
      };
      const borderStyle = {
        top: { style: 'thin', color: { argb: 'FF000000' } },
        left: { style: 'thin', color: { argb: 'FF000000' } },
        bottom: { style: 'thin', color: { argb: 'FF000000' } },
        right: { style: 'thin', color: { argb: 'FF000000' } },
      };

      // ========== Sheet 1: User Statistics ==========
      const wsUserStats = workbook.addWorksheet('User Stats');

      // Title
      wsUserStats.mergeCells('A1:B1');
      wsUserStats.getCell('A1').value = 'THỐNG KÊ NGƯỜI DÙNG';
      wsUserStats.getCell('A1').font = titleFont;
      wsUserStats.getCell('A1').alignment = { horizontal: 'center' };

      // Headers
      wsUserStats.getCell('A3').value = 'STT';
      wsUserStats.getCell('B3').value = 'Loại';
      wsUserStats.getCell('C3').value = 'Số lượng';
      ['A3', 'B3', 'C3'].forEach((cell) => {
        wsUserStats.getCell(cell).fill = headerFill;
        wsUserStats.getCell(cell).font = headerFont;
        wsUserStats.getCell(cell).border = borderStyle;
        wsUserStats.getCell(cell).alignment = { horizontal: 'center' };
      });

      // Data
      const userStatsRows = [
        ['Tổng người truy cập', stats.totalUsers],
        ['Đăng nhập Google', stats.googleUsers],
        ['Đăng nhập Facebook', stats.facebookUsers],
        ['Đăng nhập Apple', stats.appleUsers],
      ];
      userStatsRows.forEach((row, index) => {
        const rowNum = index + 4;
        wsUserStats.getCell(`A${rowNum}`).value = index + 1;
        wsUserStats.getCell(`B${rowNum}`).value = row[0];
        wsUserStats.getCell(`C${rowNum}`).value = row[1];
        ['A', 'B', 'C'].forEach((col) => {
          wsUserStats.getCell(`${col}${rowNum}`).border = borderStyle;
          wsUserStats.getCell(`${col}${rowNum}`).alignment = { horizontal: 'center' };
        });
      });

      wsUserStats.columns = [
        { width: 8 },
        { width: 25 },
        { width: 15 },
      ];

      // ========== Sheet 2: Shape Statistics ==========
      const wsShapes = workbook.addWorksheet('Shape Stats');

      // Title
      wsShapes.mergeCells('A1:C1');
      wsShapes.getCell('A1').value = 'THỐNG KÊ LƯỢT CHỌN SHAPE';
      wsShapes.getCell('A1').font = titleFont;
      wsShapes.getCell('A1').alignment = { horizontal: 'center' };

      // Headers
      wsShapes.getCell('A3').value = 'STT';
      wsShapes.getCell('B3').value = 'Shape';
      wsShapes.getCell('C3').value = 'Số lượt chọn';
      ['A3', 'B3', 'C3'].forEach((cell) => {
        wsShapes.getCell(cell).fill = headerFill;
        wsShapes.getCell(cell).font = headerFont;
        wsShapes.getCell(cell).border = borderStyle;
        wsShapes.getCell(cell).alignment = { horizontal: 'center' };
      });

      // Data
      const shapeEntries = Object.entries(shapeStats.shapes);
      shapeEntries.forEach(([shape, count], index) => {
        const rowNum = index + 4;
        wsShapes.getCell(`A${rowNum}`).value = index + 1;
        wsShapes.getCell(`B${rowNum}`).value = SHAPE_NAMES[shape] || shape;
        wsShapes.getCell(`C${rowNum}`).value = count;
        ['A', 'B', 'C'].forEach((col) => {
          wsShapes.getCell(`${col}${rowNum}`).border = borderStyle;
          wsShapes.getCell(`${col}${rowNum}`).alignment = { horizontal: 'center' };
        });
      });

      // Total row
      const totalRowNum = shapeEntries.length + 4;
      wsShapes.getCell(`A${totalRowNum}`).value = '';
      wsShapes.getCell(`B${totalRowNum}`).value = 'TỔNG CỘNG';
      wsShapes.getCell(`C${totalRowNum}`).value = shapeStats.totalSelections;
      ['A', 'B', 'C'].forEach((col) => {
        wsShapes.getCell(`${col}${totalRowNum}`).border = borderStyle;
        wsShapes.getCell(`${col}${totalRowNum}`).font = { bold: true };
        wsShapes.getCell(`${col}${totalRowNum}`).fill = {
          type: 'pattern',
          pattern: 'solid',
          fgColor: { argb: 'FFFFF2CC' },
        };
        wsShapes.getCell(`${col}${totalRowNum}`).alignment = { horizontal: 'center' };
      });

      wsShapes.columns = [
        { width: 8 },
        { width: 20 },
        { width: 15 },
      ];

      // ========== Sheet 3: User Details ==========
      const wsUserDetails = workbook.addWorksheet('User Details');
      const users = usersData?.users || [];

      // Title
      wsUserDetails.mergeCells('A1:E1');
      wsUserDetails.getCell('A1').value = 'DANH SÁCH NGƯỜI DÙNG';
      wsUserDetails.getCell('A1').font = titleFont;
      wsUserDetails.getCell('A1').alignment = { horizontal: 'center' };

      // Headers
      const userHeaders = ['STT', 'Email', 'Phương thức đăng nhập', 'Tên', 'Shape đã chọn'];
      userHeaders.forEach((header, index) => {
        const col = String.fromCharCode(65 + index);
        wsUserDetails.getCell(`${col}3`).value = header;
        wsUserDetails.getCell(`${col}3`).fill = headerFill;
        wsUserDetails.getCell(`${col}3`).font = headerFont;
        wsUserDetails.getCell(`${col}3`).border = borderStyle;
        wsUserDetails.getCell(`${col}3`).alignment = { horizontal: 'center' };
      });

      // Data
      users.forEach((user, index) => {
        const rowNum = index + 4;
        wsUserDetails.getCell(`A${rowNum}`).value = index + 1;
        wsUserDetails.getCell(`B${rowNum}`).value = user.email;
        wsUserDetails.getCell(`C${rowNum}`).value = user.provider;
        wsUserDetails.getCell(`D${rowNum}`).value = user.displayName;
        wsUserDetails.getCell(`E${rowNum}`).value = SHAPE_NAMES[user.shape] || user.shape || 'Chưa chọn';

        ['A', 'B', 'C', 'D', 'E'].forEach((col) => {
          wsUserDetails.getCell(`${col}${rowNum}`).border = borderStyle;
          if (col === 'A') {
            wsUserDetails.getCell(`${col}${rowNum}`).alignment = { horizontal: 'center' };
          }
        });

        // Alternate row color
        if (index % 2 === 1) {
          ['A', 'B', 'C', 'D', 'E'].forEach((col) => {
            wsUserDetails.getCell(`${col}${rowNum}`).fill = {
              type: 'pattern',
              pattern: 'solid',
              fgColor: { argb: 'FFF2F2F2' },
            };
          });
        }
      });

      wsUserDetails.columns = [
        { width: 8 },
        { width: 35 },
        { width: 22 },
        { width: 25 },
        { width: 18 },
      ];

      // Generate filename with timestamp
      const now = new Date();
      const timestamp = now.toISOString().slice(0, 10).replace(/-/g, '');
      const filename = `DMM_Event_Stats_${timestamp}.xlsx`;

      // Download file
      const buffer = await workbook.xlsx.writeBuffer();
      const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Export error:', error);
      alert('Lỗi khi xuất file Excel');
    } finally {
      setIsExporting(false);
    }
  };

  // Login screen
  if (!isAuthenticated) {
    return (
      <div className="event-admin event-admin--login">
        <Logo size="md" />
        <form onSubmit={handleLogin} className="admin-login-form">
          <h2>Admin Login</h2>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Nhập mật khẩu"
            className="event-input"
            autoFocus
          />
          <button type="submit" className="event-btn event-btn--primary">
            Đăng nhập
          </button>
        </form>
      </div>
    );
  }

  return (
    <div className="event-admin">
      <div className="admin-header">
        <Logo size="sm" />
        <h1>Event Admin</h1>
        <button
          className="event-btn event-btn--text"
          onClick={() => setIsAuthenticated(false)}
        >
          Đăng xuất
        </button>
      </div>

      <div className="admin-content">
        {/* User Stats Section */}
        <section className="admin-section">
          <h2>Thống kê người dùng</h2>
          {stats ? (
            <div className="admin-stats">
              <div className="event-stat-card">
                <span className="event-stat-value">{stats.totalUsers}</span>
                <span className="event-stat-label">Tổng người truy cập</span>
              </div>
              <div className="event-stat-card">
                <span className="event-stat-value">{stats.googleUsers}</span>
                <span className="event-stat-label">Đăng nhập Google</span>
              </div>
              <div className="event-stat-card">
                <span className="event-stat-value">{stats.facebookUsers}</span>
                <span className="event-stat-label">Đăng nhập Facebook</span>
              </div>
              <div className="event-stat-card">
                <span className="event-stat-value">{stats.appleUsers}</span>
                <span className="event-stat-label">Đăng nhập Apple</span>
              </div>
            </div>
          ) : (
            <p>Đang tải...</p>
          )}
        </section>

        {/* Shape Stats Section */}
        <section className="admin-section">
          <h2>Thống kê lượt chọn Shape</h2>
          {shapeStats ? (
            <>
              <div className="admin-stats">
                <div className="event-stat-card">
                  <span className="event-stat-value">{shapeStats.totalSelections}</span>
                  <span className="event-stat-label">Tổng lượt chọn</span>
                </div>
              </div>
              <div className="admin-stats shape-stats">
                {Object.entries(shapeStats.shapes).map(([shape, count]) => (
                  <div key={shape} className="event-stat-card shape-stat-card">
                    {SHAPE_IMAGES[shape] && (
                      <img
                        src={getMediaUrl(SHAPE_IMAGES[shape])}
                        alt={SHAPE_NAMES[shape] || shape}
                        className="admin-shape-img"
                      />
                    )}
                    <span className="event-stat-value">{count}</span>
                    <span className="event-stat-label">
                      {SHAPE_NAMES[shape] || shape}
                    </span>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <p>Đang tải...</p>
          )}
        </section>

        {/* User Details Section */}
        <section className="admin-section">
          <h2>Danh sách người dùng ({users.length})</h2>
          {users.length > 0 ? (
            <div className="admin-table-container">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>STT</th>
                    <th>Email</th>
                    <th>Phương thức</th>
                    <th>Tên</th>
                    <th>Shape đã chọn</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user, index) => (
                    <tr key={index}>
                      <td>{index + 1}</td>
                      <td>{user.email}</td>
                      <td>
                        <span className={`provider-badge provider-${user.provider.toLowerCase()}`}>
                          {user.provider || '-'}
                        </span>
                      </td>
                      <td>{user.displayName || '-'}</td>
                      <td>
                        {user.shape ? (
                          <div className="shape-cell">
                            {SHAPE_IMAGES[user.shape] && (
                              <img
                                src={getMediaUrl(SHAPE_IMAGES[user.shape])}
                                alt={SHAPE_NAMES[user.shape] || user.shape}
                                className="admin-shape-img-small"
                              />
                            )}
                            <span>{SHAPE_NAMES[user.shape] || user.shape}</span>
                          </div>
                        ) : (
                          <span className="no-shape">Chưa chọn</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p>Không có dữ liệu người dùng</p>
          )}
        </section>

        <div className="admin-actions">
          <button className="event-btn event-btn--secondary" onClick={loadData}>
            Làm mới
          </button>
          <button
            className="event-btn event-btn--primary"
            onClick={exportToExcel}
            disabled={isExporting}
          >
            {isExporting ? 'Đang xuất...' : 'Xuất Excel'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default EventAdminPage;
