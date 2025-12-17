import React, { useState, useEffect, useRef } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import "@pages/support.css";
import ReturnExchange from "./ReturnExchange";
import SizingGuide from "./SizingGuide";
import WarrantyInfo from "./WarrantyInfo";
import FAQs from "./FAQs";
import ContactUs from "@components/contactUs/ContactUs";
import { ROUTES } from "@/constants/routes";

const SupportDetail = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("return-exchange");
  const tabsRef = useRef(null);

  useEffect(() => {
    const tabFromUrl = searchParams.get('tab');
    const validTabs = ['return-exchange', 'sizing-guide', 'warranty-info', 'faqs'];
    if (tabFromUrl && validTabs.includes(tabFromUrl)) {
      setActiveTab(tabFromUrl);
    }
  }, [searchParams]);

  const tabs = [
    {
      id: "return-exchange",
      label: "Return & Exchange",
      component: ReturnExchange,
    },
    { id: "sizing-guide", label: "Sizing Guide", component: SizingGuide },
    {
      id: "warranty-info",
      label: "Warranty Information",
      component: WarrantyInfo,
    },
    { id: "faqs", label: "FAQs", component: FAQs },
  ];

  const handleTabClick = (tabId) => {
    setActiveTab(tabId);
    navigate(`${ROUTES.SUPPORT_DETAIL}?tab=${tabId}`, { replace: true });
  };

  const handleScrollRight = () => {
    if (tabsRef.current) {
      const scrollAmount = tabsRef.current.clientWidth * 0.7; // Scroll 70% của width
      tabsRef.current.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }
  };

  return (
    <>
      <div
        className={`support-hero-section support-hero-section--image support-hero-section--${activeTab}`}
        data-navbar-theme="white"
      >
        <div className="hero-content">
          <h1>Support</h1>
        </div>
      </div>

      <div className="support-wrapper" data-navbar-theme="black">
        <div className="support-tabs-container">
          <div className="support-tabs" ref={tabsRef}>
            {tabs.map((tab) => (
              <button
                key={tab.id}
                className={`support-tab ${activeTab === tab.id ? "active" : ""}`}
                onClick={() => handleTabClick(tab.id)}
              >
                <span className="bodytext-6--no-margin">{tab.label}</span>
              </button>
            ))}
          </div>
          <button className="tabs-scroll-arrow" onClick={handleScrollRight}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M6 12L10 8L6 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </button>
        </div>
        <div className="support-container">
          <div className="support-content">
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
      
      {activeTab !== "faqs" && <ContactUs />}
    </>
  );
};

export default SupportDetail;
