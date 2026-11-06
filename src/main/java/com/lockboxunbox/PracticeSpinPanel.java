package com.lockboxunbox;

import java.awt.BorderLayout;
import java.awt.GridLayout;
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
	private final JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));

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

		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttons.add(button);

		add(text, BorderLayout.NORTH);
		add(buttons, BorderLayout.CENTER);
	}

	/**
	 * The panel's button stack. Lets the developer build add its test buttons to
	 * this tab. The shipped plugin only ever shows Practice Spin here.
	 */
	JPanel buttonList()
	{
		return buttons;
	}

	public void setLoggedIn(boolean loggedIn)
	{
		button.setEnabled(loggedIn);
	}
}
