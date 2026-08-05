package com.uhrynivcraft.gui;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the shop menus purely from config.yml, so server owners can add
 * categories/items without touching code. Uses PersistentDataContainer
 * tags on each ItemStack's meta to identify category + price at click time.
 */
public class ShopGUI {

    public static final String MAIN_MENU_TITLE = "§8Uhryniv Shop";
    public static final String CATEGORY_TITLE_PREFIX = "§8Uhryniv Shop » ";

    private final UhrynivCraft plugin;
    private final NamespacedKey categoryKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey isCategoryIconKey;

    /** category name -> list of items, parsed fresh from config each time a menu opens */
    private Map<String, List<ShopItem>> categoryItems = new LinkedHashMap<>();
    private Map<String, String> categoryDisplayNames = new LinkedHashMap<>();
    private Map<String, Material> categoryIcons = new LinkedHashMap<>();

    public ShopGUI(UhrynivCraft plugin) {
        this.plugin = plugin;
        this.categoryKey = new NamespacedKey(plugin, "shop_category");
        this.priceKey = new NamespacedKey(plugin, "shop_price");
        this.isCategoryIconKey = new NamespacedKey(plugin, "shop_category_icon");
        reload();
    }

    /** Re-parses categories/items from config.yml - call after /up reload. */
    public void reload() {
        categoryItems = new LinkedHashMap<>();
        categoryDisplayNames = new LinkedHashMap<>();
        categoryIcons = new LinkedHashMap<>();

        ConfigurationSection categories = plugin.getConfigManager().raw().getConfigurationSection("shop.categories");
        if (categories == null) return;

        for (String catKey : categories.getKeys(false)) {
            ConfigurationSection catSection = categories.getConfigurationSection(catKey);
            if (catSection == null) continue;

            categoryDisplayNames.put(catKey, catSection.getString("display-name", catKey));
            categoryIcons.put(catKey, Material.matchMaterial(catSection.getString("icon", "CHEST")) != null
                    ? Material.matchMaterial(catSection.getString("icon", "CHEST")) : Material.CHEST);

            List<ShopItem> items = new ArrayList<>();
            for (Map<?, ?> raw : catSection.getMapList("items")) {
                Material mat = Material.matchMaterial(String.valueOf(raw.get("material")));
                if (mat == null) continue;
                Object nameObj = raw.get("name");
                Object priceObj = raw.get("price");
                Object amountObj = raw.get("amount");
                String name = nameObj != null ? String.valueOf(nameObj) : mat.name();
                double price = (priceObj instanceof Number) ? ((Number) priceObj).doubleValue() : 0;
                int amount = (amountObj instanceof Number) ? ((Number) amountObj).intValue() : 1;
                items.add(new ShopItem(mat, name, price, amount));
            }
            categoryItems.put(catKey, items);
        }
    }

    public void openMainMenu(Player player) {
        int size = ((categoryDisplayNames.size() + 8) / 9) * 9;
        size = Math.max(9, Math.min(54, size));
        Inventory inv = Bukkit.createInventory(null, size, MAIN_MENU_TITLE);

        int slot = 0;
        for (String catKey : categoryDisplayNames.keySet()) {
            ItemStack icon = new ItemStack(categoryIcons.getOrDefault(catKey, Material.CHEST));
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(MessageUtil.color(categoryDisplayNames.get(catKey)));
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, catKey);
            meta.getPersistentDataContainer().set(isCategoryIconKey, PersistentDataType.BYTE, (byte) 1);
            icon.setItemMeta(meta);
            if (slot < inv.getSize()) {
                inv.setItem(slot, icon);
            }
            slot++;
        }
        player.openInventory(inv);
    }

    public void openCategory(Player player, String category) {
        List<ShopItem> items = categoryItems.getOrDefault(category, List.of());
        int size = Math.max(9, Math.min(54, ((items.size() + 8) / 9) * 9));
        Inventory inv = Bukkit.createInventory(null, size, CATEGORY_TITLE_PREFIX + categoryDisplayNames.getOrDefault(category, category));

        int slot = 0;
        for (ShopItem item : items) {
            if (slot >= inv.getSize()) break;
            ItemStack stack = new ItemStack(item.material(), item.amount());
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(MessageUtil.color(item.displayName()));
            meta.setLore(List.of(MessageUtil.color("&7Ціна: &b" + MessageUtil.formatAmount(item.price()) + " UP"),
                    MessageUtil.color("&eНатисніть, щоб купити")));
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, category);
            meta.getPersistentDataContainer().set(priceKey, PersistentDataType.DOUBLE, item.price());
            stack.setItemMeta(meta);
            inv.setItem(slot, stack);
            slot++;
        }
        player.openInventory(inv);
    }

    public NamespacedKey getCategoryKey() {
        return categoryKey;
    }

    public NamespacedKey getPriceKey() {
        return priceKey;
    }

    public NamespacedKey getIsCategoryIconKey() {
        return isCategoryIconKey;
    }

    public Map<String, List<ShopItem>> getCategoryItems() {
        return categoryItems;
    }
}
