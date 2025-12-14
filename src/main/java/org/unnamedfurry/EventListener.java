package org.unnamedfurry;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class EventListener extends ListenerAdapter {
    TextCommands textCommands = new TextCommands();
    SlashCommands slashCommands = new SlashCommands();
    MusicBot musicBot = new MusicBot();
    EmbedBot ticketBot = new EmbedBot();
    TenzraTicketBot tenzraTicketBot = new TenzraTicketBot();

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
        } else if (content.startsWith("?clearCommands")) {
            textCommands.clearCommands(channel, messageEvent);
        } else if (content.startsWith("?registerCommands")) {
            textCommands.registerCommands(channel, messageEvent);
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
            OptionMapping option1 = event.getInteraction().getOption("айди-канала");
            String channelId;
            if (option1 == null){
                channelId = "";
            } else {
                channelId = option1.getAsString();
            }
            OptionMapping option2 = event.getInteraction().getOption("айди-сообщения");
            String messageId;
            if (option2 == null){
                messageId = "";
            } else {
                messageId = option2.getAsString();
            }
            ticketBot.deleteSentEmbed(event, channelId, messageId);
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
}