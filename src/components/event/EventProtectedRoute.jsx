/**
 * EventProtectedRoute - Protect event routes that require login
 * If user is not logged in, redirect to EVENT_GUIDE
 * If logged in, render the children
 */
import { Navigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import useEventStore from '@/store/useEventStore';

const EventProtectedRoute = ({ children }) => {
  const user = useEventStore((state) => state.user);

  // If not logged in, redirect to event guide
  if (!user) {
    return <Navigate to={ROUTES.EVENT_GUIDE} replace />;
  }

  // User is logged in, render the page
  return children;
};

export default EventProtectedRoute;
