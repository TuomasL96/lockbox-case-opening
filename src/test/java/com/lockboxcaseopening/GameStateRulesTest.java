package com.lockboxcaseopening;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.GameState;
import org.junit.Test;

public class GameStateRulesTest
{
	@Test
	public void loadingDoesNotCloseTheAnimation()
	{
		assertFalse(LockboxCaseOpeningPlugin.endsSession(GameState.LOADING));
		assertFalse(LockboxCaseOpeningPlugin.endsSession(GameState.LOGGED_IN));
	}

	@Test
	public void leavingTheWorldClosesTheAnimation()
	{
		assertTrue(LockboxCaseOpeningPlugin.endsSession(GameState.LOGIN_SCREEN));
		assertTrue(LockboxCaseOpeningPlugin.endsSession(GameState.HOPPING));
		assertTrue(LockboxCaseOpeningPlugin.endsSession(GameState.CONNECTION_LOST));
	}
}
