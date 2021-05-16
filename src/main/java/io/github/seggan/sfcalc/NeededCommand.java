package io.github.seggan.sfcalc;

import io.github.mooy1.infinitylib.commands.AbstractCommand;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.utils.PatternUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.github.seggan.sfcalc.StringRegistry.format;

public class NeededCommand extends AbstractCommand {
    private static final List<String> ids = new ArrayList<>();
    private SFCalc plugin;

    public NeededCommand(SFCalc pl) {
        super("needed", "Tells you how much more resources are needed", false);
        this.plugin = pl;
    }

    @Override
    protected void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        long amount;
        String reqItem;
        SlimefunItem item;

        StringRegistry registry = plugin.getStringRegistry();

        if (!(sender instanceof Player)) {
            sender.sendMessage(format(registry.getNotAPlayerString()));
            return;
        }

        if (args.length > 3 || args.length < 2) {
            return;
        }

        reqItem = args[1];

        if (args.length == 2) {
            amount = 1;
        } else if (!PatternUtils.NUMERIC.matcher(args[2]).matches()) {
            sender.sendMessage(format(registry.getNotANumberString()));
            return;
        } else {
            try {
                amount = Long.parseLong(args[2]);
                if (amount == 0 || amount > Integer.MAX_VALUE) {
                    sender.sendMessage(format(registry.getInvalidNumberString()));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(format(registry.getInvalidNumberString()));
                return;
            }
        }

        item = SlimefunItem.getByID(reqItem.toUpperCase());

        if (item == null) {
            sender.sendMessage(format(registry.getNoItemString()));
            return;
        }

        SFCalcMetrics.addItemSearched(item.getItemName());

        plugin.getCalc().printResults(sender, item, amount, true);
    }

    @Override
    protected void onTab(@Nonnull CommandSender sender, @Nonnull String[] args, @Nonnull List<String> tabs) {
        if (ids.isEmpty()) {
            for (SlimefunItem item : SlimefunPlugin.getRegistry().getEnabledSlimefunItems()) {
                ids.add(item.getId().toLowerCase(Locale.ROOT));
            }
        }

        if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], ids, tabs);
        }
    }
}
