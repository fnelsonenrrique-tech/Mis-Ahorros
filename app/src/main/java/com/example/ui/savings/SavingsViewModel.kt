package com.example.ui.savings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsTransaction
import com.example.data.model.TransactionType
import com.example.data.preferences.CurrencyPreferences
import com.example.data.repository.SavingsRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SavingsUiState {
  data object Loading : SavingsUiState

  data class Success(
    val goals: List<SavingsGoal>,
    val transactions: List<SavingsTransaction>,
    val totalSavedCents: Long,
    val totalTargetCents: Long,
    val currencySymbol: String,
  ) : SavingsUiState

  data class Error(val message: String) : SavingsUiState
}

enum class MovementFilter(val labelEs: String) {
  ALL("Todos"),
  DEPOSITS("Depósitos"),
  WITHDRAWALS("Retiros"),
}

class SavingsViewModel(
  private val repository: SavingsRepository,
  private val currencyPreferences: CurrencyPreferences,
  private val currentUserId: String,
) : ViewModel() {

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  private val _feedbackMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val feedbackMessages: SharedFlow<String> = _feedbackMessages.asSharedFlow()

  val uiState: StateFlow<SavingsUiState> =
    combine(
        repository.observeGoals(currentUserId),
        repository.observeTransactions(currentUserId),
        currencyPreferences.currencySymbol,
      ) { goals, transactions, currency ->
        val totalSaved = goals.sumOf { it.currentAmountCents }
        val totalTarget = goals.sumOf { it.targetAmountCents }
        SavingsUiState.Success(
          goals = goals,
          transactions = transactions,
          totalSavedCents = totalSaved,
          totalTargetCents = totalTarget,
          currencySymbol = currency,
        ) as SavingsUiState
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing savings data", rootError)
        emit(
          SavingsUiState.Error(
            rootError.localizedMessage ?: "No pudimos cargar tus ahorros en este momento."
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SavingsUiState.Loading,
      )

  fun createGoal(
    name: String,
    targetAmountCents: Long,
    initialAmountCents: Long,
    categoryIcon: String,
    colorKey: String,
    onDone: () -> Unit,
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      val result =
        repository.createGoal(
          name = name,
          targetAmountCents = targetAmountCents,
          initialAmountCents = initialAmountCents,
          categoryIcon = categoryIcon,
          colorKey = colorKey,
        )
      _isSaving.value = false
      result
        .onSuccess {
          _feedbackMessages.tryEmit("Meta \"$name\" creada.")
          onDone()
        }
        .onFailure { err ->
          _feedbackMessages.tryEmit(err.localizedMessage ?: "No se pudo crear la meta.")
        }
    }
  }

  fun updateGoalDetails(
    goalId: String,
    name: String,
    targetAmountCents: Long,
    categoryIcon: String,
    colorKey: String,
    onDone: () -> Unit,
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      val result =
        repository.updateGoalDetails(
          goalId = goalId,
          name = name,
          targetAmountCents = targetAmountCents,
          categoryIcon = categoryIcon,
          colorKey = colorKey,
        )
      _isSaving.value = false
      result
        .onSuccess {
          _feedbackMessages.tryEmit("Meta actualizada.")
          onDone()
        }
        .onFailure { err ->
          _feedbackMessages.tryEmit(err.localizedMessage ?: "No se pudo actualizar la meta.")
        }
    }
  }

  fun recordMovement(
    goalId: String,
    type: TransactionType,
    amountCents: Long,
    note: String,
    onDone: () -> Unit,
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      val result =
        repository.recordMovement(
          goalId = goalId,
          type = type,
          amountCents = amountCents,
          note = note,
        )
      _isSaving.value = false
      result
        .onSuccess {
          val actionLabel = if (type == TransactionType.DEPOSIT) "Depósito registrado." else "Retiro registrado."
          _feedbackMessages.tryEmit(actionLabel)
          onDone()
        }
        .onFailure { err ->
          _feedbackMessages.tryEmit(err.localizedMessage ?: "No se pudo registrar el movimiento.")
        }
    }
  }

  fun deleteGoal(goalId: String, onDone: () -> Unit) {
    viewModelScope.launch {
      _isSaving.value = true
      val result = repository.deleteGoal(goalId)
      _isSaving.value = false
      result
        .onSuccess {
          _feedbackMessages.tryEmit("Meta eliminada.")
          onDone()
        }
        .onFailure { err ->
          _feedbackMessages.tryEmit(err.localizedMessage ?: "No se pudo eliminar la meta.")
        }
    }
  }

  fun deleteTransaction(transaction: SavingsTransaction) {
    viewModelScope.launch {
      val result = repository.deleteTransaction(transaction)
      result
        .onSuccess { _feedbackMessages.tryEmit("Movimiento eliminado y saldo ajustado.") }
        .onFailure { err ->
          _feedbackMessages.tryEmit(err.localizedMessage ?: "No se pudo eliminar el movimiento.")
        }
    }
  }

  fun setCurrencySymbol(symbol: String) {
    viewModelScope.launch { currencyPreferences.setCurrencySymbol(symbol) }
  }

  private companion object {
    const val TAG = "SavingsVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
