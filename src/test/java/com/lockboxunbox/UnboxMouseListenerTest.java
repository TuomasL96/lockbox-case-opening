package com.lockboxunbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Guice;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Random;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import org.junit.Test;

public class UnboxMouseListenerTest
{
	private static final Rectangle PANEL = new Rectangle(100, 100, 200, 100);
	private static final Rectangle COVER = new Rectangle(500, 300, 100, 200);
	private static final Point INSIDE = new Point(150, 150);
	private static final Point OVER_COVER = new Point(550, 400);
	private static final Point OUTSIDE = new Point(10, 10);

	private static MouseEvent event(int id, Point point)
	{
		Component source = new Component()
		{
		};
		return new MouseEvent(source, id, 0, 0, point.x, point.y, 1, false, MouseEvent.BUTTON1);
	}

	private static MouseManager newMouseManager()
	{
		RuneLiteConfig config = new RuneLiteConfig()
		{
		};
		return Guice.createInjector(binder -> binder.bind(RuneLiteConfig.class).toInstance(config))
			.getInstance(MouseManager.class);
	}

	private final UnboxController controller = new UnboxController(new Random(1));
	private final UnboxMouseListener listener =
		new UnboxMouseListener(controller, () -> PANEL, () -> COVER, Runnable::run);

	@Test
	public void insideDuringSpinConsumes()
	{
		assertEquals(ClickAction.CONSUME,
			UnboxMouseListener.decide(INSIDE, PANEL, COVER, ReelAnimation.Phase.SPINNING));
	}

	@Test
	public void insideDuringRevealClosesAndConsumes()
	{
		assertEquals(ClickAction.CLOSE_AND_CONSUME,
			UnboxMouseListener.decide(INSIDE, PANEL, null, ReelAnimation.Phase.REVEAL));
	}

	@Test
	public void outsideClosesAndPasses()
	{
		assertEquals(ClickAction.CLOSE_AND_PASS,
			UnboxMouseListener.decide(OUTSIDE, PANEL, COVER, ReelAnimation.Phase.SETTLING));
	}

	@Test
	public void overCoverClosesAndConsumes()
	{
		assertEquals(ClickAction.CLOSE_AND_CONSUME,
			UnboxMouseListener.decide(OVER_COVER, PANEL, COVER, ReelAnimation.Phase.SPINNING));
	}

	@Test
	public void noCoverDuringReveal()
	{
		assertEquals(ClickAction.CLOSE_AND_PASS,
			UnboxMouseListener.decide(OVER_COVER, PANEL, null, ReelAnimation.Phase.REVEAL));
	}

	@Test
	public void outsidePressClosesTheAnimation()
	{
		controller.openClicked(Collections.emptyMap(), System.nanoTime(), 6, 4);
		MouseEvent press = event(MouseEvent.MOUSE_PRESSED, OUTSIDE);
		listener.mousePressed(press);
		assertNull(controller.animation());
		assertFalse(press.isConsumed());
	}

	@Test
	public void releaseOfConsumedPressIsConsumed()
	{
		controller.openClicked(Collections.emptyMap(), System.nanoTime(), 6, 4);

		MouseEvent press = event(MouseEvent.MOUSE_PRESSED, OVER_COVER);
		listener.mousePressed(press);
		assertTrue(press.isConsumed());
		assertTrue(listener.awaitingRelease());

		MouseEvent release = event(MouseEvent.MOUSE_RELEASED, OVER_COVER);
		listener.mouseReleased(release);
		assertTrue(release.isConsumed());
		assertFalse(listener.awaitingRelease());

		MouseEvent click = event(MouseEvent.MOUSE_CLICKED, OVER_COVER);
		listener.mouseClicked(click);
		assertTrue(click.isConsumed());
	}

	@Test
	public void clicksPassThroughWhenIdle()
	{
		MouseEvent press = event(MouseEvent.MOUSE_PRESSED, INSIDE);
		listener.mousePressed(press);
		assertFalse(press.isConsumed());
	}

	@Test
	public void runsAfterStretchedModeHasTranslatedTheClick()
	{
		Point stretched = new Point(1300, 700);
		Point real = new Point(650, 350);
		MouseManager mouseManager = newMouseManager();
		mouseManager.registerMouseListener(0, new MouseAdapter()
		{
			@Override
			public MouseEvent mousePressed(MouseEvent event)
			{
				return event(MouseEvent.MOUSE_PRESSED, real);
			}
		});
		Rectangle coverAtRealPosition = new Rectangle(600, 300, 100, 100);
		UnboxMouseListener stretchedListener =
			new UnboxMouseListener(controller, () -> PANEL, () -> coverAtRealPosition, Runnable::run);
		controller.openClicked(Collections.emptyMap(), System.nanoTime(), 6, 4);

		UnboxMouseListener.register(mouseManager, stretchedListener);
		MouseEvent result = mouseManager.processMousePressed(event(MouseEvent.MOUSE_PRESSED, stretched));

		assertTrue(result.isConsumed());
	}
}
