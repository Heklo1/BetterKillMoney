package me.Heklo.BetterKillMoney;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

	public class Main extends JavaPlugin implements Listener {
		
		public static Economy economy = null;
		public static EconomyResponse response;
		public FileConfiguration config;
		
		@Override
		public void onEnable() // Called on startup/reload
		{
			if (startEconomy())
			{
				Bukkit.getConsoleSender().sendMessage("[BetterKillMoney] Successfully enabled BetterKillMoney v1.0 - by Heklo");
			}
			else
		    {
		      getLogger().severe("BetterKillMoney disabled: No compatible Vault plugin found.");
		      getServer().getPluginManager().disablePlugin(this);
		      return;
		    }
			this.saveDefaultConfig();
			config = this.getConfig();
			Bukkit.getPluginManager().registerEvents(this, this);
		}
		
		@Override
		public void onDisable()
		{
			// Called on shutdown/reload
		}
		
		private boolean startEconomy() 
		{
			if (getServer().getPluginManager().getPlugin("Vault") == null) 
			{
				return false;
		    }
			
		    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		    
		    if (rsp == null) 
		    {
		    	return false;
		    }
		    
		    economy = (Economy)rsp.getProvider();
		    return economy != null;
		}
		
		@EventHandler
		public void onEntityDeath(EntityDeathEvent e)
		{
			Entity ekiller = e.getEntity().getKiller();
		    if ((ekiller instanceof Player))
		    {
		    	Player p = (Player)ekiller;
		    	Entity k = e.getEntity();
		    	rewardPlayer(p, k);
		    }
		    
		    if(e.getEntity() instanceof Player)
		    {
				Player dead = (Player)e.getEntity();
				dead.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aYou were killed by &2" + ekiller.getName() + "&a."));
		    }
		}
		
		public double getScale(Player p)
		{
			double mrf = getConfig().getDouble("MRF"); // Minimum Reduction Factor: if 0.2, Full p4 diamond armor will get 20% profit
			double minKitPoints = getConfig().getDouble("min-kit-points"); // Full chain armor with no enchants has 0.48 armor points.
			double maxKitPoints = getConfig().getDouble("max-kit-points"); // Full diamond armor with protection 4 has 1.0 armor points.
			
			double points = getDamageReduced(p);
			if(points < minKitPoints) { points = minKitPoints; } // Cap minimum at the default kit (full chain).
			double scale = 1 - (points-minKitPoints) / (maxKitPoints-minKitPoints) * (1-mrf); //  extra points / point range multiplied by mrf inverse. It just works.
			return scale;
		}
		
		public void rewardPlayer(Player p, Entity k)
		{
			@SuppressWarnings("deprecation")
			String targetType = k.getType().getName();
			
			if(!getConfig().getConfigurationSection("Rewards").contains(targetType))
				targetType = "OTHER";
			
			ConfigurationSection rewards = getConfig().getConfigurationSection("Rewards." + targetType);
			double scale = getScale(p);
			
			// ====== Money ======
			ConfigurationSection moneyReward = rewards.getConfigurationSection("Money");
			double moneyMin = moneyReward.getDouble("minimum");
			double moneyMax = moneyReward.getDouble("maximum");
			boolean moneyGearBased = moneyReward.getBoolean("gear-based");
			
			Random rand = new Random();
			double base = rand.nextDouble() * (moneyMax - moneyMin) + moneyMin;
			
			// Multiply by scale, round to 2 decimals.
			double moneyGained = Math.round(base * (moneyGearBased ? scale : 1) * 100D) / 100D;
			
	    	p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aKilled &2" + k.getName() + " &afor " + moneyGained));
	    	response = economy.depositPlayer(p, moneyGained);
	    	
	    	// ====== Commands ======
	    	if(rewards.contains("Commands"))
	    	{
		    	for(Object obj : rewards.getConfigurationSection("Commands").getValues(false).values())
		    	{
		    		ConfigurationSection commandReward = (ConfigurationSection)obj;
		    		String cmd = commandReward.getString("cmd").replace("%player%", p.getName());
		    		boolean cmdGearBased = commandReward.getBoolean("gear-based");
		    		double chance = commandReward.getDouble("chance") * (cmdGearBased ? scale : 1);

		    		if(rand.nextFloat() < chance)
		    			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
		    	}
	    	}
	    	
		}
		
		
	    public static double getDamageReduced(Player p) // CREDIT: Danablend
	    {
	        ItemStack helmet = p.getEquipment().getHelmet();
	        ItemStack chest = p.getEquipment().getChestplate();
	        ItemStack boots = p.getEquipment().getBoots();
	        ItemStack pants = p.getEquipment().getLeggings();
	        
	        double totalEPF = 0;
	        
	        double red = 0.0;
	        if(helmet != null) {
	            if (helmet.getType() == Material.LEATHER_HELMET) red = red + 0.04;
	            else if (helmet.getType() == Material.GOLD_HELMET) red = red + 0.08;
	            else if (helmet.getType() == Material.CHAINMAIL_HELMET) red = red + 0.08;
	            else if (helmet.getType() == Material.IRON_HELMET) red = red + 0.08;
	            else if (helmet.getType() == Material.DIAMOND_HELMET) red = red + 0.12;
	            totalEPF += helmet.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
	            
	        }
	        if(chest != null) {
	            if (chest.getType() == Material.LEATHER_CHESTPLATE)    red = red + 0.12;
	            else if (chest.getType() == Material.GOLD_CHESTPLATE)red = red + 0.20;
	            else if (chest.getType() == Material.CHAINMAIL_CHESTPLATE) red = red + 0.20;
	            else if (chest.getType() == Material.IRON_CHESTPLATE) red = red + 0.24;
	            else if (chest.getType() == Material.DIAMOND_CHESTPLATE) red = red + 0.32;
	            totalEPF += chest.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
	        }
	        if(pants != null) {
	            if (pants.getType() == Material.LEATHER_LEGGINGS) red = red + 0.08;
	            else if (pants.getType() == Material.GOLD_LEGGINGS)    red = red + 0.12;
	            else if (pants.getType() == Material.CHAINMAIL_LEGGINGS) red = red + 0.16;
	            else if (pants.getType() == Material.IRON_LEGGINGS)    red = red + 0.20;
	            else if (pants.getType() == Material.DIAMOND_LEGGINGS) red = red + 0.24;
	            totalEPF += pants.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
	        }
	        if(boots != null) {
	            if (boots.getType() == Material.LEATHER_BOOTS) red = red + 0.04;
	            else if (boots.getType() == Material.GOLD_BOOTS) red = red + 0.04;
	            else if (boots.getType() == Material.CHAINMAIL_BOOTS) red = red + 0.04;
	            else if (boots.getType() == Material.IRON_BOOTS) red = red + 0.08;
	            else if (boots.getType() == Material.DIAMOND_BOOTS)    red = red + 0.12;
	            totalEPF += boots.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
	        }
	        
	        double total = red + (totalEPF*0.0125); // Maxes out at 1.0 with prot 4 diamond.
	        return total;
	    }
		
	    @Override
		public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
		{
			if(label.equalsIgnoreCase("bkm"))
			{
				if(args.length == 0)
				{
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&3[BKM] &7BetterKillMoney - &fv1.0")); return true;
				}
				
				else if(args[0].equalsIgnoreCase("reload"))
				{
					if(!sender.hasPermission("bkm.admin"))
					{
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3[BKM] &7No permission."));
						return true;
					}
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3[BKM] &7Reloading config..."));
					this.reloadConfig();
					this.config = getConfig();
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3[BKM] &7Reload complete."));
					return true;
				}
				else
				{
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3[BKM] &7Unknown command."));
					return true;
				}
			}
			
			return false;
		}

}
