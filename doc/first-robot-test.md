# First robot test: Continuous fire and the two new tuners

This branch (`group-fire`) changes how robot 1 shoots. The simulator checked it, but the simulator cannot
show real overshoot, real scoring, or a real jam. Do these steps on the robot, in this order. Each step says
when to stop.

**You need:** a FULL battery (12.6 V or more with the motor off) and a used battery (12.0 to 12.4 V),
12 POLLEN, safety glasses for everybody near the robot, a block to lift the robot, and our HIVE on the field.

**Stop at once, at any step, when:**

- the flywheel or the feed wheels move before you pressed a button, or do not stop when you press one;
- holding RB feeds a POLLEN while the screen says `NOT aimed` or `goal NOT seen`;
- the feed wheels keep running after you let go of RB;
- the flywheel motor is too hot to touch.

Then press STOP on the Driver Hub, set `continuousOnHold = false` in `Tunables.Launcher`, install, and tell the
mentor what you saw.

## 1. Install this branch

Where: a terminal, in the `octobots-2026-r1-group-fire` folder.

```bash
git status
```

It must say `On branch group-fire`. Build and install from Android Studio, as usual.

## 2. Read the INIT screen

1. Pick **R1 TeleOp**. Press INIT. Do not press ▶.
2. Read every `!!` line. **Stop** when one says `Holding fire will NOT work`, or a line names the shot table or
   `speedControl`.
3. `Run the Flywheel Tuner` is fine: bang-bang is not tuned yet, and we use PID.

## 3. The speed reading

In **Tune and check**, run the **Encoder Check**. **Stop** when the launcher's line is not OK: the tuners need
a true speed reading.

## 4. Flywheel Tuner, run 1 (FULL battery, no POLLEN)

1. Robot on the block. Take every POLLEN out. Put in the FULL battery.
2. **Tune and check > Flywheel Tuner**. Press ▶, then A. It runs every test by itself.
3. **Stop** when it says `read 0 or backward` (run the Encoder Check again), `under 10 V` (charge or change the
   battery; the finished tests stay saved), or `took over 9 A` (turn the wheel by hand: something rubs).
4. At `run 1 of 2 done`: press STOP, turn the robot off.

## 5. Flywheel Tuner, run 2 (used battery) and the driving checks

1. Put in the used battery (12.0 to 12.4 V). Open the Flywheel Tuner again, press ▶, then A.
2. For each driving check: take the robot off the block, clear the floor, and drive as the screen says.
3. At the end it says DONE or NOT DONE. **Do not copy its lines.** The Group Shot Tuner reads them itself.

## 6. Group Shot Tuner (real POLLEN, on the field)

1. **Tune and check > Group Shot Tuner**. In INIT, press B for RED or X for BLUE.
2. **Stop** when `Aim offset NOT the same` shows: do what the screen says (copy the saved number into
   `Tunables.java`, install, erase the saved numbers in the tuning menu), then open the tuner again.
3. Press ▶. Drive with the sticks until the screen says `OK` for the camera distance. Press A.
4. Load one POLLEN for each check shot, and exactly 4 for each group. Press A each time, then enter where the
   POLLEN went. Not sure: press X, and it shoots again.
5. **Stop** when the check shots at a spot fail (fewer than 3 of 4 scored). Fix the shot table (Shot Tuner)
   or the aim (Aim Tuner) first.
6. At the end: DONE with a winner, or NOT DONE. DONE: D-pad right shows the lines. Copy them into
   `Tunables.java` and install. NOT DONE: change nothing; page 3 says what the misses point to.

## 7. TeleOp

**Tell the drivers first:** with this branch, RB works in a new way.

- The robot starts turning to the HIVE, and the flywheel spins up, as soon as RB goes down.
- A tap fires its one POLLEN when you let go of RB, not when you press it.
- Holding RB longer than about a third of a second fires a group.

1. **R1 TeleOp**, from 36 in, facing our HIVE.
2. Tap RB: exactly one POLLEN, when you let go.
3. Hold RB with 4 POLLEN: the robot turns to the HIVE, then feeds all four with no break.
4. Hold RB and let go after the first POLLEN: the feed wheels stop at once.
5. Hold RB while facing away from the HIVE: nothing feeds, and the screen says `goal NOT seen`.
6. Tap RB, then press and hold it: SPRAY, as before.

**Stop** at any surprise, and write down what you saw.

## What only the robot shows

- Real overshoot, a real ball's speed loss, and how fast the wheel gets back.
- Whether 4 POLLEN in a row score from 27, 36 and 45 in.
- The camera's loop time with the real webcam.
- A jam in the feed wheels during a group.
