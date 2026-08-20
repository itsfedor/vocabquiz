package com.nous.vocabquiz;

import java.util.List;

public class QuizQuestion {
    public String id;
    public String theme;
    public String question;       // "What animal says 'moo'?"
    public String answer;         // "cow"
    public List<String> aliases;  // ["a cow", "the cow"] — acceptable variants
    public String difficulty;     // "easy", "medium", "hard"
    public String level;          // "a0", "a1", ..., "all"
    public String hint;           // optional hint text
}
