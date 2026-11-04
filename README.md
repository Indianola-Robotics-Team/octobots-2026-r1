# Octobots 2026-27, robot 1

The code for Indianola Robotics' (FTC team 8938) first robot of the BIOBUZZ season. It is the FTC SDK
(FtcRobotController 12.0) with our helper library, [ftc-helper-lib](https://github.com/crossbotics/ftc-helper-lib),
in the `ftc-helper-lib` folder (a git submodule). **Library version: v0.26.0** (`git submodule status`
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
| `autos/` | the autonomous: **R1 Launch and Park** (below) |

How the library works: `ftc-helper-lib/docs/modules/robot.md`; short "how do I...?" answers:
`ftc-helper-lib/docs/cookbook.md`.

**Tune and check** (the library's own OpMode, TeleOp list, "Finders" group; no file in our code) has
every check and finder for this robot in one menu: D-pad up/down to pick, **A** to open, **START** to run;
STOP, then INIT, for the next one. For robot 1 it lists the **Hardware Check** (every device found, each
motor turns, the IMU), the **Servo Finder** (the deploy servo's positions), the **Flywheel Finder** (the
launcher's kP..kF), the **Drive Tuner** (the wheel size, for the autonomous) and the **Shot Tuner** (not
used by robot 1: it shoots from one spot). They show the lines to copy into `Tunables.java` or
`Robot.java`. One file of our own stays in `finders/`: **R1 Motor Direction Check** (robot on a block:
X = left, Y = right, each wheel must roll forward), because it uses our drive's `.reversed(...)`.
The full first-time order, with what "pass" means at each step: the team pages' Part C,
"The first robot sessions" (https://crossbotics.com/teams/8938-octobots/robot/).
How: `ftc-helper-lib/docs/guides/finders.md`.

## Version 1

goBILDA's BIOBUZZ starter bot (3200-2627-0003) with our changes: an intake that folds up at the start
and is pushed over by a servo at START (then gravity holds it on a hard stop), Offset surgical tubing
rollers (90 mm and 60 mm taking turns), two goBILDA 72 mm Boot-Wheel spinners at the front corners driven
from the intake through miter gears, a flywheel on a sliding mount (so a NECTAR fits), and a ramp that sends
shots out of the back. Device names are goBILDA's (`left_drive`, `right_drive`, `intake`, `launcher`,
`feeder`: a goBILDA Speed servo set to spin, it feeds a POLLEN into the flywheel) plus `intake_deploy`
(the deploy servo). v1 shoots POLLEN only, for now.

**Buttons (R1 TeleOp, one driver on gamepad 1):** left stick drive, right stick X turn; **LT slow** (the
further you pull it, the slower, like last season); left stick click slow on/off. **B** intake on/off,
double-tap B pushes out until the next tap (the intake starts by itself once it is deployed). The flywheel spins
up at START and stays at speed. **LB** lets it coast (when parked; again: spin up), **RB** fires one POLLEN (hold to keep firing), D-pad up/down flywheel
speed. **X** deploys the intake again. **BACK** pauses everything (again: go on). **GUIDE** shows this
map (hold GUIDE 1 s after START, PRACTICE profile: the tuning menu). Free: A, Y, RT, D-pad left/right.
**Four pieces at most** (rule G407): nothing in the code stops the intake at 4 (`holds(4)` only tells
the simulator), so the driver taps **B** (intake off) when the robot is full.

**Still estimates (marked "est." in Robot.java):** the intake's hinge and arm, the spinners' place (goBILDA
72 mm Boot-Wheels, flush with the roller and low, from photos; the roller's tubing is soft in the simulator), the
deploy servo's arm, the flywheel slide, the ramp, the shot angle. Measure the robot and replace them.
**Still to find on the robot**, in this order:
1. **Tune and check > Hardware Check**: every device found, each motor and servo turns. Then
   **R1 Motor Direction Check** (robot on a block): each drive wheel rolls forward.
2. How the Control Hub is mounted (`imu(...)` in Robot.java: the way its logo and USB port face).
3. The deploy servo's positions with **Tune and check > Servo Finder** (`Tunables.IntakeDeploy`: `stowed`,
   `deployed`). `travelSec` is the time for a full 0-to-1 move (now from the servo's catalog speed):
   keep it, or time the push and divide by `deployed - stowed`.
4. The flywheel's numbers with **Tune and check > Flywheel Finder** (`Tunables.Launcher` kP..kF: today goBILDA's),
   and the feeder's `feedSec` (the library's 0.4 s for now: watch it feed one piece, never two).
5. The wheel size with **Tune and check > Drive Tuner**, then `Tunables.Drive.calibrated = true` (until then the
   autonomous refuses to run).
6. The shot: its real launch angle and speed (see "The autonomous" below), then the start spot.
7. Every number marked "est." or GUESS in Robot.java (measure the robot), then remake the simulator's
   robot file (below).

## The autonomous: R1 Launch and Park

`autos/LaunchAndPark.java`. It fires the four preloaded POLLEN into our HIVE's up CELL, then parks
partly in our LOADING ZONE without touching a wall (LEAVE 3 + PARK 5). Same route for RED and BLUE,
each from its own wall (the field is turned, not mirrored).

**Place the robot** (Driver Station: "Nose corner on wall 20.5in from R, back at HIVE"): on our side,
right of centre, the robot's **front-right corner touching our wall, 20.5 in from the right-hand wall**,
the body turned 52 degrees to the wall so the **back (the ramp) points at our HIVE's up CELL**. Intake
folded. Legal by G304: touching the wall, in our half, not in the LOADING ZONE, clear of the FLOWER.

**Why there:** the up CELL's opening faces the audience side, so a shot must come down into it from that
side. The simulator's hit rate (400 shots, realistic scatter) from spots along our wall found a
band right of centre; this spot is the best one that keeps 4.5 in from the GARDEN's POLLEN:

| Shot (from the spot, after backing 4 in) | 210 in/s | 220 in/s | 230 in/s | 240 in/s |
| --- | --- | --- | --- | --- |
| 70 degrees up | 1 % | 71 % | 98 % | 30 % |
| 65 degrees up | 8 % | 96 % | 90 % | 5 % |
| 60 or 55 degrees up | 0 | 0 | 0 | 0 |

**Important:** the built ramp is 53 degrees up from the floor (measured 2026-10-27), and from this spot
that misses. The robot file now uses **65 degrees** (the plan for the ramp; mentor, 2026-10-27), so the
shot is 115 degrees. At 65 degrees the speed matters: the simulator's exit speed is half the wheel's rim
speed, so goBILDA's 2679 RPM is about 265 in/s and misses. `Tunables.Launcher.speedRpm` is now **2275 RPM**
(225 in/s), which scores 4 of 4. Find the real speed on the robot once the ramp is 65 degrees.

**Route** (tank: turns, forward and back only; numbers in `Tunables.Auto`): back 4 in off the wall,
fire all four (the intake stays folded until now: at START it would fall onto the wall, so the auto
sets `Robot.deployAtStart = false`); turn 45 degrees left, push the intake over, back 85.5 in along
our wall past our FLOWER; turn to face the far wall and back 12.6 in into the LOADING ZONE. It ends
about 2.5-3 in from our wall, partly in the zone, after about 17 s. `parkWithSeconds(10)` keeps
time for the park steps if firing is slow.

**Check it** (from the `ftc-helper-lib` folder): `./gradlew :sim:scenario -Pfile=../simulator/scenarios/r1-auto.sim`.
It checks the start spot, four fired, the intake pushed over off the wall, no bumps, the end pose
(at least 2 in from the wall, in the zone), under 25 s, and prints the shot verdicts and points. With
the robot file's 65 degree shot at 2275 RPM: 4 of 4 clean, the HIVE tips (+20), red 37. (At 2679 RPM: 0 of 4,
too fast. At 53 degrees: 0 of 4.)

**Measure on the robot before trusting it:**
1. The shot: film a shot from the side next to a tape measure. Launch angle (degrees up from the
   floor) and exit speed (inches per second). Put the angle in Robot.java (`shotAngle(180 - up)`),
   remake the robot file, and run the hit rate from the start spot (simulator page: Hit rate).
2. Then the speed: tune `Tunables.Launcher.speedRpm` from the start spot until all four go in.
3. That the four preloads fire with the intake still folded up.
4. The drive: the Drive Tuner (Tune and check) first (the inches are only right on a tuned robot), then check the end
   spot: at least 2 in from the wall, partly in the LOADING ZONE. Change `Tunables.Auto` if not.
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
git -C ftc-helper-lib checkout v0.26.0        # the new version's tag (an example)
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

and the autonomous (from its start spot it fires all four, then parks in the LOADING ZONE):

```bash
./gradlew :sim:scenario -Pfile=../simulator/scenarios/r1-auto.sim
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

No feeder on the robot? It is `optional()` too: leave `feeder` out of the robot configuration. INIT
says the feeder is off, the flywheel still spins, and RB feeds nothing. `r1-no-feeder.sim` checks this (`set device feeder missing`).

Drive it in a browser (then open http://localhost:8780 and pick "R1 TeleOp"):

```bash
./gradlew :sim:run -PteamCode=../TeamCode
```
