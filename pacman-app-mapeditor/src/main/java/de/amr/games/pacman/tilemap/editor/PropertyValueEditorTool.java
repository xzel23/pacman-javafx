/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.tilemap.editor;

import de.amr.games.pacman.lib.RectArea;
import de.amr.games.pacman.lib.Vector2i;
import de.amr.games.pacman.lib.tilemap.LayerID;
import de.amr.games.pacman.lib.tilemap.WorldMap;
import de.amr.games.pacman.uilib.tilemap.TileMapRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import static de.amr.games.pacman.Globals.TS;
import static de.amr.games.pacman.lib.tilemap.WorldMap.*;
import static de.amr.games.pacman.tilemap.editor.ArcadeMap.PAC_SPRITE;
import static de.amr.games.pacman.tilemap.editor.ArcadeMap.SPRITE_SHEET;

/**
 * @author Armin Reichert
 */
public class PropertyValueEditorTool implements Tool {
    private final TileMapRenderer renderer;
    private final double size;
    private final String propertyName;
    private final String description;

    public PropertyValueEditorTool(TileMapRenderer renderer, double size, String propertyName, String description) {
        this.renderer = renderer;
        this.size = size;
        this.propertyName = propertyName;
        this.description = description;
    }

    @Override
    public TileMapRenderer renderer() {
        return renderer;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public void apply(WorldMap worldMap, LayerID layerID, Vector2i tile) {
        worldMap.setProperty(layerID, propertyName, WorldMap.formatTile(tile));
    }

    @Override
    public void draw(GraphicsContext g, int row, int col) {
        g.setFill(Color.BLACK);
        g.fillRect(col * size, row * size, size, size);
        if (renderer instanceof TerrainTileMapRenderer tr) {
            g.save();
            g.setImageSmoothing(true);
            g.scale(size / (double) TS, size / (double) TS);
            Vector2i tile = new Vector2i(col, row);
            double x = col * TS, y = row * TS;
            switch (propertyName) {
                case PROPERTY_POS_PAC -> drawSprite(g, x, y, PAC_SPRITE);
                case PROPERTY_POS_RED_GHOST -> drawSprite(g, x, y, ArcadeMap.RED_GHOST_SPRITE);
                case PROPERTY_POS_PINK_GHOST -> drawSprite(g, x, y, ArcadeMap.PINK_GHOST_SPRITE);
                case PROPERTY_POS_CYAN_GHOST -> drawSprite(g, x, y, ArcadeMap.CYAN_GHOST_SPRITE);
                case PROPERTY_POS_ORANGE_GHOST -> drawSprite(g, x, y, ArcadeMap.ORANGE_GHOST_SPRITE);
                case PROPERTY_POS_BONUS -> drawSprite(g, x, y, ArcadeMap. BONUS_SPRITE);
                case PROPERTY_POS_SCATTER_RED_GHOST -> tr.drawScatterTarget(g, tile, Color.RED);
                case PROPERTY_POS_SCATTER_PINK_GHOST -> tr.drawScatterTarget(g, tile, Color.PINK);
                case PROPERTY_POS_SCATTER_CYAN_GHOST -> tr.drawScatterTarget(g, tile, Color.CYAN);
                case PROPERTY_POS_SCATTER_ORANGE_GHOST -> tr.drawScatterTarget(g, tile, Color.ORANGE);
                default -> {}
            }
            g.restore();
        }
    }

    private void drawSprite(GraphicsContext g, double x, double y, RectArea sprite) {
        g.drawImage(SPRITE_SHEET,
            sprite.x(), sprite.y(), sprite.width(), sprite.height(),
            x + 1, y + 1, TS - 2, TS - 2);
    }
}
