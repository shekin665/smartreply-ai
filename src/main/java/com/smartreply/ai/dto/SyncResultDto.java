package com.smartreply.ai.dto;

/** Response body for POST /api/emails/sync-real-inbox. */
public class SyncResultDto {

    private final int imported;

    public SyncResultDto(int imported) {
        this.imported = imported;
    }

    public int getImported() {
        return imported;
    }
}
