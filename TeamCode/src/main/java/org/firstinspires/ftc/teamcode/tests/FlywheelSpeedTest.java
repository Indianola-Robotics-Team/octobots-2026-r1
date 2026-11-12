package org.firstinspires.ftc.teamcode.tests;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.firstinspires.ftc.teamcode.Tunables;

/**
 * ONE-OFF TEST (2026-11-12): is "bang-bang + feedforward" speed control better than the hub's own speed control
 * for R1's flywheel? Idea from team 16010 Astra Machina. Proposal: ftc-helper-lib docs/design/flywheel-bang-feedforward.md.
 *
 * It does NOT use Robot.java: it drives the flywheel motor, the feeder arm and the feed wheel itself, so both
 * speed-control modes run on the same hardware with the same feeding rhythm.
 *
 * Set up: robot on the floor, front (ramp) toward a net. Load 4 POLLEN the normal way.
 *
 * Gamepad 1:
 *   B           flywheel on / off
 *   A           switch mode: HUB (the hub's PIDF, what R1 uses now) or BANG (bang-bang + feedforward)
 *   X           measure kS and kV for BANG (spins the wheel at two powers, about 6 s; nothing loaded)
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
    private static final double PUSH_SEC = 0.15;          // arm going up (torque servo: 27 deg at 0.2 s per 60 deg = 0.09 s, plus margin)
    private static final double RETURN_SEC = 0.15;        // arm coming back
    private static final double GAP_SEC = 0.25;           // wait after the arm is back, before the next ball (library shotIntervalSec)
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
    private Servo feederArm;
    private CRServo feedWheel;
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
        feederArm = hardwareMap.get(Servo.class, "feeder_arm");
        feedWheel = hardwareMap.get(CRServo.class, "feeder_wheel");
        battery = hardwareMap.voltageSensor.iterator().next();

        targetRpm = Tunables.Launcher.speedRpm;
        // A first kS/kV guess from the hub's own F gain (kF 12.5): power = kF x counts/s / 32767.
        // X measures the real numbers.
        kS = 0;
        kV = Tunables.Launcher.kF * COUNTS_PER_REV / 60.0 / 32767.0;
        kSource = "from hub kF (press X to measure)";

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File dir = new File("/sdcard/FIRST/flywheel-test");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        logFile = new File(dir, "flywheel-" + stamp + ".csv");
        log.append("t,loop_ms,mode,target_rpm,rpm,power,volts,burst,shot,event\n");

        feederArm.setPosition(Tunables.FeederArm.rest);
        feedWheel.setPower(0);
        coast();

        telemetry.addLine("Flywheel Speed Test: B on/off, A mode, X measure kS/kV, RB burst of 4");
        telemetry.addLine("Log: " + logFile.getPath());
        telemetry.update();
        waitForStart();

        boolean lastA = false, lastB = false, lastX = false, lastRb = false, lastUp = false, lastDown = false;
        double t0 = now();
        double lastLoop = t0;

        while (opModeIsActive()) {
            for (LynxModule h : hubs) h.clearBulkCache();
            double t = now();
            double loopMs = (t - lastLoop) * 1000;
            lastLoop = t;
            readBattery(t);

            // Buttons (on press only)
            boolean idle = shotInBurst < 0;
            if (gamepad1.b && !lastB && idle) { on = !on; if (!on) coast(); }
            if (gamepad1.a && !lastA && idle) { switchMode(mode == Mode.HUB ? Mode.BANG : Mode.HUB); }
            if (gamepad1.x && !lastX && idle) { measureKsKv(); lastLoop = now(); }
            if (gamepad1.dpad_up && !lastUp && idle) targetRpm = Math.min(MAX_RPM, targetRpm + 50);
            if (gamepad1.dpad_down && !lastDown && idle) targetRpm = Math.max(500, targetRpm - 50);
            if (gamepad1.right_bumper && !lastRb && idle && on) startBurst(t);
            lastA = gamepad1.a; lastB = gamepad1.b; lastX = gamepad1.x; lastRb = gamepad1.right_bumper;
            lastUp = gamepad1.dpad_up; lastDown = gamepad1.dpad_down;

            double rpm = flywheel.getVelocity() / COUNTS_PER_REV * 60.0;
            double power = control(rpm);
            String event = runBurst(t, rpm);

            log.append(String.format(Locale.US, "%.4f,%.1f,%s,%.0f,%.1f,%.3f,%.2f,%d,%d,%s\n",
                    t - t0, loopMs, mode, on ? targetRpm : 0, rpm, power, volts,
                    shotInBurst >= 0 ? burstId : 0, shotInBurst + 1, event));   // shot 0 = no burst running

            telemetry.addData("Mode", "%s   (A switches)", mode);
            telemetry.addData("Flywheel", on ? String.format(Locale.US, "%.0f / %.0f rpm", rpm, targetRpm) : "off (B)");
            telemetry.addData("Loop", "%.0f ms%s", loopMs, loopMs > 25 ? "  TOO SLOW: result does not count" : "");
            telemetry.addData("Battery", "%.2f V", volts);
            telemetry.addData("BANG kS / kV", "%.3f / %.6f  %s", kS, kV, kSource);
            telemetry.addData("Burst", shotInBurst >= 0 ? "firing ball " + (shotInBurst + 1) : "RB fires 4");
            telemetry.addLine("Last HUB burst:  " + lastHub);
            telemetry.addLine("Last BANG burst: " + lastBang);
            telemetry.addLine("(drop = lowest RPM below target after a shot; back = time to get within 100 RPM)");
            telemetry.update();
        }

        coast();
        feedWheel.setPower(0);
        feederArm.setPosition(Tunables.FeederArm.rest);
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
     */
    private void measureKsKv() {
        double[] powers = {0.4, 0.7};
        double[] speeds = new double[2];
        flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        for (int i = 0; i < 2 && opModeIsActive(); i++) {
            flywheel.setPower(powers[i]);
            double start = now(), sum = 0;
            int n = 0;
            while (opModeIsActive() && now() - start < 3.0) {
                for (LynxModule h : hubs) h.clearBulkCache();
                if (now() - start > 2.0) { sum += flywheel.getVelocity() / COUNTS_PER_REV * 60.0; n++; }
                telemetry.addData("Measuring kS/kV", "power %.1f, %.0f rpm", powers[i], flywheel.getVelocity() / COUNTS_PER_REV * 60.0);
                telemetry.update();
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
            kSource = "MEASURE FAILED (wheel did not speed up): kept the old numbers";
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
            if (burstId > 0) summarize();
            return "";
        }
        int i = shotInBurst;
        double s = t - shotStart;
        double stage = num(Tunables.FeederWheel.stageSec, 0.2);
        double stop = num(Tunables.FeederWheel.stopSec, 0.3);
        String event = "";

        // Feed wheel: on from the start of the shot until "stop" seconds after the push starts.
        feedWheel.setPower(s < stage + stop ? num(Tunables.FeederWheel.power, 1.0) : 0);
        if (s >= stage && Double.isNaN(pushAt[i])) {
            pushAt[i] = t;
            rpmAtPush[i] = rpm;
            minAfter[i] = rpm;
            feederArm.setPosition(Tunables.FeederArm.push);
            event = "push";
        }
        if (s >= stage + PUSH_SEC) feederArm.setPosition(Tunables.FeederArm.rest);
        if (s >= stage + PUSH_SEC + RETURN_SEC + GAP_SEC) {
            shotInBurst++;
            shotStart = t;
            if (shotInBurst >= BURST) {
                shotInBurst = -1;
                feedWheel.setPower(0);
                summarize();
                saveLog();
                return event.isEmpty() ? "burst_end" : event;
            }
        }
        return event;
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
            double drop = targetRpm - minAfter[i];
            String back;
            if (Double.isInfinite(backAt[i])) back = "not before next ball";
            else if (!Double.isNaN(backAt[i])) back = String.format(Locale.US, "%.2fs", backAt[i] - pushAt[i]);
            else if (drop <= READY_RPM) back = "never left";      // the speed stayed within 100 RPM
            else back = "...";                                     // still recovering
            if (Double.isNaN(pushAt[i])) { sb.append("[-] "); continue; }
            sb.append(String.format(Locale.US, "[%.0f at push, drop %.0f, back %s] ", rpmAtPush[i], drop, back));
        }
        String text = String.format(Locale.US, "#%d at %.0f rpm: ", burstId, targetRpm) + sb;
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

    private void saveLog() {
        try (FileWriter w = new FileWriter(logFile, false)) {
            w.write(log.toString());
        } catch (IOException e) {
            telemetry.addLine("LOG NOT SAVED: " + e.getMessage());
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
