package com.polopoly.win;

public class Polar {

    public double distance;
    public Deg deg;

    public Polar(double distance, Deg deg) {
        this.distance = distance;
        this.deg = deg;
    }
    
    public Coords coords() {
        return new Coords(distance * deg.sin(), distance * deg.cos());
    }
    
    public Coords coords(Coords origo) {
        return origo.plus(coords());
    }

    @Override
    public String toString() {
        return String.format("(%.1f|%s)", distance, deg);
    }
}
