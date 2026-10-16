package com.smartreply.ai.dto;

public class DashboardStatsDto {

    private final long inboxCount;
    private final long unreadCount;
    private final long sentCount;
    private final long draftsCount;
    private final long spamCount;
    private final int repliesGenerated;
    private final int timeSavedMinutes;

    public DashboardStatsDto(long inboxCount, long unreadCount, long sentCount, long draftsCount,
                              long spamCount, int repliesGenerated, int timeSavedMinutes) {
        this.inboxCount = inboxCount;
        this.unreadCount = unreadCount;
        this.sentCount = sentCount;
        this.draftsCount = draftsCount;
        this.spamCount = spamCount;
        this.repliesGenerated = repliesGenerated;
        this.timeSavedMinutes = timeSavedMinutes;
    }

    public long getInboxCount() {
        return inboxCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public long getSentCount() {
        return sentCount;
    }

    public long getDraftsCount() {
        return draftsCount;
    }

    public long getSpamCount() {
        return spamCount;
    }

    public int getRepliesGenerated() {
        return repliesGenerated;
    }

    public int getTimeSavedMinutes() {
        return timeSavedMinutes;
    }
}
