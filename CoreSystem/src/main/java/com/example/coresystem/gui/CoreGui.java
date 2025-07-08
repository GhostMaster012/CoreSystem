package com.example.coresystem.gui;

import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
import com.example.coresystem.utils.ChatUtils;
import com.example.coresystem.utils.ExperienceManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
// import java.util.stream.Collectors; // Not currently needed

public class CoreGui {

    private final CoreSystem plugin;
    public static final String GUI_TITLE = ChatUtils.color("&5&lCore System Menu");
    private final DecimalFormat df = new DecimalFormat("#.#");

    public CoreGui(CoreSystem plugin) {
        this.plugin = plugin;
    }

    public void openMainCoreGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());

        boolean papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        // Slot 10: Level
        ItemStack levelItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta levelMeta = levelItem.getItemMeta();
        levelMeta.setDisplayName(ChatUtils.color("&aNivel del Núcleo"));
        List<String> levelLore = new ArrayList<>();
        if (papiEnabled) {
            levelLore.add(ChatUtils.color("&7Nivel Actual: &e%coresystem_level%"));
        } else {
            levelLore.add(ChatUtils.color("&7Nivel Actual: &e" + playerData.getLevel()));
        }
        levelLore.add(ChatUtils.color("&7XP Total Acumulado: &e" + (papiEnabled ? "%coresystem_xp_total%" : df.format(playerData.getXp()))));
        levelMeta.setLore(papiEnabled ? PlaceholderAPI.setPlaceholders(player, levelLore) : levelLore);
        levelItem.setItemMeta(levelMeta);
        gui.setItem(10, levelItem);

        // Slot 12: Salud
        ItemStack healthItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta healthMeta = healthItem.getItemMeta();
        healthMeta.setDisplayName(ChatUtils.color("&cSalud del Núcleo"));
        List<String> healthLore = new ArrayList<>();
        if (papiEnabled) {
            healthLore.add(ChatUtils.color("&7Salud: &e%coresystem_health%&7/&e%coresystem_max_health%"));
            healthLore.add(ChatUtils.color("&7Barra: %coresystem_health_bar%"));
        } else {
            healthLore.add(ChatUtils.color("&7Salud: &e" + df.format(playerData.getCurrentHealth()) + "&7/&e" + df.format(playerData.getMaxHealth())));
        }
        healthMeta.setLore(papiEnabled ? PlaceholderAPI.setPlaceholders(player, healthLore) : healthLore);
        healthItem.setItemMeta(healthMeta);
        gui.setItem(12, healthItem);

        // Slot 14: XP Progress
        ItemStack xpItem = new ItemStack(Material.BOOK);
        ItemMeta xpMeta = xpItem.getItemMeta();
        xpMeta.setDisplayName(ChatUtils.color("&eProgreso de Experiencia"));
        List<String> xpLore = new ArrayList<>();
        if (papiEnabled) {
            xpLore.add(ChatUtils.color("&7XP Actual (Nivel): &b%coresystem_xp%")); // This placeholder shows progress in current level
            xpLore.add(ChatUtils.color("&7XP para Siguiente Nivel: &b%coresystem_xp_required%"));
        } else {
            double currentLevelXp = ExperienceManager.getCurrentLevelProgressXp(playerData.getXp(), playerData.getLevel());
            double requiredForNext = ExperienceManager.getRequiredXpForLevel(playerData.getLevel());
            xpLore.add(ChatUtils.color("&7XP Actual (Nivel): &b" + df.format(currentLevelXp)));
            xpLore.add(ChatUtils.color("&7XP para Siguiente Nivel: &b" + df.format(requiredForNext)));
        }
        xpMeta.setLore(papiEnabled ? PlaceholderAPI.setPlaceholders(player, xpLore) : xpLore);
        xpItem.setItemMeta(xpMeta);
        gui.setItem(14, xpItem);

        // Slot 16: Energía
        ItemStack energyItem = new ItemStack(Material.LAPIS_BLOCK);
        ItemMeta energyMeta = energyItem.getItemMeta();
        energyMeta.setDisplayName(ChatUtils.color("&9Energía del Núcleo"));
        List<String> energyLore = new ArrayList<>();
        if (papiEnabled) {
            energyLore.add(ChatUtils.color("&7Energía: &b%coresystem_energy%&7/&b%coresystem_max_energy%"));
        } else {
            energyLore.add(ChatUtils.color("&7Energía: &b" + df.format(playerData.getEnergy()) + "&7/&b" + df.format(playerData.getMaxEnergy())));
        }
        energyMeta.setLore(papiEnabled ? PlaceholderAPI.setPlaceholders(player, energyLore) : energyLore);
        energyItem.setItemMeta(energyMeta);
        gui.setItem(16, energyItem);

        // Slot 22: Estado y Arquetipo
        ItemStack statusItem = new ItemStack(playerData.isCoreActive() ? Material.BEACON : Material.BARRIER);
        ItemMeta statusMeta = statusItem.getItemMeta();
        statusMeta.setDisplayName(ChatUtils.color("&6Estado del Núcleo"));
        List<String> statusLore = new ArrayList<>();
         if (papiEnabled) {
            statusLore.add(ChatUtils.color("&7Estado: &f%coresystem_status%"));
            statusLore.add(ChatUtils.color("&7Arquetipo: &f%coresystem_type%"));
        } else {
            String statusText = "Inactive";
            if(playerData.isCoreActive()) statusText = "Active";
            else if (playerData.hasCoreBackup()) statusText = "Destroyed (Backup Available)";
            statusLore.add(ChatUtils.color("&7Estado: &f" + statusText));
            statusLore.add(ChatUtils.color("&7Arquetipo: &f" + (playerData.getArchetype() != null ? playerData.getArchetype() : "None")));
        }
        statusMeta.setLore(papiEnabled ? PlaceholderAPI.setPlaceholders(player, statusLore) : statusLore);
        statusItem.setItemMeta(statusMeta);
        gui.setItem(22, statusItem);

        ItemStack fillerPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = fillerPane.getItemMeta();
        fillerMeta.setDisplayName(" ");
        fillerPane.setItemMeta(fillerMeta);
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, fillerPane);
            }
        }
        player.openInventory(gui);
    }
}
