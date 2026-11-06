# Lockbox Unbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a RuneLite plugin that plays a centred case-opening reel for each Forgotten lockbox open, always landing on the real loot, plus a clearly labelled Practice Spin.

**Architecture:** Pure Java classes hold the logic (`DropTable`, `LootDiff`, `ReelAnimation`, `UnboxController`) and are unit tested. Thin RuneLite classes (overlays, mouse listener, side panel, plugin) feed them events and draw their state. Nothing touches game widgets or sends actions to the server.

**Tech Stack:** Java 11 (`options.release`), Gradle wrapper 8.10, RuneLite client `latest.release`, Lombok 1.18.30, JUnit 4.12.

**Spec:** `docs/superpowers/specs/2026-11-07-lockbox-unbox-design.md`

## Global Constraints

- Never add menu entries, call `client.menuAction`, or otherwise send actions to the server.
- Never hide, move or resize game widgets.
- No network calls, reflection or third-party dependencies beyond the example-plugin template.
- No Valve art, sounds or names. "CS2" must not appear in plugin metadata, UI text or settings.
- Licence BSD 2-Clause. `icon.png` no larger than 48x72 at the repo root.
- Display name `Lockbox Unbox`, author `Ihan itse`, package `com.lockboxunbox`.
- Lockbox item ID 30763.
- Exact banner text: `SIMULATION – no items awarded`.
- Work on branch `feature/lockbox-unbox`. Never commit to `master` directly.
- All code uses tabs, matching RuneLite style.

## Review Focus

1. Another inventory change lands before the lockbox is consumed (eating, dropping): the reel must keep waiting, not take that as the loot. Test in Task 4.
2. A close from a mouse click is queued, and a new open starts before it runs: the close must not kill the new animation. Test in Task 4.
3. A very small client window: the panel stays inside the canvas. Test in Task 5.
4. A press that closes the panel and was consumed: its release must also be consumed, or the game sees half a click. Test in Task 6.
5. A stack of several lockboxes, or loot that merges into an existing stack: the loot is the difference, not the total. Test in Task 2.

---

### Task 1: Project setup and drop table

**Files:**
- Create: `build.gradle`, `settings.gradle`, `runelite-plugin.properties`, `.gitignore`, `LICENSE`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
- Create: `src/main/java/com/lockboxunbox/Tier.java`
- Create: `src/main/java/com/lockboxunbox/Loot.java`
- Create: `src/main/java/com/lockboxunbox/DropTable.java`
- Test: `src/test/java/com/lockboxunbox/DropTableTest.java`

**Interfaces:**
- Produces:
  - `enum Tier { COMMON, UNCOMMON, RARE, VERY_RARE, LEGENDARY, UNKNOWN }` with `String label()` and `java.awt.Color color()`
  - Lombok `@Value class Loot { int itemId; int quantity; Tier tier; }`
  - `DropTable.LOCKBOX_ID` (`int`, 30763)
  - `static Loot DropTable.roll(java.util.Random random)`
  - `static Loot DropTable.of(int itemId, int quantity)`, tier from the item ID, `Tier.UNKNOWN` if absent
  - `static boolean DropTable.isEntry(int itemId, int quantity)`
  - `static int DropTable.totalWeight()`

Tier labels and colours:

| Tier | Label | Colour |
|---|---|---|
| COMMON | Common | `0x4B69FF` |
| UNCOMMON | Uncommon | `0x8847FF` |
| RARE | Rare | `0xD32CE6` |
| VERY_RARE | Very rare | `0xEB4B4B` |
| LEGENDARY | Legendary | `0xFFD700` |
| UNKNOWN | Unknown | `0x9E9E9E` |

- [ ] **Step 1: Create the branch and project files**

