package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GoalCategory(val key: String, val labelEs: String) {
  EMERGENCY("emergency", "Emergencia"),
  TRAVEL("travel", "Viajes"),
  HOME("home", "Hogar"),
  EDUCATION("education", "Educación"),
  TECH("tech", "Tecnología"),
  GIFT("gift", "Regalos"),
  GENERAL("general", "General");

  companion object {
    fun fromKey(key: String): GoalCategory = entries.find { it.key == key } ?: GENERAL
  }
}

enum class GoalColor(val key: String, val labelEs: String) {
  EMERALD("emerald", "Esmeralda"),
  AMBER("amber", "Ámbar"),
  TERRACOTTA("terracotta", "Terracota"),
  OCEAN("ocean", "Océano"),
  PLUM("plum", "Ciruela");

  companion object {
    fun fromKey(key: String): GoalColor = entries.find { it.key == key } ?: EMERALD
  }
}

enum class TransactionType(val key: String, val labelEs: String) {
  DEPOSIT("deposit", "Depósito"),
  WITHDRAWAL("withdrawal", "Retiro");

  companion object {
    fun fromKey(key: String): TransactionType = entries.find { it.key == key } ?: DEPOSIT
  }
}

@IgnoreExtraProperties
data class SavingsGoal(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val targetAmountCents: Long = 0L,
  val currentAmountCents: Long = 0L,
  val categoryIcon: String = GoalCategory.GENERAL.key,
  val colorKey: String = GoalColor.EMERALD.key,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val progressFraction: Float
    get() =
      if (targetAmountCents <= 0L) 0f
      else (currentAmountCents.toFloat() / targetAmountCents.toFloat()).coerceIn(0f, 1f)

  val progressPercent: Int
    get() =
      if (targetAmountCents <= 0L) 0
      else ((currentAmountCents * 100L) / targetAmountCents).toInt().coerceIn(0, 999)

  val isCompleted: Boolean
    get() = targetAmountCents > 0L && currentAmountCents >= targetAmountCents

  val remainingCents: Long
    get() = (targetAmountCents - currentAmountCents).coerceAtLeast(0L)
}

@IgnoreExtraProperties
data class SavingsTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val goalId: String = "",
  val goalName: String = "",
  val type: String = TransactionType.DEPOSIT.key,
  val amountCents: Long = 0L,
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  val isDeposit: Boolean
    get() = type == TransactionType.DEPOSIT.key
}
