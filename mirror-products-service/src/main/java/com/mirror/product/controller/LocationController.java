package com.mirror.product.controller;

import com.mirror.product.dto.LocationRequest;
import com.mirror.product.dto.LocationResponse;
import com.mirror.product.entity.Location;
import com.mirror.product.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
@CrossOrigin(origins = "*")
public class LocationController {

    @Autowired
    private LocationService locationService;

    /**
     * Get all active locations
     * GET /api/locations
     */
    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAllLocations() {
        List<LocationResponse> locations = locationService.getAllActiveLocations();
        return ResponseEntity.ok(locations);
    }

    /**
     * Get all locations including inactive (admin only)
     * GET /api/locations/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<LocationResponse>> getAllLocationsIncludingInactive() {
        List<LocationResponse> locations = locationService.getAllLocations();
        return ResponseEntity.ok(locations);
    }

    /**
     * Get location by ID
     * GET /api/locations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getLocationById(@PathVariable String id) {
        return locationService.getLocationById(id)
                .map(location -> ResponseEntity.ok(location))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new location
     * POST /api/locations
     */
    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody LocationRequest request) {
        LocationResponse createdLocation = locationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLocation);
    }

    /**
     * Update an existing location
     * PUT /api/locations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<LocationResponse> updateLocation(
            @PathVariable String id,
            @Valid @RequestBody LocationRequest request) {
        return locationService.updateLocation(id, request)
                .map(location -> ResponseEntity.ok(location))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a location (soft delete)
     * DELETE /api/locations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteLocation(@PathVariable String id) {
        boolean deleted = locationService.deleteLocation(id);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                "message", "Location deactivated successfully",
                "id", id,
                "status", "INACTIVE"
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Hard delete a location (permanent removal)
     * DELETE /api/locations/{id}/permanent
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Map<String, Object>> hardDeleteLocation(@PathVariable String id) {
        boolean deleted = locationService.hardDeleteLocation(id);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                "message", "Location permanently deleted",
                "id", id
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Filter locations by city
     * GET /api/locations/city/{city}
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<List<LocationResponse>> getLocationsByCity(@PathVariable String city) {
        List<LocationResponse> locations = locationService.getLocationsByCity(city);
        return ResponseEntity.ok(locations);
    }

    /**
     * Filter locations by type
     * GET /api/locations/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<LocationResponse>> getLocationsByType(@PathVariable String type) {
        try {
            Location.LocationType locationType = Location.LocationType.valueOf(type.toUpperCase());
            List<LocationResponse> locations = locationService.getLocationsByType(locationType);
            return ResponseEntity.ok(locations);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Filter locations by city and type
     * GET /api/locations/filter?city={city}&type={type}
     */
    @GetMapping("/filter")
    public ResponseEntity<List<LocationResponse>> getLocationsByCityAndType(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type) {
        
        if (city != null && type != null) {
            try {
                Location.LocationType locationType = Location.LocationType.valueOf(type.toUpperCase());
                List<LocationResponse> locations = locationService.getLocationsByCityAndType(city, locationType);
                return ResponseEntity.ok(locations);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (city != null) {
            return getLocationsByCity(city);
        } else if (type != null) {
            return getLocationsByType(type);
        } else {
            return getAllLocations();
        }
    }

    /**
     * Search locations by text
     * GET /api/locations/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<List<LocationResponse>> searchLocations(@RequestParam("q") String searchTerm) {
        List<LocationResponse> locations = locationService.searchLocations(searchTerm);
        return ResponseEntity.ok(locations);
    }

    /**
     * Get available filter options
     * GET /api/locations/filters
     */
    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        Map<String, Object> filters = locationService.getFilterOptions();
        return ResponseEntity.ok(filters);
    }

    /**
     * Get location statistics
     * GET /api/locations/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLocationStatistics() {
        Map<String, Object> statistics = locationService.getLocationStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Activate a location
     * POST /api/locations/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateLocation(@PathVariable String id) {
        boolean activated = locationService.activateLocation(id);
        
        if (activated) {
            return ResponseEntity.ok(Map.of(
                "message", "Location activated successfully",
                "id", id,
                "status", "ACTIVE"
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate a location
     * POST /api/locations/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateLocation(@PathVariable String id) {
        boolean deactivated = locationService.deactivateLocation(id);
        
        if (deactivated) {
            return ResponseEntity.ok(Map.of(
                "message", "Location deactivated successfully",
                "id", id,
                "status", "INACTIVE"
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Health check endpoint
     * GET /api/locations/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        long totalLocations = locationService.getLocationStatistics()
                .entrySet()
                .stream()
                .filter(entry -> "totalActiveLocations".equals(entry.getKey()))
                .mapToLong(entry -> (Long) entry.getValue())
                .findFirst()
                .orElse(0L);

        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "service", "LocationService",
            "totalActiveLocations", totalLocations,
            "timestamp", System.currentTimeMillis()
        ));
    }
}