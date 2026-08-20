package com.nous.vocabquiz;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private Economy economy;
    
    public EconomyManager() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) this.economy = rsp.getProvider();
    }
    
    public boolean isReady() { return economy != null; }
    
    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null) return false;
        economy.depositPlayer(player, amount);
        return true;
    }
}
