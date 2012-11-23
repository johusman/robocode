package com.polopoly.win;

public class Coords {
    public double x;
    public double y;

    public Coords(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Coords plus(Coords coords) {
        return new Coords(this.x + coords.x, this.y + coords.y);
    }
    
    public Polar polarFrom(Coords origo) {
        double dx = x - origo.x;
        double dy = y - origo.y;
        double distance = Math.sqrt(dx*dx + dy*dy);
        double radians = Math.atan2 (dy, dx);
        return new Polar(distance, new Deg(90).minus(Deg.fromRad(radians)));
    }
    
    @Override
    public String toString() {
        return String.format("(%.1f, %.1f)", this.x, this.y);
    }
}
