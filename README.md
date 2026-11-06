<img src="icon.png" width="48" height="48" alt="">

# Lockbox Unbox

A RuneLite plugin that turns opening a Forgotten lockbox into an unboxing moment. Click **Open** and a reel spins in the middle of your screen, then lands on the loot you really received.

## How it works

- The reel starts on your click and slows down once the loot reaches your inventory, so it always shows your real result.
- Your inventory is covered until the reel stops, so the result isn't spoiled.
- Tiles are coloured by drop chance, from blue through purple, pink and red.
- Oathplate pieces appear as a gold star and are revealed when the reel stops.
- Clicking outside the panel closes it straight away, and so does taking damage.

| Tier | Items |
|---|---|
| Common | Oathplate shards, dragon darts |
| Uncommon | Dragon bolts (unf) |
| Rare | Dragon platelegs, dragon plateskirt |
| Very rare | Coins |
| Legendary | Oathplate helm, chest, legs |

## Practice Spin

The Lockbox Unbox side panel has a **Practice Spin** button that plays the reel using the real odds. It's visual only: no items are awarded, and a "SIMULATION – no items awarded" banner stays on the panel the whole time.

## Settings

| Setting | Default | Notes |
|---|---|---|
| Spin duration | 6 seconds | 3 to 10 seconds |
| Reveal duration | 4 seconds | Oathplate stays until you click |
| Sound effects | On | Uses in-game UI sounds |

## Fair play

The plugin only draws on your screen. It adds no menu options, sends nothing to the game server, makes no network requests, and can't predict loot: it learns the result from your inventory, after the game has given it to you.

## Development

Run the client with the plugin loaded:

```
./gradlew run
```

The developer build adds two test buttons to the Lockbox Unbox side panel, both with the inventory cover on:

- **Spin with inventory cover**: a spin using the real odds.
- **Simulate Oathplate drop**: a spin that lands on Oathplate.

Neither exists in the shipped plugin.

Run the tests with `./gradlew test`.
