package io.github.seggan.sfcalc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.mrCookieSlime.CSCoreLibPlugin.cscorelib2.inventory.ItemUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;

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

    public void printResults(CommandSender sender, String s, SlimefunItem item, long amount) {
        Map<String, Integer> results = calculate(item);

        sender.sendMessage(String.format(plugin.headerString, Util.capitalize(ChatColor.stripColor(item.getItemName()))));

        if (s.equals("sfneeded")) {
            List<String> sfInv = new ArrayList<>();

            for (ItemStack i : ((Player) sender).getInventory().getContents()) {
                if (i == null) {
                    continue;
                }

                SlimefunItem sfItem = SlimefunItem.getByItem(i);

                if (sfItem == null) {
                    continue;
                }

                for (int n = 0; n < i.getAmount(); n++) {
                    sfInv.add(ChatColor.stripColor(sfItem.getItemName()));
                }
            }

            for (Map.Entry<String, Integer> entry : results.entrySet()) {
                int inInventory = Collections.frequency(sfInv, entry.getKey());
                sender.sendMessage(Util.format(plugin.neededString, entry.getValue() * amount - inInventory, Util.capitalize(entry.getKey())));
            }
        } else {
            for (Map.Entry<String, Integer> entry : results.entrySet()) {
                sender.sendMessage(Util.format(plugin.amountString, entry.getValue() * amount, Util.capitalize(entry.getKey())));
            }
        }
    }

    private Map<String, Integer> calculate(SlimefunItem item) {
        Map<String, Integer> result = new HashMap<>();

        switch (item.getID().toLowerCase()) {
        case "carbon":
            add(result, "coal", 8);
            break;
        case "compressed_carbon":
            addAll(result, calculate(SlimefunItem.getByID("CARBON")), 4);
            break;
        case "reinforced_plate":
            addAll(result, calculate(SlimefunItem.getByID("REINFORCED_ALLOY_INGOT")), 8);
            break;
        case "steel_plate":
            addAll(result, calculate(SlimefunItem.getByID("STEEL_INGOT")), 8);
            break;
        default:
            for (ItemStack i : item.getRecipe()) {
                if (i == null) {
                    // empty slot
                    continue;
                }

                SlimefunItem ingredient = SlimefunItem.getByItem(i);

                if (ingredient == null) {
                    // ingredient is null; it's a normal Minecraft item
                    add(result, ItemUtils.getItemName(i));
                    continue;
                }

                if (ingredient.getRecipeType().getKey().getKey().equals("metal_forge")) {
                    add(result, "diamond", 9);
                }

                if (plugin.blacklistedIds.contains(ingredient.getID().toLowerCase())) {
                    // it's a blacklisted item
                    add(result, ChatColor.stripColor(ingredient.getItemName()));
                    continue;
                }

                if (!plugin.blacklistedRecipes.contains(ingredient.getRecipeType())) {
                    // item is a crafted Slimefun item; get its ingredients
                    addAll(result, calculate(ingredient));
                } else {
                    // item is a dust or a geo miner resource; just add it
                    add(result, ChatColor.stripColor(ingredient.getItemName()));
                }
            }
        }

        return result;
    }

    private void add(Map<String, Integer> map, String key) {
        add(map, key, 1);
    }

    private void add(Map<String, Integer> map, String key, int amount) {
        map.merge(key, amount, Integer::sum);
    }

    private void addAll(Map<String, Integer> map, Map<String, Integer> otherMap) {
        for (Map.Entry<String, Integer> entry : otherMap.entrySet()) {
            add(map, entry.getKey(), entry.getValue());
        }
    }

    private void addAll(Map<String, Integer> map, Map<String, Integer> otherMap, int multiplier) {
        for (Map.Entry<String, Integer> entry : otherMap.entrySet()) {
            add(map, entry.getKey(), entry.getValue() * multiplier);
        }
    }

}
