package com.lockboxunbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class DropTableTest
{
	@Test
	public void lockboxIdMatchesTheGame()
	{
		assertEquals(30763, DropTable.LOCKBOX_ID);
		assertEquals(ItemID.FORGOTTEN_LOCKBOX, DropTable.LOCKBOX_ID);
	}

	@Test
	public void weightsTotal403()
	{
		assertEquals(403, DropTable.totalWeight());
	}

	@Test
	public void shardsShareATierAcrossQuantities()
	{
		assertEquals(Tier.COMMON, DropTable.of(ItemID.OATHPLATE_SHARDS, 14).getTier());
		assertEquals(Tier.COMMON, DropTable.of(ItemID.OATHPLATE_SHARDS, 11).getTier());
	}

	@Test
	public void tiersMatchSpec()
	{
		assertEquals(Tier.COMMON, DropTable.of(ItemID.DRAGON_DART, 125).getTier());
		assertEquals(Tier.UNCOMMON, DropTable.of(ItemID.DRAGON_BOLTS_UNFEATHERED, 125).getTier());
		assertEquals(Tier.RARE, DropTable.of(ItemID.DRAGON_PLATELEGS, 1).getTier());
		assertEquals(Tier.RARE, DropTable.of(ItemID.DRAGON_PLATESKIRT, 1).getTier());
		assertEquals(Tier.VERY_RARE, DropTable.of(ItemID.COINS, 233333).getTier());
		assertEquals(Tier.LEGENDARY, DropTable.of(ItemID.OATHPLATE_HELM, 1).getTier());
		assertEquals(Tier.LEGENDARY, DropTable.of(ItemID.OATHPLATE_CHEST, 1).getTier());
		assertEquals(Tier.LEGENDARY, DropTable.of(ItemID.OATHPLATE_LEGS, 1).getTier());
	}

	@Test
	public void unlistedItemIsUnknown()
	{
		assertEquals(Tier.UNKNOWN, DropTable.of(ItemID.ABYSSAL_WHIP, 1).getTier());
	}

	@Test
	public void isEntryNeedsIdAndQuantity()
	{
		assertTrue(DropTable.isEntry(ItemID.DRAGON_DART, 80));
		assertFalse(DropTable.isEntry(ItemID.DRAGON_DART, 81));
		assertFalse(DropTable.isEntry(ItemID.ABYSSAL_WHIP, 1));
	}

	@Test
	public void rollFollowsWeights()
	{
		Random random = new Random(42);
		int helms = 0;
		int coins = 0;
		for (int i = 0; i < 403000; i++)
		{
			Loot loot = DropTable.roll(random);
			if (loot.getItemId() == ItemID.OATHPLATE_HELM)
			{
				helms++;
			}
			else if (loot.getItemId() == ItemID.COINS)
			{
				coins++;
			}
		}
		assertTrue("helms=" + helms, helms >= 700 && helms <= 1300);
		assertTrue("coins=" + coins, coins >= 18000 && coins <= 22000);
	}
}
