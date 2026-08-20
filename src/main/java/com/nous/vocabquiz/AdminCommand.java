package com.nous.vocabquiz;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final QuizEngine engine;
    private final ThemeManager themeManager;
    
    public AdminCommand(QuizEngine engine, ThemeManager themeManager) {
        this.engine = engine;
        this.themeManager = themeManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /vocabquiz join — player clicks [Участвовать] (no permission needed)
        if (args.length == 1 && args[0].equalsIgnoreCase("join")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can join!");
                return true;
            }
            engine.playerJoin((Player) sender);
            return true;
        }
        
        // Admin commands
        if (!sender.hasPermission("vocabquiz.admin")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§6/vocabquiz skip §7— cancel current round");
            sender.sendMessage("§6/vocabquiz theme §7— switch to next daily theme");
            sender.sendMessage("§6/vocabquiz reload §7— reload all theme files");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "skip":
                engine.skip();
                sender.sendMessage("§aQuiz round cancelled!");
                break;
            case "theme":
                themeManager.nextTheme();
                sender.sendMessage("§aSwitched to theme: §e" + themeManager.getCurrentTheme());
                break;
            case "reload":
                themeManager.reload();
                sender.sendMessage("§aThemes reloaded! Current: §e" + themeManager.getCurrentTheme());
                break;
            default:
                sender.sendMessage("§cUnknown subcommand: " + args[0]);
        }
        return true;
    }
}
