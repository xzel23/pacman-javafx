/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.arcade;

import de.amr.games.pacman.model.GameLevel;
import de.amr.games.pacman.model.LevelCounter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ArcadePacMan_LevelCounter implements LevelCounter {

    public static final byte LEVEL_COUNTER_MAX_SIZE = 7;

    private final BooleanProperty enabledPy = new SimpleBooleanProperty(true);
    private final List<Byte> symbols = new ArrayList<>();

    public BooleanProperty enabledProperty() {
        return enabledPy;
    }

    @Override
    public Stream<Byte> symbols() {
        return symbols.stream();
    }

    @Override
    public void reset() {
        symbols.clear();
    }

    @Override
    public void update(GameLevel level) {
        if (level.number() == 1) {
            symbols.clear();
        }
        if (isEnabled()) {
            symbols.add(level.bonusSymbol(0));
            if (symbols.size() > LEVEL_COUNTER_MAX_SIZE) {
                symbols.removeFirst();
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        enabledProperty().set(enabled);
    }

    @Override
    public boolean isEnabled() {
        return enabledProperty().get();
    }
}
