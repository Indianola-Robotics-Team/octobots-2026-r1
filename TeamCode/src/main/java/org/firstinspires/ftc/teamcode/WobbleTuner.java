package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.crossbotics.ftchelper.core.Time;
import com.crossbotics.ftchelper.launcher.Flywheel;
import com.crossbotics.ftchelper.robot.ControlMap;
import com.crossbotics.ftchelper.robot.Pad;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Robot 1: the wobble and drop tuner. It holds the flywheel at one speed and measures two things:
 * - the WOBBLE: how far the speed wanders from the target with no ball (it must stay inside readyBandRpm);
 * - each SHOT: how far one ball pulls the speed down, from the speed just before it and below the target.
 * From those it says which readyBandRpm and shotDropRpm can work (Tunables.Launcher).
 *
 * Buttons (gamepad 1):
 * - The flywheel spins from START, at the shot table's speed for 36 in. B: off / on.
 * - D-pad up / down: the speed +/- 50 RPM.
 * - RB: fire one ball (it waits for READY, like TeleOp). Point the robot away from the HIVE: it may turn to it.
 * - A: clear the numbers (start a new measurement, e.g. after a speed change).
 * Run it on the battery you will use in the match: the numbers change with the battery.
 */
@TeleOp(name = "R1 Wobble Tuner", group = "Tune")
public class WobbleTuner extends ControlMap<Robot> {
    public static boolean oneDriver = true;

    private double speed = Double.NaN;          // the speed asked for (RPM)
    // The wobble: the speed minus the target, every reading while settled with no ball (RPM).
    private double wobbleMin, wobbleMax, wobbleSum, wobbleSumSq;
    private int wobbleN;
    private long wobbleFirstAt = 0;             // when the first wobble reading was taken (for its length)
    // The speeds of the last 0.3 s (for "the speed just before the shot"), and when each was read.
    private final double[] recent = new double[200];
    private final long[] recentAt = new long[200];
    private int recentNext = 0, recentCount = 0;
    // A shot being watched (NaN: none): the speed just before it, the lowest since (and when), its target.
    private double shotBefore = Double.NaN, shotLowest = Double.NaN, shotTarget = Double.NaN;
    private long shotStartAt = 0, shotLowestAt = 0;
    private double recoverSec = Double.NaN;
    private boolean leftBand = false;          // the speed left the ready band during this shot
    private int lastFired = 0;
    private double lastTarget = 0;
    private long measureAfter = 0;              // no wobble readings before this (the wheel settles first)
    private String note = "";                   // why the last shot was not kept, if it was not
    // Each shot kept: {drop from the speed before, dip below the target, seconds back to READY, 1 = a dip was seen}.
    private final List<double[]> shots = new ArrayList<>();

    /** A dip smaller than this (RPM) is "no dip seen": the ball may not have reached the wheel. */
    static final double MIN_DIP = 20;

    @Override
    protected void buttons(Robot robot, Pad g1, Pad g2) {
        speed = rpmAt(36);
        robot.launcher.useSpeed(speed);
        g1.b.toggle(robot.launcher).says("flywheel on/off");
        g1.dpadUp.press(() -> setSpeed(robot, speed + 50)).says("speed +50 RPM");
        g1.dpadDown.press(() -> setSpeed(robot, speed - 50)).says("speed -50 RPM");
        g1.rb.press(() -> fireOne(robot)).says("fire one ball (waits for READY; not while a shot is measured)");
        g1.a.press(this::clear).says("clear the numbers");
        clear();
        robot.everyLoop(() -> measure(robot));
    }

    // One ball, and only when nothing is on its way or being measured: one shot = one set of numbers.
    private void fireOne(Robot robot) {
        if (!Double.isNaN(shotBefore) || robot.launcher.part().isShooting()) {
            note = "RB ignored: the last shot is still being measured";
            return;
        }
        robot.launcher.fire();
    }

    private void setSpeed(Robot robot, double rpm) {
        speed = Math.max(500, rpm);
        robot.launcher.useSpeed(speed);
        clear();
    }

