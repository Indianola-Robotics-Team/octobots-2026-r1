package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.LogoFacingDirection;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.UsbFacingDirection;
import com.crossbotics.ftchelper.robot.Action;
import com.crossbotics.ftchelper.robot.Camera;
import com.crossbotics.ftchelper.robot.Drive;
import com.crossbotics.ftchelper.robot.Indexer;
import com.crossbotics.ftchelper.robot.Intake;
import com.crossbotics.ftchelper.robot.Launcher;
import com.crossbotics.ftchelper.robot.Positioner;
import com.crossbotics.ftchelper.robot.RobotDefinition;
import com.crossbotics.ftchelper.robot.Shape;

/**
 * Robot 1, version 1 (file 1 of 5): WHAT is on the robot. A goBILDA Strafer chassis (mecanum wheels: it
 * also drives sideways), a folding intake that a servo pushes over at START, tubing rollers, two corner
 * spinners, a flywheel fed by two Gecko feed wheels, and a Logitech webcam. POLLEN only. FRONT = the launcher (shots go up a front ramp).
 * Sizes and places in mm: forward, left, up from the chassis middle on the tiles, or from the shape it rides on.
 * "est." = an estimate (CAD or photos): measure ours. More: https://crossbotics.com/teams/8938-octobots/build/
 */
public class Robot extends RobotDefinition {
    public Positioner intakeDeploy;
    public Intake intake;
    public Launcher launcher;
    public Camera camera;

    /** Push the intake over right after START? An autonomous that starts against a wall sets it false. */
    public boolean deployAtStart = true;

    /** Spin the flywheel up right after START? A launching autonomous sets it true: the heavy wheel needs
     *  seconds to reach speed, so it must not start only when the first fire step begins. */
    public boolean spinAtStart = false;

