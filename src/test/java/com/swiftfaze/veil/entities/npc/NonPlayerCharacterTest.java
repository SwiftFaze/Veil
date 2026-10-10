package com.swiftfaze.veil.entities.npc;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NonPlayerCharacterTest {

    private static final TileCoord WAYPOINT_A = new TileCoord(3, 4);
    private static final TileCoord WAYPOINT_B = new TileCoord(5, 4);
    private static final TileCoord WAYPOINT_C = new TileCoord(5, 6);
    private static final TileCoord WAYPOINT_D = new TileCoord(3, 6);

    @Test
    void pingPongTurnsAroundAtTheEndOfItsRoute() {
        NonPlayerCharacter npc = patrolling(PatrolMode.PING_PONG, List.of(), WAYPOINT_A, WAYPOINT_B);

        stepTurns(npc, 3);

        assertPosition(npc, 4, 4);
    }

    @Test
    void pingPongWithThreeWaypointsTurnsAtTheCornerThenPacesBack() {
        NonPlayerCharacter npc = patrolling(PatrolMode.PING_PONG, List.of(), WAYPOINT_A, WAYPOINT_B, WAYPOINT_C);

        stepTurns(npc, 4);
        assertPosition(npc, 5, 6);

        stepTurns(npc, 3);
        assertPosition(npc, 4, 4);
    }

    @Test
    void loopReturnsFromTheLastWaypointToTheFirst() {
        NonPlayerCharacter npc = patrolling(PatrolMode.LOOP, List.of(), WAYPOINT_A, WAYPOINT_B, WAYPOINT_C, WAYPOINT_D);

        stepTurns(npc, 4);
        assertPosition(npc, 5, 6);

        stepTurns(npc, 2);
        assertPosition(npc, 3, 6);

        stepTurns(npc, 2);
        assertPosition(npc, 3, 4);
    }

    @Test
    void blockedStepWaitsOnTheCurrentTileAndRetriesTheSameStep() {
        List<TileCoord> blocked = new ArrayList<>(List.of(new TileCoord(4, 4)));
        NonPlayerCharacter npc = patrolling(PatrolMode.PING_PONG, blocked, WAYPOINT_A, WAYPOINT_B);

        stepTurns(npc, 3);
        assertPosition(npc, 3, 4);

        blocked.clear();
        stepTurns(npc, 1);
        assertPosition(npc, 4, 4);
    }

    @Test
    void stationaryNpcNeverMoves() {
        NpcDefinition definition = new NpcDefinition("core:statue", "Statue", 'S', Color.GRAY, new TileCoord(2, 2), null);
        NonPlayerCharacter npc = new NonPlayerCharacter(definition, (x, y) -> true);

        stepTurns(npc, 3);

        assertPosition(npc, 2, 2);
    }

    private static NonPlayerCharacter patrolling(PatrolMode mode, List<TileCoord> blocked, TileCoord... waypoints) {
        Patrol patrol = new Patrol(mode, List.of(waypoints));
        NpcDefinition definition = new NpcDefinition("core:test", "Test", 'T', Color.WHITE, waypoints[0], patrol);
        return new NonPlayerCharacter(definition, (x, y) -> !blocked.contains(new TileCoord(x, y)));
    }

    private static void stepTurns(NonPlayerCharacter npc, int turns) {
        for (int i = 0; i < turns; i++) {
            npc.onTurn(i + 1);
        }
    }

    private static void assertPosition(NonPlayerCharacter npc, int x, int y) {
        assertEquals(new TileCoord(x, y), new TileCoord(npc.getX(), npc.getY()));
    }
}
