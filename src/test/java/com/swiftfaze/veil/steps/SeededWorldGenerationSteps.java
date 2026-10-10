package com.swiftfaze.veil.steps;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.world.CoreTiles;
import com.swiftfaze.veil.game.worldgen.GeneratedWorldScene;
import com.swiftfaze.veil.world.PerlinNoise;
import com.swiftfaze.veil.world.TerrainBuckets;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import com.swiftfaze.veil.world.WorldSeeds;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeededWorldGenerationSteps {

    private static final int MAP_WIDTH = GameConst.DEFAULT_MAP_WIDTH;
    private static final int MAP_HEIGHT = GameConst.DEFAULT_MAP_HEIGHT;
    private static final int SMALL_MAP = 40;
    private static final int SEED_SEARCH_LIMIT = 5000;
    private static final String SEED_LOG_PREFIX = "World generated from seed ";
    private static final List<String> CORE_TERRAIN = List.of(CoreTiles.WATER, CoreTiles.SAND, CoreTiles.MUD,
            CoreTiles.GRASS, CoreTiles.TREE, CoreTiles.ROCK, CoreTiles.STONE, CoreTiles.SNOW, CoreTiles.ICE);

    private static final String HUT_BUILDING = """
            {
              "id": "test:hut",
              "name": "hut",
              "type": "house",
              "width": 3,
              "height": 3,
              "tiles": [
                ["core:stone", "core:stone", "core:stone"],
                ["core:stone", "core:wood", "core:stone"],
                ["core:stone", "core:door", "core:stone"]
              ]
            }
            """;

    private enum SpawnSetup { CENTRE_CLEAR, CENTRE_WATER, CENTRE_UNDER_BUILDING }

    private enum ModsSetup { WITH_HUT, NO_BUILDINGS }

    private ModRegistry realRegistry;
    private ModRegistry registry;
    private DoubleBinaryOperator noise;
    private Path tempMods;
    private long worldSeed;
    private GeneratedWorldScene world;
    private GeneratedWorldScene otherWorld;
    private String bucketedTile;
    private SpawnSetup spawnSetup;
    private int walkableOffset;
    private Set<String> blueprintsRegistered = Set.of();
    private final Set<String> blueprintsSeen = new HashSet<>();
    private GamePanel panel;
    /** Every log event from the scenario so far, from any logger. Filled by {@link #attachLogCapture()}. */
    private final List<ILoggingEvent> captured = new ArrayList<>();
    private AppenderBase<ILoggingEvent> logCapture;

    @Before
    public void clearSeedOverride() {
        System.clearProperty(WorldSeeds.PROPERTY);
    }

    @Before
    public void attachLogCapture() {
        logCapture = new AppenderBase<>() {
            @Override
            protected void append(ILoggingEvent event) {
                captured.add(event);
            }
        };
        logCapture.start();
        rootLogger().addAppender(logCapture);
    }

    @After
    public void detachLogCapture() {
        rootLogger().detachAppender(logCapture);
    }

    private static Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    /** The world a fresh game generates for {@code seed} with the real mods. */
    static GeneratedWorldScene worldFromSeed(long seed) {
        return new GeneratedWorldScene(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT, seed);
    }

    /** Asserts two worlds match in terrain, building positions and spawn point. */
    static void assertSameWorld(GeneratedWorldScene expected, GeneratedWorldScene actual) {
        assertEquals(positions(expected), positions(actual));
        assertEquals(expected.getSpawnX(), actual.getSpawnX());
        assertEquals(expected.getSpawnY(), actual.getSpawnY());
        assertTrue(tilesMatch(expected, actual), "terrain differs");
    }

    @After
    public void restoreSeedOverrideAndTempMods() throws IOException {
        System.clearProperty(WorldSeeds.PROPERTY);
        if (tempMods != null) {
            try (Stream<Path> paths = Files.walk(tempMods)) {
                paths.sorted(Comparator.reverseOrder()).forEach(SeededWorldGenerationSteps::deleteQuietly);
            }
        }
    }

    // --- Determinism -------------------------------------------------------------------------

    @Given("a world generated from seed {int}")
    public void aWorldGeneratedFromSeed(int seed) {
        world = generate(seed);
    }

    @When("another world is generated from seed {int}")
    public void anotherWorldIsGeneratedFromSeed(int seed) {
        otherWorld = generate(seed);
    }

    @Then("both worlds have the same tile at every position")
    public void bothWorldsHaveTheSameTileAtEveryPosition() {
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                assertEquals(tileId(world.getTile(x, y)), tileId(otherWorld.getTile(x, y)), "tile at " + x + "," + y);
            }
        }
    }

    @Then("both worlds have the same buildings at the same positions")
    public void bothWorldsHaveTheSameBuildingsAtTheSamePositions() {
        assertEquals(positions(world), positions(otherWorld));
    }

    @Then("both worlds spawn the player at the same position")
    public void bothWorldsSpawnThePlayerAtTheSamePosition() {
        assertEquals(world.getSpawnX(), otherWorld.getSpawnX());
        assertEquals(world.getSpawnY(), otherWorld.getSpawnY());
    }

    @Then("the two worlds differ in at least one tile")
    public void theTwoWorldsDifferInAtLeastOneTile() {
        boolean differ = false;
        for (int x = 0; x < MAP_WIDTH && !differ; x++) {
            for (int y = 0; y < MAP_HEIGHT && !differ; y++) {
                differ = !tileId(world.getTile(x, y)).equals(tileId(otherWorld.getTile(x, y)));
            }
        }
        assertTrue(differ, "expected seeds 12345 and 12601 to produce different terrain");
    }

    // --- Terrain -----------------------------------------------------------------------------

    @Then("^every tile outside a building is one of (.+)$")
    public void everyTileOutsideABuildingIsOneOf(String expectedList) {
        assertEquals(CORE_TERRAIN, List.of(expectedList.split(", ")));
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (!coveredByAnyBuilding(world.getBuildings(), x, y)) {
                    assertTrue(CORE_TERRAIN.contains(tileId(world.getTile(x, y))), "tile at " + x + "," + y);
                }
            }
        }
    }

    @When("^the noise value (-?\\d+\\.\\d+) is bucketed$")
    public void theNoiseValueIsBucketed(String value) {
        bucketedTile = TerrainBuckets.tileIdFor(Double.parseDouble(value));
    }

    @Then("the terrain tile is {string}")
    public void theTerrainTileIs(String expected) {
        assertEquals(expected, bucketedTile);
    }

    @Then("every tile is one of the core terrain tiles")
    public void everyTileIsOneOfTheCoreTerrainTiles() {
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                assertTrue(CORE_TERRAIN.contains(tileId(world.getTile(x, y))), "tile at " + x + "," + y);
            }
        }
    }

    // --- Buildings ---------------------------------------------------------------------------

    @Then("^the world contains (\\d+) buildings?$")
    public void theWorldContainsBuildings(int expected) {
        assertEquals(expected, world.getBuildings().size());
    }

    @Then("no two placed buildings share a tile")
    public void noTwoPlacedBuildingsShareATile() {
        List<Building> buildings = world.getBuildings();
        for (int i = 0; i < buildings.size(); i++) {
            for (int j = i + 1; j < buildings.size(); j++) {
                for (Point tile : footprint(buildings.get(i))) {
                    assertFalse(buildings.get(j).covers(tile.x, tile.y), "overlap at " + tile);
                }
            }
        }
    }

    @Then("every tile under each building's footprint was walkable terrain before placement")
    public void everyFootprintTileWasWalkableTerrainBeforePlacement() {
        PerlinNoise terrain = new PerlinNoise(worldSeed);
        for (Building building : world.getBuildings()) {
            for (Point tile : footprint(building)) {
                String id = TerrainBuckets.tileIdFor(terrain.noise(tile.x, tile.y));
                assertTrue(realRegistry().getTile(id).isWalkable(), "unwalkable " + id + " under " + tile);
            }
        }
    }

    @Then("the tile directly outside each building's door is walkable and not under any building")
    public void theTileOutsideEachDoorIsWalkableAndFree() {
        for (Building building : world.getBuildings()) {
            List<Point> doorways = outsideDoorTiles(building);
            assertFalse(doorways.isEmpty(), "building has no door");
            for (Point tile : doorways) {
                assertTrue(world.isWalkable(tile.x, tile.y), "door opens onto unwalkable " + tile);
                assertFalse(coveredByAnyBuilding(world.getBuildings(), tile.x, tile.y), "door opens into a building at " + tile);
            }
        }
    }

    @Then("every building's footprint lies within the map bounds")
    public void everyFootprintLiesWithinTheMapBounds() {
        for (Building building : world.getBuildings()) {
            assertTrue(building.getWorldX() >= 0 && building.getWorldY() >= 0);
            assertTrue(building.getWorldX() + building.getWidth() <= MAP_WIDTH);
            assertTrue(building.getWorldY() + building.getHeight() <= MAP_HEIGHT);
        }
    }

    @Given("^the registered blueprints are \"([^\"]*)\" and \"([^\"]*)\"$")
    public void theRegisteredBlueprintsAre(String first, String second) throws IOException {
        blueprintsRegistered = Set.of(first, second);
        registry = loadTempMods(ModsSetup.WITH_HUT);
        assertEquals(blueprintsRegistered, new HashSet<>(registry.getBuildingIds()));
    }

    @When("^worlds are generated from seeds (\\d+) to (\\d+)$")
    public void worldsAreGeneratedFromSeeds(int from, int to) {
        for (int seed = from; seed <= to; seed++) {
            for (Building building : generate(seed).getBuildings()) {
                blueprintsSeen.addAll(blueprintIdsMatching(building));
            }
        }
    }

    @Then("both blueprints appear among the placed buildings")
    public void bothBlueprintsAppearAmongThePlacedBuildings() {
        assertTrue(blueprintsSeen.containsAll(blueprintsRegistered), "seen " + blueprintsSeen);
    }

    @Given("terrain where every tile is core:water")
    public void terrainWhereEveryTileIsWater() {
        noise = (x, y) -> -1.0;
    }

    @When("the world is generated")
    public void theWorldIsGenerated() {
        world = generate(12345);
    }

    @Then("generation completes without error")
    public void generationCompletesWithoutError() {
        assertNotNull(world);
    }

    @Given("no building blueprints are registered")
    public void noBuildingBlueprintsAreRegistered() throws IOException {
        registry = loadTempMods(ModsSetup.NO_BUILDINGS);
        assertEquals(List.of(), registry.getBuildingIds());
    }

    @When("a world is generated from seed {int}")
    public void aWorldIsGeneratedFromSeed(int seed) {
        world = generate(seed);
    }

    // --- Spawn -------------------------------------------------------------------------------

    @Given("a generated world whose centre tile is walkable and outside any building")
    public void aGeneratedWorldWhoseCentreIsWalkableAndClear() {
        spawnSetup = SpawnSetup.CENTRE_CLEAR;
    }

    @Given("a generated world whose centre tile is core:water")
    public void aGeneratedWorldWhoseCentreIsWater() {
        spawnSetup = SpawnSetup.CENTRE_WATER;
    }

    @Given("the nearest walkable tile outside any building is {int} tiles from the centre")
    public void theNearestWalkableTileIsTilesFromTheCentre(int offset) {
        walkableOffset = offset;
    }

    @Given("a generated world whose centre tile is under a building")
    public void aGeneratedWorldWhoseCentreIsUnderABuilding() {
        spawnSetup = SpawnSetup.CENTRE_UNDER_BUILDING;
    }

    @When("the player spawns")
    public void thePlayerSpawns() {
        world = switch (spawnSetup) {
            case CENTRE_CLEAR -> firstWorld(this::generate, SeededWorldGenerationSteps::centreIsWalkableAndClear);
            case CENTRE_WATER -> generateWaterWithOneWalkableTile();
            case CENTRE_UNDER_BUILDING -> firstWorld(this::smallGrassWorld, SeededWorldGenerationSteps::blockedAtCentre);
        };
    }

    @Then("the player is at the map centre")
    public void thePlayerIsAtTheMapCentre() {
        assertEquals(MAP_WIDTH / 2, world.getSpawnX());
        assertEquals(MAP_HEIGHT / 2, world.getSpawnY());
    }

    @Then("the player is on that tile")
    public void thePlayerIsOnThatTile() {
        assertEquals(MAP_WIDTH / 2 + walkableOffset, world.getSpawnX());
        assertEquals(MAP_HEIGHT / 2, world.getSpawnY());
    }

    @Then("the player is on a walkable tile outside every building footprint")
    public void thePlayerIsOnAWalkableTileOutsideEveryFootprint() {
        int x = world.getSpawnX();
        int y = world.getSpawnY();
        assertTrue(world.isWalkable(x, y), "spawn " + x + "," + y + " is not walkable");
        assertFalse(coveredByAnyBuilding(world.getBuildings(), x, y), "spawn lies inside a building");
    }

    // --- Seed source -------------------------------------------------------------------------

    @Given("no world-seed override is set")
    public void noWorldSeedOverrideIsSet() {
        System.clearProperty(WorldSeeds.PROPERTY);
    }

    @Given("the world-seed override is {string}")
    public void theWorldSeedOverrideIs(String value) {
        System.setProperty(WorldSeeds.PROPERTY, value);
    }

    @When("two new games are started")
    public void twoNewGamesAreStarted() {
        Random fresh = new Random(42);
        newGame(fresh);
        newGame(fresh);
    }

    @When("a new game is started")
    public void aNewGameIsStarted() {
        panel = newGame(new Random(42));
    }

    @Then("^each game logs \"World generated from seed <n>\" at INFO$")
    public void eachGameLogsAtInfo() {
        List<ILoggingEvent> seedLines = worldSeedLines();
        assertEquals(2, seedLines.size(), "one log line per new game");
        for (ILoggingEvent line : seedLines) {
            assertEquals(Level.INFO, line.getLevel());
            assertTrue(line.getFormattedMessage().matches(SEED_LOG_PREFIX + "-?\\d+"), line.getFormattedMessage());
        }
    }

    @Then("the two logged seeds differ")
    public void theTwoLoggedSeedsDiffer() {
        List<String> lines = worldSeedLines().stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertNotEquals(lines.get(0), lines.get(1));
    }

    @Then("^the log reads \"World generated from seed (-?\\d+)\"$")
    public void theLogReads(String seed) {
        assertTrue(worldSeedLines().stream().anyMatch(e -> e.getFormattedMessage().equals(SEED_LOG_PREFIX + seed)),
                "logged: " + worldSeedLines());
    }

    @Then("^the world matches a world generated from seed (-?\\d+)$")
    public void theWorldMatchesAWorldGeneratedFromSeed(String seed) {
        GeneratedWorldScene actual = (GeneratedWorldScene) panel.getScene();
        assertSameWorld(generate(Long.parseLong(seed)), actual);
    }

    @Then("^a warning naming \"([^\"]*)\" is logged$")
    public void aWarningNamingIsLogged(String value) {
        assertTrue(captured.stream().anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains(value)), "warnings: " + captured);
    }

    @Then("the game starts with a world generated from a random seed")
    public void theGameStartsWithARandomSeedWorld() {
        assertTrue(panel.getScene() instanceof GeneratedWorldScene);
        assertEquals(1, worldSeedLines().size());
    }

    // --- Helpers -----------------------------------------------------------------------------

    private GeneratedWorldScene generate(long seed) {
        worldSeed = seed;
        ModRegistry mods = registry != null ? registry : realRegistry();
        if (noise == null) {
            return new GeneratedWorldScene(MAP_WIDTH, MAP_HEIGHT, seed, mods);
        }
        return new GeneratedWorldScene(MAP_WIDTH, MAP_HEIGHT, seed, new GeneratedWorldScene.Sources(mods, noise));
    }

    private ModRegistry realRegistry() {
        if (realRegistry == null) {
            realRegistry = ModLoader.load(Paths.get("mods"));
        }
        return realRegistry;
    }

    private GeneratedWorldScene smallGrassWorld(long seed) {
        return new GeneratedWorldScene(SMALL_MAP, SMALL_MAP, seed, new GeneratedWorldScene.Sources(realRegistry(), (x, y) -> 0.0));
    }

    private GeneratedWorldScene generateWaterWithOneWalkableTile() {
        int centre = MAP_WIDTH / 2;
        DoubleBinaryOperator island = (x, y) -> x == centre + walkableOffset && y == centre ? 0.0 : -1.0;
        return new GeneratedWorldScene(MAP_WIDTH, MAP_HEIGHT, 1, new GeneratedWorldScene.Sources(realRegistry(), island));
    }

    /** The first world, seeds counted up from 1, that satisfies {@code wanted}: the scenario's precondition. */
    private static GeneratedWorldScene firstWorld(LongFunction<GeneratedWorldScene> build,
                                                  Predicate<GeneratedWorldScene> wanted) {
        for (long seed = 1; seed <= SEED_SEARCH_LIMIT; seed++) {
            GeneratedWorldScene candidate = build.apply(seed);
            if (wanted.test(candidate)) {
                return candidate;
            }
        }
        throw new AssertionError("no seed up to " + SEED_SEARCH_LIMIT + " met the precondition");
    }

    private static boolean centreIsWalkableAndClear(GeneratedWorldScene scene) {
        return scene.isWalkable(scene.getWidth() / 2, scene.getHeight() / 2) && !blockedAtCentre(scene);
    }

    private static boolean blockedAtCentre(GeneratedWorldScene scene) {
        return coveredByAnyBuilding(scene.getBuildings(), scene.getWidth() / 2, scene.getHeight() / 2);
    }

    private static boolean coveredByAnyBuilding(List<Building> buildings, int x, int y) {
        return buildings.stream().anyMatch(b -> b.covers(x, y));
    }

    private static List<Point> footprint(Building building) {
        List<Point> tiles = new ArrayList<>();
        for (int x = 0; x < building.getWidth(); x++) {
            for (int y = 0; y < building.getHeight(); y++) {
                tiles.add(new Point(building.getWorldX() + x, building.getWorldY() + y));
            }
        }
        return tiles;
    }

    /** Tiles directly outside each door on an edge row or column (corner doors give two). */
    private static List<Point> outsideDoorTiles(Building building) {
        List<Point> tiles = new ArrayList<>();
        for (int y = 0; y < building.getHeight(); y++) {
            for (int x = 0; x < building.getWidth(); x++) {
                if (!CoreTiles.DOOR.equals(building.getBlueprint().get(y).get(x).getId())) {
                    continue;
                }
                int wx = building.getWorldX() + x;
                int wy = building.getWorldY() + y;
                if (y == 0) {
                    tiles.add(new Point(wx, wy - 1));
                }
                if (y == building.getHeight() - 1) {
                    tiles.add(new Point(wx, wy + 1));
                }
                if (x == 0) {
                    tiles.add(new Point(wx - 1, wy));
                }
                if (x == building.getWidth() - 1) {
                    tiles.add(new Point(wx + 1, wy));
                }
            }
        }
        return tiles;
    }

    /** Ids of every registered blueprint whose tiles match this placed building's. */
    private List<String> blueprintIdsMatching(Building building) {
        List<String> matches = new ArrayList<>();
        for (String id : realOrTestRegistry().getBuildingIds()) {
            if (tileIds(realOrTestRegistry().getBuilding(id)).equals(tileIds(building))) {
                matches.add(id);
            }
        }
        return matches;
    }

    private ModRegistry realOrTestRegistry() {
        return registry != null ? registry : realRegistry();
    }

    private static List<List<String>> tileIds(Building building) {
        return building.getBlueprint().stream().map(row -> row.stream().map(Tile::getId).toList()).toList();
    }

    private static List<String> positions(GeneratedWorldScene scene) {
        return scene.getBuildings().stream().map(b -> b.getWorldX() + "," + b.getWorldY()).toList();
    }

    private static boolean tilesMatch(WorldScene a, WorldScene b) {
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (!tileId(a.getTile(x, y)).equals(tileId(b.getTile(x, y)))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String tileId(Tile tile) {
        return tile == null ? "" : tile.getId();
    }

    private static GamePanel newGame(Random fresh) {
        return new GamePanel(GameEventLog.noOp(), WorldSeeds.fromSystemProperty(fresh::nextLong));
    }

    private List<ILoggingEvent> worldSeedLines() {
        return captured.stream()
                .filter(e -> e.getFormattedMessage().startsWith(SEED_LOG_PREFIX))
                .toList();
    }

    /**
     * A mods root holding a copy of core and, optionally, the test:hut blueprint. Core's
     * buildings are left out when {@code withCoreBuildings} is false.
     */
    private ModRegistry loadTempMods(ModsSetup setup) throws IOException {
        tempMods = Files.createTempDirectory("veil-seeded-mods");
        Path core = tempMods.resolve("core");
        Path source = Paths.get("mods", "core");
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                if (setup == ModsSetup.NO_BUILDINGS && relative.startsWith("buildings")) {
                    continue;
                }
                Path target = core.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target);
                }
            }
        }
        if (setup == ModsSetup.WITH_HUT) {
            Path mod = tempMods.resolve("test");
            Files.createDirectories(mod.resolve("buildings"));
            Files.writeString(mod.resolve("mod.json"), "{\"id\": \"test\", \"dependsOn\": [\"core\"]}");
            Files.writeString(mod.resolve("buildings").resolve("hut.json"), HUT_BUILDING);
        }
        return ModLoader.load(tempMods);
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
