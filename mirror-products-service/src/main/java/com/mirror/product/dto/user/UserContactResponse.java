package com.mirror.product.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserContactResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
