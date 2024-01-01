package org.nutrivision.bscs.capstone.detection.adapter;

public class NutritionItem {
    private String label;
    private String value;

    public NutritionItem(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}

