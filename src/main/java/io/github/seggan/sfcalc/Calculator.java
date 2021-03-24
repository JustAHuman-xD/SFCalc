package io.github.seggan.sfcalc;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.cscorelib2.inventory.ItemUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Copyright (C) 2020 Seggan
 * Email: segganew@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
public class Calculator {

    private final SFCalc plugin;

    public Calculator(SFCalc plugin) {
        this.plugin = plugin;
    }

    public void printResults(CommandSender sender, String command, SlimefunItem item, long amount) {
        Map<String, Long> results = calculate(item);

        String header;
        String name = ChatColor.stripColor(ItemUtils.getItemName(item.getItem()));
        if (amount == 1) {
            header = String.format(plugin.headerString, name);
        } else {
            header = Util.format(plugin.headerAmountString, amount, name);
        }

        sender.sendMessage(header);

        // This will put our entries in order from lowest to highest
        List<Map.Entry<String, Long>> entries = new ArrayList<>(results.entrySet());
        entries.sort(Comparator.comparingLong(Map.Entry::getValue));

        if (command.equals("sfneeded") && sender instanceof Player) {
            Map<String, Long> inv = getInventoryAsItemList((Player) sender);

            for (Map.Entry<String, Long> entry : entries) {
                Long inInventory = inv.getOrDefault(entry.getKey(), 0L);
                long originalValues = entry.getValue() * amount - inInventory;
                String parsedAmount;
                if (originalValues <= 64) {
                    parsedAmount = Long.toString(originalValues);
                } else {
                    parsedAmount = Util.format(plugin.stackString, originalValues, (long) Math.floor(originalValues / 64.0), originalValues % 64);
                }
                sender.sendMessage(Util.format(
                    plugin.neededString, parsedAmount, WordUtils.capitalizeFully(entry.getKey())));
            }
        } else {
            for (Map.Entry<String, Long> entry : entries) {
                long originalValues = entry.getValue() * amount;
                String parsedAmount;
                if (originalValues <= 64) {
                    parsedAmount = Long.toString(originalValues);
                } else {
                    parsedAmount = Util.format(plugin.stackString, originalValues, (long) Math.floor(originalValues / 64.0), originalValues % 64);
                }
                sender.sendMessage(Util.format(
                    plugin.amountString, parsedAmount, WordUtils.capitalizeFully(entry.getKey())));
            }
        }
    }

    private Map<String, Long> getInventoryAsItemList(Player player) {
        Map<String, Long> inv = new HashMap<>();

        for (ItemStack item : player.getInventory().getContents()) {
            // if the Item is null or air, it will return null too
            if (item == null) {
                continue;
            }

            add(inv, ChatColor.stripColor(ItemUtils.getItemName(item)), item.getAmount());
        }

        return inv;
    }

    private Map<String, Long> calculate(SlimefunItem item) {
        Map<String, Long> result = new HashMap<>();
        Map<String, Map<String, Long>> calculated = new HashMap<>(); //stores names that are already calculated for reference

        for (ItemStack i : item.getRecipe()) {
            if (i == null) {
                // empty slot
                continue;
            }
            
            String name = ChatColor.stripColor(ItemUtils.getItemName(i));
            
            int amount = i.getAmount();

            if (calculated.containsKey(name)) { //check already calculated items
                addAll(result, calculated.get(name), amount);
                continue;
            }
            
            Map<String, Long> recipe = new HashMap<>();
            
            SlimefunItem ingredient = SlimefunItem.getByItem(i);

            if (ingredient == null) {
                // ingredient is null; it's a normal Minecraft item
                add(recipe, name, 1);
                
            } else {
                
                if (ingredient.getRecipeType().getKey().getKey().equals("metal_forge")) {
                    add(recipe, "diamond", 9);
                }

                if (plugin.blacklistedIds.contains(ingredient.getId().toLowerCase(Locale.ROOT))) {
                    // it's a blacklisted item
                    add(recipe, name, 1);
                } else if (!plugin.blacklistedRecipes.contains(ingredient.getRecipeType())) {
                    // item is a crafted Slimefun item; get its ingredients
                    addAll(recipe, calculate(ingredient), 1);
                } else {
                    // item is a dust or a geo miner resource; just add it
                    add(recipe, name, 1);
                }
            }

            calculated.put(name, recipe);
            addAll(result, recipe, amount);
            
        }

        return result;
    }

    private void add(Map<String, Long> map, String key, long amount) {
        map.merge(key.toLowerCase(), amount, Long::sum);
    }

    private void addAll(Map<String, Long> map, Map<String, Long> otherMap, long multiplier) {
        for (Map.Entry<String, Long> entry : otherMap.entrySet()) {
            add(map, entry.getKey(), entry.getValue() * multiplier);
        }
    }
}
