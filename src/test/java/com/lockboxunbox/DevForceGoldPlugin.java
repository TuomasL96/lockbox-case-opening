package com.lockboxunbox;

import java.awt.GridLayout;
import java.util.Random;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JPanel;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

/**
 * Developer-only plugin with a side panel for testing the inventory cover and
 * the Oathplate reveal. It lives in src/test and never ships. RuneLite only
 * accepts direct subclasses of Plugin, so this is a separate plugin rather than
 * a subclass of LockboxUnboxPlugin.
 */
@PluginDescriptor(name = "Lockbox Unbox (dev)")
public class DevForceGoldPlugin extends Plugin
{
	private final Random random = new Random();

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ClientToolbar clientToolbar;

	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		PluginPanel panel = new PluginPanel()
		{
		};
		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));
		buttons.add(button("Spin with inventory cover", plugin -> plugin.startPractice(DropTable.roll(random), true)));
		buttons.add(button("Simulate Oathplate drop",
			plugin -> plugin.startPractice(DropTable.of(ItemID.OATHPLATE_HELM, 1), true)));
		panel.add(buttons);

		navigationButton = NavigationButton.builder()
			.tooltip("Lockbox Unbox (dev)")
			.icon(ImageUtil.loadImageResource(LockboxUnboxPlugin.class, "panel_icon.png"))
			.priority(11)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
		navigationButton = null;
	}

	private JButton button(String text, Consumer<LockboxUnboxPlugin> action)
	{
		JButton button = new JButton(text);
		button.setFocusable(false);
		button.addActionListener(event ->
		{
			for (Plugin plugin : pluginManager.getPlugins())
			{
				if (plugin instanceof LockboxUnboxPlugin)
				{
					action.accept((LockboxUnboxPlugin) plugin);
				}
			}
		});
		return button;
	}
}
