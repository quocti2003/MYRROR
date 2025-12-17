import React, { useState, useEffect, useRef } from "react";
import "./FAQs.css";

const FAQs = () => {
  const [expandedFAQ, setExpandedFAQ] = useState(null);
  const [activeSection, setActiveSection] = useState("products");
  const [searchQuery, setSearchQuery] = useState("");

  // Dữ liệu FAQs cho từng section
  const faqData = {
    products: [
      {
        question: "What makes Mirror jewelry different?",
        answer:
          "Each Mirror creation is the result of advanced innovation and intentional design. Engineered through science and crafted for eternity, every piece blends modern technology with timeless artistry.",
      },
      {
        question: "Are Mirror diamonds real?",
        answer:
          "Mirror Future Diamonds are lab-created using advanced technology, replicating the same chemical, physical, and optical properties as mined diamonds - with guaranteed brilliance, ethics, and sustainability.",
      },
      {
        question: "Can I customize my Mirror piece?",
        answer:
          "Yes. Mirror offers customization options for select designs. Please contact us or visit our showroom to discuss your vision with our team.",
      },
      {
        question: "Is Mirror jewelry hypoallergenic?",
        answer:
          "Yes. Our materials are carefully selected to be skin-safe and hypoallergenic, suitable for most wearers.",
      },
    ],
    orders: [
      {
        question: "How can I place an order?",
        answer:
          "You can order online through our website or in person at our Vietnam showroom. For international orders, we recommend a virtual consultation before purchasing.",
      },
      {
        question: "What payment methods do you accept?",
        answer:
          "We accept major credit cards, bank transfers, and digital payment platforms. Full payment details are available at checkout or in the showroom.",
      },
      {
        question: "Can I cancel or change my order after it's placed?",
        answer:
          "Once an order is confirmed, cancellations or modifications may not be possible due to the nature of our made-to-order or limited-release designs. Please contact us immediately if you need assistance.",
      },
    ],
    exchanges: [
      {
        question: "Do you accept returns?",
        answer:
          "Due to the precision, rarity, and limited nature of our creations, all sales are final. Mirror does not accept returns under any circumstances.",
      },
      {
        question: "Do you offer exchanges?",
        answer:
          "Exchanges may be allowed for domestic purchases under the following conditions:\n\nIn-Person: Within 3 days of purchase, unworn, with all original packaging and certificates, returned to the original showroom.\n\nNationwide Shipping: Within 5 days of confirmed delivery, pending approval after inspection. Customers are responsible for return shipping.",
      },
      {
        question: "What items are not eligible for exchange?",
        answer:
          "Custom or engraved items\n\nEarrings (for hygiene reasons)\n\nPromotional or discounted items\n\nItems showing wear, damage, or missing documentation",
      },
    ],
    care: [
      {
        question: "Do you offer cleaning services?",
        answer:
          "Yes. We offer lifetime complimentary cleaning and shining services at our Vietnam showroom. This includes buffing, inspection, and surface refinement.",
      },
      {
        question: "What does the warranty cover?",
        answer:
          "Each piece includes a 12-month limited warranty covering:\n\nManufacturing or craftsmanship defects\n\nClasp or chain malfunction\n\nStone loosening due to workmanship",
      },
      {
        question: "What's not covered under warranty?",
        answer:
          "Damage from misuse, accidents, or improper wear\n\nNatural wear-and-tear, discoloration, or third-party alterations\n\nLoss, theft, or environmental damage",
      },
      {
        question: "Do you offer repair services?",
        answer:
          "Yes. For issues outside the warranty, Mirror provides professional paid repair services, including component replacement, reshaping, polishing, and stone reset. Repair quotes are provided after inspection.",
      },
      {
        question: "How can I keep my jewelry looking its best?",
        answer:
          "Avoid wearing your piece when swimming, showering, or exercising\n\nStore it in its original Mirror box or pouch\n\nKeep away from harsh chemicals or ultrasonic cleaners\n\nClean gently with warm water and a soft brush",
      },
    ],
    shipping: [
      {
        question: "Do you ship internationally?",
        answer:
          "Yes, we offer international shipping. However, all international sales are final and not eligible for exchange or return.",
      },
      {
        question: "How long will it take to receive my order?",
        answer:
          "Delivery timelines vary depending on whether the piece is made-to-order or in stock. Estimated timelines are shared at checkout or during consultation.",
      },
      {
        question: "Is shipping insured?",
        answer:
          "Yes. All shipments are insured for peace of mind during transit.",
      },
      {
        question: "Can I track my order?",
        answer:
          "Yes. Once your order is shipped, you will receive a tracking number via email.",
      },
    ],
    location: [
      {
        question: "Where is Mirror located?",
        answer:
          "Mirror's flagship showroom is located in Vietnam. Full address and visiting hours are available on our Location page.",
      },
      {
        question: "Can I book an appointment to visit?",
        answer:
          "Absolutely. We recommend scheduling an appointment for personalized service, whether for a consultation, sizing, or viewing.",
      },
      {
        question: "Do you offer virtual consultations?",
        answer:
          "Yes. We offer virtual consultations for international clients and customers who wish to receive expert guidance before purchasing.",
      },
    ],
  };

  // Dữ liệu để định nghĩa các section và sidebar, giúp chúng đồng bộ
  const sectionsInfo = [
    { id: "products", title: "Products" },
    { id: "orders", title: "Orders & Payments" },
    { id: "exchanges", title: "Exchanges & Returns" },
    { id: "care", title: "Care & Repairs" },
    { id: "shipping", title: "Shipping & Delivery" },
    { id: "location", title: "Location" },
  ];

  // Logic Intersection Observer để theo dõi section active (giữ nguyên)
  const observer = useRef(null);
  useEffect(() => {
    observer.current = new IntersectionObserver(
      (entries) => {
        const visibleSection = entries.find(
          (entry) => entry.isIntersecting
        )?.target;
        if (visibleSection) {
          setActiveSection(visibleSection.id);
        }
      },
      {
        rootMargin: "-40% 0px -60% 0px",
        threshold: 0,
      }
    );

    const sections = document.querySelectorAll(".faq-section");
    sections.forEach((section) => observer.current.observe(section));

    return () => {
      sections.forEach((section) => observer.current.unobserve(section));
    };
  }, []);

  // Handle smooth scroll for anchor navigation
  useEffect(() => {
    const handleAnchorClick = (e) => {
      const target = e.target.closest('a[href^="#"]');
      if (target) {
        e.preventDefault();
        const id = target.getAttribute("href").slice(1);
        const element = document.getElementById(id);
        if (element) {
          element.scrollIntoView({ behavior: "smooth" });
        }
      }
    };

    const navElement = document.querySelector(".faq-nav");
    if (navElement) {
      navElement.addEventListener("click", handleAnchorClick);
    }

    return () => {
      if (navElement) {
        navElement.removeEventListener("click", handleAnchorClick);
      }
    };
  }, []);

  const toggleFAQ = (id) => {
    const panel = document.querySelector(`[data-faq-id="${id}"]`);

    if (panel) {
      if (panel.style.maxHeight) {
        panel.style.maxHeight = null;
        setExpandedFAQ(null);
      } else {
        // Đóng tất cả panels khác trước
        document.querySelectorAll(".faq-answer").forEach((p) => {
          p.style.maxHeight = null;
        });

        // Mở panel hiện tại
        panel.style.maxHeight = panel.scrollHeight + "px";
        setExpandedFAQ(id);
      }
    }
  };

  // Filter FAQs based on search query
  const getFilteredFAQs = () => {
    if (!searchQuery.trim()) return null;

    const query = searchQuery.toLowerCase();
    const results = [];

    sectionsInfo.forEach((section) => {
      faqData[section.id]?.forEach((faq, index) => {
        if (
          faq.question.toLowerCase().includes(query) ||
          faq.answer.toLowerCase().includes(query)
        ) {
          results.push({
            ...faq,
            sectionId: section.id,
            sectionTitle: section.title,
            id: `search-${section.id}-${index}`,
          });
        }
      });
    });

    return results;
  };

  const filteredFAQs = getFilteredFAQs();

  return (
    // Thêm container để xác định vùng hoạt động của sticky
    <div className="faqs-sticky-container">
      <div className="faqs-wrapper">
        <div className="faq-sidebar-section">
          {/* Sidebar giờ sẽ được render động từ `sectionsInfo` */}
          <nav className="faq-nav">
            <ul>
              {sectionsInfo.map((section) => (
                <li
                  key={section.id}
                  className={`nav-item ${
                    activeSection === section.id ? "active" : ""
                  }`}
                >
                  <a href={`#${section.id}`} className="bodytext-4--no-margin">
                    {section.title}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        </div>

        <div className="faqs-main-section">
          <div className="search-section">
            <div className="search-container">
              <svg className="search-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
                <circle cx="9" cy="9" r="7" stroke="currentColor" strokeWidth="1.5"/>
                <path d="M14 14L18 18" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
              </svg>
              <input
                type="text"
                placeholder="What you are looking for"
                className="search-input bodytext-4--no-margin"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <svg className="search-arrow" width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M4 10H16M16 10L11 5M16 10L11 15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
          </div>

          <div className="faq-content-section">
            {/* Search Results */}
            {filteredFAQs !== null ? (
              <div className="faq-search-results">
                {filteredFAQs.length > 0 ? (
                  <div className="faq-list">
                    {filteredFAQs.map((faq) => (
                      <div
                        key={faq.id}
                        className={`faq-item ${
                          expandedFAQ === faq.id ? "active" : ""
                        }`}
                        onClick={() => toggleFAQ(faq.id)}
                      >
                        <button className="faq-question">
                          <span>{faq.question}</span>
                          <span
                            className={`faq-icon ${
                              expandedFAQ === faq.id ? "expanded" : ""
                            }`}
                          ></span>
                        </button>
                        <div className="faq-answer" data-faq-id={faq.id}>
                          <p>{faq.answer}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="faq-no-results">
                    <p className="bodytext-4--no-margin">No results found for "{searchQuery}"</p>
                  </div>
                )}
              </div>
            ) : (
              /* Normal Sections View */
              <div className="faq-sections">
                {sectionsInfo.map((section) => (
                  <div key={section.id} id={section.id} className="faq-section">
                    <div className="faq-image-placeholder">
                      <h3 className="heading-3--no-margin placeholder-box">
                        {section.title}
                      </h3>
                    </div>

                    <div className="faq-list">
                      {faqData[section.id]?.map((faq, index) => (
                        <div
                          key={`${section.id}-${index}`}
                          className={`faq-item ${
                            expandedFAQ === `${section.id}-${index}`
                              ? "active"
                              : ""
                          }`}
                          onClick={() => toggleFAQ(`${section.id}-${index}`)}
                        >
                          <button className="faq-question">
                            <span>{faq.question}</span>
                            <span
                              className={`faq-icon ${
                                expandedFAQ === `${section.id}-${index}`
                                  ? "expanded"
                                  : ""
                              }`}
                            ></span>
                          </button>
                          <div
                            className="faq-answer"
                            data-faq-id={`${section.id}-${index}`}
                          >
                            <p>{faq.answer}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default FAQs;
