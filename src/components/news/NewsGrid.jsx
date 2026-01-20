import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";
import { getNewsDetailRoute } from "@/constants/routes";
import { MediaImage } from "@components/common/media";
import "./NewsGrid.css";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import UnderlineButtonOpposite from "@components/common/button/UnderlineButtonOpposite";

const NewsGrid = () => {
  const [visibleItems, setVisibleItems] = useState(999); // Show all items
  const [imageErrors, setImageErrors] = useState({});
  const navigate = useNavigate();

  // Sample news data - replace with actual data
  const newsData = [
    {
      id: 1,
      title: "Digital Jewelry Week Milan 2025",
      slug: "milan",
      image: "news/mirror-milan1.webp",
      date: "10/2025",
    },
    {
      id: 2,
      title: "Lumex-91™: The Next Star Has Arrived",
      slug: "new-cut",
      image: "news/lumex_91.webp",
      date: "10/2024",
    },
    {
      id: 3,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null, // No detail page yet
      image: "news/news_img_2.svg",
      date: "09/2024",
    },
    {
      id: 4,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news_img_3.svg",
      date: "08/2024",
    },
    {
      id: 5,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news_img_4.svg",
      date: "07/2024",
    },
    {
      id: 6,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news_img_7.svg",
      date: "06/2024",
    },
    {
      id: 7,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news_img_6.svg",
      date: "05/2024",
    },
    {
      id: 8,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news_img_5.svg",
      date: "04/2024",
    },
    {
      id: 9,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news_img_8.svg",
      date: "03/2024",
    },
    // Add more placeholder items
    {
      id: 10,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news-9.webp",
      date: "02/2024",
    },
    {
      id: 11,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news-10.webp",
      date: "01/2024",
    },
    {
      id: 12,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news-11.webp",
      date: "12/2023",
    },
    {
      id: 13,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news-12.webp",
      date: "11/2023",
    },
    {
      id: 14,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news-13.webp",
      date: "10/2023",
    },
    {
      id: 15,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news-14.webp",
      date: "09/2023",
    },
    {
      id: 16,
      title: "La Société Benefit, vers un nouveau paradigme entrepreneurial?",
      slug: null,
      image: "news/news-15.webp",
      date: "08/2023",
    },
  ];

  const handleViewMore = () => {
    setVisibleItems((prev) => Math.min(prev + 8, newsData.length));
  };

  const handleNewsItemClick = async (item) => {
    // Only navigate if the item has a slug (detail page exists)
    if (item.slug) {
      await optimizedTransitionUtils.transitionToRoute(
        navigate,
        getNewsDetailRoute(item.slug)
      );
    }
  };

  const handleImageError = (itemId) => {
    setImageErrors((prev) => ({ ...prev, [itemId]: true }));
  };

  // Group items into rows of 4
  const groupedData = [];
  for (let i = 0; i < Math.min(visibleItems, newsData.length); i += 4) {
    groupedData.push(newsData.slice(i, i + 4));
  }

  return (
    <section className="news-grid">
      <div className="news-grid-container">
        {groupedData.map((row, rowIndex) => (
          <div key={rowIndex} className="news-row">
            <div className="news-row-content">
              {row.map((item) => (
                <div
                  key={item.id}
                  className="news-item"
                  onClick={() => item.slug && handleNewsItemClick(item)}
                >
                  <div
                    className={`news-item-image ${
                      !item.image || imageErrors[item.id] ? "no-image" : ""
                    } ${
                      item.id === 2 || item.id === 8
                        ? "news-item-image-portrait"
                        : ""
                    }`}
                  >
                    {item.image && !imageErrors[item.id] && (
                      <MediaImage
                        src={item.image}
                        alt=""
                        onError={() => handleImageError(item.id)}
                      />
                    )}
                  </div>
                  <div className="news-item-content">
                    <div className="news-item-text">
                      <p className="news-item-date bodytext-6">{item.date}</p>
                      <h3 className="news-item-title heading-3--no-margin">
                        {item.title}
                      </h3>
                    </div>
                    {item.slug ? (
                      <UnderlineButtonOpposite
                        className="news-item-button"
                        textClassName="bodytext-6--no-margin"
                      >
                        Discover
                      </UnderlineButtonOpposite>
                    ) : (
                      <UnderlineButtonOpposite
                        className="news-item-button"
                        textClassName="bodytext-6--no-margin"
                        disabled
                      >
                        Discover
                      </UnderlineButtonOpposite>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
};

export default NewsGrid;
