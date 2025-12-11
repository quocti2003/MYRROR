# Inventory Management - Backend Implementation Guide

## Tong quan

Tai lieu nay huong dan backend developer implement cac API endpoint can thiet cho he thong quan ly kho hang.

## Database Schema

Frontend su dung bang `products` voi cac column sau:

```sql
CREATE TABLE products (
  id VARCHAR PRIMARY KEY,
  name VARCHAR NOT NULL,
  description TEXT,
  sku_id VARCHAR NOT NULL,
  sku VARCHAR UNIQUE,
  price NUMERIC NOT NULL,
  currency VARCHAR NOT NULL DEFAULT 'VND',
  metal_type VARCHAR,
  metal_purity VARCHAR,
  stone_type VARCHAR,
  weight_grams NUMERIC,
  dimensions TEXT,
  image_url VARCHAR,
  image_urls TEXT,
  tags TEXT,
  status VARCHAR NOT NULL DEFAULT 'available',
  featured BOOLEAN DEFAULT false,
  stock_quantity INTEGER,
  min_stock_level INTEGER,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP,
  is_deleted BOOLEAN NOT NULL DEFAULT false,
  is_active BOOLEAN NOT NULL DEFAULT true,
  design_id VARCHAR,
  designer_id BIGINT,
  published_at TIMESTAMP,
  fulfillment_completed_at TIMESTAMP
);

-- Index cho tim kiem theo SKU
CREATE UNIQUE INDEX idx_products_sku ON products(sku) WHERE sku IS NOT NULL;
```

## API Endpoints Can Thiet

### 1. GET /api/products

**Mo ta:** Lay danh sach tat ca san pham (is_deleted = false)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "PRD001",
      "name": "Nhan kim cuong",
      "sku": "SKU001",
      "price": 15000000,
      "currency": "VND",
      "metal_type": "Gold",
      "metal_purity": "18K",
      "stone_type": "Diamond",
      "weight_grams": 5.2,
      "status": "available",
      "image_url": "https://...",
      "created_at": "2024-12-10T10:00:00Z"
    }
  ]
}
```

### 2. GET /api/products/:id

**Mo ta:** Lay chi tiet san pham theo ID

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "PRD001",
    "name": "Nhan kim cuong",
    "description": "Nhan kim cuong cao cap",
    "sku": "SKU001",
    "price": 15000000,
    "currency": "VND",
    "metal_type": "Gold",
    "metal_purity": "18K",
    "stone_type": "Diamond",
    "weight_grams": 5.2,
    "dimensions": "Size 6",
    "status": "available",
    "image_url": "https://...",
    "is_active": true,
    "created_at": "2024-12-10T10:00:00Z",
    "updated_at": "2024-12-10T12:00:00Z"
  }
}
```

### 3. GET /api/products/sku/:sku ⭐ QUAN TRONG

**Mo ta:** Tim san pham theo ma SKU. Endpoint nay duoc su dung boi Scanner.

**Parameters:**
- `sku` (path): Ma SKU can tim

**Response thanh cong:**
```json
{
  "success": true,
  "data": {
    "id": "PRD001",
    "name": "Nhan kim cuong",
    "sku": "SKU001",
    "price": 15000000,
    ...
  }
}
```

**Response khong tim thay (404):**
```json
{
  "success": false,
  "message": "Product not found"
}
```

### 4. POST /api/products

**Mo ta:** Tao san pham moi

**Request Body:**
```json
{
  "name": "Nhan kim cuong",
  "sku": "SKU001",
  "description": "Mo ta san pham",
  "price": 15000000,
  "currency": "VND",
  "metal_type": "Gold",
  "metal_purity": "18K",
  "stone_type": "Diamond",
  "weight_grams": 5.2,
  "dimensions": "Size 6",
  "status": "available",
  "image_url": "https://..."
}
```

**Validation:**
- `name`: bat buoc, khong rong
- `sku`: bat buoc, unique
- `price`: bat buoc, > 0

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "PRD001",
    ...
  },
  "message": "Product created successfully"
}
```

### 5. PUT /api/products/:id

**Mo ta:** Cap nhat san pham

**Request Body:** Giong POST, chi gui cac field can cap nhat

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "PRD001",
    ...
  },
  "message": "Product updated successfully"
}
```

### 6. DELETE /api/products/:id

**Mo ta:** Xoa san pham (soft delete)

**Logic:**
```sql
UPDATE products SET is_deleted = true, updated_at = NOW() WHERE id = :id
```

**Response:**
```json
{
  "success": true,
  "message": "Product deleted successfully"
}
```

## Status Values

Frontend su dung cac status sau:

| Value | Mo ta |
|-------|-------|
| `available` | San pham san sang ban |
| `hold` | Khach dang giu/dat coc |
| `warranty` | Dang bao hanh |
| `sold` | Da ban |

## Error Handling

**Format loi chuan:**
```json
{
  "success": false,
  "message": "Error message",
  "errors": [
    {
      "field": "sku",
      "message": "SKU already exists"
    }
  ]
}
```

**HTTP Status Codes:**
- `200` - Thanh cong
- `201` - Tao thanh cong
- `400` - Bad request / Validation error
- `404` - Khong tim thay
- `500` - Server error

## Notes cho Backend Developer

### 1. Tim kiem theo SKU
- Endpoint `/api/products/sku/:sku` rat quan trong cho chuc nang Scanner
- Can dam bao response nhanh (< 200ms)
- Tra ve 404 neu khong tim thay, khong tra ve mang rong

### 2. Soft Delete
- Luon su dung soft delete (set is_deleted = true)
- Cac query GET phai filter `WHERE is_deleted = false`

### 3. Timestamps
- Tu dong set `created_at` khi tao moi
- Tu dong set `updated_at` khi cap nhat

### 4. SKU Unique
- Dam bao SKU la unique trong he thong
- Tra ve loi validation neu trung

### 5. Response Format
- Frontend mong doi response co dang `{ success: true, data: {...} }`
- Hoac `{ success: true, data: [...] }` cho list

### 6. Currency
- Mac dinh la "VND"
- Ho tro "USD" neu can

## Testing Checklist

Backend can test cac case sau:

- [ ] GET /api/products - tra ve danh sach san pham
- [ ] GET /api/products/:id - tra ve chi tiet san pham
- [ ] GET /api/products/:id - tra ve 404 neu khong ton tai
- [ ] GET /api/products/sku/:sku - tim thay san pham
- [ ] GET /api/products/sku/:sku - tra ve 404 neu khong tim thay
- [ ] POST /api/products - tao san pham thanh cong
- [ ] POST /api/products - tra ve loi neu thieu field bat buoc
- [ ] POST /api/products - tra ve loi neu SKU da ton tai
- [ ] PUT /api/products/:id - cap nhat thanh cong
- [ ] PUT /api/products/:id - tra ve 404 neu khong ton tai
- [ ] DELETE /api/products/:id - soft delete thanh cong
- [ ] DELETE /api/products/:id - san pham da xoa khong hien trong GET list

## Gia lap API cho Dev Frontend

Neu chua co backend, frontend developer co the su dung mock data:

```javascript
// Mock products
const mockProducts = [
  {
    id: "PRD001",
    name: "Nhan kim cuong",
    sku: "SKU001",
    price: 15000000,
    currency: "VND",
    metal_type: "Gold",
    metal_purity: "18K",
    stone_type: "Diamond",
    status: "available",
  },
  // ... more products
];
```

---

Lien he: Tao issue tren GitHub neu co cau hoi ve API.
