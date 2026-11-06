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
        public static double practiceMaxPower = DEFAULT;               // top wheel power in PRACTICE (0.6)
        public static double slowSpeed = DEFAULT;                      // top power with the slow trigger held (0.3)
        public static double strafeCorrection = DEFAULT;               // strafing: more push sideways (1.15; the Drive Tuner finds it)
        public static double turnKp = DEFAULT;                         // auto turns: push per degree off (0.012)
        public static double turnKd = DEFAULT;                         // auto turns: brake near the end (0.003)
        public static double moveMinPower = DEFAULT;                   // auto: least power that moves it (0.30)
        public static boolean calibrated = false;                      // set true after the Drive Tuner (auto needs it)
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
        public static double speedRpm = 2525;                          // TeleOp shot speed (each auto writes its own; goBILDA had 2679)
        public static double stepRpm = DEFAULT;                        // speed change per D-pad press (100)
        public static double readyBandRpm = DEFAULT;                   // "at speed" = this close to the target (60)
        public static double idleRpm = DEFAULT;                        // speed between shots, 0 = stop (0)
        public static double shotIntervalSec = DEFAULT;                // least time between two shots (0.25)
        public static double kP = 40;                                  // speed control: push per speed error (goBILDA)
        public static double kI = 0;                                   // speed control: not used (goBILDA)
        public static double kD = 0;                                   // speed control: not used (goBILDA)
        public static double kF = 12.5;                                // speed control: steady power (goBILDA)
        public static double nominalVolts = DEFAULT;                   // battery volts kF was found at (12.8)
        public static boolean batteryCompensation = false;             // off until proven on a real robot
    }

    public static class FeederArm {                                    // "feeder_arm" in the configuration
        public static double rest = 0.5;                               // arm down: its pads squeeze the POLLEN against the feed wheels (sets the gap: Servo Finder, step 0.001)
        public static double push = 0.4;                               // arm tilted up: the POLLEN is in the flywheel (Servo Finder)
        public static double pushSec = DEFAULT;                        // time to push up (the torque servo's catalog time)
        public static double returnSec = DEFAULT;                      // time to come back down (the same)
    }

    public static class FeederWheel {                                  // "feeder_wheel" in the configuration
        public static double power = DEFAULT;                          // wheel power, 0 to 1 (1.0)
        public static boolean alwaysOn = false;                        // true = runs all the time the flywheel is on
        public static double stageSec = 0.5;                           // only while firing: runs this long before the arm pushes (0.2)
        public static double stopSec = DEFAULT;                        // only while firing: stops this long after the push starts, below 0 = before it (0.3)
    }
}
