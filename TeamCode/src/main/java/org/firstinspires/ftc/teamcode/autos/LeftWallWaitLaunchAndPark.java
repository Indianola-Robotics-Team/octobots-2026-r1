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
 * Route: wait 15 s, fire from the start spot at 2325 RPM (8 s at most: the wait leaves less time). Then it
 * pushes the intake over while it drives 8 in forward (clear of the FLOWER), and slides to the left corner
 * of the LOADING ZONE (no turns): only its corner is over the zone's corner, which is a PARK.
 * Plain numbers, the way the auto builder writes them, so the builder can open this file ("Start from
 * one of your autos") and show it on the map. Moves: inches, then the direction in degrees clockwise from
 * the robot's front; moveWithTurn's direction and heading count from the START heading.
 * simulator/scenarios/r1-auto-left.sim checks it.
 */
@Autonomous(name = "R1 Left Wall: Wait, Launch, Park Left", preselectTeleOp = "R1 TeleOp")
public class LeftWallWaitLaunchAndPark extends Auto<Robot> {
    @Override
    protected void steps(Steps s, Robot robot) {
        robot.deployAtStart = false;   // the back is on the wall: the intake is pushed over while it drives away
        s.start("Back on L wall, 3rd tile, 0.6in off centre, facing HIVE", -62, -11.2, -90)
         .waitSeconds("Wait for our partner", 15)
         .action(robot.launcher.fireAllAction(2325), 8)
         .parkWithSeconds(10)
         .alongside(Actions.deployIntake(robot))
         .move("Off the wall", 8, 0, 0.6)
         .move("To the LOADING ZONE", 42.3, 81, 0.6);
    }

    // BIOBUZZ's field is turned half a turn between the alliances (not mirrored): the same route,
    // driven from each alliance's own wall, works for both.
    @Override
    protected boolean mirrorForRed() {
        return false;
    }
}
