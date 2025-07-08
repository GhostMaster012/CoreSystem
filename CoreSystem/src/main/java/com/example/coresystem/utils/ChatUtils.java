package com.example.coresystem.utils;

import com.example.coresystem.CoreSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
// import org.bukkit.entity.Player; // Not directly needed if MessageManager handles player context

import java.util.HashMap;
import java.util.Map;

public class ChatUtils {

    // No longer static final prefixes, they come from MessageManager/messages.yml
    // private static final String PREFIX = ChatColor.DARK_PURPLE + "[CoreSystem] " + ChatColor.LIGHT_PURPLE;
    // private static final String ERROR_PREFIX = ChatColor.DARK_PURPLE + "[CoreSystem] " + ChatColor.RED;

    private static MessageManager getMsgManager() {
        // This assumes CoreSystem.getInstance() is reliably available.
        // Consider injecting MessageManager if this class were instantiated,
        // but for static utility methods, this is a common approach.
        CoreSystem plugin = CoreSystem.getInstance();
        if (plugin == null || plugin.getMessageManager() == null) {
            // Fallback or error if MessageManager isn't ready - should not happen in normal operation
            // For simplicity, this might lead to NullPointerExceptions if called too early.
            // A robust solution might queue messages or have a default non-YAML message system.
            System.err.println("[CoreSystem] ChatUtils: MessageManager not available!");
            return null; // Or throw an exception
        }
        return plugin.getMessageManager();
    }

    // Send a message from messages.yml using its path
    public static void sendMessage(CommandSender sender, String messagePath, Map<String, String> placeholders) {
        MessageManager mm = getMsgManager();
        if (mm != null) {
            mm.sendMessage(sender, messagePath, placeholders);
        } else { // Fallback to direct message if MessageManager failed (should not happen)
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d[CoreSystem] &f" + messagePath)); // Basic fallback
        }
    }

    public static void sendMessage(CommandSender sender, String messagePath) {
        sendMessage(sender, messagePath, new HashMap<>());
    }

    // Send an error message from messages.yml
    public static void sendErrorMessage(CommandSender sender, String messagePath, Map<String, String> placeholders) {
         MessageManager mm = getMsgManager();
        if (mm != null) {
            mm.sendErrorMessage(sender, messagePath, placeholders);
        } else {
             sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[CoreSystem Error] &f" + messagePath)); // Basic fallback
        }
    }
    public static void sendErrorMessage(CommandSender sender, String messagePath) {
        sendErrorMessage(sender, messagePath, new HashMap<>());
    }


    // Colorize a raw string (can still be useful for on-the-fly messages not in messages.yml)
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Send a raw, already colored message, but prefixed.
    // This is for messages constructed directly in code that still need the standard prefix.
    public static void sendPrefixedRawMessage(CommandSender sender, String rawMessage) {
        MessageManager mm = getMsgManager();
        if (mm != null) {
            // This uses the prefix from MessageManager
            sender.sendMessage(mm.getPrefixedMessage("raw_message_placeholder", null, Map.of("message", rawMessage)).replace("{message}", rawMessage) );
            // A bit hacky, ideally MessageManager would have a method for this
            // Or, a more direct way:
            // sender.sendMessage(mm.getPrefix() + rawMessage); // If MessageManager exposed getPrefix()
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d[CoreSystem] &r" + rawMessage));
        }
    }
     public static void sendErrorPrefixedRawMessage(CommandSender sender, String rawMessage) {
        MessageManager mm = getMsgManager();
        if (mm != null) {
            sender.sendMessage(mm.getErrorPrefixedMessage("raw_error_placeholder", null, Map.of("message", rawMessage)).replace("{message}", rawMessage) );
            // sender.sendMessage(mm.getErrorPrefix() + rawMessage); // If MessageManager exposed getErrorPrefix()
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[CoreSystem Error] &r" + rawMessage));
        }
    }


    // Convenience for creating placeholder maps quickly
    public static Map<String, String> params(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide an even number of arguments for key-value pairs.");
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put("{" + keyValuePairs[i] + "}", keyValuePairs[i+1]); // Store keys with {} for direct replacement
                                                                    // Or store without {} and add them in replacePlaceholders
                                                                    // Storing with {} here requires replacePlaceholders to expect that.
                                                                    // Let's adjust: MessageManager expects keys WITHOUT {}.
            map.put(keyValuePairs[i], keyValuePairs[i+1]);
        }
        return map;
    }


}
