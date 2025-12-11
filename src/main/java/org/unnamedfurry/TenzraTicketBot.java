package org.unnamedfurry;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class TenzraTicketBot {
    final static Logger logger = LoggerFactory.getLogger(TenzraTicketBot.class);
    public void textEmbedGen(SlashCommandInteractionEvent event){
        try {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Раздел технической поддержки")
                    .setDescription("Добро пожаловать в раздел технической и консультационной поддержки.\nДля того что бы открыть тикет, выберите категорию тикета ниже:")
                    .setAuthor("Tenzra Support");
            StringSelectMenu selectMenu = StringSelectMenu.create("tenzra-node-menu")
                    .setPlaceholder("Категория тикета: ")
                    .addOption("DE-Нода", "node-de-ticket", "Техническая поддержка по услугам, расположенным в локации Германия.", Emoji.fromUnicode("\uD83C\uDDE9\uD83C\uDDEA"))
                    .addOption("FI-Нода", "node-fi-ticket", "Техническая поддержка по услугам, расположенным в локации Финляндия.", Emoji.fromUnicode("\uD83C\uDDEB\uD83C\uDDEE"))
                    .addOption("RU-Нода", "node-ru-ticket", "Техническая поддержка по услугам, расположенным в локации Россия.", Emoji.fromUnicode("\uD83C\uDDF7\uD83C\uDDFA"))
                    .addOption("Финансовые вопросы", "ticket-finance", "Финансовая поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDCB5"))
                    .addOption("Консультационные вопросы", "ticket-consultation", "Консультационная поддержка по любым услугам.", Emoji.fromUnicode("\uD83D\uDC69\uD83C\uDFFB\u200D\uD83D\uDD27"))
                    .build();
            MessageEmbed embed = builder.build();
            event.getMessageChannel().sendMessageEmbeds(embed).addComponents(ActionRow.of(selectMenu)).queue();
            event.reply("Success!\nYour selection menu id is: " + selectMenu.getCustomId() + ". Copy it and make sure your bot monitors this menu! ;)").setEphemeral(true).queue();
        } catch (Exception e) {
            logger.error("Caught an unexpected error while generating ticket embed!: {}", e.getMessage());
        }
    }

    public void parseMenu(StringSelectInteractionEvent event){
        try {
            Guild guild = event.getGuild();
            if (guild != null){
                String value = event.getValues().getFirst();
                List<Category> tickets = guild.getCategoriesByName("tickets", true);
                if (!tickets.isEmpty()){
                    switch (value){
                        case "node-de-ticket":
                            EmbedBuilder DEbuilder = new EmbedBuilder();
                            DEbuilder.setTitle("Тип процессора")
                                    .setDescription("Для открытия тикета выберите модель вашего процессора: ")
                                    .setAuthor("Tenzra Support");
                            StringSelectMenu DEselectMenu = StringSelectMenu.create("tenzra-core-menu")
                                    .setPlaceholder("Категория процессора: ")
                                    .addOption("Ryzen 9 3900", "de-9-3900", "Техническая поддержка по услугам, имеющих процессор Ryzen 9 3900.", Emoji.fromCustom(":ryzen9:", 1439205578667720704L, false))
                                    .addOption("Ryzen 9 5950X", "de-9-5950X", "Техническая поддержка по услугам, имеющих процессор Ryzen 9 5950X.", Emoji.fromCustom(":ryzen9:", 1439205578667720704L, false))
                                    .addOption("Ryzen 9 7950x3D", "de-9-7950x3D", "Техническая поддержка по услугам, имеющих процессор Ryzen 9 7950x3D.", Emoji.fromCustom(":ryzen9:", 1439205578667720704L, false))
                                    .addOption("Ryzen 9 9950X", "de-9-9950X", "Техническая поддержка по услугам, имеющих процессор Ryzen 9 9950X.", Emoji.fromCustom(":ryzen9:", 1439205578667720704L, false))
                                    .build();
                            MessageEmbed DEembed = DEbuilder.build();
                            event.replyEmbeds(DEembed).addComponents(ActionRow.of(DEselectMenu)).setEphemeral(true).queue();
                            break;
                        case "node-fi-ticket":
                            EmbedBuilder FIbuilder = new EmbedBuilder();
                            FIbuilder.setTitle("Тип процессора")
                                    .setDescription("Для открытия тикета выберите модель вашего процессора: ")
                                    .setAuthor("Tenzra Support");
                            StringSelectMenu FIselectMenu = StringSelectMenu.create("tenzra-core-menu")
                                    .setPlaceholder("Категория процессора: ")
                                    .addOption("Ryzen 9 3900", "fi-9-3900", "Техническая поддержка по услугам, имеющих процессор Ryzen 9 3900.", Emoji.fromCustom(":ryzen9:", 1439205578667720704L, false))
                                    .addOption("Ryzen 9 5950X", "fi-9-5950X", "Техническая поддержка по услугам, имеющих процессор Ryzen 9 5950X.", Emoji.fromCustom(":ryzen9:", 1439205578667720704L, false))
                                    .build();
                            MessageEmbed FIembed = FIbuilder.build();
                            event.replyEmbeds(FIembed).addComponents(ActionRow.of(FIselectMenu)).setEphemeral(true).queue();
                            break;
                        case "node-ru-ticket":
                            EmbedBuilder RUbuilder = new EmbedBuilder();
                            RUbuilder.setTitle("Тип процессора")
                                    .setDescription("Для открытия тикета выберите модель вашего процессора: ")
                                    .setAuthor("Tenzra Support");
                            StringSelectMenu RUselectMenu = StringSelectMenu.create("tenzra-core-menu")
                                    .setPlaceholder("Категория процессора: ")
                                    .addOption("Xeon E5 2690 V3", "vo-E5-2690-V3", "Техническая поддержка по услугам, имеющих процессор Xeon E5-2690 V3, находящихся в локации Воронеж.", Emoji.fromCustom(":ryzen9:", 1439205578667720704L, false))
                                    .addOption("Xeon E5 2690 V3", "mo-E5-2690-V3", "Техническая поддержка по услугам, имеющих процессор Xeon E5-2690 V3, находящихся в локации Москва.", Emoji.fromCustom(":ryzen9:", 1439205578667720704L, false))
                                    .addOption("Xeon E5 2680 V4", "mo-E5-2680-V4", "Техническая поддержка по услугам, имеющих процессор E5 2680 V4, находящихся в локации Москва.", Emoji.fromCustom(":ryzen9:", 1439205578667720704L, false))
                                    .build();
                            MessageEmbed RUembed = RUbuilder.build();
                            event.replyEmbeds(RUembed).addComponents(ActionRow.of(RUselectMenu)).setEphemeral(true).queue();
                            break;
                    }
                } else {
                    guild.createCategory("tickets").queue();
                }
            } else {
                event.getMessageChannel().sendMessage("Вам нужно находиться на сервере для использования этой команды.").queue();
            }
        } catch (Exception e) {
            logger.error("Caught an unexpected error while parsing choose menu in tickets!: {}", e.getMessage());
        }
    }

    public void createTicket(StringSelectInteractionEvent event){
        try {
            Guild guild = event.getGuild();
            List<Category> tickets = Objects.requireNonNull(guild).getCategoriesByName("tickets", true);
            Category category = tickets.getFirst();
            String channelName = event.getUser().getName() + "-" + event.getInteraction().getValues().getFirst();
            category.createTextChannel(channelName)
                    .addPermissionOverride(Objects.requireNonNull(event.getMember()), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue(textChannel -> {
                        event.reply("Тикет создан успешно: <#" + category.getChannels().getLast().getId() + "> !").setEphemeral(true).queue();
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setTitle("Ожидайте ответа поддержки")
                                .setDescription("<@&1439323279251869915>, открыт новый тикет!")
                                .setAuthor("Tenzra Support").build();
                        Button button = Button.of(ButtonStyle.PRIMARY, "close-ticket", "Закрыть тикет", Emoji.fromUnicode("\uD83D\uDDD1\uFE0F"));
                        MessageEmbed embed = builder.build();
                        textChannel.sendMessageEmbeds(embed).addComponents(ActionRow.of(button)).queue();
                    });
        } catch (Exception e) {
            logger.error("Caught an unexpected error while creating ticket!: {}", e.getMessage());
        }
    }
}

