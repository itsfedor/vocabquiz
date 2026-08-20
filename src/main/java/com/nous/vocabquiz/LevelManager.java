package com.nous.vocabquiz;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LevelManager {
    private final VocabQuiz plugin;
    private final ConfigManager config;
    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\[([A-C][0-2]|D1)\\]");
    
    public LevelManager(VocabQuiz plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }
    
    /** Get player's English level from LuckPerms resolved prefix (e.g. [D1] [Admin] → d1) */
    public String getLevel(Player player) {
        if (!config.isLevelFilterEnabled()) return "all";
        
        try {
            LuckPerms lp = Bukkit.getServicesManager().load(LuckPerms.class);
            if (lp != null) {
                User user = lp.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null) {
                        String clean = prefix.replaceAll("[§&][0-9a-fA-Fk-oK-OrR]", "");
                        Matcher m = LEVEL_PATTERN.matcher(clean);
                        if (m.find()) return m.group(1).toLowerCase();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting player level: " + e.getMessage());
        }
        return config.getDefaultLevel();
    }
}
