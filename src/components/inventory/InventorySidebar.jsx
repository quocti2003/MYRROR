import React, { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { ROUTES } from "@/constants/routes";
import {
  LayoutDashboard,
  ScanLine,
  Plus,
  Package,
  Printer,
  Menu,
  X,
  Gem,
  ShoppingCart,
} from "lucide-react";
import "./InventorySidebar.css";

const InventorySidebar = () => {
  const [isOpen, setIsOpen] = useState(false);
  const location = useLocation();

  const menuItems = [
    {
      path: ROUTES.INVENTORY_DASHBOARD,
      icon: LayoutDashboard,
      label: "Trang chu",
    },
    {
      path: ROUTES.INVENTORY_CREATE_ORDER,
      icon: ShoppingCart,
      label: "Tao don hang",
      highlight: true,
    },
    {
      path: ROUTES.INVENTORY_SCANNER,
      icon: ScanLine,
      label: "Quet ma",
    },
    {
      path: ROUTES.INVENTORY_ADD_PRODUCT,
      icon: Plus,
      label: "Them san pham",
    },
    {
      path: ROUTES.INVENTORY_PRODUCTS,
      icon: Package,
      label: "Danh sach",
    },
    {
      path: ROUTES.INVENTORY_PRINT,
      icon: Printer,
      label: "In label",
    },
  ];

  const toggleSidebar = () => setIsOpen(!isOpen);

  return (
    <>
      {/* Mobile toggle button */}
      <button className="inventory-sidebar-toggle" onClick={toggleSidebar}>
        {isOpen ? <X size={24} /> : <Menu size={24} />}
      </button>

      {/* Overlay for mobile */}
      {isOpen && (
        <div className="inventory-sidebar-overlay" onClick={toggleSidebar} />
      )}

      <aside className={`inventory-sidebar ${isOpen ? "open" : ""}`}>
        <div className="inventory-sidebar-header">
          <Gem size={28} />
          <span>Quan ly kho</span>
        </div>

        <nav className="inventory-sidebar-nav">
          {menuItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `inventory-sidebar-link ${isActive ? "active" : ""} ${
                  item.highlight ? "highlight" : ""
                }`
              }
              onClick={() => setIsOpen(false)}
            >
              <item.icon size={20} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="inventory-sidebar-footer">
          <p>Mirror Diamond</p>
        </div>
      </aside>
    </>
  );
};

export default InventorySidebar;
