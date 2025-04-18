/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.ui._2d;

import de.amr.games.pacman.lib.RectArea;
import de.amr.games.pacman.lib.Vector2f;
import de.amr.games.pacman.lib.Vector2i;
import de.amr.games.pacman.lib.tilemap.WorldMap;
import de.amr.games.pacman.model.GameLevel;
import de.amr.games.pacman.model.LevelCounter;
import de.amr.games.pacman.model.Score;
import de.amr.games.pacman.model.ScoreManager;
import de.amr.games.pacman.model.actors.Actor2D;
import de.amr.games.pacman.model.actors.AnimatedActor2D;
import de.amr.games.pacman.model.actors.Bonus;
import de.amr.games.pacman.model.actors.Creature;
import de.amr.games.pacman.uilib.animation.SpriteAnimation;
import javafx.beans.property.FloatProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.tinylog.Logger;

import java.util.function.Predicate;

import static de.amr.games.pacman.Globals.*;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

/**
 * @author Armin Reichert
 */
public interface GameRenderer {

    Canvas canvas();

    default GraphicsContext ctx() { return canvas().getGraphicsContext2D(); }

    default void fillCanvas(Paint paint) {
        ctx().setFill(paint);
        ctx().fillRect(0, 0, canvas().getWidth(), canvas().getHeight());
    }

    void applyMapSettings(WorldMap worldMap);

    GameSpriteSheet spriteSheet();

    FloatProperty scalingProperty();

    default void setScaling(float value) { scalingProperty().set(value); }

    default float scaling() { return scalingProperty().get(); }

    default float scaled(double value) { return scaling() * (float) value; }

    /**
     * Draws a sprite (section of the sprite sheet source) at the given (scaled) position.
     *
     * @param region        the sprite sheet region to draw
     * @param x             scaled x-coordinate
     * @param y             scaled y-coordinate
     */
    default void drawSpriteSheetRegion(RectArea region, double x, double y) {
        if (region != null) {
            ctx().drawImage(spriteSheet().sourceImage(),
                region.x(), region.y(), region.width(), region.height(),
                x, y, region.width(), region.height());
        }
    }

    default void drawSpriteScaled(RectArea sprite, double x, double y) {
        drawImageRegionScaled(spriteSheet().sourceImage(), sprite, x, y);
    }

    default void drawImageRegionScaled(Image image, RectArea region, double x, double y) {
        if (region != null) {
            ctx().drawImage(image,
                region.x(), region.y(), region.width(), region.height(),
                scaled(x), scaled(y), scaled(region.width()), scaled(region.height()));
        }
    }

    /**
     * Draws a sprite centered over a tile (e.g. the collision box of creature).
     * <p>
     * The position specifies the left-upper corner of the tile. Note that the sprites for
     * Pac-Man and the ghosts are larger than one tile but their collision box is exactly one tile large.
     * </p>
     *
     * @param sprite sprite (region in sprite sheet) (may be null)
     * @param tileX  x-coordinate of left-upper corner of bounding box
     * @param tileY  y-coordinate of left-upper corner of bounding box
     */
    default void drawSpriteScaledCenteredOverTile(RectArea sprite, double tileX, double tileY) {
        drawSpriteScaledCenteredAt(sprite, tileX + HTS, tileY + HTS);
    }

    /**
     * Draws a sprite centered over a position.
     *
     * @param sprite sprite (region in sprite sheet, can be null)
     * @param centerX  x-coordinate of the center position
     * @param centerY  y-coordinate of the center position
     */
    default void drawSpriteScaledCenteredAt(RectArea sprite, double centerX, double centerY) {
        drawSpriteScaled(sprite, centerX - 0.5 * sprite.width(), centerY - 0.5 * sprite.height());
    }

    /**
     * Draws the sprite over the collision box (one tile large) of the given entity (if visible).
     *
     * @param actor an entity e.g. Pac-Man or a ghost
     * @param sprite sprite sheet region (can be null)
     */
    default void drawActorSprite(Actor2D actor, RectArea sprite) {
        requireNonNull(actor);
        if (actor.isVisible() && sprite != null) {
            drawSpriteScaledCenteredOverTile(sprite, actor.posX(), actor.posY());
        }
    }

    /**
     * Draws animated entity (Pac-Man, ghost, moving bonus) if entity is visible.
     *
     * @param animatedActor the animated entity
     */
    default void drawAnimatedActor(AnimatedActor2D animatedActor) {
        if (animatedActor == null || !animatedActor.actor().isVisible()) {
            return;
        }
        animatedActor.animations().ifPresent(animations -> {
            if (animations instanceof SpriteAnimationSet spriteAnimations) {
                SpriteAnimation currentAnimation = spriteAnimations.currentAnimation();
                if (currentAnimation != null) {
                    drawActorSprite(animatedActor.actor(), spriteAnimations.currentSprite(animatedActor));
                } else {
                    Logger.error("No current animation for actor {}", animatedActor);
                }
            }
        });
    }

    default void drawBonus(Bonus bonus) {
        if (bonus.state() == Bonus.STATE_EDIBLE) {
            drawActorSprite(bonus.actor(), spriteSheet().bonusSymbolSprite(bonus.symbol()));
        } else if (bonus.state() == Bonus.STATE_EATEN) {
            drawActorSprite(bonus.actor(), spriteSheet().bonusValueSprite(bonus.symbol()));
        }
    }

    void drawLevelCounter(LevelCounter levelCounter, Vector2f sceneSizeInPx);

    void drawMaze(GameLevel level, double x, double y, Paint backgroundColor, boolean highlighted, boolean blinking);

