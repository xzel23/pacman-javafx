/*
 * Copyright (c) 2021-2025 Armin Reichert (MIT License) See file LICENSE in repository root directory for details.
 */
package de.amr.games.pacman.tengen.ms_pacman;

import de.amr.games.pacman.lib.RectArea;
import de.amr.games.pacman.lib.nes.NES_ColorScheme;
import de.amr.games.pacman.lib.tilemap.WorldMap;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.tinylog.Logger;

import java.util.*;

import static de.amr.games.pacman.Globals.TS;
import static de.amr.games.pacman.lib.RectArea.rect;
import static de.amr.games.pacman.lib.nes.NES_ColorScheme.*;
import static de.amr.games.pacman.uilib.Ufx.*;
import static java.util.Objects.requireNonNull;

/**
 * This class provides an API to access the maze images in files {@code non_arcade_mazes.png} and {@code arcade_mazes.png}.
 * <p>
 * These PNG files contain images for all mazes of the different map categories.
 * The color schemes correspond to the ones used in STRANGE mode running through all levels (1-32).
 * Because levels 28-31 use random color schemes, and the MINI and BIG categories use their
 * map in varying color schemes, the images in the file do not cover all required map/color scheme
 * combinations.
 * </p>
 * <p>
*  For this reason, a cache is provided where each maze image contained in the file can be
 * stored after having been recolored to the color scheme required in the game level.
 * </p>
 */
public class MapRepository {

    static final int ARCADE_MAZE_WIDTH = 28*8, ARCADE_MAZE_HEIGHT = 31*8;

    // Map row counts as they appear in the sprite sheet (row by row)
    static final byte[] NON_ARCADE_MAP_ROW_COUNTS = {
        31, 31, 31, 31, 31, 31, 30, 31,
        31, 37, 31, 31, 31, 37, 31, 25,
        37, 31, 37, 37, 37, 37, 37, 31,
        37, 37, 31, 25, 31, 25, 31, 31, 37,
        25, 25, 25, 25,
    };

    // Strange map #15 (level 32) has 3 different images to create an animation effect
    static final RectArea[] STRANGE_MAP_15_SPRITES = {
        rect(1568,  840, 224, 248),
        rect(1568, 1088, 224, 248),
        rect(1568, 1336, 224, 248),
    };

    // Pattern (0^8 1^8 2^8 1^8)+
    public static RectArea strangeMap15Sprite(long tick) {
        // numFrames = 4, frameDuration = 8
        int index = (int) ((tick % 32) / 8);
        // (0, 1, 2, 3) -> (0, 1, 2, 1)
        if (index == 3) index = 1;
        return STRANGE_MAP_15_SPRITES[index];
    }

    private static List<NES_ColorScheme> randomColorSchemes(int count, NES_ColorScheme usedScheme) {
        Set<NES_ColorScheme> colorSchemes = new HashSet<>();
        while (colorSchemes.size() < count) {
            NES_ColorScheme colorScheme = NES_ColorScheme.random();
            if (!colorScheme.equals(usedScheme)) {
                colorSchemes.add(colorScheme);
            }
        }
        return colorSchemes.stream().toList();
    }

    // Maze areas where ghosts are shown in maze images, must be masked at runtime
    static final RectArea GHOST_OUTSIDE_HOUSE_AREA = new RectArea(105, 85, 14, 13);
    static final RectArea GHOSTS_INSIDE_HOUSE_AREA = new RectArea(89, 113, 46, 13);

    private final Map<ColoredMapDefinition, ColoredMapImage> mazeCache = new HashMap<>();
    private final Image arcadeMazesImage;
    private final Image nonArcadeMazesImage;

    public MapRepository(Image arcadeMazesImage, Image nonArcadeMazesImage) {
        this.arcadeMazesImage = requireNonNull(arcadeMazesImage);
        this.nonArcadeMazesImage = requireNonNull(nonArcadeMazesImage);
    }

