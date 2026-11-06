package com.lockboxunbox;

import lombok.Value;

/**
 * One reward: an item, how many of it, and its rarity tier.
 */
@Value
public class Loot
{
	int itemId;
	int quantity;
	Tier tier;
}
