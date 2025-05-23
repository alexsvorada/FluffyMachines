package io.ncbpfluffybear.fluffymachines.items.tools;

import io.github.thebusybiscuit.slimefun4.core.attributes.DamageableItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.TrashCan;
import io.ncbpfluffybear.fluffymachines.FluffyMachines;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

/**
 * This {@link SimpleSlimefunItem} allows players to
 * instantly break any {@link EnergyNetComponent} block
 * or {@link CargoNet} block.
 *
 * @author NCBPFluffyBear
 */
public class FluffyWrench extends SimpleSlimefunItem<ItemUseHandler> implements Listener, DamageableItem, Rechargeable {

    private final Wrench type;

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private static final int WRENCH_DELAY = 250; // Not an itemsetting, too low causes dupes and no reason to increase

    public FluffyWrench(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, Wrench type) {
        super(category, item, recipeType, recipe);
        this.type = type;

        Bukkit.getPluginManager().registerEvents(this, FluffyMachines.getInstance());
    }

    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {
        return e -> e.setUseBlock(Event.Result.DENY);
    }

    @EventHandler
    public void onWrenchInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack wrenchItem = e.getItem();

        Long cooldown = cooldowns.get(p.getUniqueId());
        if (isItem(wrenchItem) && cooldown != null
        ) {
            if ((System.currentTimeMillis() - cooldown) < WRENCH_DELAY) {
                return;
            }
        }
        cooldowns.put(p.getUniqueId(), System.currentTimeMillis());

        Block block = e.getClickedBlock();
        // Check if player has wrench and is left clicking block
        // Can't use offhand because a player can offhand the wrench to escape the event
        if (isItem(e.getItem()) && !isItem(p.getInventory().getItemInOffHand())
            && e.getAction().toString().endsWith("_BLOCK")
            && Slimefun.getProtectionManager().hasPermission(e.getPlayer(),
            block.getLocation(), Interaction.BREAK_BLOCK)
        ) {
            e.setCancelled(true);
            SlimefunItem slimefunBlock = BlockStorage.check(block);

            // Check if slimefunBlock is not a machine or a cargo component
            if (slimefunBlock == null
                || (!(slimefunBlock instanceof EnergyNetComponent)
                && !slimefunBlock.getId().startsWith("CARGO_NODE")
                && !slimefunBlock.getId().equals(SlimefunItems.CARGO_MANAGER.getItemId())
                && !(slimefunBlock instanceof TrashCan))
            ) {
                return;
            }

            if (!type.isElectric) {
                damageItem(p, wrenchItem);
                breakBlock(block, p);
            } else if (removeItemCharge(wrenchItem, 1)) {
                breakBlock(block, p);
            }
        }
    }

    public static void breakBlock(Block block, Player p) {
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, p);
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (!breakEvent.isCancelled()) {
            BlockStorage.clearBlockInfo(block);
            block.setType(Material.AIR);
        }
    }

    @Override
    public boolean isDamageable() {
        return true;
    }

    @Override
    public float getMaxItemCharge(ItemStack item) {
        if (!type.isElectric) {
            return 0;
        } else {
            return type.getMaxCharge();
        }
    }

    public enum Wrench {
        DEFAULT(Material.GOLDEN_AXE, false, 0),
        REINFORCED(Material.DIAMOND_AXE, false, 0),
        CARBONADO(Material.NETHERITE_AXE, true, 5000);

        private final Material material;
        private final boolean isElectric;
        private final int maxCharge;

        Wrench(Material material, boolean isElectric, int maxCharge) {
            this.material = material;
            this.isElectric = isElectric;
            this.maxCharge = maxCharge;
        }

        public Material getMaterial() {
            return material;
        }

        public boolean isElectric() {
            return isElectric;
        }

        public int getMaxCharge() {
            return maxCharge;
        }
    }
}
