/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.arcade.ms_pacman;

import de.amr.games.pacman.model.HuntingTimer;
import de.amr.games.pacman.event.GameEventType;
import de.amr.games.pacman.lib.Vector2i;
import de.amr.games.pacman.lib.Waypoint;
import de.amr.games.pacman.lib.tilemap.LayerID;
import de.amr.games.pacman.lib.tilemap.WorldMap;
import de.amr.games.pacman.lib.timer.TickTimer;
import de.amr.games.pacman.model.*;
import de.amr.games.pacman.model.actors.Bonus;
import de.amr.games.pacman.model.actors.Ghost;
import de.amr.games.pacman.model.actors.MovingBonus;
import de.amr.games.pacman.model.actors.Pac;
import de.amr.games.pacman.steering.RuleBasedPacSteering;
import de.amr.games.pacman.steering.Steering;
import org.tinylog.Logger;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static de.amr.games.pacman.Globals.*;
import static de.amr.games.pacman.lib.tilemap.WorldMap.*;
import static de.amr.games.pacman.model.actors.GhostState.*;
import static java.util.Objects.requireNonNull;

/**
 * Ms. Pac-Man Arcade game.
 *
 * <p>There are however some differences to the original.
 * <ul>
 *     <li>Attract mode is just a random hunting for at least 20 seconds.</li>
 *     <li>Timing of hunting phases unclear, just took all the information I had</li>
 *     <li>Bonus does not follow original "fruit paths" but randomly selects a portal to
 *     enter the maze, turns around the house and leaves the maze at a random portal on the other side</li>
 * </ul>
 * </p>
 *
 * @author Armin Reichert
 */
public class ArcadeMsPacMan_GameModel extends GameModel {

    public static Ghost blinky() {
        return new Ghost(GameModel.RED_GHOST_ID, "Blinky");
    }

    public static Ghost pinky() {
        return new Ghost(GameModel.PINK_GHOST_ID, "Pinky");
    }

    public static Ghost inky() {
        return new Ghost(GameModel.CYAN_GHOST_ID, "Inky");
    }

    public static Ghost sue() {
        return new Ghost(GameModel.ORANGE_GHOST_ID, "Sue");
    }

    // These are the Pac-Man level data as given in the Pac-Man dossier.
    // I have no information that Ms. Pac-Man uses different data.
    private static final byte[][] LEVEL_DATA = {
        /* 1*/ { 80, 75, 40,  20,  80, 10,  85,  90, 50, 6, 5},
        /* 2*/ { 90, 85, 45,  30,  90, 15,  95,  95, 55, 5, 5},
        /* 3*/ { 90, 85, 45,  40,  90, 20,  95,  95, 55, 4, 5},
        /* 4*/ { 90, 85, 45,  40,  90, 20,  95,  95, 55, 3, 5},
        /* 5*/ {100, 95, 50,  40, 100, 20, 105, 100, 60, 2, 5},
        /* 6*/ {100, 95, 50,  50, 100, 25, 105, 100, 60, 5, 5},
        /* 7*/ {100, 95, 50,  50, 100, 25, 105, 100, 60, 2, 5},
        /* 8*/ {100, 95, 50,  50, 100, 25, 105, 100, 60, 2, 5},
        /* 9*/ {100, 95, 50,  60, 100, 30, 105, 100, 60, 1, 3},
        /*10*/ {100, 95, 50,  60, 100, 30, 105, 100, 60, 5, 5},
        /*11*/ {100, 95, 50,  60, 100, 30, 105, 100, 60, 2, 5},
        /*12*/ {100, 95, 50,  80, 100, 40, 105, 100, 60, 1, 3},
        /*13*/ {100, 95, 50,  80, 100, 40, 105, 100, 60, 1, 3},
        /*14*/ {100, 95, 50,  80, 100, 40, 105, 100, 60, 3, 5},
        /*15*/ {100, 95, 50, 100, 100, 50, 105, 100, 60, 1, 3},
        /*16*/ {100, 95, 50, 100, 100, 50, 105, 100, 60, 1, 3},
        /*17*/ {100, 95, 50, 100, 100, 50, 105,   0,  0, 0, 0},
        /*18*/ {100, 95, 50, 100, 100, 50, 105, 100, 60, 1, 3},
        /*19*/ {100, 95, 50, 120, 100, 60, 105,   0,  0, 0, 0},
        /*20*/ {100, 95, 50, 120, 100, 60, 105,   0,  0, 0, 0},
        /*21*/ { 90, 95, 50, 120, 100, 60, 105,   0,  0, 0, 0},
    };

