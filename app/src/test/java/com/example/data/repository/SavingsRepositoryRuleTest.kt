package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createGoal_withInitialDeposit_createsGoalAndTransaction() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = SavingsRepository(firestore)

    val createResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createGoal(
          name = "Viaje a Oaxaca",
          targetAmountCents = 200_000L,
          initialAmountCents = 25_000L,
          categoryIcon = "travel",
          colorKey = "amber",
        )
      }
    assertTrue("Create goal failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
    val goalId = createResult.getOrThrow()

    val goals =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeGoals(uid).first { list -> list.any { it.id == goalId } }
      }
    val goal = goals.first { it.id == goalId }
    assertEquals("Viaje a Oaxaca", goal.name)
    assertEquals(25_000L, goal.currentAmountCents)

    val txs =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTransactions(uid).first { list -> list.any { it.goalId == goalId } }
      }
    assertTrue(txs.any { it.goalId == goalId && it.amountCents == 25_000L && it.isDeposit })
  }

  @Test
  fun recordMovement_depositAndWithdrawal_updatesBalanceAtomically() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = SavingsRepository(firestore)

    val goalId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createGoal(
            name = "Fondo de emergencia",
            targetAmountCents = 100_000L,
            initialAmountCents = 10_000L,
            categoryIcon = "emergency",
            colorKey = "emerald",
          )
          .getOrThrow()
      }

    val depositResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.recordMovement(
          goalId = goalId,
          type = TransactionType.DEPOSIT,
          amountCents = 15_000L,
          note = "Bono semanal",
        )
      }
    assertTrue("Deposit failed: ${depositResult.exceptionOrNull()}", depositResult.isSuccess)

    val withdrawResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.recordMovement(
          goalId = goalId,
          type = TransactionType.WITHDRAWAL,
          amountCents = 5_000L,
          note = "Gasto imprevisto",
        )
      }
    assertTrue("Withdraw failed: ${withdrawResult.exceptionOrNull()}", withdrawResult.isSuccess)

    val updatedGoal =
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.getGoalById(uid, goalId).getOrThrow() }
    assertNotNull(updatedGoal)
    assertEquals(20_000L, updatedGoal?.currentAmountCents)
  }

  @Test
  fun crossUserAccess_bobCannotReadAliceGoal() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = SavingsRepository(firestore)
    val goalId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.createGoal("Meta privada", 50_000L).getOrThrow()
      }

    signInTestUser(BOB_EMAIL)
    val bobRepo = SavingsRepository(firestore)
    val readResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getGoalById(aliceUid, goalId) }
    assertTrue(readResult.isFailure)
    val firestoreException = readResult.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  @Test
  fun unauthenticatedUser_observeGoals_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = SavingsRepository(firestore)

    var thrown: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeGoals("some_uid").first() }
    } catch (e: Exception) {
      thrown = e
    }
    assertNotNull(thrown)
    val firestoreException = thrown?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $thrown", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_ahorros@test.com"
    const val BOB_EMAIL = "bob_ahorros@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
