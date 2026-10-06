package dev.overloadsim.compat;

import appeng.api.networking.IGridConnection;

/** Owned bridges may outlive an endpoint that AE2 has already removed. */
public final class GridConnections {
    private GridConnections() {}

    public static void destroyOwned(IGridConnection connection, boolean owned) {
        // AE2 node removal already deletes its edges, and GridConnection.destroy is not idempotent.
        // Destroying that cached edge again can recreate a grid for the removed endpoint.
        if (connection != null && owned
                && connection.a().getConnections().contains(connection)
                && connection.b().getConnections().contains(connection)) connection.destroy();
    }
}
