# Octobots 2026-27, robot 1

The code for Indianola Robotics' (FTC team 8938) first robot of the BIOBUZZ season. It is the FTC SDK
(FtcRobotController 12.0) with our helper library, [ftc-helper-lib](https://github.com/crossbotics/ftc-helper-lib),
in the `ftc-helper-lib` folder (a git submodule). **Library version: v0.30.0** (`git submodule status`
shows it). The team's step-by-step pages (https://crossbotics.com/teams/8938-octobots/) match this version.

## Get it

```bash
git clone --recurse-submodules https://github.com/jasoncross/octobots-2026-r1.git
```

Open the folder in Android Studio (Narwhal 3 Feature Drop or newer). Builds need JDK 21.

## The robot in five files

All in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`:

| File | What it says |
| --- | --- |
| `Robot.java` | what is on the robot: each part's name in the robot configuration, its catalog part, how it looks and where it sits |
| `Tunables.java` | every number, one section per part |
| `Controls.java` | the buttons, and the TeleOp ("R1 TeleOp") |
| `Actions.java` | steps that use several parts (deploy the intake); firing all is the library's `robot.launcher.fireAllAction()` |
| `autos/` | three autonomous programs: **R1 Right Corner: Launch, Park Right**, **R1 Right Wall: Launch, Park Right**, **R1 Left Wall: Wait, Launch, Park Left** (below) |

How the library works: `ftc-helper-lib/docs/modules/robot.md`; short "how do I...?" answers:
`ftc-helper-lib/docs/cookbook.md`.

**Tune and check** (the library's own OpMode, TeleOp list, "Finders" group; no file in our code) has
every check and finder for this robot in one menu: D-pad up/down to pick, **A** to open, **START** to run;
STOP, then INIT, for the next one. For robot 1 it lists the **Hardware Check** (every device found, each
motor turns, the IMU), the **Motor Direction Check** (robot on a block: X = front left, Y = front right,
A = back left, B = back right; each wheel must roll toward the launcher, with our drive's directions from
`Robot.java`), the **Servo Finder** (the deploy servo's positions), the **Flywheel Finder** (the
launcher's kP..kF), the **Drive Tuner** (the wheel size and the strafe correction, for the autonomous) and the **Shot Tuner** (not
used by robot 1: it shoots from one spot). They show the lines to copy into `Tunables.java` or
`Robot.java`. No finder files of our own: the library has them all.
The full first-time order, with what "pass" means at each step: the team pages' Part C,
"The first robot sessions" (https://crossbotics.com/teams/8938-octobots/robot/).
How: `ftc-helper-lib/docs/guides/finders.md`.

## Version 1

A goBILDA **Strafer** chassis (3209-0001-0007): four 312 RPM motors and 104 mm GripForce **mecanum**
wheels (wheels with small rollers at 45 degrees), so the robot can also **strafe** (drive sideways without
turning). On it: an intake that folds up at the start and is pushed over by a servo at START (then gravity
holds it on a hard stop), Offset surgical tubing rollers (90 mm and 60 mm taking turns), two goBILDA 72 mm
Boot-Wheel spinners at the intake's corners driven from the intake through miter gears, a flywheel on a
sliding mount, two feed parts (a **feeder wheel**: Gecko wheels on a goBILDA Speed servo set to spin, it
moves the POLLEN up to the ramp; and a **feeder arm** on a goBILDA torque servo, it pushes each POLLEN up into
the flywheel, then comes back down), and a ramp that sends shots out. Robot 1 handles **POLLEN only**.

**The FRONT is the launcher end** (since 2026-11-04): shots go up the ramp out of the front, and the
intake is at the back. Driving forward = launcher first. Device names: `front_left`, `front_right`,
`back_left`, `back_right` (the drive motors, with the launcher as the front), `imu`, `intake`,
`intake_deploy` (the deploy servo), `launcher`, `feeder_wheel` (the CR servo; it was `feeder`), `feeder_arm`
(the torque servo, a SERVO in the configuration, not a CR servo). **A Control Hub set up with the old names**
(when the intake was the front): keep every wire in its port and swap the four drive names: old
`front_left` -> `back_right`, old `front_right` -> `back_left`, old `back_left` -> `front_right`, old
`back_right` -> `front_left`. Then run **Tune and check > Motor Direction Check**.

**Buttons (R1 TeleOp, one driver on gamepad 1):** left stick drive and strafe, right stick X turn; **LT
slow** (the further you pull it, the slower, like last season); left stick click slow on/off; right stick
click field-centric on/off (forward = away from the driver, however the robot faces). **B** intake on/off,
double-tap B pushes out until the next tap (the intake starts by itself once it is deployed). The flywheel spins
up at START and stays at speed. **LB** lets it coast (when parked; again: spin up), **RB** fires one POLLEN (hold to keep firing), D-pad up/down flywheel
speed. **X** deploys the intake again. **BACK** pauses everything (again: go on). **GUIDE** shows this
map (hold GUIDE 1 s after START, PRACTICE profile: the tuning menu). Free: A, Y, RT, D-pad left/right.
**Four pieces at most** (rule G407): nothing in the code stops the intake at 4 (`holds(4)` only tells
the simulator), so the driver taps **B** (intake off) when the robot is full.

**Still estimates (marked "est." in Robot.java):** the Strafer's sizes (chassis box, track, wheelbase), the intake's hinge and arm, the spinners' place (goBILDA
72 mm Boot-Wheels, flush with the roller and low, from photos; the roller's tubing is soft in the simulator), the
deploy servo's arm, the flywheel slide, the ramp, the shot angle. Measure the robot and replace them.
**Still to find on the robot**, in this order:
1. **Tune and check > Hardware Check**: every device found, each motor and servo turns. Then
   **Tune and check > Motor Direction Check** (robot on a block): all four drive wheels roll toward the launcher.
2. How the Control Hub is mounted (`imu(...)` in Robot.java: the way its logo and USB port face).
   **Every servo, the first time (and after a new horn):** robot on a block, the servo's arm (horn) OFF or
   loose. In **Tune and check > Servo Finder**, choose the servo and press A: it goes to the middle (0.5) and
   holds. Put the arm on in the middle of the part's swing, tighten, and mark horn and shaft with a paint pen.
   Then press D-pad left once (a 0.01 step) and D-pad up a few times, and write down which way "bigger" moves the part (no servo standard:
   goBILDA says clockwise, but the mounting decides). Find each end in small steps until the part JUST
   touches; a humming servo is pushing: back off at once. Then save each position with A. Full steps:
   [A new servo part, step by step](https://crossbotics.com/ftc-helper/docs/guides/finders/#a-new-servo-part-step-by-step).
3. The deploy servo's positions with **Tune and check > Servo Finder** (`Tunables.IntakeDeploy`: `stowed`,
   `deployed`). `travelSec` is the time for a full 0-to-1 move (now from the servo's catalog speed):
   keep it, or time the push and divide by `deployed - stowed`.
4. The flywheel's numbers with **Tune and check > Flywheel Finder** (`Tunables.Launcher` kP..kF: today goBILDA's),
   Then the feeder arm's `rest` and `push` with **Tune and check > Servo Finder** (`Tunables.FeederArm`: INIT
   stops until they are written). Then watch a shot and tune the feeder wheel (`Tunables.FeederWheel`):
   `alwaysOn = false` runs it only while firing: it turns `stageSec` (0.2 s) to bring the POLLEN to the arm, the
   arm pushes, and the wheel stops `stopSec` (0.3 s) after the push starts (below 0 = before the push, 0 = as it
   starts). `alwaysOn = true` runs it all the time the flywheel is on, and the arm pushes at once.
5. The wheel size and the strafe correction with **Tune and check > Drive Tuner**, then `Tunables.Drive.calibrated = true` (until then the
   autonomous refuses to run).
6. The shot: its real launch angle and speed (see "The autonomous programs" below), then each start spot.
7. Every number marked "est." or GUESS in Robot.java (measure the robot), then remake the simulator's
   robot file (below).

## The autonomous programs

Three programs, all in `autos/`. Each fires the four preloaded POLLEN into our HIVE, then parks at a
corner of our LOADING ZONE without touching a wall (LEAVE 3 + PARK 5). Same routes for RED and BLUE, each
from its own wall (the field is turned, not mirrored). Right and left: as our drivers see them from our wall.
Every program fires first, from its start spot, then pushes the intake over WHILE it drives away from the
wall (at START it would fall onto the wall: `Robot.deployAtStart = false`). The mecanum wheels slide the
robot to the zone. PARK counts with any part of the robot over the zone's tape, so each one ends BESIDE
the zone, only its corner over the zone's corner: the zone stays free for our partner.

**Written like the auto builder writes them:** plain numbers in the steps, so the simulator's auto builder
(and crossbotics.com's path builder) can open each file ("Start from one of your autos"), show it on the
map, and change it. A move is `move(name, inches, direction, power)`: the direction in degrees clockwise
from the robot's front. `moveWithTurn(name, inches, direction, heading, power)` turns while it drives (no
time lost); its direction and heading count from the START heading.

| Program | Place the robot | Shoots | Parks |
| --- | --- | --- | --- |
| **R1 Right Corner: Launch, Park Right** (`RightCornerLaunchAndPark.java`) | right of centre on our wall: the **back-left corner touching our wall, 20.5 in from the right-hand wall**, turned 52 degrees so the **front (the ramp) points at our HIVE's up CELL** | at once, at 2525 RPM (`speedRpm`'s number) | the zone's right corner, facing left (it turns while it drives) |
| **R1 Right Wall: Launch, Park Right** (`RightWallLaunchAndPark.java`) | **back on the right wall, the 3rd tile out from our wall, centred in it**, front facing the HIVE | at once, at the usual speed (`speedRpm`) | the zone's right corner, facing left |
| **R1 Left Wall: Wait, Launch, Park Left** (`LeftWallWaitLaunchAndPark.java`) | **back on the left wall, the 3rd tile out**, front facing the HIVE, **0.6 in off centre toward the field's centre line** (centred, it would overlap the FLOWER on that wall by 0.4 in) | after **15 s**, at the usual speed (`speedRpm`) | the zone's left corner, facing right |

**Why the left one waits:** from the left, the HIVE's CELL on our side is the DOWN one at the start. Our
partner's shots from the right tip the HIVE; then the CELL on the left is up and open to us. Run it only
with a partner that scores first. Two of our programs can share the zone: one parks right, one left.

**Why the ramp is 75 degrees** (the plan; the built ramp is 53, measured 2026-10-27): from tile 3 on a
side wall, the up CELL is only about 40 in away. A 65 degree shot that reaches the opening's height there
is still going up, at any speed (the simulator: 0 of 4 from 1500 to 2275 RPM). At 75 degrees it comes down
into it. The simulator (realistic scatter, the exit speed half the wheel's rim speed), from the start
spots: auto 1 4 of 4 at 2500 to 2550 RPM, autos 2 and 3 4 of 4 at 2300 to 2350 RPM. Each auto fires with
`robot.launcher.fireAllAction(rpm)`: that shot's own speed, then back to `speedRpm` (the TeleOp's).

**Routes** (after firing; the intake is pushed over during the first move):
- **Right Corner:** slide 9.4 in straight away from our wall (clear of our FLOWER), then 74.5 in to the
  zone's right corner, turning to face left on the way.
- **Right Wall:** slide 35.8 in off the wall toward ours, then 64.1 in past the HIVE's frame to the zone's
  right corner.
- **Left Wall:** wait 15 s first; after firing, 8 in forward (clear of the FLOWER), then 42.3 in to the
  zone's left corner. `parkWithSeconds(8)` keeps time for the park if firing is slow (the feeder wheel and
  arm take longer a shot than the old feeder: with 10 s, the 4th shot was cut short after the 15 s wait).

**Check them** (from the `ftc-helper-lib` folder): `r1-auto.sim` (Right Corner), `r1-auto-right.sim`,
`r1-auto-left.sim` (`./gradlew :sim:scenario -Pfile=../simulator/scenarios/r1-auto.sim`, and so on). Each
checks the start spot, four fired, the intake pushed over while it drives off the wall, no bumps, and the
end pose (beside the zone at its corner, the heading), and prints the shot verdicts and points: 4 of 4 clean
in all three. The left one first drops four POLLEN into the up CELL, to tip the HIVE like our partner would.

**Measure on the robot before trusting them:**
1. The shot: film a shot from the side next to a tape measure. Launch angle (degrees up from the
   floor) and exit speed (inches per second). Put the angle in Robot.java (`shotAngle(up)`),
   remake the robot file, and run the hit rate from each start spot (simulator page: Hit rate).
2. Then the speeds: from each start spot, change the RPM in that auto's `fireAllAction(...)` until all four
   go in (and `Tunables.Launcher.speedRpm` for the TeleOp).
3. That the four preloads fire with the intake still folded up.
4. The drive: the Drive Tuner (Tune and check) first (the inches are only right on a tuned robot), then
   check each end spot: at least 2 in from the wall, its corner over the LOADING ZONE. If not, open the auto
   in the auto builder, drag its end point, and paste the Java back.
5. Our partner robot must not start or park in the same places.

## Update the library

The project uses ONE exact library version (the submodule). The library has a release about once a
week; we update after an event or in a calm week, never the week before an event. The docs on
crossbotics.com follow the newest release (docs fixes go live between releases), so they can be a
little ahead of our version: `git submodule status` says which one we have, and `ftc-helper-lib/docs`
is the copy for it. Read the new version's notes first (its GitHub release, or the CHANGELOG): each
version says what (if anything) to change. Step by step: the team pages' Part D, "Keep the library up
to date".

```bash
git -C ftc-helper-lib fetch --tags
git -C ftc-helper-lib checkout v0.30.0        # the new version's tag (an example)
./gradlew :TeamCode:assembleDebug             # build; fix what the CHANGELOG says
```

Then, from the `ftc-helper-lib` folder, remake the robot file and run the check (both below). Commit
(`git add ftc-helper-lib TeamCode simulator README.md`), install on the robot and test it there:
the Robot Controller app must be built and installed again for the robot to get the new library.
Teammates then run `git pull` and `git submodule update`.

## The simulator

**The easy way, in Android Studio:** pick **Simulator** or **Simulator tests** in the list next to the
green Run button, then click Run. (Not in the list? The library adds them at the first Gradle sync:
close the project and open it again.) **Simulator** opens the simulator: go to http://localhost:8780 and pick
"R1 TeleOp". **Simulator tests** runs all the tests below. The red Stop button stops it.

`simulator/robot.properties` is made again from Robot.java each time the simulator starts. The numbers
that are still MEASURE in Tunables.java get stand-ins there (simulator only: the real robot still stops
at INIT). The same from the Terminal, in the `ftc-helper-lib` folder:

Check it (size rules, the intake drops at START, picks up a POLLEN, fires it):

```bash
./gradlew :sim:scenario -Pfile=../simulator/scenarios/r1-v1.sim
```

and the three autonomous programs (each fires all four, then parks in the LOADING ZONE):

```bash
./gradlew :sim:scenario -Pfile=../simulator/scenarios/r1-auto.sim
./gradlew :sim:scenario -Pfile=../simulator/scenarios/r1-auto-right.sim
./gradlew :sim:scenario -Pfile=../simulator/scenarios/r1-auto-left.sim
```

No fold-out servo on the robot yet? Nothing to change: `intake_deploy` is `optional()` in `Robot.java`.
Leave it out of the robot configuration: INIT says it is off (at events a loud `!!` warning), the intake
is put down by hand before the match and starts at START, and X does nothing. Add it to the configuration
when the servo is on, and run the **Servo Finder** (Tune and check) before any TeleOp. Note: with the intake down, the
robot may be too long for the 18 in start cube; check it in a sizing box. `r1-no-deploy-servo.sim`
checks this (`set device intake_deploy missing`); in the browser simulator, add `missing = intake_deploy`
to the robot file.

```bash
./gradlew :sim:scenario -Pfile=../simulator/scenarios/r1-no-deploy-servo.sim
```

No feeder on the robot? Both feed parts are `optional()` too: leave `feeder_arm` and `feeder_wheel` out of the
robot configuration. INIT says they are off, the flywheel still spins, and RB feeds nothing. `r1-no-feeder.sim`
checks this (`set device feeder_arm missing`, `set device feeder_wheel missing`).

The feed wheel's timing: `r1-feed-wheel.sim` (only while firing: a wheel that stops before it starts sends
nothing; the default timing sends one POLLEN and the arm comes back) and `r1-feed-wheel-always.sim`
(`alwaysOn = true`: two RB presses, two POLLEN).

Drive it in a browser (then open http://localhost:8780 and pick "R1 TeleOp"):

```bash
./gradlew :sim:run -PteamCode=../TeamCode
```
