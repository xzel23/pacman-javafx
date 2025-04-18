/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.uilib.input;

import de.amr.games.pacman.lib.nes.JoypadButtonID;
import de.amr.games.pacman.uilib.ActionProvider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class Joypad {

    /** <code>Select=SPACE, Start=ENTER, B=B, A=N, Up/Down/Left/Right=cursor keys </code> */
    public static final JoypadKeyBinding BINDING1 = new JoypadKeyBinding(
        new KeyCodeCombination(KeyCode.SPACE),
        new KeyCodeCombination(KeyCode.ENTER),
        new KeyCodeCombination(KeyCode.B),
        new KeyCodeCombination(KeyCode.N),
        new KeyCodeCombination(KeyCode.UP),
        new KeyCodeCombination(KeyCode.DOWN),
        new KeyCodeCombination(KeyCode.LEFT),
        new KeyCodeCombination(KeyCode.RIGHT)
    );

    /** <code>Select=U, Start=I, B=J, A=K, Up/Down/Left/Right=W/S/A/D</code> */
    public static final JoypadKeyBinding BINDING2 = new JoypadKeyBinding(
        new KeyCodeCombination(KeyCode.U),
        new KeyCodeCombination(KeyCode.I),
        new KeyCodeCombination(KeyCode.J),
        new KeyCodeCombination(KeyCode.K),
        new KeyCodeCombination(KeyCode.W),
        new KeyCodeCombination(KeyCode.S),
        new KeyCodeCombination(KeyCode.A),
        new KeyCodeCombination(KeyCode.D)
    );

    private final JoypadKeyBinding[] bindings = {
        BINDING1, BINDING2
    };

    private final Keyboard keyboard;
    private int selectedIndex;

    public Joypad(Keyboard keyboard) {
        this.keyboard = requireNonNull(keyboard);
    }

    public JoypadKeyBinding currentKeyBinding() {
        return bindings[selectedIndex];
    }

    public Stream<KeyCodeCombination> currentKeys() {
        return Stream.of(JoypadButtonID.values()).map(currentKeyBinding()::key);
    }

    public boolean isButtonPressed(JoypadButtonID buttonID) {
        return keyboard.isMatching(key(buttonID));
    }

    public KeyCodeCombination key(JoypadButtonID buttonID) {
        return currentKeyBinding().key(buttonID);
    }

    public void registerCurrentBinding(ActionProvider actionProvider) {
        currentKeys().forEach(combination -> keyboard.bind(combination, actionProvider));
    }

    public void unregisterCurrentBinding(ActionProvider actionProvider) {
        currentKeys().forEach(combination -> keyboard.unbind(combination, actionProvider));
    }

    public void selectNextKeyBinding(ActionProvider actionProvider) {
        selectedIndex = (selectedIndex + 1) % bindings.length;
        setBinding(selectedIndex, actionProvider);
    }

    private void setBinding(int index, ActionProvider actionProvider) {
        unregisterCurrentBinding(actionProvider);
        selectedIndex = index;
        registerCurrentBinding(actionProvider);
    }
}