// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.hypot

class BubbleMotionTest {

    @Test
    fun coveringRadiusReachesTheFarthestCorner() {
        assertEquals(hypot(300f, 400f), BubbleMotion.coveringRadius(300f, 400f, 0f, 0f), 0.01f)
        assertEquals(hypot(150f, 200f), BubbleMotion.coveringRadius(300f, 400f, 150f, 200f), 0.01f)
    }

    @Test
    fun openingGoesFromTheIconToTheWholePopup() {
        assertEquals(20f, BubbleMotion.openRadius(0f, 20f, 500f), 0.01f)
        assertEquals(500f, BubbleMotion.openRadius(1f, 20f, 500f), 0.01f)
    }

    @Test
    fun theSpringMayOvershootButNeverGoesNegative() {
        assertTrue(BubbleMotion.openRadius(1.1f, 20f, 500f) > 500f)
        assertEquals(0f, BubbleMotion.openRadius(-1f, 20f, 500f), 0.01f)
    }

    @Test
    fun gatheringEndsInACircleThatFitsThePopup() {
        val round = BubbleMotion.roundRadius(300f, 800f)
        assertTrue(round < 150f)
        assertEquals(500f, BubbleMotion.gatherRadius(0f, 500f, round), 0.01f)
        assertEquals(round, BubbleMotion.gatherRadius(1f, 500f, round), 0.01f)
    }

    @Test
    fun theHoleSwallowsTheWholeBubbleBeforeTheDropletsLand() {
        assertEquals(0f, BubbleMotion.holeRadius(0f, 100f), 0.01f)
        assertTrue(BubbleMotion.holeRadius(0.45f, 100f) >= 200f)
        assertFalse(BubbleMotion.filmGone(0.2f))
        assertTrue(BubbleMotion.filmGone(0.5f))
    }

    @Test
    fun dropletsAreTheSameForTheSameSeed() {
        assertEquals(BubbleMotion.droplets(7, 12), BubbleMotion.droplets(7, 12))
        assertEquals(12, BubbleMotion.droplets(7, 12).size)
    }

    @Test
    fun dropletsNearThePunctureLeaveFirst() {
        val near = BubbleMotion.angleFromPuncture(BubbleMotion.PUNCTURE_ANGLE + 0.1f)
        val far = BubbleMotion.angleFromPuncture(BubbleMotion.PUNCTURE_ANGLE + PI.toFloat())
        assertEquals(0.1f, near, 0.01f)
        assertEquals(PI.toFloat(), far, 0.01f)
    }

    @Test
    fun aDropletIsHiddenUntilItsTurnAndFadesAtTheEnd() {
        val droplet = BubbleMotion.Droplet(0f, 1f, 1f, 0.3f)
        assertEquals(0f, BubbleMotion.dropletAt(droplet, 0.1f, 100f, 4f).alpha, 0.01f)
        val flying = BubbleMotion.dropletAt(droplet, 0.6f, 100f, 4f)
        assertTrue(flying.alpha > 0f)
        assertTrue(flying.x > 100f)
        assertEquals(0f, BubbleMotion.dropletAt(droplet, 1f, 100f, 4f).alpha, 0.01f)
    }
}
