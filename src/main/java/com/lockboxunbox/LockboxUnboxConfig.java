package com.lockboxunbox;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("lockboxunbox")
public interface LockboxUnboxConfig extends Config
{
	@Range(min = 3, max = 10)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "spinSeconds",
		name = "Spin duration",
		description = "How long the reel spins before it stops",
		position = 1
	)
	default int spinSeconds()
	{
		return 6;
	}

	@Range(min = 1, max = 15)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "revealSeconds",
		name = "Reveal duration",
		description = "How long the result stays up. Oathplate stays until you click",
		position = 2
	)
	default int revealSeconds()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "soundEffects",
		name = "Sound effects",
		description = "Play in-game UI sounds during the animation",
		position = 3
	)
	default boolean soundEffects()
	{
		return true;
	}
}
