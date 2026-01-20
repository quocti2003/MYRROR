import React, { useState, useEffect, useCallback, useMemo } from "react";
import { Calendar, momentLocalizer } from "react-big-calendar";
import moment from "moment";
import "react-big-calendar/lib/css/react-big-calendar.css";
import {
  appointmentsAPI,
  blockedSlotsAPI,
  locationsAPI,
  handleAPIError,
} from "@services/api";

const localizer = momentLocalizer(moment);

// Luxury Design System
const theme = {
  colors: {
    // Core palette
    charcoal: "#1a1a1f",
    warmBlack: "#0d0d0f",
    champagne: "#d4af37",
    roseGold: "#b76e79",
    cream: "#faf8f5",
    warmGray: "#e8e4df",

    // Status colors (muted, elegant)
    pending: { bg: "#fef7e6", text: "#8b6914", accent: "#d4af37" },
    confirmed: { bg: "#e8f4f8", text: "#1e6b7b", accent: "#3d9fb3" },
    completed: { bg: "#e8f5ed", text: "#1a6b3a", accent: "#2d9a5d" },
    cancelled: { bg: "#fce8e8", text: "#8b1a1a", accent: "#c53030" },
    noShow: { bg: "#f0ebe5", text: "#5a5147", accent: "#8b7355" },
    blocked: { bg: "linear-gradient(135deg, #2d1f1f 0%, #3d2929 100%)", text: "#e8c4c4", accent: "#c78080" },
  },
  shadows: {
    soft: "0 2px 20px rgba(0,0,0,0.04), 0 1px 4px rgba(0,0,0,0.03)",
    medium: "0 8px 32px rgba(0,0,0,0.08), 0 2px 8px rgba(0,0,0,0.04)",
    glow: "0 0 40px rgba(212, 175, 55, 0.15)",
    inner: "inset 0 1px 2px rgba(0,0,0,0.06)",
  },
  gradients: {
    champagneShimmer: "linear-gradient(135deg, #d4af37 0%, #f4d79c 50%, #d4af37 100%)",
    warmSurface: "linear-gradient(180deg, #ffffff 0%, #faf8f5 100%)",
    darkOverlay: "linear-gradient(180deg, rgba(26,26,31,0) 0%, rgba(26,26,31,0.02) 100%)",
  },
};

