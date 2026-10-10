package com.swiftfaze.veil.world;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;

import java.nio.file.Paths;

public class TileTestScene2 extends WorldScene {

    private static final int HOUSE_X = 130;
    private static final int HOUSE_Y = 130;

    public TileTestScene2(int width, int height) {
        super(width, height);

        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        fillAll(mods.getTile(CoreTiles.GRASS));

        Building house2 = mods.getBuilding("core:small_house_01");

        house2.setWorldX(HOUSE_X);
        house2.setWorldY(HOUSE_Y);

        placeBuilding(house2);

        // Playtest aid: one tile inside the house's door (local 3,6), so E can be tried
        // straight after walking in. The door itself comes from the building's objects list.
        placeObject(HOUSE_X + 3, HOUSE_Y + 4, mods.getObject("core:starter_chest").newInstance());
    }
}
