package com.mirror.product.dto;

import com.mirror.product.entity.Location;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
public class LocationResponse {
    // Getters and Setters
    private String id;
    private String name;
    private Location.LocationType type;
    private String address;
    private String city;
    private CoordinatesDto coordinates;
    private String hours;
    private String phone;
    private Location.LocationStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public LocationResponse() {}

    public LocationResponse(Location location) {
        this.id = location.getId();
        this.name = location.getName();
        this.type = location.getType();
        this.address = location.getAddress();
        this.city = location.getCity();
        this.coordinates = new CoordinatesDto(location.getLatitude(), location.getLongitude());
        this.hours = location.getHours();
        this.phone = location.getPhone();
        this.status = location.getStatus();
        this.createdAt = location.getCreatedAt();
        this.updatedAt = location.getUpdatedAt();
    }

    // Nested class for coordinates (to match frontend expectations)
    public static class CoordinatesDto {
        private BigDecimal lat;
        private BigDecimal lng;

        public CoordinatesDto() {}

        public CoordinatesDto(BigDecimal lat, BigDecimal lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public BigDecimal getLat() {
            return lat;
        }

        public void setLat(BigDecimal lat) {
            this.lat = lat;
        }

        public BigDecimal getLng() {
            return lng;
        }

        public void setLng(BigDecimal lng) {
            this.lng = lng;
        }
    }

    @Override
    public String toString() {
        return "LocationResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", city='" + city + '\'' +
                ", status=" + status +
                '}';
    }
}