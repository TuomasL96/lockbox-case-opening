package com.lockboxunbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws a solid panel over the inventory until the reel stops, so the loot
 * can't be seen early. It only reads widget positions and never changes them.
 */
@Singleton
public class InventoryCoverOverlay extends Overlay
{
	private static final String TEXT = "Unboxing…";
	private static final Color COVER_COLOR = new Color(0x1A1A1A);

	private final Client client;
	private final UnboxController controller;

	private volatile Rectangle bounds;

	@Inject
	public InventoryCoverOverlay(Client client, UnboxController controller)
	{
		this.client = client;
		this.controller = controller;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		// Below the panel, which can overlap the inventory in small windows.
		setPriority(Overlay.PRIORITY_HIGH);
	}

	/**
	 * The area covered, or null if nothing is. Safe to read from any thread.
	 */
	public Rectangle bounds()
	{
		return bounds;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!controller.coverVisible(System.nanoTime()))
		{
			bounds = null;
			return null;
		}

		Rectangle covered = union(widgetBounds(InterfaceID.Inventory.ITEMS), widgetBounds(InterfaceID.Bankside.ITEMS));
		bounds = covered;
		if (covered != null)
		{
			graphics.setColor(COVER_COLOR);
			graphics.fill(covered);
			UnboxOverlay.drawCentered(graphics, TEXT, (int) covered.getCenterX(), (int) covered.getCenterY(), Color.WHITE);
		}
		return null;
	}

	private Rectangle widgetBounds(int componentId)
	{
		Widget widget = client.getWidget(componentId);
		return widget == null || widget.isHidden() ? null : widget.getBounds();
	}

	private static Rectangle union(Rectangle first, Rectangle second)
	{
		if (first == null)
		{
			return second;
		}
		return second == null ? first : first.union(second);
	}
}
