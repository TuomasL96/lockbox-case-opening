<img src="icon.png" width="48" height="48" alt="">

# Lockbox Case Opening

A RuneLite plugin that turns opening a Forgotten lockbox into a case opening. Click **Open** and a reel spins in the middle of your screen, then lands on the loot you really received.

## How it works

- The reel starts on your click and slows down once the loot reaches your inventory, so it always shows your real result.
- Your inventory is covered until the reel stops, and the game's chatbox result popup is hidden until the panel closes, so the result isn't spoiled.
- The other tiles on the reel are random picks from the drop table, there for decoration.
- Tiles are coloured by drop chance, from blue through purple, pink and red.
- Oathplate pieces appear as a gold star and are revealed when the reel stops.
- Clicking outside the panel closes it straight away, and so does taking damage.
- Clicks on the panel and on the covered inventory are blocked, so you can't misclick an item you can't see.
- If the plugin can't tell what you received, the panel closes without showing a result. It never guesses.

| Tier | Items |
|---|---|
| Common | Oathplate shards, dragon darts |
| Uncommon | Dragon bolts (unf) |
| Rare | Dragon platelegs, dragon plateskirt |
| Very rare | Coins |
| Legendary | Oathplate helm, chest, legs |

## Settings

| Setting | Default | Notes |
|---|---|---|
| Spin duration | 6 seconds | 3 to 10 seconds |
| Reveal duration | 4 seconds | Oathplate stays until you click |
| Sound effects | On | Uses in-game UI sounds |

## Fair play

The plugin is cosmetic. It draws the reel and the inventory cover, hides the chatbox result popup while the panel is open and then shows it again, and blocks clicks on the panel and the covered inventory.

It adds no menu options, sends nothing to the game server, makes no network requests, and can't predict loot: it learns the result from your inventory, after the game has given it to you.