    private void clear() {
        wobbleMin = Double.POSITIVE_INFINITY;
        wobbleMax = Double.NEGATIVE_INFINITY;
        wobbleSum = wobbleSumSq = 0;
        wobbleN = 0;
        wobbleFirstAt = 0;
        shots.clear();
        shotBefore = Double.NaN;
        note = "";
        measureAfter = Time.nanos() + 3_000_000_000L;   // let the wheel settle first
    }

    // Every loop: read the wheel, add to the wobble or to the shot being watched, and show the numbers.
    private void measure(Robot robot) {
        Flywheel fly = robot.launcher.part().getFlywheel();
        double rpm = fly.getRpm(), target = fly.getTargetRpm();
        long now = Time.nanos();
        double band = fly.getTolerance();
        recent[recentNext] = rpm;
        recentAt[recentNext] = now;
        recentNext = (recentNext + 1) % recent.length;
        recentCount = Math.min(recentCount + 1, recent.length);

        // A new target (D-pad, B off or on, a spin-down after a shot): settle 3 s, and drop a shot being watched.
        if (target != lastTarget || target == 0) {
            measureAfter = Math.max(measureAfter, now + 3_000_000_000L);
            if (!Double.isNaN(shotBefore)) note = "last shot not kept: the speed changed during it";
            shotBefore = Double.NaN;
        }
        lastTarget = target;

        // A piece fed (the launcher's count went up): watch it. The speed just before = the 0.3 s before this loop.
        int fired = robot.launcher.part().getShotsFired();
        if (fired > lastFired && target > 0) {
            shotBefore = averageSince(now - 300_000_000L, now - 1);
            shotLowest = rpm;
            shotLowestAt = now;
            shotTarget = target;
            shotStartAt = now;
            recoverSec = Double.NaN;
            leftBand = false;
            note = "";
        }
        lastFired = fired;
        if (!Double.isNaN(shotBefore)) {
            if (rpm < shotLowest) {
                shotLowest = rpm;
                shotLowestAt = now;
            }
            double sec = (now - shotStartAt) / 1e9;
            boolean dipSeen = shotBefore - shotLowest >= MIN_DIP;
            if (Math.abs(rpm - shotTarget) > band) leftBand = true;
            // Back: READY again (the library's own test: in the band for its settle time), after the dip.
            if (dipSeen && leftBand && now > shotLowestAt && fly.isReady() && Double.isNaN(recoverSec)) recoverSec = sec;
            if (sec > 2.5) {   // the shot is over: keep its numbers
                shots.add(new double[] {shotBefore - shotLowest, shotTarget - shotLowest, leftBand ? recoverSec : -1, dipSeen ? 1 : 0});
                if (shots.size() > 8) shots.remove(0);
                shotBefore = Double.NaN;
                measureAfter = Math.max(measureAfter, now + 1_000_000_000L);
            }
        }
        // The wobble: every reading (in the band or not) once settled, with no ball on its way.
        boolean quiet = Double.isNaN(shotBefore) && !robot.launcher.part().isShooting();
        if (target > 0 && quiet && now > measureAfter) {
            double d = rpm - target;
            if (wobbleN == 0) wobbleFirstAt = now;
            wobbleMin = Math.min(wobbleMin, d);
            wobbleMax = Math.max(wobbleMax, d);
            wobbleSum += d;
            wobbleSumSq += d * d;
            wobbleN++;
        }
        show(robot, target, rpm, band, fly.getShotDrop());
    }

