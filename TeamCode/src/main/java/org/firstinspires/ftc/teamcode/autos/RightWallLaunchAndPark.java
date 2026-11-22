package org.firstinspires.ftc.teamcode.autos;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.crossbotics.ftchelper.robot.Auto;
import com.crossbotics.ftchelper.robot.Steps;
import org.firstinspires.ftc.teamcode.Actions;
import org.firstinspires.ftc.teamcode.Robot;

/**
 * Robot 1, auto 2: from the RIGHT wall (the audience wall, as our drivers see it), fire the four preloaded
 * POLLEN into our HIVE's up CELL, then park at the right corner of our LOADING ZONE (LEAVE 3 + PARK 5).
 *
 * Start: the back (the intake) touching the right wall, the 3rd tile out from our wall, centred in it, the
 * front (the launcher) facing the HIVE. The up CELL's opening faces this side, so the shots come down into it.
 *
 * Route: turn to the goal with the camera (aimAtGoal), fire two, bump twice (3 in out fast, back slowly) so the last two roll into the feeder, fire them,
 * from the start spot at the shot table's speed for the camera's distance (before: Tunables.Launcher.speedRpm, the usual
 * shooting spot). It turns back to the start heading (turnTo 0), so the park moves go where they were
 * measured. Then it pushes the intake over while it slides off the wall toward ours, and slides past
 * the HIVE's frame to the right corner of the LOADING ZONE (no turns): only its corner is over the zone's
 * corner, which is a PARK.
 * Plain numbers, the way the auto builder writes them, so the builder can open this file ("Start from
 * one of your autos") and show it on the map. Moves: inches, then the direction in degrees from the robot's
 * front, counterclockwise like every angle (90 = left, -90 = right); forward/back need no angle.
 * moveWithTurn's direction and heading count from the START heading, counterclockwise too.
 * simulator/scenarios/r1-auto-right.sim checks it.
 */
@Autonomous(name = "R1 Right Wall: Launch, Park Right", preselectTeleOp = "R1 TeleOp")
public class RightWallLaunchAndPark extends Auto<Robot> {
    static final double STEADY_SEC = 0.75;   // READY this long in a row before each fire step

    @Override
    protected void steps(Steps s, Robot robot) {
        robot.spinAtStart = true;      // the flywheel starts at START: at speed by the first shot
        robot.deployAtStart = false;   // the back is on the wall: the intake is pushed over while it drives away
        s.start("Back on R wall, 3rd tile, centred, facing HIVE", 62, -11.8, 90)
         .aimAtGoal("Aim at the up CELL", 2)   // the camera turns the robot to the goal (2 s at most; not seen: it goes on)
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
         .parkWithSeconds(10)
         .alongside(Actions.deployIntake(robot))
         .turnTo("Back to the start heading", 0, 0.5)   // the aim turned it a little: the park moves need the start heading
         //.move("Off the wall", 42.8, 52, 0.6)   // was 35.8: +7 in because it came up short. Strafing left may slip more than right (the Drive Tuner tests right only). After a left strafe test, put back 35.8
         //.move("To the LOADING ZONE", 64.1, 12, 0.6);
                 //.move("Off the wall", 42.8, 52, 0.6)
                 //.move("To the LOADING ZONE", 64, 14, 0.6);
                 .move("Off the wall", 48.5, 53, 0.6)
                 .move("To the LOADING ZONE", 60, 10, 0.6);
    }

    // BIOBUZZ's field is turned half a turn between the alliances (not mirrored): the same route,
    // driven from each alliance's own wall, works for both.
    @Override
    protected boolean mirrorForRed() {
        return false;
    }
}
