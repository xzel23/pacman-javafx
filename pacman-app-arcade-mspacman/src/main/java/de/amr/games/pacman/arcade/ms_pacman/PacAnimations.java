/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.arcade.ms_pacman;

import de.amr.games.pacman.lib.Direction;
import de.amr.games.pacman.lib.RectArea;
import de.amr.games.pacman.model.actors.Actor2D;
import de.amr.games.pacman.model.actors.ActorAnimations;
import de.amr.games.pacman.model.actors.Pac;
import de.amr.games.pacman.ui._2d.SpriteAnimationSet;
import de.amr.games.pacman.uilib.animation.SpriteAnimation;
import de.amr.games.pacman.uilib.assets.SpriteSheet;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Armin Reichert
 */
public class PacAnimations extends SpriteAnimationSet {

    public PacAnimations(ArcadeMsPacMan_SpriteSheet spriteSheet) {
        requireNonNull(spriteSheet);

        var munching = SpriteAnimation
            .spriteSheet(spriteSheet)
            .sprites(spriteSheet.pacMunchingSprites(Direction.LEFT))
            .endLoop();

        var dying = SpriteAnimation
            .spriteSheet(spriteSheet)
            .sprites(spriteSheet.pacDyingSprites())
            .frameTicks(8)
            .end();

        var husbandMunching = SpriteAnimation
            .spriteSheet(spriteSheet)
            .sprites(spriteSheet.mrPacManMunchingSprites(Direction.LEFT))
            .frameTicks(2)
            .endLoop();

        add(Map.of(
            ActorAnimations.ANIM_PAC_MUNCHING, munching,
            ActorAnimations.ANIM_PAC_DYING, dying,
            ActorAnimations.ANIM_MR_PACMAN_MUNCHING, husbandMunching
        ));
    }

    @Override
    protected RectArea[] selectedSprites(SpriteSheet spriteSheet, Actor2D actor) {
        ArcadeMsPacMan_SpriteSheet gss = (ArcadeMsPacMan_SpriteSheet) spriteSheet;
        if (actor instanceof Pac msPacMan) {
            if (isCurrentAnimationID(ActorAnimations.ANIM_PAC_MUNCHING)) {
                return gss.pacMunchingSprites(msPacMan.moveDir());
            }
            if (isCurrentAnimationID(ActorAnimations.ANIM_MR_PACMAN_MUNCHING)) {
                return gss.mrPacManMunchingSprites(msPacMan.moveDir());
            }
        }
        return super.selectedSprites(spriteSheet, actor);
    }
}