package com.example.coresystem.listeners;

import com.example.coresystem.gui.CoreGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public class GuiListener implements Listener {

    // No need for CoreSystem plugin instance if just checking title and cancelling.
    // If actions were to be taken based on clicks, then plugin instance would be needed.

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (view.getTitle().equals(CoreGui.GUI_TITLE)) {
            event.setCancelled(true); // Prevent taking items from the GUI

            // Example: If you want to handle clicks on specific items later
            // ItemStack clickedItem = event.getCurrentItem();
            // if (clickedItem != null && clickedItem.hasItemMeta()) {
            //     Player player = (Player) event.getWhoClicked();
            //     String displayName = clickedItem.getItemMeta().getDisplayName();
            //     if (displayName.contains("Close")) {
            //         player.closeInventory();
            //     }
            //     // Add more button handlers here
            // }
        }
    }
}
