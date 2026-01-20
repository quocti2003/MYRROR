import React, { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import "@pages/servicesDetail.css";
import ProductCareRepair from "./ProductCareRepair";
import TradeInUpgrade from "./TradeInUpgrade";
import ContactUs from "@components/contactUs/ContactUs";
import { ROUTES } from "@/constants/routes";
import { getImageUrl } from "@utils/cloudflareMediaUtil";

const ServicesDetailPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("product-care-repair");

  useEffect(() => {
    const tabFromUrl = searchParams.get('tab');
    if (tabFromUrl && (tabFromUrl === 'product-care-repair' || tabFromUrl === 'trade-in-upgrade')) {
      setActiveTab(tabFromUrl);
    }
  }, [searchParams]);

  const handleTabClick = (tabId) => {
    setActiveTab(tabId);
    navigate(`${ROUTES.SERVICES_DETAIL}?tab=${tabId}`, { replace: true });
  };

  const tabs = [
    {
      id: "product-care-repair",
      label: "Product Care & Repair",
      component: ProductCareRepair,
    },
    {
      id: "trade-in-upgrade", 
      label: "Trade-In & Upgrade",
      component: TradeInUpgrade,
    },
  ];

  const getHeroImage = () => {
    switch (activeTab) {
      case "product-care-repair":
        return getImageUrl("services/Product care & Repair_2.webp");
      case "trade-in-upgrade":
        return getImageUrl("services/Trade in_1920x600.webp");
      default:
        return getImageUrl("services/service_detail_img.webp");
    }
  };

  const getHeroTheme = () => {
    switch (activeTab) {
      case "trade-in-upgrade":
      case "product-care-repair":
        return "white";
      default:
        return "blend";
    }
  };

  return (
    <>
      <div
        className="services-detail-hero-section"
        data-navbar-theme={getHeroTheme()}
        style={{ backgroundImage: `url("${getHeroImage()}")` }}
      >
      </div>

      <div className="services-detail-wrapper" data-navbar-theme="blend">
        <div className="services-detail-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={`services-detail-tab ${activeTab === tab.id ? "active" : ""}`}
              onClick={() => handleTabClick(tab.id)}
            >
              <span className="bodytext-6--no-margin">{tab.label}</span>
            </button>
          ))}
        </div>
        <div className="services-detail-container">
          <div className="services-detail-content">
            {tabs.map((tab) => (
              <div
                key={tab.id}
                className={`tab-content ${
                  activeTab === tab.id ? "active" : ""
                }`}
              >
                {activeTab === tab.id && <tab.component />}
              </div>
            ))}
          </div>
        </div>
      </div>

      <ContactUs />
    </>
  );
};

export default ServicesDetailPage;