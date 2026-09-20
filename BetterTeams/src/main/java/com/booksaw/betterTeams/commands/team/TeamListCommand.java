package com.booksaw.betterTeams.commands.team;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.commands.SubCommand;
import com.booksaw.betterTeams.menu.TeamListMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /teamlist: a menu of every team, click one to see who is in it.
 */
public class TeamListCommand extends SubCommand {

	@Override
	public CommandResponse onCommand(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			return new CommandResponse("needPlayer");
		}

		TeamListMenu.open(player);
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
		return "teamlist";
	}

	@Override
	public String getNode() {
		return "teamlist";
	}

	@Override
	public String getHelp() {
		return "Browse all teams and their members in a menu";
	}

	@Override
	public String getArguments() {
		return "";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public int getMaximumArguments() {
		return 0;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
	}
}
