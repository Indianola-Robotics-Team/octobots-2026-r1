package org.firstinspires.ftc.teamcode;

import static com.crossbotics.ftchelper.robot.Starting.*;

/**
 * Robot 1, version 1 (file 2 of 5): the NUMBERS, one section per part. A section's name is the part's
 * name in the robot configuration in CamelCase: "intake_deploy" reads IntakeDeploy.
 *
 * DEFAULT = "use the library's starting value" (the number in brackets in each comment). MEASURE =
 * "a number only our robot can give": INIT stops until we write it.
 * Change one only when you see the problem it fixes. Every setting, what it does and when to change it:
 * https://crossbotics.com/ftc-helper/docs/guides/tunables/ (start with "Find a setting by what you want").
 *
 * Tune on the robot with the tuning menu (PRACTICE profile: INIT, START, then hold GUIDE on gamepad 1
 * for 1 s): INIT lists every value that differs from this file, ready to copy back. This file stays the
 * record. The finders ("Tune and check", in the "Finders" group on the Driver Station) measure
 * the numbers and show the lines to copy here.
 */
public class Tunables {
    public static class Drive {                                        // a fixed name: always Drive
        public static double maxPower = DEFAULT;                       // top wheel power at events, 0 to 1 (1.0)
        public static double practiceMaxPower = DEFAULT;               // top wheel power in PRACTICE (the same as maxPower: full speed)
        public static double slowSpeed = DEFAULT;                      // top power with the slow trigger held (0.3)
        public static double strafeCorrection = 1.115;                 // strafing: more push sideways (Drive Tuner, power 0.5)
        // Strafing LEFT, if it slips more that way: Drive Tuner "Strafe left" test, then remove the // (not written = 1.115 both ways)
        // public static double strafeCorrectionLeft = 1.115;
        public static double turnKp = DEFAULT;                         // auto turns: push per degree off (0.012)
        public static double turnKd = DEFAULT;                         // auto turns: brake near the end (0.003)
        public static double moveMinPower = DEFAULT;                   // auto: least power that moves it (0.30)
        public static double slowDownIn = DEFAULT;                     // auto: slow down over at least the last N in of each move (0 = off; try 6)
        public static double turnMinPower = DEFAULT;                   // auto turns: least power while still off (0; Move Tuner)
        public static double turnToleranceDeg = DEFAULT;               // auto turns: done this close, in degrees (1.0; Move Tuner)
        public static double moveTurnScale = DEFAULT;                  // moveWithTurn: how hard it turns while it moves (0.60; Move Tuner)
        public static double moveMaxTurnPower = DEFAULT;               // moveWithTurn: most turn power (0.35; Move Tuner)
        public static boolean calibrated = true;                       // the Drive Tuner is done (auto needs it)
    }

    public static class Intake {                                       // "intake" in the configuration
        public static double inPower = DEFAULT;                        // power taking pieces in, 0 to 1 (1.0)
        public static double outPower = DEFAULT;                       // power pushing pieces out (0.7)
    }

    /** MEASURE: INIT stops until the Servo Finder gives these two numbers (a guess could break the servo). */
    public static class IntakeDeploy {                                 // "intake_deploy" in the configuration
        public static double stowed = 0.64;                            // intake folded (Servo Finder)
        public static double deployed = 0.5;                           // intake pushed over (Servo Finder)
        public static double travelSec = DEFAULT;                      // a full 0-to-1 move, seconds (servo's speed)
    }

    /** kP and kF are goBILDA's tested numbers for this robot (from their starter code); speedRpm is ours. */
    public static class Launcher {                                     // "launcher" in the configuration
        public static double speedRpm = 2630;                          // TeleOp and autos 2-3 shot speed (auto 1 has its own; goBILDA had 2679)
        public static double[][] speedByDistance = {{24,2530},{36,2630},{39,2840},{46,3120},{55,3150},{73,3340}};
        public static double stepRpm = 50;                             // speed change per D-pad press (library default 100)
        public static double readyBandRpm = 100;                       // "at speed" = this close to the target (library 60: at 3450 the wheel sat near 3429 and seldom fired)
        public static double idleRpm = DEFAULT;                        // speed between shots, 0 = stop (0)
        public static double shotIntervalSec = DEFAULT;                // least time between two shots (0.25)
        public static double shotDropRpm = DEFAULT;                    // a drop of more than this = a shot (150): misses shots = less
        public static boolean sprayEnabled = true;                     // RB tap, then press and hold: fires as fast as it can (no speed wait); also the last 5 s of TeleOp
        public static double maxFiringQueue = DEFAULT;                 // fire presses that may wait (4; 0 = no queue: a press during a shot is ignored)
        public static double kP = 1.47;                                  // speed control: push per speed error (goBILDA)
        public static double kI = 0.147;                                   // speed control: not used (goBILDA)
        public static double kD = 0;                                   // speed control: not used (the Flywheel Finder says kD = 0)
        public static double kF = 14.68;                               // speed control: steady power (Flywheel Finder, top speed 4784 RPM)
        public static double nominalVolts = 13.0;                      // battery volts kF was found at (Flywheel Finder)
        public static boolean batteryCompensation = false;             // off until proven on a real robot
    }

    public static class Feeder {                                       // "feeder_left" and "feeder_right" in the configuration
        public static double power = DEFAULT;                          // both wheels' power, -1 to 1 (1.0); below 0 = both turn the other way
        public static double feedSec = 1.05;                        // the most time the wheels run per shot (0.4): none goes = more
        public static boolean stopOnShot = true;                       // stop the wheels when the flywheel sees the shot (not in SPRAY)
    }

    public static class Webcam1 {                                      // "Webcam 1" in the configuration (the Logitech camera)
        public static boolean liveView = true;                         // its picture on the Driver Station (menu > Camera Stream, in INIT), PRACTICE only
        public static double pitchDeg = 36;                            // tilted 35 degrees up (to see the HIVE): the aim uses the floor's angle. TODO measure the mount
    }

    public static class Aim {                                          // a fixed name: always Aim (the library's defaults for the rest)
        public static boolean aimOnFire = true;                        // RB with the HIVE in view: turn to it, then fire
        public static double aimOnFireSec = 0.5;                       // the most RB waits for that turn before it fires anyway
        public static double aimDriveSpeed = 0.5;                      // aim mode (right stick click): drive and strafe at this share of full speed (1 = full)
    }
}
