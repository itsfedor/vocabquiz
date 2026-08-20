package com.nous.vocabquiz;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class ThemeManager {
    private final VocabQuiz plugin;
    private final ConfigManager config;
    private final Logger logger;
    private final Map<String, List<QuizQuestion>> questionsByTheme = new LinkedHashMap<>();
    private String currentTheme;
    private int themeIndex = 0;
    
    public ThemeManager(VocabQuiz plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        loadAllThemes();
        pickDailyTheme();
    }
    
    private void loadAllThemes() {
        for (String themeName : config.getThemeList()) {
            String path = "questions/" + themeName + ".yml";
            File file = new File(plugin.getDataFolder(), path);
            if (!file.exists()) {
                plugin.saveResource(path, false);
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            List<Map<?, ?>> list = yaml.getMapList("questions");
            List<QuizQuestion> questions = new ArrayList<>();
            
            for (Map<?, ?> map : list) {
                QuizQuestion q = new QuizQuestion();
                q.id = String.valueOf(map.get("id"));
                q.theme = map.containsKey("theme") ? String.valueOf(map.get("theme")) : themeName;
                q.question = String.valueOf(map.get("question"));
                q.answer = String.valueOf(map.get("answer"));
                q.difficulty = map.containsKey("difficulty") ? String.valueOf(map.get("difficulty")) : "easy";
                q.level = map.containsKey("level") ? String.valueOf(map.get("level")) : "all";
                q.hint = map.containsKey("hint") ? String.valueOf(map.get("hint")) : null;
                
                @SuppressWarnings("unchecked")
                List<String> aliases;
                if (map.containsKey("aliases")) {
                    aliases = (List<String>) map.get("aliases");
                } else {
                    aliases = Collections.singletonList(q.answer);
                }
                q.aliases = aliases;
                
                questions.add(q);
            }
            
            questionsByTheme.put(themeName, questions);
            logger.info("Loaded " + questions.size() + " questions for theme: " + themeName);
        }
    }
    
    /** Pick today's theme. Cycle through themes in order. */
    public void pickDailyTheme() {
        List<String> themes = new ArrayList<>(questionsByTheme.keySet());
        if (themes.isEmpty()) {
            logger.warning("No themes loaded!");
            return;
        }
        
        String mode = config.getThemeMode();
        if ("random".equals(mode)) {
            currentTheme = themes.get(new Random().nextInt(themes.size()));
        } else {
            currentTheme = themes.get(themeIndex % themes.size());
            themeIndex++;
        }
        logger.info("Today's theme: " + currentTheme);
    }
    
    /** Get a random question from current theme, filtered by player level */
    public QuizQuestion getRandomQuestionForLevel(String level) {
        List<QuizQuestion> pool = questionsByTheme.getOrDefault(currentTheme, Collections.emptyList());
        if (pool.isEmpty()) return null;
        
        // Filter by level
        List<QuizQuestion> filtered = new ArrayList<>();
        for (QuizQuestion q : pool) {
            if (q.level == null || q.level.equals("all") || q.level.equals(level)) {
                filtered.add(q);
            }
        }
        
        if (filtered.isEmpty()) filtered = pool; // fallback
        return filtered.get(new Random().nextInt(filtered.size()));
    }
    
    /** Skip to next theme (admin command) */
    public void nextTheme() {
        pickDailyTheme();
    }
    
    /** Reload all theme files */
    public void reload() {
        questionsByTheme.clear();
        loadAllThemes();
        pickDailyTheme();
    }
    
    public String getCurrentTheme() { return currentTheme; }
    public Set<String> getThemes() { return questionsByTheme.keySet(); }
}
