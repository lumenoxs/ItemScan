package me.rpicpu.itemscan.data;

public enum ViolationCategory {
    REPAIR_COST("repairCost", "Invalid Repair Cost"),
    DUPE_UUID("dupeUuid", "Duplicate Item UUID"),
    SEVERE("severe", "Illegal Item");

    private final String configKey;
    private final String displayName;

    ViolationCategory(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    public String configKey() {
        return configKey;
    }

    public String displayName() {
        return displayName;
    }
}
