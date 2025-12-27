# Automatic Profile Switching - Analysis & Implementation

## Discovery Summary

**Question**: Can K2Look detect the active Karoo profile and automatically switch datafield profiles
to match?

**Answer**: ✅ **YES!** The Karoo Extension SDK provides full support for this feature.

---

## Analysis Results

### Karoo System Service Capabilities

The `KarooSystemService` in the Karoo Extension SDK (v1.1.5+) provides an `ActiveRideProfile` event
that:

1. **Monitors Profile Changes**: Receives updates when user scrolls to different profile on Karoo
   launcher
2. **Provides Profile Details**: Includes profile ID, name, pages, activity type, routing preference
3. **Updates Before Ride**: Profile change takes effect when scrolled to, even before ride starts
4. **Real-time Notifications**: K2Look receives immediate notification of profile changes

### Key API Components

#### RideProfile Model

```kotlin
@Serializable
data class RideProfile(
    val id: String,           // Unique profile ID
    val name: String,         // Profile name (e.g., "Road Bike", "Gravel")
    val pages: List<Page>,
    val indoor: Boolean,
    val defaultActivityType: String,
    val routingPreference: String
)
```

#### ActiveRideProfile Event

```kotlin
@Serializable
data class ActiveRideProfile(
    val profile: RideProfile  // Current active profile
) : KarooEvent()
```

#### Consumer Registration

```kotlin
karooSystem.addConsumer { event: ActiveRideProfile ->
    // Triggered when profile changes on Karoo launcher
    val profileName = event.profile.name
    // Auto-switch K2Look profile to match
}
```

---

## Implementation

### 1. KarooDataService Updates

Added `ActiveRideProfile` monitoring to `KarooDataService.kt`:

```kotlin
// Import
import io.hammerhead.karooext.models.ActiveRideProfile
import io.hammerhead.karooext.models.RideProfile

// StateFlow for active profile
private val _activeRideProfile = MutableStateFlow<RideProfile?>(null)
val activeRideProfile: StateFlow<RideProfile?> = _activeRideProfile.asStateFlow()

// Consumer registration in registerConsumers()
val profileId = karooSystem.addConsumer(
    onError = { error ->
        Log.e(TAG, "ActiveRideProfile consumer error: $error")
    }
) { event: ActiveRideProfile ->
    Log.i(TAG, "Active ride profile changed: ${event.profile.name}")
    _activeRideProfile.value = event.profile
}
consumerIds.add(profileId)
```

**Status**: ✅ Implemented in KarooDataService.kt

### 2. ViewModel Auto-Switching Logic

Added to `LayoutBuilderViewModel` in implementation plan:

```kotlin
// Auto-switch toggle
private val _autoSwitchEnabled = MutableStateFlow(true)
val autoSwitchEnabled: StateFlow<Boolean> = _autoSwitchEnabled.asStateFlow()

fun setAutoSwitch(enabled: Boolean) {
    _autoSwitchEnabled.value = enabled
}

// Monitor Karoo profile and auto-switch
fun observeKarooProfile(karooDataService: KarooDataService) {
    viewModelScope.launch {
        karooDataService.activeRideProfile.collect { karooProfile ->
            if (karooProfile != null && _autoSwitchEnabled.value) {
                // Find matching K2Look profile by name (case-insensitive)
                val matchingProfile = _profiles.value.find {
                    it.name.equals(karooProfile.name, ignoreCase = true)
                }

                if (matchingProfile != null) {
                    Log.i(TAG, "Auto-switching to '${matchingProfile.name}'")
                    _activeProfile.value = matchingProfile
                }
            }
        }
    }
}
```

**Status**: ✅ Documented in implementation plan

### 3. User Experience Flow

