package dev.overloadsim.output;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Opaque row identities prevent a delayed delete from acting on a newly reordered row. */
public final class OutputTargetSnapshot<T> {
    private int slot = -1;
    private List<T> rows = List.of();
    private List<String> tokens = List.of();

    public void refresh(int selectedSlot, List<T> visibleRows) {
        if (slot == selectedSlot && rows.equals(visibleRows)) return;
        slot = selectedSlot;
        rows = List.copyOf(visibleRows);
        var next = new ArrayList<String>(rows.size());
        for (int i = 0; i < rows.size(); i++) next.add(UUID.randomUUID().toString());
        tokens = List.copyOf(next);
    }

    public String token(int row) { return row >= 0 && row < tokens.size() ? tokens.get(row) : ""; }

    public T resolve(int selectedSlot, String token) {
        if (slot != selectedSlot || token == null) return null;
        int index = tokens.indexOf(token);
        return index < 0 ? null : rows.get(index);
    }
}
