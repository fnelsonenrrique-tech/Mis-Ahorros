package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isCheckingAutoSignIn by remember { mutableStateOf(true) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingAutoSignIn = false
        onAuthSuccess()
      },
      onUnauthenticated = { isCheckingAutoSignIn = false },
      scope = scope,
    )
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors =
              listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.background,
              )
          )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 520.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          tonalElevation = 0.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Savings,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Image(
          painter = painterResource(id = R.drawable.img_savings_empty_1794806065641),
          contentDescription = "Ilustración de alcancía de ahorros con monedas doradas",
          modifier =
            Modifier.fillMaxWidth()
              .height(210.dp)
              .clip(RoundedCornerShape(28.dp)),
          contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
          text = stringResource(R.string.auth_title),
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = stringResource(R.string.auth_subtitle),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            FeatureHighlightRow(
              icon = Icons.Rounded.AccountBalanceWallet,
              title = "Metas claras",
              subtitle = "Separa tus ahorros por objetivo y mira tu progreso al instante.",
            )
            FeatureHighlightRow(
              icon = Icons.Rounded.CheckCircle,
              title = "Depósitos y retiros rápidos",
              subtitle = "Anota cada movimiento en segundos con un toque.",
            )
            FeatureHighlightRow(
              icon = Icons.Rounded.CloudDone,
              title = "Respaldo automático",
              subtitle = "Tus datos permanecen seguros y sincronizados con tu cuenta.",
            )
          }
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AnimatedVisibility(visible = errorMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          ) {
            Text(
              text = errorMessage.orEmpty(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
          }
        }

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isLoading = false },
            )
          },
          enabled = !isLoading && !isCheckingAutoSignIn,
          shape = RoundedCornerShape(18.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier =
            Modifier.fillMaxWidth()
              .height(56.dp)
              .testTag("google_sign_in_button"),
        ) {
          if (isLoading || isCheckingAutoSignIn) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = if (isCheckingAutoSignIn) "Verificando sesión..." else "Conectando...",
              style = MaterialTheme.typography.labelLarge,
            )
          } else {
            Text(
              text = stringResource(R.string.auth_button_google),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      modifier =
        Modifier.size(36.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
