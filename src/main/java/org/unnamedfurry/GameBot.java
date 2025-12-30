package org.unnamedfurry;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class GameBot {
    private static final Logger log = LoggerFactory.getLogger(GameBot.class);

    public void base64encode(SlashCommandInteractionEvent event) throws Exception {
        Message.Attachment attachment = Objects.requireNonNull(event.getOption("attachment")).getAsAttachment();
        File file = new File("/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-b64e."+attachment.getFileExtension());
        attachment.getProxy().downloadToFile(file).join();
        byte[] src = Files.readAllBytes(Path.of("/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-b64e."+attachment.getFileExtension()));
        String base64encoded = Base64.getEncoder().encodeToString(src);
        File output = new File("/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-b64o.txt");
        Files.writeString(output.toPath(), base64encoded);
        event.reply("Файл в кодировке base64: ").addFiles(FileUpload.fromData(output, "output.txt")).setEphemeral(true).queue(
                s -> {
                    if (file.delete()){return;}
                    else {log.error("Cannot delete file!!! Code - 1. File: {}{}", attachment.getFileName(), attachment.getFileExtension());}
                    if (output.delete()){return;}
                    else {log.error("Cannot delete file!!! Code - 2. File: {}{}", attachment.getFileName(), attachment.getFileExtension());}
                }
        );
    }

    public void base64decode(SlashCommandInteractionEvent event) throws Exception {
        Message.Attachment attachment = Objects.requireNonNull(event.getOption("base64encoded")).getAsAttachment();
        File file = new File("/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-b64d.txt");
        attachment.getProxy().downloadToFile(file).join();
        String requestingType = Objects.requireNonNull(event.getOption("filetype")).getAsString();
        List<String> FILE_EXTENSIONS = List.of("txt", "doc", "png", "jpeg", "jpg", "mp4", "mkv", "mov", "webm", "gif", "wav", "mp3", "m4a", "aac");
        if (!FILE_EXTENSIONS.contains(requestingType)){
            event.reply("Неподдерживаемый формат файла. Текущие поддерживаемые форматы: txt, doc, png, jpeg, jpg, mp4, mkv, mov, webm, gif, wav, mp3, m4a, aac.").setEphemeral(true).queue();
            return;
        }
        byte[] src = Base64.getDecoder().decode(Files.readString(file.toPath()));
        File output = new File("/root/DiscordBot/tempFiles/"+event.getUser().getId()+"."+requestingType);
        Files.write(output.toPath(), src);
        FileUpload upload = FileUpload.fromData(output, "output."+requestingType);
        event.reply("Success!").addFiles(upload).setEphemeral(true).queue(
                s -> {
                    if (file.delete()){return;}
                    else {log.error("Cannot delete file!!! Code - 3. File: {}{}", attachment.getFileName(), attachment.getFileExtension());}
                    if (output.delete()){return;}
                    else {log.error("Cannot delete file!!! Code - 4. File: {}{}", attachment.getFileName(), attachment.getFileExtension());}
                }
        );
    }
}