    private void show(Robot robot, double target, double rpm, double band, double shotDrop) {
        robot.show("Speed", fmt("%.0f / %.0f RPM (asked %.0f)", rpm, target, speed));
        robot.show("Now", fmt("readyBandRpm %.0f, shotDropRpm %.0f", band, shotDrop));
        double wobbleSec = wobbleN == 0 ? 0 : (Time.nanos() - wobbleFirstAt) / 1e9;
        if (wobbleSec < 10) {
            robot.show("Wobble", target > 0 ? fmt("measuring: keep it spinning, no ball (%.0f of 20 s)", wobbleSec)
                    : "flywheel off: B turns it on");
        } else {
            double mean = wobbleSum / wobbleN;
            double sd = Math.sqrt(Math.max(0, wobbleSumSq / wobbleN - mean * mean));
            robot.show("Wobble", fmt("%+.0f to %+.0f RPM from the target (average %+.0f, spread %.0f), %d readings",
                    wobbleMin, wobbleMax, mean, sd, wobbleN));
        }
        if (shots.isEmpty()) {
            robot.show("Shots", "none yet: RB fires one (5 or more)");
        } else {
            StringBuilder drops = new StringBuilder(), dips = new StringBuilder(), rec = new StringBuilder();
            double smallestDip = Double.POSITIVE_INFINITY;
            int seen = 0;
            for (double[] s : shots) {
                drops.append(s[3] > 0 ? fmt(" %.0f", s[0]) : " (no dip)");
                dips.append(s[3] > 0 ? fmt(" %.0f", s[1]) : " -");
                rec.append(s[2] < 0 ? " (stayed in band)" : Double.isNaN(s[2]) ? " -" : fmt(" %.1f", s[2]));
                if (s[3] > 0) {
                    smallestDip = Math.min(smallestDip, s[1]);
                    seen++;
                }
            }
            robot.show("Shot drop", "from the speed before:" + drops + " RPM");
            robot.show("Shot dip", "below the target:" + dips + " RPM (a shot counts when this is more than shotDropRpm)");
            robot.show("Back to READY", "after" + rec + " s");
            if (wobbleSec >= 10 && seen > 0) robot.show("Suggest", suggest(Math.max(Math.abs(wobbleMin), Math.abs(wobbleMax)), smallestDip));
        }
        if (!Double.isNaN(shotBefore)) robot.show("Watching", fmt("a shot: before %.0f, lowest %.0f", shotBefore, shotLowest));
        if (!note.isEmpty()) robot.show("Note", note);
    }

    // Shot sensing works when: the wobble < readyBandRpm < shotDropRpm < the smallest shot dip below the target.
    private static String suggest(double wobble, double smallestDip) {
        // The hub reads the speed in steps of about 43 RPM: the margins are a step or two, not less. In a match the
        // target also moves with the camera's distance, and the battery moves the speed: never below 80.
        double band = Math.max(80, Math.ceil((wobble + 45) / 10) * 10);
        if (smallestDip < band + 90) {
            return fmt("readyBandRpm %.0f (worst wobble %.0f + one hub step). The shots dip only %.0f below the target: "
                    + "too close to sense safely. Keep shotDropRpm at 150 (each feed runs its feedSec)", band, wobble, smallestDip);
        }
        return fmt("readyBandRpm %.0f, shotDropRpm %.0f (worst wobble %.0f, smallest dip %.0f: two hub steps of room)",
                band, band + 45, wobble, smallestDip);
    }

    // The average speed read between from and to (nanoseconds).
    private double averageSince(long from, long to) {
        double sum = 0;
        int n = 0;
        for (int i = 0; i < recentCount; i++) {
            if (recentAt[i] >= from && recentAt[i] <= to) {
                sum += recent[i];
                n++;
            }
        }
        return n == 0 ? Double.NaN : sum / n;
    }

    // The shot table's speed at this distance (between rows: a straight line), or speedRpm with no table.
    private static double rpmAt(double inches) {
        double[][] t = Tunables.Launcher.speedByDistance;
        if (t == null || t.length == 0) return Tunables.Launcher.speedRpm;
        if (inches <= t[0][0]) return t[0][1];
        for (int i = 1; i < t.length; i++) {
            if (inches <= t[i][0]) {
                double f = (inches - t[i - 1][0]) / (t[i][0] - t[i - 1][0]);
                return t[i - 1][1] + f * (t[i][1] - t[i - 1][1]);
            }
        }
        return t[t.length - 1][1];
    }

    private static String fmt(String f, Object... a) {
        return String.format(Locale.US, f, a);
    }
}
