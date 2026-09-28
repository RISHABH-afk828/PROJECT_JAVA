package com.example.hyperlocal.vendor.dto;

public class StoreStatusUpdateRequest {
    private Boolean manualOpenOverride; // null = use schedule, true = force open, false = force closed

    public Boolean getManualOpenOverride() {
        return manualOpenOverride;
    }

    public void setManualOpenOverride(Boolean manualOpenOverride) {
        this.manualOpenOverride = manualOpenOverride;
    }
}