```
┌─────────────────────────────────────────────────────────┐
│ User Action: Scroll to "Road Bike" on Karoo Launcher   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Karoo System: Sends ActiveRideProfile event            │
│               profile.name = "Road Bike"                │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ KarooDataService: Updates activeRideProfile StateFlow  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ ViewModel: Observes profile change                     │
│            Searches for K2Look profile "Road Bike"     │
│            Case-insensitive match found!                │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ ViewModel: Auto-switches to matching profile           │
│            Updates _activeProfile StateFlow             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ UI: Recomposes with new profile                        │
│     Glasses display updates with new metrics           │
└─────────────────────────────────────────────────────────┘
```

---

## Features

### Automatic Profile Switching

✅ **Seamless Integration**: One profile switch updates both Karoo and glasses  
✅ **Name-Based Matching**: K2Look profiles match Karoo profiles by name  
✅ **Case-Insensitive**: "Road Bike" matches "road bike" or "ROAD BIKE"  
✅ **Instant Updates**: Display updates as soon as profile is scrolled to  
✅ **User Control**: Can be toggled on/off in settings  
✅ **Manual Override**: User can always manually select profile  
✅ **Fallback**: Keeps current profile if no match found

### Usage Example

**Setup**:

1. Create Karoo profiles: "Road Bike", "Gravel", "Indoor Trainer"
2. Create matching K2Look profiles with same names
3. Configure each K2Look profile with appropriate metrics:
    - **Road Bike**: Speed, Power, Heart Rate
    - **Gravel**: Speed, Heart Rate, Distance
    - **Indoor Trainer**: Power 3s, Cadence, HR Zone

**During Ride**:

- Scroll to "Road Bike" on Karoo → Glasses show speed/power/HR
- Switch to "Gravel" profile → Glasses auto-update to speed/HR/distance
- Switch to "Indoor Trainer" → Glasses show power/cadence/zones

**No manual intervention needed in K2Look app!**

---

## Implementation Status

### Completed ✅

1. **KarooDataService.kt**:
    - ✅ Added `ActiveRideProfile` import
    - ✅ Added `activeRideProfile` StateFlow
    - ✅ Registered ActiveRideProfile consumer
    - ✅ Profile change logging

2. **Implementation Plan**:
    - ✅ Documented auto-switching in Section 2.2.1
    - ✅ Added ViewModel auto-switch logic in Section 4.4
    - ✅ Updated Phase 5 tasks
    - ✅ Added manual testing checklist
    - ✅ Updated success criteria
    - ✅ Updated summary with seamless UX description

### To Be Implemented 📋

1. **ViewModel** (Phase 5):
    - [ ] Add `autoSwitchEnabled` StateFlow
    - [ ] Implement `setAutoSwitch()` method
    - [ ] Implement `observeKarooProfile()` method
    - [ ] Connect to KarooDataService on initialization

2. **UI** (Phase 2):
    - [ ] Add auto-switch toggle in Settings tab
    - [ ] Show current Karoo profile name in UI (optional)
    - [ ] Visual indicator when auto-switch happens
    - [ ] Toast notification on profile switch (optional)

3. **Settings** (Phase 6):
    - [ ] Persist auto-switch preference
    - [ ] Add help text explaining feature
    - [ ] Profile naming best practices guide

---

## Testing Checklist

### Unit Tests

- [ ] Test profile name matching (exact match)
- [ ] Test case-insensitive matching
- [ ] Test no match scenario (should keep current profile)
- [ ] Test auto-switch disabled (should not switch)
- [ ] Test multiple profiles with similar names

### Integration Tests

- [ ] Mock ActiveRideProfile events
- [ ] Verify StateFlow updates
- [ ] Verify ViewModel switches profiles
- [ ] Verify glasses display updates

### Manual Tests

- [ ] Create matching profiles on Karoo and K2Look
- [ ] Switch profiles on Karoo launcher
- [ ] Verify K2Look auto-switches
- [ ] Verify glasses display updates
- [ ] Test with auto-switch disabled
- [ ] Test with non-matching profile name
- [ ] Test case variations ("road bike" vs "Road Bike")

