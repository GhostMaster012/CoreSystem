package com.example.coresystem.utils;

import com.example.coresystem.CoreSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI; // Import PAPI

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageManager {

    private final CoreSystem plugin;
    private FileConfiguration messagesConfig;
    private String prefix;
    private String errorPrefix;
    private final Pattern placeholderPattern = Pattern.compile("\\{([^}]+)\\}"); // Pattern for {placeholder}

    public MessageManager(CoreSystem plugin) {
        this.plugin = plugin;
        loadMessages();
        this.prefix = getRawMessage("plugin-prefix", "&d[CoreSystem] ");
        this.errorPrefix = getRawMessage("error-prefix", "&c[CoreSystem Error] ");
    }

    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultConfigStream = plugin.getResource("messages.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    private String getRawMessage(String path, String defaultValue) {
        String msg = messagesConfig.getString(path, defaultValue);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private List<String> getRawMessageList(String path, List<String> defaultValue) {
        List<String> msgs = messagesConfig.getStringList(path);
        if (msgs == null || msgs.isEmpty()) {
            return defaultValue.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
        }
        return msgs.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
    }


    public String getMessage(String path, Map<String, String> placeholders) {
        return getMessage(path, null, placeholders);
    }

    public String getMessage(String path, Player playerContext, Map<String, String> placeholders) {
        String message = getRawMessage(path, "&cMissing message: " + path);
        message = replacePlaceholders(message, placeholders);
        if (playerContext != null && plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(playerContext, message);
        }
        return message;
    }

    public List<String> getMessageList(String path, Player playerContext, Map<String, String> placeholders) {
        List<String> messages = getRawMessageList(path, List.of("&cMissing message list: " + path));
        List<String> processedMessages = new ArrayList<>();
        for (String line : messages) {
            String processedLine = replacePlaceholders(line, placeholders);
            if (playerContext != null && plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                processedLine = PlaceholderAPI.setPlaceholders(playerContext, processedLine);
            }
            processedMessages.add(processedLine);
        }
        return processedMessages;
    }


    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }
        Matcher matcher = placeholderPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = placeholders.getOrDefault(key, matcher.group(0)); // Keep original if no replacement
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement)); // Use quoteReplacement for safety
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
         Player playerContext = (sender instanceof Player) ? (Player) sender : null;
        String message = prefix + getMessage(path, playerContext, placeholders);
        sender.sendMessage(message);
    }

    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, new HashMap<>());
    }

    public void sendErrorMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        Player playerContext = (sender instanceof Player) ? (Player) sender : null;
        String message = errorPrefix + getMessage(path, playerContext, placeholders);
        sender.sendMessage(message);
    }
     public void sendErrorMessage(CommandSender sender, String path) {
        sendErrorMessage(sender, path, new HashMap<>());
    }

    // Send list of messages (e.g. for help commands)
    public void sendMessageList(CommandSender sender, String path, Map<String, String> placeholders) {
        Player playerContext = (sender instanceof Player) ? (Player) sender : null;
        List<String> messages = getMessageList(path, playerContext, placeholders);
        for(String msg : messages){
            sender.sendMessage(msg); // Prefix is usually not applied to each line of a multi-line message
        }
    }
     public void sendMessageList(CommandSender sender, String path) {
        sendMessageList(sender, path, new HashMap<>());
    }


    // Specific message getters with prefixing, useful for non-command feedback
    public String getPrefixedMessage(String path, Player playerContext, Map<String, String> placeholders) {
        return prefix + getMessage(path, playerContext, placeholders);
    }
    public String getPrefixedMessage(String path, Player playerContext) {
        return prefix + getMessage(path, playerContext, new HashMap<>());
    }
     public String getPrefixedMessage(String path) {
        return prefix + getMessage(path, null, new HashMap<>());
    }

    public String getErrorPrefixedMessage(String path, Player playerContext, Map<String, String> placeholders) {
        return errorPrefix + getMessage(path, playerContext, placeholders);
    }
     public String getErrorPrefixedMessage(String path, Player playerContext) {
        return errorPrefix + getMessage(path, playerContext, new HashMap<>());
    }
    public String getErrorPrefixedMessage(String path) {
        return errorPrefix + getMessage(path, null, new HashMap<>());
    }
}
