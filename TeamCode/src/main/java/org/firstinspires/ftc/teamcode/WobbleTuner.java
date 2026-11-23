package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.crossbotics.ftchelper.core.Time;
import com.crossbotics.ftchelper.drive.DriverControls;
import com.crossbotics.ftchelper.finders.FinderOpMode;
import com.crossbotics.ftchelper.finders.FinderResults;
import com.crossbotics.ftchelper.gamepad.ControllerBindings;
import com.crossbotics.ftchelper.launcher.Flywheel;
import com.crossbotics.ftchelper.launcher.Launcher;
import com.crossbotics.ftchelper.launcher.Launcher.ShotMode;
import com.crossbotics.ftchelper.telemetry.RobotTelemetry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Robot 1: the wobble and drop tuner. It holds the flywheel at one speed and measures two things:
 * - the WOBBLE: how far the speed wanders from the target with no ball (it must stay inside readyBandRpm);
 * - each SHOT: how far one ball pulls the speed down, from the speed just before it and below the target.
 * After 10 s of wobble and 5 shots it says which readyBandRpm and shotDropRpm can work (Tunables.Launcher).
 *
 * SAFETY: nothing moves until a fresh press. It is a finder, not a TeleOp: the robot's START actions (the
 * intake folding out, the flywheel spinning up) do not run, and it never turns the robot to aim.
 *
 * Buttons (gamepad 1):
 * - B: the flywheel on / off, at the shot table's speed for 36 in.
 * - D-pad up / down: the speed +/- 50 RPM.
 * - RB: fire one ball (it waits until the wheel is at speed).
 * - X: the intake folds out and runs / stops.
 * - A: clear the numbers (start a new measurement, e.g. after a speed change).
 * - The sticks drive. GUIDE (or START held 1 s): help, and everything stops.
 * Run it on the battery you will use in the match: the numbers change with the battery.
 */
@TeleOp(name = "R1 Wobble Tuner", group = "Tune")
public class WobbleTuner extends FinderOpMode {
    /** The wobble is measured this long before it counts (seconds). */
    static final double WOBBLE_SEC = 10;
    /** A suggestion needs this many shots with a speed drop that was seen. */
    static final int ENOUGH_SHOTS = 5;

    private Robot robot;
    private Launcher part;                      // the launcher's wheel and feeder, as in a match
    private DriverControls driving;
    private boolean spinning = false;           // B turned the flywheel on
    private boolean intakeOn = false;           // X turned the intake on

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
    protected String finderName() {
        return "R1 Wobble Tuner";
    }

    @Override
    protected boolean batteryMatters() {
        return true;
    }

    // INIT: the parts are made, and nothing moves. The robot's START actions are never run here.
    @Override
    protected void setup() {
        robot = new Robot();
        robot.setup(hardwareMap);
        part = robot.launcher.part();
        part.clearReadyChecks();                 // no aim here: it fires where it points
        part.shotMode(ShotMode.CAREFUL);         // every shot waits for the speed: never SPRAY
        part.speedScale(1);                      // no speed trim: the numbers are for the real speed
        part.stop();
        driving = DriverControls.create(hardwareMap, robot.robotConfig().drive(), null);
        speed = rpmAt(robot, 36);
        clear();
    }

    @Override
    protected void showInit(RobotTelemetry t) {
        t.warn("{B} spins the flywheel. {B} again = stop.");
        t.warn("{RB} SHOOTS. Safety glasses. Nobody in front.");
        t.warn("{X} folds out and runs the intake.");
        t.warn("The sticks drive. Let go = stop.");
        t.text("R1 Wobble Tuner: how steady is the flywheel,");
        t.text("and how much does one ball slow it?");
        t.text("Nothing moves until you press a button.");
        t.line("Test speed", fmt("%.0f RPM (the table's for 36 in)", speed));
    }

