/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.ui._2d;

import de.amr.games.pacman.lib.Vector2f;
import de.amr.games.pacman.lib.tilemap.WorldMap;
import de.amr.games.pacman.model.GameLevel;
import de.amr.games.pacman.model.LevelCounter;
import de.amr.games.pacman.uilib.tilemap.FoodMapRenderer;
import de.amr.games.pacman.uilib.tilemap.TerrainMapColorScheme;
import de.amr.games.pacman.uilib.tilemap.TerrainMapRenderer;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.Map;

import static de.amr.games.pacman.Globals.TS;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

/**
 * As this game variant allows playing arbitrary custom maps, we use a
 * vector graphics rendering that draws wall and obstacle paths generated from
 * the map data.
 *
 * @author Armin Reichert
 */
public class VectorGraphicsGameRenderer implements GameRenderer {

    private final GameSpriteSheet spriteSheet;
    private final GraphicsContext ctx;
    private final FloatProperty scalingPy = new SimpleFloatProperty(1.0f);
    private final TerrainMapRenderer terrainRenderer = new TerrainMapRenderer();
    private final FoodMapRenderer foodRenderer = new FoodMapRenderer();
    private Color bgColor;
    private TerrainMapColorScheme blinkingOnColors;
    private TerrainMapColorScheme blinkingOffColors;

    public VectorGraphicsGameRenderer(GameSpriteSheet spriteSheet, Canvas canvas) {
        this.spriteSheet = requireNonNull(spriteSheet);
        ctx = requireNonNull(canvas).getGraphicsContext2D();
        terrainRenderer.scalingProperty().bind(scalingPy);
        foodRenderer.scalingProperty().bind(scalingPy);
        setBackgroundColor(Color.BLACK);
    }

    @Override
    public void applyMapSettings(WorldMap worldMap) {}

    @Override
    public GameSpriteSheet spriteSheet() {
        return spriteSheet;
    }

    @Override
    public Canvas canvas() {
        return ctx.getCanvas();
    }

    @Override
    public FloatProperty scalingProperty() {
        return scalingPy;
    }

    public void setBackgroundColor(Color color) {
        bgColor = requireNonNull(color);
        blinkingOnColors = new TerrainMapColorScheme(bgColor, Color.BLACK, Color.WHITE, Color.BLACK);
        blinkingOffColors = new TerrainMapColorScheme(bgColor, Color.WHITE, Color.BLACK, Color.BLACK);
    }

    @Override
    public void drawMaze(GameLevel level, double x, double y, Paint backgroundColor, boolean mazeHighlighted, boolean blinking) {
        WorldMap worldMap = level.worldMap();
        if (mazeHighlighted) {
            terrainRenderer.setColorScheme(blinking ? blinkingOnColors : blinkingOffColors);
            terrainRenderer.drawTerrain(ctx, worldMap, worldMap.obstacles());
        }
        else {
            Map<String, String> colorMap = worldMap.getConfigValue("colorMap");
            TerrainMapColorScheme colors = new TerrainMapColorScheme(
                bgColor,
                Color.web(colorMap.get("fill")),
                Color.web(colorMap.get("stroke")),
                Color.web(colorMap.get("door"))
            );
            terrainRenderer.setColorScheme(colors);
            terrainRenderer.drawTerrain(ctx, worldMap, worldMap.obstacles());
            terrainRenderer.drawHouse(ctx, level.houseMinTile(), level.houseSizeInTiles());
            foodRenderer.setPelletColor(Color.web(colorMap.get("pellet")));
            foodRenderer.setEnergizerColor(Color.web(colorMap.get("pellet")));
            worldMap.tiles().filter(level::hasFoodAt).filter(not(level::isEnergizerPosition))
                .forEach(tile -> foodRenderer.drawPellet(ctx, tile));
            if (blinking) {
                level.energizerTiles().filter(level::hasFoodAt).forEach(tile -> foodRenderer.drawEnergizer(ctx, tile));
            }
        }
    }

    @Override
    public void drawLevelCounter(LevelCounter levelCounter, Vector2f sceneSizeInPx) {
        float x = sceneSizeInPx.x() - 4 * TS, y = sceneSizeInPx.y() - 2 * TS;
        for (byte symbol : levelCounter.symbols().toList()) {
            drawSpriteScaled(spriteSheet().bonusSymbolSprite(symbol), x, y);
            x -= TS * 2;
        }
    }
}