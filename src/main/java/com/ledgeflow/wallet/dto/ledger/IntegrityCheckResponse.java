package com.ledgeflow.wallet.dto.ledger;

import java.time.LocalDateTime;
import java.util.List;

public class IntegrityCheckResponse {

    private boolean valid;
    private long totalEntriesChecked;
    private String statusMessage;
    private List<String> inconsistencies;
    private LocalDateTime checkedAt;

    public IntegrityCheckResponse() {
        this.checkedAt = LocalDateTime.now();
    }

    public IntegrityCheckResponse(boolean valid, long totalEntriesChecked, String statusMessage, List<String> inconsistencies) {
        this.valid = valid;
        this.totalEntriesChecked = totalEntriesChecked;
        this.statusMessage = statusMessage;
        this.inconsistencies = inconsistencies;
        this.checkedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public long getTotalEntriesChecked() {
        return totalEntriesChecked;
    }

    public void setTotalEntriesChecked(long totalEntriesChecked) {
        this.totalEntriesChecked = totalEntriesChecked;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public List<String> getInconsistencies() {
        return inconsistencies;
    }

    public void setInconsistencies(List<String> inconsistencies) {
        this.inconsistencies = inconsistencies;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}
