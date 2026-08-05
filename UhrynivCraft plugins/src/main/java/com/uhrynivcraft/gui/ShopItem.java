package com.uhrynivcraft.gui;

import org.bukkit.Material;

/** A single purchasable entry parsed from shop.categories.<CATEGORY>.items in config.yml. */
public record ShopItem(Material material, String displayName, double price, int amount) {}
