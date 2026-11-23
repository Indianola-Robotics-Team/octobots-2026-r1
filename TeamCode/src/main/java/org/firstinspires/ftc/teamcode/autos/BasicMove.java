package org.firstinspires.ftc.teamcode.autos;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.crossbotics.ftchelper.robot.Auto;
import com.crossbotics.ftchelper.robot.Steps;
import org.firstinspires.ftc.teamcode.Robot;

/**
 * Robot 1, the basic move: wait, then drive one straight line and stop. For a match where we only need to
 * get out of our partner's way, or into the park area. No shots.
 *
 * In INIT, choose with the D-pad (up/down = the line, left/right = change it):
 * - Start delay: 0 to 25 s (the wait after START).
 * - Distance: 0 to 96 inches, in 1 in steps (24 at the start). Hold left/right to step fast.
 * - Direction: the way to drive, from the robot's front, counterclockwise like every angle, in 5 degree
 *   steps: 0 = forward, 90 = left, 180 = back, -90 = right, 45 = forward and to the left. No turn.
 *   Hold left/right to step fast. The INIT screen says the direction in words.
 * Place the robot so that line takes it where it must go. Measure it on the field first.
 * simulator/scenarios/r1-basic-move.sim checks it.
 */
@Autonomous(name = "R1 Basic Move", preselectTeleOp = "R1 TeleOp")
public class BasicMove extends Auto<Robot> {
    static final String DISTANCE = "Distance (in)";
    static final String DIRECTION = "Direction (deg)";
    // The menu steps. Make them bigger if choosing takes too long.
    static final double DISTANCE_STEP_IN = 1, DIRECTION_STEP_DEG = 5;
    // The direction chosen now (the steps are read again each time the drivers change a line).
    private volatile double directionDeg;

    @Override
    protected void steps(Steps s, Robot robot) {
        s.menuNumber(DISTANCE, 0, 96, DISTANCE_STEP_IN, 24)
         .menuAngle(DIRECTION, DIRECTION_STEP_DEG, 0);
        double inches = s.number(DISTANCE);
        double directionDeg = s.number(DIRECTION);
        this.directionDeg = directionDeg;
        s.start("Placed by hand: the chosen line takes it where it must go", 0);
        if (inches > 0) s.move("Move " + (int) inches + " in at " + (int) directionDeg + " deg", inches, directionDeg, 0.5);
    }

    // A reminder on the INIT screen while the drivers choose: which way + and - go.
    @Override
    protected void atInit(Robot robot) {
        robot.duringInit(() -> {
            robot.show("Direction help", "90 = LEFT, -90 = RIGHT, 0 = forward, 180 = back (from the robot's front)");
            robot.show("Direction now", "drives " + words(directionDeg));
        });
    }

    // The direction in words: "forward-left", or "about forward-left" between the 8 main ways.
    static String words(double deg) {
        String[] names = {"forward", "forward-left", "left", "back-left", "back", "back-right", "right", "forward-right"};
        double d = ((deg % 360) + 360) % 360;
        int nearest = (int) Math.round(d / 45) % 8;
        return (Math.abs(d - Math.round(d / 45) * 45) < 1e-6 ? "" : "about ") + names[nearest];
    }

    // The longest start delay in the menu, seconds (the library's default is 10).
    @Override
    protected double maxStartDelaySec() {
        return 25;
    }

    // BIOBUZZ's field is turned half a turn between the alliances (not mirrored): the same move works for both.
    @Override
    protected boolean mirrorForRed() {
        return false;
    }
}
