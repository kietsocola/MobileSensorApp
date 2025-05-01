package com.example.sensorappmain;

public class Feature {
    private String name;
    private int iconResId;
    private Class<?> activityClass;

    public Feature(String name, int iconResId, Class<?> activityClass) {
        this.name = name;
        this.iconResId = iconResId;
        this.activityClass = activityClass;
    }

    public String getName() { return name; }
    public int getIconResId() { return iconResId; }
    public Class<?> getActivityClass() { return activityClass; }
}
