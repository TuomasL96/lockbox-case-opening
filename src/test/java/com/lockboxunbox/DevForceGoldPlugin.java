package com.lockboxunbox;

import javax.inject.Inject;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

/**
 * Developer-only plugin. Typing ::unboxgold plays a Practice Spin that lands on
 * Oathplate, so the gold reveal can be tested. It lives in src/test and never
 * ships. RuneLite only accepts direct subclasses of Plugin, so this is a
 * separate plugin rather than a subclass of LockboxUnboxPlugin.
 */
@PluginDescriptor(name = "Lockbox Unbox (dev)")
public class DevForceGoldPlugin extends Plugin
{
	@Inject
	private PluginManager pluginManager;

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!"unboxgold".equals(event.getCommand()))
		{
			return;
		}
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin instanceof LockboxUnboxPlugin)
			{
				((LockboxUnboxPlugin) plugin).startPractice(DropTable.of(ItemID.OATHPLATE_HELM, 1));
			}
		}
	}
}
