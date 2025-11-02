package gg.leon.betterabilities;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class BetterAbilities extends JavaPlugin implements Listener {

    private final Map<UUID, String> playerAbilities = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_MS = 3000;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BetterAbilities aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BetterAbilities deaktiviert!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl benutzen.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("abilities")) {
            openAbilitiesUI(player);
            return true;
        }

        return false;
    }

    private void openAbilitiesUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Wähle deine Fähigkeit");

        gui.setItem(1, createAbilityItem(Material.BLAZE_ROD, ChatColor.RED + "Feuer", List.of(ChatColor.GRAY + "Rechtsklick: Schieße einen Feuerball")));
        gui.setItem(3, createAbilityItem(Material.WATER_BUCKET, ChatColor.AQUA + "Wasser", List.of(ChatColor.GRAY + "Sneaken + Rechtsklick: Heile dich")));
        gui.setItem(5, createAbilityItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Erde", List.of(ChatColor.GRAY + "Sneaken + Rechtsklick: Resistenz")));
        gui.setItem(7, createAbilityItem(Material.FEATHER, ChatColor.WHITE + "Luft", List.of(ChatColor.GRAY + "Rechtsklick: Windstoß")));

        player.openInventory(gui);
    }

    private ItemStack createAbilityItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle() == null || !ChatColor.stripColor(event.getView().getTitle()).equalsIgnoreCase("Wähle deine Fähigkeit"))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String ability = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        playerAbilities.put(player.getUniqueId(), ability);

        switch (ability.toLowerCase()) {
            case "feuer" -> player.getInventory().addItem(createAbilityItem(Material.BLAZE_ROD, ChatColor.RED + "Feuer", List.of(ChatColor.GRAY + "Rechtsklick: Schieße einen Feuerball")));
            case "wasser" -> player.getInventory().addItem(createAbilityItem(Material.WATER_BUCKET, ChatColor.AQUA + "Wasser", List.of(ChatColor.GRAY + "Sneaken + Rechtsklick: Heile dich")));
            case "erde" -> player.getInventory().addItem(createAbilityItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Erde", List.of(ChatColor.GRAY + "Sneaken + Rechtsklick: Resistenz")));
            case "luft" -> player.getInventory().addItem(createAbilityItem(Material.FEATHER, ChatColor.WHITE + "Luft", List.of(ChatColor.GRAY + "Rechtsklick: Windstoß")));
        }

        player.closeInventory();
        player.sendMessage(ChatColor.GOLD + "Du hast die Fähigkeit " + ChatColor.YELLOW + ability + ChatColor.GOLD + " gewählt!");
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) return;

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        String ability = playerAbilities.get(player.getUniqueId());
        if (ability == null || !displayName.equalsIgnoreCase(ability)) return;

        switch (ability.toLowerCase()) {
            case "feuer" -> shootFireball(player);
            case "wasser" -> healWater(player);
            case "erde" -> earthShield(player);
            case "luft" -> airBoost(player);
        }
    }


    private boolean isOnCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) return false;
        long lastUse = cooldowns.get(player.getUniqueId());
        return (System.currentTimeMillis() - lastUse) < COOLDOWN_MS;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        new BukkitRunnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - cooldowns.get(player.getUniqueId());
                if (elapsed >= COOLDOWN_MS) {
                    player.sendActionBar(ChatColor.GREEN + "Fähigkeit bereit!");
                    cancel();
                    return;
                }
                double remaining = (COOLDOWN_MS - elapsed) / 1000.0;
                player.sendActionBar(ChatColor.RED + "⏳ Abklingzeit: " + String.format("%.1f", remaining) + "s");
            }
        }.runTaskTimer(this, 0L, 5L);
    }


    private void shootFireball(Player player) {
        if (isOnCooldown(player)) return;
        player.launchProjectile(Fireball.class).setVelocity(player.getLocation().getDirection().multiply(1.5));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
        player.sendMessage(ChatColor.RED + "Feuerball abgeschossen!");
        setCooldown(player);
    }

    private void healWater(Player player) {
        if (!player.isSneaking() || isOnCooldown(player)) return;
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1));
        player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation(), 30, 1, 1, 1);
        player.playSound(player.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 1f, 1f);
        player.sendMessage(ChatColor.AQUA + "Du wurdest geheilt!");
        setCooldown(player);
    }

    private void earthShield(Player player) {
        if (!player.isSneaking() || isOnCooldown(player)) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 1));
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 40, 1, 1, 1, Material.DIRT.createBlockData());
        player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 0.8f);
        player.sendMessage(ChatColor.GREEN + "Du bist geschützt!");
        setCooldown(player);
    }

    private void airBoost(Player player) {
        if (isOnCooldown(player)) return;
        WindCharge wc = player.launchProjectile(WindCharge.class);
        wc.setVelocity(player.getLocation().getDirection().multiply(1.3));
        player.setVelocity(player.getVelocity().add(new Vector(0, 0.6, 0)));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.2f);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
        player.sendMessage(ChatColor.WHITE + "Luft abgeschossen!");
        setCooldown(player);
    }
}
