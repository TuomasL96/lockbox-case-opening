# Lockbox Unbox: design

> **Update:** Practice Spin (simulate mode) and the developer test buttons were
> removed from `main` so the plugin can go to the Plugin Hub. They live on the
> `developer` branch. Sections below that mention them describe that branch.

A RuneLite Plugin Hub plugin that turns opening a Forgotten lockbox (Yama loot)
into a horizontal case-opening reel, in the style of CS2, landing on the loot the
player really received.

## Intent

Opening a lockbox has no ceremony: the items appear in the inventory on the next
game tick. This plugin adds the hype moment. It must be fun to watch, must never
show a result that differs from the real loot, and must be acceptable for the
Plugin Hub.

## Goals

- A real "Open" on a Forgotten lockbox plays the reel, centred on screen.
- The reel always lands on the item and quantity actually received.
- Tiles are coloured by the item's overall rarity. Oathplate pieces are the
  "knives": a gold star tile, with the piece revealed at the end.
- The inventory is hidden from the moment of the click until the reel stops, so
  the loot cannot be spoiled.
- A click outside the panel stops the animation immediately.
- A Practice Spin (simulate mode) plays the same visuals with nothing awarded.

## Non-goals

- Other boxes, caches or chests.
- Loot tracking, history or statistics.
- Sharing, clips or anything else that leaves the client.

## Anti-goals (failure even if it works)

- The reel ever showing different loot from the inventory.
- A simulated spin being mistaken for a real drop.
- Any behaviour that sends actions to the server or predicts loot.
- Trapping the player: the animation must always be dismissable, and must never
  leave the inventory covered.

## Constraints

**Game behaviour (confirmed by the user).** There is no native animation. Loot
reaches the inventory on the next game tick after "Open". The item is Forgotten
lockbox, ID 30763 (per the OSRS Wiki).

**The plugin must not:**

- add menu entries, or call `client.menuAction` or anything else that sends an
  action to the server
- hide, move or resize game widgets
- add network calls or third-party dependencies
- use reflection
- include Valve art, sounds, names or the CS2 name in the plugin

**Hub requirements:** public repo, BSD 2-Clause licence,
`runelite-plugin.properties`, `icon.png` no larger than 48x72 at the repo root.

**Rule risk on simulate mode.** RuneLite's rejected list includes "Plugins that
simulate content for any purpose", with Quest Helper's Leviathan simulation as
the example. The user has decided to keep simulate mode and considers it within
the rules. Reviewers have the final say, so it stays self-contained and easy to
remove.

## Approaches

