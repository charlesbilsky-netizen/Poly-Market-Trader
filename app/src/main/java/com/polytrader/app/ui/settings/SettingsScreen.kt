package com.polytrader.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.polytrader.app.di.AppContainer
import com.polytrader.app.ui.theme.PolyTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val settingsRepo = container.settingsRepository
    val settings by settingsRepo.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var wallet by remember { mutableStateOf("") }
    var gemini by remember { mutableStateOf("") }
    var xai by remember { mutableStateOf("") }
    var openai by remember { mutableStateOf("") }
    var dark by remember { mutableStateOf(true) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        val s = settings ?: return@LaunchedEffect
        if (!loaded) {
            wallet = s.walletAddress
            gemini = s.geminiKey
            xai = s.xaiKey
            openai = s.openaiKey
            dark = s.darkTheme
            loaded = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Portfolio", style = MaterialTheme.typography.titleMedium)
            Text(
                "Paste your public Polymarket proxy-wallet address (the 0x… address in your " +
                    "polymarket.com profile URL). Read-only — this app can never trade or move funds.",
                style = MaterialTheme.typography.bodySmall,
                color = PolyTheme.extras.textMuted,
            )
            OutlinedTextField(
                value = wallet,
                onValueChange = { wallet = it },
                label = { Text("Wallet address (0x…)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Text("AI research keys (bring your own)", style = MaterialTheme.typography.titleMedium)
            Text(
                "All optional. Grok unlocks native X (Twitter) sentiment; Gemini unlocks web-grounded " +
                    "research; OpenAI is a reasoning-only fallback. Keys are stored on-device only.",
                style = MaterialTheme.typography.bodySmall,
                color = PolyTheme.extras.textMuted,
            )
            OutlinedTextField(
                value = xai, onValueChange = { xai = it },
                label = { Text("xAI Grok API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = gemini, onValueChange = { gemini = it },
                label = { Text("Google Gemini API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = openai, onValueChange = { openai = it },
                label = { Text("OpenAI API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dark theme", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Meridian dark is the signature look",
                        style = MaterialTheme.typography.bodySmall,
                        color = PolyTheme.extras.textMuted,
                    )
                }
                Switch(checked = dark, onCheckedChange = { dark = it })
            }

            Button(
                onClick = {
                    scope.launch {
                        val w = wallet.trim()
                        if (w.isNotEmpty() && !Regex("^0x[a-fA-F0-9]{40}$").matches(w)) {
                            snackbar.showSnackbar("Wallet must be a 42-char 0x… address")
                            return@launch
                        }
                        settingsRepo.setWalletAddress(w)
                        settingsRepo.setAiKeys(gemini, xai, openai)
                        settingsRepo.setDarkTheme(dark)
                        snackbar.showSnackbar("Settings saved")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) { Text("Save settings") }

            Text(
                "PolyTrader is a research companion. It never places orders — every trade action " +
                    "opens polymarket.com.",
                style = MaterialTheme.typography.bodySmall,
                color = PolyTheme.extras.textMuted,
            )
        }
    }
}
