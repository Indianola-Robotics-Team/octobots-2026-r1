package org.firstinspires.ftc.teamcode.autos;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.crossbotics.ftchelper.robot.Auto;
import com.crossbotics.ftchelper.robot.Steps;
import org.firstinspires.ftc.teamcode.Actions;
import org.firstinspires.ftc.teamcode.Robot;

/**
 * Robot 1, auto 3: from the LEFT wall (as our drivers see it), wait for our partner's shots, fire the four
 * preloaded POLLEN into our HIVE, then park at the left corner of our LOADING ZONE (LEAVE 3 + PARK 5).
 *
 * Why wait: from this side the HIVE's CELL that faces us is the DOWN one at the start. Our partner's shots
 * from the right tip the HIVE, and then the CELL on our side is up and open to us.
 *
 * Start: the back (the intake) touching the left wall, the 3rd tile out from our wall, the front (the
 * launcher) facing the HIVE. Not quite centred: 0.6 in toward the field's centre line, so the robot clears
 * the FLOWER on this wall (centred, it would overlap it by 0.4 in).
 *
 * Route: wait 15 s, then for each shot: aim (aimAtGoal), wait for a steady wheel, fire one (the speed from the shot
 * table and the camera's distance). After two: one bump (3 in out fast, back slowly) so the last two roll into the
 * feeder, then two more; then more tries until parkWithSeconds(6) cuts them (the wait leaves 9 s for the shots). It turns back to the start heading (turnTo 0), so the park moves go where
 * they were measured. Then it pushes the intake over while it drives 8 in forward (clear of the
 * FLOWER), and slides to the left corner of the LOADING ZONE (no turns): only its corner is over the zone's
 * corner, which is a PARK.
 * Plain numbers, the way the auto builder writes them, so the builder can open this file ("Start from
 * one of your autos") and show it on the map. Moves: inches, then the direction in degrees from the robot's
 * front, counterclockwise like every angle (90 = left, -90 = right); forward/back need no angle.
 * moveWithTurn's direction and heading count from the START heading, counterclockwise too.
 * simulator/scenarios/r1-auto-left.sim checks it.
 */
@Autonomous(name = "R1 Left Wall: Wait, Launch, Park Left", preselectTeleOp = "R1 TeleOp")
public class LeftWallWaitLaunchAndPark extends Auto<Robot> {
    static final double STEADY_SEC = 0.75;   // READY this long in a row before each fire step

    @Override
    protected void steps(Steps s, Robot robot) {
        robot.spinAtStart = true;      // the flywheel starts at START: at speed by the first shot
        robot.deployAtStart = false;   // the back is on the wall: the intake is pushed over while it drives away
        s.start("Back on L wall, 3rd tile, 0.6in off centre, facing HIVE", -62, -11.2, -90)
         .waitSeconds("Wait for our partner", 15)
         .aimAtGoal("Aim at the up CELL", 2)   // the camera turns the robot to the goal (2 s at most; not seen: it goes on)
         .action(Actions.steadyWheel(robot, STEADY_SEC), 4)   // the wheel at a steady speed first (not still speeding up)
         .action(robot.launcher.fireAction(1), 3)
         .aimAtGoal("Aim", 1)   // aimed already: done at once; turned (a bump, a push): turn back to the goal
         .action(Actions.steadyWheel(robot, STEADY_SEC), 4)
         .action(robot.launcher.fireAction(1), 3)
         // Bump (once here: after the 15 s wait, time is short): out 3 in FAST, then back SLOWLY. The sudden stop at the end of the fast move throws
         // the loose POLLEN forward into the feeder (they keep moving when the robot stops). A fast move
         // BACK would throw them backward, away from the feeder. Back 3.5 in, not 3: the last half inch pushes it
         // square on the wall again (the fast move overshoots a little).
         .forward("Bump out", 3, 1.0)
         .back("Bump back", 3.5, 0.3)
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
         .parkWithSeconds(6)   // the park takes about 4 s: 6 leaves time for the 4 slower, steadier shots
         .alongside(Actions.deployIntake(robot))
         .turnTo("Back to the start heading", 0, 0.5)   // the aim turned it a little: the park moves need the start heading
         .forward("Off the wall", 8, 0.6)
         .move("To the LOADING ZONE", 42.3, -81, 0.6);
    }

    // BIOBUZZ's field is turned half a turn between the alliances (not mirrored): the same route,
    // driven from each alliance's own wall, works for both.
    @Override
    protected boolean mirrorForRed() {
        return false;
    }
}
