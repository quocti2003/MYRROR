package com.mirror.product.mapper;

import com.mirror.product.dto.AppointmentRequest;
import com.mirror.product.dto.AppointmentResponse;
import com.mirror.product.entity.Appointment;
import com.mirror.product.entity.Location;
import com.mirror.product.enums.AppointmentStatus;
import com.mirror.product.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AppointmentMapper {

    private final LocationRepository locationRepository;

    public Appointment toEntity(AppointmentRequest request) {
        if (request == null) {
            return null;
        }

        return Appointment.builder()
                .customerTitle(request.getCustomerTitle())
                .customerFirstName(request.getCustomerFirstName())
                .customerLastName(request.getCustomerLastName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .venueId(request.getVenueId())
                .service(request.getService())
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .preferences(request.getPreferences())
                .status(AppointmentStatus.PENDING)
                .build();
    }

    public AppointmentResponse toResponse(Appointment entity) {
        if (entity == null) {
            return null;
        }

        AppointmentResponse response = new AppointmentResponse();
        response.setId(entity.getId());
        response.setCustomerTitle(entity.getCustomerTitle());
        response.setCustomerFirstName(entity.getCustomerFirstName());
        response.setCustomerLastName(entity.getCustomerLastName());
        response.setCustomerEmail(entity.getCustomerEmail());
        response.setCustomerPhone(entity.getCustomerPhone());
        response.setVenueId(entity.getVenueId());
        response.setService(entity.getService());
        response.setAppointmentDate(entity.getAppointmentDate());
        response.setAppointmentTime(entity.getAppointmentTime());
        response.setLanguage(entity.getLanguage());
        response.setPreferences(entity.getPreferences());
        response.setStatus(entity.getStatus());
        response.setCancellationReason(entity.getCancellationReason());
        response.setStaffNotes(entity.getStaffNotes());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        // Load venue details
        loadVenueDetails(entity, response);

        return response;
    }

    public List<AppointmentResponse> toResponseList(List<Appointment> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntity(Appointment entity, AppointmentRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getCustomerTitle() != null) {
            entity.setCustomerTitle(request.getCustomerTitle());
        }
        if (request.getCustomerFirstName() != null) {
            entity.setCustomerFirstName(request.getCustomerFirstName());
        }
        if (request.getCustomerLastName() != null) {
            entity.setCustomerLastName(request.getCustomerLastName());
        }
        if (request.getCustomerEmail() != null) {
            entity.setCustomerEmail(request.getCustomerEmail());
        }
        if (request.getCustomerPhone() != null) {
            entity.setCustomerPhone(request.getCustomerPhone());
        }
        if (request.getVenueId() != null) {
            entity.setVenueId(request.getVenueId());
        }
        if (request.getService() != null) {
            entity.setService(request.getService());
        }
        if (request.getAppointmentDate() != null) {
            entity.setAppointmentDate(request.getAppointmentDate());
        }
        if (request.getAppointmentTime() != null) {
            entity.setAppointmentTime(request.getAppointmentTime());
        }
        if (request.getLanguage() != null) {
            entity.setLanguage(request.getLanguage());
        }
        if (request.getPreferences() != null) {
            entity.setPreferences(request.getPreferences());
        }
    }

    private void loadVenueDetails(Appointment entity, AppointmentResponse response) {
        String venueId = entity.getVenueId();
        if (venueId == null || venueId.trim().isEmpty()) {
            return;
        }

        try {
            locationRepository.findById(venueId).ifPresent(location -> {
                response.setVenueName(location.getName());
                response.setVenueAddress(location.getAddress());
                response.setVenueCity(location.getCity());
            });
        } catch (Exception e) {
            // Log error but don't fail the mapping
        }
    }
}
