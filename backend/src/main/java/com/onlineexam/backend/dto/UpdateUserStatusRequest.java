
package com.onlineexam.backend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(

        @NotNull(message = "Account status is required")
        Boolean enabled

) {
}
