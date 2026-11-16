package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsTransaction
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SavingsRepository(private val db: FirebaseFirestore) {

  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("El usuario debe iniciar sesión con Google primero.")
  }

  fun observeGoals(userId: String): Flow<List<SavingsGoal>> = flow {
    val path = "users/$userId/goals"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("goals")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(SavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeTransactions(userId: String): Flow<List<SavingsTransaction>> = flow {
    val path = "users/$userId/transactions"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("transactions")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              SavingsTransaction::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun getGoalById(userId: String, goalId: String): Result<SavingsGoal?> {
    val docRef = db.collection("users").document(userId).collection("goals").document(goalId)
    return try {
      val snapshot = docRef.get().await()
      val goal =
        snapshot.toObject(
          SavingsGoal::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      Result.success(goal)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createGoal(
    name: String,
    targetAmountCents: Long,
    initialAmountCents: Long = 0L,
    categoryIcon: String = "general",
    colorKey: String = "emerald",
  ): Result<String> {
    val uid = requireUserId()
    val trimmedName = name.trim()
    require(trimmedName.isNotEmpty() && trimmedName.length <= 60) {
      "El nombre debe tener entre 1 y 60 caracteres."
    }
    require(targetAmountCents > 0L) { "La meta debe ser mayor a cero." }
    require(initialAmountCents >= 0L) { "El monto inicial no puede ser negativo." }

    val goalRef = db.collection("users").document(uid).collection("goals").document()
    val goalPayload =
      mapOf(
        "userId" to uid,
        "name" to trimmedName,
        "targetAmountCents" to targetAmountCents,
        "currentAmountCents" to initialAmountCents,
        "categoryIcon" to categoryIcon,
        "colorKey" to colorKey,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    return try {
      goalRef.set(goalPayload).await()
      if (initialAmountCents > 0L) {
        val txRef = db.collection("users").document(uid).collection("transactions").document()
        val txPayload =
          mapOf(
            "userId" to uid,
            "goalId" to goalRef.id,
            "goalName" to trimmedName,
            "type" to TransactionType.DEPOSIT.key,
            "amountCents" to initialAmountCents,
            "note" to "Ahorro inicial",
            "createdAt" to FieldValue.serverTimestamp(),
          )
        txRef.set(txPayload).await()
      }
      Result.success(goalRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, goalRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateGoalDetails(
    goalId: String,
    name: String,
    targetAmountCents: Long,
    categoryIcon: String,
    colorKey: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val trimmedName = name.trim()
    require(goalId.isNotBlank()) { "El identificador de la meta no puede estar vacío." }
    require(trimmedName.isNotEmpty() && trimmedName.length <= 60) {
      "El nombre debe tener entre 1 y 60 caracteres."
    }
    require(targetAmountCents > 0L) { "La meta debe ser mayor a cero." }

    val goalRef = db.collection("users").document(uid).collection("goals").document(goalId)
    val updates =
      mapOf(
        "name" to trimmedName,
        "targetAmountCents" to targetAmountCents,
        "categoryIcon" to categoryIcon,
        "colorKey" to colorKey,
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    return try {
      goalRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, goalRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordMovement(
    goalId: String,
    type: TransactionType,
    amountCents: Long,
    note: String = "",
  ): Result<String> {
    val uid = requireUserId()
    require(goalId.isNotBlank()) { "Meta inválida." }
    require(amountCents > 0L) { "El monto debe ser mayor a cero." }
    val trimmedNote = note.trim().take(120)

    val goalRef = db.collection("users").document(uid).collection("goals").document(goalId)
    val txRef = db.collection("users").document(uid).collection("transactions").document()

    return try {
      db.runTransaction { transaction ->
          val snapshot = transaction.get(goalRef)
          if (!snapshot.exists()) {
            throw IllegalStateException("La meta de ahorro no existe.")
          }
          val currentBalance = snapshot.getLong("currentAmountCents") ?: 0L
          val goalName = snapshot.getString("name") ?: "Meta"
          val newBalance =
            when (type) {
              TransactionType.DEPOSIT -> currentBalance + amountCents
              TransactionType.WITHDRAWAL -> {
                val updated = currentBalance - amountCents
                if (updated < 0L) {
                  throw IllegalStateException("No tienes saldo suficiente en esta meta para retirar ese monto.")
                }
                updated
              }
            }

          transaction.update(
            goalRef,
            mapOf(
              "currentAmountCents" to newBalance,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )

          val txPayload = buildMap<String, Any> {
            put("userId", uid)
            put("goalId", goalId)
            put("goalName", goalName)
            put("type", type.key)
            put("amountCents", amountCents)
            if (trimmedNote.isNotEmpty()) {
              put("note", trimmedNote)
            }
            put("createdAt", FieldValue.serverTimestamp())
          }
          transaction.set(txRef, txPayload)
        }
        .await()
      Result.success(txRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteGoal(goalId: String): Result<Unit> {
    val uid = requireUserId()
    require(goalId.isNotBlank()) { "El identificador de la meta no puede estar vacío." }

    val goalRef = db.collection("users").document(uid).collection("goals").document(goalId)
    val txQuery =
      db.collection("users").document(uid).collection("transactions").whereEqualTo("goalId", goalId)

    return try {
      val txSnapshot = txQuery.get().await()
      val batch = db.batch()
      for (doc in txSnapshot.documents) {
        batch.delete(doc.reference)
      }
      batch.delete(goalRef)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, goalRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteTransaction(transactionItem: SavingsTransaction): Result<Unit> {
    val uid = requireUserId()
    require(transactionItem.id.isNotBlank()) { "El identificador del movimiento no puede estar vacío." }

    val txRef =
      db.collection("users").document(uid).collection("transactions").document(transactionItem.id)
    val goalRef =
      db.collection("users").document(uid).collection("goals").document(transactionItem.goalId)

    return try {
      db.runTransaction { transaction ->
          val goalSnap = transaction.get(goalRef)
          if (goalSnap.exists()) {
            val currentBalance = goalSnap.getLong("currentAmountCents") ?: 0L
            val adjustedBalance =
              if (transactionItem.isDeposit) {
                (currentBalance - transactionItem.amountCents).coerceAtLeast(0L)
              } else {
                currentBalance + transactionItem.amountCents
              }
            transaction.update(
              goalRef,
              mapOf(
                "currentAmountCents" to adjustedBalance,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
            )
          }
          transaction.delete(txRef)
        }
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, txRef.path)
      Result.failure(e)
    }
  }
}