---

## Benefits

### For Users

1. **Simplified Workflow**: One action (scroll profile on Karoo) updates everything
2. **Consistent Experience**: Same profile names across Karoo and glasses
3. **No App Switching**: Don't need to open K2Look app during ride
4. **Smart Defaults**: Automatically shows relevant metrics for each bike/scenario
5. **Flexible Control**: Can disable auto-switch for manual control

### For Development

1. **Leverages Existing SDK**: Uses built-in `ActiveRideProfile` event
2. **Simple Implementation**: Just observe StateFlow and match by name
3. **No Additional Services**: No background service or polling needed
4. **Reliable**: Karoo System Service handles all profile tracking
5. **Future-Proof**: SDK maintained by SRAM/Hammerhead

---

## Profile Naming Best Practices

### Recommended Naming Convention

**Use descriptive, consistent names across Karoo and K2Look**:

✅ **Good Examples**:

- "Road Bike" (both Karoo and K2Look)
- "Gravel Bike" (both Karoo and K2Look)
- "Indoor Trainer" (both Karoo and K2Look)
- "MTB" (both Karoo and K2Look)

❌ **Avoid**:

- Different names: Karoo "Road", K2Look "Road Bike" (won't match)
- Special characters that might not match
- Extremely long names
- Names with leading/trailing spaces

### User Guidance

When creating profiles, suggest users:

1. Match K2Look profile names to their Karoo profile names
2. Use simple, clear names
3. Test auto-switching after creating profiles
4. Can disable auto-switch if they prefer manual control

---

## Documentation Updates

### Files Modified

1. **`app/src/main/kotlin/com/kema/k2look/service/KarooDataService.kt`**
    - Added imports for `ActiveRideProfile` and `RideProfile`
    - Added `_activeRideProfile` and `activeRideProfile` StateFlow
    - Registered `ActiveRideProfile` consumer in `registerConsumers()`

2. **`docs/DataField-Builder-Implementation-Plan.md`**
    - Section 2.2.1: NEW - Automatic Profile Switching explanation
    - Section 4.4: Added auto-switch methods to ViewModel
    - Section 5: Updated Phase 5 with auto-switch tasks
    - Section 8.3: Added automatic switching test checklist
    - Section 10: Updated success criteria with auto-switch features
    - Summary: Highlighted seamless UX with auto-switching

### Files Created

3. **`docs/Automatic-Profile-Switching-Analysis.md`** (this file)
    - Complete analysis of SDK capabilities
    - Implementation details
    - Testing checklist
    - User guidance

---

## Timeline Impact

**No impact to overall timeline** - this feature adds minimal complexity:

- **Phase 1**: No change (data models already support profiles)
- **Phase 2**: Add auto-switch toggle UI (~2 hours)
- **Phase 5**: Implement auto-switch logic (~4 hours)
- **Phase 6**: Persist auto-switch preference (~1 hour)
- **Phase 7**: Testing (~2 hours)

**Total additional time**: ~9 hours (approximately 1 day)

This is well within the buffer time already allocated for polish and testing.

---

## Conclusion

✅ **K2Look CAN and WILL automatically switch datafield profiles based on active Karoo profile**

This feature provides a seamless user experience where selecting a profile on the Karoo launcher
automatically updates the glasses display with the appropriate metrics for that bike/scenario. The
implementation leverages the Karoo Extension SDK's built-in profile monitoring and requires minimal
code.

**Implementation**:

- ✅ Backend complete (KarooDataService monitoring added)
- 📋 Frontend pending (ViewModel logic + UI toggle)
- 📋 Testing pending

**Next Steps**:

1. Complete Phase 1-4 (data models, UI, layout engine, integration)
2. Implement auto-switch ViewModel logic in Phase 5
3. Add settings toggle in Phase 6
4. Test with real profiles on Karoo 2 in Phase 7

**Expected Result**: Users will love the seamless integration! 🎉

