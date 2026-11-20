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
 * - Distance: 0 to 96 inches, in 6 in steps (24 at the start).
 * - Direction: the way to drive, from the robot's front, counterclockwise like every angle:
 *   0 = forward, 90 = left, 180 = back, -90 = right, 45 = forward and to the left. No turn.
 * Place the robot so that line takes it where it must go. Measure it on the field first.
 * simulator/scenarios/r1-basic-move.sim checks it.
 */
@Autonomous(name = "R1 Basic Move", preselectTeleOp = "R1 TeleOp")
public class BasicMove extends Auto<Robot> {
    static final String DISTANCE = "Distance (in)";
    static final String DIRECTION = "Direction (deg)";

    @Override
    protected void steps(Steps s, Robot robot) {
        s.menuChoice(DISTANCE, "24", "30", "36", "42", "48", "54", "60", "66", "72", "78", "84", "90", "96",
                        "0", "6", "12", "18")
         .menuChoice(DIRECTION, "0 forward", "45 forward-left", "90 left", "135 back-left", "180 back",
                        "-135 back-right", "-90 right", "-45 forward-right");
        double inches = number(s.choice(DISTANCE));
        double directionDeg = number(s.choice(DIRECTION));
        s.start("Placed by hand: the chosen line takes it where it must go", 0);
        if (inches > 0) s.move("Move " + (int) inches + " in at " + (int) directionDeg + " deg", inches, directionDeg, 0.5);
    }

    // A reminder on the INIT screen while the drivers choose: which way + and - go.
    @Override
    protected void atInit(Robot robot) {
        robot.duringInit(() -> robot.show("Direction help", "90 = LEFT, -90 = RIGHT, 0 = forward, 180 = back (from the robot's front)"));
    }

    // The number at the start of a menu option: "-90 right" -> -90. Nothing chosen yet: 0.
    private static double number(String option) {
        if (option == null || option.isEmpty()) return 0;
        return Double.parseDouble(option.split(" ")[0]);
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
