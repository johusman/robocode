package com.polopoly.win;

public class Deg {
    private final static double PI2 = Math.PI * 2;
    public final static double RAD = PI2 / 360.0;
    public enum Compass {
        North,
        South,
        East,
        West
    };
    
    private double degrees;
    
    public Deg(double degrees) {
        this.degrees = (degrees % 360 + 360) % 360;
    }
    
    public static Deg fromRad(double radians) {
        return new Deg(radians / RAD);
    }
    
    public double degrees() {
        return degrees;
    }
    
    public double degrees180() {
        return (degrees > 180) ? degrees - 360 : degrees;
    }
    
    public Deg plus(Deg term) {
        return new Deg(this.degrees + term.degrees);
    }
    
    public Deg minus(Deg term) {
        return new Deg(this.degrees - term.degrees);
    }
    
    public Deg flip() {
        return new Deg(degrees + 180);
    }
    
    public Deg times(double factor) {
        return new Deg(degrees * factor);
    }
    
    public Compass compass() {
        if (degrees <= 45 && degrees > 315) {
            return Compass.North;
        } else if (degrees <= 135 && degrees > 45) {
            return Compass.East;
        } else if (degrees <= 225 && degrees > 135) {
            return Compass.South;
        } else {
            return Compass.West;
        }
    }

    public double cos() {
        return Math.cos(degrees * RAD);
    }

    public double sin() {
        return Math.sin(degrees * RAD);
    }
    
    public static Deg arcsin(double value) {
        return new Deg(Math.asin(value) / RAD);
    }

    public static Deg arccos(double value) {
        return new Deg(Math.acos(value) / RAD);
    }
    
    @Override
    public String toString() {
        return String.format("%.1f°", degrees);
    }
}