    @Override
    protected void bind(ControllerBindings c) {
        c.bindPress(ControllerBindings.Pad.G1, ControllerBindings.Btn.B, this::toggleFlywheel)
         .bindPress(ControllerBindings.Pad.G1, ControllerBindings.Btn.X, this::toggleIntake)
         .bindPress(ControllerBindings.Pad.G1, ControllerBindings.Btn.DPAD_UP, () -> setSpeed(speed + 50))
         .bindPress(ControllerBindings.Pad.G1, ControllerBindings.Btn.DPAD_DOWN, () -> setSpeed(speed - 50))
         .bindPress(ControllerBindings.Pad.G1, ControllerBindings.Btn.RB, this::fireOne)
         .bindPress(ControllerBindings.Pad.G1, ControllerBindings.Btn.A, this::clear);
    }

    private void toggleFlywheel() {
        spinning = !spinning;
        note = "";
        if (!spinning) part.stop();              // also drops a shot that waits
    }

    private void toggleIntake() {
        intakeOn = !intakeOn;
        if (intakeOn) {
            robot.intakeDeploy.goTo("deployed");
            robot.intake.in();
        } else {
            robot.intake.stop();
        }
    }

    @Override
    protected void step() {
        // SAFETY: a stick already pushed at START or after help does not drive until it was let go.
        if (inputsReady()) driving.update(gamepad1);
        else driving.hold();
        if (spinning) {
            part.setRpm(speed);
            part.spinUp();
        }
        part.update();
        // Only the two intake parts, and only after X: the rest of the robot stays still.
        if (intakeOn) {
            if (robot.intakeDeploy.isPresent()) robot.intakeDeploy.update();
            if (robot.intake.isPresent()) robot.intake.update();
        }
        measure();
    }

    // Help: everything stops, and starts again only with a fresh press.
    @Override
    protected void pause() {
        if (!Double.isNaN(shotBefore)) note = "That shot was not kept: you opened help.";
        shotBefore = Double.NaN;
        spinning = false;
        intakeOn = false;
        if (part != null) part.stop();
        if (robot != null && robot.intake != null && robot.intake.isPresent()) robot.intake.stop();
        if (driving != null) driving.stop();
    }

    @Override
    protected void stopAll() {
        spinning = false;
        intakeOn = false;
        RuntimeException first = null;
        try { if (part != null) part.stop(); } catch (RuntimeException e) { first = e; }
        try { if (driving != null) driving.stop(); } catch (RuntimeException e) { if (first == null) first = e; }
        try { if (robot != null && robot.all() != null) robot.all().stop(); } catch (RuntimeException e) { if (first == null) first = e; }
        if (first != null) throw first;
    }

    // One ball, and only when nothing is on its way or being measured: one shot = one set of numbers.
    private void fireOne() {
        if (!spinning) {
            note = "No shot: the flywheel is off. Press {B} first.";
            return;
        }
        if (!Double.isNaN(shotBefore) || part.isShooting()) {
            note = "No new shot: the last one is still measured.";
            return;
        }
        part.shoot(1);
    }

    private void setSpeed(double rpm) {
        speed = Math.max(500, rpm);
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
        results.remove("wobble:band");
        results.remove("wobble:drop");
        results.notDone("the test is not finished.");
        measureAfter = Time.nanos() + 3_000_000_000L;   // let the wheel settle first
    }

    // Every loop: read the wheel, add to the wobble or to the shot being watched, and show the numbers.
    private void measure() {
        Flywheel fly = part.getFlywheel();
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
            if (!Double.isNaN(shotBefore)) note = "That shot was not kept: the speed changed.";
            shotBefore = Double.NaN;
        }
        lastTarget = target;

