package com.taxpilot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Phone is required") @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "Phone number is invalid") String phone,
        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must contain at least 8 characters") String password
) {
}
