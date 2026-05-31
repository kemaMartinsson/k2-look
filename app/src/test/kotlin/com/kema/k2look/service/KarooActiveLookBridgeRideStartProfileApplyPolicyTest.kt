package com.kema.k2look.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KarooActiveLookBridgeRideStartProfileApplyPolicyTest {

    @Test
    fun `defer profile redraw while ride start countdown is pending`() {
        assertTrue(
            shouldDeferProfileRedrawForRideStartCountdown(
                pendingRideStartCountdown = true,
                rideStartCountdownActive = false
            )
        )
    }

    @Test
    fun `defer profile redraw while ride start countdown is active`() {
        assertTrue(
            shouldDeferProfileRedrawForRideStartCountdown(
                pendingRideStartCountdown = false,
                rideStartCountdownActive = true
            )
        )
    }

    @Test
    fun `allow profile redraw when no ride start countdown is pending or active`() {
        assertFalse(
            shouldDeferProfileRedrawForRideStartCountdown(
                pendingRideStartCountdown = false,
                rideStartCountdownActive = false
            )
        )
    }

    @Test
    fun `apply deferred profile redraw after save when countdown ends during save`() {
        assertTrue(
            shouldApplyDeferredProfileRedrawAfterSave(
                deferredBeforeSave = true,
                pendingRideStartCountdown = false,
                rideStartCountdownActive = false
            )
        )
    }

    @Test
    fun `keep deferring profile redraw after save while countdown is still pending`() {
        assertFalse(
            shouldApplyDeferredProfileRedrawAfterSave(
                deferredBeforeSave = true,
                pendingRideStartCountdown = true,
                rideStartCountdownActive = false
            )
        )
    }

    @Test
    fun `do not force post-save redraw when save was never deferred`() {
        assertFalse(
            shouldApplyDeferredProfileRedrawAfterSave(
                deferredBeforeSave = false,
                pendingRideStartCountdown = false,
                rideStartCountdownActive = false
            )
        )
    }

    @Test
    fun `defer battery redraw while ride start countdown is pending`() {
        assertFalse(
            shouldUpdateBatteryDisplayNow(
                pendingRideStartCountdown = true,
                rideStartCountdownActive = false
            )
        )
    }

    @Test
    fun `defer battery redraw while ride start countdown is active`() {
        assertFalse(
            shouldUpdateBatteryDisplayNow(
                pendingRideStartCountdown = false,
                rideStartCountdownActive = true
            )
        )
    }

    @Test
    fun `apply battery redraw immediately when countdown is inactive`() {
        assertTrue(
            shouldUpdateBatteryDisplayNow(
                pendingRideStartCountdown = false,
                rideStartCountdownActive = false
            )
        )
    }
}
