package com.lockboxunbox;

import java.util.Random;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;

/**
 * Developer-only plugin that adds test buttons to the Lockbox Unbox side panel,
 * for the inventory cover and the Oathplate reveal. It lives in src/test and
 * never ships. RuneLite only accepts direct subclasses of Plugin, so this is a
 * separate plugin rather than a subclass of LockboxUnboxPlugin.
 */
@PluginDescriptor(name = "Lockbox Unbox (dev)")
public class DevForceGoldPlugin extends Plugin
{
	private final Random random = new Random();
	private final JButton coverButton =
		button("Spin with inventory cover", plugin -> plugin.startPractice(DropTable.roll(random), true));
	private final JButton goldButton =
		button("Simulate Oathplate drop", plugin -> plugin.startPractice(DropTable.of(ItemID.OATHPLATE_HELM, 1), true));

	@Inject
	private PluginManager pluginManager;

	@Override
	protected void startUp()
	{
		addButtons();
	}

	@Override
	protected void shutDown()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (coverButton.getParent() != null)
			{
				JPanel buttons = (JPanel) coverButton.getParent();
				buttons.remove(coverButton);
				buttons.remove(goldButton);
				buttons.revalidate();
				buttons.repaint();
			}
		});
	}

	/**
	 * The main plugin may start after this one, and rebuilds its panel whenever
	 * it restarts, so the buttons are added again each time it does.
	 */
	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (event.isLoaded() && event.getPlugin() instanceof LockboxUnboxPlugin)
		{
			addButtons();
		}
	}

	private void addButtons()
	{
		SwingUtilities.invokeLater(() ->
		{
			LockboxUnboxPlugin plugin = mainPlugin();
			PracticeSpinPanel panel = plugin == null ? null : plugin.panel();
			if (panel != null && coverButton.getParent() != panel.buttonList())
			{
				panel.buttonList().add(coverButton);
				panel.buttonList().add(goldButton);
				panel.buttonList().revalidate();
			}
		});
	}

	private LockboxUnboxPlugin mainPlugin()
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin instanceof LockboxUnboxPlugin)
			{
				return (LockboxUnboxPlugin) plugin;
			}
		}
		return null;
	}

	private JButton button(String text, Consumer<LockboxUnboxPlugin> action)
	{
		JButton button = new JButton(text);
		button.setFocusable(false);
		button.addActionListener(event ->
		{
			LockboxUnboxPlugin plugin = mainPlugin();
			if (plugin != null)
			{
				action.accept(plugin);
			}
		});
		return button;
	}
}
