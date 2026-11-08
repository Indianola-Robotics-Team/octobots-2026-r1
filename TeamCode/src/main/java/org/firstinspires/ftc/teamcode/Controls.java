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

    @Override
    protected void buttons(Robot robot, Pad g1, Pad g2) {
        // LB: one tap switches the intake on or off (back to running in if it was pushing out); a
        // double-tap pushes out until the next tap. (LB was the flywheel's: it is on B now.)
        g1.lb.tapOrDoubleTap(() -> {
            if (robot.intake.isPushingOut()) robot.intake.in();
            else robot.intake.toggle();
        }, robot.intake::out).says("intake on/off; double-tap: out until tapped");
        // B: the flywheel on / off. Off, a fire press still spins it up, fires, and switches it off again.
        g1.b.toggle(robot.launcher).says("flywheel on/off (off: RB spins it up to fire)");
        // Left stick click: reverse drive (two buzzes = on, one = off). (It was slow on/off: LT is slow.)
        g1.leftStickButton.toggle(robot.reverseDriveMode()).rumble().says("reverse drive on/off");
        // Right stick click: auto-aim on/off (two buzzes = on, one = off; no HIVE in view: it stays off).
        g1.rightStickButton.toggle(robot.aimMode()).rumble().says("auto-aim on/off");
        g1.rumbleWhenAimed();   // buzz while the aim is close: the closer, the stronger
        // Field-centric: hold RT (it has no job of its own), then click the right stick. Not vetted yet.
        g1.rt.with(g1.rightStickButton, () -> driving().toggleFieldCentric()).says("field-centric on/off (not vetted)");
        // D-pad right/left: a longer / shorter push of the feeder arm, for when the POLLEN stops getting all the way in.
        g1.dpadRight.press(() -> robot.launcher.feeder().adjustPush(+1)).says("feeder arm: push further");
        g1.dpadLeft.press(() -> robot.launcher.feeder().adjustPush(-1)).says("feeder arm: push less");
        g1.lt.slow();   // LT: drive slowly while held (not the intake)
        // X: push the intake over again. needs(...): off while the deploy servo is not on the robot.
        g1.x.press(Actions.deployIntake(robot)).says("deploy intake").needs(robot.intakeDeploy);
        // Y, held: the camera's lighting test again (aim at a tag first). Held, so a bump does not blind the camera.
        g1.y.heldFor(0.5, robot.camera::relight).rumble().says("camera: lighting test again (aim at a tag)");
    }

    // The library's field-centric button (the right stick click) is off: field-centric is RT + right stick click.
    // The library's slow on/off (the left stick click) is off: the left stick click is reverse drive.
    @Override
    protected void libraryButtons(LibraryButtons lib, Pad g1, Pad g2) {
        lib.fieldCentric(null);
        lib.slowToggle(null);   // the left stick click is reverse drive; LT still drives slowly while held
    }

    // Seconds LEFT when both gamepads buzz: 1:00, 1:40 and 1:59 into the 2:00 TeleOp.
    @Override
    protected double[] buzzAtSecondsLeft() {
        return new double[] {60, 20, 1};
    }
}
