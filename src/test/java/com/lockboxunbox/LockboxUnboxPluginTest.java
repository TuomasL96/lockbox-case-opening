package com.lockboxunbox;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Developer launcher. Run with -Dlockbox.force=gold to force an Oathplate
 * result in Practice Spin.
 */
public class LockboxUnboxPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DevLockboxUnboxPlugin.class);
		RuneLite.main(args);
	}
}
