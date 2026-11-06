package com.lockboxunbox;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel with the Practice Spin button. Everything simulation-related
 * starts here, so removing simulate mode means removing this class and the
 * code that adds it.
 */
public class PracticeSpinPanel extends PluginPanel
{
	private final JButton button = new JButton("Practice Spin");

	public PracticeSpinPanel(Runnable onPractice)
	{
		setLayout(new BorderLayout(0, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel about = new JLabel("<html>Plays the unboxing reel using the real lockbox odds.</html>");
		JLabel notice = new JLabel("<html>Visual only. No items are awarded.</html>");
		notice.setForeground(ColorScheme.BRAND_ORANGE);

		JPanel text = new JPanel(new BorderLayout(0, 8));
		text.setBackground(ColorScheme.DARK_GRAY_COLOR);
		text.add(about, BorderLayout.NORTH);
		text.add(notice, BorderLayout.SOUTH);

		button.setFocusable(false);
		button.addActionListener(event -> onPractice.run());

		add(text, BorderLayout.NORTH);
		add(button, BorderLayout.CENTER);
	}

	public void setLoggedIn(boolean loggedIn)
	{
		button.setEnabled(loggedIn);
	}
}
