package com.lockboxunbox;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Developer launcher. Also loads a dev-only plugin with a side panel for
 * testing the inventory cover and the Oathplate reveal.
 */
public class LockboxUnboxPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LockboxUnboxPlugin.class, DevForceGoldPlugin.class);
		RuneLite.main(args);
	}
}
