package dev.noah.lpc;

import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class LPC extends JavaPlugin implements Listener {

	private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

	private LuckPerms luckPerms;
	
	@Override
	public void onEnable() {
		// Load an instance of 'LuckPerms' using the services manager.
		this.luckPerms = getServer().getServicesManager().load(LuckPerms.class);

		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (args.length == 1 && "reload".equals(args[0])) {
			reloadConfig();

			sender.sendMessage(unsafeColorize("&aLPC has been reloaded."));
			return true;
		}

		return false;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
		if (args.length == 1)
			return Collections.singletonList("reload");

		return new ArrayList<>();
	}


	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(final AsyncPlayerChatEvent event) {
		final String message = event.getMessage();
		final Player player = event.getPlayer();

		// Get a LuckPerms cached metadata for the player.
		final CachedMetaData metaData = this.luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
		final String group = metaData.getPrimaryGroup();

		String format = getConfig().getString(getConfig().getString("group-formats." + group) != null ? "group-formats." + group : "chat-format")
				.replace("{prefix}", metaData.getPrefix() != null ? metaData.getPrefix() : "")
				.replace("{suffix}", metaData.getSuffix() != null ? metaData.getSuffix() : "")
				.replace("{prefixes}", metaData.getPrefixes().keySet().stream().map(key -> metaData.getPrefixes().get(key)).collect(Collectors.joining()))
				.replace("{suffixes}", metaData.getSuffixes().keySet().stream().map(key -> metaData.getSuffixes().get(key)).collect(Collectors.joining()))
				.replace("{world}", player.getWorld().getName())
				.replace("{name}", player.getName())
				.replace("{displayname}", player.getDisplayName())
				.replace("{username-color}", metaData.getMetaValue("username-color") != null ? metaData.getMetaValue("username-color") : "")
				.replace("{message-color}", metaData.getMetaValue("message-color") != null ? metaData.getMetaValue("message-color") : "");

		format = unsafeColorize(translateHexColorCodes(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? PlaceholderAPI.setPlaceholders(player, format) : format));

		event.setFormat(format.replace("{message}", colorize(message,player)).replace("%", "%%"));
	}

	// Old colorize method without any checks
	private String unsafeColorize(final String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	private String colorize(final String message, Player player) {
		String output = message;
		//do each step / color on its own and check for permissions for each step
		if (player.hasPermission("lpc.colorcodes")) {
			//translate each color individually
			output = output.replace("&0", ChatColor.BLACK.toString());
			output = output.replace("&1", ChatColor.DARK_BLUE.toString());
			output = output.replace("&2", ChatColor.DARK_GREEN.toString());
			output = output.replace("&3", ChatColor.DARK_AQUA.toString());
			output = output.replace("&4", ChatColor.DARK_RED.toString());
			output = output.replace("&5", ChatColor.DARK_PURPLE.toString());
			output = output.replace("&6", ChatColor.GOLD.toString());
			output = output.replace("&7", ChatColor.GRAY.toString());
			output = output.replace("&8", ChatColor.DARK_GRAY.toString());
			output = output.replace("&9", ChatColor.BLUE.toString());

			output = output.replace("&a", ChatColor.GREEN.toString());
			output = output.replace("&A", ChatColor.GREEN.toString());

			output = output.replace("&b", ChatColor.AQUA.toString());
			output = output.replace("&B", ChatColor.AQUA.toString());

			output = output.replace("&c", ChatColor.RED.toString());
			output = output.replace("&C", ChatColor.RED.toString());

			output = output.replace("&d", ChatColor.LIGHT_PURPLE.toString());
			output = output.replace("&D", ChatColor.LIGHT_PURPLE.toString());

			output = output.replace("&e", ChatColor.YELLOW.toString());
			output = output.replace("&E", ChatColor.YELLOW.toString());

			output = output.replace("&f", ChatColor.WHITE.toString());
			output = output.replace("&F", ChatColor.WHITE.toString());
		}

		if (player.hasPermission("lpc.magic")) {
			//translate magic
			output = output.replace("&k", ChatColor.MAGIC.toString());
			output = output.replace("&K", ChatColor.MAGIC.toString());
		}
		if(player.hasPermission("lpc.bold")) {
			//translate bold
			output = output.replace("&l", ChatColor.BOLD.toString());
			output = output.replace("&L", ChatColor.BOLD.toString());
		}
		if(player.hasPermission("lpc.underline")) {
			//translate underline
			output = output.replace("&n", ChatColor.UNDERLINE.toString());
			output = output.replace("&N", ChatColor.UNDERLINE.toString());
		}
		if(player.hasPermission("lpc.italics")) {
			//translate italics
			output = output.replace("&o", ChatColor.ITALIC.toString());
			output = output.replace("&O", ChatColor.ITALIC.toString());
		}
		if(player.hasPermission("lpc.strikethrough")) {
			//translate strikethrough
			output = output.replace("&m", ChatColor.STRIKETHROUGH.toString());
			output = output.replace("&M", ChatColor.STRIKETHROUGH.toString());

		}
		if(player.hasPermission("lpc.reset")){
			//translate reset
			output = output.replace("&r", ChatColor.RESET.toString());
			output = output.replace("&R", ChatColor.RESET.toString());
		}

		if(player.hasPermission("lpc.rgbcodes")){
			output = translateHexColorCodes(output);
		}

		return output;
	}

	private String translateHexColorCodes(final String message) {
		final char colorChar = ChatColor.COLOR_CHAR;

		final Matcher matcher = HEX_PATTERN.matcher(message);
		final StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);

		while (matcher.find()) {
			final String group = matcher.group(1);

			matcher.appendReplacement(buffer, colorChar + "x"
					+ colorChar + group.charAt(0) + colorChar + group.charAt(1)
					+ colorChar + group.charAt(2) + colorChar + group.charAt(3)
					+ colorChar + group.charAt(4) + colorChar + group.charAt(5));
		}

		return matcher.appendTail(buffer).toString();
	}
}