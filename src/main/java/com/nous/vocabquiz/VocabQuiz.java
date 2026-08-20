package com.nous.vocabquiz;

import org.bukkit.plugin.java.JavaPlugin;

public final class VocabQuiz extends JavaPlugin {
    
    private ConfigManager configManager;
    private ThemeManager themeManager;
    private QuizEngine quizEngine;
    private EconomyManager economyManager;
    private LevelManager levelManager;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.economyManager = new EconomyManager();
        
        if (!economyManager.isReady()) {
            getLogger().severe("Vault not found! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.levelManager = new LevelManager(this, configManager);
        this.themeManager = new ThemeManager(this, configManager);
        this.quizEngine = new QuizEngine(this, configManager, themeManager, economyManager, levelManager);
        
        getCommand("answer").setExecutor(new AnswerCommand(quizEngine));
        getCommand("vocabquiz").setExecutor(new AdminCommand(quizEngine, themeManager));
        
        getLogger().info("VocabQuiz v1.0.0 enabled! " 
            + configManager.getIntervalMinutes() + "min interval, "
            + configManager.getAnswerTimeoutSeconds() + "s timeout, $"
            + String.format("%.0f", configManager.getCorrectAnswerReward()) + " reward, "
            + themeManager.getThemes().size() + " themes loaded");
    }
    
    @Override
    public void onDisable() {
        if (quizEngine != null) quizEngine.stop();
        getLogger().info("VocabQuiz disabled!");
    }
}