    /**
     * Over-paints all eaten pellet tiles.
     * Assumes to be called in scaled graphics context!
     */
    default void overPaintEatenPelletTiles(GameLevel level, Paint paint) {
        level.worldMap().tiles()
            .filter(not(level::isEnergizerPosition))
            .filter(level::hasEatenFoodAt).forEach(tile -> paintSquareInsideTile(tile, 4, paint));
    }

    /**
     * Over-paints all eaten energizer tiles.
     * Assumes to be called in scaled graphics context!
     */
    default void overPaintEnergizerTiles(GameLevel level, Predicate<Vector2i> condition, Paint paint) {
        level.energizerTiles().filter(condition).forEach(tile -> paintSquareInsideTile(tile, 10, paint));
    }

    /**
     * Draws a square of the given size in background color over the tile. Used to hide eaten food and energizers.
     * Assumes to be called in scaled graphics context!
     */
    private void paintSquareInsideTile(Vector2i tile, double squareSize, Paint paint) {
        double centerX = tile.x() * TS + HTS, centerY = tile.y() * TS + HTS;
        ctx().setFill(paint);
        ctx().fillRect(centerX - 0.5 * squareSize, centerY - 0.5 * squareSize, squareSize, squareSize);
    }

    /**
     * @param animatedCreature animated entity (must contain an entity of type {@link Creature}
     */
    default void drawAnimatedCreatureInfo(AnimatedActor2D animatedCreature) {
        animatedCreature.animations().map(SpriteAnimationSet.class::cast).ifPresent(animations -> {
            var guy = (Creature) animatedCreature.actor();
            String animID = animations.currentID();
            if (animID != null) {
                String text = animID + " " + animations.currentAnimation().frameIndex();
                ctx().setFill(Color.WHITE);
                ctx().setFont(Font.font("Monospaced", scaled(6)));
                ctx().fillText(text, scaled(guy.posX() - 4), scaled(guy.posY() - 4));
            }
            if (guy.wishDir() != null) {
                float scaling = scaling();
                Vector2f center = guy.position().plus(HTS, HTS);
                Vector2f arrowHead = center.plus(guy.wishDir().vector().scaled(12f)).scaled(scaling);
                Vector2f guyCenter = center.scaled(scaling);
                float radius = scaling * 2, diameter = 2 * radius;
                ctx().setStroke(Color.WHITE);
                ctx().setLineWidth(0.5);
                ctx().strokeLine(guyCenter.x(), guyCenter.y(), arrowHead.x(), arrowHead.y());
                ctx().setFill(guy.isNewTileEntered() ? Color.YELLOW : Color.GREEN);
                ctx().fillOval(arrowHead.x() - radius, arrowHead.y() - radius, diameter, diameter);
            }
        });
    }

    /**
     * Draws text at the given position (scaled by the current scaling value).
     *
     * @param text  text
     * @param color text color
     * @param font  text font
     * @param x     unscaled x-position
     * @param y     unscaled y-position (baseline)
     */
    default void fillTextAtScaledPosition(String text, Color color, Font font, double x, double y) {
        ctx().setFont(font);
        ctx().setFill(color);
        ctx().fillText(text, scaled(x), scaled(y));
    }

    default void drawLivesCounter(int numLives, int maxLives, double x, double y) {
        if (numLives == 0) {
            return;
        }
        for (int i = 0; i < Math.min(numLives, maxLives); ++i) {
            drawSpriteScaled(spriteSheet().livesCounterSprite(), x + TS * (2 * i), y);
        }
        // show text indicating that more lives are available than symbols displayed (can happen when lives are added via cheat)
        int moreLivesThanSymbols = numLives - maxLives;
        if (moreLivesThanSymbols > 0) {
            Font font = Font.font("Serif", FontWeight.BOLD, scaled(8));
            fillTextAtScaledPosition("+" + moreLivesThanSymbols, Color.YELLOW, font, x + TS * 10, y + TS);
        }
    }

    default void drawScores(ScoreManager scoreManager, Color color, Font font) {
        drawScore(scoreManager.score(),     "SCORE",      tiles_to_px(1),  tiles_to_px(1), font, color);
        drawScore(scoreManager.highScore(), "HIGH SCORE", tiles_to_px(14), tiles_to_px(1), font, color);
    }

    default void drawScore(Score score, String title, double x, double y, Font font, Color color) {
        var pointsText = String.format("%02d", score.points());
        fillTextAtScaledPosition(title, color, font, x, y);
        fillTextAtScaledPosition(String.format("%7s", pointsText), color, font, x, y + TS + 1);
        if (score.points() != 0) {
            fillTextAtScaledPosition("L" + score.levelNumber(), color, font, x + tiles_to_px(8), y + TS + 1);
        }
    }

    default void drawTileGrid(double sizeX, double sizeY, Color strokeColor) {
        double thin = 0.2, medium = 0.4, thick = 0.8;
        int numCols = (int) (sizeX / TS), numRows = (int) (sizeY / TS);
        double width = scaled(numCols * TS), height = scaled(numRows * TS);
        ctx().save();
        ctx().setStroke(strokeColor);
        for (int row = 0; row <= numRows; ++row) {
            double y = scaled(row * TS);
            ctx().setLineWidth(row % 10 == 0 ? thick : row % 5 == 0 ? medium : thin);
            ctx().strokeLine(0, y, width, y);
        }
        for (int col = 0; col <= numCols; ++col) {
            double x = scaled(col * TS);
            ctx().setLineWidth(col % 10 == 0 ? thick : col % 5 == 0? medium : thin);
            ctx().strokeLine(x, 0, x, height);
        }
        ctx().restore();
    }
}