| Approach | Trade-off | Outcome |
|---|---|---|
| RuneLite overlay drawn with Graphics2D | Smooth, full control, leaves game interfaces untouched; all art is hand-drawn | **Chosen** (builder's pick, stated to the user) |
| Native game widgets | Looks built in, but choppier and closer to altering the interface | Rejected |
| RuneLite side panel | Simplest, but not centred | Rejected, misses the brief |

## Behaviour

### Real open

1. `MenuOptionClicked` fires for option "Open" on item 30763 in the inventory.
   The plugin snapshots the inventory and immediately shows the panel and the
   inventory cover. The reel spins at constant speed. The result is unknown at
   this point, and no tile placed in view can depend on it.
2. On `ItemContainerChanged` for the inventory, the plugin compares it with the
   snapshot. If the lockbox count fell by one, the result is the gained item and
   quantity (see Loot detection).
3. The winner is written into a tile beyond the right edge of the panel, and the
   reel eases out to a stop on that tile with no jump in speed. Total duration
   from click to stop is the configured spin time.
4. Reveal: the inventory cover lifts as soon as the reel stops. The winning tile
   glows, and the panel shows the item name, quantity and rarity label. It closes
   after the configured reveal time, or on a click.
5. Gold reveal: an Oathplate result adds a gold flash and its own sound, and has
   no reveal timeout. It stays until a click or damage closes it.

**Failure:** if no lockbox is consumed within 5 game ticks (for example "You can't
open that here"), the panel closes silently. No result is ever made up.

### Loot detection

Compare the snapshot with the current inventory, summing by item ID so that
stacks work (darts added to an existing stack count as the difference).

- Prefer a gained item whose ID and quantity match a table entry.
- Otherwise, if any item was gained, use the largest gain and show it on a grey
  tile. This covers future changes to the drop table, and the reel still shows
  the real loot.
- If the box was consumed but nothing was gained, close without a result.

### Stopping

- Any mouse button pressed in the game client outside the panel closes it at once,
  whatever the phase. The loot is already in the inventory, so nothing is lost.
  Losing window focus does not close it.
- That click still reaches the game, except over the inventory cover, where it
  is consumed so nobody clicks an item they can't see. The cover exists only
  during the spin, so during the reveal an inventory click works normally.
- Clicks inside the panel are consumed. During the reveal, one closes the panel.
- Any hitpoints loss closes the animation at once, in any phase, for real and
  practice spins alike. This has no setting.
- The animation also closes on logout, world hop, or plugin shutdown.
- Each repeat open takes two clicks: one to dismiss and one to open. There is no
  skip setting or queue for now.
- The mouse listener is registered only while the animation is active.

### Inventory cover

A solid dark panel is drawn over the visible inventory from the click until the
reel stops. It covers the normal inventory and the bank-side inventory when either is
showing, and draws nothing if neither is. A solid cover was chosen over a blur
because it fully hides the items. Clicks are handled as above; widgets are not
touched.

### Practice Spin

A RuneLite side panel with a "Practice Spin" button, enabled only while logged in.
It picks a result from the drop table by weighted random and plays the same
animation, with these differences:

- A permanent "SIMULATION – no items awarded" banner is drawn on the panel,
  including in the reveal.
- There is no chat message, no game interaction and no inventory cover, since
  nothing needs hiding.
- It is ignored while another animation is running, and a real open replaces a
  running practice spin.

The shipped plugin only ever uses the real odds. A forced result exists solely in
the developer test launcher under `src/test`, which is not part of the shipped
plugin, so the gold reveal can be tested without a lucky roll.

All of it lives in its own class, so removing it deletes one class and one call
site.

### Drop table and rarity

From the OSRS Wiki, out of 403. Colour is based on drop chance only, not value.
It uses the item's combined chance across all its quantities, so both shard rolls
share a colour.

| Tier (tile colour) | Item | Rolls | Combined |
|---|---|---|---|
| Common (blue) | Oathplate shards | 14 at 60, 11 at 50 | 110/403 |
| Common (blue) | Dragon dart | 125 at 60, 80 at 50 | 110/403 |
| Uncommon (purple) | Dragon bolts (unf) | 125 at 60 | 60/403 |
| Rare (pink) | Dragon platelegs | 1 at 50 | 50/403 |
| Rare (pink) | Dragon plateskirt | 1 at 50 | 50/403 |
| Very rare (red) | Coins | 233,333 at 20 | 20/403 |
| Legendary (gold) | Oathplate helm, chest, legs | 1 at 1 each | 1/403 each |

Filler tiles are drawn from the same weights, so gold stays as rare in the reel
as it is in the box. Only the winning tile is chosen by the real result, and no
near-misses are planted.

## Look

```
+----------------------------------------------------------+
|              FORGOTTEN LOCKBOX                           |
|  [ ] [ ] [ ] [ ] [ ] [ ] | [ ] [ ] [ ] [ ] [ ] [ ] [ ]   |
|                          |  (centre marker)              |
|              Dragon dart x125 · Common                   |
+----------------------------------------------------------+
```

Dark panel, centred on the game canvas and clamped to fit small clients. Tiles
show the item image, with the tier colour as a bar and a soft background glow.
Tiles fade towards the panel edges. Gold tiles show a star until the reveal.

## Architecture

- `LockboxUnboxPlugin`: event wiring, lifecycle
- `LockboxUnboxConfig`: settings
- `DropTable`: entries, tiers, weighted picks (pure Java)
- `LootDiff`: snapshot comparison (pure Java)
- `ReelAnimation`: phases, tile list, position from a supplied clock (pure Java)
- `UnboxOverlay`: draws the panel, reel and banner
- `InventoryCoverOverlay`: draws the cover
- `UnboxMouseListener`: outside-click handling
- `PracticeSpinPanel`: side panel and button

Game state is touched on the client thread. Mouse and Swing callbacks run on other
threads, so they hand work over through `clientThread` or read volatile bounds.
Animation runs on `System.nanoTime` rather than game ticks. Item images come from
`ItemManager`, so the plugin ships no item art.

## Settings

| Setting | Default |
|---|---|
| Spin duration | 6 seconds (range 3 to 10) |
| Reveal duration | 4 seconds (not used for gold) |
| Sound effects | On |

## Testing

Unit tests, written first, cover: table weights total 403; tier mapping; loot
diff cases (new item, existing stack, unmatched item, nothing gained); reel
landing exactly on the winner from any result time; speed continuity at the
switch to easing; timeout; the cover lifting when the reel stops; gold having no
reveal timeout; damage closing every phase.

Manual checks in a dev client, which only the user can do with a real lockbox:

- the reel matches the inventory for real opens, including into an existing stack
- full inventory
- opening with the bank open, if the game allows it
- "You can't open that here"
- fixed, resizable and stretched modes
- outside clicks during spin and reveal
- taking damage during spin and reveal
- logging out mid-spin

## Left to the builder

- Exact item IDs for Oathplate pieces and bolts, from the latest RuneLite API.
- Colour values and tile sizes.
- The easing curve.

## Calls made by the builder (not specified by the user)

- Rarity tier mapping and tier names, avoiding Valve's names.
- The gold star tile that reveals the piece at the end, matching how CS2 hides
  knives in the reel.
- A 4 second default reveal for results other than gold.
- A 5 tick timeout on failed opens.
- Unknown items are shown on a grey tile rather than skipped.
- Click handling as described in Stopping.
- Sound uses in-game UI sounds played locally, on by default, with a setting to
  turn it off.
- Display name "Lockbox Unbox", package `com.lockboxunbox`.
- Hub submission is undecided, so everything is built to Hub standard and the
  user decides after testing. The author name is "Ihan itse" (the user's choice).
- Compile against `latest.release` of RuneLite. The copy on this machine
  (1.11.6-SNAPSHOT) predates the lockbox.

## Known limits

- If the cursor rests where the new item lands, the game's own hover text could
  name it. Fixing this would mean changing menu entries, so it is left alone.
- If the game prints a chat message on opening, it could spoil the result. This
  is to be checked in the first real test.
