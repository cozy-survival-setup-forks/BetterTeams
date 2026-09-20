package com.booksaw.betterTeams.commands.team;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.commands.SubCommand;
import com.booksaw.betterTeams.menu.LevelsMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Opens the menu with every team level, the level of your team (or the team you name) is marked.
 */
public class LevelsCommand extends SubCommand {

	@Override
	public CommandResponse onCommand(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			return new CommandResponse("needPlayer");
		}

		Team team = args.length >= 1 ? Team.getTeam(args[0]) : Team.getTeam(player);
		if (args.length >= 1 && team == null) {
			return new CommandResponse("rank.noTeam");
		}

		LevelsMenu.open(player, team);
		return new CommandResponse(true);
	}

	/**
	 * Opening a menu has to happen on the main thread
	 */
	@Override
	protected boolean runAsync(String[] args) {
		return false;
	}

	@Override
	public String getCommand() {
		return "levels";
	}

	@Override
	public String getNode() {
		return "levels";
	}

	@Override
	public String getHelp() {
		return "View all team levels and what they give";
	}

	@Override
	public String getArguments() {
		return "[team]";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public int getMaximumArguments() {
		return 1;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		addTeamStringList(options, args[0]);
	}
}
