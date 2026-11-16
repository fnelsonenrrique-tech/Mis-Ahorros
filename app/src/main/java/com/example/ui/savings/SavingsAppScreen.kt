package com.example.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.GoalCategory
import com.example.data.model.GoalColor
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsTransaction
import com.example.data.model.TransactionType
import com.example.data.preferences.CurrencyPreferences
import com.example.data.repository.SavingsRepository
import com.example.ui.auth.signOutUser
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class SavingsTab {
  GOALS,
  HISTORY,
}

private data class GoalEditorRequest(
  val existingGoal: SavingsGoal? = null,
  val prefilledName: String = "",
  val prefilledCategory: GoalCategory = GoalCategory.GENERAL,
  val prefilledColor: GoalColor = GoalColor.EMERALD,
)

private data class MovementRequest(
  val goalId: String? = null,
  val type: TransactionType = TransactionType.DEPOSIT,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsAppScreen(currentUserId: String) {
  val viewModel: SavingsViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            SavingsViewModel(
              repository = SavingsRepository(db),
              currencyPreferences = CurrencyPreferences(app.applicationContext),
              currentUserId = currentUserId,
            )
          }
        },
    )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  var selectedTab by remember { mutableStateOf(SavingsTab.GOALS) }
  var movementFilter by remember { mutableStateOf(MovementFilter.ALL) }
  var goalEditorRequest by remember { mutableStateOf<GoalEditorRequest?>(null) }
  var movementRequest by remember { mutableStateOf<MovementRequest?>(null) }
  var showProfileSheet by remember { mutableStateOf(false) }
  var transactionToDelete by remember { mutableStateOf<SavingsTransaction?>(null) }

  LaunchedEffect(viewModel) {
    viewModel.feedbackMessages.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  val currentUser = Firebase.auth.currentUser
  val successState = uiState as? SavingsUiState.Success

  if (transactionToDelete != null) {
    val tx = transactionToDelete!!
    AlertDialog(
      onDismissRequest = { transactionToDelete = null },
      title = { Text("Eliminar movimiento") },
      text = { Text("Se eliminará este movimiento y se ajustará el saldo de \"${tx.goalName}\".") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteTransaction(tx)
            transactionToDelete = null
          }
        ) {
          Text("Eliminar", color = MaterialTheme.colorScheme.tertiary)
        }
      },
      dismissButton = {
        TextButton(onClick = { transactionToDelete = null }) { Text("Cancelar") }
      },
    )
  }

  goalEditorRequest?.let { req ->
    GoalEditorSheet(
      existingGoal = req.existingGoal,
      prefilledName = req.prefilledName,
      prefilledCategory = req.prefilledCategory,
      prefilledColor = req.prefilledColor,
      currencySymbol = successState?.currencySymbol ?: "$",
      isSaving = isSaving,
      onDismiss = { goalEditorRequest = null },
      onSave = { name, targetCents, initialCents, categoryKey, colorKey ->
        val existing = req.existingGoal
        if (existing == null) {
          viewModel.createGoal(
            name = name,
            targetAmountCents = targetCents,
            initialAmountCents = initialCents,
            categoryIcon = categoryKey,
            colorKey = colorKey,
            onDone = { goalEditorRequest = null },
          )
        } else {
          viewModel.updateGoalDetails(
            goalId = existing.id,
            name = name,
            targetAmountCents = targetCents,
            categoryIcon = categoryKey,
            colorKey = colorKey,
            onDone = { goalEditorRequest = null },
          )
        }
      },
      onDeleteGoal = { goalId ->
        viewModel.deleteGoal(goalId) { goalEditorRequest = null }
      },
    )
  }

  movementRequest?.let { req ->
    val goals = successState?.goals.orEmpty()
    if (goals.isNotEmpty()) {
      MovementSheet(
        goals = goals,
        initialGoalId = req.goalId,
        initialType = req.type,
        currencySymbol = successState?.currencySymbol ?: "$",
        isSaving = isSaving,
        onDismiss = { movementRequest = null },
        onSubmit = { goalId, type, amountCents, note ->
          viewModel.recordMovement(
            goalId = goalId,
            type = type,
            amountCents = amountCents,
            note = note,
            onDone = { movementRequest = null },
          )
        },
      )
    }
  }

  if (showProfileSheet) {
    ProfileSettingsSheet(
      displayName = currentUser?.displayName,
      email = currentUser?.email,
      currentCurrency = successState?.currencySymbol ?: "$",
      onSelectCurrency = { viewModel.setCurrencySymbol(it) },
      onSignOut = {
        showProfileSheet = false
        signOutUser(credentialManager = credentialManager, scope = scope)
      },
      onDismiss = { showProfileSheet = false },
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        CenterAlignedTopAppBar(
          title = {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
          },
          actions = {
            IconButton(
              onClick = { showProfileSheet = true },
              modifier = Modifier.size(48.dp).testTag("profile_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = "Cuenta y moneda",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
              )
            }
          },
          colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = MaterialTheme.colorScheme.background
            ),
        )
      },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.navigationBars,
          ) {
            NavigationBarItem(
              selected = selectedTab == SavingsTab.GOALS,
              onClick = { selectedTab = SavingsTab.GOALS },
              icon = {
                Icon(
                  imageVector =
                    if (selectedTab == SavingsTab.GOALS) Icons.Rounded.Savings
                    else Icons.Outlined.Savings,
                  contentDescription = null,
                )
              },
              label = { Text(stringResource(R.string.tab_goals)) },
              modifier = Modifier.testTag("nav_tab_goals"),
            )
            NavigationBarItem(
              selected = selectedTab == SavingsTab.HISTORY,
              onClick = { selectedTab = SavingsTab.HISTORY },
              icon = {
                Icon(
                  imageVector =
                    if (selectedTab == SavingsTab.HISTORY) Icons.AutoMirrored.Rounded.ReceiptLong
                    else Icons.AutoMirrored.Outlined.ReceiptLong,
                  contentDescription = null,
                )
              },
              label = { Text(stringResource(R.string.tab_history)) },
              modifier = Modifier.testTag("nav_tab_history"),
            )
          }
        }
      },
      floatingActionButton = {
        if (successState != null && selectedTab == SavingsTab.GOALS && successState.goals.isNotEmpty()) {
          ExtendedFloatingActionButton(
            onClick = { goalEditorRequest = GoalEditorRequest() },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.new_goal)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.testTag("fab_new_goal"),
          )
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavigationRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            NavigationRailItem(
              selected = selectedTab == SavingsTab.GOALS,
              onClick = { selectedTab = SavingsTab.GOALS },
              icon = {
                Icon(
                  imageVector =
                    if (selectedTab == SavingsTab.GOALS) Icons.Rounded.Savings
                    else Icons.Outlined.Savings,
                  contentDescription = null,
                )
              },
              label = { Text(stringResource(R.string.tab_goals)) },
            )
            NavigationRailItem(
              selected = selectedTab == SavingsTab.HISTORY,
              onClick = { selectedTab = SavingsTab.HISTORY },
              icon = {
                Icon(
                  imageVector =
                    if (selectedTab == SavingsTab.HISTORY) Icons.AutoMirrored.Rounded.ReceiptLong
                    else Icons.AutoMirrored.Outlined.ReceiptLong,
                  contentDescription = null,
                )
              },
              label = { Text(stringResource(R.string.tab_history)) },
            )
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          contentAlignment = Alignment.TopCenter,
        ) {
          when (val state = uiState) {
            SavingsUiState.Loading -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
              }
            }
            is SavingsUiState.Error -> {
              Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
              ) {
                Surface(
                  shape = MaterialTheme.shapes.medium,
                  color = MaterialTheme.colorScheme.tertiaryContainer,
                  modifier = Modifier.widthIn(max = 460.dp),
                ) {
                  Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                    Text(
                      text = "Ocurrió un problema",
                      style = MaterialTheme.typography.titleLarge,
                      color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      text = state.message,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                  }
                }
              }
            }
            is SavingsUiState.Success -> {
              when (selectedTab) {
                SavingsTab.GOALS -> {
                  GoalsTabContent(
                    state = state,
                    onQuickDeposit = {
                      movementRequest = MovementRequest(type = TransactionType.DEPOSIT)
                    },
                    onQuickWithdraw = {
                      movementRequest = MovementRequest(type = TransactionType.WITHDRAWAL)
                    },
                    onEditGoal = { goal ->
                      goalEditorRequest = GoalEditorRequest(existingGoal = goal)
                    },
                    onGoalDeposit = { goal ->
                      movementRequest =
                        MovementRequest(goalId = goal.id, type = TransactionType.DEPOSIT)
                    },
                    onGoalWithdraw = { goal ->
                      movementRequest =
                        MovementRequest(goalId = goal.id, type = TransactionType.WITHDRAWAL)
                    },
                    onCreateSuggestedGoal = { name, cat, color ->
                      goalEditorRequest =
                        GoalEditorRequest(
                          prefilledName = name,
                          prefilledCategory = cat,
                          prefilledColor = color,
                        )
                    },
                    onCreateCustomGoal = { goalEditorRequest = GoalEditorRequest() },
                  )
                }
                SavingsTab.HISTORY -> {
                  HistoryTabContent(
                    state = state,
                    filter = movementFilter,
                    onFilterChange = { movementFilter = it },
                    onDeleteTransaction = { transactionToDelete = it },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GoalsTabContent(
  state: SavingsUiState.Success,
  onQuickDeposit: () -> Unit,
  onQuickWithdraw: () -> Unit,
  onEditGoal: (SavingsGoal) -> Unit,
  onGoalDeposit: (SavingsGoal) -> Unit,
  onGoalWithdraw: (SavingsGoal) -> Unit,
  onCreateSuggestedGoal: (String, GoalCategory, GoalColor) -> Unit,
  onCreateCustomGoal: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item(key = "hero") {
      TotalSavingsHeroCard(
        totalSavedCents = state.totalSavedCents,
        totalTargetCents = state.totalTargetCents,
        goalCount = state.goals.size,
        currencySymbol = state.currencySymbol,
        onQuickDeposit = onQuickDeposit,
        onQuickWithdraw = onQuickWithdraw,
      )
    }

    if (state.goals.isEmpty()) {
      item(key = "empty_goals") {
        EmptySavingsView(
          onCreateSuggestedGoal = onCreateSuggestedGoal,
          onCreateCustomGoal = onCreateCustomGoal,
        )
      }
    } else {
      item(key = "goals_header") {
        Text(
          text = "Tus metas",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )
      }

      items(items = state.goals, key = { it.id }) { goal ->
        GoalCard(
          goal = goal,
          currencySymbol = state.currencySymbol,
          onClick = { onEditGoal(goal) },
          onDepositClick = { onGoalDeposit(goal) },
          onWithdrawClick = { onGoalWithdraw(goal) },
        )
      }
    }
  }
}

@Composable
private fun HistoryTabContent(
  state: SavingsUiState.Success,
  filter: MovementFilter,
  onFilterChange: (MovementFilter) -> Unit,
  onDeleteTransaction: (SavingsTransaction) -> Unit,
) {
  val filtered =
    remember(state.transactions, filter) {
      when (filter) {
        MovementFilter.ALL -> state.transactions
        MovementFilter.DEPOSITS -> state.transactions.filter { it.isDeposit }
        MovementFilter.WITHDRAWALS -> state.transactions.filter { !it.isDeposit }
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "filter_row") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MovementFilter.entries.forEach { option ->
          FilterChip(
            selected = filter == option,
            onClick = { onFilterChange(option) },
            label = { Text(option.labelEs) },
            modifier = Modifier.testTag("filter_${option.name.lowercase()}"),
          )
        }
      }
    }

    if (filtered.isEmpty()) {
      item(key = "empty_history") {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
          Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Sin movimientos aún",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Cada depósito o retiro que registres en tus metas aparecerá aquí.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(items = filtered, key = { it.id }) { tx ->
        TransactionRow(
          transaction = tx,
          currencySymbol = state.currencySymbol,
          onDeleteClick = { onDeleteTransaction(tx) },
        )
      }
    }
  }
}
