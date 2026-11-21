package org.firstinspires.ftc.teamcode.autos;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.crossbotics.ftchelper.robot.Auto;
import com.crossbotics.ftchelper.robot.Steps;
import org.firstinspires.ftc.teamcode.Actions;
import org.firstinspires.ftc.teamcode.Robot;

/**
 * Robot 1, auto 4: from the LEFT corner of our wall, wait until the camera sees our HIVE's up CELL (our partner's
 * shots tip the HIVE), aim at it, fire the four preloaded POLLEN, then park beside the left corner of our
 * LOADING ZONE (LEAVE 3 + PARK 5).
 *
 * Why wait: from the left, the CELL that faces us is the DOWN one at the start. Our partner's shots tip the
 * HIVE; then that CELL is up. Its tags then read right side up, and only then is it the camera's goal
 * ("RED SCORING" / "BLUE SCORING" is whichever CELL is up). No tip: it never fires, and it still parks.
 *
 * Start (G304: touching the wall, our half, not in the LOADING ZONE): in the left corner, turned so its back
 * corner points into the field's corner (it touches both walls), the front (the ramp) pointing at the HIVE's
 * left CELL (heading -44.7). The LOADING ZONE is just to the right.
 *
 * Route: wait for the goal (no time limit: until the park time), turn to it with the camera, fire two (the
 * speed from the shot table and the camera's distance, about 64 in), bump twice (3 in out fast, back slowly into the
 * corner) so the last two roll into the feeder, fire them. parkWithSeconds(4): the last 4 s are for the park,
 * so a late tip still leaves time to park (the shots are cut short instead). Then it pushes the intake over
 * while it drives 12 in out of the corner, turning to face right: only its corner is over the zone's corner,
 * which is a PARK, and the zone stays free.
 * Plain numbers, the way the auto builder writes them, so the builder can open this file ("Start from
 * one of your autos") and show it on the map. moveWithTurn's direction and heading count from the START
 * heading, counterclockwise like every angle (90 = left, -90 = right).
 * simulator/scenarios/r1-auto-left-corner.sim checks it.
 */
@Autonomous(name = "R1 Left Corner: Wait for Tip, Launch, Park", preselectTeleOp = "R1 TeleOp")
public class LeftCornerWaitLaunchAndPark extends Auto<Robot> {
    static final double STEADY_SEC = 0.75;   // READY this long in a row before each fire step

    @Override
    protected void steps(Steps s, Robot robot) {
        robot.spinAtStart = true;      // the flywheel starts at START: at speed by the first shot
        robot.deployAtStart = false;   // the back is in the corner: the intake is pushed over while it drives away
        s.start("Back corner in L corner, front at HIVE", -58.4, -58.4, -44.7)
         .waitForGoal("Wait for the tip")
         .aimAtGoal("Aim at the up CELL", 2)
         .action(Actions.steadyWheel(robot, STEADY_SEC), 4)   // the wheel at a steady speed first (not still speeding up)
         .action(robot.launcher.fireAction(1), 3)
         .aimAtGoal("Aim", 1)   // aimed already: done at once; turned (a bump, a push): turn back to the goal
         .action(Actions.steadyWheel(robot, STEADY_SEC), 4)
         .action(robot.launcher.fireAction(1), 3)
         // Bump, twice: out 3 in FAST, then back SLOWLY. The sudden stop at the end of the fast move throws
         // the loose POLLEN forward into the feeder (they keep moving when the robot stops). A fast move
         // BACK would throw them backward, away from the feeder. Back 3.5 in, not 3: the last half inch pushes it
         // square on the wall again (the fast move overshoots a little).
         .forward("Bump out", 3, 1.0)
         .back("Bump back", 3.5, 0.3)
         .forward("Bump out again", 3, 1.0)
         .back("Bump back again", 3.5, 0.3)
         .aimAtGoal("Aim", 1)   // aimed already: done at once; turned (a bump, a push): turn back to the goal
         .action(Actions.steadyWheel(robot, STEADY_SEC), 4)
         .action(robot.launcher.fireAction(1), 3)
         .aimAtGoal("Aim", 1)   // aimed already: done at once; turned (a bump, a push): turn back to the goal
         .action(Actions.steadyWheel(robot, STEADY_SEC), 4)
         .action(robot.launcher.fireAction(1), 3)
         // More tries until the park time (parkWithSeconds cuts what is left): a POLLEN that did not go yet
         // gets two more shots (no bump here: the park time could cut a bump in half and shift the park).
         .aimAtGoal("Aim", 1)   // aimed already: done at once; turned (a bump, a push): turn back to the goal
         .action(Actions.steadyWheel(robot, STEADY_SEC), 4)
         .action(robot.launcher.fireAction(1), 3)
         .aimAtGoal("Aim", 1)   // aimed already: done at once; turned (a bump, a push): turn back to the goal
         .action(Actions.steadyWheel(robot, STEADY_SEC), 4)
         .action(robot.launcher.fireAction(1), 3)
         .parkWithSeconds(4)
         .alongside(Actions.deployIntake(robot))
         .moveWithTurn("To the LOADING ZONE", 12, -21, -45.3, 0.6);
    }

    // BIOBUZZ's field is turned half a turn between the alliances (not mirrored): the same route,
    // driven from each alliance's own wall, works for both.
    @Override
    protected boolean mirrorForRed() {
        return false;
    }
}
