package com.lockboxunbox;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The motion of one reel. The reel spins at a constant speed until the result
 * is known, then eases to a stop on the winning tile. All times are supplied in
 * nanoseconds, so it runs independently of game ticks.
 */
public class ReelAnimation
{
	public enum Phase
	{
		SPINNING,
		SETTLING,
		REVEAL,
		DONE
	}

	public static final double SPIN_TILES_PER_SECOND = 24.0;
	public static final double MIN_SETTLE_SECONDS = 2.0;
	public static final int VISIBLE_HALF_TILES = 4;
	public static final double MAX_LAND_OFFSET = 0.35;

	private static final double NANOS_PER_SECOND = 1_000_000_000.0;

	private final Random random;
	private final long startNanos;
	private final int spinSeconds;
	private final int revealSeconds;
	private final Map<Integer, Loot> tiles = new HashMap<>();

	// Written last in setWinner, so other threads reading it see the fields below.
	private volatile Loot winner;
	private int winnerIndex;
	private long settleStartNanos;
	private double settleStartPosition;
	private double settleDistance;
	private double settleSeconds;

	public ReelAnimation(Random random, long startNanos, int spinSeconds, int revealSeconds)
	{
		this.random = random;
		this.startNanos = startNanos;
		this.spinSeconds = spinSeconds;
		this.revealSeconds = revealSeconds;
	}

	/**
	 * Places the winner on a tile still out of view and starts easing towards it.
	 * Ease-out cubic starts at speed 3D/T, so T is derived from the distance to
	 * keep the speed continuous. That shifts the stop by a fraction of a second.
	 */
	public void setWinner(Loot winner, long nowNanos)
	{
		double elapsed = seconds(nowNanos - startNanos);
		double wantedSeconds = Math.max(spinSeconds - elapsed, MIN_SETTLE_SECONDS);
		double speed = SPIN_TILES_PER_SECOND;

		settleStartPosition = speed * elapsed;
		settleStartNanos = nowNanos;
		winnerIndex = (int) Math.ceil(settleStartPosition + speed * wantedSeconds / 3);

		double offset = (random.nextDouble() * 2 - 1) * MAX_LAND_OFFSET;
		settleDistance = winnerIndex + offset - settleStartPosition;
		settleSeconds = 3 * settleDistance / speed;

		tiles.put(winnerIndex, winner);
		this.winner = winner;
	}

	/**
	 * Reel position in tiles. Tile i sits under the marker at position i.
	 */
	public double position(long nowNanos)
	{
		if (winner == null)
		{
			return SPIN_TILES_PER_SECOND * seconds(nowNanos - startNanos);
		}
		double progress = Math.min(1, Math.max(0, seconds(nowNanos - settleStartNanos) / settleSeconds));
		return settleStartPosition + settleDistance * (1 - Math.pow(1 - progress, 3));
	}

	public Loot tile(int index)
	{
		return tiles.computeIfAbsent(index, i -> DropTable.roll(random));
	}

	public Phase phase(long nowNanos)
	{
		if (winner == null)
		{
			return Phase.SPINNING;
		}
		double sinceStop = secondsSinceStop(nowNanos);
		if (sinceStop < 0)
		{
			return Phase.SETTLING;
		}
		if (winner.getTier() != Tier.LEGENDARY && sinceStop >= revealSeconds)
		{
			return Phase.DONE;
		}
		return Phase.REVEAL;
	}

	public boolean coverVisible(long nowNanos)
	{
		Phase phase = phase(nowNanos);
		return phase == Phase.SPINNING || phase == Phase.SETTLING;
	}

	/**
	 * Seconds since the reel stopped. Only meaningful once a winner is set.
	 */
	public double secondsSinceStop(long nowNanos)
	{
		return seconds(nowNanos - settleStartNanos) - settleSeconds;
	}

	public boolean hasWinner()
	{
		return winner != null;
	}

	public Loot winner()
	{
		return winner;
	}

	public int winnerIndex()
	{
		return winnerIndex;
	}

	private static double seconds(long nanos)
	{
		return nanos / NANOS_PER_SECOND;
	}
}
