package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.crossbotics.ftchelper.robot.ControlMap;
import com.crossbotics.ftchelper.robot.Pad;

/**
 * Robot 1, version 1 (file 3 of 5): the BUTTONS, and the TeleOp. Like goBILDA's code, one driver does
 * everything on gamepad 1 ({@code oneDriver = true}):
 * - left stick drives and strafes (sideways), right stick X turns. LT: slow while held (left stick
 *   click: slow on/off). Right stick double-click: field-centric on/off
 * - the intake runs once it is deployed (just after START). LB: intake on / off. Double-tap LB: push pieces
 *   out (the feed wheel turns backward too), until LB is tapped again (then it runs in).
 * - the flywheel spins from START and stays at speed, ready. B: switch it off / on. Off, RB still fires:
 *   the wheel spins up, fires, and switches off again
 * - RB fires one POLLEN: the feeder wheel brings it to the feeder arm, and the arm pushes it into the wheel (it waits until the wheel is at
 *   speed; hold RB to keep firing until you let go)
 * - D-pad up/down: flywheel speed +/- 50 RPM (Tunables.Launcher.stepRpm)
 * - X: push the intake over again (if it did not fall at START)
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
        g1.rt.free();   // the intake is on LB
        g1.lt.slow();   // LT: drive slowly while held (not the intake)
        // X: push the intake over again. needs(...): off while the deploy servo is not on the robot.
        g1.x.press(Actions.deployIntake(robot)).says("deploy intake").needs(robot.intakeDeploy);
    }

    // Seconds LEFT when both gamepads buzz: 1:00, 1:40 and 1:59 into the 2:00 TeleOp.
    @Override
    protected double[] buzzAtSecondsLeft() {
        return new double[] {60, 20, 1};
    }
}
