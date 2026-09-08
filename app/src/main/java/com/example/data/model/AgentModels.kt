package com.example.data.model

data class AgentInfo(
    val id: String,
    val name: String,
    val codename: String,
    val role: String,
    val description: String,
    val iconEmoji: String,
    val isActive: Boolean = true,
    val lastAction: String = "Monitorando ativamente"
)

data class AgentNotification(
    val id: String,
    val agentName: String,
    val title: String,
    val message: String,
    val severity: String = "info", // "info", "warning", "critical"
    val createdAt: String,
    val isRead: Boolean = false
)

data class CustomAgent(
    val id: String,
    val name: String,
    val description: String,
    val naturalPrompt: String,
    val ruleJson: String,
    val isActive: Boolean = true,
    val createdAt: String = ""
)

data class MonthlyReport(
    val id: String,
    val monthYear: String,
    val totalExpenses: Double,
    val reportMarkdown: String,
    val createdAt: String = ""
)

data class BudgetLimit(
    val id: String,
    val category: String,
    val monthlyLimit: Double,
    val alertThreshold: Double = 80.0
)
