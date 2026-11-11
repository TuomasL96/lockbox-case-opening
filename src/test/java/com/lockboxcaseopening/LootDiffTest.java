package com.lockboxcaseopening;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class LootDiffTest
{
	private static final int BOX = DropTable.LOCKBOX_ID;

	private static Map<Integer, Integer> inv(int... pairs)
	{
		Map<Integer, Integer> map = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			map.put(pairs[i], pairs[i + 1]);
		}
		return map;
	}

	@Test
	public void newItem()
	{
		Loot loot = LootDiff.gained(inv(BOX, 1), inv(ItemID.DRAGON_DART, 125)).get();
		assertEquals(new Loot(ItemID.DRAGON_DART, 125, Tier.COMMON), loot);
	}

	@Test
	public void existingStackUsesTheDifference()
	{
		Loot loot = LootDiff.gained(inv(BOX, 1, ItemID.DRAGON_DART, 500), inv(ItemID.DRAGON_DART, 625)).get();
		assertEquals(125, loot.getQuantity());
	}

	@Test
	public void stackOfBoxesCountsAsConsumed()
	{
		Map<Integer, Integer> before = inv(BOX, 5);
		Map<Integer, Integer> after = inv(BOX, 4, ItemID.COINS, 233333);
		assertEquals(1, LootDiff.boxesConsumed(before, after));
		assertEquals(new Loot(ItemID.COINS, 233333, Tier.VERY_RARE), LootDiff.gained(before, after).get());
	}

	@Test
	public void boxStillThereIsNotConsumed()
	{
		assertEquals(0, LootDiff.boxesConsumed(inv(BOX, 1, ItemID.SHARK, 3), inv(BOX, 1, ItemID.SHARK, 2)));
	}

	@Test
	public void tableMatchBeatsLargerUnrelatedGain()
	{
		Loot loot = LootDiff.gained(inv(BOX, 1), inv(ItemID.DRAGON_DART, 80, ItemID.FEATHER, 1000)).get();
		assertEquals(new Loot(ItemID.DRAGON_DART, 80, Tier.COMMON), loot);
	}

	@Test
	public void unmatchedGainUsesLargestAndIsUnknown()
	{
		Loot loot = LootDiff.gained(inv(BOX, 1), inv(ItemID.FEATHER, 1000, ItemID.BONES, 1)).get();
		assertEquals(new Loot(ItemID.FEATHER, 1000, Tier.UNKNOWN), loot);
	}

	@Test
	public void nothingGainedIsEmpty()
	{
		assertEquals(Optional.empty(), LootDiff.gained(inv(BOX, 1), inv()));
	}
}
