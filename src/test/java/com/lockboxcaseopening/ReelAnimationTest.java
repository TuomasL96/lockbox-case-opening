package com.lockboxcaseopening;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class ReelAnimationTest
{
	private static final long SECOND = 1_000_000_000L;
	private static final long START = 500 * SECOND;
	private static final Loot DARTS = new Loot(ItemID.DRAGON_DART, 125, Tier.COMMON);
	private static final Loot HELM = new Loot(ItemID.OATHPLATE_HELM, 1, Tier.LEGENDARY);
	private static final double[] RESULT_TIMES = {0, 0.1, 0.6, 1.8, 5.0};

	private static long at(double seconds)
	{
		return START + (long) (seconds * SECOND);
	}

	private static ReelAnimation reel(long seed)
	{
		return new ReelAnimation(new Random(seed), START, 6, 4);
	}

	/** First sampled moment the reel has reached REVEAL, in seconds. */
	private static double settleEnd(ReelAnimation reel)
	{
		for (double t = 0; t < 60; t += 0.001)
		{
			if (reel.phase(at(t)) == ReelAnimation.Phase.REVEAL)
			{
				return t;
			}
		}
		throw new AssertionError("reel never stopped");
	}

	@Test
	public void landsExactlyOnWinner()
	{
		for (double resultTime : RESULT_TIMES)
		{
			for (long seed = 0; seed < 25; seed++)
			{
				ReelAnimation reel = reel(seed);
				reel.setWinner(DARTS, at(resultTime));
				double end = settleEnd(reel) + 0.01;
				assertEquals(reel.winnerIndex(), Math.round(reel.position(at(end))));
				assertEquals(DARTS, reel.tile(reel.winnerIndex()));
			}
		}
	}

	@Test
	public void winnerIsOutOfViewWhenPlaced()
	{
		for (double resultTime : RESULT_TIMES)
		{
			ReelAnimation reel = reel(1);
			reel.setWinner(DARTS, at(resultTime));
			assertTrue(reel.winnerIndex() - reel.position(at(resultTime)) > ReelAnimation.VISIBLE_HALF_TILES + 1);
		}
	}

	@Test
	public void speedIsContinuousAtTheSwitch()
	{
		for (double resultTime : RESULT_TIMES)
		{
			ReelAnimation reel = reel(2);
			reel.setWinner(DARTS, at(resultTime));
			double step = 0.0005;
			double after = (reel.position(at(resultTime + step)) - reel.position(at(resultTime))) / step;
			assertEquals(ReelAnimation.SPIN_TILES_PER_SECOND, after, ReelAnimation.SPIN_TILES_PER_SECOND * 0.01);
		}
	}

	@Test
	public void reelNeverMovesBackwards()
	{
		ReelAnimation reel = reel(3);
		double previous = -1;
		for (double t = 0; t < 12; t += 0.001)
		{
			if (!reel.hasWinner() && t >= 0.6)
			{
				reel.setWinner(DARTS, at(t));
			}
			double position = reel.position(at(t));
			assertTrue(position >= previous);
			previous = position;
		}
	}

	@Test
	public void totalDurationMatchesSpinSetting()
	{
		ReelAnimation reel = reel(4);
		reel.setWinner(DARTS, at(0.6));
		assertEquals(6.0, settleEnd(reel), 0.25);
	}

	@Test
	public void lateResultStillGetsASettle()
	{
		ReelAnimation reel = reel(5);
		reel.setWinner(DARTS, at(5.5));
		assertTrue(settleEnd(reel) - 5.5 >= ReelAnimation.MIN_SETTLE_SECONDS - 0.15);
	}

	@Test
	public void spinsUntilAWinnerArrives()
	{
		assertEquals(ReelAnimation.Phase.SPINNING, reel(6).phase(at(60)));
	}

	@Test
	public void coverLiftsWhenReelStops()
	{
		ReelAnimation reel = reel(7);
		assertTrue(reel.coverVisible(at(0)));
		reel.setWinner(DARTS, at(0.6));
		assertEquals(ReelAnimation.Phase.SETTLING, reel.phase(at(1)));
		assertTrue(reel.coverVisible(at(1)));
		double end = settleEnd(reel);
		assertEquals(ReelAnimation.Phase.REVEAL, reel.phase(at(end)));
		assertFalse(reel.coverVisible(at(end)));
	}

	@Test
	public void revealEndsAfterRevealSeconds()
	{
		ReelAnimation reel = reel(8);
		reel.setWinner(DARTS, at(0.6));
		double end = settleEnd(reel);
		assertEquals(ReelAnimation.Phase.REVEAL, reel.phase(at(end + 3.9)));
		assertEquals(ReelAnimation.Phase.DONE, reel.phase(at(end + 4.1)));
	}

	@Test
	public void goldNeverTimesOut()
	{
		ReelAnimation reel = reel(9);
		reel.setWinner(HELM, at(0.6));
		assertEquals(ReelAnimation.Phase.REVEAL, reel.phase(at(3600)));
	}

	@Test
	public void tilesAreStable()
	{
		ReelAnimation reel = reel(10);
		assertSame(reel.tile(50), reel.tile(50));
	}
}
