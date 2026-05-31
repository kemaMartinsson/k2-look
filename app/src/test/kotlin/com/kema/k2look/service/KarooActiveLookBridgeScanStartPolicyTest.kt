package com.kema.k2look.service

import org.junit.Assert.assertEquals
import org.junit.Test

class KarooActiveLookBridgeScanStartPolicyTest {

    @Test
    fun `when karoo not connected and no pending scan then wait for karoo`() {
        assertEquals(
                ScanStartAction.WAIT_FOR_KAROO,
                resolveScanStartAction(
                        karooConnected = false,
                        pendingScanUntilKarooReady = false,
                        karooConnecting = false
                )
        )
    }

    @Test
    fun `when karoo connecting and scan already pending then no-op`() {
        assertEquals(
                ScanStartAction.NO_OP_ALREADY_PENDING,
                resolveScanStartAction(
                        karooConnected = false,
                        pendingScanUntilKarooReady = true,
                        karooConnecting = true
                )
        )
    }

    @Test
    fun `when karoo disconnected and scan already pending then keep waiting instead of no-op`() {
        assertEquals(
                ScanStartAction.WAIT_FOR_KAROO,
                resolveScanStartAction(
                        karooConnected = false,
                        pendingScanUntilKarooReady = true,
                        karooConnecting = false
                )
        )
    }

    @Test
    fun `when karoo connected then start now`() {
        assertEquals(
                ScanStartAction.START_NOW,
                resolveScanStartAction(
                        karooConnected = true,
                        pendingScanUntilKarooReady = false,
                        karooConnecting = false
                )
        )
    }
}