const AppointmentsManager = () => {
  const [appointments, setAppointments] = useState([]);
  const [blockedSlots, setBlockedSlots] = useState([]);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewMode, setViewMode] = useState("calendar");
  const [calendarView, setCalendarView] = useState("month");
  const [calendarDate, setCalendarDate] = useState(new Date());

  // Filters
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("all");
  const [selectedVenue, setSelectedVenue] = useState("all");
  const [selectedDate, setSelectedDate] = useState("");

  // Modals
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [staffNotes, setStaffNotes] = useState("");

  // Block slot form
  const [blockForm, setBlockForm] = useState({
    venueId: "",
    blockDate: "",
    startTime: "09:00",
    endTime: "18:00",
    isFullDay: false,
    reason: "",
    blockType: "SINGLE",
  });

  const statusOptions = [
    { value: "all", label: "All Statuses" },
    { value: "PENDING", label: "Pending" },
    { value: "CONFIRMED", label: "Confirmed" },
    { value: "COMPLETED", label: "Completed" },
    { value: "CANCELLED", label: "Cancelled" },
    { value: "NO_SHOW", label: "No Show" },
  ];

  const timeSlots = [
    "09:00", "10:00", "11:00", "12:00",
    "13:00", "14:00", "15:00", "16:00", "17:00", "18:00",
  ];

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [appointmentsRes, locationsRes, blockedRes] = await Promise.all([
        appointmentsAPI.getAll(),
        locationsAPI.getAll(),
        blockedSlotsAPI.getAll(),
      ]);
      setAppointments(appointmentsRes.data || []);
      setLocations(locationsRes.data || []);
      setBlockedSlots(blockedRes.data || []);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to load appointments");
      setError(errorInfo.message);
    } finally {
      setLoading(false);
    }
  };

  const getVenueName = (venueId) => {
    const venue = locations.find((l) => l.id === venueId);
    return venue?.name || "Unknown Venue";
  };

  const getStatusStyle = (status) => {
    switch (status) {
      case "PENDING": return theme.colors.pending;
      case "CONFIRMED": return theme.colors.confirmed;
      case "COMPLETED": return theme.colors.completed;
      case "CANCELLED": return theme.colors.cancelled;
      case "NO_SHOW": return theme.colors.noShow;
      default: return theme.colors.noShow;
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "N/A";
    const date = new Date(dateStr);
    return date.toLocaleDateString("en-US", {
      weekday: "short",
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  const formatTime = (timeStr) => {
    if (!timeStr) return "N/A";
    const [hours, minutes] = timeStr.split(":");
    const hour = parseInt(hours);
    const ampm = hour >= 12 ? "PM" : "AM";
    const hour12 = hour % 12 || 12;
    return `${hour12}:${minutes} ${ampm}`;
  };

  // Calendar events
  const calendarEvents = useMemo(() => {
    const events = [];

    appointments.forEach((apt) => {
      if (apt.status === "CANCELLED") return;

      const [hours, minutes] = (apt.appointmentTime || "09:00").split(":");
      const start = new Date(apt.appointmentDate);
      start.setHours(parseInt(hours), parseInt(minutes), 0);
      const end = new Date(start);
      end.setHours(start.getHours() + 1);

      events.push({
        id: apt.id,
        title: `${apt.customerFirstName} ${apt.customerLastName}`,
        start,
        end,
        resource: apt,
        type: "appointment",
        status: apt.status,
      });
    });

    blockedSlots.forEach((block) => {
      const start = new Date(block.blockDate);
      const end = new Date(block.blockDate);

      if (block.isFullDay) {
        start.setHours(9, 0, 0);
        end.setHours(18, 0, 0);
      } else {
        const [startH, startM] = (block.startTime || "09:00").split(":");
        const [endH, endM] = (block.endTime || "18:00").split(":");
        start.setHours(parseInt(startH), parseInt(startM), 0);
        end.setHours(parseInt(endH), parseInt(endM), 0);
      }

      events.push({
        id: block.id,
        title: block.reason || "Blocked",
        start,
        end,
        resource: block,
        type: "blocked",
      });
    });

    return events;
  }, [appointments, blockedSlots]);

  const eventStyleGetter = (event) => {
    if (event.type === "blocked") {
      return {
        style: {
          background: "linear-gradient(135deg, #8b4555 0%, #6b3344 100%)",
          color: "#fff",
          border: "none",
          borderRadius: "6px",
          fontSize: "0.75rem",
          fontWeight: "500",
          padding: "2px 8px",
          boxShadow: "0 2px 8px rgba(139, 69, 85, 0.3)",
        },
      };
    }

    const style = getStatusStyle(event.status);
    return {
      style: {
        backgroundColor: style.bg,
        color: style.text,
        border: `1px solid ${style.accent}`,
        borderRadius: "6px",
        fontSize: "0.75rem",
        fontWeight: "500",
        padding: "2px 8px",
      },
    };
  };

  const handleSelectEvent = (event) => {
    if (event.type === "blocked") {
      if (window.confirm(`Delete blocked slot: "${event.title}"?`)) {
        handleDeleteBlockedSlot(event.id);
      }
    } else {
      setSelectedAppointment(event.resource);
      setStaffNotes(event.resource.staffNotes || "");
      setIsDetailModalOpen(true);
    }
  };

  const handleSelectSlot = ({ start }) => {
    const dateStr = moment(start).format("YYYY-MM-DD");
    const timeStr = moment(start).format("HH:00");
    setBlockForm({
      ...blockForm,
      blockDate: dateStr,
      startTime: timeStr,
      endTime: moment(start).add(1, "hour").format("HH:00"),
    });
    setIsBlockModalOpen(true);
  };

  const handleViewDetails = (appointment) => {
    setSelectedAppointment(appointment);
    setStaffNotes(appointment.staffNotes || "");
    setIsDetailModalOpen(true);
  };

  const handleConfirm = async (id) => {
    try {
      await appointmentsAPI.confirm(id);
      await fetchData();
      setIsDetailModalOpen(false);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to confirm appointment");
      setError(errorInfo.message);
    }
  };

  const handleComplete = async (id) => {
    try {
      await appointmentsAPI.complete(id);
      await fetchData();
      setIsDetailModalOpen(false);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to complete appointment");
      setError(errorInfo.message);
    }
  };

  const handleOpenCancelModal = (appointment) => {
    setSelectedAppointment(appointment);
    setCancelReason("");
    setIsCancelModalOpen(true);
  };

  const handleCancel = async () => {
    if (!selectedAppointment) return;
    try {
      await appointmentsAPI.cancel(selectedAppointment.id, cancelReason);
      await fetchData();
      setIsCancelModalOpen(false);
      setIsDetailModalOpen(false);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to cancel appointment");
      setError(errorInfo.message);
    }
  };

  const handleMarkNoShow = async (id) => {
    if (!window.confirm("Mark this appointment as No Show?")) return;
    try {
      await appointmentsAPI.markNoShow(id);
      await fetchData();
      setIsDetailModalOpen(false);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to mark as no-show");
      setError(errorInfo.message);
    }
  };

  const handleSaveNotes = async () => {
    if (!selectedAppointment) return;
    try {
      await appointmentsAPI.addNotes(selectedAppointment.id, staffNotes);
      await fetchData();
      setError(null);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to save notes");
      setError(errorInfo.message);
    }
  };

  const handleCreateBlockedSlot = async () => {
    try {
      await blockedSlotsAPI.create({
        venueId: blockForm.venueId || null,
        blockDate: blockForm.blockDate,
        startTime: blockForm.startTime,
        endTime: blockForm.endTime,
        isFullDay: blockForm.isFullDay,
        reason: blockForm.reason,
        blockType: blockForm.blockType,
      });
      await fetchData();
      setIsBlockModalOpen(false);
      setBlockForm({
        venueId: "",
        blockDate: "",
        startTime: "09:00",
        endTime: "18:00",
        isFullDay: false,
        reason: "",
        blockType: "SINGLE",
      });
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to create blocked slot");
      setError(errorInfo.message);
    }
  };

  const handleDeleteBlockedSlot = async (id) => {
    try {
      await blockedSlotsAPI.delete(id);
      await fetchData();
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to delete blocked slot");
      setError(errorInfo.message);
    }
  };

  const filteredAppointments = appointments.filter((apt) => {
    const matchesSearch =
      apt.customerFirstName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      apt.customerLastName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      apt.customerEmail?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      apt.customerPhone?.includes(searchTerm);
    const matchesStatus = selectedStatus === "all" || apt.status === selectedStatus;
    const matchesVenue = selectedVenue === "all" || apt.venueId === selectedVenue;
    const matchesDate = !selectedDate || apt.appointmentDate === selectedDate;
    return matchesSearch && matchesStatus && matchesVenue && matchesDate;
  });

  const sortedAppointments = [...filteredAppointments].sort((a, b) => {
    const dateCompare = new Date(b.appointmentDate) - new Date(a.appointmentDate);
    if (dateCompare !== 0) return dateCompare;
    return (a.appointmentTime || "").localeCompare(b.appointmentTime || "");
  });

  // Styles
  const styles = {
    container: {
      fontFamily: "'DM Sans', -apple-system, BlinkMacSystemFont, sans-serif",
      background: theme.gradients.warmSurface,
      borderRadius: "16px",
      padding: "2rem",
      boxShadow: theme.shadows.medium,
      position: "relative",
      overflow: "hidden",
    },
    header: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: "2rem",
      flexWrap: "wrap",
      gap: "1rem",
    },
    titleSection: {
      display: "flex",
      flexDirection: "column",
      gap: "0.25rem",
    },
    title: {
      fontFamily: "'Playfair Display', Georgia, serif",
      fontSize: "1.75rem",
      fontWeight: "600",
      color: theme.colors.charcoal,
      letterSpacing: "-0.02em",
      margin: 0,
    },
    subtitle: {
      fontSize: "0.875rem",
      color: "#64748b",
      fontWeight: "400",
    },
    viewToggle: {
      display: "flex",
      gap: "4px",
      background: theme.colors.warmGray,
      padding: "4px",
      borderRadius: "10px",
    },
    viewButton: (isActive) => ({
      padding: "0.625rem 1.25rem",
      fontSize: "0.8125rem",
      fontWeight: "500",
      border: "none",
      borderRadius: "8px",
      cursor: "pointer",
      transition: "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)",
      background: isActive ? "#fff" : "transparent",
      color: isActive ? theme.colors.charcoal : "#64748b",
      boxShadow: isActive ? theme.shadows.soft : "none",
    }),
    blockButton: {
      display: "flex",
      alignItems: "center",
      gap: "0.5rem",
      padding: "0.75rem 1.25rem",
      fontSize: "0.8125rem",
      fontWeight: "500",
      border: "none",
      borderRadius: "10px",
      cursor: "pointer",
      background: "linear-gradient(135deg, #8b4555 0%, #6b3344 100%)",
      color: "#fff",
      transition: "all 0.2s ease",
      boxShadow: "0 4px 12px rgba(139, 69, 85, 0.25)",
    },
    statsGrid: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))",
      gap: "1rem",
      marginBottom: "2rem",
    },
    statCard: (color, isSpecial = false) => ({
      position: "relative",
      background: isSpecial
        ? "linear-gradient(135deg, #1a1a1f 0%, #2d2d35 100%)"
        : "#fff",
      borderRadius: "14px",
      padding: "1.25rem",
      textAlign: "center",
      boxShadow: theme.shadows.soft,
      border: isSpecial ? "none" : "1px solid rgba(0,0,0,0.04)",
      overflow: "hidden",
      transition: "transform 0.2s ease, box-shadow 0.2s ease",
    }),
    statNumber: (color, isSpecial = false) => ({
      fontSize: "2rem",
      fontWeight: "700",
      fontFamily: "'Playfair Display', serif",
      color: isSpecial ? theme.colors.champagne : color,
      lineHeight: 1,
      marginBottom: "0.375rem",
    }),
    statLabel: (isSpecial = false) => ({
      fontSize: "0.75rem",
      color: isSpecial ? "rgba(255,255,255,0.7)" : "#64748b",
      textTransform: "uppercase",
      letterSpacing: "0.05em",
      fontWeight: "500",
    }),
    calendarContainer: {
      background: "#fff",
      borderRadius: "16px",
      padding: "1.5rem",
      boxShadow: theme.shadows.soft,
      border: "1px solid rgba(0,0,0,0.04)",
    },
    filterBar: {
      display: "flex",
      gap: "0.75rem",
      marginBottom: "1.5rem",
      flexWrap: "wrap",
      alignItems: "center",
    },
    input: {
      flex: 1,
      minWidth: "220px",
      padding: "0.75rem 1rem",
      fontSize: "0.875rem",
      border: "1px solid #e2e0dc",
      borderRadius: "10px",
      background: "#fff",
      color: theme.colors.charcoal,
      transition: "all 0.2s ease",
      outline: "none",
    },
    select: {
      padding: "0.75rem 1rem",
      fontSize: "0.875rem",
      border: "1px solid #e2e0dc",
      borderRadius: "10px",
      background: "#fff",
      color: theme.colors.charcoal,
      cursor: "pointer",
      minWidth: "140px",
      outline: "none",
    },
    table: {
      width: "100%",
      borderCollapse: "separate",
      borderSpacing: "0 8px",
      fontSize: "0.875rem",
    },
    tableHeader: {
      background: "transparent",
    },
    th: {
      padding: "0.75rem 1rem",
      textAlign: "left",
      fontWeight: "600",
      fontSize: "0.75rem",
      color: "#64748b",
      textTransform: "uppercase",
      letterSpacing: "0.05em",
    },
    tableRow: {
      background: "#fff",
      borderRadius: "12px",
      cursor: "pointer",
      transition: "all 0.2s ease",
      boxShadow: theme.shadows.soft,
    },
    td: {
      padding: "1rem",
      borderTop: "1px solid transparent",
      borderBottom: "1px solid transparent",
    },
    statusBadge: (style) => ({
      display: "inline-flex",
      alignItems: "center",
      gap: "0.375rem",
      padding: "0.375rem 0.875rem",
      borderRadius: "20px",
      fontSize: "0.75rem",
      fontWeight: "500",
      backgroundColor: style.bg,
      color: style.text,
      border: `1px solid ${style.accent}`,
    }),
    actionButton: (variant) => {
      const variants = {
        primary: {
          background: theme.colors.champagne,
          color: theme.colors.warmBlack,
          border: "none",
          boxShadow: "0 2px 8px rgba(212, 175, 55, 0.25)",
        },
        secondary: {
          background: "transparent",
          color: theme.colors.charcoal,
          border: "1px solid #e2e0dc",
        },
        danger: {
          background: "linear-gradient(135deg, #dc2626 0%, #b91c1c 100%)",
          color: "#fff",
          border: "none",
          boxShadow: "0 2px 8px rgba(220, 38, 38, 0.25)",
        },
        outline: {
          background: "transparent",
          color: "#64748b",
          border: "1px solid #e2e0dc",
        },
      };
      return {
        ...variants[variant],
        padding: "0.5rem 1rem",
        fontSize: "0.8125rem",
        fontWeight: "500",
        borderRadius: "8px",
        cursor: "pointer",
        transition: "all 0.2s ease",
      };
    },
    modalOverlay: {
      position: "fixed",
      inset: 0,
      background: "rgba(13, 13, 15, 0.75)",
      backdropFilter: "blur(8px)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      zIndex: 1000,
      padding: "1rem",
      animation: "fadeIn 0.2s ease",
    },
    modal: {
      background: "#fff",
      borderRadius: "20px",
      maxWidth: "560px",
      width: "100%",
      maxHeight: "90vh",
      overflow: "auto",
      boxShadow: "0 24px 48px rgba(0,0,0,0.2)",
      animation: "slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1)",
    },
    modalHeader: {
      padding: "1.75rem 1.75rem 1.25rem",
      borderBottom: "1px solid #f1eeea",
    },
    modalBody: {
      padding: "1.5rem 1.75rem",
    },
    modalFooter: {
      padding: "1.25rem 1.75rem 1.75rem",
      borderTop: "1px solid #f1eeea",
      display: "flex",
      gap: "0.75rem",
      justifyContent: "flex-end",
    },
    infoCard: {
      background: theme.colors.cream,
      borderRadius: "12px",
      padding: "1rem",
      marginBottom: "0.75rem",
    },
    label: {
      display: "block",
      fontSize: "0.75rem",
      color: "#64748b",
      textTransform: "uppercase",
      letterSpacing: "0.05em",
      marginBottom: "0.375rem",
      fontWeight: "500",
    },
    value: {
      fontSize: "0.9375rem",
      color: theme.colors.charcoal,
      fontWeight: "500",
    },
  };

  // CSS for calendar customization
  const calendarStyles = `
    @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Playfair+Display:wght@400;500;600;700&display=swap');

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px) scale(0.98); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }

    @keyframes shimmer {
      0% { background-position: -200% 0; }
      100% { background-position: 200% 0; }
    }

    .luxury-calendar .rbc-calendar {
      font-family: 'DM Sans', sans-serif;
      background: transparent;
    }

    .luxury-calendar .rbc-toolbar {
      padding: 0.5rem 0 1.5rem;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .luxury-calendar .rbc-toolbar button {
      font-family: 'DM Sans', sans-serif;
      font-size: 0.8125rem;
      font-weight: 500;
      color: #64748b;
      border: 1px solid #e2e0dc;
      border-radius: 8px;
      padding: 0.5rem 1rem;
      background: #fff;
      transition: all 0.2s ease;
    }

    .luxury-calendar .rbc-toolbar button:hover {
      background: ${theme.colors.cream};
      color: ${theme.colors.charcoal};
      border-color: ${theme.colors.champagne};
    }

    .luxury-calendar .rbc-toolbar button.rbc-active {
      background: ${theme.colors.charcoal};
      color: #fff;
      border-color: ${theme.colors.charcoal};
      box-shadow: 0 2px 8px rgba(26, 26, 31, 0.2);
    }

    .luxury-calendar .rbc-toolbar-label {
      font-family: 'Playfair Display', serif;
      font-size: 1.25rem;
      font-weight: 600;
      color: ${theme.colors.charcoal};
    }

    .luxury-calendar .rbc-header {
      padding: 1rem 0.5rem;
      font-weight: 600;
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #64748b;
      border-bottom: 2px solid #f1eeea;
    }

    .luxury-calendar .rbc-month-view {
      border: none;
      border-radius: 12px;
      overflow: hidden;
    }

    .luxury-calendar .rbc-month-row {
      border: none;
    }

    .luxury-calendar .rbc-day-bg {
      border-left: 1px solid #f1eeea;
      transition: background 0.15s ease;
    }

    .luxury-calendar .rbc-day-bg:hover {
      background: ${theme.colors.cream};
    }

    .luxury-calendar .rbc-today {
      background: linear-gradient(135deg, rgba(212, 175, 55, 0.08) 0%, rgba(212, 175, 55, 0.04) 100%);
    }

    .luxury-calendar .rbc-off-range-bg {
      background: #fafafa;
    }

    .luxury-calendar .rbc-date-cell {
      padding: 0.5rem;
      text-align: right;
      font-weight: 500;
      color: ${theme.colors.charcoal};
    }

    .luxury-calendar .rbc-date-cell.rbc-now {
      font-weight: 700;
    }

    .luxury-calendar .rbc-date-cell.rbc-now a {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      background: ${theme.colors.champagne};
      color: ${theme.colors.warmBlack};
      border-radius: 50%;
      text-decoration: none;
    }

    .luxury-calendar .rbc-event {
      border: none !important;
      padding: 4px 8px !important;
      font-size: 0.75rem !important;
      font-weight: 500 !important;
      border-radius: 6px !important;
      margin: 1px 2px;
    }

    .luxury-calendar .rbc-event:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }

    .luxury-calendar .rbc-event-content {
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .luxury-calendar .rbc-show-more {
      font-size: 0.75rem;
      font-weight: 600;
      color: ${theme.colors.champagne};
      background: transparent;
      padding: 2px 4px;
    }

    .luxury-calendar .rbc-time-view {
      border: none;
    }

    .luxury-calendar .rbc-time-header {
      border-bottom: 2px solid #f1eeea;
    }

    .luxury-calendar .rbc-time-content {
      border-top: none;
    }

    .luxury-calendar .rbc-timeslot-group {
      border-bottom: 1px solid #f5f3f0;
    }

    .luxury-calendar .rbc-time-slot {
      font-size: 0.75rem;
      color: #94a3b8;
    }

    .luxury-calendar .rbc-current-time-indicator {
      background: ${theme.colors.champagne};
      height: 2px;
    }

    .luxury-calendar .rbc-current-time-indicator::before {
      content: '';
      position: absolute;
      left: -4px;
      top: -3px;
      width: 8px;
      height: 8px;
      background: ${theme.colors.champagne};
      border-radius: 50%;
    }

    /* Row hover effects for table */
    .luxury-table-row:hover {
      transform: translateY(-1px);
      box-shadow: 0 8px 24px rgba(0,0,0,0.08) !important;
    }

    .luxury-table-row td:first-child {
      border-radius: 12px 0 0 12px;
    }

    .luxury-table-row td:last-child {
      border-radius: 0 12px 12px 0;
    }

    /* Button hover states */
    .luxury-btn:hover {
      transform: translateY(-1px);
    }

    .luxury-btn-primary:hover {
      background: linear-gradient(135deg, #e5c349 0%, #d4af37 100%);
      box-shadow: 0 4px 16px rgba(212, 175, 55, 0.35);
    }

    .luxury-btn-danger:hover {
      background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    }

    /* Input focus states */
    .luxury-input:focus {
      border-color: ${theme.colors.champagne};
      box-shadow: 0 0 0 3px rgba(212, 175, 55, 0.15);
    }

    .luxury-select:focus {
      border-color: ${theme.colors.champagne};
      box-shadow: 0 0 0 3px rgba(212, 175, 55, 0.15);
    }

    /* Checkbox styling */
    .luxury-checkbox {
      width: 18px;
      height: 18px;
      accent-color: ${theme.colors.champagne};
      cursor: pointer;
    }

    /* Scrollbar styling */
    .luxury-modal::-webkit-scrollbar {
      width: 6px;
    }

    .luxury-modal::-webkit-scrollbar-track {
      background: #f1eeea;
      border-radius: 3px;
    }

    .luxury-modal::-webkit-scrollbar-thumb {
      background: #c4c0ba;
      border-radius: 3px;
    }

    .luxury-modal::-webkit-scrollbar-thumb:hover {
      background: #a8a49e;
    }
  `;

  if (loading) {
    return (
      <div style={{
        ...styles.container,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        minHeight: "400px",
      }}>
        <div style={{ textAlign: "center" }}>
          <div style={{
            width: "48px",
            height: "48px",
            border: "3px solid #f1eeea",
            borderTopColor: theme.colors.champagne,
            borderRadius: "50%",
            animation: "spin 1s linear infinite",
            margin: "0 auto 1rem",
          }} />
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          <p style={{ color: "#64748b", fontSize: "0.9375rem" }}>Loading appointments...</p>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <style>{calendarStyles}</style>

      {/* Decorative diamond pattern */}
      <div style={{
        position: "absolute",
        top: 0,
        right: 0,
        width: "200px",
        height: "200px",
        background: `radial-gradient(circle at 100% 0%, rgba(212, 175, 55, 0.06) 0%, transparent 50%)`,
        pointerEvents: "none",
      }} />

      {error && (
        <div style={{
          background: "linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%)",
          border: "1px solid #fecaca",
          borderRadius: "12px",
          padding: "1rem 1.25rem",
          marginBottom: "1.5rem",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
        }}>
          <span style={{ color: "#991b1b", fontSize: "0.875rem" }}>{error}</span>
          <button
            onClick={() => setError(null)}
            style={{
              background: "none",
              border: "none",
              color: "#991b1b",
              cursor: "pointer",
              fontSize: "1.25rem",
              lineHeight: 1,
              padding: "0.25rem",
            }}
          >
            ×
          </button>
        </div>
      )}

      {/* Header */}
      <div style={styles.header}>
        <div style={styles.titleSection}>
          <h1 style={styles.title}>Appointments</h1>
          <p style={styles.subtitle}>
            {appointments.length} total · {appointments.filter(a => a.status === "PENDING").length} pending confirmation
          </p>
        </div>

        <div style={{ display: "flex", gap: "1rem", alignItems: "center", flexWrap: "wrap" }}>
          <div style={styles.viewToggle}>
            <button
              onClick={() => setViewMode("calendar")}
              style={styles.viewButton(viewMode === "calendar")}
            >
              <svg style={{ width: "16px", height: "16px", marginRight: "6px" }} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              Calendar
            </button>
            <button
              onClick={() => setViewMode("list")}
              style={styles.viewButton(viewMode === "list")}
            >
              <svg style={{ width: "16px", height: "16px", marginRight: "6px" }} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="8" y1="6" x2="21" y2="6"/>
                <line x1="8" y1="12" x2="21" y2="12"/>
                <line x1="8" y1="18" x2="21" y2="18"/>
                <circle cx="4" cy="6" r="1.5" fill="currentColor"/>
                <circle cx="4" cy="12" r="1.5" fill="currentColor"/>
                <circle cx="4" cy="18" r="1.5" fill="currentColor"/>
              </svg>
              List
            </button>
          </div>

          <button
            onClick={() => setIsBlockModalOpen(true)}
            style={styles.blockButton}
            className="luxury-btn luxury-btn-danger"
          >
            <svg style={{ width: "16px", height: "16px" }} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
            </svg>
            Block Time
          </button>
        </div>
      </div>

      {/* Stats */}
      <div style={styles.statsGrid}>
        {[
          { label: "Total", count: appointments.length, color: theme.colors.charcoal },
          { label: "Pending", count: appointments.filter(a => a.status === "PENDING").length, color: theme.colors.pending.accent },
          { label: "Confirmed", count: appointments.filter(a => a.status === "CONFIRMED").length, color: theme.colors.confirmed.accent },
          { label: "Completed", count: appointments.filter(a => a.status === "COMPLETED").length, color: theme.colors.completed.accent },
          { label: "Blocked", count: blockedSlots.length, color: "#8b4555", isSpecial: true },
        ].map((stat) => (
          <div
            key={stat.label}
            style={styles.statCard(stat.color, stat.isSpecial)}
            className="luxury-stat-card"
          >
            {stat.isSpecial && (
              <div style={{
                position: "absolute",
                top: "-20px",
                right: "-20px",
                width: "60px",
                height: "60px",
                background: "radial-gradient(circle, rgba(212, 175, 55, 0.15) 0%, transparent 70%)",
                borderRadius: "50%",
              }} />
            )}
            <div style={styles.statNumber(stat.color, stat.isSpecial)}>{stat.count}</div>
            <div style={styles.statLabel(stat.isSpecial)}>{stat.label}</div>
          </div>
        ))}
      </div>

      {/* Calendar View */}
      {viewMode === "calendar" && (
        <div style={styles.calendarContainer} className="luxury-calendar">
          <Calendar
            localizer={localizer}
            events={calendarEvents}
            startAccessor="start"
            endAccessor="end"
            style={{ height: 650 }}
            view={calendarView}
            onView={setCalendarView}
            date={calendarDate}
            onNavigate={setCalendarDate}
            eventPropGetter={eventStyleGetter}
            onSelectEvent={handleSelectEvent}
            onSelectSlot={handleSelectSlot}
            selectable
            popup
            views={["month", "week", "day"]}
          />
        </div>
      )}

      {/* List View */}
      {viewMode === "list" && (
        <>
          {/* Filters */}
          <div style={styles.filterBar}>
            <input
              type="text"
              placeholder="Search by name, email, phone..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={styles.input}
              className="luxury-input"
            />
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              style={styles.select}
              className="luxury-select"
            >
              {statusOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
            <select
              value={selectedVenue}
              onChange={(e) => setSelectedVenue(e.target.value)}
              style={styles.select}
              className="luxury-select"
            >
              <option value="all">All Venues</option>
              {locations.map((loc) => (
                <option key={loc.id} value={loc.id}>{loc.name}</option>
              ))}
            </select>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              style={{ ...styles.input, flex: "none", width: "160px" }}
              className="luxury-input"
            />
            {(searchTerm || selectedStatus !== "all" || selectedVenue !== "all" || selectedDate) && (
              <button
                onClick={() => {
                  setSearchTerm("");
                  setSelectedStatus("all");
                  setSelectedVenue("all");
                  setSelectedDate("");
                }}
                style={styles.actionButton("outline")}
                className="luxury-btn"
              >
                Clear
              </button>
            )}
          </div>

          {/* Table */}
          <div style={{ overflowX: "auto" }}>
            <table style={styles.table}>
              <thead style={styles.tableHeader}>
                <tr>
                  <th style={styles.th}>Date & Time</th>
                  <th style={styles.th}>Customer</th>
                  <th style={styles.th}>Contact</th>
                  <th style={styles.th}>Venue</th>
                  <th style={{ ...styles.th, textAlign: "center" }}>Status</th>
                  <th style={{ ...styles.th, textAlign: "center" }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {sortedAppointments.map((apt) => {
                  const statusStyle = getStatusStyle(apt.status);
                  return (
                    <tr
                      key={apt.id}
                      style={styles.tableRow}
                      className="luxury-table-row"
                      onClick={() => handleViewDetails(apt)}
                    >
                      <td style={styles.td}>
                        <div style={{ fontWeight: "600", color: theme.colors.charcoal, marginBottom: "0.25rem" }}>
                          {formatDate(apt.appointmentDate)}
                        </div>
                        <div style={{ fontSize: "0.8125rem", color: "#64748b" }}>
                          {formatTime(apt.appointmentTime)}
                        </div>
                      </td>
                      <td style={styles.td}>
                        <div style={{ fontWeight: "500", color: theme.colors.charcoal }}>
                          {apt.customerTitle && `${apt.customerTitle} `}
                          {apt.customerFirstName} {apt.customerLastName}
                        </div>
                      </td>
                      <td style={styles.td}>
                        <div style={{ fontSize: "0.8125rem", color: theme.colors.charcoal }}>
                          {apt.customerEmail || "—"}
                        </div>
                        <div style={{ fontSize: "0.8125rem", color: "#64748b" }}>
                          {apt.customerPhone || "—"}
                        </div>
                      </td>
                      <td style={{ ...styles.td, color: "#64748b" }}>
                        {getVenueName(apt.venueId)}
                      </td>
                      <td style={{ ...styles.td, textAlign: "center" }}>
                        <span style={styles.statusBadge(statusStyle)}>
                          <span style={{
                            width: "6px",
                            height: "6px",
                            borderRadius: "50%",
                            background: statusStyle.accent,
                          }} />
                          {apt.status}
                        </span>
                      </td>
                      <td style={{ ...styles.td, textAlign: "center" }} onClick={(e) => e.stopPropagation()}>
                        <div style={{ display: "flex", gap: "0.5rem", justifyContent: "center" }}>
                          {apt.status === "PENDING" && (
                            <button
                              onClick={() => handleConfirm(apt.id)}
                              style={styles.actionButton("primary")}
                              className="luxury-btn luxury-btn-primary"
                            >
                              Confirm
                            </button>
                          )}
                          {apt.status === "CONFIRMED" && (
                            <button
                              onClick={() => handleComplete(apt.id)}
                              style={styles.actionButton("primary")}
                              className="luxury-btn luxury-btn-primary"
                            >
                              Complete
                            </button>
                          )}
                          {(apt.status === "PENDING" || apt.status === "CONFIRMED") && (
                            <button
                              onClick={() => handleOpenCancelModal(apt)}
                              style={styles.actionButton("outline")}
                              className="luxury-btn"
                            >
                              Cancel
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {sortedAppointments.length === 0 && (
            <div style={{
              textAlign: "center",
              padding: "4rem 2rem",
              color: "#64748b",
            }}>
              <svg style={{ width: "48px", height: "48px", margin: "0 auto 1rem", opacity: 0.3 }} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              <p style={{ fontFamily: "'Playfair Display', serif", fontSize: "1.125rem", marginBottom: "0.5rem" }}>
                No appointments found
              </p>
              <p style={{ fontSize: "0.875rem" }}>
                {(searchTerm || selectedStatus !== "all" || selectedVenue !== "all" || selectedDate)
                  ? "Try adjusting your filters"
                  : "Appointments will appear here when customers book"}
              </p>
            </div>
          )}
        </>
      )}

      {/* Detail Modal */}
      {isDetailModalOpen && selectedAppointment && (
        <div style={styles.modalOverlay} onClick={() => setIsDetailModalOpen(false)}>
          <div style={styles.modal} className="luxury-modal" onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                <div>
                  <h2 style={{
                    fontFamily: "'Playfair Display', serif",
                    fontSize: "1.375rem",
                    fontWeight: "600",
                    color: theme.colors.charcoal,
                    margin: "0 0 0.375rem 0",
                  }}>
                    Appointment Details
                  </h2>
                  <p style={{ fontSize: "0.8125rem", color: "#94a3b8", margin: 0 }}>
                    ID: {selectedAppointment.id}
                  </p>
                </div>
                <span style={styles.statusBadge(getStatusStyle(selectedAppointment.status))}>
                  <span style={{
                    width: "6px",
                    height: "6px",
                    borderRadius: "50%",
                    background: getStatusStyle(selectedAppointment.status).accent,
                  }} />
                  {selectedAppointment.status}
                </span>
              </div>
            </div>

            <div style={styles.modalBody}>
              {/* Customer Info */}
              <div style={{ marginBottom: "1.5rem" }}>
                <h3 style={{
                  fontSize: "0.6875rem",
                  fontWeight: "600",
                  color: "#94a3b8",
                  textTransform: "uppercase",
                  letterSpacing: "0.1em",
                  marginBottom: "0.75rem",
                }}>
                  Customer
                </h3>
                <div style={styles.infoCard}>
                  <div style={styles.value}>
                    {selectedAppointment.customerTitle && `${selectedAppointment.customerTitle} `}
                    {selectedAppointment.customerFirstName} {selectedAppointment.customerLastName}
                  </div>
                  {selectedAppointment.customerEmail && (
                    <div style={{ fontSize: "0.875rem", color: "#64748b", marginTop: "0.5rem" }}>
                      {selectedAppointment.customerEmail}
                    </div>
                  )}
                  {selectedAppointment.customerPhone && (
                    <div style={{ fontSize: "0.875rem", color: "#64748b", marginTop: "0.25rem" }}>
                      {selectedAppointment.customerPhone}
                    </div>
                  )}
                </div>
              </div>

              {/* Appointment Details */}
              <div style={{ marginBottom: "1.5rem" }}>
                <h3 style={{
                  fontSize: "0.6875rem",
                  fontWeight: "600",
                  color: "#94a3b8",
                  textTransform: "uppercase",
                  letterSpacing: "0.1em",
                  marginBottom: "0.75rem",
                }}>
                  Details
                </h3>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem" }}>
                  <div style={styles.infoCard}>
                    <div style={styles.label}>Date</div>
                    <div style={styles.value}>{formatDate(selectedAppointment.appointmentDate)}</div>
                  </div>
                  <div style={styles.infoCard}>
                    <div style={styles.label}>Time</div>
                    <div style={styles.value}>{formatTime(selectedAppointment.appointmentTime)}</div>
                  </div>
                  <div style={styles.infoCard}>
                    <div style={styles.label}>Venue</div>
                    <div style={styles.value}>{getVenueName(selectedAppointment.venueId)}</div>
                  </div>
                  <div style={styles.infoCard}>
                    <div style={styles.label}>Service</div>
                    <div style={styles.value}>{selectedAppointment.service || "General Inquiry"}</div>
                  </div>
                </div>
              </div>

              {/* Staff Notes */}
              <div>
                <h3 style={{
                  fontSize: "0.6875rem",
                  fontWeight: "600",
                  color: "#94a3b8",
                  textTransform: "uppercase",
                  letterSpacing: "0.1em",
                  marginBottom: "0.75rem",
                }}>
                  Staff Notes
                </h3>
                <textarea
                  value={staffNotes}
                  onChange={(e) => setStaffNotes(e.target.value)}
                  style={{
                    ...styles.input,
                    width: "100%",
                    minHeight: "100px",
                    resize: "vertical",
                    fontFamily: "'DM Sans', sans-serif",
                  }}
                  className="luxury-input"
                  placeholder="Add internal notes about this appointment..."
                />
                <button
                  onClick={handleSaveNotes}
                  style={{ ...styles.actionButton("secondary"), marginTop: "0.75rem" }}
                  className="luxury-btn"
                >
                  Save Notes
                </button>
              </div>
            </div>

            <div style={styles.modalFooter}>
              {selectedAppointment.status === "PENDING" && (
                <button
                  onClick={() => handleConfirm(selectedAppointment.id)}
                  style={styles.actionButton("primary")}
                  className="luxury-btn luxury-btn-primary"
                >
                  Confirm
                </button>
              )}
              {selectedAppointment.status === "CONFIRMED" && (
                <>
                  <button
                    onClick={() => handleComplete(selectedAppointment.id)}
                    style={styles.actionButton("primary")}
                    className="luxury-btn luxury-btn-primary"
                  >
                    Complete
                  </button>
                  <button
                    onClick={() => handleMarkNoShow(selectedAppointment.id)}
                    style={styles.actionButton("secondary")}
                    className="luxury-btn"
                  >
                    No Show
                  </button>
                </>
              )}
              {(selectedAppointment.status === "PENDING" || selectedAppointment.status === "CONFIRMED") && (
                <button
                  onClick={() => handleOpenCancelModal(selectedAppointment)}
                  style={styles.actionButton("outline")}
                  className="luxury-btn"
                >
                  Cancel
                </button>
              )}
              <div style={{ flex: 1 }} />
              <button
                onClick={() => setIsDetailModalOpen(false)}
                style={styles.actionButton("secondary")}
                className="luxury-btn"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Cancel Modal */}
      {isCancelModalOpen && selectedAppointment && (
        <div style={styles.modalOverlay} onClick={() => setIsCancelModalOpen(false)}>
          <div style={{ ...styles.modal, maxWidth: "420px" }} className="luxury-modal" onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h2 style={{
                fontFamily: "'Playfair Display', serif",
                fontSize: "1.25rem",
                fontWeight: "600",
                color: theme.colors.charcoal,
                margin: 0,
              }}>
                Cancel Appointment
              </h2>
            </div>

            <div style={styles.modalBody}>
              <p style={{ color: "#64748b", marginBottom: "1.25rem", fontSize: "0.9375rem" }}>
                Are you sure you want to cancel the appointment for{" "}
                <strong style={{ color: theme.colors.charcoal }}>
                  {selectedAppointment.customerFirstName} {selectedAppointment.customerLastName}
                </strong>?
              </p>
              <div>
                <label style={styles.label}>Reason for cancellation</label>
                <textarea
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  style={{
                    ...styles.input,
                    width: "100%",
                    minHeight: "100px",
                    resize: "vertical",
                    fontFamily: "'DM Sans', sans-serif",
                  }}
                  className="luxury-input"
                  placeholder="Enter reason..."
                />
              </div>
            </div>

            <div style={styles.modalFooter}>
              <button
                onClick={() => setIsCancelModalOpen(false)}
                style={styles.actionButton("secondary")}
                className="luxury-btn"
              >
                Keep Appointment
              </button>
              <button
                onClick={handleCancel}
                style={{
                  ...styles.actionButton("danger"),
                  background: "linear-gradient(135deg, #dc2626 0%, #b91c1c 100%)",
                }}
                className="luxury-btn luxury-btn-danger"
              >
                Cancel Appointment
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Block Slot Modal */}
      {isBlockModalOpen && (
        <div style={styles.modalOverlay} onClick={() => setIsBlockModalOpen(false)}>
          <div style={{ ...styles.modal, maxWidth: "480px" }} className="luxury-modal" onClick={(e) => e.stopPropagation()}>
            <div style={styles.modalHeader}>
              <h2 style={{
                fontFamily: "'Playfair Display', serif",
                fontSize: "1.25rem",
                fontWeight: "600",
                color: theme.colors.charcoal,
                margin: 0,
              }}>
                Block Time Slot
              </h2>
              <p style={{ fontSize: "0.875rem", color: "#64748b", marginTop: "0.5rem", marginBottom: 0 }}>
                Prevent customers from booking during this time
              </p>
            </div>

            <div style={styles.modalBody}>
              <div style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
                <div>
                  <label style={styles.label}>Venue (optional)</label>
                  <select
                    value={blockForm.venueId}
                    onChange={(e) => setBlockForm({ ...blockForm, venueId: e.target.value })}
                    style={{ ...styles.select, width: "100%" }}
                    className="luxury-select"
                  >
                    <option value="">All Venues</option>
                    {locations.map((loc) => (
                      <option key={loc.id} value={loc.id}>{loc.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label style={styles.label}>Date *</label>
                  <input
                    type="date"
                    value={blockForm.blockDate}
                    onChange={(e) => setBlockForm({ ...blockForm, blockDate: e.target.value })}
                    style={{ ...styles.input, width: "100%" }}
                    className="luxury-input"
                    required
                  />
                </div>

                <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
                  <input
                    type="checkbox"
                    id="isFullDay"
                    checked={blockForm.isFullDay}
                    onChange={(e) => setBlockForm({ ...blockForm, isFullDay: e.target.checked })}
                    className="luxury-checkbox"
                  />
                  <label htmlFor="isFullDay" style={{ fontSize: "0.9375rem", color: theme.colors.charcoal, cursor: "pointer" }}>
                    Block entire day
                  </label>
                </div>

                {!blockForm.isFullDay && (
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
                    <div>
                      <label style={styles.label}>Start Time</label>
                      <select
                        value={blockForm.startTime}
                        onChange={(e) => setBlockForm({ ...blockForm, startTime: e.target.value })}
                        style={{ ...styles.select, width: "100%" }}
                        className="luxury-select"
                      >
                        {timeSlots.map((t) => (
                          <option key={t} value={t}>{formatTime(t)}</option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label style={styles.label}>End Time</label>
                      <select
                        value={blockForm.endTime}
                        onChange={(e) => setBlockForm({ ...blockForm, endTime: e.target.value })}
                        style={{ ...styles.select, width: "100%" }}
                        className="luxury-select"
                      >
                        {timeSlots.map((t) => (
                          <option key={t} value={t}>{formatTime(t)}</option>
                        ))}
                      </select>
                    </div>
                  </div>
                )}

                <div>
                  <label style={styles.label}>Reason</label>
                  <input
                    type="text"
                    value={blockForm.reason}
                    onChange={(e) => setBlockForm({ ...blockForm, reason: e.target.value })}
                    style={{ ...styles.input, width: "100%" }}
                    className="luxury-input"
                    placeholder="e.g., Lunch break, Staff meeting, Holiday"
                  />
                </div>

                <div>
                  <label style={styles.label}>Block Type</label>
                  <select
                    value={blockForm.blockType}
                    onChange={(e) => setBlockForm({ ...blockForm, blockType: e.target.value })}
                    style={{ ...styles.select, width: "100%" }}
                    className="luxury-select"
                  >
                    <option value="SINGLE">Single (One time)</option>
                    <option value="RECURRING_DAILY">Daily (Every day)</option>
                    <option value="RECURRING_WEEKLY">Weekly (Same day each week)</option>
                  </select>
                </div>
              </div>
            </div>

            <div style={styles.modalFooter}>
              <button
                onClick={() => setIsBlockModalOpen(false)}
                style={styles.actionButton("secondary")}
                className="luxury-btn"
              >
                Cancel
              </button>
              <button
                onClick={handleCreateBlockedSlot}
                disabled={!blockForm.blockDate}
                style={{
                  ...styles.actionButton("danger"),
                  background: !blockForm.blockDate
                    ? "#e2e0dc"
                    : "linear-gradient(135deg, #8b4555 0%, #6b3344 100%)",
                  color: !blockForm.blockDate ? "#94a3b8" : "#fff",
                  cursor: !blockForm.blockDate ? "not-allowed" : "pointer",
                  boxShadow: !blockForm.blockDate ? "none" : "0 4px 12px rgba(139, 69, 85, 0.25)",
                }}
                className="luxury-btn"
              >
                Block Slot
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AppointmentsManager;
