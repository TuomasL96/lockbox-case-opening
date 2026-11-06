package com.lockboxunbox;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Lockbox Unbox",
	description = "Plays an unboxing reel when you open a Forgotten lockbox",
	tags = {"yama", "lockbox", "oathplate", "unboxing", "animation"}
)
public class LockboxUnboxPlugin extends Plugin
{
	private static final String OPEN_OPTION = "Open";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private LockboxUnboxConfig config;

	@Inject
	private UnboxController controller;

	@Inject
	private UnboxOverlay unboxOverlay;

	@Inject
	private InventoryCoverOverlay coverOverlay;

	private UnboxMouseListener mouseListener;
	private volatile boolean mouseListenerRegistered;

	@Provides
	LockboxUnboxConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LockboxUnboxConfig.class);
	}

	/**
	 * True for states that mean the player left the world. Loading isn't one of
	 * them, because it also happens when walking between map regions.
	 */
	static boolean endsSession(GameState state)
	{
		return state != GameState.LOGGED_IN && state != GameState.LOADING;
	}

	@Override
	protected void startUp()
	{
		mouseListener = new UnboxMouseListener(controller, unboxOverlay::bounds, coverOverlay::bounds, clientThread::invoke);
		overlayManager.add(unboxOverlay);
		overlayManager.add(coverOverlay);
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(() -> controller.close());
		unregisterMouseListener();
		overlayManager.remove(unboxOverlay);
		overlayManager.remove(coverOverlay);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.isConsumed() || !event.isItemOp() || event.getItemId() != DropTable.LOCKBOX_ID
			|| !OPEN_OPTION.equals(event.getMenuOption()))
		{
			return;
		}
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return;
		}
		controller.openClicked(itemCounts(inventory), System.nanoTime(), config.spinSeconds(), config.revealSeconds());
		registerMouseListener();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			controller.inventoryChanged(itemCounts(event.getItemContainer()), System.nanoTime());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		controller.gameTick();
		if (!controller.isActive() && !mouseListener.awaitingRelease())
		{
			unregisterMouseListener();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Hitsplat hitsplat = event.getHitsplat();
		if (event.getActor() == client.getLocalPlayer() && hitsplat.getAmount() > 0
			&& hitsplat.getHitsplatType() != HitsplatID.HEAL)
		{
			controller.close();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (endsSession(event.getGameState()))
		{
			controller.close();
			unregisterMouseListener();
		}
	}

	private static Map<Integer, Integer> itemCounts(ItemContainer container)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() >= 0 && item.getQuantity() > 0)
			{
				counts.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		return counts;
	}

	private void registerMouseListener()
	{
		if (!mouseListenerRegistered && controller.isActive())
		{
			UnboxMouseListener.register(mouseManager, mouseListener);
			mouseListenerRegistered = true;
		}
	}

	private void unregisterMouseListener()
	{
		if (mouseListenerRegistered)
		{
			mouseManager.unregisterMouseListener(mouseListener);
			mouseListenerRegistered = false;
		}
	}
}
