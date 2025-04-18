/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.arcade.pacman_xxl;

import de.amr.games.pacman.Globals;
import de.amr.games.pacman.arcade.ms_pacman.ArcadeMsPacMan_GameModel;
import de.amr.games.pacman.model.MapSelector;
import de.amr.games.pacman.steering.RuleBasedPacSteering;

import java.io.File;

public class PacManXXL_MsPacMan_GameModel extends ArcadeMsPacMan_GameModel {

    public PacManXXL_MsPacMan_GameModel(MapSelector mapSelector) {
        super(mapSelector);
    }

    @Override
    public void init() {
        super.init();
        demoLevelSteering = new RuleBasedPacSteering(this); // super class uses predefined steering
        scoreManager.setHighScoreFile(new File(Globals.HOME_DIR, "highscore-mspacman_xxl.xml"));
        mapSelector.loadAllMaps(this);
    }
}