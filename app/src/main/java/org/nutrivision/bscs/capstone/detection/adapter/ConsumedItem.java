package org.nutrivision.bscs.capstone.detection.adapter;

public class ConsumedItem {
    private String date;
    private String food;

    public ConsumedItem(String date, String food) {
        this.date = date;
        this.food = food;
    }

    public String getFood() {
        return food;
    }

    public String getDate() {
        return date;
    }
}

