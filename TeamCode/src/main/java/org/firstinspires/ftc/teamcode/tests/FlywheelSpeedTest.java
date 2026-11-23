package org.firstinspires.ftc.teamcode.tests;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.crossbotics.ftchelper.finders.FinderResults;
import com.crossbotics.ftchelper.gamepad.ButtonNames;
import org.firstinspires.ftc.teamcode.Tunables;

/**
 * ONE-OFF TEST (2026-11-12): is "bang-bang + feedforward" speed control better than the hub's own speed control
 * for R1's flywheel? Idea from team 16010 Astra Machina. Proposal: ftc-helper-lib docs/design/flywheel-bang-feedforward.md.
 *
 * It does NOT use Robot.java: it drives the flywheel motor and the two feed wheels itself, so both
 * speed-control modes run on the same hardware with the same feeding rhythm.
 *
 * Set up: robot on the floor, front (ramp) toward a net. Load 4 POLLEN the normal way.
 *
 * ADVANCED: a team can skip it. SAFETY: nothing moves until everything was let go after START and then
 * pressed again. B stops any test at once (X's two powers, a burst's feed), and it never starts again by itself.
 *
 * Gamepad 1:
 *   B           flywheel on / off; during a test: stop it now
 *   A           switch mode: HUB (the hub's PIDF, what R1 uses now) or BANG (bang-bang + feedforward)
 *   X           measure kS and kV for BANG (spins the wheel at two powers, about 6 s; nothing loaded).
 *               They are NOT the library's bangKs and bangKv: they are power, not volts, and this test's BANG
 *               is another control law than speedControl "BANG_BANG". Do not copy them into Tunables.java.
 *   RB          fire a burst of 4, same rhythm in both modes (the wheel must be on)
 *   D-pad up/dn target speed +/- 50 RPM
 *
 * Every loop goes into a CSV file: /sdcard/FIRST/flywheel-test/flywheel-<date>.csv (saved after each burst and
 * at STOP). Copy it to the laptop and run: python3 tools/flywheel_test_report.py flywheel-<date>.csv
 * The Driver Hub also shows a short result for the last burst of each mode.
 *
 * Test plan: 10 bursts in each mode, change mode every 2 bursts. Then the same on a battery at about 11.8 V.
 * A result counts only if the loop time stays under 25 ms (shown on the screen).
 */
@TeleOp(name = "Flywheel Speed Test (one-off)", group = "Tests")
public class FlywheelSpeedTest extends LinearOpMode {

    // ---- Hardware facts (from Robot.java) ----
    private static final double COUNTS_PER_REV = 28;      // goBILDA 6000 RPM motor (5203-2402-0019 + 1:1 kit), encoder on the motor
    private static final double MAX_RPM = 6000;

    // ---- Feeding rhythm (the same in both modes) ----
    private static final double GAP_SEC = 0.25;           // wait after the wheels stop, before the next ball (library shotIntervalSec)
    private static final int BURST = 4;

    // ---- BANG settings ----
    private static final double BAND = 0.08;              // full power while more than 8% below the target (Astra: about 8-10%)
    private static final double BAND_EXIT = 1.5;          // go back to full power only when more than 1.5 x the band below (stops flicker at the edge)
    private static final double KP_BANG = 0.0005;         // power per RPM of error, near the target: very small (100 RPM low = +0.05 power)

    // ---- Result settings ----
    private static final double READY_RPM = 100;          // "back to speed" = within this many RPM (Tunables.Launcher readyBandRpm)
    private static final double READY_HOLD_SEC = 0.05;    // ...for this long

    private enum Mode { HUB, BANG }

    private DcMotorEx flywheel;
    private CRServo feedLeft, feedRight;
    private VoltageSensor battery;
    private List<LynxModule> hubs;

    private Mode mode = Mode.HUB;
    private boolean on = false;
    private double targetRpm;
    private double kS, kV;                // BANG: power = kS + kV x RPM
    private String kSource;
    private boolean bangFull = true;      // BANG: true = in the full-power part
    private double volts = Double.NaN;
    private double nextVoltsAt = 0;

    // Burst state
    private int burstId = 0;
    private int shotInBurst = -1;         // -1 = no burst running
    private double shotStart;             // when the current shot's feed started
    private final double[] pushAt = new double[BURST];
    private final double[] rpmAtPush = new double[BURST];
    private final double[] minAfter = new double[BURST];
    private final double[] backAt = new double[BURST];
    private double inBandSince = Double.NaN;
    private Mode burstMode;
    private String lastHub = "no burst yet", lastBang = "no burst yet";
    private boolean burstSlow = false;    // a loop over 25 ms in this burst: the whole burst does not count
    private String note = "";             // what happened last, and what to do

