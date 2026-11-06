package com.lockboxunbox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.GameState;
import org.junit.Test;

public class GameStateRulesTest
{
	@Test
	public void loadingDoesNotCloseTheAnimation()
	{
		assertFalse(LockboxUnboxPlugin.endsSession(GameState.LOADING));
		assertFalse(LockboxUnboxPlugin.endsSession(GameState.LOGGED_IN));
	}

	@Test
	public void leavingTheWorldClosesTheAnimation()
	{
		assertTrue(LockboxUnboxPlugin.endsSession(GameState.LOGIN_SCREEN));
		assertTrue(LockboxUnboxPlugin.endsSession(GameState.HOPPING));
		assertTrue(LockboxUnboxPlugin.endsSession(GameState.CONNECTION_LOST));
	}
}
