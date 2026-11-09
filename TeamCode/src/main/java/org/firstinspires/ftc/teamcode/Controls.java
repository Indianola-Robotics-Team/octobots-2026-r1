package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.crossbotics.ftchelper.robot.ControlMap;
import com.crossbotics.ftchelper.robot.Pad;
import com.crossbotics.ftchelper.teleop.LibraryButtons;

/**
 * Robot 1, version 1 (file 3 of 5): the BUTTONS, and the TeleOp. Like goBILDA's code, one driver does
 * everything on gamepad 1 ({@code oneDriver = true}):
 * - left stick drives and strafes (sideways), right stick X turns. LT: slow while held. Left stick
 *   click: reverse drive on/off (the intake end is the front). Right stick click: auto-aim on/off (the
 *   robot turns to the HIVE; it turns on only with the HIVE in view; move the turn stick to turn by hand).
 *   The gamepad buzzes when the aim is close. Field-centric: hold RT, then click the right stick (not vetted yet)
 * - the intake runs once it is deployed (just after START). LB: intake on / off. Double-tap LB: push pieces
 *   out (the feed wheel turns backward too), until LB is tapped again (then it runs in).
 * - the flywheel spins from START and stays at speed, ready. B: switch it off / on. Off, RB still fires:
 *   the wheel spins up, fires, and switches off again
 * - RB with the HIVE in view and auto-aim off: the robot turns to it quickly, then fires (Tunables.Aim.aimOnFire;
 *   not while it sprays: SPRAY never waits)
 * - RB fires one POLLEN: the feeder wheel brings it to the feeder arm, and the arm pushes it into the wheel (it waits until the wheel is at
 *   speed; hold RB to keep firing until you let go). Tap RB, then press and hold it: SPRAY, as fast as it can (no wait for the
 *   speed), until you let go. The last 5 s of TeleOp spray too (Tunables.Launcher.sprayEnabled)
 * - D-pad up/down: flywheel speed +/- 50 RPM (Tunables.Launcher.stepRpm)
 * - D-pad right/left: the feeder arm pushes further / less, 0.01 a press (the screen shows the number for
 *   Tunables.FeederArm.push)
 * - X: push the intake over again (if it did not fall at START)
 * - Y, held 1/2 s: the camera's lighting test again (aim at a tag first). For a start with no tag in
 *   view. Up to 5 s, and the camera sees nothing until it is done
 * - both gamepads buzz at 1:00, 1:40 and 1:59 of TeleOp (1:00, 0:20 and 0:01 left)
 */
@TeleOp(name = "R1 TeleOp")
public class Controls extends ControlMap<Robot> {
    public static boolean oneDriver = true;

    // Always there: the library's buttons, on gamepad 1. Two are off on robot 1 (null = no button).
    @Override
    protected void libraryButtons(LibraryButtons lib, Pad g1, Pad g2) {
        lib.swapSticks(false);                   // left stick: drive and slide; right stick X: turn
        lib.slowToggle(null);                    // off: the left stick click is reverse drive (LT drives slowly, below)
        lib.fieldCentric(null);                  // off: field-centric is RT + right stick click (below)
        lib.pause(g1.back);                      // BACK: pause at the press; a tap goes on
        lib.resetForward(g1.back);               // BACK held 1 s: "forward" is where the robot faces now
        lib.debug(g1.start, g1.back);            // hold START, then press BACK: the debug lines
        lib.setPose(g1.start);                   // START held 1 s: set pose here (robots with odometry)
        lib.buttonMap(g1.guide);                 // GUIDE: the button map on the Driver Hub
        lib.tuningMenu(g1.guide);                // GUIDE held 1 s: the tuning menu (PRACTICE profile)
        lib.allianceInInit(g1.x, g1.b);          // in INIT: X = BLUE, B = RED
    }

    @Override
    protected void buttons(Robot robot, Pad g1, Pad g2) {
        g1.rb.hold(robot.launcher::fireWhileHeld, robot.launcher::ceaseFire)
                .says("fire one; hold: keep firing; tap, then hold: spray");   // the launcher's usual fire button
        // D-pad up / down (flywheel speed): still the launcher's own until the library release after v0.38.0.
        g1.lb.tapOrDoubleTap(() -> {                                            // intake: tap on/off, double-tap out
            if (robot.intake.isPushingOut()) robot.intake.in();
            else robot.intake.toggle();
        }, robot.intake::out).says("intake on/off; double-tap: out until tapped");
        g1.b.toggle(robot.launcher).says("flywheel on/off (off: RB spins it up to fire)");   // off: RB spins up, fires, stops
        g1.leftStickButton.toggle(robot.reverseDriveMode()).rumble().says("reverse drive on/off");   // 2 buzzes = on
        g1.rightStickButton.toggle(robot.aimMode()).rumble().says("auto-aim on/off");   // stays off with no HIVE in view
        g1.rumbleWhenAimed();                                                   // buzz while the aim is close
        g1.rt.with(g1.rightStickButton, () -> driving().toggleFieldCentric())
                .says("field-centric on/off (not vetted)");                     // hold RT, then click the right stick
        g1.dpadRight.press(() -> robot.launcher.feeder().adjustPush(+1)).says("feeder arm: push further");
        g1.dpadLeft.press(() -> robot.launcher.feeder().adjustPush(-1)).says("feeder arm: push less");
        g1.lt.slow();                                                           // LT: drive slowly while held
        g1.x.press(Actions.deployIntake(robot)).says("deploy intake").needs(robot.intakeDeploy);   // push the intake over again
        g1.y.heldFor(0.5, robot.camera::relight).rumble()
                .says("camera: lighting test again (aim at a tag)");            // held: a bump does not blind it
        g1.a.free();                                                            // free: replace free() with a job
        g1.rightStickY.free();                                                  // free: a stick, .use(...)
    }

    // Seconds LEFT when both gamepads buzz: 1:00, 1:40 and 1:59 into the 2:00 TeleOp.
    @Override
    protected double[] buzzAtSecondsLeft() {
        return new double[] {60, 20, 1};
    }
}
