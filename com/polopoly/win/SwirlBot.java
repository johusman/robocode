package com.polopoly.win;
import robocode.*;
//import java.awt.Color;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * FirstTest - a robot by (your name here)
 */
public class SwirlBot extends AdvancedRobot
{
    private final static int PREFERRED_BEARING = -60;
    private volatile int firestate = 0;
    private volatile long lastTime = 0;
    private volatile boolean adjusting = false;
    private volatile boolean reversed = false;
    
	/**
	 * run: FirstTest's default behavior
	 */
	public void run() {
	    setAhead(50000);
		while(true) {
		    reversed = (getTime() & 0x40) == 0;
		    switch (firestate) {
		        case 0:
		            adjusting = false;
		            int diff = (int) Math.round(to180(getGunHeading() - getHeading()));
		            int preferred = reversed ? -PREFERRED_BEARING : PREFERRED_BEARING;
		            if (diff != preferred) {
		                System.out.println("heading: " + getHeading());
		                System.out.println("gun-heading: " + getGunHeading());
		                System.out.println("diff: " + diff);
		                System.out.println("Turning right: " + to180(preferred - diff));
		                adjusting = true;
		                turnGunRight(to180(preferred - diff));
		                adjusting = false;
		            }
		            if (!reversed) {
		                turnLeft(1);
		            } else {
		                turnRight(1);
		            }
		            break;
		        case 1:
		            adjusting = true;
		            if (getGunHeat() < 0.01) {
		                fire(1);
		                turnGunRight(10);		                
		                firestate = 2;
		            } else {
		                setAhead(-2);
		                turnGunLeft(1);
		            }
		            break;
                case 2:
                    adjusting = true;
                    if (getGunHeat() < 0.01) {
                        fire(1);
                        setAhead(50000);
                        firestate = 0;
                    } else {
                        turnGunRight(1);
                    }
                    break;
                default:
                    System.out.println("This shouldn't happen");
            }
		}
	}
	
	public double to180(double value) {
	    value = value % 360;
	    if (value > 180.0) {
	        value -= 360;
	    }
	    return value;
	}
	
	public boolean isCalm() {
	    return (getTime() - lastTime > 4000);
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
	    if (!adjusting && e.getDistance() < 700 || e.getDistance() < 200) {
    		fire(e.getDistance() < 10 ? 20 : 5);
    		setAhead(50);
    		turnGunLeft(5);
    		firestate = 1;
	    }
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
	    if (isCalm()) {
            setAhead(-50);
            if (Math.abs(e.getBearing()) < 30) {
                turnLeft(45);
            }
            lastTime = getTime();
            System.out.println("Time: " + lastTime);
	    } else {
	        System.out.println("Panic!");
	    }
	    setAhead(50000);
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {
        setAhead(-100);
        if (reversed) {
            turnRight(180);
        } else {
            turnLeft(180);
        }
        setAhead(50000);
	}
}
																		