    // To assure that the demo level runs at least 20 seconds:
    private static final int DEMO_LEVEL_MIN_DURATION_SEC = 20;

    private static final byte PELLET_VALUE = 10;
    private static final byte ENERGIZER_VALUE = 50;
    private static final short POINTS_ALL_GHOSTS_EATEN_IN_LEVEL = 12_000;

    private static final byte[] BONUS_VALUE_FACTORS = {1, 2, 5, 7, 10, 20, 50};
    private static final byte[] KILLED_GHOST_VALUE_MULTIPLIER = {2, 4, 8, 16}; // factor * 100 = value

    private final Steering autopilot = new RuleBasedPacSteering(this);
    protected Steering demoLevelSteering = new RuleBasedPacSteering(this);

    private byte cruiseElroy; //TODO is this existing in Ms. Pac-Man at all?

    protected final ArcadeMsPacMan_HuntingTimer huntingTimer = new ArcadeMsPacMan_HuntingTimer();;
    protected final ArcadeMsPacMan_LevelCounter levelCounter = new ArcadeMsPacMan_LevelCounter();
    protected final MapSelector mapSelector;

    public ArcadeMsPacMan_GameModel() {
        this(new ArcadeMsPacMan_MapSelector());
    }

    protected ArcadeMsPacMan_GameModel(MapSelector mapSelector) {
        this.mapSelector = requireNonNull(mapSelector);
        scoreManager.setHighScoreFile(new File(HOME_DIR, "highscore-ms_pacman.xml"));
        scoreManager.setExtraLifeScores(10_000);
        huntingTimer.setOnPhaseChange(() -> level.ghosts(HUNTING_PAC, LOCKED, LEAVING_HOUSE).forEach(Ghost::reverseASAP));
        lastLevelNumber = Integer.MAX_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LevelCounter> T levelCounter() {
        return (T) levelCounter;
    }

    @Override
    public MapSelector mapSelector() {
        return mapSelector;
    }

    @Override
    public void init() {
        initialLivesProperty().set(3);
        simulateOverflowBugProperty().set(true);
        mapSelector.loadAllMaps(this);
    }

    @Override
    public void resetEverything() {
        resetForStartingNewGame();
    }

    @Override
    public void resetForStartingNewGame() {
        playingProperty().set(false);
        livesProperty().set(initialLivesProperty().get());
        level = null;
        demoLevelProperty().set(false);
        cruiseElroy = 0;
        levelCounter().reset();
        scoreManager().loadHighScore();
        scoreManager.resetScore();
        gateKeeper.reset();
        huntingTimer.reset();
    }

    @Override
    public void endGame() {
        playingProperty().set(false);
        if (!THE_COIN_MECHANISM.isEmpty()) {
            THE_COIN_MECHANISM.consumeCoin();
        }
        scoreManager().updateHighScore();
        level.showMessage(GameLevel.Message.GAME_OVER);
        THE_GAME_EVENT_MANAGER.publishEvent(this, GameEventType.STOP_ALL_SOUNDS);
    }

    @Override
    public boolean canStartNewGame() {
        return !THE_COIN_MECHANISM.isEmpty();
    }

    @Override
    public boolean continueOnGameOver() {
        return false;
    }

    @Override
    public boolean isOver() {
        return livesProperty().get() == 0;
    }

    @Override
    public long gameOverStateTicks() {
        return 150;
    }

    private LevelData levelData(int levelNumber) {
        return new LevelData(LEVEL_DATA[Math.min(levelNumber - 1, LEVEL_DATA.length - 1)]);
    }

    @Override
    public void buildGameLevel(int levelNumber) {
        WorldMap worldMap = mapSelector.selectWorldMap(levelNumber);

        level = new GameLevel(levelNumber, worldMap);
        level.setCutSceneNumber(cutSceneNumberAfterLevel(levelNumber));
        level.setNumFlashes(levelData(levelNumber).numFlashes());

        /* In Ms. Pac-Man, the level counter stays fixed from level 8 on and bonus symbols are created randomly
         * (also inside a level) whenever a bonus score is reached. At least that's what I was told. */
        levelCounter().setEnabled(levelNumber < 8);

        if (!worldMap.hasProperty(LayerID.TERRAIN, PROPERTY_POS_HOUSE_MIN_TILE)) {
            Logger.warn("No house min tile found in map!");
            worldMap.setProperty(LayerID.TERRAIN, PROPERTY_POS_HOUSE_MIN_TILE, formatTile(Vector2i.of(10, 15)));
        }
        if (!worldMap.hasProperty(LayerID.TERRAIN, PROPERTY_POS_HOUSE_MAX_TILE)) {
            Logger.warn("No house max tile found in map!");
            worldMap.setProperty(LayerID.TERRAIN, PROPERTY_POS_HOUSE_MAX_TILE, formatTile(Vector2i.of(17, 19)));
        }
        Vector2i minTile = worldMap.getTerrainTileProperty(PROPERTY_POS_HOUSE_MIN_TILE, null);
        Vector2i maxTile = worldMap.getTerrainTileProperty(PROPERTY_POS_HOUSE_MAX_TILE, null);
        level.createArcadeHouse(minTile.x(), minTile.y(), maxTile.x(), maxTile.y());

        var pac = new Pac();
        level.setPac(pac);
        pac.setName("Ms. Pac-Man");
        pac.setGameLevel(level);
        pac.reset();
        pac.setAutopilot(autopilot);

        var ghosts = List.of(blinky(), pinky(), inky(), sue());
        level.setGhosts(ghosts.toArray(Ghost[]::new));
        ghosts.forEach(ghost -> {
            ghost.setGameLevel(level);
            ghost.setRevivalPosition(level.ghostPosition(ghost.id()));
            ghost.setHuntingBehaviour(this::ghostHuntingBehaviour);
            ghost.reset();
        });
        ghosts.get(RED_GHOST_ID).setRevivalPosition(level.ghostPosition(PINK_GHOST_ID)); // middle house position

        level.setBonusSymbol(0, computeBonusSymbol(level.number()));
        level.setBonusSymbol(1, computeBonusSymbol(level.number()));
    }

    @Override
    protected void setActorBaseSpeed(int levelNumber) {
        level.pac().setBaseSpeed(1.25f);
        level.ghosts().forEach(ghost -> ghost.setBaseSpeed(1.25f));
    }

    @Override
    public void buildDemoLevel() {
        demoLevelProperty().set(true);
        levelCounter.setEnabled(false);
        buildGameLevel(1);
        assignDemoLevelBehavior(level.pac());
        demoLevelSteering.init();
        level.showMessage(GameLevel.Message.GAME_OVER);
    }

    private int cutSceneNumberAfterLevel(int number) {
        return switch (number) {
            case 2 -> 1;
            case 5 -> 2;
            case 9, 13, 17 -> 3;
            default -> 0;
        };
    }

    @Override
    public void assignDemoLevelBehavior(Pac pac) {
        pac.setAutopilot(demoLevelSteering);
        pac.setUsingAutopilot(true);
        pac.setImmune(false);
    }

    @Override
    public float pacNormalSpeed() {
        if (level == null) {
            return 0;
        }
        byte percentage = levelData(level.number()).pacSpeedPercentage();
        if (percentage == 0) {
            percentage = 100;
        }
        return percentage * 0.01f * level.pac().baseSpeed();
    }

    @Override
    public float pacPowerSpeed() {
        if (level == null) {
            return 0;
        }
        byte percentage = levelData(level.number()).pacSpeedPoweredPercentage();
        if (percentage == 0) {
            percentage = 100;
        }
        return percentage * 0.01f * level.pac().baseSpeed();
    }

    @Override
    public long pacPowerTicks() {
        return level != null ? 60 * levelData(level.number()).pacPowerSeconds() : 0;
    }

    @Override
    public long pacPowerFadingTicks() {
        //TODO find better solution.
        // Ghost flashing animation has frame length 14 so one full flash takes 28 ticks
        return level != null ? level.numFlashes() * 28L : 0;
    }

    @Override
    public long pacDyingTicks() {
        return 240;
    }

    @Override
    public float ghostAttackSpeed(Ghost ghost) {
        if (level.isTunnel(ghost.tile()) && level.number() <= 3) {
            return ghostTunnelSpeed(ghost);
        }
        if (ghost.id() == RED_GHOST_ID && cruiseElroy == 1) {
            return levelData(level.number()).elroy1SpeedPercentage() * 0.01f * ghost.baseSpeed();
        }
        if (ghost.id() == RED_GHOST_ID && cruiseElroy == 2) {
            return levelData(level.number()).elroy2SpeedPercentage() * 0.01f * ghost.baseSpeed();
        }
        return levelData(level.number()).ghostSpeedPercentage() * 0.01f * ghost.baseSpeed();
    }

    @Override
    public float ghostSpeedInsideHouse(Ghost ghost) {
        return 0.5f;
    }

    @Override
    public float ghostSpeedReturningToHouse(Ghost ghost) {
        return 2;
    }

    @Override
    public float ghostFrightenedSpeed(Ghost ghost) {
        return level !=null
            ? levelData(level.number()).ghostSpeedFrightenedPercentage() * 0.01f * ghost.baseSpeed()
            : 0;
    }

    @Override
    public float ghostTunnelSpeed(Ghost ghost) {
        return level !=null
            ? levelData(level.number()).ghostSpeedTunnelPercentage() * 0.01f * ghost.baseSpeed()
            : 0;
    }

    @Override
    public HuntingTimer huntingTimer() {
        return huntingTimer;
    }

    @Override
    public boolean isPacManKillingIgnored() {
        float levelRunningSeconds = (System.currentTimeMillis() - levelStartTime) / 1000f;
        if (isDemoLevel() && levelRunningSeconds < DEMO_LEVEL_MIN_DURATION_SEC) {
            Logger.info("Pac-Man killing ignored, demo level running for {} seconds", levelRunningSeconds);
            return true;
        }
        return false;
    }

    @Override
    protected void onFoodEaten(Vector2i tile, int uneatenFoodCount, boolean energizer) {
        level.pac().setRestingTicks(energizer ? 3 : 1);
        if (uneatenFoodCount == levelData(level.number()).elroy1DotsLeft()) {
            cruiseElroy = 1;
        } else if (uneatenFoodCount == levelData(level.number()).elroy2DotsLeft()) {
            cruiseElroy = 2;
        }
        if (energizer) {
            onEnergizerEaten();
            scoreManager().scorePoints(ENERGIZER_VALUE);
            Logger.info("Scored {} points for eating energizer", ENERGIZER_VALUE);
        } else {
            scoreManager.scorePoints(PELLET_VALUE);
        }
        gateKeeper.registerFoodEaten(level);
        if (isBonusReached()) {
            activateNextBonus();
            eventLog.bonusIndex = level.nextBonusIndex();
        }
    }

    @Override
    public void onPacKilled() {
        huntingTimer.stop();
        Logger.info("Hunting timer stopped");
        level.powerTimer().stop();
        level.powerTimer().reset(0);
        Logger.info("Power timer stopped and set to zero");
        gateKeeper.resetCounterAndSetEnabled(true);
        setCruiseElroyEnabled(false);
        level.pac().die();
    }

    @Override
    public void killGhost(Ghost ghost) {
        eventLog.killedGhosts.add(ghost);
        int killedSoFar = level.victims().size();
        int points = 100 * KILLED_GHOST_VALUE_MULTIPLIER[killedSoFar];
        level.addVictim(ghost);
        ghost.eaten(killedSoFar);
        scoreManager.scorePoints(points);
        Logger.info("Scored {} points for killing {} at tile {}", points, ghost.name(), ghost.tile());
        if (level.victimsCount() == 16) {
            int extraPoints = POINTS_ALL_GHOSTS_EATEN_IN_LEVEL;
            scoreManager.scorePoints(extraPoints);
            Logger.info("Scored {} points for killing all ghosts in level {}", extraPoints, level.number());
        }
    }

    private void setCruiseElroyEnabled(boolean enabled) {
        if (enabled && cruiseElroy < 0 || !enabled && cruiseElroy > 0) {
            cruiseElroy = (byte) -cruiseElroy;
        }
    }

    @Override
    protected void onGhostReleased(Ghost ghost) {
        if (ghost.id() == ORANGE_GHOST_ID && cruiseElroy < 0) {
            Logger.trace("Re-enable cruise elroy mode because {} exits house:", ghost.name());
            setCruiseElroyEnabled(true);
        }
    }

    @Override
    public boolean isBonusReached() {
        return level.eatenFoodCount() == 64 || level.eatenFoodCount() == 176;
    }

    /**
     * <p>Got this information from
     * <a href="https://www.reddit.com/r/Pacman/comments/12q4ny3/is_anyone_able_to_explain_the_ai_behind_the/">Reddit</a>:
     * </p>
     * <p style="font-style:italic">
     * The exact fruit mechanics are as follows: After 64 dots are consumed, the game spawns the first fruit of the level.
     * After 176 dots are consumed, the game attempts to spawn the second fruit of the level. If the first fruit is still
     * present in the level when (or eaten very shortly before) the 176th dot is consumed, the second fruit will not
     * spawn. Dying while a fruit is on screen causes it to immediately disappear and never return.
     * (TODO: what does "never" mean here? For the rest of the game?).
     * The type of fruit is determined by the level count - levels 1-7 will always have two cherries, two strawberries,
     * etc. until two bananas on level 7. On level 8 and beyond, the fruit type is randomly selected using the weights in
     * the following table:
     *
     * <table>
     * <tr align="left">
     *   <th>Cherry</th><th>Strawberry</th><th>Peach</th><th>Pretzel</th><th>Apple</th><th>Pear&nbsp;</th><th>Banana</th>
     * </tr>
     * <tr align="right">
     *     <td>5/32</td><td>5/32</td><td>5/32</td><td>5/32</td><td>4/32</td><td>4/32</td><td>4/32</td>
     * </tr>
     * </table>
     * </p>
     */
    @Override
    public byte computeBonusSymbol(int levelNumber) {
        if (levelNumber <= 7) {
            return (byte) (levelNumber - 1);
        }
        int choice = randomInt(0, 320);
        if (choice <  50) return 0; // 5/32 probability
        if (choice < 100) return 1; // 5/32
        if (choice < 150) return 2; // 5/32
        if (choice < 200) return 3; // 5/32
        if (choice < 240) return 4; // 4/32
        if (choice < 280) return 5; // 4/32
        else              return 6; // 4/32
    }

    /**
     * Bonus symbol enters the world at some tunnel entry, walks to the house entry, takes a tour around the
     * house and finally leaves the world through a tunnel on the opposite side of the world.
     * <p>
     * According to <a href="https://strategywiki.org/wiki/Ms._Pac-Man/Walkthrough">this</a> Wiki,
     * some maps have a fixed entry tile for the bonus.
     * TODO: Not sure if that's correct.
     *
     * <p>
     * Note: This is not the exact behavior from the original Arcade game that uses fruit paths.
     **/
    @Override
    public void activateNextBonus() {
        if (level.bonus().isPresent() && level.bonus().get().state() != Bonus.STATE_INACTIVE) {
            Logger.info("Previous bonus is still active, skip this one");
            return;
        }
        level.advanceNextBonus();

        List<Portal> portals = level.portals().toList();
        if (portals.isEmpty()) {
            return; // should not happen
        }

        Vector2i entryTile, exitTile;
        boolean crossMazeLeftToRight;

        WorldMap worldMap = level.worldMap();
        if (worldMap.hasProperty(LayerID.TERRAIN, "pos_bonus")) {
            // use entry tile stored in terrain map
            entryTile = level.worldMap().getTerrainTileProperty("pos_bonus", null);
            if (entryTile.x() == 0) {
                // start tile is at left maze border
                exitTile = portals.get(THE_RNG.nextInt(portals.size())).rightTunnelEnd().plus(1, 0);
                crossMazeLeftToRight = true;
            } else {
                // start tile is at right maze border
                exitTile = portals.get(THE_RNG.nextInt(portals.size())).leftTunnelEnd().minus(1, 0);
                crossMazeLeftToRight = false;
            }
        }
        else { // choose random portals for entry and exit
            crossMazeLeftToRight = THE_RNG.nextBoolean();
            if (crossMazeLeftToRight) {
                entryTile = portals.get(THE_RNG.nextInt(portals.size())).leftTunnelEnd();
                exitTile  = portals.get(THE_RNG.nextInt(portals.size())).rightTunnelEnd().plus(1, 0);
            } else {
                entryTile = portals.get(THE_RNG.nextInt(portals.size())).rightTunnelEnd();
                exitTile = portals.get(THE_RNG.nextInt(portals.size())).leftTunnelEnd().minus(1, 0);
            }
        }

        Vector2i houseEntry = tileAt(level.houseEntryPosition());
        Vector2i behindHouse = houseEntry.plus(0, level.houseSizeInTiles().y() + 1);
        List<Waypoint> route = Stream.of(entryTile, houseEntry, behindHouse, houseEntry, exitTile).map(Waypoint::new).toList();

        byte symbol = level.bonusSymbol(level.nextBonusIndex());
        var movingBonus = new MovingBonus(level, symbol, BONUS_VALUE_FACTORS[symbol] * 100);
        movingBonus.setEdible(TickTimer.INDEFINITE);
        movingBonus.setRoute(route, crossMazeLeftToRight);
        movingBonus.setBaseSpeed(1.25f);
        Logger.info("Moving bonus created, route: {} ({})", route, crossMazeLeftToRight ? "left to right" : "right to left");

        level.setBonus(movingBonus);
        THE_GAME_EVENT_MANAGER.publishEvent(this, GameEventType.BONUS_ACTIVATED, movingBonus.actor().tile());
    }

    /**
     * In Ms. Pac-Man, Blinky and Pinky move randomly during the *first* scatter phase. Some say,
     * the original intention had been to randomize the scatter target of *all* ghosts but because of a bug,
     * only the scatter target of Blinky and Pinky would have been affected. Who knows?
     */
    private void ghostHuntingBehaviour(Ghost ghost) {
        if (huntingTimer.phaseIndex() == 0 && (ghost.id() == RED_GHOST_ID || ghost.id() == PINK_GHOST_ID)) {
            ghost.roam(ghostAttackSpeed(ghost));
        } else {
            boolean chasing = huntingTimer.huntingPhase() == HuntingTimer.HuntingPhase.CHASING
                || ghost.id() == RED_GHOST_ID && cruiseElroy > 0;
            Vector2i targetTile = chasing
                    ? chasingTargetTile(ghost.id(), level, simulateOverflowBugProperty().get())
                    : level.ghostScatterTile(ghost.id());
            ghost.followTarget(targetTile, ghostAttackSpeed(ghost));
        }
    }
}