package com.lockboxunbox;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.SoundEffectID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.QuantityFormatter;

/**
 * Draws the centred panel, the reel and the result.
 */
@Singleton
public class UnboxOverlay extends Overlay
{
	private static final String TITLE = "FORGOTTEN LOCKBOX";
	private static final int MAX_WIDTH = 720;
	private static final int MAX_HEIGHT = 190;
	private static final int MARGIN = 20;
	private static final int TILE_WIDTH = 104;
	private static final int TILE_PITCH = TILE_WIDTH + 8;
	private static final int FADE_WIDTH = 70;
	private static final int HEADER_HEIGHT = 34;
	private static final int FOOTER_HEIGHT = 40;
	private static final int MIN_REEL_HEIGHT = 40;
	private static final long TICK_SOUND_GAP_NANOS = 60_000_000L;
	private static final double GOLD_FLASH_SECONDS = 0.6;

	private static final Color PANEL_COLOR = new Color(0x16171B);
	private static final Color PANEL_EDGE = new Color(0x3A3C44);
	private static final Color TILE_COLOR = new Color(0x26282E);
	private static final Color MARKER_COLOR = new Color(0xFFD700);

	private final Client client;
	private final ItemManager itemManager;
	private final UnboxController controller;
	private final LockboxUnboxConfig config;

	private volatile Rectangle bounds;
	private ReelAnimation soundAnimation;
	private long lastTile;
	private long lastTickSoundNanos;
	private boolean stopSoundPlayed;

