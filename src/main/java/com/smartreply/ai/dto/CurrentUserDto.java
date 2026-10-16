package com.smartreply.ai.dto;

/** The signed-in Google account, as shown in the dashboard top bar. */
public class CurrentUserDto {

    private final String email;
    private final String displayName;
    private final String pictureUrl;

    public CurrentUserDto(String email, String displayName, String pictureUrl) {
        this.email = email;
        this.displayName = displayName;
        this.pictureUrl = pictureUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }
}
