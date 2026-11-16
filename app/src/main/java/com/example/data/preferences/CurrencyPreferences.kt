package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ahorros_prefs")

class CurrencyPreferences(private val context: Context) {

  private val currencyKey = stringPreferencesKey("currency_symbol")

  val currencySymbol: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[currencyKey] ?: "$" }

  suspend fun setCurrencySymbol(symbol: String) {
    context.dataStore.edit { prefs -> prefs[currencyKey] = symbol }
  }

  companion object {
    val SUPPORTED_SYMBOLS = listOf("$", "€", "S/", "COL$", "Q")
  }
}
