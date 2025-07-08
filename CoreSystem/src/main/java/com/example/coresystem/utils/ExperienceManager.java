package com.example.coresystem.utils;

// This class can be expanded later to load XP requirements from a config file (experience.yml)
public class ExperienceManager {

    // Simple formula: level * 100 (e.g., Level 1 to 2 needs 100 XP, Level 2 to 3 needs 200 XP)
    // More complex formulas: baseXP * (level^multiplier) or a predefined list per level
    public static double getRequiredXpForLevel(int currentLevel) {
        if (currentLevel <= 0) return 100; // Should not happen with level starting at 1
        // Example: To reach level (currentLevel + 1)
        // For level 1 to 2: 1 * 100 = 100 XP
        // For level 2 to 3: 2 * 100 = 200 XP
        // etc.
        // This means `currentLevel` is the level you ARE, and you need this much XP to get to `currentLevel + 1`
        return (double) currentLevel * 100;
    }

    // Calculates total XP accumulated up to the start of a given level
    public static double getTotalXpForLevel(int level) {
        if (level <= 1) return 0;
        double total = 0;
        for (int i = 1; i < level; i++) {
            total += getRequiredXpForLevel(i);
        }
        return total;
    }

    // Calculates XP earned within the current level (progress)
    public static double getCurrentLevelProgressXp(double totalPlayerXp, int playerLevel) {
        if (playerLevel <= 1 && totalPlayerXp >=0) return totalPlayerXp;
        double xpForPreviousLevels = getTotalXpForLevel(playerLevel);
        return totalPlayerXp - xpForPreviousLevels;
    }
}
