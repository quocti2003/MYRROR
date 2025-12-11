# Inventory Management - Frontend Documentation

## Tong quan

He thong quan ly kho hang cho cua hang trang suc. Cho phep nhan vien quan ly san pham, quet ma SKU, va tao du lieu in label.

## Cau truc thu muc

```
src/components/inventory/
├── InventoryLayout.jsx      # Layout chinh (sidebar + content)
├── InventoryLayout.css
├── InventorySidebar.jsx     # Navigation ben trai
├── InventorySidebar.css
├── Dashboard.jsx            # Trang tong quan
├── Dashboard.css
├── Scanner.jsx              # Trang quet ma SKU
├── Scanner.css
├── ProductForm.jsx          # Form them/sua san pham
├── ProductForm.css
├── ProductList.jsx          # Danh sach san pham
├── ProductList.css
├── ProductDetail.jsx        # Chi tiet san pham
├── ProductDetail.css
├── PrintLabel.jsx           # Tao du lieu in label
├── PrintLabel.css
└── index.js                 # Export all components
```

## Routes

| Path | Component | Mo ta |
|------|-----------|-------|
| `/inventory` | Dashboard | Redirect to dashboard |
| `/inventory/dashboard` | Dashboard | Trang tong quan |
| `/inventory/scanner` | Scanner | Quet ma SKU |
| `/inventory/add` | ProductForm | Them san pham moi |
| `/inventory/products` | ProductList | Danh sach san pham |
| `/inventory/products/:id` | ProductDetail | Chi tiet san pham |
| `/inventory/products/:id/edit` | ProductForm (isEdit=true) | Sua san pham |
| `/inventory/print` | PrintLabel | Tao du lieu in label |

## Components

### 1. InventoryLayout
- Layout wrapper cho tat ca trang inventory
- Bao gom sidebar va main content area
- Su dung React Router `<Outlet />` de render child routes

### 2. InventorySidebar
- Navigation menu ben trai
- Responsive - an di tren mobile, hien menu button
- Highlight nut "Quet ma" vi la chuc nang chinh

### 3. Dashboard
- Hien thi thong ke: tong so san pham, theo status, tong gia tri kho
- Nut truy cap nhanh: Quet ma, Them SP, Danh sach
- San pham moi them gan day (5 san pham)

### 4. Scanner
- O input text auto-focus de nhan input tu may quet
- May quet hoat dong nhu keyboard - nhap SKU va gui Enter
- Sau khi tim thay san pham, hien thi thong tin va cac nut hanh dong:
  - Xem chi tiet
  - Sua
  - Xoa (co confirm dialog)
  - Copy de in

### 5. ProductForm
- Dung chung cho them moi va sua san pham
- Prop `isEdit={true}` de su dung cho mode edit
- Form fields:
  - Thong tin co ban: name, sku, description, price, currency, status
  - Chat lieu: metal_type, metal_purity, stone_type, weight_grams, dimensions
  - Hinh anh: image_url
- Preview label o sidebar ben phai
- So sanh thay doi khi edit

### 6. ProductList
- Bang hien thi san pham voi cac cot: hinh, SKU, ten, kim loai, gia, trang thai
- Tim kiem theo ten hoac SKU
- Bo loc: status, khoang gia
- Phan trang (10 san pham/trang)
- Click vao row de xem chi tiet

### 7. ProductDetail
- Hien thi day du thong tin san pham
- Hinh anh lon ben trai
- Thong tin chi tiet ben phai
- Nut hanh dong: Sua, Xoa, Copy de in

### 8. PrintLabel
- Chon nhieu san pham tu danh sach
- Tuy chon thong tin tren label: SKU, ten, gia
- Xuat ra JSON hoac CSV
- Copy vao clipboard de dan vao phan mem in

## API Su dung

Tu file `src/services/api.js`:

```javascript
productsAPI.getAll()           // Lay danh sach san pham
productsAPI.getById(id)        // Lay chi tiet theo ID
productsAPI.getBySku(sku)      // Lay theo SKU (cho Scanner)
productsAPI.create(data)       // Tao san pham moi
productsAPI.update(id, data)   // Cap nhat san pham
productsAPI.delete(id)         // Xoa san pham (soft delete)
```

## Data Model

San pham (Product) theo database schema:

```javascript
{
  id: string,
  name: string,           // Ten san pham (bat buoc)
  sku: string,            // Ma SKU (bat buoc, unique)
  description: string,    // Mo ta
  price: number,          // Gia (bat buoc)
  currency: string,       // Don vi tien (VND/USD)
  metal_type: string,     // Loai kim loai
  metal_purity: string,   // Do tinh khiet (10K, 14K, 18K...)
  stone_type: string,     // Loai da
  weight_grams: number,   // Trong luong (gram)
  dimensions: string,     // Kich thuoc
  status: string,         // Trang thai (available/hold/warranty/sold)
  image_url: string,      // URL hinh anh
  is_active: boolean,
  is_deleted: boolean,
  created_at: timestamp,
  updated_at: timestamp
}
```

## Status San Pham

| Value | Label | Color | Mo ta |
|-------|-------|-------|-------|
| available | Con hang | #10b981 (green) | San pham san sang ban |
| hold | Dang giu | #f59e0b (yellow) | Khach dang giu/dat coc |
| warranty | Bao hanh | #ef4444 (red) | Dang bao hanh |
| sold | Da ban | #6b7280 (gray) | Da ban |

## Huong dan su dung

### Quet ma san pham
1. Vao trang Scanner (`/inventory/scanner`)
2. Dung may quet de quet ma QR/barcode tren san pham
3. May quet se nhap SKU vao o input va tu dong tim kiem
4. Hoac nhap thu cong SKU va nhan Enter

### Them san pham moi
1. Vao trang Them san pham (`/inventory/add`)
2. Dien cac thong tin bat buoc: ten, SKU, gia
3. Dien cac thong tin khac neu can
4. Nhan "Luu san pham"
5. Copy du lieu de in label neu can

### In label
1. Vao trang In label (`/inventory/print`)
2. Chon cac san pham can in
3. Chon thong tin muon hien thi tren label
4. Chon dinh dang (JSON hoac CSV)
5. Click "Copy du lieu"
6. Dan vao phan mem in label cua ban

## Luu y

- Frontend su dung API endpoint `/api/products/sku/:sku` de tim kiem theo SKU
- Xoa san pham la soft delete (set is_deleted = true)
- May quet hoat dong nhu keyboard, nen o input can auto-focus
- Responsive design - hoat dong tren mobile va desktop
