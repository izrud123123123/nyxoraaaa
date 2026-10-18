package com.nyxora.client.settings;

/** Sidebar categories that own settings. Profiles/Settings pages are UI-only and hold no settings here. */
public enum Category {
    SKY("Sky"),
    PARTICLES("Particles"),
    COMBAT("Combat Effects"),
    MOTION("Motion"),
    INTERFACE("Interface");

    private final String displayName;

    Category(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}
