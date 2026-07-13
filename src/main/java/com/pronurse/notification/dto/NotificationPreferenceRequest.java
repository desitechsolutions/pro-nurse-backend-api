package com.pronurse.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification preference update request")
public class NotificationPreferenceRequest {

    @Schema(description = "Enable booking alerts", example = "true")
    private Boolean bookingAlerts;

    @Schema(description = "Enable payment alerts", example = "true")
    private Boolean paymentAlerts;

    @Schema(description = "Enable review alerts", example = "true")
    private Boolean reviewAlerts;

    @Schema(description = "Enable promotional alerts", example = "false")
    private Boolean promotionalAlerts;
}


