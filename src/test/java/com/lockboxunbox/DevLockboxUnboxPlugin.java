package com.lockboxunbox;

import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Developer-only plugin that can force an Oathplate result, so the gold reveal
 * can be tested. It lives in src/test and never ships.
 */
@PluginDescriptor(name = "Lockbox Unbox (dev)")
public class DevLockboxUnboxPlugin extends LockboxUnboxPlugin
{
	@Override
	protected Loot rollPractice()
	{
		if ("gold".equals(System.getProperty("lockbox.force")))
		{
			return DropTable.of(ItemID.OATHPLATE_HELM, 1);
		}
		return super.rollPractice();
	}
}
