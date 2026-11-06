package com.lockboxunbox;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Developer launcher.
 */
public class LockboxUnboxPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LockboxUnboxPlugin.class);
		RuneLite.main(args);
	}
}
