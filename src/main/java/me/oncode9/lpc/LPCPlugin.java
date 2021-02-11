package me.oncode9.lpc;

import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LPCPlugin extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		saveDefaultConfig();

		if (isPlaceholderAPIEnabled())
			getLogger().info("Hooked into PlaceholderAPI.");

		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final String[] args) {
		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			reloadConfig();

			sender.sendMessage(colorize("&aThe configuration file has been reloaded."));
			return true;
		}

		return false;
	}

	@Override
	public List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String alias, final String[] args) {
		if (args.length == 1)
			return Collections.singletonList("reload");

		return new ArrayList<>();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(final AsyncPlayerChatEvent event) {
		final Player player = event.getPlayer();
		final String group = loadUser(player).getPrimaryGroup();
		final String message = event.getMessage();

		String format = Objects.requireNonNull(getConfig().getString(getConfig().getString("group-formats." + group) != null ? "group-formats." + group : "chat-format"))
				.replace("{world}", player.getWorld().getName())
				.replace("{prefix}", getPrefix(player))
				.replace("{prefixes}", getPrefixes(player))
				.replace("{name}", player.getName())
				.replace("{suffix}", getSuffix(player))
				.replace("{suffixes}", getSuffixes(player))
				.replace("{username-color}", playerMeta(player).getMetaValue("username-color") != null
						? playerMeta(player).getMetaValue("username-color") : groupMeta(group).getMetaValue("username-color") != null
						? groupMeta(group).getMetaValue("username-color") : "")
				.replace("{message-color}", playerMeta(player).getMetaValue("message-color") != null
						? playerMeta(player).getMetaValue("message-color") : groupMeta(group).getMetaValue("message-color") != null
						? groupMeta(group).getMetaValue("message-color") : "");

		format = translateHexColorCodes(colorize(isPlaceholderAPIEnabled() ? PlaceholderAPI.setPlaceholders(player, format) : format));

		event.setFormat(format.replace("{message}", player.hasPermission("lpc.colorcodes") && player.hasPermission("lpc.rgbcodes")
				? translateHexColorCodes(colorize(message)) : player.hasPermission("lpc.colorcodes") ? colorize(message) : player.hasPermission("lpc.rgbcodes")
				? translateHexColorCodes(message) : message).replace("%", "%%"));
	}

	private String colorize(final String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	private String translateHexColorCodes(final String message) {
		final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
		final char colorChar = ChatColor.COLOR_CHAR;

		final Matcher matcher = hexPattern.matcher(message);
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

	private String getPrefix(final Player player) {
		final String prefix = playerMeta(player).getPrefix();

		return prefix != null ? prefix : "";
	}

	private String getSuffix(final Player player) {
		final String suffix = playerMeta(player).getSuffix();

		return suffix != null ? suffix : "";
	}

	private String getPrefixes(final Player player) {
		final SortedMap<Integer, String> map = playerMeta(player).getPrefixes();
		final StringBuilder prefixes = new StringBuilder();

		for (final String prefix : map.values())
			prefixes.append(prefix);

		return prefixes.toString();
	}

	private String getSuffixes(final Player player) {
		final SortedMap<Integer, String> map = playerMeta(player).getSuffixes();
		final StringBuilder suffixes = new StringBuilder();

		for (final String prefix : map.values())
			suffixes.append(prefix);

		return suffixes.toString();
	}

	private CachedMetaData playerMeta(final Player player) {
		return loadUser(player).getCachedData().getMetaData(getApi().getContextManager().getQueryOptions(player));
	}

	private CachedMetaData groupMeta(final String group) {
		return loadGroup(group).getCachedData().getMetaData(getApi().getContextManager().getStaticQueryOptions());
	}

	private User loadUser(final Player player) {
		if (!player.isOnline())
			throw new IllegalStateException("Player is offline!");

		return getApi().getUserManager().getUser(player.getUniqueId());
	}

	private Group loadGroup(final String group) {
		return getApi().getGroupManager().getGroup(group);
	}

	private LuckPerms getApi() {
		final RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
		Validate.notNull(provider);
		return provider.getProvider();
	}

	private boolean isPlaceholderAPIEnabled() {
		return getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
	}
}