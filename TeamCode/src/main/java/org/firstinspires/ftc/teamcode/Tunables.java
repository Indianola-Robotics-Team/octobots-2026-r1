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
        public static double distanceSmoothSec = 0.3;                  // ease the speed into each new camera distance (0 = off): its wobble no longer restarts READY
        public static boolean lockSpeedWhileFiring = true;            // a shot that waits keeps its speed (the camera's distance cannot move it)
        public static double idleRpm = DEFAULT;                        // speed between shots, 0 = stop (0)
        public static double shotIntervalSec = DEFAULT;                // least time between two shots (0.25)
        public static double shotDropRpm = DEFAULT;                    // a drop of more than this below the target = a shot (150). A ball takes only ~70 off the weighted wheel: not seen, so each feed runs feedSec. Change it only if the R1 Wobble Tuner's Suggest line gives a number
        public static boolean sprayEnabled = true;                     // RB tap, then press and hold: fires as fast as it can (no speed wait); also the last 5 s of TeleOp
        public static double maxFiringQueue = DEFAULT;                 // fire presses that may wait (4; 0 = no queue: a press during a shot is ignored)
        public static double ejectRpm = 600;                           // Y double-tap: the low speed that pushes one POLLEN out of the robot (library 600). Tune: just clears the robot
        public static double ejectFeedSec = 0.6;                       // Y double-tap: the feed wheels run only this long, so one POLLEN goes out (feedSec is 1.05). Tune: 2 come out = less, none = more
        public static double kP = 10;                                    // speed control: push per speed error (10 = the SDK default and last year's; the Finder's 1.47 was too weak: slow to speed and slow to recover after a shot)
        public static double kI = 0;                                   // speed control: 0 (was 0.147): during the long spin-up it built up and pushed the wheel to about 3500 (aim 2600)
        public static double kD = 0;                                   // speed control: not used (the Flywheel Finder says kD = 0)
        public static double kF = 12.08;                               // speed control: steady power (2-weight wheel, 2026-11-21: Wobble Tuner avg +16 at 2630, +27 at 2530; 12.28 sat +55). With kI 0 this alone sets the steady speed: settles ABOVE the target = lower it, BELOW = raise it
        public static double nominalVolts = 13.0;                      // battery volts kF was found at (Flywheel Finder)
        public static boolean batteryCompensation = true;              // on: with kI 0, kF alone holds the speed, and kF's push changes with the battery (about 120 RPM a volt). Scales kF by nominalVolts / volts. Check READY on a full and a 12.2 V battery
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
        public static double toleranceDeg = 1.5;                       // aimed = within this many degrees (library 2): the CELL leaves less room from an angle
        public static boolean angleAim = false;                        // aim into the CELL's opening, not at its tags. OFF until tested on the robot (A in TeleOp turns it on to try; then set true here)
        public static double goalFrontIn = 3;                          // the opening is this far in front of the tags under it (from the field CAD)
        public static double cameraOffset = 0;                         // aim bias, degrees: + = the robot points more RIGHT. D-pad left/right change it 0.5 a press
    }
}
