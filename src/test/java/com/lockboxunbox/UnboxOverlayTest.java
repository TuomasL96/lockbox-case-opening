package com.lockboxunbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import org.junit.Test;

public class UnboxOverlayTest
{
	@Test
	public void panelIsCentred()
	{
		assertEquals(new Rectangle(140, 305, 720, 190), UnboxOverlay.panelBounds(1000, 800));
	}

	@Test
	public void panelFitsTinyClient()
	{
		Rectangle panel = UnboxOverlay.panelBounds(300, 150);
		assertTrue(new Rectangle(20, 20, 260, 110).contains(panel));
	}
}
