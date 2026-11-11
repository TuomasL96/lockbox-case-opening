package com.lockboxcaseopening;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Developer launcher.
 */
public class LockboxCaseOpeningPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LockboxCaseOpeningPlugin.class);
		RuneLite.main(args);
	}
}
