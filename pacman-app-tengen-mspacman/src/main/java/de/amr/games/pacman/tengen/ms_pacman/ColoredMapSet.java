/*
 * Copyright (c) 2021-2025 Armin Reichert (MIT License) See file LICENSE in repository root directory for details.
 */
package de.amr.games.pacman.tengen.ms_pacman;

import java.util.List;

public record ColoredMapSet(ColoredMapImage normalMaze, List<ColoredMapImage> flashingMazes) {
}
