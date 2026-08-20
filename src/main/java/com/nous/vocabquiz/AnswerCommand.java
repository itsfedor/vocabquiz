package com.nous.vocabquiz;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnswerCommand implements CommandExecutor {
    private final QuizEngine engine;
    
    public AnswerCommand(QuizEngine engine) {
        this.engine = engine;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can answer!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage("§cUsage: /answer <word>");
            return true;
        }
        
        String answer = String.join(" ", args);
        engine.checkAnswer(player, answer);
        return true;
    }
}