class TenzraTicketVerification{
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "Дата: " + date + ", Время: " + time;
    }
    final static Logger logger = LoggerFactory.getLogger(TextVerification.class);
    public static boolean allowedExecAdminCommands(Message message, MessageChannel channel){
        boolean bypassedVerification = false;
        Member member = message.getMember();
        try {
            if (Objects.requireNonNull(member).getId().equals("897054945889644564") || member.hasPermission(Permission.ADMINISTRATOR) || checkRoles(message)){
                bypassedVerification = true;
            } else {
                channel.sendMessage("<@" + member.getId() + "> , у вас нет прав для выполнения этого действия! Проверьте наличие обязательных прав для выполнения или обратитесь к администратору/овнеру сервера.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                logger.info("Запрошена команда (" + message.getContentRaw() + ") участником (" + message.getAuthor() + ") без следующих прав: Администратор, Овнер или Создатель бота.");
            }
        } catch (Exception e) {
            channel.sendMessage("Произошла неивзестная ошибка при обработке команды. Обратитесь к создателю бота @unnamed_furry.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            throw new RuntimeException(e);
        }
        return bypassedVerification;
    }
    public static boolean checkRoles(Message message) throws Exception {
        Path jarDir = Paths.get(
                BotLauncher.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).getParent();
        File file = new File(String.valueOf(jarDir.resolve("whitelistedRoles.json")));
        //File file = new File("whitelistedRoles.json");

        if (file.exists()){
            String json = Files.readString(file.toPath());
            Guild guild = message.getGuild();
            JSONObject object = new JSONObject(json);
            JSONArray array = object.getJSONArray(guild.getId());
            String[] existingRolesArr = new String[array.length()];
            for (int i=0; i<array.length(); i++){
                existingRolesArr[i] = array.getString(i);
            }
            Member member = message.getMember();
            assert member != null;
            List<Role> roles = member.getRoles();
            List<String> roleIdsAsString = roles.stream()
                    .map(Role::getId)
                    .toList();

            return Arrays.stream(existingRolesArr).anyMatch(roleIdsAsString::contains);
        } else {
            return false;
        }
    }
}