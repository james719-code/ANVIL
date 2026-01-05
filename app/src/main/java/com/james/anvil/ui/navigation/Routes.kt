package com.james.anvil.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlin Serialization.
 * This replaces string-based route definitions for compile-time safety.
 */

// ============================================
// TOP-LEVEL DESTINATIONS (Bottom Nav)
// ============================================

@Serializable
object DashboardRoute

@Serializable
object TasksRoute

@Serializable
object BlocklistRoute

@Serializable
object SettingsRoute

// ============================================
// SECONDARY DESTINATIONS (From Dashboard)
// ============================================

@Serializable
object BudgetRoute

@Serializable
object LoansRoute

// ============================================
// DETAIL DESTINATIONS (With Arguments)
// ============================================

@Serializable
data class EditTaskRoute(val taskId: Long)

@Serializable
data class TaskDetailRoute(val taskId: Long)

@Serializable
data class LoanDetailRoute(val loanId: Long)

// ============================================
// NESTED GRAPH MARKERS
// ============================================

@Serializable
object TasksGraphRoute

@Serializable
object FinanceGraphRoute

@Serializable
object BlocklistGraphRoute
