package com.lockboxunbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class UnboxControllerTest
{
	private static final long SECOND = 1_000_000_000L;
	private static final int BOX = DropTable.LOCKBOX_ID;
	private static final Loot DARTS = new Loot(ItemID.DRAGON_DART, 125, Tier.COMMON);
	private static final Loot HELM = new Loot(ItemID.OATHPLATE_HELM, 1, Tier.LEGENDARY);

	private final UnboxController controller = new UnboxController(new Random(1));

	private static Map<Integer, Integer> inv(int... pairs)
	{
		Map<Integer, Integer> map = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			map.put(pairs[i], pairs[i + 1]);
		}
		return map;
	}

	private void open(long nanos)
	{
		controller.openClicked(inv(BOX, 1, ItemID.SHARK, 3), nanos, 6, 4);
	}

	private void receiveDarts(long nanos)
	{
		controller.inventoryChanged(inv(ItemID.SHARK, 3, ItemID.DRAGON_DART, 125), nanos);
	}

	@Test
	public void openStartsSpinningWithoutAWinner()
	{
		open(0);
		assertTrue(controller.isActive());
		assertFalse(controller.isPractice());
		assertFalse(controller.animation().hasWinner());
		assertEquals(ReelAnimation.Phase.SPINNING, controller.animation().phase(SECOND));
	}

	@Test
	public void consumedBoxSetsTheWinner()
	{
		open(0);
		receiveDarts(SECOND / 2);
		assertEquals(DARTS, controller.animation().winner());
	}

	@Test
	public void otherInventoryChangeKeepsWaiting()
	{
		open(0);
		controller.inventoryChanged(inv(BOX, 1, ItemID.SHARK, 2), SECOND / 2);
		assertTrue(controller.isActive());
		assertFalse(controller.animation().hasWinner());
	}

	@Test
	public void noBoxConsumedTimesOut()
	{
		open(0);
		for (int i = 0; i < UnboxController.TIMEOUT_TICKS - 1; i++)
		{
			controller.gameTick();
		}
		assertTrue(controller.isActive());
		controller.gameTick();
		assertFalse(controller.isActive());
	}

	@Test
	public void consumedBoxStopsTheTimeout()
	{
		open(0);
		receiveDarts(SECOND / 2);
		for (int i = 0; i < UnboxController.TIMEOUT_TICKS * 2; i++)
		{
			controller.gameTick();
		}
		assertTrue(controller.isActive());
	}

	@Test
	public void consumedWithNoGainCloses()
	{
		open(0);
		controller.inventoryChanged(inv(ItemID.SHARK, 3), SECOND / 2);
		assertFalse(controller.isActive());
	}

	@Test
	public void secondOpenDuringSpinIsIgnored()
	{
		open(0);
		ReelAnimation first = controller.animation();
		open(SECOND);
		assertSame(first, controller.animation());
	}

	@Test
	public void openDuringRevealStartsNew()
	{
		open(0);
		receiveDarts(0);
		ReelAnimation first = controller.animation();
		long inReveal = 7 * SECOND;
		assertEquals(ReelAnimation.Phase.REVEAL, first.phase(inReveal));
		open(inReveal);
		assertNotSame(first, controller.animation());
		assertEquals(ReelAnimation.Phase.SPINNING, controller.animation().phase(inReveal));
	}

	@Test
	public void realOpenReplacesPractice()
	{
		controller.startPractice(DARTS, 0, 6, 4);
		open(SECOND);
		assertFalse(controller.isPractice());
		assertFalse(controller.animation().hasWinner());
	}

	@Test
	public void practiceIgnoredWhileActive()
	{
		open(0);
		ReelAnimation first = controller.animation();
		controller.startPractice(DARTS, SECOND, 6, 4);
		assertSame(first, controller.animation());
		assertFalse(controller.isPractice());
	}

	@Test
	public void practiceHasWinnerAtOnceAndNeverTimesOut()
	{
		controller.startPractice(DARTS, 0, 6, 4);
		assertEquals(DARTS, controller.animation().winner());
		assertTrue(controller.isPractice());
		for (int i = 0; i < 100; i++)
		{
			controller.gameTick();
		}
		assertTrue(controller.isActive());
	}

	@Test
	public void practiceIgnoresInventoryChanges()
	{
		controller.startPractice(HELM, 0, 6, 4);
		receiveDarts(SECOND);
		assertEquals(HELM, controller.animation().winner());
	}

	@Test
	public void staleCloseDoesNotKillNewAnimation()
	{
		open(0);
		ReelAnimation stale = controller.animation();
		controller.close();
		open(SECOND);
		controller.close(stale);
		assertTrue(controller.isActive());
	}

	@Test
	public void closeWithCurrentAnimationCloses()
	{
		open(0);
		controller.close(controller.animation());
		assertFalse(controller.isActive());
		assertNull(controller.animation());
	}

	@Test
	public void updateClosesWhenDone()
	{
		open(0);
		receiveDarts(0);
		controller.update(7 * SECOND);
		assertTrue(controller.isActive());
		controller.update(11 * SECOND);
		assertFalse(controller.isActive());
	}

	@Test
	public void closeWorksInEveryPhase()
	{
		open(0);
		controller.close();
		assertFalse(controller.isActive());

		open(0);
		receiveDarts(0);
		assertEquals(ReelAnimation.Phase.SETTLING, controller.animation().phase(SECOND));
		controller.close();
		assertFalse(controller.isActive());

		controller.startPractice(HELM, 0, 6, 4);
		assertEquals(ReelAnimation.Phase.REVEAL, controller.animation().phase(60 * SECOND));
		controller.close();
		assertFalse(controller.isActive());
	}

	@Test
	public void gainBeforeTheBoxIsConsumedIsNotCountedAsLoot()
	{
		controller.openClicked(inv(BOX, 1), 0, 6, 4);
		controller.inventoryChanged(inv(BOX, 1, ItemID.COINS, 1000), SECOND / 4);
		controller.inventoryChanged(inv(ItemID.COINS, 234333), SECOND / 2);
		assertEquals(new Loot(ItemID.COINS, 233333, Tier.VERY_RARE), controller.animation().winner());
	}

	@Test
	public void twoBoxesConsumedAtOnceCloseWithoutAResult()
	{
		controller.openClicked(inv(BOX, 3), 0, 6, 4);
		controller.inventoryChanged(inv(BOX, 1, ItemID.DRAGON_DART, 125, ItemID.OATHPLATE_HELM, 1), SECOND / 2);
		assertFalse(controller.isActive());
	}

	@Test
	public void realOpenShowsTheCoverUntilTheReelStops()
	{
		open(0);
		assertTrue(controller.coverVisible(SECOND));
		receiveDarts(0);
		assertFalse(controller.coverVisible(7 * SECOND));
	}

	@Test
	public void practiceHasNoCoverByDefault()
	{
		controller.startPractice(DARTS, 0, 6, 4);
		assertFalse(controller.coverVisible(SECOND));
	}

	@Test
	public void practiceCanAskForTheCover()
	{
		controller.startPractice(DARTS, 0, 6, 4, true);
		assertTrue(controller.isPractice());
		assertTrue(controller.coverVisible(SECOND));
		assertFalse(controller.coverVisible(7 * SECOND));
	}

	@Test
	public void coverRequestDoesNotCarryIntoTheNextPractice()
	{
		controller.startPractice(DARTS, 0, 6, 4, true);
		controller.close();
		controller.startPractice(DARTS, 0, 6, 4);
		assertFalse(controller.coverVisible(SECOND));
	}

	@Test
	public void idleHasNoCover()
	{
		assertFalse(controller.coverVisible(SECOND));
	}
}
