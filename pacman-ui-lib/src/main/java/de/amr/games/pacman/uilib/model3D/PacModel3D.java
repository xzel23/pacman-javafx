/*
Copyright (c) 2021-2025 Armin Reichert (MIT License)
See file LICENSE in repository root directory for details.
*/
package de.amr.games.pacman.uilib.model3D;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.util.stream.Stream;

import static de.amr.games.pacman.uilib.Ufx.coloredMaterial;


/**
 * @author Armin Reichert
 */
public interface PacModel3D {

    String MESH_ID_EYES   = "PacMan.Eyes";
    String MESH_ID_HEAD   = "PacMan.Head";
    String MESH_ID_PALATE = "PacMan.Palate";

    static Group createPacShape(Model3D model3D, double size, Color headColor, Color eyesColor, Color palateColor) {
        var head = new MeshView(model3D.mesh(PacModel3D.MESH_ID_HEAD));
        head.setId(Model3D.toCSS_ID(PacModel3D.MESH_ID_HEAD));
        head.setMaterial(coloredMaterial(headColor));

        var eyes = new MeshView(model3D.mesh(PacModel3D.MESH_ID_EYES));
        eyes.setId(Model3D.toCSS_ID(PacModel3D.MESH_ID_EYES));
        eyes.setMaterial(coloredMaterial(eyesColor));

        var palate = new MeshView(model3D.mesh(PacModel3D.MESH_ID_PALATE));
        palate.setId(Model3D.toCSS_ID(PacModel3D.MESH_ID_PALATE));
        palate.setMaterial(coloredMaterial(palateColor));

        var root = new Group(head, eyes, palate);

        var bounds = head.getBoundsInLocal();
        var centeredOverOrigin = new Translate(-bounds.getCenterX(), -bounds.getCenterY(), -bounds.getCenterZ());
        Stream.of(head, eyes, palate).forEach(node -> node.getTransforms().add(centeredOverOrigin));

        // TODO check/fix Pac-Man mesh position and rotation in OBJ file
        root.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        root.getTransforms().add(new Rotate(180, Rotate.Y_AXIS));
        root.getTransforms().add(new Rotate(180, Rotate.Z_AXIS));

        var rootBounds = root.getBoundsInLocal();
        root.getTransforms().add(new Scale(size / rootBounds.getWidth(), size / rootBounds.getHeight(), size / rootBounds.getDepth()));

        return root;
    }

    static Group createPacSkull(Model3D model3D, double size, Color headColor, Color palateColor) {
        var head = new MeshView(model3D.mesh(PacModel3D.MESH_ID_HEAD));
        head.setId(Model3D.toCSS_ID(PacModel3D.MESH_ID_HEAD));
        head.setMaterial(coloredMaterial(headColor));

        var palate = new MeshView(model3D.mesh(PacModel3D.MESH_ID_PALATE));
        palate.setId(Model3D.toCSS_ID(PacModel3D.MESH_ID_PALATE));
        palate.setMaterial(coloredMaterial(palateColor));

        Bounds bounds = head.getBoundsInLocal();
        var centeredOverOrigin = new Translate(-bounds.getCenterX(), -bounds.getCenterY(), -bounds.getCenterZ());
        head.getTransforms().add(centeredOverOrigin);
        palate.getTransforms().add(centeredOverOrigin);

        var root = new Group(head, palate);

        Bounds rootBounds = root.getBoundsInLocal();
        // TODO check/fix Pac-Man mesh position and rotation in .obj file
        root.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        root.getTransforms().add(new Rotate(180, Rotate.Y_AXIS));
        root.getTransforms().add(new Rotate(180, Rotate.Z_AXIS));
        root.getTransforms().add(new Scale(size / rootBounds.getWidth(), size / rootBounds.getHeight(), size / rootBounds.getDepth()));

        return root;
    }

    static Group createFemaleParts(double pacSize, Color hairBowColor, Color hairBowPearlsColor, Color boobsColor) {
        var bowMaterial = coloredMaterial(hairBowColor);

        var bowLeft = new Sphere(1.2);
        bowLeft.getTransforms().addAll(new Translate(3.0, 1.5, -pacSize * 0.55));
        bowLeft.setMaterial(bowMaterial);

        var bowRight = new Sphere(1.2);
        bowRight.getTransforms().addAll(new Translate(3.0, -1.5, -pacSize * 0.55));
        bowRight.setMaterial(bowMaterial);

        var pearlMaterial = coloredMaterial(hairBowPearlsColor);

        var pearlLeft = new Sphere(0.4);
        pearlLeft.getTransforms().addAll(new Translate(2, 0.5, -pacSize * 0.58));
        pearlLeft.setMaterial(pearlMaterial);

        var pearlRight = new Sphere(0.4);
        pearlRight.getTransforms().addAll(new Translate(2, -0.5, -pacSize * 0.58));
        pearlRight.setMaterial(pearlMaterial);

        var beautySpot = new Sphere(0.5);
        beautySpot.getTransforms().addAll(new Translate(-0.33 * pacSize, -0.4 * pacSize, -0.14 * pacSize));
        beautySpot.setMaterial(coloredMaterial(Color.rgb(120, 120, 120)));

        var silicone = coloredMaterial(boobsColor);

        double bx = -0.2 * pacSize; // forward
        double by = 1.6; // or - 1.6 // sidewards
        double bz = 0.4 * pacSize; // up/down
        var boobLeft = new Sphere(1.8);
        boobLeft.setMaterial(silicone);
        boobLeft.getTransforms().addAll(new Translate(bx, -by, bz));

        var boobRight = new Sphere(1.8);
        boobRight.setMaterial(silicone);
        boobRight.getTransforms().addAll(new Translate(bx, by, bz));

        return new Group(bowLeft, bowRight, pearlLeft, pearlRight, boobLeft, boobRight, beautySpot);
    }
}
