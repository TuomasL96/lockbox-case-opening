package com.lockboxcaseopening;

import java.util.Map;
import java.util.Optional;

/**
 * Works out what a lockbox gave by comparing the inventory before and after.
 * Maps are item ID to total quantity.
 */
public final class LootDiff
{
	private LootDiff()
	{
	}

	public static int boxesConsumed(Map<Integer, Integer> before, Map<Integer, Integer> after)
	{
		return Math.max(0, count(before, DropTable.LOCKBOX_ID) - count(after, DropTable.LOCKBOX_ID));
	}

	/**
	 * Prefers a gain matching a drop table entry, so an unrelated change on the
	 * same tick can't be mistaken for the loot. Otherwise uses the largest gain.
	 */
	public static Optional<Loot> gained(Map<Integer, Integer> before, Map<Integer, Integer> after)
	{
		Loot largest = null;
		for (Map.Entry<Integer, Integer> item : after.entrySet())
		{
			int itemId = item.getKey();
			int gain = item.getValue() - count(before, itemId);
			if (itemId == DropTable.LOCKBOX_ID || gain <= 0)
			{
				continue;
			}
			if (DropTable.isEntry(itemId, gain))
			{
				return Optional.of(DropTable.of(itemId, gain));
			}
			if (largest == null || gain > largest.getQuantity())
			{
				largest = DropTable.of(itemId, gain);
			}
		}
		return Optional.ofNullable(largest);
	}

	private static int count(Map<Integer, Integer> items, int itemId)
	{
		return items.getOrDefault(itemId, 0);
	}
}
