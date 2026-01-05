package org.unnamedfurry;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.*;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateAvatarEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideCreateEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideDeleteEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideUpdateEvent;
import net.dv8tion.jda.api.events.guild.update.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.ImageProxy;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EventListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(EventListener.class);
    TextCommands textCommands = new TextCommands();
    SlashCommands slashCommands = new SlashCommands();
    MusicBot musicBot = new MusicBot();
    EmbedBot ticketBot = new EmbedBot();
    TenzraTicketBot tenzraTicketBot = new TenzraTicketBot();
    GameBot gameBot = new GameBot();
    LinkedHashMap<String, String> messageHistory = new LinkedHashMap<String, String>(1000, 0.5f, false){
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 1000;
        }
    };

    @Override
    public void onMessageReceived(MessageReceivedEvent messageEvent){
        if (messageEvent.getAuthor().isBot()) return;

        Message message = messageEvent.getMessage();
        String content = message.getContentRaw();
        MessageChannel channel = message.getChannel();

        if (content.equals("!ping")) {
            channel.sendMessage("Pong!").queue();
        } else if (content.startsWith("!avatar")) {
            String[] contentFormatted = content.split(" ");
            if (contentFormatted.length == 2){
                textCommands.avatarCommand(contentFormatted, channel, message);
            } else {
                String[] contentFormatted2 = new String[2];
                contentFormatted2[0] = contentFormatted[0];
                contentFormatted2[1] = messageEvent.getAuthor().getId();
                textCommands.avatarCommand(contentFormatted2, channel, message);
            }
        } else if(content.startsWith("!ban")){
            textCommands.banCommand(channel, message, content);
        } else if(content.startsWith("!unban")){
            textCommands.unbanCommand(channel, message, content);
        } else if (content.startsWith("!clear")){
            textCommands.clearMessages(channel, message, content);
        } else if (content.startsWith("!whitelistRole")){
            textCommands.whitelistRole(message, channel, content);
        } else if (content.startsWith("!kick")){
            textCommands.kickCommand(channel, message, content);
        } else if (content.startsWith("!timeout")) {
            textCommands.timeoutCommand(channel, message, content);
        } else if (content.equals("?clearCommands")) {
            textCommands.clearCommands(channel, messageEvent);
        } else if (content.equals("?registerCommands")) {
            textCommands.registerCommands(channel, messageEvent);
        } else if (content.startsWith("?enableLogger")) {
            String[] args = content.split(" ");
            if (args.length == 7) {
                textCommands.enableLogger(message, channel, messageEvent, args[1], args[2], args[3], args[4], args[5], args[6]);
            } else if (args.length == 1) {
                textCommands.enableLogger(message, channel, messageEvent);
            } else {
                channel.sendMessage("Неправильное использование команды. Правильно: `?enableLogger userChannelId messageChannelId permissionChannelId channelChannelId guildChannelId roleChannelId`.").queue();
            }
        } else if (content.startsWith("?disableLogger")) {
            textCommands.disableLogger(message, channel, messageEvent);
        } else if (content.startsWith("?updateLogger")) {
            String[] args = content.split(" ");
            if (args.length == 7) {
                textCommands.updateLogger(message, channel, messageEvent, args[1], args[2], args[3], args[4], args[5], args[6]);
            } else {
                channel.sendMessage("Неправильное использование команды. Правильно: `?editLogger userChannelId messageChannelId permissionChannelId channelChannelId guildChannelId roleChannelId`.").queue();
            }
        } else if (content.startsWith("!play")) {
            musicBot.play(message);
        } else if (content.startsWith("!stop")) {
            musicBot.stop(message);
        } else if (content.startsWith("!pause")) {
            musicBot.pause(message);
        } else if (content.startsWith("!skip")) {
            musicBot.skip(message);
        } else if (content.startsWith("!wipeQueue")) {
            musicBot.clear(message);
        } else if (content.startsWith("!queue")) {
            musicBot.queue(message);
        } else if (content.startsWith("!help") || content.startsWith("!usage")) {
            textCommands.HelpCommand(channel, message);
        } else {
            String key = messageEvent.getGuild().getId() + "-" + message.getId();
            String value = message.getContentRaw() + "ʩ" + messageEvent.getAuthor().getId();
            messageHistory.put(key, value);
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        MessageChannel channel = event.getChannel();
        BotLauncher bot = new BotLauncher();

        if (event.getName().equals("help")){
            String a = slashCommands.HelpCommand(event);
            event.deferReply().queue();
            event.getHook().sendMessage(a).queue();
        } else if (event.getName().equals("tenzra-embed-gen")) {
            tenzraTicketBot.textEmbedGen(event);
        } else if (event.getName().equals("save-embed-template")){
            OptionMapping option1 = event.getInteraction().getOption("главный-текст");
            String mainText;
            if (option1 == null){
                mainText = "";
            } else {
                mainText = option1.getAsString();
            }
            OptionMapping option2 = event.getInteraction().getOption("основной-текст");
            String regularText;
            if (option2 == null){
                regularText = "";
            } else {
                regularText = option2.getAsString();
            }
            OptionMapping option3 = event.getInteraction().getOption("файл-embed");
            Message.Attachment mainAttachment;
            if (option3 == null){
                mainAttachment = null;
            } else {
                mainAttachment = option3.getAsAttachment();
            }
            OptionMapping option4 = event.getInteraction().getOption("вложенное-имя-бота");
            String embedBotName;
            if (option4 == null){
                embedBotName = "";
            } else {
                embedBotName = option4.getAsString();
            }
            OptionMapping option5 = event.getInteraction().getOption("вложенный-главный-текст");
            String embedMainText;
            if (option5 == null){
                embedMainText = "";
            } else {
                embedMainText = option5.getAsString();
            }
            OptionMapping option6 = event.getInteraction().getOption("вложенный-основной-текст");
            String embedRegularText;
            if (option6 == null){
                embedRegularText = "";
            } else {
                embedRegularText = option6.getAsString();
            }
            OptionMapping option7 = event.getInteraction().getOption("вложенный-файл-embed");
            Message.Attachment embedAttachment;
            if (option7 == null){
                embedAttachment = null;
            } else {
                embedAttachment = option7.getAsAttachment();
            }

            ticketBot.saveEmbed(event,
                    event.getInteraction().getOption("имя-шаблона").getAsString(),
                    event.getUser(), mainText, regularText, mainAttachment, embedBotName, embedMainText, embedRegularText, embedAttachment
            );
        } else if (event.getName().equals("send-embed-template")) {
            OptionMapping option1 = event.getInteraction().getOption("имя-шаблона");
            String templateName;
            if (option1 == null){
                templateName = "";
            } else {
                templateName = option1.getAsString();
            }
            OptionMapping option2 = event.getInteraction().getOption("айди-канала");
            String channelId;
            if (option2 == null){
                channelId = "";
            } else {
                channelId = option2.getAsString();
            }
            ticketBot.sendEmbed(event, channel, templateName, channelId, event.getUser());
        } else if (event.getName().equals("delete-embed-template")){
            String templateName = Objects.requireNonNull(event.getInteraction().getOption("имя-шаблона")).getAsString();
            ticketBot.deleteSentEmbed(event, templateName);
        } else if (event.getName().equals("say")) {
            OptionMapping option2 = event.getInteraction().getOption("текст");
            String text;
            if (option2 == null){
                text = "";
            } else {
                text = option2.getAsString();
            }

            if (event.getGuild() != null && bot.presentInChannel(event.getGuild())) {
                channel.sendMessage(text).queue();
                event.reply("Успешно.").setEphemeral(true).queue();
            } else {
                event.reply(text).setEphemeral(false).queue(
                        success -> {},
                        failure -> {
                            event.reply("Не удалось отправить сообщение в этот канал.").setEphemeral(true).queue();
                        }
                );
            }
        } else if (event.getName().equals("base64encode")) {
            gameBot.base64encode(event);
        } else if (event.getName().equals("base64decode")) {
            gameBot.base64decode(event);
        } else if (event.getName().equals("binaryencode")){
            gameBot.binaryEncode(event);
        } else if (event.getName().equals("binarydecode")){
            gameBot.binaryDecode(event);
        }
    }

    //These are listeners that were written specifically for Tenzra support bot.

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event){
        if (event.getInteraction().getValues().getFirst().startsWith("node")){
            tenzraTicketBot.parseMenu(event);
        } else if (event.getInteraction().getValues().getFirst().startsWith("vo") || event.getInteraction().getValues().getFirst().startsWith("mo")) {
            tenzraTicketBot.createTicket(event);
            StringSelectMenu selectMenu = StringSelectMenu.create("tenzra-node-menu")
                    .setPlaceholder("Категория тикета: ")
                    .addOption("DE-Нода", "node-de-ticket", "Техническая поддержка по услугам, расположенным в локации Германия.", Emoji.fromUnicode("\uD83C\uDDE9\uD83C\uDDEA"))
                    .addOption("FI-Нода", "node-fi-ticket", "Техническая поддержка по услугам, расположенным в локации Финляндия.", Emoji.fromUnicode("\uD83C\uDDEB\uD83C\uDDEE"))
                    .addOption("RU-Нода", "node-ru-ticket", "Техническая поддержка по услугам, расположенным в локации Россия.", Emoji.fromUnicode("\uD83C\uDDF7\uD83C\uDDFA"))
                    .addOption("Финансовые вопросы", "ticket-finance", "Финансовая поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDCB5"))
                    .addOption("Консультационные вопросы", "ticket-consultation", "Консультационная поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDC69\uD83C\uDFFB\u200D\uD83D\uDD27"))
                    .build();
            event.getMessageChannel().getHistory().retrievePast(1).queue(messages -> {
                messages.getFirst().editMessageComponents(ActionRow.of(selectMenu)).queue();
            });
        } else if (event.getInteraction().getValues().getFirst().startsWith("de")) {
            tenzraTicketBot.createTicket(event);
            StringSelectMenu selectMenu = StringSelectMenu.create("tenzra-node-menu")
                    .setPlaceholder("Категория тикета: ")
                    .addOption("DE-Нода", "node-de-ticket", "Техническая поддержка по услугам, расположенным в локации Германия.", Emoji.fromUnicode("\uD83C\uDDE9\uD83C\uDDEA"))
                    .addOption("FI-Нода", "node-fi-ticket", "Техническая поддержка по услугам, расположенным в локации Финляндия.", Emoji.fromUnicode("\uD83C\uDDEB\uD83C\uDDEE"))
                    .addOption("RU-Нода", "node-ru-ticket", "Техническая поддержка по услугам, расположенным в локации Россия.", Emoji.fromUnicode("\uD83C\uDDF7\uD83C\uDDFA"))
                    .addOption("Финансовые вопросы", "ticket-finance", "Финансовая поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDCB5"))
                    .addOption("Консультационные вопросы", "ticket-consultation", "Консультационная поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDC69\uD83C\uDFFB\u200D\uD83D\uDD27"))
                    .build();
            event.getMessageChannel().getHistory().retrievePast(1).queue(messages -> {
                messages.getFirst().editMessageComponents(ActionRow.of(selectMenu)).queue();
            });
        } else if (event.getInteraction().getValues().getFirst().startsWith("fi")){
            tenzraTicketBot.createTicket(event);
            StringSelectMenu selectMenu = StringSelectMenu.create("tenzra-node-menu")
                    .setPlaceholder("Категория тикета: ")
                    .addOption("DE-Нода", "node-de-ticket", "Техническая поддержка по услугам, расположенным в локации Германия.", Emoji.fromUnicode("\uD83C\uDDE9\uD83C\uDDEA"))
                    .addOption("FI-Нода", "node-fi-ticket", "Техническая поддержка по услугам, расположенным в локации Финляндия.", Emoji.fromUnicode("\uD83C\uDDEB\uD83C\uDDEE"))
                    .addOption("RU-Нода", "node-ru-ticket", "Техническая поддержка по услугам, расположенным в локации Россия.", Emoji.fromUnicode("\uD83C\uDDF7\uD83C\uDDFA"))
                    .addOption("Финансовые вопросы", "ticket-finance", "Финансовая поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDCB5"))
                    .addOption("Консультационные вопросы", "ticket-consultation", "Консультационная поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDC69\uD83C\uDFFB\u200D\uD83D\uDD27"))
                    .build();
            event.getMessageChannel().getHistory().retrievePast(1).queue(messages -> {
                messages.getFirst().editMessageComponents(ActionRow.of(selectMenu)).queue();
            });
        } else if (event.getInteraction().getValues().getFirst().equals("ticket-finance")) {
            tenzraTicketBot.createTicket(event);
            StringSelectMenu selectMenu = StringSelectMenu.create("tenzra-node-menu")
                    .setPlaceholder("Категория тикета: ")
                    .addOption("DE-Нода", "node-de-ticket", "Техническая поддержка по услугам, расположенным в локации Германия.", Emoji.fromUnicode("\uD83C\uDDE9\uD83C\uDDEA"))
                    .addOption("FI-Нода", "node-fi-ticket", "Техническая поддержка по услугам, расположенным в локации Финляндия.", Emoji.fromUnicode("\uD83C\uDDEB\uD83C\uDDEE"))
                    .addOption("RU-Нода", "node-ru-ticket", "Техническая поддержка по услугам, расположенным в локации Россия.", Emoji.fromUnicode("\uD83C\uDDF7\uD83C\uDDFA"))
                    .addOption("Финансовые вопросы", "ticket-finance", "Финансовая поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDCB5"))
                    .addOption("Консультационные вопросы", "ticket-consultation", "Консультационная поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDC69\uD83C\uDFFB\u200D\uD83D\uDD27"))
                    .build();
            event.getMessageChannel().getHistory().retrievePast(1).queue(messages -> {
                messages.getFirst().editMessageComponents(ActionRow.of(selectMenu)).queue();
            });
        } else if (event.getInteraction().getValues().getFirst().equals("ticket-consultation")) {
            tenzraTicketBot.createTicket(event);
            StringSelectMenu selectMenu = StringSelectMenu.create("tenzra-node-menu")
                    .setPlaceholder("Категория тикета: ")
                    .addOption("DE-Нода", "node-de-ticket", "Техническая поддержка по услугам, расположенным в локации Германия.", Emoji.fromUnicode("\uD83C\uDDE9\uD83C\uDDEA"))
                    .addOption("FI-Нода", "node-fi-ticket", "Техническая поддержка по услугам, расположенным в локации Финляндия.", Emoji.fromUnicode("\uD83C\uDDEB\uD83C\uDDEE"))
                    .addOption("RU-Нода", "node-ru-ticket", "Техническая поддержка по услугам, расположенным в локации Россия.", Emoji.fromUnicode("\uD83C\uDDF7\uD83C\uDDFA"))
                    .addOption("Финансовые вопросы", "ticket-finance", "Финансовая поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDCB5"))
                    .addOption("Консультационные вопросы", "ticket-consultation", "Консультационная поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDC69\uD83C\uDFFB\u200D\uD83D\uDD27"))
                    .build();
            event.getMessageChannel().getHistory().retrievePast(1).queue(messages -> {
                messages.getFirst().editMessageComponents(ActionRow.of(selectMenu)).queue();
            });
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event){
        if (event.getInteraction().getButton().getCustomId().equals("close-ticket")){
            Button Abutton = Button.of(ButtonStyle.DANGER, "approve", "Да, закрыть тикет", Emoji.fromUnicode("\u2705"));
            Button Dbutton = Button.of(ButtonStyle.SUCCESS, "deny", "Нет, оставить тикет открытым", Emoji.fromUnicode("\u274C"));
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Подтвердите действие")
                    .setDescription("Вы уверены что хотите закрыть тикет?")
                    .setAuthor("Tenzra Support");
            MessageEmbed embed = builder.build();
            event.getMessageChannel().sendMessageEmbeds(embed).addComponents(ActionRow.of(Abutton, Dbutton)).queue();
        } else if (event.getInteraction().getButton().getCustomId().equals("approve")) {
            event.getMessageChannel().delete().queue();
        } else if (event.getInteraction().getButton().getCustomId().equals("deny")) {
            event.getMessageChannel().deleteMessageById(event.getMessageChannel().getLatestMessageId()).queue();
        }
    }

    //Logger section

    //User section

    @Override
    public void onGuildBan(GuildBanEvent event) {
        String[] args = isEnabled(1, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Участник <@" + event.getUser().getId() + "> был забанен.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging member's banning: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        String[] args = isEnabled(1, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Участник <@" + event.getUser().getId() + "> был разбанен.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging member's unbanning: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        String[] args = isEnabled(1, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Участник <@" + event.getMember().getId() + "> изменил ник. \nСтарый ник - `" + event.getOldNickname() + "`, новый ник - `" + event.getNewNickname() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging member's nickname changing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildMemberUpdateAvatar(GuildMemberUpdateAvatarEvent event) {
        String[] args = isEnabled(1, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                ImageProxy oldAvatar = event.getOldAvatar();
                ImageProxy newAvatar = event.getNewAvatar();
                FileUpload oldAvatar1 = oldAvatar.downloadAsFileUpload("oldAvatar.png");
                FileUpload newAvatar1 = newAvatar.downloadAsFileUpload("newAvatar.png");
                String message = "Участник <@" + event.getMember().getId() + "> изменил аватар. \nСтарый аватар - `oldAvatar.png`, новый аватар - `newAvatar.png`.";
                channel.sendMessage(message).addFiles(oldAvatar1, newAvatar1).queue();
                oldAvatar1.close();
                newAvatar1.close();
            } catch (Exception e) {
                log.error("Error logging member's avatar changing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        String[] args = isEnabled(1, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Участник <@" + event.getMember().getId() + "> зашел на сервер.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging new member's joining: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        String[] args = isEnabled(1, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Участник `" + event.getMember() + "` вышел из сервера.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging old member's leaving: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        String[] args = isEnabled(1, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                List<Role> roles = event.getRoles();
                List<String> roleIds = new ArrayList<>();
                for (Role r : roles){
                    roleIds.add("<@&" + r.getId() + ">");
                }
                String rolesStr = String.join(", ", roleIds);
                String message = "Участник <@" + event.getMember().getId() + "> получил новые роли. \nТекущие роли: " + rolesStr + ".";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging member's new role adding: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        String[] args = isEnabled(1, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                List<Role> roles = event.getRoles();
                List<String> roleIds = new ArrayList<>();
                for (Role r : roles){
                    roleIds.add("<@&" + r.getId() + ">");
                }
                String rolesStr = String.join(", ", roleIds);
                String message = "Участник <@" + event.getMember().getId() + "> потерял старые роли. \nТекущие роли: " + rolesStr + ".";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging member's old role removing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildMemberUpdateTimeOut(GuildMemberUpdateTimeOutEvent event) {
        String[] args = isEnabled(1, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Участник <@" + event.getMember().getId() + "> был отправлен в тайм-аут. \nСтарое время тайм-аута: `" + event.getOldTimeOutEnd().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "`, новое время тайм-аута: `" + event.getNewTimeOutEnd().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging member's timeout changing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    //Message section

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        String[] args = isEnabled(2, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String key = event.getGuild().getId() + "-" + event.getMessage().getId();
                if (!messageHistory.containsKey(key)){
                    log.error("Message can't be found in message cash. Key: " + key + ".");
                    return;
                }
                String[] oldMessage = messageHistory.get(key).split("ʩ");
                String newMessage = event.getMessage().getContentRaw();
                if (oldMessage[0] != null && oldMessage[1] != null){
                    String message = "Участник <@" + oldMessage[1] + "> изменил сообщение. \nСтарое сообщение - `" + oldMessage[0] + "`, новое сообщение - `" + newMessage + "`.";
                    channel.sendMessage(message).queue();
                }
            } catch (Exception e) {
                log.error("Error logging message updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        String[] args = isEnabled(2, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String key = event.getGuild().getId() + "-" + event.getMessageId();
                String[] oldMessage = messageHistory.get(key).split("ʩ");
                if (oldMessage[0] != null && oldMessage[1] != null){
                    String message = "Участник <@" + oldMessage[1] + "> удалил сообщение. \nСтарое сообщение - `" + oldMessage[0] + "`.";
                    channel.sendMessage(message).queue();
                }
            } catch (Exception e) {
                log.error("Error logging message deleting: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        String[] args = isEnabled(2, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                List<String> messageIds = event.getMessageIds();
                String guildId = event.getGuild().getId();
                for (String id : messageIds){
                    String key = guildId + "-" + id;
                    String[] oldMessage = messageHistory.get(key).split("ʩ");
                    if (oldMessage[0] != null && oldMessage[1] != null){
                        String message = "Участник <@" + oldMessage[1] + "> удалил сообщение. \nСтарое сообщение - `" + oldMessage[0] + "`.";
                        channel.sendMessage(message).queue();
                    }
                }
            } catch (Exception e) {
                log.error("Error logging message bulk-deleting: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    //Permission section

    private String formatPermissions(EnumSet<Permission> set) {
        if (set.isEmpty())
            return " ";

        return set.stream()
                .map(Permission::getName)
                .collect(Collectors.joining(", "));
    }

    @Override
    public void onPermissionOverrideCreate(PermissionOverrideCreateEvent event) {
        String[] args = isEnabled(3, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                EnumSet<Permission> newAllowSet = event.getPermissionOverride().getAllowed();
                EnumSet<Permission> newDenySet = event.getPermissionOverride().getDenied();
                String newPermissions = "+ Разрешено: " + formatPermissions(newAllowSet) + "\n" +
                                "- Запрещено: " + formatPermissions(newDenySet);
                String message = "Участник `" + event.getMember() + "` изменил разрешения для `" + event.getPermissionHolder() + "`. \nНовые разрешения: \n" + newPermissions + "\n.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging permission override creating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onPermissionOverrideUpdate(PermissionOverrideUpdateEvent event) {
        String[] args = isEnabled(3, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                EnumSet<Permission> oldAllowSet = event.getOldAllow();
                EnumSet<Permission> oldDenySet = event.getOldDeny();
                String oldPermissions = "+ Разрешено: " + formatPermissions(oldAllowSet) + "\n" +
                        "- Запрещено: " + formatPermissions(oldDenySet);
                EnumSet<Permission> newAllowSet = event.getPermissionOverride().getAllowed();
                EnumSet<Permission> newDenySet = event.getPermissionOverride().getDenied();
                String newPermissions = "+ Разрешено: " + formatPermissions(newAllowSet) + "\n" +
                        "- Запрещено: " + formatPermissions(newDenySet);
                String message = "Участник `" + event.getMember() + "` изменил разрешения для `" + event.getPermissionHolder() + "`. \nСтарые разрешения: \n" + oldPermissions + "\n, новые разрешения: \n" + newPermissions + "\n.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging permission override updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onPermissionOverrideDelete(PermissionOverrideDeleteEvent event) {
        String[] args = isEnabled(3, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                EnumSet<Permission> oldAllowSet = event.getPermissionOverride().getAllowed();
                EnumSet<Permission> oldDenySet = event.getPermissionOverride().getDenied();
                String oldPermissions = "+ Разрешено: " + formatPermissions(oldAllowSet) + "\n" +
                        "- Запрещено: " + formatPermissions(oldDenySet);
                String message = "Участник `" + event.getMember() + "` удалил разрешения для `" + event.getPermissionHolder() + "`. \nСтарые разрешения: \n" + oldPermissions + "\n.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging permission override deleting: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    //Channel section

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        String[] args = isEnabled(4, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Создан новый канал. \nНазвание - `" + event.getChannel().getName() + "`, айди - `" + event.getChannel().getId() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging new channel creating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        String[] args = isEnabled(4, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Удален старый канал. \nНазвание - `" + event.getChannel().getName() + "`, айди - `" + event.getChannel().getId() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging new channel deleting: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onChannelUpdateBitrate(ChannelUpdateBitrateEvent event) {
        String[] args = isEnabled(4, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Изменен битрейт канала. \nНазвание - `" + event.getChannel().getName() + "`, айди - `" + event.getChannel().getId() + "`, старый битрейт - `" + event.getOldValue() + "`, новый битрейт - `" + event.getNewValue() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging channel bitrate changing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        String[] args = isEnabled(4, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Изменено название канала. \nСтарое название - `" + event.getOldValue() + "`, новое название - `" + event.getNewValue() + "`, айди - `" + event.getChannel().getId() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging channel name editing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onChannelUpdateRegion(ChannelUpdateRegionEvent event) {
        String[] args = isEnabled(4, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Изменен регион канала. \nНазвание - `" + event.getChannel().getName() + "`, айди - `" + event.getChannel().getId() + "`, старый регион - `" + event.getOldValue() + "`, новый регион - `" + event.getOldValue() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging channel region editing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onChannelUpdateSlowmode(ChannelUpdateSlowmodeEvent event) {
        String[] args = isEnabled(4, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Изменен slowmode канала. \nНазвание - `" + event.getChannel().getName() + "`, айди - `" + event.getChannel().getId() + "`, старый режим - `" + event.getOldValue() + ", новый режим - `" + event.getNewValue() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging channel slowmode updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onChannelUpdateVoiceStatus(ChannelUpdateVoiceStatusEvent event) {
        String[] args = isEnabled(4, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Изменен статус канала. \nНазвание - `" + event.getChannel().getName() + "`, айди - `" + event.getChannel().getId() + "`, старое значение - `" + event.getOldValue() + "`, новое значение - `" + event.getNewValue() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging channel status updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onChannelUpdateType(ChannelUpdateTypeEvent event) {
        String[] args = isEnabled(4, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Изменен тип канала. \nНазвание - `" + event.getChannel().getName() + "`, айди - `" + event.getChannel().getId() + "`, старое значение - `" + event.getOldValue() + "`, новое значение - `" + event.getNewValue() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging channel type updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    //Guild section

    @Override
    public void onGuildUpdateAfkChannel(GuildUpdateAfkChannelEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен AFK канал. \nСтарый канал - `" + event.getOldAfkChannel() + "`, новый канал - `" + event.getNewAfkChannel() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging AFK channel updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateSystemChannel(GuildUpdateSystemChannelEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен системный канал. \nСтарый канал - `" + event.getOldSystemChannel() + "`, новый канал - `" + event.getNewSystemChannel() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging system channel updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateRulesChannel(GuildUpdateRulesChannelEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен правовой канал. \nСтарый канал - `" + event.getOldRulesChannel() + "`, новый канал - `" + event.getNewRulesChannel() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging rules channel updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateSafetyAlertsChannel(GuildUpdateSafetyAlertsChannelEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен канал оповещений безопасности. \nСтарый канал - `" + event.getOldSafetyAlertsChannel() + "`, новый канал - `" + event.getNewSafetyAlertsChannel() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging safety alerts channel updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateAfkTimeout(GuildUpdateAfkTimeoutEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен таймаут афк. \nСтарое значение - `" + event.getOldAfkTimeout() + "`, новое значение - `" + event.getNewAfkTimeout() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging AFK timeout updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateIcon(GuildUpdateIconEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                ImageProxy oldAvatar = event.getOldIcon();
                ImageProxy newAvatar = event.getNewIcon();
                FileUpload oldAvatar1 = oldAvatar.downloadAsFileUpload("oldIcon.png");
                FileUpload newAvatar1 = newAvatar.downloadAsFileUpload("newIcon.png");
                String message = "Изменена иконка сервера. \nСтарый аватар - `oldIcon.png`, новый аватар - `newIcon.png`.";
                channel.sendMessage(message).addFiles(oldAvatar1, newAvatar1).queue();
                oldAvatar1.close();
                newAvatar1.close();
            } catch (Exception e) {
                log.error("Error logging server's icon changing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateMFALevel(GuildUpdateMFALevelEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен уровень MFA. \nСтарый уровень - `" + event.getOldMFALevel() + "`, новый уровень - `" + event.getNewMFALevel() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging MFA level updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateName(GuildUpdateNameEvent event){
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлено название сервера. \nСтарое название - `" + event.getOldName() + "`, новое название - `" + event.getNewName() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging server's name updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateNotificationLevel(GuildUpdateNotificationLevelEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен уровень уведомлений. \nСтарый уровень - `" + event.getOldNotificationLevel() + "`, новый уровень - `" + event.getNewNotificationLevel() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging notification level updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateOwner(GuildUpdateOwnerEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен владелец сервера. \nСтарый владелец - `<@" + event.getOldOwnerId() + ">`, новый владелец - `<@" + event.getNewOwnerId() + ">`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging server's owner updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateVerificationLevel(GuildUpdateVerificationLevelEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен уровень верификации. \nСтарый уровень - `" + event.getOldVerificationLevel() + "`, новый уровень - `" + event.getNewVerificationLevel() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging verification level updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateLocale(GuildUpdateLocaleEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен язык. \nСтарый язык - `" + event.getOldValue() + "`, новый язык - `" + event.getNewValue() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging server's locale updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateBanner(GuildUpdateBannerEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                ImageProxy oldAvatar = event.getOldBanner();
                ImageProxy newAvatar = event.getNewBanner();
                FileUpload oldAvatar1 = oldAvatar.downloadAsFileUpload("oldBanner.png");
                FileUpload newAvatar1 = newAvatar.downloadAsFileUpload("newBanner.png");
                String message = "Изменен баннер сервера. \nСтарый аватар - `oldBanner.png`, новый аватар - `newBanner.png`.";
                channel.sendMessage(message).addFiles(oldAvatar1, newAvatar1).queue();
                oldAvatar1.close();
                newAvatar1.close();
            } catch (Exception e) {
                log.error("Error logging server's banner changing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onGuildUpdateDescription(GuildUpdateDescriptionEvent event) {
        String[] args = isEnabled(5, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлено описание сервера. \nСтарое описание - `" + event.getOldDescription() + "`, новое описание - `" + event.getNewDescription() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging server's description updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    //Roles section

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        String[] args = isEnabled(6, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Создана новая роль. \nНовое название - `" + event.getRole().getName() + "`, новый айди - `" + event.getRole().getId() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging new role creating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        String[] args = isEnabled(6, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Удалена новая роль. \nСтарое название - `" + event.getRole().getName() + "`, старый айди - `" + event.getRole().getId() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging old role deleting: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onRoleUpdateColor(RoleUpdateColorEvent event) {
        String[] args = isEnabled(6, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен цвет роли. \nНазвание - `" + event.getRole().getName() + "`, айди - `" + event.getRole().getId() + "`, старый цвет - `" + event.getOldColor() + "`, новый цвет - `" + event.getNewColor() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging old role color updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onRoleUpdateIcon(RoleUpdateIconEvent event) {
        String[] args = isEnabled(6, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                RoleIcon oldIcon = event.getOldIcon();
                RoleIcon newIcon = event.getNewIcon();
                String message = "Изменена иконка роли. \nНазвание - `" + event.getRole().getName() + "`, айди - `" + event.getRole().getId() + "`, старая иконка - `" + oldIcon + "`, новая иконка - `" + newIcon + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging roles's icon changing: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onRoleUpdateMentionable(RoleUpdateMentionableEvent event) {
        String[] args = isEnabled(6, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлена упоминаемость роли. \nНазвание - `" + event.getRole().getName() + "`, айди - `" + event.getRole().getId() + "`, старое значение - `" + event.getOldValue() + "`, новое значение - `" + event.getNewValue() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging old role mentionable updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    @Override
    public void onRoleUpdateName(RoleUpdateNameEvent event) {
        String[] args = isEnabled(6, event.getGuild().getId());
        if (args != null){
            try {
                MessageChannel channel = event.getGuild().getChannelById(TextChannel.class, args[1]);
                String message = "Обновлен цвет роли. \nСтарое название - `" + event.getOldName() + "`, новое название - `" + event.getNewName() + "`, айди - `" + event.getRole().getId() + "`.";
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                log.error("Error logging old role name updating: \n{}\n{}", e.getMessage(), e.getStackTrace());
            }
        }
    }

    //Checking section

    public String[] isEnabled(int index, String guildId){
        try {
            Path jarDir = Paths.get(
                    BotLauncher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();
            Path path = jarDir.resolve("loggingConfig.json");
            //Path path = Path.of("loggingConfig.json");

            String content = Files.readString(path);
            JSONObject json = new JSONObject(content);
            JSONArray array = json.getJSONArray(guildId);
            String[] args = new String[2];

            switch (index){
                case 1:
                    if (array.getBoolean(0)){
                        args[0] = "true";
                        args[1] = array.getString(1);
                        return args;
                    }
                    break;
                case 2:
                    if (array.getBoolean(0)){
                        args[0] = "true";
                        args[1] = array.getString(2);
                        return args;
                    }
                    break;
                case 3:
                    if (array.getBoolean(0)){
                        args[0] = "true";
                        args[1] = array.getString(3);
                        return args;
                    }
                    break;
                case 4:
                    if (array.getBoolean(0)){
                        args[0] = "true";
                        args[1] = array.getString(4);
                        return args;
                    }
                    break;
                case 5:
                    if (array.getBoolean(0)){
                        args[0] = "true";
                        args[1] = array.getString(5);
                        return args;
                    }
                    break;
                case 6:
                    if (array.getBoolean(0)){
                        args[0] = "true";
                        args[1] = array.getString(6);
                        return args;
                    }
                    break;
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to disable logger: \n{}\n{}", e.getMessage(), e.getStackTrace());
        }
        return null;
    }
}