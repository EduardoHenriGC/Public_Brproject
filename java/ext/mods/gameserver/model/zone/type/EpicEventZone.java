package ext.mods.gameserver.model.zone.type;

import ext.mods.gameserver.enums.ZoneId;
import ext.mods.gameserver.model.actor.Creature;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.zone.type.subtype.ZoneType;
import ext.mods.gameserver.scripting.script.events.epicboss.EpicBossEventManager;

public class EpicEventZone extends ZoneType
{
	public EpicEventZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.EPIC_EVENT, true);
		
		if (creature instanceof Player player)
		{
			if (EpicBossEventManager.getInstance() != null && EpicBossEventManager.getInstance().isEventRunning())
			{
				player.updatePvPFlag(1);
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.EPIC_EVENT, false);
		
		if (creature instanceof Player player)
		{
			player.updatePvPFlag(0);
		}
	}
}
