package com.example.ui.savings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GoalCategory
import com.example.data.model.GoalColor
import com.example.data.model.SavingsGoal
import com.example.data.model.TransactionType
import com.example.data.preferences.CurrencyPreferences
import com.example.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalEditorSheet(
  existingGoal: SavingsGoal?,
  prefilledName: String = "",
  prefilledCategory: GoalCategory = GoalCategory.GENERAL,
  prefilledColor: GoalColor = GoalColor.EMERALD,
  currencySymbol: String,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSave: (name: String, targetCents: Long, initialCents: Long, categoryKey: String, colorKey: String) -> Unit,
  onDeleteGoal: ((String) -> Unit)? = null,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember(existingGoal, prefilledName) {
    mutableStateOf(existingGoal?.name ?: prefilledName)
  }
  var targetInput by remember(existingGoal) {
    mutableStateOf(existingGoal?.let { MoneyFormatter.centsToInputString(it.targetAmountCents) } ?: "")
  }
  var initialInput by remember(existingGoal) { mutableStateOf("") }
  var selectedCategory by remember(existingGoal, prefilledCategory) {
    mutableStateOf(existingGoal?.let { GoalCategory.fromKey(it.categoryIcon) } ?: prefilledCategory)
  }
  var selectedColor by remember(existingGoal, prefilledColor) {
    mutableStateOf(existingGoal?.let { GoalColor.fromKey(it.colorKey) } ?: prefilledColor)
  }
  var errorMsg by remember { mutableStateOf<String?>(null) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  if (showDeleteConfirm && existingGoal != null && onDeleteGoal != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Eliminar meta") },
      text = {
        Text("Se eliminará \"${existingGoal.name}\" junto con su historial de movimientos. Esta acción no se puede deshacer.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirm = false
            onDeleteGoal(existingGoal.id)
          }
        ) {
          Text("Eliminar", color = MaterialTheme.colorScheme.tertiary)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
      },
    )
  }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = if (existingGoal == null) "Nueva meta de ahorro" else "Editar meta",
        style = MaterialTheme.typography.headlineSmall,
      )

      OutlinedTextField(
        value = name,
        onValueChange = {
          if (it.length <= 60) name = it
          errorMsg = null
        },
        label = { Text("Nombre de la meta") },
        placeholder = { Text("Ej. Viaje, Fondo de emergencia...") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth().testTag("goal_name_input"),
      )

      OutlinedTextField(
        value = targetInput,
        onValueChange = {
          targetInput = it
          errorMsg = null
        },
        label = { Text("Monto objetivo ($currencySymbol)") },
        placeholder = { Text("5000") },
        singleLine = true,
        prefix = { Text("$currencySymbol ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("goal_target_input"),
      )

      if (existingGoal == null) {
        OutlinedTextField(
          value = initialInput,
          onValueChange = {
            initialInput = it
            errorMsg = null
          },
          label = { Text("Ahorro inicial (opcional)") },
          placeholder = { Text("0") },
          singleLine = true,
          prefix = { Text("$currencySymbol ") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("goal_initial_input"),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Categoría", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GoalCategory.entries.forEach { category ->
            val selected = category == selectedCategory
            FilterChip(
              selected = selected,
              onClick = { selectedCategory = category },
              label = { Text(category.labelEs) },
              leadingIcon = {
                Icon(
                  imageVector = categoryIconVector(category.key),
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              },
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Color", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          GoalColor.entries.forEach { colorOption ->
            val palette = goalAccentColors(colorOption.key)
            val isSelected = colorOption == selectedColor
            Box(
              modifier =
                Modifier.size(44.dp)
                  .clip(CircleShape)
                  .background(palette.accent)
                  .then(
                    if (isSelected) {
                      Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else Modifier
                  )
                  .clickable { selectedColor = colorOption },
              contentAlignment = Alignment.Center,
            ) {
              if (isSelected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = colorOption.labelEs,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }

      if (errorMsg != null) {
        Text(
          text = errorMsg.orEmpty(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.tertiary,
        )
      }

      Button(
        onClick = {
          val trimmed = name.trim()
          val targetCents = MoneyFormatter.parseAmountToCents(targetInput)
          val initialCents =
            if (initialInput.isBlank()) 0L else MoneyFormatter.parseAmountToCents(initialInput)

          when {
            trimmed.isEmpty() -> errorMsg = "Escribe un nombre para tu meta."
            targetCents == null -> errorMsg = "Ingresa un monto objetivo válido mayor a cero."
            initialCents == null -> errorMsg = "El monto inicial no es válido."
            else ->
              onSave(
                trimmed,
                targetCents,
                initialCents,
                selectedCategory.key,
                selectedColor.key,
              )
          }
        },
        enabled = !isSaving,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("save_goal_button"),
      ) {
        if (isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Text(if (existingGoal == null) "Crear meta" else "Guardar cambios")
        }
      }

      if (existingGoal != null && onDeleteGoal != null) {
        OutlinedButton(
          onClick = { showDeleteConfirm = true },
          enabled = !isSaving,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("delete_goal_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Eliminar meta")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovementSheet(
  goals: List<SavingsGoal>,
  initialGoalId: String?,
  initialType: TransactionType,
  currencySymbol: String,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSubmit: (goalId: String, type: TransactionType, amountCents: Long, note: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedGoalId by remember(initialGoalId, goals) {
    mutableStateOf(initialGoalId ?: goals.firstOrNull()?.id.orEmpty())
  }
  var movementType by remember(initialType) { mutableStateOf(initialType) }
  var amountInput by remember { mutableStateOf("") }
  var noteInput by remember { mutableStateOf("") }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  val selectedGoal = goals.find { it.id == selectedGoalId } ?: goals.firstOrNull()
  val parsedAmountCents = MoneyFormatter.parseAmountToCents(amountInput)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = "Registrar movimiento",
        style = MaterialTheme.typography.headlineSmall,
      )

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TransactionType.entries.forEachIndexed { index, type ->
          SegmentedButton(
            selected = movementType == type,
            onClick = {
              movementType = type
              errorMsg = null
            },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = TransactionType.entries.size),
            modifier = Modifier.testTag("movement_type_${type.key}"),
          ) {
            Text(type.labelEs)
          }
        }
      }

      if (goals.size > 1) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Meta", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            goals.forEach { goal ->
              FilterChip(
                selected = goal.id == selectedGoal?.id,
                onClick = {
                  selectedGoalId = goal.id
                  errorMsg = null
                },
                label = { Text(goal.name) },
              )
            }
          }
        }
      } else if (selectedGoal != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(selectedGoal.name, style = MaterialTheme.typography.titleMedium)
            Text(
              "Saldo: ${MoneyFormatter.formatCents(selectedGoal.currentAmountCents, currencySymbol)}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      OutlinedTextField(
        value = amountInput,
        onValueChange = {
          amountInput = it
          errorMsg = null
        },
        label = { Text("Monto ($currencySymbol)") },
        placeholder = { Text("0.00") },
        singleLine = true,
        prefix = { Text("$currencySymbol ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("movement_amount_input"),
      )

      // Quick amount chips for frictionless entry
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(50L, 100L, 250L, 500L, 1000L).forEach { quickWhole ->
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier =
              Modifier.clip(RoundedCornerShape(999.dp)).clickable {
                val current = MoneyFormatter.parseAmountToCents(amountInput) ?: 0L
                val next = current + (quickWhole * 100L)
                amountInput = MoneyFormatter.centsToInputString(next)
                errorMsg = null
              },
          ) {
            Text(
              text = "+$currencySymbol$quickWhole",
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }
      }

      if (selectedGoal != null && parsedAmountCents != null) {
        val projected =
          if (movementType == TransactionType.DEPOSIT) {
            selectedGoal.currentAmountCents + parsedAmountCents
          } else {
            selectedGoal.currentAmountCents - parsedAmountCents
          }
        val isValidBalance = projected >= 0L
        Text(
          text =
            if (isValidBalance) {
              "Nuevo saldo en ${selectedGoal.name}: ${MoneyFormatter.formatCents(projected, currencySymbol)}"
            } else {
              "Saldo insuficiente en ${selectedGoal.name} (disponible: ${MoneyFormatter.formatCents(selectedGoal.currentAmountCents, currencySymbol)})"
            },
          style = MaterialTheme.typography.bodyMedium,
          color =
            if (isValidBalance) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.tertiary,
        )
      }

      OutlinedTextField(
        value = noteInput,
        onValueChange = { if (it.length <= 120) noteInput = it },
        label = { Text("Nota (opcional)") },
        placeholder = { Text("Ej. Quincena, venta, regalo...") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth().testTag("movement_note_input"),
      )

      if (errorMsg != null) {
        Text(
          text = errorMsg.orEmpty(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.tertiary,
        )
      }

      Button(
        onClick = {
          val goal = selectedGoal
          val cents = parsedAmountCents
          when {
            goal == null -> errorMsg = "Selecciona una meta."
            cents == null -> errorMsg = "Ingresa un monto válido mayor a cero."
            movementType == TransactionType.WITHDRAWAL && cents > goal.currentAmountCents ->
              errorMsg = "No puedes retirar más del saldo actual de la meta."
            else -> onSubmit(goal.id, movementType, cents, noteInput)
          }
        },
        enabled = !isSaving && selectedGoal != null,
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor =
              if (movementType == TransactionType.DEPOSIT) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.tertiary
          ),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("confirm_movement_button"),
      ) {
        if (isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Text(
            if (movementType == TransactionType.DEPOSIT) "Guardar depósito"
            else "Confirmar retiro"
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsSheet(
  displayName: String?,
  email: String?,
  currentCurrency: String,
  onSelectCurrency: (String) -> Unit,
  onSignOut: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Column {
        Text(
          text = displayName?.takeIf { it.isNotBlank() } ?: "Mi cuenta",
          style = MaterialTheme.typography.headlineSmall,
        )
        if (!email.isNullOrBlank()) {
          Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Símbolo de moneda", style = MaterialTheme.typography.titleMedium)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CurrencyPreferences.SUPPORTED_SYMBOLS.forEach { symbol ->
            FilterChip(
              selected = symbol == currentCurrency,
              onClick = { onSelectCurrency(symbol) },
              label = { Text(symbol, fontWeight = FontWeight.SemiBold) },
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      OutlinedButton(
        onClick = onSignOut,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sign_out_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.Logout,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.sign_out))
      }
    }
  }
}