    public ColoredMapSet createMazeSet(WorldMap worldMap, int flashCount) {
        MapCategory mapCategory = worldMap.getConfigValue("mapCategory");
        int mapNumber = worldMap.getConfigValue("mapNumber");
        NES_ColorScheme nesColorScheme = worldMap.getConfigValue("nesColorScheme");
        // if color scheme has been randomly selected (levels 28-31, except ARCADE mazes), use multiple flash colors
        boolean randomColorScheme = worldMap.getConfigValue("randomColorScheme");
        return switch (mapCategory) {
            case ARCADE  -> arcadeMazeSet(mapNumber, nesColorScheme, flashCount);
            case MINI    -> miniMazeSet(mapNumber, nesColorScheme, flashCount, randomColorScheme);
            case BIG     -> bigMazeSet(mapNumber, nesColorScheme, flashCount, randomColorScheme);
            case STRANGE -> { // TODO HACK!
                int spriteNumber = worldMap.getConfigValue("levelNumber");
                NES_ColorScheme colorScheme = worldMap.getConfigValue("nesColorScheme");
                yield strangeMazeSet(spriteNumber, randomColorScheme ? colorScheme : null, flashCount, randomColorScheme);
            }
        };
    }

    private ColoredMapSet arcadeMazeSet(int mapNumber, NES_ColorScheme colorScheme, int flashCount) {
        int spriteIndex = switch (mapNumber) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> switch (colorScheme) {
                case _16_20_15_ORANGE_WHITE_RED   -> 2;
                case _35_28_20_PINK_YELLOW_WHITE  -> 4;
                case _17_20_20_BROWN_WHITE_WHITE  -> 6;
                case _0F_20_28_BLACK_WHITE_YELLOW -> 8;
                default -> throw new IllegalArgumentException("No maze image found for map #3 and color scheme: " + colorScheme);
            };
            case 4 -> switch (colorScheme) {
                case _01_38_20_BLUE_YELLOW_WHITE   -> 3;
                case _36_15_20_PINK_RED_WHITE      -> 5;
                case _13_20_28_VIOLET_WHITE_YELLOW -> 7;
                default -> throw new IllegalArgumentException("No maze image found for map #4 and color scheme: " + colorScheme);
            };
            default -> throw new IllegalArgumentException("Illegal Arcade map number: " + mapNumber);
        };
        int col = spriteIndex % 3, row = spriteIndex / 3;
        RectArea mazeSprite = new RectArea(col * ARCADE_MAZE_WIDTH, row * ARCADE_MAZE_HEIGHT, ARCADE_MAZE_WIDTH, ARCADE_MAZE_HEIGHT);
        ColoredMapImage normalMaze = new ColoredMapImage(arcadeMazesImage, mazeSprite, colorScheme);
        List<ColoredMapImage> flashingMazes = new ArrayList<>();
        ColoredMapImage blackWhiteMaze = getOrCreateMaze(MapCategory.ARCADE, mapNumber, mazeSprite, FLASHING_BLACK_WHITE, colorScheme, 13, 23);
        for (int i = 0; i < flashCount; ++i) {
            flashingMazes.add(blackWhiteMaze);
        }
        return new ColoredMapSet(normalMaze, flashingMazes);
    }

    private ColoredMapSet miniMazeSet(int mapNumber, NES_ColorScheme colorScheme, int flashCount, boolean multipleFlashColors) {
        int spriteNumber = switch (mapNumber) {
            case 1 -> 34;
            case 2 -> 35;
            case 3 -> 36;
            case 4 -> 30;
            case 5 -> 28;
            case 6 -> 37;
            default -> throw new IllegalArgumentException("Illegal MINI map number: " + mapNumber);
        };
        NES_ColorScheme availableColorScheme = switch (mapNumber) {
            case 1 -> _36_15_20_PINK_RED_WHITE;
            case 2 -> _21_20_28_BLUE_WHITE_YELLOW;
            case 3 -> _35_28_20_PINK_YELLOW_WHITE;
            case 4 -> _28_16_20_YELLOW_RED_WHITE;
            case 5 -> _00_2A_24_GRAY_GREEN_PINK;
            case 6 -> _23_20_2B_VIOLET_WHITE_GREEN;
            default -> null;
        };
        int pacTileX = 13, pacTileY = 23;
        RectArea mazeSprite = nonArcadeMazeSprite(spriteNumber);
        ColoredMapImage normalMaze = colorScheme.equals(availableColorScheme)
            ? new ColoredMapImage(nonArcadeMazesImage, nonArcadeMazeSprite(spriteNumber), colorScheme)
            : getOrCreateMaze(MapCategory.MINI, spriteNumber, mazeSprite, colorScheme, availableColorScheme, pacTileX, pacTileY);

        List<ColoredMapImage> flashingMazes = new ArrayList<>();
        if (multipleFlashColors) {
            for (var randomScheme : randomColorSchemes(flashCount, colorScheme)) {
                ColoredMapImage randomMaze = getOrCreateMaze(MapCategory.MINI, spriteNumber, mazeSprite, randomScheme, availableColorScheme, pacTileX, pacTileY);
                flashingMazes.add(randomMaze);
            }
        } else {
            ColoredMapImage blackWhiteMaze = getOrCreateMaze(MapCategory.MINI, spriteNumber, mazeSprite, FLASHING_BLACK_WHITE, availableColorScheme, pacTileX, pacTileY);
            for (int i = 0; i < flashCount; ++i) {
                flashingMazes.add(blackWhiteMaze);
            }
        }
        return new ColoredMapSet(normalMaze, flashingMazes);
    }

    private ColoredMapSet bigMazeSet(int mapNumber, NES_ColorScheme colorScheme, int flashCount, boolean multipleFlashColors) {
        int spriteNumber = switch (mapNumber) {
            case  1 -> 19;
            case  2 -> 20;
            case  3 -> 21;
            case  4 -> 22;
            case  5 -> 23;
            case  6 -> 17;
            case  7 -> 10;
            case  8 -> 14;
            case  9 -> 26;
            case 10 -> 25;
            case 11 -> 33;
            default -> throw new IllegalArgumentException("Illegal BIG map number: " + mapNumber);
        };
        NES_ColorScheme availableColorScheme = switch (mapNumber) {
            case  1 -> _07_20_20_BROWN_WHITE_WHITE;
            case  2 -> _15_25_20_RED_ROSE_WHITE;
            case  3 -> _0F_20_1C_BLACK_WHITE_GREEN;
            case  4 -> _19_20_20_GREEN_WHITE_WHITE;
            case  5 -> _0C_20_14_GREEN_WHITE_VIOLET;
            case  6 -> _25_20_20_ROSE_WHITE_WHITE;
            case  7 -> _0F_01_20_BLACK_BLUE_WHITE;
            case  8 -> _28_20_2A_YELLOW_WHITE_GREEN;
            case  9 -> _03_20_20_BLUE_WHITE_WHITE;
            case 10 -> _10_20_28_GRAY_WHITE_YELLOW;
            case 11 -> _15_25_20_RED_ROSE_WHITE;
            default -> null;
        };
        int pacTileX = 13, pacTileY = 32;
        RectArea mazeSprite = nonArcadeMazeSprite(spriteNumber);
        ColoredMapImage normalMaze = colorScheme.equals(availableColorScheme)
            ? new ColoredMapImage(nonArcadeMazesImage, nonArcadeMazeSprite(spriteNumber), colorScheme)
            : getOrCreateMaze(MapCategory.BIG, spriteNumber, mazeSprite, colorScheme, availableColorScheme, pacTileX, pacTileY);

        List<ColoredMapImage> flashingMazes = new ArrayList<>();
        if (multipleFlashColors) {
            for (var randomScheme : randomColorSchemes(flashCount, colorScheme)) {
                ColoredMapImage randomMaze = getOrCreateMaze(MapCategory.BIG, spriteNumber, mazeSprite, randomScheme, availableColorScheme, pacTileX, pacTileY);
                flashingMazes.add(randomMaze);
            }
        } else {
            ColoredMapImage blackWhiteMaze = getOrCreateMaze(MapCategory.BIG, spriteNumber, mazeSprite, FLASHING_BLACK_WHITE, availableColorScheme, pacTileX, pacTileY);
            for (int i = 0; i < flashCount; ++i) {
                flashingMazes.add(blackWhiteMaze);
            }
        }
        return new ColoredMapSet(normalMaze, flashingMazes);
    }

    private ColoredMapSet strangeMazeSet(int spriteNumber, NES_ColorScheme randomColorScheme, int flashCount, boolean multipleFlashColors) {
        NES_ColorScheme availableColorScheme = switch (spriteNumber) {
            case 1  -> _36_15_20_PINK_RED_WHITE;
            case 2  -> _21_20_28_BLUE_WHITE_YELLOW;
            case 3  ->  _16_20_15_ORANGE_WHITE_RED;
            case 4  -> _01_38_20_BLUE_YELLOW_WHITE;
            case 5  -> _35_28_20_PINK_YELLOW_WHITE;
            case 6  -> _36_15_20_PINK_RED_WHITE;
            case 7  -> _17_20_20_BROWN_WHITE_WHITE;
            case 8  -> _13_20_28_VIOLET_WHITE_YELLOW;
            case 9  -> _0F_20_28_BLACK_WHITE_YELLOW;
            case 10 -> _0F_01_20_BLACK_BLUE_WHITE;
            case 11 -> _14_25_20_VIOLET_ROSE_WHITE;
            case 12 -> _15_20_20_RED_WHITE_WHITE;
            case 13 -> _1B_20_20_GREEN_WHITE_WHITE;
            case 14 -> _28_20_2A_YELLOW_WHITE_GREEN;
            case 15 -> _1A_20_28_GREEN_WHITE_YELLOW;
            case 16 -> _18_20_20_KHAKI_WHITE_WHITE;
            case 17 -> _25_20_20_ROSE_WHITE_WHITE;
            case 18 -> _12_20_28_BLUE_WHITE_YELLOW;
            case 19 -> _07_20_20_BROWN_WHITE_WHITE;
            case 20 -> _15_25_20_RED_ROSE_WHITE;
            case 21 -> _0F_20_1C_BLACK_WHITE_GREEN;
            case 22 -> _19_20_20_GREEN_WHITE_WHITE;
            case 23 -> _0C_20_14_GREEN_WHITE_VIOLET;
            case 24 -> _23_20_2B_VIOLET_WHITE_GREEN;
            case 25 -> _10_20_28_GRAY_WHITE_YELLOW;
            case 26 -> _03_20_20_BLUE_WHITE_WHITE;
            case 27 -> _04_20_20_VIOLET_WHITE_WHITE;
            case 28 -> _00_2A_24_GRAY_GREEN_PINK;
            case 29 -> _21_35_20_BLUE_PINK_WHITE;
            case 30 -> _28_16_20_YELLOW_RED_WHITE;
            case 31 -> _12_16_20_BLUE_RED_WHITE;
            case 32 -> _15_25_20_RED_ROSE_WHITE;
            default -> throw new IllegalArgumentException("Illegal sprite number: " + spriteNumber);
        };
        RectArea mazeSprite = nonArcadeMazeSprite(spriteNumber);
        NES_ColorScheme colorScheme = randomColorScheme != null ? randomColorScheme : availableColorScheme;
        int pacTileX = 13, pacTileY = 23;
        ColoredMapImage normalMaze = colorScheme.equals(availableColorScheme)
            ? new ColoredMapImage(nonArcadeMazesImage, nonArcadeMazeSprite(spriteNumber), availableColorScheme)
            : getOrCreateMaze(MapCategory.STRANGE, spriteNumber, mazeSprite, colorScheme, availableColorScheme, pacTileX, pacTileY);

        List<ColoredMapImage> flashingMazes = new ArrayList<>();
        if (multipleFlashColors) {
            for (var randomScheme : randomColorSchemes(flashCount, colorScheme)) {
                ColoredMapImage randomMaze = getOrCreateMaze(MapCategory.STRANGE, spriteNumber, mazeSprite, randomScheme, availableColorScheme, pacTileX, pacTileY);
                flashingMazes.add(randomMaze);
            }
        } else {
            ColoredMapImage blackWhiteMaze = getOrCreateMaze(MapCategory.STRANGE, spriteNumber, mazeSprite, FLASHING_BLACK_WHITE, availableColorScheme, pacTileX, pacTileY);
            for (int i = 0; i < flashCount; ++i) {
                flashingMazes.add(blackWhiteMaze);
            }
        }
        return new ColoredMapSet(normalMaze, flashingMazes);
    }

    private RectArea nonArcadeMazeSprite(int spriteNumber) {
        int colIndex, y;
        switch (spriteNumber) {
            case 1,2,3,4,5,6,7,8            -> { colIndex = (spriteNumber - 1);  y = 0;    }
            case 9,10,11,12,13,14,15,16     -> { colIndex = (spriteNumber - 9);  y = 248;  }
            case 17,18,19,20,21,22,23,24    -> { colIndex = (spriteNumber - 17); y = 544;  }
            case 25,26,27,28,29,30,31,32,33 -> { colIndex = (spriteNumber - 25); y = 840;  }
            case 34,35,36,37                -> { colIndex = (spriteNumber - 34); y = 1136; }
            default -> throw new IllegalArgumentException("Illegal non-Arcade map number: " + spriteNumber);
        }
        int width = 28 * TS, height = NON_ARCADE_MAP_ROW_COUNTS[spriteNumber - 1] * TS;
        return new RectArea(colIndex * width, y, width, height);
    }

    private ColoredMapImage getOrCreateMaze(
        MapCategory mapCategory,
        int spriteNumber,
        RectArea mazeSprite,
        NES_ColorScheme newColorScheme,
        NES_ColorScheme existingColorScheme,
        int pacTileX, int pacTileY)
    {
        var cacheKey = new ColoredMapDefinition(mapCategory, spriteNumber, newColorScheme);
        if (!mazeCache.containsKey(cacheKey)) {
            Image spriteSource = mapCategory == MapCategory.ARCADE ? arcadeMazesImage : nonArcadeMazesImage;
            Image mazeImage = recolorMazeImage(spriteSource, mazeSprite, existingColorScheme, newColorScheme, pacTileX, pacTileY);
            var maze = new ColoredMapImage(mazeImage, new RectArea(0, 0, mazeSprite.width(), mazeSprite.height()), newColorScheme);
            mazeCache.put(cacheKey, maze);
            Logger.info("{} maze recolored to {} and put into cache (size: {})", mapCategory, newColorScheme, mazeCache.size());
        }
        return mazeCache.get(cacheKey);
    }

    private Image recolorMazeImage(
        Image source, RectArea mazeArea,
        NES_ColorScheme oldColorScheme, NES_ColorScheme newColorScheme,
        int pacTileX, int pacTileY)
    {
        Image mazeImage = subImage(source, mazeArea);
        Image imageWithoutGarbage = maskImage(mazeImage, (x, y) -> isActorPixel(x, y, pacTileX, pacTileY), Color.TRANSPARENT);
        return exchange_NESColorScheme(imageWithoutGarbage, oldColorScheme, newColorScheme);
    }

    private boolean isActorPixel(int x, int y, int pacTileX, int pacTileY) {
        return GHOST_OUTSIDE_HOUSE_AREA.contains(x, y)
            || GHOSTS_INSIDE_HOUSE_AREA.contains(x, y)
            || (pacTileX == x && pacTileY == y);
    }
}