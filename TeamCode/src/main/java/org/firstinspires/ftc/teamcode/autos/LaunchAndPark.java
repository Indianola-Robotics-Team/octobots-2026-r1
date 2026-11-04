package org.firstinspires.ftc.teamcode.autos;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.crossbotics.ftchelper.robot.Auto;
import com.crossbotics.ftchelper.robot.Steps;
import org.firstinspires.ftc.teamcode.Actions;
import org.firstinspires.ftc.teamcode.Robot;
import org.firstinspires.ftc.teamcode.Tunables;

/**
 * Robot 1, version 1 (file 5 of 5): fire the four preloaded POLLEN into our HIVE's up CELL, then park in
 * our LOADING ZONE without touching a wall (LEAVE 3 + PARK 5 points).
 *
 * Start (G304: touching the wall, our half, not in the LOADING ZONE or on a FLOWER): right of
 * centre, the robot's front-right corner on our wall, 20.5 in from the right-hand wall, the back (the
 * ramp) aimed at the HIVE's up CELL (the body turned 52 degrees to the wall). Why there: the up CELL's
 * opening faces the audience side, so a shot must come down into it from that side; from this spot, in
 * the simulator, a steep shot goes in cleanly 90-98 % of the time (README, "The autonomous").
 *
 * Route (turns, forward and back only, no strafing; the numbers are in Tunables.Auto):
 * 1. back 4 in off the wall (toward the HIVE), then fire all four. The intake stays folded until
 *   later: at START it would fall onto the wall (Robot.deployAtStart).
 * 2. turn 45 degrees left, push the intake over (clear of the walls now), back 85.5 in along our
 *   wall, past our FLOWER, to above the middle of the LOADING ZONE
 * 3. turn to face the far wall and back 12.6 in into the LOADING ZONE: the ramp ends about 2.5 in from our
 *   wall, partly in the zone, touching nothing.
 * Headings below are the field's as seen from our wall (0 = facing the far wall, counterclockwise +).
 * simulator/scenarios/r1-auto.sim checks it.
 */
@Autonomous(name = "R1 Launch and Park", preselectTeleOp = "R1 TeleOp")
public class LaunchAndPark extends Auto<Robot> {
    /** Where it starts: inches right of centre, inches from centre toward the far wall, field heading. */
    static final double START_X = 51.6, START_Y = -58.5, START_HEADING = -142.2;
    /** Field heading while backing along our wall to the LOADING ZONE, and when parked. */
    static final double ALONG_WALL_HEADING = -97.6, PARK_HEADING = 0;

    @Override
    protected void steps(Steps s, Robot robot) {
        robot.deployAtStart = false;   // the front is on the wall: the intake is pushed over later (below)
        s.start("Nose corner on wall 20.5in from R, back at HIVE", START_X, START_Y, START_HEADING)
         .move("Off the wall", Tunables.Auto.offWallInches, 180, Tunables.Auto.slowPower)
         .action(robot.launcher.fireAllAction(), 12)   // the library's: fires all 4 (Robot.java holds(4))
         .parkWithSeconds(Tunables.Auto.parkSeconds)
         .turnTo("Along our wall", turnToField(ALONG_WALL_HEADING), Tunables.Auto.turnPower)
         .action(Actions.deployIntake(robot), 3)
         .move("To the LOADING ZONE", Tunables.Auto.alongWallInches, 180, Tunables.Auto.drivePower)
         .waitSeconds("Settle", 0.3)
         .turnTo("Face the far wall", turnToField(PARK_HEADING), Tunables.Auto.turnPower)
         .waitSeconds("Settle again", 0.3)
         .move("Into the LOADING ZONE", Tunables.Auto.intoZoneInches, 180, Tunables.Auto.slowPower);
    }

    /** turnTo's 0 is the heading at the start: a field heading as turnTo needs it. */
    private static double turnToField(double fieldHeading) {
        return fieldHeading - START_HEADING;
    }

    // BIOBUZZ's field is turned half a turn between the alliances (not mirrored): the same route,
    // driven from each alliance's own wall, works for both.
    @Override
    protected boolean mirrorForRed() {
        return false;
    }
}
