package com.polopoly.win;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import robocode.AdvancedRobot;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class DriveBy extends AdvancedRobot {
    
    private boolean searching = true;
    private Set<String> deadBots = new HashSet<String>();
    private RobotModel model;
    private long lastStill = 0;
    
    private long killShotCount = 0;
    private long fireCount = 0;
    private long missCount = 0;
    
    @Override
    public void run() {
        setBodyColor(new Color(240, 100, 0));
        setGunColor(new Color(50, 50, 50));
        setRadarColor(Color.black);
        setBulletColor(Color.orange);

        setAdjustRadarForGunTurn(true);
        while(true) {
            setAhead(50000);

            double targetDistance = Math.min(100.0, (getTime() - lastStill) * 2 + 30);
            
            long modelAge = (model == null) ? Long.MAX_VALUE : getTime() - model.updateTime; 
            
            searching = (model == null || modelAge > 4);
            
            if (searching) {
                setTurnRadarLeft(45);
            } else {
                Deg radarCorrection = d(getRadarHeading()).minus(model.pos.polarFrom(getPos()).deg);
                //System.out.println("Radar correction: " + radarCorrection);
                setTurnRadarLeft(radarCorrection.degrees180());
            }
            
            if (model != null) {
                Polar polar = model.pos.polarFrom(getPos());
                double velocity = (model.heading != null) ? Math.abs(model.heading.distance) : 8;
                
                Deg gunCorrection = d(getGunHeading()).minus(polar.deg);
                //System.out.println("Gun correction: " + gunCorrection);
                setTurnGunLeft(gunCorrection.degrees180());
                double gunError = Math.abs(gunCorrection.degrees180());
                if (getGunHeat() < 0.01 && gunError < 3.0) {
                    if(polar.distance < 150) {
                        System.out.println("Firing with error " + gunError + ", power " + ((150 - polar.distance) / 50 + 1));
                        fire((150 - polar.distance) / 50 + 1);
                        fireCount++;
                    } else if (velocity < 1) {
                        System.out.println("Kill shot! Velcity = " + velocity);
                        fire((400 - polar.distance) / 150 + 1);
                        killShotCount++;
                        fireCount++;
                    }
                }
                
                if (polar.distance > targetDistance) {
                    Deg targetHeading = Deg.arcsin(targetDistance / polar.distance).plus(polar.deg);
                    Deg heading = d(getHeading());
                    Deg correction = heading.minus(targetHeading);
                    //System.out.println("Distance: " + targetDistance + " model distance: " + polar.distance);
                    //System.out.println("Correction: " + correction.degrees180());
                    switch (correction.compass()) {
                    case South:
                        if (correction.degrees180() > 0) {
                            //System.out.println("South; turning right (" + correction.degrees180() + ")");
                            turnRight(90);
                        } else {
                            //System.out.println("South; turning left (" + correction.degrees180() + ")");
                            turnLeft(90);
                        }
                        break;
                    default:
                        setTurnLeft(correction.degrees180());
                    }
                }
            }
            execute();
        }
    }
    
    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        Deg heading = d(getHeading());
        Deg bearing = d(event.getBearing());
        Deg alpha = heading.plus(bearing);
        Polar robotPolar = new Polar(event.getDistance(), alpha);
        //System.out.println("SCAN: my pos: " + getPos());
        updateModel(event.getName(), robotPolar.coords(getPos()), event.getEnergy(), new Polar(event.getVelocity(), d(event.getHeading())));
    }
    
    @Override
    public void onBulletHit(BulletHitEvent event) {
        //System.out.println("BULLET: my pos: " + getPos());
        updateModel(event.getName(), new Coords(event.getBullet().getX(), event.getBullet().getY()), event.getEnergy(), null);
    }

    @Override
    public void onRobotDeath(RobotDeathEvent event) {
        String name = event.getName();
        deadBots.add(name);
        if (model != null && name.equals(model.name)) {
            System.out.println("Model death: " + name);
            model = null;
        }
    }
    
    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        Deg dev = d(event.getHeading()).minus(d(getHeading()));
        switch(dev.compass()) {
        case South:
            dev = dev.flip();
            // FALL-THRU
        case North:
            if (dev.degrees180() > 0) {
                turnLeft(20);
            } else {
                turnRight(20);
            }
            break;
        }
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        missCount++;
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        lastStill = getTime();
    }
    
    @Override
    public void onHitRobot(HitRobotEvent event) {
        if (model == null || !event.getName().equals(model.name)) {
            System.out.println("Hit robot bearing: " + event.getBearing());
            if (event.getBearing() > 0) {
                turnLeft(90 - event.getBearing());
                System.out.println("Turning left: " + (90 - event.getBearing()));
            } else {
                turnRight(90 + event.getBearing());
                System.out.println("Turning right: " + (90 + event.getBearing()));
            }
            ahead(20);
        } else {
            setTurnLeft(90);
        }
    }
    
    @Override
    public void onDeath(DeathEvent event) {
        System.out.println("DEATH");
        System.out.println("Fire: " + fireCount + " (killshot: " + killShotCount + "), misses: " + missCount);
    }
    
    @Override
    public void onWin(WinEvent event) {
        System.out.println("WIN");
        System.out.println("Fire: " + fireCount + " (killshot: " + killShotCount + "), misses: " + missCount);
    }
    
    public void updateModel(String name, Coords coords, double energy, Polar heading) {
        if (!deadBots.contains(name)) {
            if (model == null || name.equals(model.name) || energy < (model.energy / 2.0)) {
                //System.out.println("Updating model with pos: " + coords + " and polar: " + coords.polarFrom(getPos()));
                model = new RobotModel(name, coords, energy, getTime(), heading);
            } else {
                //System.out.println("Ignoring. Current robot: " + model.name);
            }
        }
    }

    public Deg d(double deg) {
        return new Deg(deg);
    }
    
    public Coords getPos() {
        return new Coords(getX(), getY());
    }
}
