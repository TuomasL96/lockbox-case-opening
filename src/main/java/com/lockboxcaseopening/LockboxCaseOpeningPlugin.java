package com.lockboxcaseopening;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Lockbox Case Opening",
	description = "Plays a case opening reel when you open a Forgotten lockbox",
	tags = {"yama", "lockbox", "forgotten", "oathplate", "case opening", "reel", "unboxing", "animation"}
)
public class LockboxCaseOpeningPlugin extends Plugin
{
	private static final String OPEN_OPTION = "Open";

	private static final int[] RESULT_POPUPS = {
		InterfaceID.Objectbox.UNIVERSE,
		InterfaceID.ObjectboxDouble.UNIVERSE,
		InterfaceID.Messagebox.UNIVERSE,
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private LockboxCaseOpeningConfig config;

	@Inject
	private UnboxController controller;

	@Inject
	private UnboxOverlay unboxOverlay;

	@Inject
	private InventoryCoverOverlay coverOverlay;

	// Only touched on the client thread.
	private final List<Widget> hiddenPopups = new ArrayList<>();

	private UnboxMouseListener mouseListener;
	private volatile boolean mouseListenerRegistered;

	@Provides
	LockboxCaseOpeningConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LockboxCaseOpeningConfig.class);
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
		clientThread.invoke(() ->
		{
			controller.close();
			restorePopups();
		});
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

	/**
	 * Hides the game's own result popup in the chatbox while the reel runs, so
	 * it can't show the loot early, and brings it back once the reel is gone.
	 */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (controller.isActive())
		{
			hidePopups();
		}
		else
		{
			restorePopups();
		}
	}

	private void hidePopups()
	{
		for (int component : RESULT_POPUPS)
		{
			Widget widget = client.getWidget(component);
			if (widget != null && !widget.isHidden())
			{
				widget.setHidden(true);
				hiddenPopups.add(widget);
			}
		}
	}

	/**
	 * Shows only the popups this plugin hid, never one the game hid itself.
	 */
	private void restorePopups()
	{
		for (Widget widget : hiddenPopups)
		{
			widget.setHidden(false);
		}
		hiddenPopups.clear();
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
