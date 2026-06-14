package com.kema.k2look.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KarooActiveLookBridgeAutoSwitchPolicyTest {

    @Test
    fun `do not auto-switch when Karoo profile is null`() {
        assertFalse(
                shouldAttemptAutoSwitchForKarooProfile(
                        profileName = null,
                        lastKarooProfileName = "MTB"
                )
        )
    }

    @Test
    fun `do not auto-switch when Karoo profile name is unchanged`() {
        assertFalse(
                shouldAttemptAutoSwitchForKarooProfile(
                        profileName = "MTB",
                        lastKarooProfileName = "MTB"
                )
        )
    }

    @Test
    fun `auto-switch when Karoo profile changes`() {
        assertTrue(
                shouldAttemptAutoSwitchForKarooProfile(
                        profileName = "Gravel",
                        lastKarooProfileName = "MTB"
                )
        )
    }

    @Test
    fun `auto-switch on first observed Karoo profile`() {
        assertTrue(
                shouldAttemptAutoSwitchForKarooProfile(
                        profileName = "MTB",
                        lastKarooProfileName = null
                )
        )
    }
}
