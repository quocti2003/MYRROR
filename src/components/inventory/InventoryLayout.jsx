import React from "react";
import { Outlet } from "react-router-dom";
import InventorySidebar from "./InventorySidebar";
import "./InventoryLayout.css";

const InventoryLayout = () => {
  return (
    <div className="inventory-layout">
      <InventorySidebar />
      <main className="inventory-main">
        <Outlet />
      </main>
    </div>
  );
};

export default InventoryLayout;
