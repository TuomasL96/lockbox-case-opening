package com.lockboxunbox;

import java.awt.Color;

/**
 * Rarity tier of a lockbox reward, ordered from most to least likely.
 */
public enum Tier
{
	COMMON("Common", new Color(0x4B69FF)),
	UNCOMMON("Uncommon", new Color(0x8847FF)),
	RARE("Rare", new Color(0xD32CE6)),
	VERY_RARE("Very rare", new Color(0xEB4B4B)),
	LEGENDARY("Legendary", new Color(0xFFD700)),
	UNKNOWN("Unknown", new Color(0x9E9E9E));

	private final String label;
	private final Color color;

	Tier(String label, Color color)
	{
		this.label = label;
		this.color = color;
	}

	public String label()
	{
		return label;
	}

	public Color color()
	{
		return color;
	}
}
