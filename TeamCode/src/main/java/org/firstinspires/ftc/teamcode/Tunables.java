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
        public static double stowed = MEASURE;                         // intake folded (Servo Finder)
        public static double deployed = MEASURE;                       // intake pushed over (Servo Finder)
        public static double travelSec = DEFAULT;                      // a full 0-to-1 move, seconds (servo's speed)
    }

    /** kP and kF are goBILDA's tested numbers for this robot (from their starter code); speedRpm is ours. */
    public static class Launcher {                                     // "launcher" in the configuration
        public static double speedRpm = 2275;                          // shot speed: 225 in/s at 65° in the simulator (goBILDA had 2679)
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

    public static class Feeder {                                       // "feeder" in the configuration
        public static double power = DEFAULT;                          // feeder power, 0 to 1 (1.0)
        public static double feedSec = DEFAULT;                        // run time for one piece (0.4)
    }

    /** The autonomous "R1 Launch and Park" (autos/LaunchAndPark.java). These ARE the route: no defaults. */
    public static class Auto {                                         // our own section: no part reads it
        public static double offWallInches = 4;                        // back off the wall before the turn
        public static double alongWallInches = 85.5;                   // along our wall to the LOADING ZONE
        public static double intoZoneInches = 12.6;                    // last move into the LOADING ZONE
        public static double slowPower = 0.4;                          // power for the short moves
        public static double drivePower = 0.6;                         // power for the long move
        public static double turnPower = 0.5;                          // power for the turns
        public static double parkSeconds = 10;                         // seconds kept for parking (it takes ~6 s)
    }
}