Run `git checkout -b feature/lockbox-unbox`. Copy `build.gradle`, `.gitignore` and `gradle-wrapper.properties` from `runelite/example-plugin@master`, with `pluginMainClass = 'com.lockboxunbox.LockboxUnboxPluginTest'` and `group = 'com.lockboxunbox'`. `settings.gradle`: `rootProject.name = 'lockbox-unbox'`. Generate the wrapper with the cached Gradle 8.10.2 at `~/.gradle/wrapper/dists/gradle-8.10.2-all/` (then keep the template's 8.10 properties). `runelite-plugin.properties`:

```properties
displayName=Lockbox Unbox
author=Ihan itse
description=Plays an unboxing reel when you open a Forgotten lockbox
tags=yama,lockbox,oathplate,unboxing,animation
plugins=com.lockboxunbox.LockboxUnboxPlugin
build=standard
```

`LICENSE`: BSD 2-Clause, copyright 2026 Ihan itse.

- [ ] **Step 2: Find the item IDs**

Run `./gradlew dependencies --configuration testCompileClasspath` so `latest.release` resolves, then use `javap -constants net.runelite.api.gameval.ItemID` on the resolved `runelite-api` jar from the Gradle cache. Confirm 30763 is the lockbox and record the IDs for Oathplate helm, chest, legs and shards, and Dragon bolts (unf), matching against the OSRS Wiki pages. Use the `ItemID` constants in code.

- [ ] **Step 3: Write the failing tests in `DropTableTest`**

```java
@Test public void weightsTotal403() { assertEquals(403, DropTable.totalWeight()); }
@Test public void shardsShareATierAcrossQuantities() {
	assertEquals(Tier.COMMON, DropTable.of(SHARDS, 14).getTier());
	assertEquals(Tier.COMMON, DropTable.of(SHARDS, 11).getTier());
}
@Test public void tiersMatchSpec() {
	assertEquals(Tier.COMMON, DropTable.of(DRAGON_DART, 125).getTier());
	assertEquals(Tier.UNCOMMON, DropTable.of(BOLTS_UNF, 125).getTier());
	assertEquals(Tier.RARE, DropTable.of(DRAGON_PLATELEGS, 1).getTier());
	assertEquals(Tier.RARE, DropTable.of(DRAGON_PLATESKIRT, 1).getTier());
	assertEquals(Tier.VERY_RARE, DropTable.of(COINS, 233333).getTier());
	assertEquals(Tier.LEGENDARY, DropTable.of(OATHPLATE_HELM, 1).getTier());
	assertEquals(Tier.LEGENDARY, DropTable.of(OATHPLATE_CHEST, 1).getTier());
	assertEquals(Tier.LEGENDARY, DropTable.of(OATHPLATE_LEGS, 1).getTier());
}
@Test public void unlistedItemIsUnknown() { assertEquals(Tier.UNKNOWN, DropTable.of(ABYSSAL_WHIP, 1).getTier()); }
@Test public void isEntryNeedsIdAndQuantity() {
	assertTrue(DropTable.isEntry(DRAGON_DART, 80));
	assertFalse(DropTable.isEntry(DRAGON_DART, 81));
}
@Test public void rollFollowsWeights() {
	// 403000 seeded rolls: Oathplate helm count within 700..1300, coins within 18000..22000.
}
```

- [ ] **Step 4: Run tests to verify they fail**

Run: `./gradlew test --tests '*DropTableTest'`
Expected: compile failure, `DropTable` not found.

- [ ] **Step 5: Implement `Tier`, `Loot`, `DropTable`**

The 11 entries, as item, quantity, weight: shards 14/60, shards 11/50, dart 125/60, dart 80/50, bolts 125/60, platelegs 1/50, plateskirt 1/50, coins 233333/20, helm 1/1, chest 1/1, legs 1/1. Tier by item as in the spec table.

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew test --tests '*DropTableTest'`
Expected: PASS.

- [ ] **Step 7: Commit**

`git add -A && git commit -m "feat: add project setup and lockbox drop table"`

---

### Task 2: Loot detection

**Files:**
- Create: `src/main/java/com/lockboxunbox/LootDiff.java`
- Test: `src/test/java/com/lockboxunbox/LootDiffTest.java`

**Interfaces:**
- Consumes: `DropTable.isEntry`, `DropTable.of`, `DropTable.LOCKBOX_ID`, `Loot`
- Produces:
  - `static boolean LootDiff.boxConsumed(Map<Integer,Integer> before, Map<Integer,Integer> after)`, true when the lockbox count fell by at least one
  - `static Optional<Loot> LootDiff.gained(Map<Integer,Integer> before, Map<Integer,Integer> after)`

Maps are item ID to summed quantity.

- [ ] **Step 1: Write the failing tests**

```java
@Test public void newItem() // before {30763:1}, after {DRAGON_DART:125} -> dart x125, COMMON
@Test public void existingStackUsesTheDifference() // before {30763:1, DRAGON_DART:500}, after {DRAGON_DART:625} -> x125
@Test public void stackOfBoxesCountsAsConsumed() // before {30763:5}, after {30763:4, COINS:233333}: consumed, coins x233333
@Test public void boxStillThereIsNotConsumed() // before {30763:1, SHARK:3}, after {30763:1, SHARK:2}: false
@Test public void tableMatchBeatsLargerUnrelatedGain() // after adds DRAGON_DART:80 and FEATHER:1000 -> dart x80
@Test public void unmatchedGainUsesLargestAndIsUnknown() // after adds FEATHER:1000 and BONES:1 -> feather, Tier.UNKNOWN
@Test public void nothingGainedIsEmpty() // before {30763:1}, after {} -> Optional.empty()
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests '*LootDiffTest'`
Expected: compile failure, `LootDiff` not found.

- [ ] **Step 3: Implement `LootDiff`**

Ignore the lockbox itself when looking for gains.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests '*LootDiffTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

`git add -A && git commit -m "feat: detect lockbox loot from inventory changes"`

---

### Task 3: Reel animation

**Files:**
- Create: `src/main/java/com/lockboxunbox/ReelAnimation.java`
- Test: `src/test/java/com/lockboxunbox/ReelAnimationTest.java`

**Interfaces:**
- Consumes: `DropTable.roll`, `Loot`, `Tier`
- Produces:
  - `enum ReelAnimation.Phase { SPINNING, SETTLING, REVEAL, DONE }`
  - `new ReelAnimation(Random random, long startNanos, int spinSeconds, int revealSeconds)`
  - `void setWinner(Loot winner, long nowNanos)`
  - `double position(long nowNanos)`, in tiles, tile `i` centred under the marker at `position == i`
  - `Loot tile(int index)`, stable once generated
  - `Phase phase(long nowNanos)`
  - `boolean coverVisible(long nowNanos)`, true in SPINNING and SETTLING
  - `boolean hasWinner()`, `Loot winner()`, `int winnerIndex()`
  - constants `SPIN_TILES_PER_SECOND = 24.0`, `MIN_SETTLE_SECONDS = 2.0`, `VISIBLE_HALF_TILES = 4`, `MAX_LAND_OFFSET = 0.35`

- [ ] **Step 1: Write the failing tests**

```java
@Test public void landsExactlyOnWinner() // for result times 0, 0.1, 0.6, 1.8, 5.0 s and several seeds: at settle end, Math.round(position) == winnerIndex, tile(winnerIndex) equals winner
@Test public void winnerIsOutOfViewWhenPlaced() // winnerIndex - position(at setWinner) > VISIBLE_HALF_TILES + 1, for each result time above
@Test public void speedIsContinuousAtTheSwitch() // numeric speed just before and just after setWinner differ by < 1%
@Test public void reelNeverMovesBackwards() // position strictly non-decreasing sampled every 1 ms
@Test public void totalDurationMatchesSpinSetting() // result at 0.6 s with spin 6: settle ends within 0.25 s of 6 s after start
@Test public void lateResultStillGetsASettle() // result at 5.5 s with spin 6: settle lasts at least 2.0 s
@Test public void spinsUntilAWinnerArrives() // no winner, 60 s in: phase SPINNING
@Test public void coverLiftsWhenReelStops() // coverVisible true in SETTLING, false in REVEAL
@Test public void revealEndsAfterRevealSeconds() // non-gold: DONE at settle end + revealSeconds
@Test public void goldNeverTimesOut() // LEGENDARY winner, an hour after settle end: phase REVEAL
@Test public void tilesAreStable() // tile(50) returns the same Loot on repeat calls
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests '*ReelAnimationTest'`
Expected: compile failure, `ReelAnimation` not found.

- [ ] **Step 3: Implement `ReelAnimation`**

Before a winner: `position = v * elapsed`, with `v = SPIN_TILES_PER_SECOND`. Settling uses ease-out cubic, whose starting speed is `3D/T`, so speed stays continuous when `T = 3D/v`:

```java
double wantedT = Math.max(spinSeconds - elapsedAtResult, MIN_SETTLE_SECONDS);
winnerIndex = (int) Math.ceil(startPos + v * wantedT / 3);
landAt = winnerIndex + offset; // offset uniform in +/- MAX_LAND_OFFSET
double D = landAt - startPos;
double T = 3 * D / v; // differs from wantedT by under 0.25 s
// position(s) = startPos + D * (1 - pow(1 - s, 3)), s = (now - resultNanos) / T, clamped to 0..1
```

Tiles generate lazily from `DropTable.roll` into a `HashMap`, and `winnerIndex` is overwritten with the winner.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests '*ReelAnimationTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

`git add -A && git commit -m "feat: add reel animation timing"`

---

### Task 4: Unbox controller

**Files:**
- Create: `src/main/java/com/lockboxunbox/UnboxController.java`
- Test: `src/test/java/com/lockboxunbox/UnboxControllerTest.java`

**Interfaces:**
- Consumes: `LootDiff`, `ReelAnimation`, `Loot`
- Produces:
  - `new UnboxController(Random random)`
  - `void openClicked(Map<Integer,Integer> before, long nowNanos, int spinSeconds, int revealSeconds)`
  - `void inventoryChanged(Map<Integer,Integer> after, long nowNanos)`
  - `void gameTick()`
  - `void startPractice(Loot loot, long nowNanos, int spinSeconds, int revealSeconds)`
  - `void update(long nowNanos)`, closes when phase is DONE
  - `void close()`, unconditional
  - `void close(ReelAnimation expected)`, closes only if `expected` is still current
  - `ReelAnimation animation()`, null when idle, backed by a `volatile` field
  - `boolean isPractice()`, `boolean isActive()`
  - constant `TIMEOUT_TICKS = 5`

- [ ] **Step 1: Write the failing tests**

```java
@Test public void openStartsSpinningWithoutAWinner()
@Test public void consumedBoxSetsTheWinner() // inventoryChanged with box gone and darts gained -> winner dart x125
@Test public void otherInventoryChangeKeepsWaiting() // SHARK count falls, box still there -> no winner, still active
@Test public void noBoxConsumedTimesOut() // 5 gameTick calls -> idle; 4 -> still active
@Test public void consumedWithNoGainCloses() // box gone, nothing gained -> idle, never a winner
@Test public void secondOpenDuringSpinIsIgnored() // same animation instance kept
@Test public void openDuringRevealStartsNew() // a new instance, in SPINNING
@Test public void realOpenReplacesPractice() // isPractice false afterwards
@Test public void practiceIgnoredWhileActive() // running animation unchanged
@Test public void practiceHasWinnerAtOnceAndNeverTimesOut() // 100 gameTick calls -> still active
@Test public void practiceIgnoresInventoryChanges() // winner unchanged after inventoryChanged
@Test public void staleCloseDoesNotKillNewAnimation() // keep A, close(), openClicked gives B, close(A) -> B still active
@Test public void updateClosesWhenDone()
@Test public void closeWorksInEveryPhase() // close() from SPINNING, SETTLING and a gold REVEAL -> idle, for real and practice
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests '*UnboxControllerTest'`
Expected: compile failure, `UnboxController` not found.

- [ ] **Step 3: Implement `UnboxController`**

Single class holding the current animation, `before` snapshot, `practice` flag and tick count. The tick count resets on each open.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests '*UnboxControllerTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

`git add -A && git commit -m "feat: add unbox session controller"`

---

### Task 5: Overlays

**Files:**
- Create: `src/main/java/com/lockboxunbox/UnboxOverlay.java`
- Create: `src/main/java/com/lockboxunbox/InventoryCoverOverlay.java`
- Create: `src/main/java/com/lockboxunbox/LockboxUnboxConfig.java`
- Test: `src/test/java/com/lockboxunbox/UnboxOverlayTest.java`

**Interfaces:**
- Consumes: `UnboxController`, `ReelAnimation`, `Loot`, `Tier`
- Produces:
  - `static Rectangle UnboxOverlay.panelBounds(int canvasWidth, int canvasHeight)`
  - `Rectangle UnboxOverlay.bounds()`, volatile, last drawn panel, null when idle
  - `Rectangle InventoryCoverOverlay.bounds()`, volatile, null when no cover drawn
  - `LockboxUnboxConfig` (group `lockboxunbox`): `int spinSeconds()` default 6, `@Range(min=3,max=10)`; `int revealSeconds()` default 4, `@Range(min=1,max=15)`; `boolean soundEffects()` default true

Both overlays use `OverlayPosition.DYNAMIC`, `OverlayLayer.ALWAYS_ON_TOP`, highest priority, and draw only while `controller.isActive()`.

Panel: width `min(720, canvas - 40)`, height `min(190, canvas - 40)`, centred. Tiles 104 wide with an 8 gap. Item images come from `ItemManager.getImage(id, quantity, quantity > 1)`, cached by item and quantity for the session.

- [ ] **Step 1: Write the failing tests**

```java
@Test public void panelIsCentred() // panelBounds(1000, 800) -> x 140, y 305, w 720, h 190
@Test public void panelFitsTinyClient() // panelBounds(300, 150) -> fully inside the canvas with a margin of 20
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests '*UnboxOverlayTest'`
Expected: compile failure, `UnboxOverlay` not found.

- [ ] **Step 3: Implement `UnboxOverlay`**

Each frame, call `controller.update(System.nanoTime())`, then draw: dark panel, title `FORGOTTEN LOCKBOX`, clipped reel, centre marker. Each tile gets its tier colour as a bar and soft background, and fades towards the edges. LEGENDARY tiles show a gold star in place of the item, except the winning tile from REVEAL on, which shows the item with a gold flash for the first 0.6 s. In REVEAL, draw `name x quantity · tier label`, using `ItemManager.getItemComposition` for the name and omitting ` x quantity` when it is 1. When `controller.isPractice()`, draw `SIMULATION – no items awarded` in a visible strip at the top of the panel, in every phase.

Sound, when `soundEffects()` is on: `client.playSoundEffect(SoundEffectID.UI_BOOP)` each time `floor(position)` changes, at most once per 60 ms. When the reel stops: `SoundEffectID.GE_ADD_OFFER_DINGALING`, or `SoundEffectID.GE_COIN_TINKLE` for LEGENDARY. Check these in the running client and swap if one sounds wrong.

- [ ] **Step 4: Implement `InventoryCoverOverlay`**

While `animation.coverVisible(now)` and not `controller.isPractice()`, fetch `InterfaceID.Inventory.ITEMS` and `InterfaceID.Bankside.ITEMS` with `client.getWidget`. Fill the bounds of each that is non-null and not hidden with opaque `0x1A1A1A`, centring the text `Unboxing…`. Store the union in `bounds()`. If neither is showing, or the cover isn't visible, set `bounds()` to null. Only read widgets; never change them.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests '*UnboxOverlayTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

`git add -A && git commit -m "feat: add unbox and inventory cover overlays"`

---

### Task 6: Click handling

**Files:**
- Create: `src/main/java/com/lockboxunbox/ClickAction.java`
- Create: `src/main/java/com/lockboxunbox/UnboxMouseListener.java`
- Test: `src/test/java/com/lockboxunbox/UnboxMouseListenerTest.java`

**Interfaces:**
- Consumes: `UnboxController`, `ReelAnimation.Phase`, `UnboxOverlay.bounds()`, `InventoryCoverOverlay.bounds()`
- Produces:
  - `enum ClickAction { CONSUME, CLOSE_AND_PASS, CLOSE_AND_CONSUME }`
  - `static ClickAction UnboxMouseListener.decide(Point point, Rectangle panel, Rectangle cover, ReelAnimation.Phase phase)`
  - `UnboxMouseListener extends MouseAdapter` (`net.runelite.client.input.MouseAdapter`)
  - `boolean UnboxMouseListener.awaitingRelease()`

- [ ] **Step 1: Write the failing tests**

```java
@Test public void insideDuringSpinConsumes() // SPINNING, inside panel -> CONSUME
@Test public void insideDuringRevealClosesAndConsumes() // REVEAL, inside -> CLOSE_AND_CONSUME
@Test public void outsideClosesAndPasses() // SETTLING, outside both -> CLOSE_AND_PASS
@Test public void overCoverClosesAndConsumes() // SPINNING, inside cover -> CLOSE_AND_CONSUME
@Test public void noCoverDuringReveal() // REVEAL, cover null -> CLOSE_AND_PASS
@Test public void releaseOfConsumedPressIsConsumed() // press with CLOSE_AND_CONSUME, then release and click events: both consumed, then awaitingRelease() is false
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests '*UnboxMouseListenerTest'`
Expected: compile failure, `UnboxMouseListener` not found.

- [ ] **Step 3: Implement `UnboxMouseListener`**

`mousePressed` applies to all buttons. Mouse callbacks run off the client thread, so read `controller.animation()` once, and close with `clientThread.invoke(() -> controller.close(animation))`. Consume with `event.consume()`, remembering it so `mouseReleased` and `mouseClicked` for that press are consumed too. Do nothing when no animation is current and no release is pending.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests '*UnboxMouseListenerTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

`git add -A && git commit -m "feat: close the animation on outside clicks"`

---

### Task 7: Plugin wiring and Practice Spin

**Files:**
- Create: `src/main/java/com/lockboxunbox/LockboxUnboxPlugin.java`
- Create: `src/main/java/com/lockboxunbox/PracticeSpinPanel.java`
- Create: `src/main/resources/com/lockboxunbox/panel_icon.png`
- Create: `src/test/java/com/lockboxunbox/LockboxUnboxPluginTest.java`
- Create: `src/test/java/com/lockboxunbox/DevLockboxUnboxPlugin.java`

**Interfaces:**
- Consumes: everything above
- Produces:
  - `protected Loot LockboxUnboxPlugin.rollPractice()`, returns `DropTable.roll(random)`
  - `void LockboxUnboxPlugin.requestPractice()`, safe from any thread
  - `new PracticeSpinPanel(Runnable onPractice)` with `void setLoggedIn(boolean)`

- [ ] **Step 1: Implement `LockboxUnboxPlugin`**

`@PluginDescriptor(name = "Lockbox Unbox", tags = {...})`. `startUp` adds both overlays and the navigation button; `shutDown` closes the controller and removes everything, including the mouse listener.

| Event | Action |
|---|---|
| `MenuOptionClicked` | if not consumed, `isItemOp()`, `getItemId() == DropTable.LOCKBOX_ID`, option `Open`: snapshot inventory, `openClicked`, register listener |
| `ItemContainerChanged` | if container ID is `InventoryID.INV`: `inventoryChanged` |
| `GameTick` | `gameTick()`, then unregister listener if idle and `!awaitingRelease()` |
| `HitsplatApplied` | target is the local player, `getAmount() > 0`, type not `HitsplatID.HEAL`: `close()` |
| `GameStateChanged` | anything but `LOGGED_IN`: `close()`, panel `setLoggedIn(false)`; `LOGGED_IN`: `setLoggedIn(true)` |

The snapshot sums `client.getItemContainer(InventoryID.INV)` by item ID, skipping empty slots. `requestPractice` uses `clientThread.invoke`, registers the listener, and calls `startPractice(rollPractice(), ...)`.

- [ ] **Step 2: Implement `PracticeSpinPanel`**

`extends PluginPanel`: a short explanation, a `Practice Spin` button (disabled when logged out), and a line reading `Visual only. No items are awarded.`

- [ ] **Step 3: Add the dev launcher**

`LockboxUnboxPluginTest.main` calls `ExternalPluginManager.loadBuiltin(DevLockboxUnboxPlugin.class)` then `RuneLite.main(args)`. `DevLockboxUnboxPlugin extends LockboxUnboxPlugin` with its own `@PluginDescriptor(name = "Lockbox Unbox (dev)")` and overrides `rollPractice()`: when system property `lockbox.force` is `gold`, return `DropTable.of(ItemID.OATHPLATE_HELM constant, 1)`, otherwise `super.rollPractice()`. This class lives only in `src/test`, so no force path ships.

- [ ] **Step 4: Verify the build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

`git add -A && git commit -m "feat: wire up plugin and Practice Spin"`

---

### Task 8: Hub readiness

**Files:**
- Create: `icon.png`
- Modify: `README.md`

**Interfaces:**
- Consumes: the finished plugin

- [ ] **Step 1: Create `icon.png`**

Original art derived from `assets/icon.svg`, at most 48x72.

- [ ] **Step 2: Rewrite `README.md`**

Cover what the plugin does, the settings, Practice Spin, and how the plugin stays client-side. Keep it free of CS2 branding.

- [ ] **Step 3: Check compliance**

Run:
`grep -rnEi 'menuAction|setHidden|setForcedPosition|createMenuEntry|java\.net|okhttp|setAccessible|CS2|counter.?strike' src/main runelite-plugin.properties README.md`
Expected: no matches.

Run: `file icon.png`
Expected: PNG, width at most 48, height at most 72.

- [ ] **Step 4: Smoke test the client**

Run `./gradlew run -Dlockbox.force=gold` with the JVM property forwarded in the `run` task. Confirm the client starts with the plugin enabled and without errors. Ask the user to run the design's manual checks: only the user can log in with a real lockbox.

- [ ] **Step 5: Commit**

`git add -A && git commit -m "docs: add icon and README for Hub readiness"`
