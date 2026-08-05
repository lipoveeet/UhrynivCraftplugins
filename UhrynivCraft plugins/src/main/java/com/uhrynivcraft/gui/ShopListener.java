package com.uhrynivcraft.gui;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.model.TransactionType;
import com.uhrynivcraft.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class ShopListener implements Listener {

    private final UhrynivCraft plugin;

    public ShopListener(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§8Uhryniv Shop")) return;

        event.setCancelled(true); // shop menus are strictly click-to-buy/navigate, never draggable

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemMeta meta = clicked.getItemMeta();
        ShopGUI gui = plugin.getShopGUI();

        boolean isCategoryIcon = meta.getPersistentDataContainer().has(gui.getIsCategoryIconKey(), PersistentDataType.BYTE);
        if (isCategoryIcon) {
            String category = meta.getPersistentDataContainer().get(gui.getCategoryKey(), PersistentDataType.STRING);
            if (category != null) {
                gui.openCategory(player, category);
            }
            return;
        }

        Double price = meta.getPersistentDataContainer().get(gui.getPriceKey(), PersistentDataType.DOUBLE);
        if (price == null) return;

        handlePurchase(player, clicked, price);
    }

    private void handlePurchase(Player player, ItemStack item, double price) {
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName() : item.getType().name();

        if (player.getInventory().firstEmpty() == -1) {
            MessageUtil.send(player, plugin.getConfigManager().getMessage("shop-inventory-full"));
            return;
        }

        boolean success = plugin.getEconomyManager().subtractRaw(player.getUniqueId(), price,
                TransactionType.SHOP_PURCHASE, "shop: " + itemName);

        if (!success) {
            MessageUtil.send(player, MessageUtil.format(plugin.getConfigManager().getMessage("shop-not-enough-money"),
                    Map.of("item", itemName)));
            return;
        }

        // Give a clean copy without our internal PDC purchase tags
        ItemStack toGive = item.clone();
        ItemMeta cleanMeta = toGive.getItemMeta();
        cleanMeta.getPersistentDataContainer().remove(plugin.getShopGUI().getPriceKey());
        cleanMeta.getPersistentDataContainer().remove(plugin.getShopGUI().getCategoryKey());
        toGive.setItemMeta(cleanMeta);

        player.getInventory().addItem(toGive);
        MessageUtil.send(player, MessageUtil.format(plugin.getConfigManager().getMessage("shop-purchase-success"),
                Map.of("item", itemName, "price", MessageUtil.formatAmount(price))));
    }
}
