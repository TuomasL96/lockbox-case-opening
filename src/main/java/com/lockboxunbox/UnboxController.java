package com.lockboxunbox;

import java.util.Map;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Runs one unboxing from start to finish: waits for the real result, passes it
 * to the reel, and ends the animation. Call everything except the getters on
 * the client thread.
 */
@Singleton
public class UnboxController
{
	public static final int TIMEOUT_TICKS = 5;

	private final Random random;

	private volatile ReelAnimation animation;
	private volatile boolean practice;
	private volatile boolean practiceCover;
	private Map<Integer, Integer> before;
	private int ticksWaited;

	@Inject
	public UnboxController()
	{
		this(new Random());
	}

	UnboxController(Random random)
	{
		this.random = random;
	}

	/**
	 * Starts a real unboxing. A real spin in progress is left alone. Anything
	 * else (a practice spin, or a reveal) gives way to it.
	 */
	public void openClicked(Map<Integer, Integer> before, long nowNanos, int spinSeconds, int revealSeconds)
	{
		ReelAnimation current = animation;
		if (current != null && !practice && current.coverVisible(nowNanos))
		{
			return;
		}
		this.before = before;
		ticksWaited = 0;
		practice = false;
		practiceCover = false;
		animation = new ReelAnimation(random, nowNanos, spinSeconds, revealSeconds);
	}

	public void inventoryChanged(Map<Integer, Integer> after, long nowNanos)
	{
		ReelAnimation current = animation;
		if (current == null || practice || current.hasWinner())
		{
			return;
		}
		int consumed = LootDiff.boxesConsumed(before, after);
		if (consumed == 0)
		{
			// Keeps anything gained while waiting from being counted as loot.
			before = after;
			return;
		}
		Loot loot = LootDiff.gained(before, after).orElse(null);
		if (consumed > 1 || loot == null)
		{
			// Two results can't be told apart, and the reel must never guess.
			close();
			return;
		}
		current.setWinner(loot, nowNanos);
	}

	public void gameTick()
	{
		ReelAnimation current = animation;
		if (current == null || practice || current.hasWinner())
		{
			return;
		}
		ticksWaited++;
		if (ticksWaited >= TIMEOUT_TICKS)
		{
			close();
		}
	}

	public void startPractice(Loot loot, long nowNanos, int spinSeconds, int revealSeconds)
	{
		startPractice(loot, nowNanos, spinSeconds, revealSeconds, false);
	}

	/**
	 * Starts a practice spin, optionally with the inventory cover a real open
	 * would show.
	 */
	public void startPractice(Loot loot, long nowNanos, int spinSeconds, int revealSeconds, boolean showCover)
	{
		if (animation != null)
		{
			return;
		}
		ReelAnimation reel = new ReelAnimation(random, nowNanos, spinSeconds, revealSeconds);
		reel.setWinner(loot, nowNanos);
		practice = true;
		practiceCover = showCover;
		animation = reel;
	}

	public void update(long nowNanos)
	{
		ReelAnimation current = animation;
		if (current != null && current.phase(nowNanos) == ReelAnimation.Phase.DONE)
		{
			close();
		}
	}

	public void close()
	{
		animation = null;
		practice = false;
		practiceCover = false;
		before = null;
	}

	/**
	 * Closes only if the given animation is still the current one, so a close
	 * queued by an old click can't end a newer animation.
	 */
	public void close(ReelAnimation expected)
	{
		if (animation == expected)
		{
			close();
		}
	}

	public ReelAnimation animation()
	{
		return animation;
	}

	/**
	 * True while the inventory should be covered. Safe to call from any thread.
	 */
	public boolean coverVisible(long nowNanos)
	{
		ReelAnimation current = animation;
		return current != null && (!practice || practiceCover) && current.coverVisible(nowNanos);
	}

	public boolean isPractice()
	{
		return practice;
	}

	public boolean isActive()
	{
		return animation != null;
	}
}
