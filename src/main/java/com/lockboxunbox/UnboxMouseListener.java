package com.lockboxunbox;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.runelite.client.input.MouseAdapter;

/**
 * Closes the animation on a click outside the panel. Only registered while an
 * animation is showing. Mouse callbacks run off the client thread, so closing is
 * handed over through the supplied executor.
 */
public class UnboxMouseListener extends MouseAdapter
{
	private final UnboxController controller;
	private final Supplier<Rectangle> panel;
	private final Supplier<Rectangle> cover;
	private final Consumer<Runnable> clientThread;

	private volatile boolean swallowRelease;
	private volatile boolean swallowClick;

	public UnboxMouseListener(UnboxController controller, Supplier<Rectangle> panel, Supplier<Rectangle> cover,
		Consumer<Runnable> clientThread)
	{
		this.controller = controller;
		this.panel = panel;
		this.cover = cover;
		this.clientThread = clientThread;
	}

	static ClickAction decide(Point point, Rectangle panel, Rectangle cover, ReelAnimation.Phase phase)
	{
		if (panel != null && panel.contains(point))
		{
			boolean spinning = phase == ReelAnimation.Phase.SPINNING || phase == ReelAnimation.Phase.SETTLING;
			return spinning ? ClickAction.CONSUME : ClickAction.CLOSE_AND_CONSUME;
		}
		if (cover != null && cover.contains(point))
		{
			return ClickAction.CLOSE_AND_CONSUME;
		}
		return ClickAction.CLOSE_AND_PASS;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		swallowRelease = false;
		swallowClick = false;

		ReelAnimation reel = controller.animation();
		if (reel == null)
		{
			return event;
		}

		ClickAction action = decide(event.getPoint(), panel.get(), cover.get(), reel.phase(System.nanoTime()));
		if (action != ClickAction.CONSUME)
		{
			clientThread.accept(() -> controller.close(reel));
		}
		if (action != ClickAction.CLOSE_AND_PASS)
		{
			swallowRelease = true;
			swallowClick = true;
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (swallowRelease)
		{
			swallowRelease = false;
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (swallowClick)
		{
			swallowClick = false;
			event.consume();
		}
		return event;
	}

	/**
	 * True between a consumed press and its release. The listener has to stay
	 * registered until then, or the game would see half a click.
	 */
	public boolean awaitingRelease()
	{
		return swallowRelease;
	}
}
