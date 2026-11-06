package com.lockboxunbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.swing.JButton;
import org.junit.Test;

public class PracticeSpinPanelTest
{
	@Test
	public void onlyPracticeSpinIsShownByDefault()
	{
		PracticeSpinPanel panel = new PracticeSpinPanel(() ->
		{
		});
		assertEquals(1, panel.buttonList().getComponentCount());
		assertEquals("Practice Spin", ((JButton) panel.buttonList().getComponent(0)).getText());
	}

	@Test
	public void practiceButtonRunsTheCallback()
	{
		boolean[] clicked = {false};
		PracticeSpinPanel panel = new PracticeSpinPanel(() -> clicked[0] = true);
		((JButton) panel.buttonList().getComponent(0)).doClick();
		assertTrue(clicked[0]);
	}
}
