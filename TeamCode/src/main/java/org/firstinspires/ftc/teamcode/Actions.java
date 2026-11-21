package org.firstinspires.ftc.teamcode;

import com.crossbotics.ftchelper.core.Time;
import com.crossbotics.ftchelper.robot.Action;
import java.util.function.BooleanSupplier;

/** Robot 1, version 1 (file 4 of 5): steps that use several parts together. */
public class Actions {

    /**
     * Pushes the folded intake over, then starts the intake and moves the servo back out of the way.
     * The intake stays down by gravity (nothing holds it).
     */
    public static Action deployIntake(Robot r) {
        return Action.named("Deploy intake")
                .then(() -> r.intakeDeploy.goTo("deployed"))
                // A servo cannot say where it is: isDone() guesses from Tunables.IntakeDeploy.travelSec.
                .waitUntil("the deploy servo", r.intakeDeploy::isDone, 2.0)
                // The intake is down now: start it (it keeps running when this action ends).
                .then(r.intake::in, () -> r.intakeDeploy.goTo("stowed"));
    }

    /**
     * Waits until the flywheel has stayed READY for {@code steadySec} seconds in a row. For an autonomous,
     * before it fires: the wheel speeds up from 0 at START and passes through READY on its way up, then
     * goes on too fast for a moment (the first shot went long). In TeleOp it spins all the time, so it is
     * already steady. Give the step a time limit: {@code .action(Actions.steadyWheel(robot, 0.75), 4)}.
     */
    public static Action steadyWheel(Robot r, double steadySec) {
        BooleanSupplier steady = new BooleanSupplier() {
            private long readySince = -1;   // when READY started (Time.nanos), -1 = not READY now
            @Override public boolean getAsBoolean() {
                if (!r.launcher.isReady()) { readySince = -1; return false; }
                if (readySince < 0) readySince = Time.nanos();
                return (Time.nanos() - readySince) / 1e9 >= steadySec;
            }
        };
        return Action.named("Steady wheel")
                .then(r.launcher::spinUp)
                .waitUntil("the wheel READY for " + steadySec + " s", steady, 10);
    }
}
