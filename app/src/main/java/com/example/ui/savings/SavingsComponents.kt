package com.example.ui.savings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GoalCategory
import com.example.data.model.GoalColor
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsTransaction
import com.example.ui.theme.AccentOcean
import com.example.ui.theme.AccentOceanSoft
import com.example.ui.theme.AccentPlum
import com.example.ui.theme.AccentPlumSoft
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldVibrant
import com.example.ui.theme.MintContainer
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaTertiary
import com.example.util.MoneyFormatter

fun categoryIconVector(key: String): ImageVector =
  when (GoalCategory.fromKey(key)) {
    GoalCategory.EMERGENCY -> Icons.Rounded.HealthAndSafety
    GoalCategory.TRAVEL -> Icons.Rounded.Flight
    GoalCategory.HOME -> Icons.Rounded.Home
    GoalCategory.EDUCATION -> Icons.Rounded.School
    GoalCategory.TECH -> Icons.Rounded.LaptopMac
    GoalCategory.GIFT -> Icons.Rounded.CardGiftcard
    GoalCategory.GENERAL -> Icons.Rounded.Savings
  }

data class GoalAccentColors(val accent: Color, val softBackground: Color)

fun goalAccentColors(key: String): GoalAccentColors =
  when (GoalColor.fromKey(key)) {
    GoalColor.EMERALD -> GoalAccentColors(EmeraldPrimary, MintContainer)
    GoalColor.AMBER -> GoalAccentColors(AmberSecondary, AmberContainer)
    GoalColor.TERRACOTTA -> GoalAccentColors(TerracottaTertiary, TerracottaContainer)
    GoalColor.OCEAN -> GoalAccentColors(AccentOcean, AccentOceanSoft)
    GoalColor.PLUM -> GoalAccentColors(AccentPlum, AccentPlumSoft)
  }

