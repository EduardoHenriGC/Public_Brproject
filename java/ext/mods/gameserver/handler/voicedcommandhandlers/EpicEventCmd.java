package ext.mods.gameserver.handler.voicedcommandhandlers;

import ext.mods.gameserver.handler.IVoicedCommandHandler;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.scripting.script.events.epicboss.EpicBossEventManager;

public class EpicEventCmd implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"epicevent"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.equals("epicevent") && player.isGM())
		{
			if (EpicBossEventManager.getInstance() != null)
			{
				EpicBossEventManager.getInstance().startEvent();
				player.sendMessage("Evento Epic Boss iniciado manualmente.");
			}
			else
			{
				player.sendMessage("O Epic Boss Event Manager nao esta ativo. Verifique se o Config.EPIC_BOSS_EVENT_ENABLED esta 'true' no events.properties.");
			}
			return true;
		}
		
		return false;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
