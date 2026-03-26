package com.mirror.product.dto.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MisaCustomerDto {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Tel")
    private String tel;

    @JsonProperty("NormalizedTel")
    private String normalizedTel;

    @JsonProperty("Addr")
    private String addr;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("Gender")
    private Integer gender;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("IdentifyNumber")
    private String identifyNumber;

    @JsonProperty("ProvinceAddr")
    private String provinceAddr;

    @JsonProperty("DistrictAddr")
    private String districtAddr;

    @JsonProperty("CommuneAddr")
    private String communeAddr;

    @JsonProperty("Birthday")
    private OffsetDateTime birthday;

    @JsonProperty("MembershipCode")
    private String membershipCode;

    @JsonProperty("MemberLevelID")
    private String memberLevelId;

    @JsonProperty("MemberLevelName")
    private String memberLevelName;

    @JsonProperty("CustomerCategoryID")
    private String customerCategoryId;

    @JsonProperty("CustomerCategoryName")
    private String customerCategoryName;

    @JsonProperty("LastSyncDate")
    private OffsetDateTime lastSyncDate;
}
