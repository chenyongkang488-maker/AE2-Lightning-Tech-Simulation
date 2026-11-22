package dev.overloadsim.compat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultiblockPreviewLayoutTest {
    @Test void framesTallStructureInWideCompactViewport() {
        // A 2x3x6 box fits in a radius-3.5 sphere. A 2:1 viewport needs
        // half-width 7.7 to keep that sphere visible with 10 percent margin.
        assertEquals(7.7f, MultiblockPreviewLayout.frameZoom(3.5f, 2, 3, 6, 180, 90), 0.0001f);
        assertEquals(18.5f, MultiblockPreviewLayout.frameZoom(18.5f, 2, 3, 6, 180, 90));
    }
    @Test void keepsDefaultZoomOnTallViewport() {
        assertEquals(9f, MultiblockPreviewLayout.frameZoom(9f, 2, 3, 6, 90, 180));
    }
    @Test void fitsBelowNavigationOnSmallWindow() {
        // 156 available pixels, 4px border on each side: the old 184px floor clips.
        assertEquals(148, MultiblockPreviewLayout.fitHeight(156, 0));
        assertEquals(144, MultiblockPreviewLayout.fitHeight(156, 2));
    }
    @Test void growsAndShrinksAgainWhenWindowChanges() {
        assertEquals(392, MultiblockPreviewLayout.fitHeight(400, 0));
        assertEquals(148, MultiblockPreviewLayout.fitHeight(156, 0));
    }
    @Test void keepsASceneAndOneMaterialRowAtMinimumSupportedHeight() {
        assertEquals(96, MultiblockPreviewLayout.fitHeight(60, 0));
        assertEquals(148, MultiblockPreviewLayout.fitHeight(156, -5));
    }
    @Test void scrollingReachesLastDetailWithoutExposingOffscreenHitTargets() {
        int scroll = MultiblockPreviewLayout.clampScroll(500, 80, 120);
        assertEquals(40, scroll);
        assertEquals(117, MultiblockPreviewLayout.contentY(48, 109, scroll));
        assertTrue(MultiblockPreviewLayout.contains(48, 128, 117));
        assertFalse(MultiblockPreviewLayout.contains(48, 128,
                MultiblockPreviewLayout.contentY(48, 2, scroll)));
        assertFalse(MultiblockPreviewLayout.contains(48, 128, 128));
        assertEquals(0, MultiblockPreviewLayout.clampScroll(-20, 80, 120));
        assertEquals(0, MultiblockPreviewLayout.clampScroll(40, 200, 120));
    }
}
