/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.model.actors;

import de.amr.games.pacman.event.GameEventType;
import de.amr.games.pacman.lib.Direction;
import de.amr.games.pacman.lib.Vector2i;
import de.amr.games.pacman.lib.Waypoint;
import de.amr.games.pacman.lib.timer.Pulse;
import de.amr.games.pacman.lib.timer.TickTimer;
import de.amr.games.pacman.model.GameLevel;
import de.amr.games.pacman.model.GameModel;
import de.amr.games.pacman.steering.RouteBasedSteering;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

import static de.amr.games.pacman.Globals.THE_GAME_EVENT_MANAGER;
import static java.util.Objects.requireNonNull;

/**
 * A bonus that tumbles through the world, starting at some portal, making one round around the ghost house and leaving
 * the maze at some portal at the other border.
 *
 * <p>
 * That's however not exactly the original Ms. Pac-Man behaviour with predefined "fruit paths".
 *
 * @author Armin Reichert
 */
public class MovingBonus extends Creature implements Bonus {

    private final Pulse animation = new Pulse(10, false);
    private final byte symbol;
    private final int points;
    private byte state;
    private long countdown;
    private RouteBasedSteering steering;

    public MovingBonus(GameLevel level, byte symbol, int points) {
        super.level = requireNonNull(level);
        this.symbol = symbol;
        this.points = points;
        reset();
        canTeleport = false; // override default from Creature
        countdown = 0;
        state = STATE_INACTIVE;
    }

    @Override
    public String name() {
        return "MovingBonus-symbol-" + symbol + "-points-" + points;
    }

    @Override
    public boolean canReverse() {
        return false;
    }

    @Override
    public boolean canAccessTile(Vector2i tile) {
        if (level.isPartOfHouse(tile)) {
            return false;
        }
        if (level.isInsideWorld(tile)) {
            return !level.isBlockedTile(tile);
        }
        return level.isPortalAt(tile);
    }

    @Override
    public MovingBonus actor() {
        return this;
    }

    @Override
    public String toString() {
        return "MovingBonus{" +
            "symbol=" + symbol +
            ", points=" + points +
            ", countdown=" + countdown +
            ", state=" + stateName() +
            '}';
    }

    @Override
    public byte state() {
        return state;
    }

    @Override
    public byte symbol() {
        return symbol;
    }

    @Override
    public int points() {
        return points;
    }

    @Override
    public void setInactive() {
        animation.stop();
        setSpeed(0);
        hide();
        state = STATE_INACTIVE;
        Logger.trace("Bonus inactive: {}", this);
    }

    @Override
    public void setEdible(long ticks) {
        animation.restart();
        setSpeed(0.5f); // how fast in the original game?
        setTargetTile(null);
        show();
        countdown = ticks;
        state = STATE_EDIBLE;
        Logger.trace("Bonus edible: {}", this);
    }

    private void updateStateEdible(GameModel game) {
        GameLevel level = game.level().orElseThrow();
        steering.steer(this, level);
        if (steering.isComplete()) {
            Logger.trace("Moving bonus reached target: {}", this);
            setInactive();
            THE_GAME_EVENT_MANAGER.publishEvent(game, GameEventType.BONUS_EXPIRED, tile());
        } else {
            navigateTowardsTarget();
            tryMoving();
            animation.tick();
        }
    }

    @Override
    public void setEaten(long ticks) {
        animation.stop();
        countdown = ticks;
        state = STATE_EATEN;
        Logger.trace("Bonus eaten: {}", this);
    }

    private void updateStateEaten(GameModel game) {
        if (countdown == 0) {
            Logger.trace("Bonus expired: {}", this);
            setInactive();
            THE_GAME_EVENT_MANAGER.publishEvent(game, GameEventType.BONUS_EXPIRED, tile());
        } else if (countdown != TickTimer.INDEFINITE) {
            --countdown;
        }
    }

    @Override
    public void update(GameModel game) {
        switch (state) {
            case STATE_INACTIVE -> {}
            case STATE_EDIBLE   -> updateStateEdible(game);
            case STATE_EATEN    -> updateStateEaten(game);
            default             -> throw new IllegalStateException("Unknown bonus state: " + state);
        }
    }

    public void setRoute(List<Waypoint> route, boolean leftToRight) {
        requireNonNull(route);
        var routeCopy = new ArrayList<>(route);
        centerOverTile(routeCopy.getFirst().tile());
        setMoveAndWishDir(leftToRight ? Direction.RIGHT : Direction.LEFT);
        routeCopy.removeFirst();
        steering = new RouteBasedSteering(routeCopy);
    }

    public float elongationY() {
        if (!animation.isRunning()) {
            return 0;
        }
        return animation.isOn() ? -3f : 3f;
    }
}