	@Inject
	public UnboxOverlay(Client client, ItemManager itemManager, UnboxController controller, LockboxUnboxConfig config)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.controller = controller;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(Overlay.PRIORITY_HIGHEST);
	}

	static Rectangle panelBounds(int canvasWidth, int canvasHeight)
	{
		int width = Math.min(MAX_WIDTH, canvasWidth - MARGIN * 2);
		int height = Math.min(MAX_HEIGHT, canvasHeight - MARGIN * 2);
		return new Rectangle((canvasWidth - width) / 2, (canvasHeight - height) / 2, width, height);
	}

	/**
	 * The panel as last drawn, or null when idle. Safe to read from any thread.
	 */
	public Rectangle bounds()
	{
		return bounds;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		long now = System.nanoTime();
		controller.update(now);
		ReelAnimation reel = controller.animation();
		if (reel == null)
		{
			bounds = null;
			soundAnimation = null;
			return null;
		}

		Rectangle panel = panelBounds(client.getCanvasWidth(), client.getCanvasHeight());
		bounds = panel;
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		ReelAnimation.Phase phase = reel.phase(now);
		double position = reel.position(now);

		drawPanel(graphics, panel);
		drawHeader(graphics, panel);
		drawReel(graphics, panel, reel, position, phase, now);
		if (phase == ReelAnimation.Phase.REVEAL)
		{
			drawResult(graphics, panel, reel.winner());
			drawGoldFlash(graphics, panel, reel, now);
		}
		playSounds(reel, position, phase, now);
		return null;
	}

	private void drawPanel(Graphics2D graphics, Rectangle panel)
	{
		graphics.setColor(PANEL_COLOR);
		graphics.fillRoundRect(panel.x, panel.y, panel.width, panel.height, 12, 12);
		graphics.setColor(PANEL_EDGE);
		graphics.drawRoundRect(panel.x, panel.y, panel.width, panel.height, 12, 12);
	}

	private static void drawHeader(Graphics2D graphics, Rectangle panel)
	{
		drawCentered(graphics, TITLE, panel.x + panel.width / 2, panel.y + 22, Color.WHITE);
	}

	private void drawReel(Graphics2D graphics, Rectangle panel, ReelAnimation reel, double position,
		ReelAnimation.Phase phase, long now)
	{
		int reelHeight = Math.max(MIN_REEL_HEIGHT, panel.height - HEADER_HEIGHT - FOOTER_HEIGHT);
		Rectangle area = new Rectangle(panel.x + 8, panel.y + HEADER_HEIGHT, panel.width - 16, reelHeight);
		int centre = panel.x + panel.width / 2;

		Shape oldClip = graphics.getClip();
		graphics.clip(area);

		int nearest = (int) Math.round(position);
		for (int i = nearest - ReelAnimation.VISIBLE_HALF_TILES; i <= nearest + ReelAnimation.VISIBLE_HALF_TILES; i++)
		{
			int x = (int) Math.round(centre + (i - position) * TILE_PITCH - TILE_WIDTH / 2.0);
			boolean won = phase == ReelAnimation.Phase.REVEAL && i == reel.winnerIndex();
			drawTile(graphics, new Rectangle(x, area.y, TILE_WIDTH, area.height), reel.tile(i), won);
		}

		Color clear = new Color(PANEL_COLOR.getRed(), PANEL_COLOR.getGreen(), PANEL_COLOR.getBlue(), 0);
		graphics.setPaint(new GradientPaint(area.x, 0, PANEL_COLOR, area.x + FADE_WIDTH, 0, clear));
		graphics.fillRect(area.x, area.y, FADE_WIDTH, area.height);
		int right = area.x + area.width;
		graphics.setPaint(new GradientPaint(right - FADE_WIDTH, 0, clear, right, 0, PANEL_COLOR));
		graphics.fillRect(right - FADE_WIDTH, area.y, FADE_WIDTH, area.height);

		graphics.setClip(oldClip);

		graphics.setColor(MARKER_COLOR);
		graphics.fillRect(centre - 1, area.y - 3, 2, area.height + 6);
	}

	private void drawTile(Graphics2D graphics, Rectangle tile, Loot loot, boolean won)
	{
		Color color = loot.getTier().color();
		graphics.setColor(TILE_COLOR);
		graphics.fillRect(tile.x, tile.y, tile.width, tile.height);

		Color glow = new Color(color.getRed(), color.getGreen(), color.getBlue(), won ? 130 : 70);
		Color clear = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
		graphics.setPaint(new GradientPaint(0, tile.y, clear, 0, tile.y + tile.height, glow));
		graphics.fillRect(tile.x, tile.y, tile.width, tile.height);

		graphics.setColor(color);
		graphics.fillRect(tile.x, tile.y + tile.height - 5, tile.width, 5);

		if (loot.getTier() == Tier.LEGENDARY && !won)
		{
			drawStar(graphics, tile, color);
		}
		else
		{
			drawItem(graphics, tile, loot);
		}

		if (won)
		{
			graphics.setColor(color);
			graphics.setStroke(new BasicStroke(2));
			graphics.drawRect(tile.x + 1, tile.y + 1, tile.width - 2, tile.height - 2);
		}
	}

	private void drawItem(Graphics2D graphics, Rectangle tile, Loot loot)
	{
		BufferedImage image = itemManager.getImage(loot.getItemId(), loot.getQuantity(), loot.getQuantity() > 1);
		int scale = tile.height >= 80 ? 2 : 1;
		int width = image.getWidth() * scale;
		int height = image.getHeight() * scale;
		int x = tile.x + (tile.width - width) / 2;
		int y = tile.y + (tile.height - height) / 2;
		graphics.drawImage(image, x, y, width, height, null);
	}

	private static void drawStar(Graphics2D graphics, Rectangle tile, Color color)
	{
		double centreX = tile.getCenterX();
		double centreY = tile.getCenterY();
		double outer = Math.min(tile.width, tile.height) * 0.3;
		double inner = outer * 0.42;

		Path2D star = new Path2D.Double();
		for (int i = 0; i < 10; i++)
		{
			double radius = i % 2 == 0 ? outer : inner;
			double angle = -Math.PI / 2 + i * Math.PI / 5;
			double x = centreX + radius * Math.cos(angle);
			double y = centreY + radius * Math.sin(angle);
			if (i == 0)
			{
				star.moveTo(x, y);
			}
			else
			{
				star.lineTo(x, y);
			}
		}
		star.closePath();
		graphics.setColor(color);
		graphics.fill(star);
	}

	private void drawResult(Graphics2D graphics, Rectangle panel, Loot loot)
	{
		String name = itemManager.getItemComposition(loot.getItemId()).getName();
		String quantity = loot.getQuantity() > 1 ? " x " + QuantityFormatter.formatNumber(loot.getQuantity()) : "";
		String text = name + quantity + " · " + loot.getTier().label();
		drawCentered(graphics, text, panel.x + panel.width / 2, panel.y + panel.height - 14, loot.getTier().color());
	}

	private static void drawGoldFlash(Graphics2D graphics, Rectangle panel, ReelAnimation reel, long now)
	{
		double since = reel.secondsSinceStop(now);
		if (reel.winner().getTier() != Tier.LEGENDARY || since >= GOLD_FLASH_SECONDS)
		{
			return;
		}
		float strength = (float) (0.5 * (1 - since / GOLD_FLASH_SECONDS));
		Composite old = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strength));
		graphics.setColor(Tier.LEGENDARY.color());
		graphics.fillRoundRect(panel.x, panel.y, panel.width, panel.height, 12, 12);
		graphics.setComposite(old);
	}

	private void playSounds(ReelAnimation reel, double position, ReelAnimation.Phase phase, long now)
	{
		if (reel != soundAnimation)
		{
			soundAnimation = reel;
			lastTile = (long) Math.floor(position);
			stopSoundPlayed = false;
		}
		if (!config.soundEffects())
		{
			return;
		}

		long tile = (long) Math.floor(position);
		if (phase == ReelAnimation.Phase.REVEAL)
		{
			if (!stopSoundPlayed)
			{
				stopSoundPlayed = true;
				client.playSoundEffect(reel.winner().getTier() == Tier.LEGENDARY
					? SoundEffectID.GE_COIN_TINKLE
					: SoundEffectID.GE_ADD_OFFER_DINGALING);
			}
		}
		else if (tile != lastTile && now - lastTickSoundNanos >= TICK_SOUND_GAP_NANOS)
		{
			lastTile = tile;
			lastTickSoundNanos = now;
			client.playSoundEffect(SoundEffectID.UI_BOOP);
		}
	}

	/**
	 * Draws text with a shadow, falling back to a system font if the game font
	 * lacks one of its characters.
	 */
	static void drawCentered(Graphics2D graphics, String text, int centreX, int baseline, Color color)
	{
		Font font = FontManager.getRunescapeBoldFont();
		if (font.canDisplayUpTo(text) != -1)
		{
			font = new Font(Font.SANS_SERIF, Font.BOLD, 13);
		}
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		int x = centreX - metrics.stringWidth(text) / 2;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, baseline + 1);
		graphics.setColor(color);
		graphics.drawString(text, x, baseline);
	}
}
