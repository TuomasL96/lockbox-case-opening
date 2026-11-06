package com.lockboxunbox;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Developer launcher. Also loads a dev-only plugin that adds the ::unboxgold
 * command, which forces an Oathplate result.
 */
public class LockboxUnboxPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LockboxUnboxPlugin.class, DevForceGoldPlugin.class);
		RuneLite.main(args);
	}
}
