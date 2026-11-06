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
 * Route: fire from the start spot at the usual speed (Tunables.Launcher.speedRpm: this is the usual
 * shooting spot). Then it pushes the intake over while it slides off the wall toward ours, and slides past
 * the HIVE's frame to the right corner of the LOADING ZONE (no turns): only its corner is over the zone's
 * corner, which is a PARK.
 * Plain numbers, the way the auto builder writes them, so the builder can open this file ("Start from
 * one of your autos") and show it on the map. Moves: inches, then the direction in degrees clockwise from
 * the robot's front; moveWithTurn's direction and heading count from the START heading.
 * simulator/scenarios/r1-auto-right.sim checks it.
 */
@Autonomous(name = "R1 Right Wall: Launch, Park Right", preselectTeleOp = "R1 TeleOp")
public class RightWallLaunchAndPark extends Auto<Robot> {
    @Override
    protected void steps(Steps s, Robot robot) {
        robot.deployAtStart = false;   // the back is on the wall: the intake is pushed over while it drives away
        s.start("Back on R wall, 3rd tile, centred, facing HIVE", 62, -11.8, 90)
         .action(robot.launcher.fireAllAction(), 12)
         .parkWithSeconds(10)
         .alongside(Actions.deployIntake(robot))
         .move("Off the wall", 35.8, 308, 0.6)
         .move("To the LOADING ZONE", 64.1, 348, 0.6);
    }

    // BIOBUZZ's field is turned half a turn between the alliances (not mirrored): the same route,
    // driven from each alliance's own wall, works for both.
    @Override
    protected boolean mirrorForRed() {
        return false;
    }
}
