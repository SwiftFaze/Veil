package com.swiftfaze.veil.entities.npc;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcTest {

    private static final TileCoord A = new TileCoord(3, 4);
    private static final TileCoord B = new TileCoord(5, 4);
    private static final TileCoord C = new TileCoord(5, 6);
    private static final TileCoord D = new TileCoord(3, 6);

    @Test
    void pingPongTurnsAroundAtTheEndOfItsRoute() {
        Npc npc = patrolling(PatrolMode.PING_PONG, Set.of(), A, B);

        stepTurns(npc, 3);

        assertPosition(npc, 4, 4);
    }

    @Test
    void pingPongWithThreeWaypointsTurnsAtTheCornerThenPacesBack() {
        Npc npc = patrolling(PatrolMode.PING_PONG, Set.of(), A, B, C);

        stepTurns(npc, 4);
        assertPosition(npc, 5, 6);

        stepTurns(npc, 3);
        assertPosition(npc, 4, 4);
    }

    @Test
    void loopReturnsFromTheLastWaypointToTheFirst() {
        Npc npc = patrolling(PatrolMode.LOOP, Set.of(), A, B, C, D);

        stepTurns(npc, 4);
        assertPosition(npc, 5, 6);

        stepTurns(npc, 2);
        assertPosition(npc, 3, 6);

        stepTurns(npc, 2);
        assertPosition(npc, 3, 4);
    }

    @Test
    void blockedStepWaitsOnTheCurrentTileAndRetriesTheSameStep() {
        Set<TileCoord> blocked = new HashSet<>(Set.of(new TileCoord(4, 4)));
        Npc npc = patrolling(PatrolMode.PING_PONG, blocked, A, B);

        stepTurns(npc, 3);
        assertPosition(npc, 3, 4);

        blocked.clear();
        stepTurns(npc, 1);
        assertPosition(npc, 4, 4);
    }

    @Test
    void stationaryNpcNeverMoves() {
        NpcDefinition definition = new NpcDefinition("core:statue", "Statue", 'S', Color.GRAY, new TileCoord(2, 2), null);
        Npc npc = new Npc(definition, (x, y) -> true);

        stepTurns(npc, 3);

        assertPosition(npc, 2, 2);
    }

    private static Npc patrolling(PatrolMode mode, Set<TileCoord> blocked, TileCoord... waypoints) {
        Patrol patrol = new Patrol(mode, List.of(waypoints));
        NpcDefinition definition = new NpcDefinition("core:test", "Test", 'T', Color.WHITE, waypoints[0], patrol);
        return new Npc(definition, (x, y) -> !blocked.contains(new TileCoord(x, y)));
    }

    private static void stepTurns(Npc npc, int turns) {
        for (int i = 0; i < turns; i++) {
            npc.onTurn(i + 1);
        }
    }

    private static void assertPosition(Npc npc, int x, int y) {
        assertEquals(new TileCoord(x, y), new TileCoord(npc.getX(), npc.getY()));
    }
}
