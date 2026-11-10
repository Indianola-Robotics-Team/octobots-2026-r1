package org.firstinspires.ftc.teamcode.autos;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.crossbotics.ftchelper.robot.Auto;
import com.crossbotics.ftchelper.robot.Steps;
import org.firstinspires.ftc.teamcode.Actions;
import org.firstinspires.ftc.teamcode.Robot;

/**
 * Robot 1, auto 1: from our wall near the right corner, fire the four preloaded POLLEN into our HIVE's up
 * CELL, then park at the right corner of our LOADING ZONE (LEAVE 3 + PARK 5).
 *
 * Start (G304: touching the wall, our half, not in the LOADING ZONE or on a FLOWER): the robot's back-left
 * corner on our wall, 20.5 in from the right-hand wall, the front (the ramp) aimed at the HIVE's up CELL
 * (heading 37.8). The up CELL's opening faces the audience side, so the shots come down into it.
 *
 * Route: fire two from the start spot (3750 RPM: an estimate, tune it), bump (2 in out, fast back to the wall)
 * so the last two roll into the feeder, fire them. The intake stays folded
 * until then: at START it would fall onto the wall (Robot.deployAtStart). Then it pushes the intake over
 * while it slides 9.4 in straight off our wall (clear of our FLOWER), and slides to the right corner of the
 * LOADING ZONE, turning to face left on the way (no time lost): only its corner is over the zone's corner,
 * which is a PARK, and the zone stays free.
 * Plain numbers, the way the auto builder writes them, so the builder can open this file ("Start from
 * one of your autos") and show it on the map. Moves: inches, then the direction in degrees from the robot's
 * front, counterclockwise like every angle (90 = left, -90 = right); forward/back need no angle.
 * moveWithTurn's direction and heading count from the START heading, counterclockwise too.
 * simulator/scenarios/r1-auto.sim checks it.
 */
@Autonomous(name = "R1 Right Corner: Launch, Park Right", preselectTeleOp = "R1 TeleOp")
public class RightCornerLaunchAndPark extends Auto<Robot> {
    @Override
    protected void steps(Steps s, Robot robot) {
        robot.deployAtStart = false;   // the back is on the wall: the intake is pushed over while it drives away
        s.start("Back corner on wall 20.5in from R, front at HIVE", 51.6, -58.5, 37.8)
         .action(robot.launcher.fireAction(2, 3750), 6)   // 3750: an estimate (the side walls fire at 3450); tune on the robot
         // Bump: the back is on the wall, so out 2 in slowly, then back to the wall fast. The quick move back
         // rolls the last two POLLEN forward into the feeder.
         .forward("Bump out", 2, 0.3)
         .back("Bump back", 2, 1.0)
         .action(robot.launcher.fireAllAction(3750), 6)
         .parkWithSeconds(10)
         .alongside(Actions.deployIntake(robot))
         .move("Off the wall", 9.4, -38, 0.6)
         .moveWithTurn("To the LOADING ZONE", 74.5, 55, 52.2, 0.6);
    }

    // BIOBUZZ's field is turned half a turn between the alliances (not mirrored): the same route,
    // driven from each alliance's own wall, works for both.
    @Override
    protected boolean mirrorForRed() {
        return false;
    }
}
