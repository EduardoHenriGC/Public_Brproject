package ext.mods.gameserver.scripting.script.events.epicboss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;

import ext.mods.commons.logging.CLogger;
import ext.mods.commons.pool.ThreadPool;
import ext.mods.Config;
import ext.mods.gameserver.model.World;
import ext.mods.gameserver.model.actor.Creature;
import ext.mods.gameserver.model.actor.Npc;
import ext.mods.gameserver.scripting.Quest;

public class EpicBossEventManager extends Quest
{
	private static final CLogger LOGGER = new CLogger(EpicBossEventManager.class.getName());
	
	private static final int BAIUM = 29020;
	private static final int ANTHARAS = 29068;
	private static final int FRINTEZZA = 29047;
	private static final int VALAKAS = 29028;
	
	private static final Integer[] EPIC_BOSSES =
	{
		BAIUM, ANTHARAS, FRINTEZZA, VALAKAS
	};
	
	private ScheduledFuture<?> _dailyStartTask;
	private ScheduledFuture<?> _nextBossSpawnTask;
	
	private List<Integer> _bossOrder = new ArrayList<>();
	private int _currentBossIndex = 0;
	private Npc _currentSpawnedBoss = null;
	
	private static EpicBossEventManager _instance;
	
	private boolean _isEventRunning = false;
	
	public static EpicBossEventManager getInstance()
	{
		return _instance;
	}
	
	public boolean isEventRunning()
	{
		return _isEventRunning;
	}
	
	public EpicBossEventManager()
	{
		super(-1, "events/epicboss");
		_instance = this;
		
		if (!Config.EPIC_BOSS_EVENT_ENABLED)
			return;
		
		addMyDying(BAIUM, ANTHARAS, FRINTEZZA, VALAKAS);
		
		scheduleNextDailyEvent();
		LOGGER.info("EpicBossEventManager: Loaded successfully. Event scheduled to " + Config.EPIC_BOSS_EVENT_HOUR + ":" + String.format("%02d", Config.EPIC_BOSS_EVENT_MINUTE));
	}
	
	private void scheduleNextDailyEvent()
	{
		TimeZone tz = TimeZone.getTimeZone("GMT-3");
		Calendar currentTime = Calendar.getInstance(tz);
		Calendar nextStartTime = Calendar.getInstance(tz);
		
		nextStartTime.set(Calendar.HOUR_OF_DAY, Config.EPIC_BOSS_EVENT_HOUR);
		nextStartTime.set(Calendar.MINUTE, Config.EPIC_BOSS_EVENT_MINUTE);
		nextStartTime.set(Calendar.SECOND, 0);
		
		if (nextStartTime.getTimeInMillis() <= currentTime.getTimeInMillis())
		{
			nextStartTime.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		long delay = nextStartTime.getTimeInMillis() - currentTime.getTimeInMillis();
		
		if (_dailyStartTask != null)
		{
			_dailyStartTask.cancel(false);
		}
		
		_dailyStartTask = ThreadPool.schedule(this::startEvent, delay);
	}
	
	public void startEvent()
	{
		LOGGER.info("EpicBossEventManager: Starting daily event.");
		
		// Clean up any remaining boss or tasks from yesterday before starting
		cleanupEvent();
		
		_isEventRunning = true;
		
		for (ext.mods.gameserver.model.actor.Player player : World.getInstance().getPlayers())
		{
			if (player.isInsideZone(ext.mods.gameserver.enums.ZoneId.EPIC_EVENT))
			{
				player.updatePvPFlag(1);
			}
		}
		
		World.announceToOnlinePlayers("[Epic Boss Event] O evento comecou! O primeiro boss esta nascendo.");
		
		// Reschedule for tomorrow
		scheduleNextDailyEvent();
		
		// Generate random boss order
		_bossOrder = new ArrayList<>(Arrays.asList(EPIC_BOSSES));
		Collections.shuffle(_bossOrder);
		
		LOGGER.info("EpicBossEventManager: Boss order randomized: " + _bossOrder.toString());
		
		_currentBossIndex = 0;
		spawnNextBoss();
	}
	
	private void cleanupEvent()
	{
		_isEventRunning = false;
		
		for (ext.mods.gameserver.model.actor.Player player : World.getInstance().getPlayers())
		{
			if (player.isInsideZone(ext.mods.gameserver.enums.ZoneId.EPIC_EVENT))
			{
				player.updatePvPFlag(0);
			}
		}
		
		if (_nextBossSpawnTask != null)
		{
			_nextBossSpawnTask.cancel(false);
			_nextBossSpawnTask = null;
		}
		
		if (_currentSpawnedBoss != null)
		{
			_currentSpawnedBoss.deleteMe();
			_currentSpawnedBoss = null;
		}
		
		_bossOrder.clear();
		_currentBossIndex = 0;
	}
	
	private void spawnNextBoss()
	{
		if (_currentBossIndex >= _bossOrder.size())
		{
			LOGGER.info("EpicBossEventManager: All bosses have been defeated. Event ended.");
			cleanupEvent();
			return;
		}
		
		int bossId = _bossOrder.get(_currentBossIndex);
		
		try
		{
			int x = Config.EPIC_BOSS_LOCATION[0];
			int y = Config.EPIC_BOSS_LOCATION[1];
			int z = Config.EPIC_BOSS_LOCATION[2];
			
			_currentSpawnedBoss = addSpawn(bossId, x, y, z, 0, false, 0, false);
			LOGGER.info("EpicBossEventManager: Spawned boss " + bossId + " at index " + _currentBossIndex);
			World.announceToOnlinePlayers("[Epic Boss Event] O Epic Boss " + _currentSpawnedBoss.getName() + " nasceu!");
		}
		catch (Exception e)
		{
			LOGGER.error("EpicBossEventManager: Error spawning boss " + bossId, e);
		}
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (!Config.EPIC_BOSS_EVENT_ENABLED)
			return;
		
		if (_currentSpawnedBoss != null && _currentSpawnedBoss.getObjectId() == npc.getObjectId())
		{
			LOGGER.info("EpicBossEventManager: Boss " + npc.getNpcId() + " died.");
			
			_currentSpawnedBoss = null;
			_currentBossIndex++;
			
			if (_currentBossIndex >= _bossOrder.size())
			{
				LOGGER.info("EpicBossEventManager: Final boss defeated. Event completed!");
				World.announceToOnlinePlayers("[Epic Boss Event] Todos os bosses foram derrotados. Evento concluido!");
				cleanupEvent();
			}
			else
			{
				LOGGER.info("EpicBossEventManager: Scheduling next boss in " + Config.EPIC_BOSS_DELAY_NEXT_BOSS + " minutes.");
				World.announceToOnlinePlayers("[Epic Boss Event] O proximo boss nascera em " + Config.EPIC_BOSS_DELAY_NEXT_BOSS + " minuto(s).");
				_nextBossSpawnTask = ThreadPool.schedule(this::spawnNextBoss, Config.EPIC_BOSS_DELAY_NEXT_BOSS * 60 * 1000L);
			}
		}
	}
}