        // A piece fed (the launcher's count went up): watch it. The speed just before = the 0.3 s before this loop.
        int fired = part.getShotsFired();
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
        boolean quiet = Double.isNaN(shotBefore) && !part.isShooting();
        if (target > 0 && quiet && now > measureAfter) {
            double d = rpm - target;
            if (wobbleN == 0) wobbleFirstAt = now;
            wobbleMin = Math.min(wobbleMin, d);
            wobbleMax = Math.max(wobbleMax, d);
            wobbleSum += d;
            wobbleSumSq += d * d;
            wobbleN++;
        }
        updateResult(band, fly.getShotDrop());
    }

    private double wobbleSec() {
        return wobbleN == 0 ? 0 : (Time.nanos() - wobbleFirstAt) / 1e9;
    }

    private int shotsSeen() {
        int seen = 0;
        for (double[] s : shots) if (s[3] > 0) seen++;
        return seen;
    }

    // Why there is no safe shotDropRpm ("" = there is one, or the test is not finished).
    private String noSafeDrop = "";

    // Shot sensing works when: the wobble < readyBandRpm < shotDropRpm < the smallest shot dip below the target.
    private void updateResult(double bandNow, double dropNow) {
        noSafeDrop = "";
        results.remove("wobble:band");
        results.remove("wobble:drop");
        if (wobbleSec() < WOBBLE_SEC) {
            results.notDone("measure the wobble for 10 s.");
            return;
        }
        if (shotsSeen() < ENOUGH_SHOTS) {
            results.notDone(shotsSeen() + " of 5 test shots measured.");
            return;
        }
        double wobble = Math.max(Math.abs(wobbleMin), Math.abs(wobbleMax));
        double smallestDip = Double.POSITIVE_INFINITY;
        for (double[] s : shots) if (s[3] > 0) smallestDip = Math.min(smallestDip, s[1]);
        // The hub reads the speed in steps of about 43 RPM: the margins are a step or two, not less. In a match the
        // target also moves with the camera's distance, and the battery moves the speed: never below 80.
        double band = Math.max(80, Math.ceil((wobble + 45) / 10) * 10);
        if (smallestDip < band + 90) {
            // No line to copy: readyBandRpm alone could end up over shotDropRpm, and shots would not be counted.
            noSafeDrop = fmt("One ball takes it only %.0f RPM under.\n", smallestDip)
                    + "That is too little to count shots safely.\n"
                    + fmt("Change nothing: readyBandRpm %.0f,\n", bandNow)
                    + fmt("shotDropRpm %.0f stay.", dropNow)
                    + (Tunables.Feeder.stopOnShot
                            ? "\nA missed count = the feeder runs all of feedSec." : "");
            results.notDone("no safe shotDropRpm was found.");
            return;
        }
        results.setTunable("wobble:band", "Launcher", "readyBandRpm", fmt("%.0f", band));
        results.setTunable("wobble:drop", "Launcher", "shotDropRpm", fmt("%.0f", band + 45));
        results.done();
    }

    @Override
    protected void show(RobotTelemetry t) {
        Flywheel fly = part.getFlywheel();
        double band = fly.getTolerance(), sec = wobbleSec();
        boolean measured = sec >= WOBBLE_SEC;
        int seen = shotsSeen();
        boolean watching = !Double.isNaN(shotBefore);
        t.warn(spinning ? "Flywheel ON. {B} = stop. Nobody in front." : "{B} spins the flywheel. Hands clear.");
        if (intakeOn) t.warn("Intake ON. {X} = stop.");
        if (!part.getLastProblem().isEmpty()) {
            t.warn("No shot: " + (part.getLastProblem().startsWith("gave up") ? "the wheel was not at speed." : part.getLastProblem()));
            t.warn("Next: check the battery, then shoot again.");
        }

        t.section("R1 Wobble Tuner: step " + (!spinning ? 1 : !measured ? 2 : 3) + " of 3");
        if (!spinning) {
            t.text("Next: load balls ({X} = intake on or off).");
            t.text("Then press {B}: the flywheel spins.");
        } else if (watching) {
            t.text("Next: wait. It measures that shot.");
        } else if (!measured) {
            t.text("Next: wait. Do not shoot yet.");
        } else if (seen < ENOUGH_SHOTS) {
            t.text(fly.isReady() ? "Next: {RB} = shoot one ball." : "Next: wait for the speed.");
        } else if (noSafeDrop.isEmpty()) {
            t.text("Next: {B} = stop. Copy the lines below.");
        } else {
            t.text("Next: {B} = stop. Change nothing:");
            for (String line : noSafeDrop.split("\n")) t.text("  " + line);
        }
        t.line("Speed", "now " + FinderResults.num(fly.getRpm(), 0) + fmt(" RPM, target %.0f", speed));
        if (!measured) {
            t.line("Wobble", fmt("measuring, no ball (%.0f of 10 s)", sec));
        } else {
            double worst = Math.max(Math.abs(wobbleMin), Math.abs(wobbleMax));
            t.line("Worst wobble", fmt("%.0f RPM: %s (limit %.0f)", worst, worst < band ? "OK" : "TOO MUCH", band));
            double mean = wobbleSum / wobbleN;
            t.line("On average", fmt("%.0f RPM %s the target", Math.abs(mean), mean < 0 ? "under" : "over"));
            t.debug("Spread", fmt("%.0f RPM, %d readings", Math.sqrt(Math.max(0, wobbleSumSq / wobbleN - mean * mean)), wobbleN));
        }
        t.line("Test shots", seen < ENOUGH_SHOTS ? seen + " of 5" : seen + " (5 or more: OK)");
        if (!shots.isEmpty()) {
            List<String> drops = new ArrayList<>(), dips = new ArrayList<>(), back = new ArrayList<>();
            for (double[] s : shots.subList(Math.max(0, shots.size() - 5), shots.size())) {
                drops.add(s[3] > 0 ? fmt("%.0f", s[0]) : "none");
                dips.add(s[3] > 0 ? fmt("%.0f", s[1]) : "none");
                back.add(s[2] < 0 ? "stayed" : Double.isNaN(s[2]) ? "late" : fmt("%.1f", s[2]));
            }
            t.line("Speed lost (RPM)", list(drops));
            t.line("Lowest under (RPM)", list(dips));
            t.line("Back at speed (s)", list(back));
        }
        if (!note.isEmpty()) t.text(note);
        if (results.isEmpty()) t.text("D-pad = change the speed. {A} = start over.");
    }

    @Override
    protected String[] helpLines() {
        return new String[] {
            "Safety glasses. Hands clear. Nobody in front.",
            "Run it on the battery you will use in a match.",
            "1. Load balls. {X} = intake out and on, or off.",
            "   {B} = flywheel on or off.",
            "2. No ball for 10 s: it measures the wobble.",
            "3. {RB} = one ball. Do it 5 times or more.",
            "Wobble = how far the speed wanders from target.",
            "Speed lost = from the speed just before a ball.",
            "Lowest under = how far under the target it fell.",
            "  A shot counts when that is over shotDropRpm.",
            "none = no drop seen. late = not back in time.",
            "D-pad up/down = 50 RPM. It starts the test over.",
            "{A} = start over. It never aims the robot.",
        };
    }

    private static String list(List<String> parts) {
        StringBuilder b = new StringBuilder();
        for (String p : parts) b.append(b.length() == 0 ? "" : ", ").append(p);
        return b.toString();
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
    // The launcher's own shot table at this distance: the same lookup as TeleOp (its estimate past the ends too).
    private static double rpmAt(Robot robot, double inches) {
        com.crossbotics.ftchelper.launcher.ShotTable t = robot.launcher.table(robot.launcher.tableName());
        if (t == null || t.points().isEmpty()) return Tunables.Launcher.speedRpm;
        return t.lookup(inches).rpm;
    }

    private static String fmt(String f, Object... a) {
        return String.format(Locale.US, f, a);
    }
}
