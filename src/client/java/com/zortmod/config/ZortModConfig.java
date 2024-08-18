package com.zortmod.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;


@Config(name = "zortmod")
public class ZortModConfig implements ConfigData{

    public boolean global_enabled = true;
    public int global_x_pos = 4;
    public int global_y_pos = 4;

    public boolean temp_enabled = true;
    public int temp_x_pos = 4;
    public int temp_y_pos = 10;
    public boolean temp_centered = true;

    @ConfigEntry.ColorPicker
    public int color = 0xFFFFFF;

    @ConfigEntry.BoundedDiscrete(min = 0, max = 255)
    public int opacity = 255;

    public boolean shadow = false;
    public float scale = 1.0f;

    public boolean sob_over_pb = true;
    public int split_duration = 30;
    public float split_scale = 1.0f;


}