    @Override
    protected void define() {
        // The drive: the Strafer's four 312 RPM motors and 104 mm GripForce mecanum wheels. Order: FL, FR, BL, BR.
        // The library reverses the left side (front_left, back_left): Tune and check > Motor Direction Check checks it.
        // "front_left" ... "imu" = names in the robot configuration (Driver Station), exactly.
        drive(Drive.mecanum("front_left", "front_right", "back_left", "back_right")
                .motor("gobilda-5203-312").wheel("gobilda-gripforce-mecanum-104").wheelMm(108.9)   // Drive Tuner, measured at power 0.5
                .wheelsAtMm(400, 336)   // track (left-right), wheelbase (front-back), wheel centres: est., measure ours
                // TODO check how our Control Hub is mounted: the way its logo and USB port face.
                .imu("imu", LogoFacingDirection.UP, UsbFacingDirection.BACKWARD));

        // The chassis box (length, width with the wheels, height) and the whole robot's height, mm. est.
        chassisMm(432, 440, 80);
        heightMm(305);

        // The deploy servo: pushes the folded intake over at START. optional(): the robot runs without it.
        // "intake_deploy" = the servo's name in the robot configuration. Its numbers: Tunables.IntakeDeploy.
        intakeDeploy = add(Positioner.servo("intake_deploy").part("gobilda-2000-speed").optional()
                .looks(Shape.arm().name("deploy_kicker").atMm(-120, -130, 150).sizeMm(50, 15, 15)
                        .angle("stowed", 90).angle("deployed", 150).note("est.")));

        // The intake, at the BACK: a 117 RPM motor. Its arm folds up (85°) and falls back onto its stop (220°; 180 = level back).
        // It has four shapes: each goes in a Java variable first; then looks(...) lists them.
        Shape intakeArm = Shape.arm().name("intake_arm").atMm(-120, 0, 150).sizeMm(95, 240, 15)
                .range(85, 220).startAt(85).dropsWhen(intakeDeploy, "deployed", 220).note("est.");
        // The main roller: soft tubing rollers, 90 and 60 mm, on a 240 mm shaft.
        Shape roller = Shape.roller("offset-tubing-90", "offset-tubing-60").name("roller").on("intake_arm")
                .atMm(88, 0, 0).lengthMm(240).soft().note("est.");
        // The spinners: 72 mm Boot-Wheels lying flat at the back corners, turned by the intake shaft.
        Shape leftSpinner = Shape.roller("gobilda-boot-72-rex").name("left_spinner").on("intake_arm")
                .atMm(139, 156, 47).axis("up").tiltDeg(-40).lengthMm(25).reversed().note("est.");
        Shape rightSpinner = Shape.roller("gobilda-boot-72-rex").name("right_spinner").on("intake_arm")
                .atMm(139, -156, 47).axis("up").tiltDeg(-40).lengthMm(25).note("est.");
        // "intake" = the motor's name in the robot configuration. Its numbers: Tunables.Intake.
        intake = add(Intake.motor("intake").part("gobilda-5203-117").holds(4)
                .looks(intakeArm, roller, leftSpinner, rightSpinner));

        // The camera: a Logitech webcam on a USB port. "Webcam 1" = its name in the robot configuration.
        // goal(...): the HIVE it looks for (our alliance's, chosen in INIT). autoExposure(): at INIT it tries a few
        // brightness settings and keeps the one that sees the most AprilTags. The right stick click aims with it (Controls.java).
        // It comes BEFORE the launcher: speedFromDistance(camera) needs it made first.
        // A camera missing from the configuration is a warning at INIT, not a stop. Its numbers: Tunables.Webcam1.
        camera = add(Camera.webcam("Webcam 1").autoExposure().goal("BLUE SCORING", "RED SCORING"));

        // The launcher: one 96 mm wheel, left of the middle. Shots leave up the front ramp. Robot 1 shoots POLLEN only.
        // The feeder: two CR servos (goBILDA Speed servos set to spin), one on each side of the POLLEN. Each turns
        // 48 mm Gecko wheels. They squeeze the POLLEN between them and drive it into the flywheel.
        // alsoServoReversed: both get the same power, and the right one spins the other way (both shafts up).
        // They run for Tunables.Feeder feedSec per shot. tunables("Feeder"): one section for both servos.
        // Its shapes: the flywheel's sliding mount and the wheel riding on it; the two feed wheels.
        // Every feeder place is est.: measure ours.
        // "launcher", "feeder_left", "feeder_right" = names in the robot configuration. Numbers: Tunables.Launcher, .Feeder.
        // optional(): the robot runs without them.
        Shape launcherSlide = Shape.slide().name("launcher_slide").atMm(40, 40, 120).sizeMm(30, 30, 48)
                .passive().travelMm(20).note("est.");
        Shape flywheel = Shape.flywheel("gobilda-hogback-96").on("launcher_slide").atMm(0, 0, 48);
        // The feed wheels: 48 mm Gecko wheels (soft 15A) lying flat, stacked on each CR servo's shaft.
        // The simulator turns both with feeder_left; reversed() draws the right one spinning the other way.
        Shape leftFeedWheels = Shape.roller("gobilda-gecko-48-rex-15a").name("left_feed_wheels").atMm(30, 40, 100)
                .axis("up").lengthMm(40).note("est.");
        Shape rightFeedWheels = Shape.roller("gobilda-gecko-48-rex-15a").name("right_feed_wheels").atMm(30, -40, 100)
                .axis("up").lengthMm(40).reversed().note("est.");
        launcher = add(Launcher.flywheel("launcher", "gobilda-5203-6000").reversed()
                .feeder(Indexer.crServo("feeder_left").alsoServoReversed("feeder_right").part("gobilda-2000-speed")
                        .reversed() // Reversed
                        .tunables("Feeder").optional().looks(leftFeedWheels, rightFeedWheels))
                .feedWheelOutWith(intake)   // the intake pushing out turns both feed wheels backward too
                .speedFromDistance(camera)   // wheel speed from Tunables.Launcher speedByDistance and the camera's distance
                .spinAtStart()   // at speed from START and between shots, ready to fire (B switches it off)
                .looks(launcherSlide, flywheel)
                .shotAngle(67));   // 67° up from the floor, out of the front: the printed A2 curved hood (octobots-2026 r1_shooter)

        // Parts that do not move: the tower in front of the launcher, and our ramp out of the front (est.).
        fixed(Shape.box("tower").atMm(88, 0, 40).sizeMm(48, 184, 264),
              Shape.box("ramp").atMm(180, 0, 200).sizeMm(120, 150, 10).tiltDeg(67));   // the A2 hood's exit, 67° up toward the front, like the shots (est. place)

        // Right after START, push the intake over (the library moves nothing in INIT: its safety choice).
        // Not now (an autonomous with its back on the wall): hold the servo at "stowed" from START. A servo with
        // no command has no power, so it cannot hold the folded intake, and the intake fell over backward.
        // A launching autonomous (spinAtStart) also starts the flywheel here.
        atStart(() -> deployAtStart ? Actions.deployIntake(this)
                : Action.named("Intake stays folded (the autonomous deploys it)")
                        .then(() -> intakeDeploy.goTo("stowed"), () -> { if (spinAtStart) launcher.spinUp(); }));
    }
}
