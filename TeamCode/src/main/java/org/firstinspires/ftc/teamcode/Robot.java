package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.LogoFacingDirection;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.UsbFacingDirection;
import com.crossbotics.ftchelper.robot.Action;
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
 * spinners, and a flywheel fed by Gecko wheels. POLLEN only. Shots leave up a ramp out of the BACK.
 * Sizes and places in mm: forward, left, up from the chassis middle on the tiles, or from the shape it rides on.
 * "est." = an estimate (CAD or photos): measure ours. More: https://crossbotics.com/teams/8938-octobots/build/
 */
public class Robot extends RobotDefinition {
    public Positioner intakeDeploy;
    public Intake intake;
    public Launcher launcher;

    /** Push the intake over right after START? An autonomous that starts against a wall sets it false. */
    public boolean deployAtStart = true;

    @Override
    protected void define() {
        // The drive: the Strafer's four 312 RPM motors and 104 mm GripForce mecanum wheels. Order: FL, FR, BL, BR.
        // The library reverses the left side (front_left, back_left): R1 Motor Direction Check checks it.
        // "front_left" ... "imu" = names in the robot configuration (Driver Station), exactly.
        drive(Drive.mecanum("front_left", "front_right", "back_left", "back_right")
                .motor("gobilda-5203-312").wheel("gobilda-gripforce-mecanum-104")
                .wheelsAtMm(400, 336)   // track (left-right), wheelbase (front-back), wheel centres: est., measure ours
                // TODO check how our Control Hub is mounted: the way its logo and USB port face.
                .imu("imu", LogoFacingDirection.UP, UsbFacingDirection.FORWARD));

        // The chassis box (length, width with the wheels, height) and the whole robot's height, mm. est.
        chassisMm(432, 440, 80);
        heightMm(305);

        // The deploy servo: pushes the folded intake over at START. optional(): the robot runs without it.
        // "intake_deploy" = the servo's name in the robot configuration. Its numbers: Tunables.IntakeDeploy.
        intakeDeploy = add(Positioner.servo("intake_deploy").part("gobilda-2000-speed").optional()
                .looks(Shape.arm().name("deploy_kicker").atMm(120, 130, 150).sizeMm(50, 15, 15)
                        .angle("stowed", 90).angle("deployed", 30).note("est.")));

        // The intake: a 117 RPM motor. Its arm folds up (95 degrees) and falls onto its stop (-40).
        // It has four shapes: each goes in a Java variable first; then looks(...) lists them.
        Shape intakeArm = Shape.arm().name("intake_arm").atMm(120, 0, 150).sizeMm(95, 240, 15)
                .range(-40, 95).startAt(95).dropsWhen(intakeDeploy, "deployed", -40).note("est.");
        // The main roller: soft tubing rollers, 90 and 60 mm, on a 240 mm shaft.
        Shape roller = Shape.roller("offset-tubing-90", "offset-tubing-60").name("roller").on("intake_arm")
                .atMm(88, 0, 0).lengthMm(240).soft().note("est.");
        // The spinners: 72 mm Boot-Wheels lying flat at the front corners, turned by the intake shaft.
        Shape leftSpinner = Shape.roller("gobilda-boot-72-rex").name("left_spinner").on("intake_arm")
                .atMm(139, 156, -47).axis("up").tiltDeg(40).lengthMm(25).reversed().note("est.");
        Shape rightSpinner = Shape.roller("gobilda-boot-72-rex").name("right_spinner").on("intake_arm")
                .atMm(139, -156, -47).axis("up").tiltDeg(40).lengthMm(25).note("est.");
        // "intake" = the motor's name in the robot configuration. Its numbers: Tunables.Intake.
        intake = add(Intake.motor("intake").part("gobilda-5203-117").holds(4)
                .looks(intakeArm, roller, leftSpinner, rightSpinner));

        // The launcher: one 96 mm wheel. Shots leave up the back ramp. Robot 1 shoots POLLEN only.
        // The "feeder" CR servo (a goBILDA Speed servo set to spin) turns Gecko wheels that push the POLLEN into the wheel; optional(): the robot runs without it.
        // Two shapes: its sliding mount and the wheel riding on it.
        // "launcher", "feeder" = names in the robot configuration. Numbers: Tunables.Launcher, .Feeder.
        Shape launcherSlide = Shape.slide().name("launcher_slide").atMm(-40, -5, 120).sizeMm(30, 30, 48)
                .passive().travelMm(20).note("est.");
        Shape flywheel = Shape.flywheel("gobilda-hogback-96").on("launcher_slide").atMm(0, 0, 48);
        launcher = add(Launcher.flywheel("launcher", "gobilda-5203-6000")
                .feeder(Indexer.crServo("feeder").part("gobilda-2000-speed").reversed().optional())
                .spinAtStart()   // at speed from START and between shots, ready to fire (LB lets it coast to park)
                .looks(launcherSlide, flywheel)
                .shotAngle(115));   // 65° up from the floor, out of the back (the planned ramp; the built one is 53°)

        // Parts that do not move: the tower behind the launcher, and our ramp out of the back (est.).
        fixed(Shape.box("tower").atMm(-88, 0, 40).sizeMm(48, 184, 264),
              Shape.box("ramp").atMm(-180, 0, 200).sizeMm(120, 150, 10).tiltDeg(-65));   // 65° up from the floor toward the back (planned; built at 53°), like the shots (115°)

        // Right after START, push the intake over (the library moves nothing in INIT: its safety choice).
        atStart(() -> deployAtStart ? Actions.deployIntake(this)
                : Action.named("Intake stays folded (the autonomous deploys it)"));
    }
}
