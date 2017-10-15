package com.thesky.rpark.osilyzer;

import com.github.mikephil.charting.data.LineData;

public class PowerBalance {
    private String name;
    private float value;

    public PowerBalance() {
    }

    public PowerBalance(String name) {
        this.name = name;
    }

    public void setLineData(float value) {
        this.value = value;
    }

    public float getLineData() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}