    // Log
    private final StringBuilder log = new StringBuilder();
    private File logFile;

    @Override
    public void runOpMode() {
        // Bulk reads: one read per hub per loop, so the loop is fast (bang-bang needs a fast loop).
        hubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule h : hubs) h.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

        flywheel = hardwareMap.get(DcMotorEx.class, "launcher");
        flywheel.setDirection(DcMotorSimple.Direction.REVERSE);           // Robot.java: Launcher ... .reversed()
        flywheel.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);  // coast: never brake a fast wheel
        feedLeft = hardwareMap.get(CRServo.class, "feeder_left");
        feedRight = hardwareMap.get(CRServo.class, "feeder_right");
        feedRight.setDirection(DcMotorSimple.Direction.REVERSE);         // Robot.java: alsoServoReversed
        battery = hardwareMap.voltageSensor.iterator().next();

        targetRpm = Tunables.Launcher.speedRpm;
        // A first kS/kV guess from the hub's own F gain (kF 12.5): power = kF x counts/s / 32767.
        // X measures the real numbers.
        kS = 0;
        kV = Tunables.Launcher.kF * COUNTS_PER_REV / 60.0 / 32767.0;
        kSource = "a guess from kF";

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File dir = new File("/sdcard/FIRST/flywheel-test");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        logFile = new File(dir, "flywheel-" + stamp + ".csv");
        log.append("t,loop_ms,mode,target_rpm,rpm,power,volts,burst,shot,event\n");

        feed(0);
        coast();

        ButtonNames.watch(gamepad1, gamepad2);
        while (opModeInInit()) {
            say("!! {B} or {X} spins the flywheel. {X} = two powers, 6 s.");
            say("!! {RB} FIRES 4 balls. It does not wait for speed.");
            say("!! Safety glasses. Hands clear. Nobody in front.");
            say("!! {B} = stop any test at once.");
            say("--- Flywheel Speed Test ---");
            say("Advanced: you can skip this test.");
            say("Compares PID and this test's own on/off control.");
            say("Its kS and kV are NOT bangKs and bangKv:");
            say("other units, other control. Do not copy them.");
            say("Press ▶ on the Driver Hub to start.");
            say("STOP (■) on the Driver Hub ends it any time.");
            telemetry.update();
            idle();
        }
        if (isStopRequested()) return;

        boolean lastA = true, lastB = true, lastX = true, lastRb = true, lastUp = true, lastDown = true;
        boolean waitForRelease = true;    // a button held through START does nothing
        double t0 = now();
        double lastLoop = t0;

        try {
        while (opModeIsActive()) {
            for (LynxModule h : hubs) h.clearBulkCache();
            double t = now();
            double loopMs = (t - lastLoop) * 1000;
            lastLoop = t;
            readBattery(t);

            // Buttons (on press only)
            if (waitForRelease) {
                if (anyPressed()) {
                    // As if held: only a press after the release counts.
                    lastA = lastB = lastX = lastRb = lastUp = lastDown = true;
                } else {
                    waitForRelease = false;
                    lastA = lastB = lastX = lastRb = lastUp = lastDown = false;
                }
            }
            boolean stopPressed = !waitForRelease && gamepad1.b && !lastB;
            // SAFETY: B during a burst stops the feed and the wheel in this same loop.
            if (stopPressed && shotInBurst >= 0) {
                cancelBurst("You stopped the burst: not counted.");
                waitForRelease = true;
                stopPressed = false;
            }
            boolean idle = shotInBurst < 0 && !waitForRelease;
            if (stopPressed && idle) { on = !on; note = ""; if (!on) coast(); }
            if (gamepad1.a && !lastA && idle) { switchMode(mode == Mode.HUB ? Mode.BANG : Mode.HUB); }
            if (gamepad1.x && !lastX && idle && !gamepad1.b) { measureKsKv(); lastLoop = now(); waitForRelease = true; idle = false; }
            if (gamepad1.dpad_up && !lastUp && idle) targetRpm = Math.min(MAX_RPM, targetRpm + 50);
            if (gamepad1.dpad_down && !lastDown && idle) targetRpm = Math.max(500, targetRpm - 50);
            if (gamepad1.right_bumper && !lastRb && idle && on) startBurst(t);
            lastA = gamepad1.a; lastB = gamepad1.b; lastX = gamepad1.x; lastRb = gamepad1.right_bumper;
            lastUp = gamepad1.dpad_up; lastDown = gamepad1.dpad_down;

            // A burst that a slow loop touched does not count, also after the loop is fast again.
            if (shotInBurst >= 0 && loopMs > 25) burstSlow = true;
            double rpm = flywheel.getVelocity() / COUNTS_PER_REV * 60.0;
            double power = control(rpm);
            String event = runBurst(t, rpm);

            log.append(String.format(Locale.US, "%.4f,%.1f,%s,%.0f,%.1f,%.3f,%.2f,%d,%d,%s\n",
                    t - t0, loopMs, mode, on ? targetRpm : 0, rpm, power, volts,
                    shotInBurst >= 0 ? burstId : 0, shotInBurst + 1, event));   // shot 0 = no burst running

            show(rpm, loopMs, waitForRelease);
        }
        } finally {
            coast();
            feed(0);
            boolean saved = saveLog();
            say("--- Flywheel Speed Test: STOPPED ---");
            say("Motor power is off. The wheel may still turn.");
            say("Last burst, hub control (PID):");
            for (String line : lastHub.split("\n")) say("  " + line);
            say("Last burst, this test's own on/off control:");
            for (String line : lastBang.split("\n")) say("  " + line);
            if (saved) say("Log saved: flywheel-test/" + logFile.getName());
            telemetry.update();
        }
    }

    // The screen: safety, where it is, the one next thing, then the numbers.
    private void show(double rpm, double loopMs, boolean waitForRelease) {
        boolean burst = shotInBurst >= 0;
        if (waitForRelease) say("!! Let go of all sticks and buttons.");
        if (burst) say("!! It is FIRING. {B} = stop now.");
        else if (on) say("!! Flywheel ON. {B} = stop. Nobody in front.");
        else say("!! {B} or {X} spins the flywheel. Hands clear.");
        if (loopMs > 25) say("!! Loop too slow: a burst will not count.");
        say("--- Flywheel Speed Test (advanced) ---");
        if (burst) say("Next: wait. Feeding ball " + (shotInBurst + 1) + " of " + BURST + ".");
        else if (on) say("Next: {RB} = fire 4 balls.");
        else say("Next: load 4 balls, then {B} = flywheel on.");
        say("Control : " + controlWords());
        say("Speed : now " + FinderResults.num(rpm, 0) + String.format(Locale.US, " RPM, target %.0f", targetRpm));
        say(String.format(Locale.US, "Battery : %.2f V", volts));
        say(String.format(Locale.US, "Test's kS %.3f, kV %.6f (%s)", kS, kV, kSource.startsWith("measured") ? "measured" : "guess"));
        say("They are NOT bangKs and bangKv:");
        say("other units, other control. Do not copy them.");
        if (!note.isEmpty()) for (String line : note.split("\n")) say(line);
        String last = mode == Mode.HUB ? lastHub : lastBang;
        say("Last burst with this control:");
        for (String line : last.split("\n")) say("  " + line);
        if (!burst) {
            say("{A} = other control. D-pad = speed.");
            say("{X} = measure on/off power (EMPTY launcher).");
        }
        telemetry.update();
    }

    private String controlWords() {
        return mode == Mode.HUB ? "Hub control (PID)" : "On/off control (this test's own)";
    }

    // Button names as printed on this controller (the marks are letters in the words).
    private void say(String line) {
        telemetry.addLine(ButtonNames.say(line));
    }

    private boolean anyPressed() {
        return gamepad1.a || gamepad1.b || gamepad1.x || gamepad1.y || gamepad1.left_bumper || gamepad1.right_bumper
                || gamepad1.dpad_up || gamepad1.dpad_down || gamepad1.dpad_left || gamepad1.dpad_right
                || gamepad1.left_trigger > 0.1 || gamepad1.right_trigger > 0.1
                || Math.abs(gamepad1.left_stick_x) > 0.1 || Math.abs(gamepad1.left_stick_y) > 0.1
                || Math.abs(gamepad1.right_stick_x) > 0.1 || Math.abs(gamepad1.right_stick_y) > 0.1;
    }

    private void cancelBurst(String why) {
        shotInBurst = -1;
        burstId++;                    // the stopped burst is not summed up
        for (int i = 0; i < BURST; i++) pushAt[i] = Double.NaN;
        feed(0);
        coast();
        on = false;
        String result = "stopped: not counted";
        if (burstMode == Mode.HUB) lastHub = result; else lastBang = result;
        burstMode = null;
        note = why + "\nNext: load 4 balls and test again.";
        saveLog();
    }

    // ------------------------------------------------------------------------------------------------
    // Speed control
    // ------------------------------------------------------------------------------------------------

    /** Sends this loop's command for the mode. Returns the power sent (BANG) or NaN (HUB: the hub decides). */
    private double control(double rpm) {
        if (!on) return 0;
        if (mode == Mode.HUB) {
            // The same as R1 today: the hub's PIDF with Tunables.Launcher kP/kI/kD/kF.
            double tps = targetRpm / 60.0 * COUNTS_PER_REV;
            if (flywheel.getMode() != DcMotor.RunMode.RUN_USING_ENCODER) {
                flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                flywheel.setVelocityPIDFCoefficients(Tunables.Launcher.kP, Tunables.Launcher.kI,
                        Tunables.Launcher.kD, Tunables.Launcher.kF);
            }
            flywheel.setVelocity(tps);
            return Double.NaN;
        }
        // BANG: full power far below the target, coast far above it, feedforward + small P near it.
        if (flywheel.getMode() != DcMotor.RunMode.RUN_WITHOUT_ENCODER) flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        double error = targetRpm - rpm;
        if (bangFull && error < BAND * targetRpm) bangFull = false;
        else if (!bangFull && error > BAND * BAND_EXIT * targetRpm) bangFull = true;
        double power;
        if (bangFull) power = 1.0;
        else if (error < -BAND * targetRpm) power = 0;   // far too fast: coast down (no reverse power)
        else power = kS + kV * targetRpm + KP_BANG * error;
        power = Math.max(0, Math.min(1, power));
        flywheel.setPower(power);
        return power;
    }

    private void switchMode(Mode m) {
        mode = m;
        bangFull = true;
        // Force the mode set-up again in control().
        flywheel.setMode(m == Mode.HUB ? DcMotor.RunMode.RUN_WITHOUT_ENCODER : DcMotor.RunMode.RUN_USING_ENCODER);
        if (!on) coast();
    }

    private void coast() {
        flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        flywheel.setPower(0);
    }

    /**
     * Measures kS and kV: holds power 0.4, then 0.7, 3 s each, and takes the average speed of the last second
     * of each. The line through the two points gives power = kS + kV x RPM. Nothing must be loaded.
     * B or STOP ends it at once.
     */
    private void measureKsKv() {
        on = false;
        note = "";
        double[] powers = {0.4, 0.7};
        double[] speeds = new double[2];
        flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        for (int i = 0; i < 2 && opModeIsActive(); i++) {
            flywheel.setPower(powers[i]);
            double start = now(), sum = 0;
            int n = 0;
            while (opModeIsActive() && now() - start < 3.0) {
                for (LynxModule h : hubs) h.clearBulkCache();
                // SAFETY: B stops it in this same loop. A B pressed at any moment of the 6 s counts.
                if (gamepad1.b) {
                    coast();
                    feed(0);
                    on = false;
                    note = "You stopped the power test.\nThe old numbers are kept.";
                    log.append("# kS/kV measure stopped with B\n");
                    return;
                }
                if (now() - start > 2.0) { sum += flywheel.getVelocity() / COUNTS_PER_REV * 60.0; n++; }
                say("!! The wheel spins by itself. {B} = stop now.");
                say("--- Power test " + (i + 1) + " of 2 ---");
                say("Next: wait. Hands clear.");
                say(String.format(Locale.US, "Power : %.1f", powers[i]));
                say("Speed : " + FinderResults.num(flywheel.getVelocity() / COUNTS_PER_REV * 60.0, 0) + " RPM");
                telemetry.update();
            }
            if (!opModeIsActive()) {
                coast();              // STOP: a part of a test is not a result
                return;
            }
            speeds[i] = n > 0 ? sum / n : 0;
        }
        coast();
        if (speeds[1] - speeds[0] > 100) {
            kV = (powers[1] - powers[0]) / (speeds[1] - speeds[0]);
            kS = powers[0] - kV * speeds[0];
            volts = battery.getVoltage();   // a fresh reading
            kSource = String.format(Locale.US, "measured at %.1f V", volts);
            log.append(String.format(Locale.US, "# kS %.4f kV %.7f (%.0f rpm at 0.4, %.0f rpm at 0.7, %.2f V)\n",
                    kS, kV, speeds[0], speeds[1], volts));
        } else {
            kSource = "old numbers";
            note = "Power test FAILED: the wheel did not speed up.\nNext: check it turns freely and its cable.";
        }
        on = false;   // the wheel is coasting: press B to spin it up again
        mode = Mode.BANG;
        bangFull = true;
    }

    // ------------------------------------------------------------------------------------------------
    // Burst of 4, the same rhythm in both modes (it does not wait for "ready": that is what we measure)
    // ------------------------------------------------------------------------------------------------

    private void startBurst(double t) {
        burstId++;
        burstMode = mode;
        burstSlow = false;
        note = "";
        shotInBurst = 0;
        shotStart = t;
        for (int i = 0; i < BURST; i++) { pushAt[i] = Double.NaN; rpmAtPush[i] = Double.NaN; minAfter[i] = Double.NaN; backAt[i] = Double.NaN; }
        inBandSince = Double.NaN;
    }

    /** Runs the feeder for the current shot. Returns an event name for the log, or "". */
    private String runBurst(double t, double rpm) {
        trackRecovery(t, rpm);
        if (shotInBurst < 0) {
            // The last ball may still be recovering after the burst ends: keep the summary up to date.
            if (burstId > 0 && burstMode != null) summarize();
            return "";
        }
        int i = shotInBurst;
        double s = t - shotStart;
        double feedSec = num(Tunables.Feeder.feedSec, 0.4);
        String event = "";

        // Both feed wheels: on from the start of the shot for feedSec (the ball goes in during that time).
        feed(s < feedSec ? num(Tunables.Feeder.power, 1.0) : 0);
        if (Double.isNaN(pushAt[i])) {
            pushAt[i] = t;
            rpmAtPush[i] = rpm;
            minAfter[i] = rpm;
            event = "push";
        }
        if (s >= feedSec + GAP_SEC) {
            shotInBurst++;
            shotStart = t;
            if (shotInBurst >= BURST) {
                shotInBurst = -1;
                feed(0);
                summarize();
                saveLog();
                return event.isEmpty() ? "burst_end" : event;
            }
        }
        return event;
    }

    /** Both feed wheels at the same power (the right one turns the other way: its direction is REVERSE). */
    private void feed(double power) {
        feedLeft.setPower(power);
        feedRight.setPower(power);
    }

    /** After each push: the lowest speed, and when it is back within READY_RPM (for READY_HOLD_SEC). */
    private void trackRecovery(double t, double rpm) {
        for (int i = 0; i < BURST; i++) {
            if (Double.isNaN(pushAt[i]) || !Double.isNaN(backAt[i])) continue;
            // Only the latest shot that is still recovering is tracked.
            if (i + 1 < BURST && !Double.isNaN(pushAt[i + 1])) { backAt[i] = Double.POSITIVE_INFINITY; continue; }
            minAfter[i] = Math.min(minAfter[i], rpm);
            boolean dropped = targetRpm - minAfter[i] > READY_RPM;   // the ball has hit the wheel
            boolean inBand = Math.abs(rpm - targetRpm) <= READY_RPM;
            if (dropped && inBand) {
                if (Double.isNaN(inBandSince)) inBandSince = t;
                if (t - inBandSince >= READY_HOLD_SEC) { backAt[i] = inBandSince; inBandSince = Double.NaN; }
            } else {
                inBandSince = Double.NaN;
            }
        }
    }

    private void summarize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < BURST; i++) {
            if (i > 0) sb.append('\n');
            if (Double.isNaN(pushAt[i])) { sb.append("feed ").append(i + 1).append(": none"); continue; }
            double drop = targetRpm - minAfter[i];
            if (drop <= READY_RPM) {
                // The speed stayed within 100 RPM: no drop to time (maybe no ball).
                sb.append("feed ").append(i + 1).append(": stayed within 100 RPM");
                continue;
            }
            String back;
            if (Double.isInfinite(backAt[i])) back = "NOT back before the next";
            else if (!Double.isNaN(backAt[i])) back = String.format(Locale.US, "back in %.2f s", backAt[i] - pushAt[i]);
            else back = "not back yet";                               // still recovering
            sb.append(String.format(Locale.US, "feed %d: %.0f under, %s", i + 1, drop, back));
        }
        String text = burstSlow ? "NOT counted: loops were too slow.\nNext: test again." : sb.toString();
        if (burstMode == Mode.HUB) lastHub = text; else lastBang = text;
    }

    // ------------------------------------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------------------------------------

    /** The battery, read at most every 0.25 s (each read costs a few ms). */
    private void readBattery(double t) {
        if (t < nextVoltsAt) return;
        nextVoltsAt = t + 0.25;
        volts = battery.getVoltage();
    }

    private boolean saveLog() {
        try (FileWriter w = new FileWriter(logFile, false)) {
            w.write(log.toString());
            return true;
        } catch (IOException e) {
            telemetry.addLine("!! Log NOT saved. Write the numbers down.");
            return false;
        }
    }

    /** A Tunables value, or the library's starting value when the file says DEFAULT (a special NaN). */
    private static double num(double v, double fallback) {
        return Double.isNaN(v) ? fallback : v;
    }

    private double now() {
        return System.nanoTime() / 1e9;
    }
}
