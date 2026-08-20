package com.nous.vocabquiz;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Logger;

public class QuizEngine {
    private final VocabQuiz plugin;
    private final ConfigManager config;
    private final ThemeManager themeManager;
    private final EconomyManager economy;
    private final LevelManager levelManager;
    private final Logger logger;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    
    // State
    private BukkitTask announceTask;
    
    // Per-player: question + timer
    private final Map<UUID, QuizQuestion> playerQuestions = new HashMap<>();
    private final Map<UUID, BukkitTask> playerTimers = new HashMap<>();
    private final Map<UUID, Long> lastParticipation = new HashMap<>();
    
    public QuizEngine(VocabQuiz plugin, ConfigManager config, ThemeManager themeManager,
                      EconomyManager economy, LevelManager levelManager) {
        this.plugin = plugin;
        this.config = config;
        this.themeManager = themeManager;
        this.economy = economy;
        this.levelManager = levelManager;
        this.logger = plugin.getLogger();
        start();
    }
    
    // ==================== PHASE 1: ANNOUNCE ====================
    
    public void start() {
        long intervalTicks = config.getIntervalMinutes() * 60L * 20L;
        
        announceTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            playerQuestions.clear();
            playerTimers.clear();
            
            // Build clickable [Участвовать] button
            String prefix = config.getMessage("prefix");
            String announceText = config.getMessage("announce")
                .replace("{theme}", themeManager.getCurrentTheme());
            String btnText = config.getMessage("participate-btn");
            
            Component msg = LEGACY.deserialize(prefix + " " + announceText);
            Component btn = Component.text("    " + btnText.replace("&", "§"), NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/vocabquiz join"));
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(Component.text(" "));
                p.sendMessage(msg);
                p.sendMessage(btn);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            }
            
            logger.info("Quiz announced! Theme: " + themeManager.getCurrentTheme());
        }, 200L, intervalTicks); // first after 10s, then every N minutes
    }
    
    // ==================== PHASE 2: PLAYER JOINS → GETS PERSONAL QUESTION ====================
    
    /** Player clicks [Участвовать] — give them a level-matched question with 30s timer */
    public void playerJoin(Player player) {
        // Cooldown check
        long now = System.currentTimeMillis();
        long last = lastParticipation.getOrDefault(player.getUniqueId(), 0L);
        long cooldownMs = config.getCooldownSeconds() * 1000L;
        if (now - last < cooldownMs) {
            long remaining = (cooldownMs - (now - last)) / 1000;
            player.sendMessage(LEGACY.deserialize(
                config.getMessage("prefix") + " " + config.getMessage("cooldown")
                    .replace("{seconds}", String.valueOf(remaining))));
            return;
        }
        
        // Already in this round?
        if (playerQuestions.containsKey(player.getUniqueId())) {
            player.sendMessage(LEGACY.deserialize(
                config.getMessage("prefix") + " " + config.getMessage("already-joined")));
            return;
        }
        
        // Pick level-matched question
        String level = levelManager.getLevel(player);
        QuizQuestion question = themeManager.getRandomQuestionForLevel(level);
        if (question == null) {
            player.sendMessage(LEGACY.deserialize(
                config.getMessage("prefix") + " &cNo questions available for your level!"));
            return;
        }
        
        lastParticipation.put(player.getUniqueId(), now);
        playerQuestions.put(player.getUniqueId(), question);
        
        // Send personal question
        player.sendMessage(Component.text(" "));
        player.sendMessage(LEGACY.deserialize(
            config.getMessage("prefix") + " " + config.getMessage("your-question")
                .replace("{level}", level.toUpperCase())
                .replace("{question}", question.question)));
        player.sendMessage(LEGACY.deserialize(
            config.getMessage("prefix") + " " + config.getMessage("timer")
                .replace("{seconds}", String.valueOf(config.getAnswerTimeoutSeconds()))));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.2f);
        
        // Start 30s timer for this player
        int timeoutSeconds = config.getAnswerTimeoutSeconds();
        BukkitTask timer = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            QuizQuestion q = playerQuestions.remove(player.getUniqueId());
            playerTimers.remove(player.getUniqueId());
            if (q != null) {
                // Timeout — player didn't answer in time
                player.sendMessage(LEGACY.deserialize(
                    config.getMessage("prefix") + " " + config.getMessage("timeout")
                        .replace("{answer}", q.answer)));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
            }
        }, timeoutSeconds * 20L);
        
        playerTimers.put(player.getUniqueId(), timer);
        
        logger.info(player.getName() + " joined quiz. Level: " + level + ", Q: " + question.id);
    }
    
    // ==================== PHASE 3: ANSWER CHECKING ====================
    
    /** Player types /answer <word> — check against their personal question */
    public boolean checkAnswer(Player player, String input) {
        QuizQuestion question = playerQuestions.get(player.getUniqueId());
        if (question == null) {
            player.sendMessage(LEGACY.deserialize(
                config.getMessage("prefix") + " " + config.getMessage("no-question")));
            return false;
        }
        
        // Normalize
        String normalizedInput = input.trim().toLowerCase();
        
        // Check answer + aliases
        boolean correct = normalizedInput.equals(question.answer.toLowerCase());
        if (!correct && question.aliases != null) {
            for (String alias : question.aliases) {
                if (normalizedInput.equals(alias.toLowerCase())) {
                    correct = true;
                    break;
                }
            }
        }
        
        if (correct) {
            // Cancel timer, remove from active
            BukkitTask timer = playerTimers.remove(player.getUniqueId());
            if (timer != null) timer.cancel();
            playerQuestions.remove(player.getUniqueId());
            
            double reward = config.getCorrectAnswerReward();
            economy.deposit(player, reward);
            
            // Tell the player
            player.sendMessage(Component.text(" "));
            player.sendMessage(LEGACY.deserialize(
                config.getMessage("prefix") + " " + config.getMessage("correct")
                    .replace("{player}", player.getName())
                    .replace("{answer}", question.answer)
                    .replace("{reward}", String.format("%.0f", reward))));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            
            logger.info(player.getName() + " answered correctly: " + question.answer + " (+$" + String.format("%.0f", reward) + ")");
            
            // Report to EnglishProgression
            try {
                Class<?> ep = Class.forName("com.nous.progression.EnglishProgression");
                ep.getMethod("addEarnings", Player.class, double.class).invoke(null, player, reward);
            } catch (Exception ignored) {}
            
            return true;
        } else {
            player.sendMessage(LEGACY.deserialize(
                config.getMessage("prefix") + " " + config.getMessage("wrong")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return false;
        }
    }
    
    // ==================== ADMIN ====================
    
    public void skip() {
        // Cancel all active player timers
        for (BukkitTask t : playerTimers.values()) t.cancel();
        playerTimers.clear();
        playerQuestions.clear();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(LEGACY.deserialize(
                config.getMessage("prefix") + " " + config.getMessage("skipped")));
        }
    }
    
    public void stop() {
        if (announceTask != null) announceTask.cancel();
        skip();
    }
    
    public boolean hasActiveQuestion(Player player) {
        return playerQuestions.containsKey(player.getUniqueId());
    }
    
    public int getActivePlayerCount() { return playerQuestions.size(); }
}
