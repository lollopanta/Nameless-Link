package com.namelessmc.bot.commands;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public abstract class Command {

	private final String label;
	private final List<String> aliases;
	private final CommandContext context;

	private static final Map<String, Command> registeredCommands = new HashMap<>();
	public static Map<String, Command> getRegisteredCommands() { return registeredCommands; }
	private static final List<String> registeredCommandLabels = new ArrayList<>();

	private static final Logger LOGGER = LoggerFactory.getLogger("Command parser");

	public Command(final String label, final List<String> aliases, final CommandContext context) {
		this.label = label;
		this.aliases = aliases;
		this.context = context;

		// check for duplicate labels or aliases
		if (registeredCommandLabels.contains(label)) {
			throw new IllegalStateException("Command already registered. Label: " + label);
		}
		if (registeredCommandLabels.stream().anyMatch(aliases::contains)) {
			throw new IllegalStateException("Command already registered. Label: " + label);
		}

		// add these labels and aliases to check for duplication next time
		registeredCommandLabels.add(label);
		registeredCommandLabels.addAll(aliases);

		// register the command and aliases
		registeredCommands.put(label, this);
		for (final String alias : aliases) {
			registeredCommands.put(alias, this);
		}
	}

	public String getLabel() {
		return this.label;
	}

	public List<String> getAliases(){
		return this.aliases;
	}

	public CommandContext getContext() {
		return this.context;
	}

	protected abstract void execute(User user, String[] args, Message message);

	public static void execute(final Message message) {
		final String commandPrefix = getPrefix(message);
		final String messageContent = message.getContentRaw();

		final CommandContext context = getContext(message);
		if (!messageContent.startsWith(commandPrefix) && context != CommandContext.PRIVATE_MESSAGE) {
			if (context == CommandContext.PRIVATE_MESSAGE) {
				sendHelp(commandPrefix, message);
			}
			return;
		}

		final String[] splitMessage = messageContent.replaceFirst(commandPrefix, "").split(" ");
		final String commandName = splitMessage[0];
		final String[] args = Arrays.copyOfRange(splitMessage, 1, splitMessage.length);

		final User user = message.getAuthor();


		final Command command = Command.getCommand(commandName, context);

		if (command == null) {
			sendHelp(commandPrefix, message);
			return;
		}

		message.getChannel().sendTyping().queue();
		LOGGER.info("User %s#%s ran command %s", user.getName(), user.getDiscriminator(), command.getLabel());
		command.execute(user, args, message);
	}

	private static void sendHelp(final String commandPrefix, final Message originalMessage) {
		final Language language = Language.getDefaultLanguage();
		final String s = language.get(Term.INVALID_COMMAND, "commands",
				"`" + commandPrefix + String.join("`, `" + commandPrefix, registeredCommandLabels) + "`");
		originalMessage.reply(Main.getEmbedBuilder().clear().setColor(Color.GREEN)
				.setTitle(language.get(Term.COMMANDS))
				.addField(language.get(Term.HELP), s, false).build()).queue();
	}

	private static CommandContext getContext(final Message message) {
		if (message.getChannel() instanceof PrivateChannel) {
			return CommandContext.PRIVATE_MESSAGE;
		} else if (message.getChannel() instanceof TextChannel) {
			return CommandContext.GUILD_MESSAGE;
		} else {
			throw new IllegalArgumentException("Unknown Channel instance");
		}
	}

	public static Command getCommand(final String label, final CommandContext context) {
		for (final Command command : registeredCommands.values()) {
			if (command.getLabel().equals(label)) {
				if (checkContext(command.getContext(), context)) {
					return command;
				}
			} else {
				if (command.getAliases().contains(label)) {
					if (checkContext(command.getContext(), context)) {
						return command;
					}
				}
			}
		}
		return null;
	}

	private static boolean checkContext(final CommandContext givenContext, final CommandContext receivedContext) {
		return givenContext == receivedContext || givenContext == CommandContext.BOTH;
	}

	public static String getPrefix(final Message message) {
		return getContext(message).equals(CommandContext.PRIVATE_MESSAGE) ? Main.getDefaultCommandPrefix() : getGuildPrefix(message.getGuild());
	}

	public static String getGuildPrefix(final Guild guild) {
		return getGuildPrefix(guild.getIdLong());
	}

	public static String getGuildPrefix(final long guildId) {
		try {
			return Main.getConnectionManager().getCommandPrefixByGuildId(guildId).orElse(Main.getDefaultCommandPrefix());
		} catch (final BackendStorageException e) {
			e.printStackTrace();
		}
		return Main.getDefaultCommandPrefix();
	}
}
