package com.nous.vocabquiz;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    private final VocabQuiz plugin;
    private FileConfiguration config;
    
    public ConfigManager(VocabQuiz plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }
    
    // Quiz timing
    public int getIntervalMinutes() { return config.getInt("quiz.interval-minutes", 10); }
    public int getAnswerTimeoutSeconds() { return config.getInt("quiz.answer-timeout-seconds", 30); }
    public int getCooldownSeconds() { return config.getInt("quiz.cooldown-seconds", 60); }
    
    // Rewards
    public double getCorrectAnswerReward() { return config.getDouble("rewards.correct-answer", 5.0); }
    
    // Themes
    public String getThemeMode() { return config.getString("themes.mode", "daily-cycle"); }
    public List<String> getThemeList() { return config.getStringList("themes.list"); }
    
    // Levels
    public boolean isLevelFilterEnabled() { return config.getBoolean("levels.enabled", true); }
    public String getLevelTrack() { return config.getString("levels.track", "english"); }
    public String getDefaultLevel() { return config.getString("levels.default", "a0"); }
    
    // Messages
    public String getMessage(String key) {
        return config.getString("messages." + key, "");
    }
}
