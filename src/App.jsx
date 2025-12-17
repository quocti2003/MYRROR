// src/App.jsx
import { useEffect } from "react";
import { BrowserRouter } from "react-router-dom";
import { Analytics } from "@vercel/analytics/react";
import { SpeedInsights } from "@vercel/speed-insights/react";
import AppRoutes from "@/routes/AppRoutes";
import ErrorBoundary from "@components/errorBoundary/ErrorBoundary";
import { AuthProvider } from "@/context/AuthContext";
import { ImmersiveModalProvider } from "@/contexts/ImmersiveModalContext";
import "@styles/fonts.css";
import "@styles/typography.css";

function App() {
  // Scroll to top on page load/reload
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  return (
    <BrowserRouter>
      <ErrorBoundary>
        <AuthProvider>
          <ImmersiveModalProvider>
            <AppRoutes />
            <Analytics />
            <SpeedInsights />
          </ImmersiveModalProvider>
        </AuthProvider>
      </ErrorBoundary>
    </BrowserRouter>
  );
}

export default App;
