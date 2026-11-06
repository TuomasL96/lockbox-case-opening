package com.lockboxunbox;

import java.util.Random;
import net.runelite.api.gameval.ItemID;

/**
 * The Forgotten lockbox drop table, from the OSRS Wiki. Weights are out of 403.
 * An item's tier comes from its combined chance across all of its quantities.
 */
public final class DropTable
{
	public static final int LOCKBOX_ID = ItemID.FORGOTTEN_LOCKBOX;

	private static final class Entry
	{
		private final Loot loot;
		private final int weight;

		private Entry(int itemId, int quantity, int weight, Tier tier)
		{
			this.loot = new Loot(itemId, quantity, tier);
			this.weight = weight;
		}
	}

	private static final Entry[] ENTRIES = {
		new Entry(ItemID.OATHPLATE_SHARDS, 14, 60, Tier.COMMON),
		new Entry(ItemID.OATHPLATE_SHARDS, 11, 50, Tier.COMMON),
		new Entry(ItemID.DRAGON_DART, 125, 60, Tier.COMMON),
		new Entry(ItemID.DRAGON_DART, 80, 50, Tier.COMMON),
		new Entry(ItemID.DRAGON_BOLTS_UNFEATHERED, 125, 60, Tier.UNCOMMON),
		new Entry(ItemID.DRAGON_PLATELEGS, 1, 50, Tier.RARE),
		new Entry(ItemID.DRAGON_PLATESKIRT, 1, 50, Tier.RARE),
		new Entry(ItemID.COINS, 233333, 20, Tier.VERY_RARE),
		new Entry(ItemID.OATHPLATE_HELM, 1, 1, Tier.LEGENDARY),
		new Entry(ItemID.OATHPLATE_CHEST, 1, 1, Tier.LEGENDARY),
		new Entry(ItemID.OATHPLATE_LEGS, 1, 1, Tier.LEGENDARY),
	};

	private DropTable()
	{
	}

	public static int totalWeight()
	{
		int total = 0;
		for (Entry entry : ENTRIES)
		{
			total += entry.weight;
		}
		return total;
	}

	/**
	 * Picks a reward using the real odds.
	 */
	public static Loot roll(Random random)
	{
		int remaining = random.nextInt(totalWeight());
		for (Entry entry : ENTRIES)
		{
			remaining -= entry.weight;
			if (remaining < 0)
			{
				return entry.loot;
			}
		}
		throw new IllegalStateException("roll fell outside the drop table");
	}

	/**
	 * Builds a reward for any item, using Tier.UNKNOWN if it isn't in the table.
	 */
	public static Loot of(int itemId, int quantity)
	{
		for (Entry entry : ENTRIES)
		{
			if (entry.loot.getItemId() == itemId)
			{
				return new Loot(itemId, quantity, entry.loot.getTier());
			}
		}
		return new Loot(itemId, quantity, Tier.UNKNOWN);
	}

	public static boolean isEntry(int itemId, int quantity)
	{
		for (Entry entry : ENTRIES)
		{
			if (entry.loot.getItemId() == itemId && entry.loot.getQuantity() == quantity)
			{
				return true;
			}
		}
		return false;
	}
}
