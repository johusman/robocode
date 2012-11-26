package com.polopoly.win;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import robocode.AdvancedRobot;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class DriveBy2 extends AdvancedRobot {
    
    private boolean searching = true;
    private Set<String> deadBots = new HashSet<String>();
    private RobotModel model;
    private long lastStill = 0;
    private boolean flipped = false;
    private long lastMoving = 0;
    private long lastFlipped = 0;
    private List<QueuedMovement> queue = Collections.synchronizedList(new LinkedList<QueuedMovement>());
    
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
            if (queue.isEmpty()) {
                setAhead(50000);
            }
            
            if (getVelocity() > 1.0) {
                lastMoving = getTime();
            }
            
            if (getTime() - lastMoving > 30) {
                tryFlip();
                lastMoving = Long.MAX_VALUE;
            }

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
                double velocity = model.heading.distance;
                double sideVelocity = velocity * model.heading.deg.minus(polar.deg).sin();
                
                double firePower = (polar.distance > 150) ? (400 - polar.distance) / 150 + 1 : (150 - polar.distance) / 50 + 1; 
                
                Deg gunCorrection = d(getGunHeading()).minus(calcTargetAngle(model, firePower));
                //System.out.println("Gun correction: " + gunCorrection);
                setTurnGunLeft(gunCorrection.degrees180());
                double gunError = Math.abs(gunCorrection.degrees180());
                if (getGunHeat() < 0.01 && gunError < 3.0) {
                    if(polar.distance < 150) {
                        //System.out.println("Firing with error " + gunError + ", power " + ((150 - polar.distance) / 50 + 1));
                        fire((150 - polar.distance) / 50 + 1);
                        fireCount++;
                    } else if (sideVelocity < 0.1) {
                        //System.out.println("Kill shot! Velcity = " + velocity);
                        fire((400 - polar.distance) / 150 + 1);
                        killShotCount++;
                        fireCount++;
                    }
                }
                
                if (!queue.isEmpty()) {
                    QueuedMovement movement = queue.get(0);
                    if (!movement.isStarted()) {
                        movement.start();
                        System.out.println("Starting movement");
                    } else if (movement.isDone()) {
                        queue.remove(0);
                        System.out.println("Movement done");
                    }
                } else {
                    if (polar.distance > targetDistance) {
                        Deg targetHeading = Deg.arcsin((flipped ? -1.0 : 1.0) * targetDistance / polar.distance).plus(polar.deg);
                        Deg heading = d(getHeading());
                        Deg correction = heading.minus(targetHeading);
                        //System.out.println("Distance: " + targetDistance + " model distance: " + polar.distance);
                        //System.out.println("Correction: " + correction.degrees180());
                        switch (correction.compass()) {
                        case South:
                            //if (correction.degrees180() > 0) {
                            //    System.out.println("South; turning right (" + correction.degrees180() + ") (me: " + getPos() + ", it: " + model.pos + ")");
                            //    queueTurnRight(90);
                            //} else {
                                System.out.println("South; turning left (" + correction.degrees180() + ") (me: " + getPos() + ", it: " + model.pos + ")");
                                if (flipped) {
                                    queueTurnRight(90);
                                } else {
                                    queueTurnLeft(90);
                                }
                            //}
                            setAhead(0);
                            break;
                        default:
                            setTurnLeft(correction.degrees180());
                        }
                    }
                }
            }
            execute();
        }
    }

    private Deg calcTargetAngle(RobotModel model, double firepower) {
        Polar robotVector = model.pos.polarFrom(getPos());
        Deg angleToRobot = robotVector.deg;
        
        Deg beta = d(180).plus(angleToRobot).minus(model.heading.deg);
        double Ve = model.heading.distance;
        double Vb = 20.0 - 3.0 * Math.min(firepower, 3.0);
        //System.out.println("beta = " + beta);
        //System.out.println("Ve = " + Ve);
        //System.out.println("Vb = " + Vb);
        
        Deg alpha = Deg.arcsin(Ve/Vb * beta.sin());
        Deg targetDeg = angleToRobot.plus(alpha);
        
        //System.out.println("Original: " + robotVector.deg + ", revised: " + targetDeg);
        
        return targetDeg;
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
    public void onRobotDeath(RobotDeathEvent event) {
        String name = event.getName();
        deadBots.add(name);
        if (model != null && name.equals(model.name)) {
            //System.out.println("Model death: " + name);
            model = null;
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
        if (queue.isEmpty()) {
            if (model == null || !event.getName().equals(model.name)) {
                System.out.println("Hit robot bearing: " + event.getBearing());
                if (event.getBearing() > 0) {
                    double turn = Math.max(Math.min(90 - event.getBearing(), 45), -45);
                    queueTurnLeft(turn);
                    System.out.println("Turning left: " + turn);
                } else {
                    double turn = Math.max(Math.min(90 + event.getBearing(), 45), -45);
                    queueTurnRight(turn);
                    System.out.println("Turning right: " + turn);
                }
                queueAhead(20);
            } else {
                System.out.println("Hit target robot, turning right 90");
                queueTurnRight(90);
            }
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
    
    public void queueTurnLeft(final double degrees) {
        System.out.println("Queueing turn left " + degrees);
        queue.add(new QueuedMovement() {
            public void start() {
                super.start();
                setTurnLeft(degrees);
            }
            
            boolean isDone() {
                return Math.abs(getTurnRemaining()) < 0.1;
            }
        });
    }

    public void queueTurnRight(final double degrees) {
        System.out.println("Queueing turn right " + degrees);
        queue.add(new QueuedMovement() {
            public void start() {
                super.start();
                setTurnRight(degrees);
            }
            
            boolean isDone() {
                return Math.abs(getTurnRemaining()) < 0.1;
            }
        });
    }

    
    public void queueAhead(final double distance) {
        System.out.println("Queueing ahead " + distance);
        queue.add(new QueuedMovement() {
            public void start() {
                super.start();
                setAhead(distance*2);
            }
            
            boolean isDone() {
                return Math.abs(getDistanceRemaining()) < distance;
            }
        });
    }
    
    public void tryFlip() {
        if (getTime() - lastFlipped > 50) {
            flipped = !flipped;
            lastFlipped = getTime();
            System.out.println("flip!");
            queueTurnLeft(180);
        }
    }

}
