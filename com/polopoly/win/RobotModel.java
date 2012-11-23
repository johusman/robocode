package com.polopoly.win;

public class RobotModel {

    public String name;
    public long updateTime;
    public double energy;
    public Coords pos;
    public Polar heading;

    public RobotModel(String name, Coords pos, double health, long updateTime, Polar heading) {
        this.name = name;
        this.updateTime = updateTime;
        this.energy = health;
        this.pos = pos;
        this.heading = heading;
    }
}
