package com.james.anvil.util

/**
 * Constants for task and budget categories.
 * Centralizes category definitions for consistency across the app.
 */
object Categories {
    
    // ============================================
    // TASK CATEGORIES
    // ============================================
    object Task {
        const val WORK = "Work"
        const val PERSONAL = "Personal"
        const val HEALTH = "Health"
        const val STUDY = "Study"
        const val SHOPPING = "Shopping"
        const val ERRANDS = "Errands"
        const val HOME = "Home"
        const val FINANCE = "Finance"
        const val SOCIAL = "Social"
        const val OTHER = "Other"
        
        val all = listOf(
            WORK, PERSONAL, HEALTH, STUDY, SHOPPING,
            ERRANDS, HOME, FINANCE, SOCIAL, OTHER
        )
        
        val default = PERSONAL
    }
    
    // ============================================
    // BUDGET CATEGORIES
    // ============================================
    object Budget {
        const val FOOD = "Food"
        const val TRANSPORTATION = "Transportation"
        const val UTILITIES = "Utilities"
        const val ENTERTAINMENT = "Entertainment"
        const val SHOPPING = "Shopping"
        const val HEALTH = "Health"
        const val EDUCATION = "Education"
        const val SAVINGS = "Savings"
        const val INCOME = "Income"
        const val LOAN = "Loan"
        const val LOAN_REPAYMENT = "Loan Repayment"
        const val GENERAL = "General"
        const val OTHER = "Other"
        
        val expenseCategories = listOf(
            FOOD, TRANSPORTATION, UTILITIES, ENTERTAINMENT,
            SHOPPING, HEALTH, EDUCATION, OTHER
        )
        
        val incomeCategories = listOf(
            INCOME, SAVINGS, OTHER
        )
        
        val all = expenseCategories + incomeCategories + listOf(LOAN, LOAN_REPAYMENT, GENERAL)
        
        val default = GENERAL
    }
    
    // ============================================
    // APP BLOCKLIST CATEGORIES
    // ============================================
    object App {
        const val SOCIAL_MEDIA = "Social Media"
        const val GAMES = "Games"
        const val STREAMING = "Streaming"
        const val PRODUCTIVITY = "Productivity"
        const val COMMUNICATION = "Communication"
        const val NEWS = "News"
        const val SHOPPING = "Shopping"
        const val UNCATEGORIZED = "Uncategorized"
        
        val all = listOf(
            SOCIAL_MEDIA, GAMES, STREAMING, PRODUCTIVITY,
            COMMUNICATION, NEWS, SHOPPING, UNCATEGORIZED
        )
        
        val default = UNCATEGORIZED
    }
    
    // ============================================
    // BONUS TASK CATEGORIES  
    // ============================================
    object Bonus {
        const val BONUS = "Bonus"
        const val EXTRA = "Extra"
        const val CHALLENGE = "Challenge"
        
        val all = listOf(BONUS, EXTRA, CHALLENGE)
        
        val default = BONUS
    }
}