@Composable
fun TotalSavingsHeroCard(
  totalSavedCents: Long,
  totalTargetCents: Long,
  goalCount: Int,
  currencySymbol: String,
  onQuickDeposit: () -> Unit,
  onQuickWithdraw: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val rawProgress =
    if (totalTargetCents <= 0L) 0f
    else (totalSavedCents.toFloat() / totalTargetCents.toFloat()).coerceIn(0f, 1f)
  val animatedProgress by
    animateFloatAsState(
      targetValue = rawProgress,
      animationSpec = tween(durationMillis = 450),
      label = "hero_progress",
    )
  val overallPercent =
    if (totalTargetCents <= 0L) 0
    else ((totalSavedCents * 100L) / totalTargetCents).toInt().coerceIn(0, 999)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(
          Brush.linearGradient(
            colors = listOf(EmeraldDark, EmeraldPrimary, EmeraldVibrant),
            start = Offset.Zero,
            end = Offset(900f, 700f),
          )
        )
        .drawBehind {
          drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = size.minDimension * 0.62f,
            center = Offset(size.width * 0.9f, size.height * 0.12f),
          )
          drawCircle(
            color = Color.White.copy(alpha = 0.04f),
            radius = size.minDimension * 0.4f,
            center = Offset(size.width * 0.1f, size.height * 0.92f),
          )
        }
        .padding(24.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.total_savings_label),
          style = MaterialTheme.typography.labelLarge,
          color = Color.White.copy(alpha = 0.85f),
        )
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Color.White.copy(alpha = 0.16f),
        ) {
          Text(
            text = if (goalCount == 1) "1 meta activa" else "$goalCount metas activas",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = MoneyFormatter.formatCents(totalSavedCents, currencySymbol),
        style = MaterialTheme.typography.displayLarge,
        color = Color.White,
        modifier = Modifier.testTag("hero_total_balance"),
      )

      if (totalTargetCents > 0L) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Meta combinada: ${MoneyFormatter.formatCents(totalTargetCents, currencySymbol)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
          )
          Text(
            text = "$overallPercent%",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFFCE8C8),
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier =
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
          color = Color(0xFFF5C469),
          trackColor = Color.White.copy(alpha = 0.20f),
          strokeCap = StrokeCap.Round,
        )
      }

      if (goalCount > 0) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Button(
            onClick = onQuickDeposit,
            shape = RoundedCornerShape(16.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = EmeraldDark,
              ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier =
              Modifier.weight(1f).height(48.dp).testTag("hero_deposit_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.deposit_action),
              style = MaterialTheme.typography.labelLarge,
            )
          }

          OutlinedButton(
            onClick = onQuickWithdraw,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier =
              Modifier.weight(1f).height(48.dp).testTag("hero_withdraw_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Remove,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.withdraw_action),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }
}

@Composable
fun GoalCard(
  goal: SavingsGoal,
  currencySymbol: String,
  onClick: () -> Unit,
  onDepositClick: () -> Unit,
  onWithdrawClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accentColors = goalAccentColors(goal.colorKey)
  val animatedProgress by
    animateFloatAsState(
      targetValue = goal.progressFraction,
      animationSpec = tween(durationMillis = 450),
      label = "goal_progress_${goal.id}",
    )

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .clickable(onClick = onClick)
        .testTag("goal_card_${goal.id}"),
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(46.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(accentColors.softBackground),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = categoryIconVector(goal.categoryIcon),
            contentDescription = GoalCategory.fromKey(goal.categoryIcon).labelEs,
            tint = accentColors.accent,
            modifier = Modifier.size(24.dp),
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = goal.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text =
              if (goal.isCompleted) "¡Meta alcanzada!"
              else "Faltan ${MoneyFormatter.formatCents(goal.remainingCents, currencySymbol)}",
            style = MaterialTheme.typography.bodyMedium,
            color =
              if (goal.isCompleted) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Surface(
          shape = RoundedCornerShape(999.dp),
          color =
            if (goal.isCompleted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (goal.isCompleted) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
              text = "${goal.progressPercent}%",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color =
                if (goal.isCompleted) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        Text(
          text = MoneyFormatter.formatCents(goal.currentAmountCents, currencySymbol),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "de ${MoneyFormatter.formatCents(goal.targetAmountCents, currencySymbol)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(999.dp)),
        color = accentColors.accent,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilledTonalButton(
          onClick = onDepositClick,
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.weight(1f).height(48.dp).testTag("goal_deposit_${goal.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.deposit_action))
        }

        OutlinedButton(
          onClick = onWithdrawClick,
          enabled = goal.currentAmountCents > 0L,
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.weight(1f).height(48.dp).testTag("goal_withdraw_${goal.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Remove,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.withdraw_action))
        }
      }
    }
  }
}

@Composable
fun TransactionRow(
  transaction: SavingsTransaction,
  currencySymbol: String,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDeposit = transaction.isDeposit
  val badgeBg =
    if (isDeposit) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.tertiaryContainer
  val badgeTint =
    if (isDeposit) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.tertiary
  val amountText =
    (if (isDeposit) "+" else "-") + MoneyFormatter.formatCents(transaction.amountCents, currencySymbol)

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth().testTag("transaction_row_${transaction.id}"),
  ) {
    Row(
      modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(badgeBg),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector =
            if (isDeposit) Icons.AutoMirrored.Rounded.TrendingUp
            else Icons.AutoMirrored.Rounded.TrendingDown,
          contentDescription = if (isDeposit) "Depósito" else "Retiro",
          tint = badgeTint,
          modifier = Modifier.size(20.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = transaction.goalName.ifBlank { "Meta" },
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val subtitle =
          if (transaction.note.isNotBlank()) {
            "${transaction.note} • ${MoneyFormatter.formatTimestamp(transaction.createdAt)}"
          } else {
            MoneyFormatter.formatTimestamp(transaction.createdAt)
          }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Text(
        text = amountText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = badgeTint,
      )

      IconButton(
        onClick = onDeleteClick,
        modifier = Modifier.size(48.dp).testTag("delete_tx_${transaction.id}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.DeleteOutline,
          contentDescription = "Eliminar movimiento",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptySavingsView(
  onCreateSuggestedGoal: (name: String, category: GoalCategory, color: GoalColor) -> Unit,
  onCreateCustomGoal: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_savings_empty_1794806065641),
        contentDescription = "Alcancía esperando tu primera meta",
        modifier =
          Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(20.dp)),
        contentScale = ContentScale.Crop,
      )
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = "Empieza tu primer ahorro",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Elige una idea rápida o crea una meta personalizada con el monto que quieras alcanzar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(18.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SuggestionChipButton(
          label = "Fondo de emergencia",
          onClick = {
            onCreateSuggestedGoal("Fondo de emergencia", GoalCategory.EMERGENCY, GoalColor.EMERALD)
          },
        )
        SuggestionChipButton(
          label = "Vacaciones",
          onClick = {
            onCreateSuggestedGoal("Vacaciones", GoalCategory.TRAVEL, GoalColor.AMBER)
          },
        )
        SuggestionChipButton(
          label = "Hogar",
          onClick = {
            onCreateSuggestedGoal("Hogar", GoalCategory.HOME, GoalColor.OCEAN)
          },
        )
      }
      Spacer(modifier = Modifier.height(18.dp))
      Button(
        onClick = onCreateCustomGoal,
        shape = RoundedCornerShape(16.dp),
        modifier =
          Modifier.fillMaxWidth().height(52.dp).testTag("empty_create_goal_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.new_goal))
      }
    }
  }
}

@Composable
private fun SuggestionChipButton(label: String, onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
    modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onClick),
  ) {
    Text(
      text = "+ $label",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    )
  }
}
