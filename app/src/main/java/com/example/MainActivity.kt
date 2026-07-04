package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ToastNotification
import com.example.network.TradeOpportunity
import com.example.ui.TradeViewModel
import com.example.ui.components.AlphaReportDialog
import com.example.ui.components.DepthChart
import com.example.ui.components.MarketHistoryChart
import com.example.ui.MathNode
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.text.font.FontStyle
import com.example.network.NewsSentimentResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Background market intelligence: daily digest + whale watch (read-only).
    com.example.work.MarketIntel.schedule(applicationContext)
    if (android.os.Build.VERSION.SDK_INT >= 33 &&
      checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
      android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
      requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
    }

    // A digest/whale notification can deep-link straight into its report.
    val pendingTitle = intent?.getStringExtra(com.example.work.MarketIntel.EXTRA_REPORT_TITLE)
    val pendingContent = intent?.getStringExtra(com.example.work.MarketIntel.EXTRA_REPORT_CONTENT)

    setContent {
      MyApplicationTheme {
        MainScreen(
          pendingReportTitle = pendingTitle,
          pendingReportContent = pendingContent
        )
      }
    }
  }
}

// Custom Colors matching the "High Density" Quant theme exactly
object QuantTheme {
  val background = Color(0xFF1A1C1E)
  val surface = Color(0xFF282A2D)
  val activeSurface = Color(0xFF1E242E)
  val border = Color(0xFF44474E)
  val activeBorder = Color(0xFFD1E1FF)
  val globalInsightsBg = Color(0xFF2D3033)
  val textPrimary = Color(0xFFD1E1FF)
  val textBody = Color(0xFFE2E2E6)
  val textMuted = Color(0xFF8E9199)
  val textSubtle = Color(0xFFC2C7CF)
  val accentGreen = Color(0xFF4ADE80)
  val accentRed = Color(0xFFF87171)
  val accentBlue = Color(0xFF004A77)
  val alertDarkRed = Color(0xFF3D0000)
  val alertLightRed = Color(0xFFFFB4AB)
  val navButtonBg = Color(0xFF33353A)
}

@Composable
fun MainScreen(
  pendingReportTitle: String? = null,
  pendingReportContent: String? = null
) {
  val context = LocalContext.current
  val viewModel: TradeViewModel = viewModel()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val watchedIds by viewModel.watchedIds.collectAsStateWithLifecycle()
  val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
  val savedReports by viewModel.savedReports.collectAsStateWithLifecycle()

  var selectedTradeOpportunity by remember { mutableStateOf<TradeOpportunity?>(null) }
  var notificationMessage by remember { mutableStateOf<String?>(null) }
  var showSettingsModal by remember { mutableStateOf(false) }
  var showSideloadingGuideGlobal by remember { mutableStateOf(false) }
  var apkDownloadUrlGlobal by remember { mutableStateOf("https://github.com/charlesbilsky-netizen/poly-market-trader/releases/latest/download/PolyTrader.apk") }

  // Open a report delivered via notification tap.
  LaunchedEffect(pendingReportContent) {
    if (!pendingReportContent.isNullOrBlank()) {
      viewModel.presentReport(pendingReportTitle ?: "REPORT", pendingReportContent)
    }
  }

  // Set up the event flow listener for high density toast alerts
  LaunchedEffect(Unit) {
    viewModel.eventFlow.collectLatest { message ->
      if (message.startsWith("NOTIFICATION:")) {
        notificationMessage = message.removePrefix("NOTIFICATION: ")
      } else {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      }
    }
  }

  if (notificationMessage != null) {
      AlertDialog(
          onDismissRequest = { notificationMessage = null },
          title = { Text("PolyTrader Alert") },
          text = { Text(notificationMessage ?: "") },
          confirmButton = {
              Button(onClick = { notificationMessage = null }) {
                  Text("Dismiss")
              }
          }
      )
  }

  if (showSettingsModal) {
      SettingsDialog(
          uiState = uiState,
          onDismiss = { showSettingsModal = false },
          onSaveCredentials = { wallet, key, secret, pass, isTest, accountUrl ->
              viewModel.updateApiCredentials(wallet, key, secret, pass, isTest, accountUrl)
          },
          onSaveSystemSettings = { googleApiKey, xaiApiKey, openaiApiKey, demoMode, maxPositionSize, riskTolerance, customInstructions ->
              viewModel.updateSystemSettings(googleApiKey, xaiApiKey, openaiApiKey, demoMode, maxPositionSize, riskTolerance, customInstructions)
          }
      )
  }

  if (showSideloadingGuideGlobal) {
      InstallCompanionAppDialog(
          onDismiss = { showSideloadingGuideGlobal = false },
          apkDownloadUrl = apkDownloadUrlGlobal
      )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    Image(
        painter = painterResource(id = R.drawable.nano_banana_bg_1782519947305),
        contentDescription = null,
        modifier = Modifier.fillMaxSize().alpha(0.3f),
        contentScale = ContentScale.Crop
    )
    Column(
      modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()
    ) {
      // 1. PolyTrader Title Header with Settings toggle
      HeaderBar(
        onRefresh = { viewModel.fetchMarkets() },
        onOpenSettings = { showSettingsModal = true },
        onOpenInstall = { showSideloadingGuideGlobal = true }
      )

      // 1b. App Sideloading Install Banner (QR & Direct Download)
      AppPromoBanner(
        onOpenInstallGuide = { showSideloadingGuideGlobal = true }
      )

      // 2. Main Dashboard Body based on active tab
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        when (uiState.activeTab) {
          "signals" -> SignalsView(
            uiState = uiState,
            viewModel = viewModel,
            watchedIds = watchedIds,
            onOpenUrl = { opportunity ->
              selectedTradeOpportunity = opportunity
            },
            apkDownloadUrl = apkDownloadUrlGlobal,
            onOpenInstallGuide = { showSideloadingGuideGlobal = true }
          )
          "watchlist" -> com.example.ui.components.WatchlistView(
            watchlist = watchlist,
            reports = savedReports,
            onOpenMarket = { entity ->
              selectedTradeOpportunity = TradeOpportunity(
                id = entity.marketId,
                title = entity.title,
                description = "Watchlisted market — live research view.",
                endsAt = "",
                url = entity.url,
                probability = entity.probability,
                delta = 0.0,
                confidenceScore = 0.0,
                confidenceGrade = "—",
                volume = entity.volume,
                liquidity = "—",
                hftSignal = "TRACKED",
                category = entity.category,
                tokenId = entity.tokenId,
                conditionId = entity.conditionId
              )
            },
            onOpenUrl = { url ->
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
              context.startActivity(intent)
            },
            onRemove = { viewModel.removeFromWatchlist(it) },
            onOpenReport = { report -> viewModel.presentReport(report.title.uppercase(), report.content) },
            onDeleteReport = { viewModel.deleteReport(it) }
          )
          "portfolio" -> PortfolioView(
            uiState = uiState,
            onOpenUrl = { url ->
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
              context.startActivity(intent)
            },
            onRefreshClob = { viewModel.fetchClobMarketsAndAnalyze() },
            onRefreshPortfolio = { viewModel.fetchPortfolioPositions() }
          )
          else -> {
            SignalsView(
              uiState = uiState,
              viewModel = viewModel,
              onOpenUrl = { opportunity ->
                selectedTradeOpportunity = opportunity
              },
              apkDownloadUrl = apkDownloadUrlGlobal,
              onOpenInstallGuide = { showSideloadingGuideGlobal = true }
            )
          }
        }
      }

      // 3. Custom Bottom Navigation with streamlined 2 tabs
      BottomNavBar(
        activeTab = uiState.activeTab,
        onTabSelected = { viewModel.setTab(it) }
      )
    }

    // Modal Market Research Preview
    selectedTradeOpportunity?.let { opportunity ->
      MovoView(
        opportunity = opportunity,
        uiState = uiState,
        onDismiss = { selectedTradeOpportunity = null },
        onOpenUrl = { url ->
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
          context.startActivity(intent)
        },
        onLoadResearch = { op -> viewModel.loadMarketResearch(op) },
        onSelectRange = { op, range -> viewModel.setResearchRange(op, range) },
        onCorrelate = { a, b -> viewModel.correlateMarkets(a, b) }
      )
    }

    // Alpha Report / research library viewer
    if (uiState.showAlphaDialog) {
      AlphaReportDialog(
        report = uiState.alphaReport,
        isGenerating = uiState.isAlphaGenerating,
        title = uiState.alphaReportTitle,
        onDismiss = { viewModel.dismissAlphaDialog() },
        onRegenerate = { viewModel.generateAlphaReport() }
      )
    }

    // 4. Real-time Toast Notification Overlay System
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(bottom = 100.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        uiState.toastNotifications.forEach { toast ->
          ToastNotificationOverlay(
            notification = toast,
            onDismiss = { viewModel.dismissNotification(toast.id) },
            onAction = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(toast.url))
              context.startActivity(intent)
              viewModel.dismissNotification(toast.id)
            }
          )
        }
      }
    }
  }
}

@Composable
fun HeaderBar(
  onRefresh: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenInstall: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(QuantTheme.background)
      .border(1.dp, QuantTheme.border)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(QuantTheme.activeBorder),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.AutoMirrored.Filled.TrendingUp,
          contentDescription = "monitoring",
          tint = Color(0xFF002F66),
          modifier = Modifier.size(24.dp)
        )
      }
      Column {
        Text(
          "POLYTRADER",
          color = QuantTheme.textPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp
        )
        Text(
          "PREDICTION MARKETS",
          color = QuantTheme.textMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.sp
        )
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Button(
        onClick = onOpenInstall,
        colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentGreen),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(30.dp)
      ) {
        Text(
          "📲 GET APP",
          color = Color.Black,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      IconButton(
        onClick = onOpenSettings,
        modifier = Modifier
          .size(30.dp)
          .background(QuantTheme.navButtonBg, CircleShape)
      ) {
        Icon(
          Icons.Default.Settings,
          contentDescription = "Settings",
          tint = QuantTheme.textPrimary,
          modifier = Modifier.size(14.dp)
        )
      }

      IconButton(
        onClick = onRefresh,
        modifier = Modifier
          .size(30.dp)
          .background(QuantTheme.navButtonBg, CircleShape)
      ) {
        Icon(
          Icons.Default.Refresh,
          contentDescription = "Sync",
          tint = QuantTheme.textPrimary,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

@Composable
fun GlobalInsightsBar(nodeStatus: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(QuantTheme.globalInsightsBg)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        "Sourcing Node:",
        color = QuantTheme.textSubtle,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
      )
      Text(
        nodeStatus,
        color = QuantTheme.textPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.offset(y = (-1).dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(modifier = Modifier.size(6.dp).background(QuantTheme.accentGreen, CircleShape))
        Text("142 Math Nodes", color = QuantTheme.textSubtle, fontSize = 13.sp)
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(modifier = Modifier.size(6.dp).background(QuantTheme.accentGreen, CircleShape))
        Text("82 Quant Hubs", color = QuantTheme.textSubtle, fontSize = 13.sp)
      }
    }
  }
}

@Composable
fun BottomNavBar(
  activeTab: String,
  onTabSelected: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(QuantTheme.background)
      .border(1.dp, QuantTheme.border)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    NavBarItem(
      label = "Discover",
      icon = Icons.Default.Bolt,
      isActive = activeTab == "signals",
      onClick = { onTabSelected("signals") }
    )
    NavBarItem(
      label = "Watchlist",
      icon = Icons.Default.Star,
      isActive = activeTab == "watchlist",
      onClick = { onTabSelected("watchlist") }
    )
    NavBarItem(
      label = "Portfolio",
      icon = Icons.Default.AccountBalanceWallet,
      isActive = activeTab == "portfolio",
      onClick = { onTabSelected("portfolio") }
    )
  }
}

@Composable
fun RowScope.NavBarItem(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isActive: Boolean,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .weight(1f)
      .clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(
      modifier = Modifier
        .size(48.dp, 32.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(if (isActive) QuantTheme.accentBlue else Color.Transparent),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        icon,
        contentDescription = label,
        tint = if (isActive) QuantTheme.textPrimary else QuantTheme.textSubtle,
        modifier = Modifier.size(20.dp)
      )
    }
    Text(
      label,
      color = if (isActive) QuantTheme.textPrimary else QuantTheme.textSubtle,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun SettingsDialog(
  uiState: com.example.ui.TradeUiState,
  onDismiss: () -> Unit,
  onSaveCredentials: (String, String, String, String, Boolean, String) -> Unit,
  onSaveSystemSettings: (String, String, String, Boolean, Double, Double, String) -> Unit
) {
  val context = LocalContext.current
  var walletAddress by remember(uiState.polymarketWallet) { mutableStateOf(uiState.polymarketWallet) }
  var apiKey by remember(uiState.polymarketApiKey) { mutableStateOf(uiState.polymarketApiKey) }
  var apiSecret by remember(uiState.polymarketApiSecret) { mutableStateOf(uiState.polymarketApiSecret) }
  var apiPassphrase by remember(uiState.polymarketApiPassphrase) { mutableStateOf(uiState.polymarketApiPassphrase) }
  var googleApiKey by remember(uiState.googleApiKey) { mutableStateOf(uiState.googleApiKey) }
  var xaiApiKey by remember(uiState.xaiApiKey) { mutableStateOf(uiState.xaiApiKey) }
  var openaiApiKey by remember(uiState.openaiApiKey) { mutableStateOf(uiState.openaiApiKey) }
  var demoMode by remember(uiState.demoMode) { mutableStateOf(uiState.demoMode) }
  var maxPositionSize by remember(uiState.maxPositionSize) { mutableStateOf(uiState.maxPositionSize.toFloat()) }
  var riskTolerance by remember(uiState.riskTolerance) { mutableStateOf(uiState.riskTolerance.toFloat()) }
  var customInstructions by remember(uiState.customInstructions) { mutableStateOf(uiState.customInstructions) }

  // Visibility states for masking toggle
  var showApiKeySecret by remember { mutableStateOf(false) }
  var showApiPassphrase by remember { mutableStateOf(false) }
  var showGoogleApiKey by remember { mutableStateOf(false) }
  var showXaiApiKey by remember { mutableStateOf(false) }
  var showOpenaiApiKey by remember { mutableStateOf(false) }

  // Bundled legal document viewer (Privacy Policy / Terms)
  var apkDownloadUrl by remember { mutableStateOf("https://github.com/charlesbilsky-netizen/poly-market-trader/releases/latest/download/PolyTrader.apk") }
  var showSideloadingGuide by remember { mutableStateOf(false) }

  var legalDoc by remember { mutableStateOf<Pair<String, String>?>(null) }
  legalDoc?.let { (title, body) ->
    AlertDialog(
      onDismissRequest = { legalDoc = null },
      title = { Text(title, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          body,
          fontSize = 12.sp,
          lineHeight = 17.sp,
          modifier = Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
        )
      },
      confirmButton = {
        Button(onClick = { legalDoc = null }) { Text("Close") }
      }
    )
  }

  if (showSideloadingGuide) {
    AlertDialog(
      onDismissRequest = { showSideloadingGuide = false },
      containerColor = QuantTheme.surface,
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("📱", fontSize = 24.sp)
          Text(
            "Quick Android Install Guide",
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = QuantTheme.textPrimary
          )
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            "Follow these 4 super simple steps to get PolyTrader running on your phone in under a minute! 🚀",
            color = QuantTheme.textBody,
            fontSize = 13.sp,
            lineHeight = 18.sp
          )

          SimpleStepItem(
            number = "1",
            title = "Scan & Open",
            desc = "Open your Android phone's regular Camera app and scan the QR code to open the download page.",
            emoji = "📸"
          )

          SimpleStepItem(
            number = "2",
            title = "Download Anyway",
            desc = "Tap \"Download APK\". If your phone asks \"File might be harmful?\", don't worry! Tap \"Download anyway\".",
            emoji = "⬇️"
          )

          SimpleStepItem(
            number = "3",
            title = "Allow in Settings",
            desc = "Open the downloaded file. If Android blocks it, tap \"Settings\" on the pop-up and toggle \"Allow from this source\" on.",
            emoji = "⚙️"
          )

          SimpleStepItem(
            number = "4",
            title = "Install & Open!",
            desc = "Tap \"Install\". If Play Protect prompts you, tap \"More details\" then \"Install anyway\". You're done! 🎉",
            emoji = "✅"
          )

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(QuantTheme.background, RoundedCornerShape(8.dp))
              .border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp))
              .padding(12.dp)
          ) {
            Text(
              "🔒 Safe & Secure: PolyTrader is an offline research companion and requires zero sensitive device permissions.",
              color = QuantTheme.accentBlue,
              fontSize = 11.sp,
              lineHeight = 15.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { showSideloadingGuide = false },
          colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentGreen),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("I'm Ready! Let's Go", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.9f)
        .padding(horizontal = 16.dp, vertical = 24.dp),
      shape = RoundedCornerShape(16.dp),
      color = QuantTheme.surface,
      border = BorderStroke(1.dp, QuantTheme.border)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "PolyTrader Configuration",
            color = QuantTheme.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = QuantTheme.textMuted)
          }
        }

        HorizontalDivider(color = QuantTheme.border)

        // Sandbox Mode Section
        Text(
          "Execution Mode",
          color = QuantTheme.accentGreen,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(top = 8.dp)
        )
        
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.background, RoundedCornerShape(12.dp))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Sandbox Trading Mode", color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Runs in safe demo sandbox without real funds", color = QuantTheme.textMuted, fontSize = 12.sp)
          }
          Switch(
            checked = demoMode,
            onCheckedChange = { demoMode = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = QuantTheme.textPrimary,
              checkedTrackColor = QuantTheme.accentBlue,
              uncheckedThumbColor = QuantTheme.textMuted,
              uncheckedTrackColor = QuantTheme.surface
            )
          )
        }

        // Wallet & API Section (Only relevant if not demo mode... actually let's just group them)
        Text(
          "Polymarket Credentials",
          color = QuantTheme.accentGreen,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(top = 16.dp)
        )

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.background, RoundedCornerShape(12.dp))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Wallet address
          OutlinedTextField(
            value = walletAddress,
            onValueChange = { walletAddress = it },
            label = { Text("Your Wallet Address (Polygon)", color = QuantTheme.textMuted) },
            placeholder = { Text("0x...", color = QuantTheme.textSubtle) },
            textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = QuantTheme.activeBorder,
              unfocusedBorderColor = QuantTheme.border,
              focusedLabelColor = QuantTheme.textPrimary,
              unfocusedLabelColor = QuantTheme.textMuted
            ),
            modifier = Modifier.fillMaxWidth()
          )

          // API Access Key
          OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Polymarket CLOB API Key", color = QuantTheme.textMuted) },
            textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = QuantTheme.activeBorder,
              unfocusedBorderColor = QuantTheme.border,
              focusedLabelColor = QuantTheme.textPrimary,
              unfocusedLabelColor = QuantTheme.textMuted
            ),
            modifier = Modifier.fillMaxWidth()
          )

          // API Secret Key
          OutlinedTextField(
            value = apiSecret,
            onValueChange = { apiSecret = it },
            label = { Text("Polymarket CLOB Secret Key", color = QuantTheme.textMuted) },
            textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
            visualTransformation = if (showApiKeySecret) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showApiKeySecret = !showApiKeySecret }) {
                Icon(
                  imageVector = if (showApiKeySecret) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = if (showApiKeySecret) "Hide" else "Show",
                  tint = QuantTheme.textMuted
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = QuantTheme.activeBorder,
              unfocusedBorderColor = QuantTheme.border,
              focusedLabelColor = QuantTheme.textPrimary,
              unfocusedLabelColor = QuantTheme.textMuted
            ),
            modifier = Modifier.fillMaxWidth()
          )

          // API Passphrase
          OutlinedTextField(
            value = apiPassphrase,
            onValueChange = { apiPassphrase = it },
            label = { Text("Polymarket CLOB Passphrase", color = QuantTheme.textMuted) },
            textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
            visualTransformation = if (showApiPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showApiPassphrase = !showApiPassphrase }) {
                Icon(
                  imageVector = if (showApiPassphrase) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = if (showApiPassphrase) "Hide" else "Show",
                  tint = QuantTheme.textMuted
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = QuantTheme.activeBorder,
              unfocusedBorderColor = QuantTheme.border,
              focusedLabelColor = QuantTheme.textPrimary,
              unfocusedLabelColor = QuantTheme.textMuted
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }

        Text(
          "AI Providers",
          color = QuantTheme.accentGreen,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(top = 16.dp)
        )

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.background, RoundedCornerShape(12.dp))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Gemini AI Assistant key
          OutlinedTextField(
            value = googleApiKey,
            onValueChange = { googleApiKey = it },
            label = { Text("Google AI Assistant API Key", color = QuantTheme.textMuted) },
            placeholder = { Text("AI Studio Gemini Key", color = QuantTheme.textSubtle) },
            textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
            visualTransformation = if (showGoogleApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showGoogleApiKey = !showGoogleApiKey }) {
                Icon(
                  imageVector = if (showGoogleApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = if (showGoogleApiKey) "Hide" else "Show",
                  tint = QuantTheme.textMuted
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = QuantTheme.activeBorder,
              unfocusedBorderColor = QuantTheme.border,
              focusedLabelColor = QuantTheme.textPrimary,
              unfocusedLabelColor = QuantTheme.textMuted
            ),
            modifier = Modifier.fillMaxWidth()
          )

          // xAI Grok Key
          OutlinedTextField(
            value = xaiApiKey,
            onValueChange = { xaiApiKey = it },
            label = { Text("xAI Grok API Key", color = QuantTheme.textMuted) },
            placeholder = { Text("xAI Grok Key", color = QuantTheme.textSubtle) },
            textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
            visualTransformation = if (showXaiApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showXaiApiKey = !showXaiApiKey }) {
                Icon(
                  imageVector = if (showXaiApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = if (showXaiApiKey) "Hide" else "Show",
                  tint = QuantTheme.textMuted
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = QuantTheme.activeBorder,
              unfocusedBorderColor = QuantTheme.border,
              focusedLabelColor = QuantTheme.textPrimary,
              unfocusedLabelColor = QuantTheme.textMuted
            ),
            modifier = Modifier.fillMaxWidth().testTag("xai_api_key_input")
          )

          // OpenAI Key
          OutlinedTextField(
            value = openaiApiKey,
            onValueChange = { openaiApiKey = it },
            label = { Text("OpenAI API Key", color = QuantTheme.textMuted) },
            placeholder = { Text("OpenAI Key", color = QuantTheme.textSubtle) },
            textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
            visualTransformation = if (showOpenaiApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showOpenaiApiKey = !showOpenaiApiKey }) {
                Icon(
                  imageVector = if (showOpenaiApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = if (showOpenaiApiKey) "Hide" else "Show",
                  tint = QuantTheme.textMuted
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = QuantTheme.activeBorder,
              unfocusedBorderColor = QuantTheme.border,
              focusedLabelColor = QuantTheme.textPrimary,
              unfocusedLabelColor = QuantTheme.textMuted
            ),
            modifier = Modifier.fillMaxWidth().testTag("openai_api_key_input")
          )
        }

        Text(
          "AI Agent Instructions",
          color = QuantTheme.accentGreen,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(top = 16.dp)
        )

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.background, RoundedCornerShape(12.dp))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // AI suggestion guidelines (Custom Instructions)
          OutlinedTextField(
            value = customInstructions,
            onValueChange = { customInstructions = it },
            label = { Text("Custom AI Research Prompts", color = QuantTheme.textMuted) },
            placeholder = { Text("e.g. Focus on political and crypto high stakes predictions.", color = QuantTheme.textSubtle) },
            textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
            minLines = 3,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = QuantTheme.activeBorder,
              unfocusedBorderColor = QuantTheme.border,
              focusedLabelColor = QuantTheme.textPrimary,
              unfocusedLabelColor = QuantTheme.textMuted
            ),
            modifier = Modifier.fillMaxWidth()
          )

          Text("Templates:", color = QuantTheme.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
          
          androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            item {
              Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(QuantTheme.surface).border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp)).clickable {
                customInstructions = "Act as an expert Polymarket trader. Focus strictly on political and high-stakes macro markets. Flag if volume is below $50k. Always extract implied probabilities."
              }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("Political Focus", color = QuantTheme.textPrimary, fontSize = 12.sp)
              }
            }
            item {
              Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(QuantTheme.surface).border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp)).clickable {
                customInstructions = "Act as a contrarian crypto trader. Weigh recent news sentiment heavily over historical base rates. Look for overreactions."
              }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("Contrarian Crypto", color = QuantTheme.textPrimary, fontSize = 12.sp)
              }
            }
            item {
              Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(QuantTheme.surface).border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp)).clickable {
                customInstructions = "Act as a quantitative analyst. Prioritize markets with tight spreads and high liquidity. Summarize the risk/reward ratio mathematically."
              }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("Quant Mode", color = QuantTheme.textPrimary, fontSize = 12.sp)
              }
            }
          }
        }

        Text(
          "System Settings",
          color = QuantTheme.accentGreen,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(top = 16.dp)
        )

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.background, RoundedCornerShape(12.dp))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Max trade size
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Max Trade Size (USDC)", color = QuantTheme.textBody, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("${maxPositionSize.toInt()} USDC", color = QuantTheme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Slider(
              value = maxPositionSize,
              onValueChange = { maxPositionSize = it },
              valueRange = 10f..1000f,
              colors = SliderDefaults.colors(
                thumbColor = QuantTheme.textPrimary,
                activeTrackColor = QuantTheme.accentBlue,
                inactiveTrackColor = QuantTheme.surface
              )
            )
          }

          // Risk multiplier
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Risk Margin Tolerance", color = QuantTheme.textBody, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text(String.format("%.2f", riskTolerance), color = QuantTheme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Slider(
              value = riskTolerance,
              onValueChange = { riskTolerance = it },
              valueRange = 0.05f..1.0f,
              colors = SliderDefaults.colors(
                thumbColor = QuantTheme.textPrimary,
                activeTrackColor = QuantTheme.accentBlue,
                inactiveTrackColor = QuantTheme.surface
              )
            )
          }
        }

        // Mobile Installation & APK Sideloading Section
        HorizontalDivider(color = QuantTheme.border)
        Text(
          "MOBILE INSTALLATION & APK",
          color = QuantTheme.accentGreen,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 0.5.sp
        )

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.background, RoundedCornerShape(12.dp))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            "Install Instantly on your Android 📲",
            color = QuantTheme.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
          )
          Text(
            "Installing is simple! Just follow these three quick steps:",
            color = QuantTheme.textBody,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
          )

          // Beautiful Step Indicators
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(QuantTheme.surface, RoundedCornerShape(8.dp))
              .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text("📸", fontSize = 20.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text("1. Scan QR", fontWeight = FontWeight.Bold, color = QuantTheme.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Text("➔", color = QuantTheme.textMuted, fontSize = 14.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text("⬇️", fontSize = 20.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text("2. Download", fontWeight = FontWeight.Bold, color = QuantTheme.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Text("➔", color = QuantTheme.textMuted, fontSize = 14.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text("🎉", fontSize = 20.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text("3. Enjoy!", fontWeight = FontWeight.Bold, color = QuantTheme.accentGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
          }

          // QR Code Container
          Box(
            modifier = Modifier
              .size(150.dp)
              .background(Color.White, RoundedCornerShape(12.dp))
              .border(2.dp, QuantTheme.accentGreen, RoundedCornerShape(12.dp))
              .padding(10.dp),
            contentAlignment = Alignment.Center
          ) {
            val bitMatrix = remember(apkDownloadUrl) {
              try {
                com.google.zxing.MultiFormatWriter().encode(
                  apkDownloadUrl,
                  com.google.zxing.BarcodeFormat.QR_CODE,
                  250,
                  250
                )
              } catch (e: Throwable) {
                null
              }
            }

            if (bitMatrix != null) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val matrixWidth = bitMatrix.width
                val matrixHeight = bitMatrix.height
                val pixelWidth = size.width / matrixWidth
                val pixelHeight = size.height / matrixHeight

                drawRect(color = Color.White)

                for (x in 0 until matrixWidth) {
                  for (y in 0 until matrixHeight) {
                    if (bitMatrix.get(x, y)) {
                      drawRect(
                        color = Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(x * pixelWidth, y * pixelHeight),
                        size = androidx.compose.ui.geometry.Size(pixelWidth + 0.1f, pixelHeight + 0.1f)
                      )
                    }
                  }
                }
              }
            } else {
              Text("QR Error", color = Color.Red, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
          }

          Text(
            "Scan the code above with your phone's camera to install directly!",
            color = QuantTheme.accentGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )

          HorizontalDivider(color = QuantTheme.border, thickness = 0.5.dp)

          // Text Field & Buttons
          OutlinedTextField(
            value = apkDownloadUrl,
            onValueChange = { apkDownloadUrl = it },
            label = { Text("Direct Link URL", color = QuantTheme.textMuted) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = QuantTheme.textPrimary,
              unfocusedTextColor = QuantTheme.textBody,
              focusedBorderColor = QuantTheme.accentBlue,
              unfocusedBorderColor = QuantTheme.border,
              cursorColor = QuantTheme.accentBlue
            ),
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val clipManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clipData = android.content.ClipData.newPlainText("PolyTrader APK Link", apkDownloadUrl)
                clipManager.setPrimaryClip(clipData)
                Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.surface),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, tint = QuantTheme.textPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy Link", color = QuantTheme.textPrimary, fontSize = 12.sp)
            }

            Button(
              onClick = {
                try {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkDownloadUrl))
                  context.startActivity(intent)
                } catch (e: Exception) {
                  Toast.makeText(context, "Error: Unable to open download link.", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentBlue),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1.2f)
            ) {
              Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Download APK", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          Button(
            onClick = { showSideloadingGuide = true },
            colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show Easy Picture Guide", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
          }
        }

        // Legal — bundled Privacy Policy & Terms (offline, Play-policy friendly)
        HorizontalDivider(color = QuantTheme.border)
        Text(
          "LEGAL",
          color = QuantTheme.textMuted,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 0.5.sp
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = { legalDoc = com.example.ui.LegalTexts.PRIVACY_TITLE to com.example.ui.LegalTexts.PRIVACY },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Privacy Policy", fontSize = 12.sp)
          }
          OutlinedButton(
            onClick = { legalDoc = com.example.ui.LegalTexts.TERMS_TITLE to com.example.ui.LegalTexts.TERMS },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Terms & Conditions", fontSize = 12.sp)
          }
        }
        Text(
          "PolyTrader is research-only: no trading execution, no fund custody, no financial advice. Not affiliated with Polymarket.",
          color = QuantTheme.textMuted,
          fontSize = 10.sp,
          lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = {
            val cleanWallet = walletAddress.trim()
            val cleanApiKey = apiKey.trim()
            val cleanApiSecret = apiSecret.trim()
            val cleanApiPassphrase = apiPassphrase.trim()
            val cleanGoogleApiKey = googleApiKey.trim()

            // 1. Wallet Address Check
            if (cleanWallet.isEmpty()) {
              Toast.makeText(context, "Validation Error: Wallet address cannot be empty.", Toast.LENGTH_LONG).show()
              return@Button
            }
            if (!cleanWallet.startsWith("0x") || cleanWallet.length != 42) {
              Toast.makeText(context, "Validation Error: Wallet address must be a valid 42-character hex address starting with 0x.", Toast.LENGTH_LONG).show()
              return@Button
            }

            // 2. CLOB API keys check when Sandbox Mode (demoMode) is disabled
            if (!demoMode) {
              if (cleanApiKey.isEmpty()) {
                Toast.makeText(context, "Validation Error: Polymarket CLOB API Key is required when Sandbox Mode is off.", Toast.LENGTH_LONG).show()
                return@Button
              }
              if (cleanApiSecret.isEmpty()) {
                Toast.makeText(context, "Validation Error: Polymarket CLOB Secret Key is required when Sandbox Mode is off.", Toast.LENGTH_LONG).show()
                return@Button
              }
              if (cleanApiPassphrase.isEmpty()) {
                Toast.makeText(context, "Validation Error: Polymarket CLOB Passphrase is required when Sandbox Mode is off.", Toast.LENGTH_LONG).show()
                return@Button
              }
            }

            val cleanXaiApiKey = xaiApiKey.trim()
            val cleanOpenaiApiKey = openaiApiKey.trim()

            // 3. AI Key check
            if (cleanGoogleApiKey.isEmpty() && cleanXaiApiKey.isEmpty() && cleanOpenaiApiKey.isEmpty()) {
              Toast.makeText(context, "Validation Error: At least one AI API Key (Google, xAI Grok, or OpenAI) is required.", Toast.LENGTH_LONG).show()
              return@Button
            }

            // All validations passed, invoke callbacks and show success toast
            onSaveCredentials(cleanWallet, cleanApiKey, cleanApiSecret, cleanApiPassphrase, false, uiState.polymarketAccountUrl)
            onSaveSystemSettings(cleanGoogleApiKey, cleanXaiApiKey, cleanOpenaiApiKey, demoMode, maxPositionSize.toDouble(), riskTolerance.toDouble(), customInstructions)
            Toast.makeText(context, "Configurations validated and saved successfully!", Toast.LENGTH_SHORT).show()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.textPrimary),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Save Configurations", color = Color(0xFF002F66), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun SignalsView(
  uiState: com.example.ui.TradeUiState,
  viewModel: TradeViewModel,
  onOpenUrl: (TradeOpportunity) -> Unit,
  watchedIds: Set<String> = emptySet(),
  apkDownloadUrl: String,
  onOpenInstallGuide: () -> Unit
) {
  val allOpportunities = (uiState.topOpportunities + uiState.opportunities).distinctBy { it.id }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Simplified Top Action Row
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            "ACTIVE PREDICTION MARKETS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = QuantTheme.textPrimary
          )
          Text(
            "Tap any option to make an instant prediction.",
            style = MaterialTheme.typography.bodySmall,
            color = QuantTheme.textMuted
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Elite forecaster deep scan over the live market universe
          Button(
            onClick = { viewModel.generateAlphaReport() },
            colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentGreen),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            enabled = !uiState.isAlphaGenerating
          ) {
            Icon(
              Icons.Default.AutoAwesome,
              contentDescription = "Alpha scan",
              modifier = Modifier.size(18.dp),
              tint = Color(0xFF111315)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Alpha", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111315))
          }

          Button(
            onClick = { viewModel.fetchMarkets() },
            colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentBlue),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Scan", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Scan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Inline QR Code and Easy 1-2-3 Sideloading Instructions for anyone!
    item {
      InlineInstallQrCard(
        apkDownloadUrl = apkDownloadUrl,
        onOpenFullGuide = onOpenInstallGuide
      )
    }

    // AI News Search & Overreaction Engine Card
    item {
      var searchQuery by remember { mutableStateOf("") }
      val context = LocalContext.current
      
      Column(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search news topics (e.g. 'Nvidia', 'Fed rates')", color = QuantTheme.textMuted, fontSize = 14.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody, fontSize = 14.sp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = QuantTheme.accentGreen,
              unfocusedBorderColor = QuantTheme.border,
              focusedContainerColor = QuantTheme.surface,
              unfocusedContainerColor = QuantTheme.surface
            ),
            modifier = Modifier.fillMaxWidth().testTag("news_search_input"),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
               Icon(
                 imageVector = Icons.Default.Search,
                 contentDescription = "Search news",
                 tint = QuantTheme.accentGreen,
                 modifier = Modifier.size(20.dp)
               )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = QuantTheme.textMuted
                  )
                }
              }
            },
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
              onDone = {
                  if (searchQuery.trim().isEmpty()) {
                    Toast.makeText(context, "Please enter a news topic.", Toast.LENGTH_SHORT).show()
                  } else {
                    viewModel.generateNewsTradeOpportunities(searchQuery.trim())
                  }
              }
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search)
          )
          
          Spacer(modifier = Modifier.height(12.dp))
          
          // Quick tags
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val tags = listOf("Fed Rate Decision", "AI Chip News", "Solana ETF")
            tags.forEach { tag ->
              Box(
                modifier = Modifier
                  .background(QuantTheme.navButtonBg, RoundedCornerShape(16.dp))
                  .border(1.dp, QuantTheme.border, RoundedCornerShape(16.dp))
                  .clickable {
                    searchQuery = tag
                    viewModel.generateNewsTradeOpportunities(tag)
                  }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(tag, color = QuantTheme.textSubtle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
      }
    }

    if (uiState.isLoading && allOpportunities.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = QuantTheme.textPrimary)
        }
      }
    } else if (uiState.error != null && allOpportunities.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = QuantTheme.surface),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, QuantTheme.border)
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("Connection Warning", color = QuantTheme.accentRed, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(uiState.error ?: "Unable to sync with prediction feed.", color = QuantTheme.textBody, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { viewModel.fetchMarkets() },
              colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentBlue),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Retry Sync")
            }
          }
        }
      }
    } else {
      items(allOpportunities) { op ->
        StandardOpportunityCard(
          opportunity = op,
          aiAnalysis = uiState.aiAnalysis[op.id],
          grokAnalysis = uiState.grokAnalysis[op.id],
          openaiAnalysis = uiState.openaiAnalysis[op.id],
          quantResult = uiState.quantAnalysis[op.id],
          newsSentimentResult = uiState.newsSentiment[op.id],
          onAnalyze = { viewModel.analyzeMarket(op) },
          onTrade = { onOpenUrl(op) },
          isWatched = op.id in watchedIds,
          onToggleWatch = { viewModel.toggleWatchlist(op) }
        )
      }
    }
  }
}

@Composable
fun NewsSentimentView(sentiment: NewsSentimentResult, originalConfidence: Double) {
  val isPositive = sentiment.sentimentScore > 0.1
  val isNegative = sentiment.sentimentScore < -0.1
  val sentimentColor = when {
    isPositive -> Color(0xFF00C853) // Green
    isNegative -> Color(0xFFFF1744) // Red
    else -> Color(0xFFB0BEC5) // Grey
  }
  
  val badgeBgColor = when {
    isPositive -> Color(0xFF1B5E20).copy(alpha = 0.2f)
    isNegative -> Color(0xFFB71C1C).copy(alpha = 0.2f)
    else -> Color(0xFF37474F).copy(alpha = 0.2f)
  }

  Spacer(modifier = Modifier.height(8.dp))
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFF0F1B2A), RoundedCornerShape(8.dp))
      .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
      .padding(10.dp)
  ) {
    Column {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.Analytics,
          contentDescription = "News",
          tint = QuantTheme.accentBlue,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "AI News Sentiment Refinement",
          color = QuantTheme.textPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
          modifier = Modifier
            .background(badgeBgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = sentiment.sentimentLabel.uppercase(),
            color = sentimentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      
      sentiment.headlines.forEach { headline ->
        Row(
          modifier = Modifier.padding(vertical = 2.dp),
          verticalAlignment = Alignment.Top
        ) {
          Text(
            text = "• ",
            color = QuantTheme.accentBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = headline,
            color = QuantTheme.textBody,
            fontSize = 12.sp,
            lineHeight = 15.sp
          )
        }
      }
      
      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = Color(0xFF1E3A5F), thickness = 0.5.dp)
      Spacer(modifier = Modifier.height(8.dp))
      
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = "Sentiment Score: ${String.format(Locale.US, "%+.2f", sentiment.sentimentScore)}",
            color = QuantTheme.textMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Refined Confidence: ${String.format(Locale.US, "%.1f%%", sentiment.refinedConfidence)} (${sentiment.refinedConfidenceGrade})",
            color = QuantTheme.accentGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
        
        Box(
          modifier = Modifier
            .background(Color(0xFF1A2A40), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "${String.format(Locale.US, "%.1f%%", originalConfidence)} ➔ ${String.format(Locale.US, "%.1f%%", sentiment.refinedConfidence)}",
            color = QuantTheme.textPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }
      
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = sentiment.reasoning,
        color = QuantTheme.textSubtle,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontStyle = FontStyle.Italic
      )
    }
  }
}

@Composable
fun TopGridOpportunityCard(
  opportunity: TradeOpportunity,
  index: Int,
  aiAnalysis: String?,
  grokAnalysis: String?,
  openaiAnalysis: String?,
  quantResult: com.example.ui.QuantAnalysisResult?,
  newsSentimentResult: NewsSentimentResult?,
  onAnalyze: () -> Unit,
  onTrade: () -> Unit
) {
  // Conforming to layout Item 3 style of High Density theme
  val isHighest = index == 1 || opportunity.probability > 85
  val cardBg = if (isHighest) QuantTheme.activeSurface else QuantTheme.surface
  val cardBorder = if (isHighest) QuantTheme.activeBorder else QuantTheme.border

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .graphicsLayer {
        cameraDistance = 12f * density
        rotationX = 2f
        rotationY = -2f
      }
      .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg)
  ) {
    Column(
      modifier = Modifier.padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(0.65f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .background(QuantTheme.accentBlue, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                index.toString(),
                color = QuantTheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              opportunity.title,
              color = if (isHighest) QuantTheme.textPrimary else QuantTheme.textBody,
              fontSize = 16.sp,
              fontWeight = if (isHighest) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            if (isHighest) "AI Confidence: High" else "Vol: ${opportunity.volume} • Liq: ${opportunity.liquidity}",
            color = QuantTheme.textMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Row(
          modifier = Modifier.weight(0.35f),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${opportunity.probability}%",
            color = QuantTheme.textBody,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 6.dp)
          )

          val deltaText = if (opportunity.delta >= 0) "+${opportunity.delta}%" else "${opportunity.delta}%"
          val deltaColor = if (opportunity.delta >= 0) QuantTheme.accentGreen else QuantTheme.accentRed
          Text(
            text = deltaText,
            color = deltaColor,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 6.dp)
          )

          Box(
            modifier = Modifier
              .background(
                if (isHighest) QuantTheme.textPrimary else QuantTheme.accentBlue,
                RoundedCornerShape(6.dp)
              )
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = if (isHighest) "CONFIDENCE: ${String.format(Locale.US, "%.1f", opportunity.confidenceScore)}" else "${String.format(Locale.US, "%.1f", opportunity.confidenceScore)} (${opportunity.confidenceGrade})",
              color = if (isHighest) Color(0xFF002F66) else QuantTheme.textPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      if (aiAnalysis != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.globalInsightsBg, RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Hub, contentDescription = "Math", tint = QuantTheme.accentGreen, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Math Consensus (Gemini)", color = QuantTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(aiAnalysis, color = QuantTheme.textBody, fontSize = 14.sp, lineHeight = 18.sp)
          }
        }
      }

      if (grokAnalysis != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.globalInsightsBg, RoundedCornerShape(6.dp))
            .border(1.dp, QuantTheme.accentBlue.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AutoAwesome, contentDescription = "Grok", tint = QuantTheme.accentBlue, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Grok-2 AI (xAI)", color = QuantTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(grokAnalysis, color = QuantTheme.textBody, fontSize = 14.sp, lineHeight = 18.sp)
          }
        }
      }

      if (openaiAnalysis != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.globalInsightsBg, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF10A37F).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Bolt, contentDescription = "OpenAI", tint = Color(0xFF10A37F), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("OpenAI GPT-4o-mini", color = QuantTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(openaiAnalysis, color = QuantTheme.textBody, fontSize = 14.sp, lineHeight = 18.sp)
          }
        }
      }

      if (quantResult != null) {
        Spacer(modifier = Modifier.height(8.dp))
        QuantAnalysisBreakoutView(result = quantResult)
      }

      if (newsSentimentResult != null) {
        NewsSentimentView(sentiment = newsSentimentResult, originalConfidence = opportunity.confidenceScore)
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (aiAnalysis == null) {
          Box(
            modifier = Modifier
              .weight(0.4f)
              .height(44.dp)
              .clip(RoundedCornerShape(8.dp))
              .border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp))
              .clickable { onAnalyze() },
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = QuantTheme.textPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "AI Research",
                color = QuantTheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(if (aiAnalysis == null) 0.6f else 1f)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0035FF)) // Polymarket Blue
            .clickable { onTrade() },
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              Icons.Default.OpenInNew,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "Open in Polymarket",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
fun StandardOpportunityCard(
  opportunity: TradeOpportunity,
  aiAnalysis: String?,
  grokAnalysis: String?,
  openaiAnalysis: String?,
  quantResult: com.example.ui.QuantAnalysisResult?,
  newsSentimentResult: NewsSentimentResult?,
  onAnalyze: () -> Unit,
  onTrade: () -> Unit,
  isWatched: Boolean = false,
  onToggleWatch: (() -> Unit)? = null
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .graphicsLayer {
        cameraDistance = 12f * density
        rotationX = 2f
        rotationY = -2f
      }
      .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp)),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = QuantTheme.surface)
  ) {
    Column(
      modifier = Modifier.padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(0.7f)) {
          Text(
            opportunity.title,
            color = QuantTheme.textBody,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            "Ends: ${formatDate(opportunity.endsAt)} • Volume: ${opportunity.volume}",
            color = QuantTheme.textMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Row(
          modifier = Modifier.weight(0.3f),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${opportunity.probability}%",
            color = QuantTheme.accentGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          if (onToggleWatch != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onToggleWatch, modifier = Modifier.size(28.dp)) {
              Icon(
                imageVector = if (isWatched) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = if (isWatched) "Remove from watchlist" else "Add to watchlist",
                tint = if (isWatched) QuantTheme.accentGreen else QuantTheme.textMuted,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        opportunity.description,
        color = QuantTheme.textSubtle,
        fontSize = 13.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      if (aiAnalysis != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.globalInsightsBg, RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Hub, contentDescription = "Math", tint = QuantTheme.accentGreen, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Math Consensus (Gemini)", color = QuantTheme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(aiAnalysis, color = QuantTheme.textBody, fontSize = 13.sp, lineHeight = 17.sp)
          }
        }
      }

      if (grokAnalysis != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.globalInsightsBg, RoundedCornerShape(6.dp))
            .border(1.dp, QuantTheme.accentBlue.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AutoAwesome, contentDescription = "Grok", tint = QuantTheme.accentBlue, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Grok-2 AI (xAI)", color = QuantTheme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(grokAnalysis, color = QuantTheme.textBody, fontSize = 13.sp, lineHeight = 17.sp)
          }
        }
      }

      if (openaiAnalysis != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(QuantTheme.globalInsightsBg, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF10A37F).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Bolt, contentDescription = "OpenAI", tint = Color(0xFF10A37F), modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("OpenAI GPT-4o-mini", color = QuantTheme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(openaiAnalysis, color = QuantTheme.textBody, fontSize = 13.sp, lineHeight = 17.sp)
          }
        }
      }

      if (quantResult != null) {
        Spacer(modifier = Modifier.height(8.dp))
        QuantAnalysisBreakoutView(result = quantResult)
      }

      if (newsSentimentResult != null) {
        NewsSentimentView(sentiment = newsSentimentResult, originalConfidence = opportunity.confidenceScore)
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (aiAnalysis == null) {
          Box(
            modifier = Modifier
              .weight(0.4f)
              .height(44.dp)
              .clip(RoundedCornerShape(8.dp))
              .border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp))
              .clickable { onAnalyze() },
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = QuantTheme.textPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "AI Research",
                color = QuantTheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(if (aiAnalysis == null) 0.6f else 1f)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0035FF)) // Polymarket Blue
            .clickable { onTrade() },
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              Icons.Default.OpenInNew,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "Open in Polymarket",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
fun PortfolioView(
  uiState: com.example.ui.TradeUiState,
  onOpenUrl: (String) -> Unit,
  onRefreshClob: () -> Unit,
  onRefreshPortfolio: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Portfolio Stats Header
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = QuantTheme.activeSurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, QuantTheme.activeBorder, RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("QUANT ACCOUNT PORTFOLIO", color = QuantTheme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(6.dp))
          Text("$42,185.30", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.TrendingUp, contentDescription = "Returns", tint = QuantTheme.accentGreen, modifier = Modifier.size(18.dp))
            Text("+$3,412.02 (+8.79%) this week", color = QuantTheme.accentGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Direct Sync Link
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(QuantTheme.surface, RoundedCornerShape(12.dp))
          .clickable { onOpenUrl("https://polymarket.com/portfolio") }
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Portfolio", tint = QuantTheme.textPrimary)
          Column {
            Text("Synchronize with Polymarket Portfolio", color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("https://polymarket.com/portfolio", color = QuantTheme.textMuted, fontSize = 13.sp)
          }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = QuantTheme.textPrimary)
      }
    }

    // Read-Only Wallet Portfolio
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("WALLET POSITIONS (READ-ONLY)", color = QuantTheme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        IconButton(
          onClick = { onRefreshPortfolio() },
          modifier = Modifier.size(24.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Sync Portfolio", tint = QuantTheme.textPrimary, modifier = Modifier.size(16.dp))
        }
      }
    }

    if (uiState.portfolioPositions.isEmpty()) {
        item {
            Text("No positions fetched. Tap refresh or add wallet in Settings.", color = QuantTheme.textMuted, fontSize = 13.sp)
        }
    }

    items(uiState.portfolioPositions) { pos ->
      PortfolioPositionRow(
        title = pos.title,
        position = pos.position,
        size = pos.size,
        entry = pos.entry,
        current = pos.current,
        profit = pos.profit
      )
    }

    // Real-time CLOB Opportunities (Replaces static history)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("LIVE AI-RANKED OPPORTUNITIES (CLOB)", color = QuantTheme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (uiState.isClobLoading) {
              CircularProgressIndicator(modifier = Modifier.size(14.dp), color = QuantTheme.accentGreen, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
          }
          IconButton(
            onClick = onRefreshClob,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Sync CLOB", tint = QuantTheme.accentGreen, modifier = Modifier.size(16.dp))
          }
        }
      }
    }

    if (uiState.clobOpportunities.isEmpty() && !uiState.isClobLoading) {
        item {
            Text("No active CLOB opportunities detected. Syncing live markets...", color = QuantTheme.textMuted, fontSize = 13.sp)
        }
    }

    items(uiState.clobOpportunities) { op ->
      StandardOpportunityCard(
        opportunity = op,
        aiAnalysis = uiState.aiAnalysis[op.id],
        grokAnalysis = uiState.grokAnalysis[op.id],
        openaiAnalysis = uiState.openaiAnalysis[op.id],
        quantResult = uiState.quantAnalysis[op.id],
        newsSentimentResult = uiState.newsSentiment[op.id],
        onAnalyze = { /* Already analyzed on fetch */ },
        onTrade = { onOpenUrl(op.url) }
      )
    }
  }
}

@Composable
fun PortfolioPositionRow(
  title: String,
  position: String,
  size: String,
  entry: String,
  current: String,
  profit: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(QuantTheme.surface, RoundedCornerShape(12.dp))
      .padding(12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(0.65f)) {
      Text(title, color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text("Size: $size • Entry: $entry • Position: $position", color = QuantTheme.textMuted, fontSize = 12.sp)
    }

    Column(modifier = Modifier.weight(0.35f), horizontalAlignment = Alignment.End) {
      Text(current, color = QuantTheme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      Text(profit, color = if (profit.startsWith("+") || profit == "$0.00") QuantTheme.accentGreen else QuantTheme.accentRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
fun NodesView(nodes: List<MathNode>) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Column {
        Text("DEEP MATH & QUANT CLUSTERS", color = QuantTheme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Monitoring real-time telemetry from 100+ non-generic research hubs in China, India, and high-density math districts.", color = QuantTheme.textMuted, fontSize = 14.sp)
      }
    }

    items(nodes) { node ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(QuantTheme.surface, RoundedCornerShape(12.dp))
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(QuantTheme.accentBlue),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Memory, contentDescription = "Node", tint = QuantTheme.textPrimary, modifier = Modifier.size(20.dp))
          }

          Column {
            Text(node.name, color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Region: ${node.region} • Active streams: ${node.activeStreams}", color = QuantTheme.textMuted, fontSize = 12.sp)
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text("${node.latency} ms", color = QuantTheme.accentGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          Text("${node.uptime}% Uptime", color = QuantTheme.textSubtle, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun ApiSettingsView(
  uiState: com.example.ui.TradeUiState,
  onSave: (String, Int, Int) -> Unit,
  onSaveCredentials: (String, String, String, String, Boolean, String) -> Unit,
  onSaveSystemSettings: (String, String, String, Boolean, Double, Double) -> Unit,
  onToggleWebSocket: () -> Unit,
  onSubscribeToken: (String) -> Unit,
  onClearLogs: () -> Unit
) {
  var selectedInterval by remember { mutableStateOf(uiState.searchInterval) }
  var rsiVal by remember { mutableStateOf(uiState.rsiThreshold.toFloat()) }
  var hftVal by remember { mutableStateOf(uiState.hftWeight.toFloat()) }

  var walletAddress by remember(uiState.polymarketWallet) { mutableStateOf(uiState.polymarketWallet) }
  var apiKey by remember(uiState.polymarketApiKey) { mutableStateOf(uiState.polymarketApiKey) }
  var apiSecret by remember(uiState.polymarketApiSecret) { mutableStateOf(uiState.polymarketApiSecret) }
  var apiPassphrase by remember(uiState.polymarketApiPassphrase) { mutableStateOf(uiState.polymarketApiPassphrase) }
  var isTestnet by remember(uiState.isTestnet) { mutableStateOf(uiState.isTestnet) }
  var accountUrl by remember(uiState.polymarketAccountUrl) { mutableStateOf(uiState.polymarketAccountUrl) }

  var googleApiKey by remember(uiState.googleApiKey) { mutableStateOf(uiState.googleApiKey) }
  var xaiApiKey by remember(uiState.xaiApiKey) { mutableStateOf(uiState.xaiApiKey) }
  var openaiApiKey by remember(uiState.openaiApiKey) { mutableStateOf(uiState.openaiApiKey) }
  var demoMode by remember(uiState.demoMode) { mutableStateOf(uiState.demoMode) }
  var maxPositionSize by remember(uiState.maxPositionSize) { mutableStateOf(uiState.maxPositionSize.toFloat()) }
  var riskTolerance by remember(uiState.riskTolerance) { mutableStateOf(uiState.riskTolerance.toFloat()) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text("QUANT WEIGHT RECALIBRATION", color = QuantTheme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Search Interval lookback (Polymarket CLOB)", color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          listOf("1m", "5m", "1h", "6h", "1d").forEach { interval ->
            val active = selectedInterval == interval
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) QuantTheme.accentBlue else QuantTheme.surface)
                .clickable { selectedInterval = interval }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(interval, color = if (active) QuantTheme.textPrimary else QuantTheme.textSubtle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("RSI Lookback Window (periods)", color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("${rsiVal.toInt()}", color = QuantTheme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Slider(
          value = rsiVal,
          onValueChange = { rsiVal = it },
          valueRange = 5f..30f,
          colors = SliderDefaults.colors(
            thumbColor = QuantTheme.textPrimary,
            activeTrackColor = QuantTheme.accentBlue,
            inactiveTrackColor = QuantTheme.surface
          )
        )
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("HFT Signal Weight multiplier (%)", color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("${hftVal.toInt()}%", color = QuantTheme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Slider(
          value = hftVal,
          onValueChange = { hftVal = it },
          valueRange = 10f..100f,
          colors = SliderDefaults.colors(
            thumbColor = QuantTheme.textPrimary,
            activeTrackColor = QuantTheme.accentBlue,
            inactiveTrackColor = QuantTheme.surface
          )
        )
      }
    }

    item {
      Button(
        onClick = { onSave(selectedInterval, rsiVal.toInt(), hftVal.toInt()) },
        colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentBlue),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Recalibrate Analytical Model", fontSize = 14.sp, fontWeight = FontWeight.Bold)
      }
    }

    item {
      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = QuantTheme.border, thickness = 1.dp)
      Spacer(modifier = Modifier.height(12.dp))
      Text("SYSTEM SETTINGS", color = QuantTheme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Demo Mode", color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Use local fallback data", color = QuantTheme.textMuted, fontSize = 13.sp)
        }
        Switch(
          checked = demoMode,
          onCheckedChange = { demoMode = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = QuantTheme.textPrimary,
            checkedTrackColor = QuantTheme.accentBlue,
            uncheckedThumbColor = QuantTheme.textMuted,
            uncheckedTrackColor = QuantTheme.surface
          )
        )
      }
    }

    item {
      OutlinedTextField(
        value = googleApiKey,
        onValueChange = { googleApiKey = it },
        label = { Text("Google AI Studio API Key", color = QuantTheme.textMuted) },
        textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = QuantTheme.activeBorder,
          unfocusedBorderColor = QuantTheme.border,
          focusedLabelColor = QuantTheme.textPrimary,
          unfocusedLabelColor = QuantTheme.textMuted
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      OutlinedTextField(
        value = xaiApiKey,
        onValueChange = { xaiApiKey = it },
        label = { Text("xAI Grok API Key", color = QuantTheme.textMuted) },
        textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = QuantTheme.activeBorder,
          unfocusedBorderColor = QuantTheme.border,
          focusedLabelColor = QuantTheme.textPrimary,
          unfocusedLabelColor = QuantTheme.textMuted
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      OutlinedTextField(
        value = openaiApiKey,
        onValueChange = { openaiApiKey = it },
        label = { Text("OpenAI API Key", color = QuantTheme.textMuted) },
        textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = QuantTheme.activeBorder,
          unfocusedBorderColor = QuantTheme.border,
          focusedLabelColor = QuantTheme.textPrimary,
          unfocusedLabelColor = QuantTheme.textMuted
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }
    
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Max Position Size (USDC)", color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("${maxPositionSize.toInt()}", color = QuantTheme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Slider(
          value = maxPositionSize,
          onValueChange = { maxPositionSize = it },
          valueRange = 10f..1000f,
          colors = SliderDefaults.colors(
            thumbColor = QuantTheme.textPrimary,
            activeTrackColor = QuantTheme.accentBlue,
            inactiveTrackColor = QuantTheme.surface
          )
        )
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Risk Tolerance", color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(String.format("%.2f", riskTolerance), color = QuantTheme.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Slider(
          value = riskTolerance,
          onValueChange = { riskTolerance = it },
          valueRange = 0.05f..1.0f,
          colors = SliderDefaults.colors(
            thumbColor = QuantTheme.textPrimary,
            activeTrackColor = QuantTheme.accentBlue,
            inactiveTrackColor = QuantTheme.surface
          )
        )
      }
    }

    item {
      Button(
        onClick = { onSaveSystemSettings(googleApiKey, xaiApiKey, openaiApiKey, demoMode, maxPositionSize.toDouble(), riskTolerance.toDouble()) },
        colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentBlue),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Save System Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Secure credentials header
    item {
      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = QuantTheme.border, thickness = 1.dp)
      Spacer(modifier = Modifier.height(12.dp))
      Text("SECURE POLYMARKET CREDENTIALS (EIP-712)", color = QuantTheme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

    item {
      OutlinedTextField(
        value = walletAddress,
        onValueChange = { walletAddress = it },
        label = { Text("Signer Wallet Address (Ethereum)", color = QuantTheme.textMuted) },
        placeholder = { Text("0x...", color = QuantTheme.textSubtle) },
        textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = QuantTheme.activeBorder,
          unfocusedBorderColor = QuantTheme.border,
          focusedLabelColor = QuantTheme.textPrimary,
          unfocusedLabelColor = QuantTheme.textMuted
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      OutlinedTextField(
        value = apiKey,
        onValueChange = { apiKey = it },
        label = { Text("CLOB API Key", color = QuantTheme.textMuted) },
        textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = QuantTheme.activeBorder,
          unfocusedBorderColor = QuantTheme.border,
          focusedLabelColor = QuantTheme.textPrimary,
          unfocusedLabelColor = QuantTheme.textMuted
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      OutlinedTextField(
        value = apiSecret,
        onValueChange = { apiSecret = it },
        label = { Text("CLOB API Secret", color = QuantTheme.textMuted) },
        textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = QuantTheme.activeBorder,
          unfocusedBorderColor = QuantTheme.border,
          focusedLabelColor = QuantTheme.textPrimary,
          unfocusedLabelColor = QuantTheme.textMuted
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      OutlinedTextField(
        value = apiPassphrase,
        onValueChange = { apiPassphrase = it },
        label = { Text("CLOB API Passphrase", color = QuantTheme.textMuted) },
        textStyle = androidx.compose.ui.text.TextStyle(color = QuantTheme.textBody),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = QuantTheme.activeBorder,
          unfocusedBorderColor = QuantTheme.border,
          focusedLabelColor = QuantTheme.textPrimary,
          unfocusedLabelColor = QuantTheme.textMuted
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Sandbox Mode", color = QuantTheme.textBody, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Perform offline signature simulations when active.", color = QuantTheme.textMuted, fontSize = 13.sp)
        }
        Switch(
          checked = isTestnet,
          onCheckedChange = { isTestnet = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = QuantTheme.textPrimary,
            checkedTrackColor = QuantTheme.accentBlue,
            uncheckedThumbColor = QuantTheme.textMuted,
            uncheckedTrackColor = QuantTheme.surface
          )
        )
      }
    }

    item {
      Button(
        onClick = { onSaveCredentials(walletAddress, apiKey, apiSecret, apiPassphrase, isTestnet, accountUrl) },
        colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentGreen),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Save & Sync Secure Credentials", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF002F66))
      }
    }

    item {
      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = QuantTheme.border, thickness = 1.dp)
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "REAL-TIME WEBSOCKET STREAM",
            color = QuantTheme.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "ws://127.0.0.1:8080/ws",
            color = QuantTheme.accentBlue,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
        
        Box(
          modifier = Modifier
            .background(
              if (uiState.isWebSocketConnected) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFF37474F).copy(alpha = 0.2f),
              RoundedCornerShape(6.dp)
            )
            .border(
              1.dp,
              if (uiState.isWebSocketConnected) Color(0xFF00C853) else Color(0xFFB0BEC5),
              RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(
                  if (uiState.isWebSocketConnected) Color(0xFF00C853) else Color(0xFFB0BEC5),
                  shape = CircleShape
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (uiState.isWebSocketConnected) "CONNECTED" else "OFFLINE",
              color = if (uiState.isWebSocketConnected) Color(0xFF00C853) else Color(0xFFB0BEC5),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    item {
      Text(
        text = "Subscribe to specific token IDs below to stream mathematical signals, overreaction models, and sentiment-adjusted confidence metrics over the connection in real time.",
        color = QuantTheme.textMuted,
        fontSize = 13.sp,
        lineHeight = 18.sp
      )
    }

    item {
      Button(
        onClick = onToggleWebSocket,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (uiState.isWebSocketConnected) Color(0xFFB71C1C) else QuantTheme.accentBlue
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (uiState.isWebSocketConnected) Icons.Default.Close else Icons.Default.FlashOn,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (uiState.isWebSocketConnected) "DISCONNECT WEBSOCKET ENGINE" else "ESTABLISH WEBSOCKET FEED",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(QuantTheme.surface, RoundedCornerShape(12.dp))
          .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Text(
          text = "CONTRACT TOKENS SUBSCRIPTIONS",
          color = QuantTheme.textPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        val allContracts = uiState.topOpportunities + uiState.opportunities
        if (allContracts.isEmpty()) {
          Text("No active contracts detected to subscribe.", color = QuantTheme.textSubtle, fontSize = 12.sp)
        } else {
          allContracts.forEach { opp ->
            val isSubscribed = uiState.webSocketSubscribedTokens.contains(opp.id)
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSubscribed) Color(0xFF0F1B2A) else Color.Transparent)
                .border(
                  1.dp,
                  if (isSubscribed) QuantTheme.accentBlue else QuantTheme.border.copy(alpha = 0.5f),
                  RoundedCornerShape(8.dp)
                )
                .clickable { onSubscribeToken(opp.id) }
                .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = opp.title,
                    color = if (isSubscribed) QuantTheme.textPrimary else QuantTheme.textBody,
                    fontSize = 13.sp,
                    fontWeight = if (isSubscribed) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "ID: ${opp.id} | Prob: ${opp.probability}%",
                    color = QuantTheme.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                  )
                }
                
                Box(
                  modifier = Modifier
                    .background(
                      if (isSubscribed) QuantTheme.accentBlue.copy(alpha = 0.2f) else Color.Transparent,
                      CircleShape
                    )
                    .border(
                      1.dp,
                      if (isSubscribed) QuantTheme.accentBlue else QuantTheme.border,
                      CircleShape
                    )
                    .size(24.dp),
                  contentAlignment = Alignment.Center
                ) {
                  if (isSubscribed) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Subscribed",
                      tint = QuantTheme.accentBlue,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF070C14), RoundedCornerShape(12.dp))
          .border(1.dp, Color(0xFF1E2D44), RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Analytics,
              contentDescription = null,
              tint = QuantTheme.accentGreen,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "WS CONSOLE ROUTER",
              color = Color(0xFF00C853),
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
          
          Text(
            text = "CLEAR",
            color = QuantTheme.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clickable { onClearLogs() }
              .padding(4.dp)
          )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFF03060A), RoundedCornerShape(8.dp))
            .border(0.5.dp, Color(0xFF131D2D), RoundedCornerShape(8.dp))
            .padding(8.dp)
        ) {
          val state = rememberLazyListState()
          
          LaunchedEffect(uiState.webSocketLogs.size) {
            if (uiState.webSocketLogs.isNotEmpty()) {
              state.animateScrollToItem(uiState.webSocketLogs.size - 1)
            }
          }
          
          LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            if (uiState.webSocketLogs.isEmpty()) {
              item {
                Text(
                  text = "Console idle. Awaiting WebSocket engine startup...",
                  color = QuantTheme.textSubtle,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace
                )
              }
            } else {
              items(uiState.webSocketLogs.size) { index ->
                val logLine = uiState.webSocketLogs[index]
                val textColor = when {
                  logLine.contains("[WS RECV]") -> Color(0xFF00E676) // Bright Green
                  logLine.contains("[WS SENT]") -> Color(0xFF29B6F6) // Sky Blue
                  logLine.contains("[WS CONNECT]") -> Color(0xFFFFA726) // Orange
                  logLine.contains("[WS DISCONNECT]") -> Color(0xFFEF5350) // Soft Red
                  else -> Color(0xFFCFD8DC) // Soft Grey
                }
                
                Text(
                  text = logLine,
                  color = textColor,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  lineHeight = 14.sp
                )
              }
            }
          }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Updates: ${uiState.webSocketUpdatesCount} frame packets",
          color = QuantTheme.textMuted,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

@Composable
fun TradeExecutionSummaryCard(
  opportunity: TradeOpportunity
) {
  val yesPrice = opportunity.probability / 100.0
  val noPrice = (100 - opportunity.probability) / 100.0
  val predicted = if (opportunity.probability >= 50) "YES" else "NO"
  val predictedProb = if (opportunity.probability >= 50) opportunity.probability else (100 - opportunity.probability)
  
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131720)),
    border = BorderStroke(1.dp, QuantTheme.border),
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "CURRENT MARKET PRICES",
          color = QuantTheme.textMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          fontFamily = FontFamily.Monospace
        )
        Box(
          modifier = Modifier
            .background(
              if (predicted == "YES") QuantTheme.accentGreen.copy(alpha = 0.15f) else QuantTheme.accentRed.copy(alpha = 0.15f),
              RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "AI PREDICTED: $predicted ($predictedProb%)",
            color = if (predicted == "YES") QuantTheme.accentGreen else QuantTheme.accentRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
      
      Spacer(modifier = Modifier.height(14.dp))
      
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // YES choice card
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(QuantTheme.surface)
            .border(
              width = 1.dp,
              color = QuantTheme.border,
              shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("YES", color = QuantTheme.accentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(String.format(Locale.US, "$%.2f USDC", yesPrice), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
          }
        }
        
        // NO choice card
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(QuantTheme.surface)
            .border(
              width = 1.dp,
              color = QuantTheme.border,
              shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("NO", color = QuantTheme.accentRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(String.format(Locale.US, "$%.2f USDC", noPrice), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
          }
        }
      }
    }
  }
}

@Composable
fun MovoView(
  opportunity: TradeOpportunity,
  uiState: com.example.ui.TradeUiState,
  onDismiss: () -> Unit,
  onOpenUrl: (String) -> Unit,
  onLoadResearch: (TradeOpportunity) -> Unit = {},
  onSelectRange: (TradeOpportunity, String) -> Unit = { _, _ -> },
  onCorrelate: (TradeOpportunity, TradeOpportunity) -> Unit = { _, _ -> }
) {
  var showAdvancedAnalytics by remember { mutableStateOf(false) }

  // Pull real CLOB market data (prices-history + order book) on open.
  LaunchedEffect(opportunity.id) { onLoadResearch(opportunity) }

  // Generate simulated chart prices if quant prices are empty
  val quantResult = uiState.quantAnalysis[opportunity.id]
  val rawPrices = quantResult?.prices ?: emptyList()
  val prices = remember(opportunity.id, rawPrices) {
    if (rawPrices.isNotEmpty()) {
      rawPrices
    } else {
      val prob = opportunity.probability.toDouble() / 100.0
      val list = mutableListOf<Double>()
      var current = prob - 0.07
      for (i in 0 until 14) {
        current += (Math.random() - 0.45) * 0.015
        current = current.coerceIn(0.01, 0.99)
        list.add(current)
      }
      list.add(prob)
      list
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.98f)
        .fillMaxHeight(0.95f)
        .clip(RoundedCornerShape(20.dp))
        .background(Color(0xFF0E1116))
        .border(1.5.dp, QuantTheme.activeBorder, RoundedCornerShape(20.dp))
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // 1. Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              Icons.AutoMirrored.Filled.TrendingUp,
              contentDescription = null,
              tint = QuantTheme.accentGreen,
              modifier = Modifier.size(24.dp)
            )
            Text(
              "MARKET RESEARCH PREVIEW",
              color = QuantTheme.textPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 1.sp
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = QuantTheme.textMuted, modifier = Modifier.size(24.dp))
          }
        }

        // 2. Title & Meta Info Card
        Card(
          colors = CardDefaults.cardColors(containerColor = QuantTheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .background(QuantTheme.accentBlue, RoundedCornerShape(6.dp))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = opportunity.category.uppercase(),
                  color = QuantTheme.textPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }
              Text(
                text = "HFT: ${opportunity.hftSignal}",
                color = if (opportunity.hftSignal == "DETECTED") QuantTheme.accentGreen else QuantTheme.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              opportunity.title,
              color = Color.White,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              opportunity.description,
              color = QuantTheme.textSubtle,
              fontSize = 14.sp,
              lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Ends: ${formatDate(opportunity.endsAt)}", color = QuantTheme.textMuted, fontSize = 12.sp)
              Text("Volume: ${opportunity.volume} • Liquidity: ${opportunity.liquidity}", color = QuantTheme.textMuted, fontSize = 12.sp)
            }
          }
        }

        // 2.5 Clean, Simplified Trade Execution Summary Card
        TradeExecutionSummaryCard(
          opportunity = opportunity
        )

        // 2.6 LIVE MARKET STRUCTURE — real CLOB prices-history + order book depth
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131720), RoundedCornerShape(12.dp))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "PROBABILITY HISTORY — LIVE CLOB DATA",
              color = QuantTheme.textMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontFamily = FontFamily.Monospace
            )
            if (opportunity.id in uiState.researchLoadingIds) {
              CircularProgressIndicator(
                color = QuantTheme.accentGreen,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp)
              )
            }
          }

          MarketHistoryChart(
            points = uiState.researchHistory[opportunity.id] ?: emptyList()
          )

          // Range selector chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf("1h", "6h", "1d", "1w", "1m", "max").forEach { range ->
              val selected = uiState.researchRange == range
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (selected) QuantTheme.accentBlue else QuantTheme.navButtonBg)
                  .border(
                    1.dp,
                    if (selected) QuantTheme.activeBorder else QuantTheme.border,
                    RoundedCornerShape(8.dp)
                  )
                  .clickable { onSelectRange(opportunity, range) }
                  .padding(horizontal = 10.dp, vertical = 5.dp)
              ) {
                Text(
                  range.uppercase(),
                  color = if (selected) QuantTheme.textPrimary else QuantTheme.textSubtle,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }

          val book = uiState.researchBooks[opportunity.id]
          if (book != null && (book.bids.isNotEmpty() || book.asks.isNotEmpty())) {
            HorizontalDivider(color = QuantTheme.border, thickness = 0.5.dp)
            Text(
              "ORDER BOOK DEPTH — CUMULATIVE",
              color = QuantTheme.textMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontFamily = FontFamily.Monospace
            )
            DepthChart(book = book)
          }
        }

        // 2.7 WHALE FEED — large fills from the public Data API
        val whales = uiState.researchWhales[opportunity.id].orEmpty()
        if (whales.isNotEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF131720), RoundedCornerShape(12.dp))
              .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              "🐋 WHALE FEED — FILLS ≥ $1K NOTIONAL",
              color = QuantTheme.textMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontFamily = FontFamily.Monospace
            )
            whales.take(6).forEach { trade ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "${trade.side ?: "?"} ${trade.outcome ?: ""}",
                  color = if (trade.side == "BUY") QuantTheme.accentGreen else QuantTheme.accentRed,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
                Text(
                  String.format(Locale.US, "$%,.0f @ %.1f¢", trade.notionalUsd, (trade.price ?: 0.0) * 100),
                  color = QuantTheme.textBody,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace
                )
                Text(
                  trade.traderLabel.take(14),
                  color = QuantTheme.textMuted,
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        // 2.8 EVENT CORRELATION MATRIX — pair this market against another
        val correlationCandidates = (uiState.topOpportunities + uiState.opportunities)
          .filter { it.id != opportunity.id }
          .distinctBy { it.id }
          .take(4)
        if (correlationCandidates.isNotEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF131720), RoundedCornerShape(12.dp))
              .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "EVENT CORRELATION MATRIX",
                color = QuantTheme.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
              )
              if (uiState.isCorrelating) {
                CircularProgressIndicator(
                  color = QuantTheme.accentGreen,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
            Text(
              "Pearson on 1w CLOB histories + AI hedge read. Pair with:",
              color = QuantTheme.textSubtle,
              fontSize = 11.sp
            )
            correlationCandidates.forEach { other ->
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(QuantTheme.navButtonBg)
                  .border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp))
                  .clickable(enabled = !uiState.isCorrelating) { onCorrelate(opportunity, other) }
                  .padding(horizontal = 10.dp, vertical = 8.dp)
              ) {
                Text(
                  "⇄ ${other.title}",
                  color = QuantTheme.textSubtle,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            if (uiState.correlationText != null) {
              HorizontalDivider(color = QuantTheme.border, thickness = 0.5.dp)
              uiState.correlationLabel?.let {
                Text(
                  "vs ${it.uppercase()}",
                  color = QuantTheme.accentGreen,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }
              Text(
                uiState.correlationText ?: "",
                color = QuantTheme.textBody,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }

        // Collapsible Advanced Analytics Section Toggle
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131720), RoundedCornerShape(12.dp))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
            .clickable { showAdvancedAnalytics = !showAdvancedAnalytics }
            .padding(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = if (showAdvancedAnalytics) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = QuantTheme.textPrimary,
                modifier = Modifier.size(16.dp)
              )
              Text(
                "ADVANCED TECHNICAL ANALYTICS",
                color = QuantTheme.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
              )
            }
            Text(
              text = if (showAdvancedAnalytics) "TAP TO COLLAPSE" else "TAP TO EXPAND",
              color = QuantTheme.textMuted,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        if (showAdvancedAnalytics) {
          // 3. Clear YES/NO Percentage Gauge
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF131720), RoundedCornerShape(12.dp))
              .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              "POLYNOMIAL MARKET PROBABILITIES",
              color = QuantTheme.textMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontFamily = FontFamily.Monospace
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // YES percentage
              Column {
                Text("YES PROBABILITY", color = QuantTheme.accentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${opportunity.probability}%", color = QuantTheme.accentGreen, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
              }

              // NO percentage
              Column(horizontalAlignment = Alignment.End) {
                Text("NO PROBABILITY", color = QuantTheme.accentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${100 - opportunity.probability}%", color = QuantTheme.accentRed, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
              }
            }

            // Gauge split line
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
            ) {
              Box(
                modifier = Modifier
                  .weight(opportunity.probability.toFloat().coerceAtLeast(1f))
                  .fillMaxHeight()
                  .background(QuantTheme.accentGreen)
              )
              Box(
                modifier = Modifier
                  .weight((100 - opportunity.probability).toFloat().coerceAtLeast(1f))
                  .fillMaxHeight()
                  .background(QuantTheme.accentRed)
              )
            }
          }

          // 4. Immersive Canvas Probability Trend Graph
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF131720), RoundedCornerShape(12.dp))
              .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "24H PROBABILITY PROJECTION & BOLLINGER BANDS",
                color = QuantTheme.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
              )
              val trendUp = prices.lastOrNull() ?: 0.0 >= (prices.firstOrNull() ?: 0.0)
              Text(
                text = if (trendUp) "▲ PROJECTION UP" else "▼ PROJECTION DOWN",
                color = if (trendUp) QuantTheme.accentGreen else QuantTheme.accentRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
            ) {
              Canvas(
                modifier = Modifier.fillMaxSize()
              ) {
                val width = size.width
                val height = size.height

                // Horizontal grid dashed lines
                val gridLinesCount = 3
                for (i in 1..gridLinesCount) {
                  val y = (height / (gridLinesCount + 1)) * i
                  drawLine(
                    color = QuantTheme.border.copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                  )
                }

                if (prices.isNotEmpty()) {
                  val path = Path()
                  val fillPath = Path()
                  
                  prices.forEachIndexed { index, price ->
                    val x = (width / (prices.size - 1)) * index
                    // map 0.0-1.0 price to canvas height
                    val y = height - (price.toFloat() * height)

                    if (index == 0) {
                      path.moveTo(x, y)
                      fillPath.moveTo(x, height)
                      fillPath.lineTo(x, y)
                    } else {
                      path.lineTo(x, y)
                      fillPath.lineTo(x, y)
                    }

                    if (index == prices.size - 1) {
                      fillPath.lineTo(x, height)
                      fillPath.close()
                    }
                  }

                  // Draw gradient under curve
                  val trendColor = if (prices.last() >= prices.first()) QuantTheme.accentGreen else QuantTheme.accentRed
                  drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                      colors = listOf(trendColor.copy(alpha = 0.2f), Color.Transparent),
                      startY = 0f,
                      endY = height
                    )
                  )

                  // Draw trendline path
                  drawPath(
                    path = path,
                    color = trendColor,
                    style = Stroke(width = 3.dp.toPx())
                  )

                  // Draw Bollinger Bands (Dotted guides)
                  val bandUpperPath = Path()
                  val bandLowerPath = Path()
                  prices.forEachIndexed { index, price ->
                    val x = (width / (prices.size - 1)) * index
                    val upperPrice = (price + 0.12).coerceAtMost(1.0)
                    val lowerPrice = (price - 0.12).coerceAtLeast(0.0)
                    val yUpper = height - (upperPrice.toFloat() * height)
                    val yLower = height - (lowerPrice.toFloat() * height)

                    if (index == 0) {
                      bandUpperPath.moveTo(x, yUpper)
                      bandLowerPath.moveTo(x, yLower)
                    } else {
                      bandUpperPath.lineTo(x, yUpper)
                      bandLowerPath.lineTo(x, yLower)
                    }
                  }
                  
                  drawPath(
                    path = bandUpperPath,
                    color = QuantTheme.textMuted.copy(alpha = 0.4f),
                    style = Stroke(width = 1.5.dp.toPx())
                  )
                  drawPath(
                    path = bandLowerPath,
                    color = QuantTheme.textMuted.copy(alpha = 0.4f),
                    style = Stroke(width = 1.5.dp.toPx())
                  )
                }
              }
            }
            
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("0.0 USDC (0%)", color = QuantTheme.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
              Text("BB Mid-Channel Line", color = QuantTheme.textMuted.copy(alpha = 0.7f), fontSize = 11.sp)
              Text("1.0 USDC (100%)", color = QuantTheme.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
          }
        }

        // 5. QUICK EXTERNAL TRADE LINK BUTTON (The real-link trade requirement!)
        Button(
          onClick = { onOpenUrl(opportunity.url) },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0035FF)), // Polymarket brand blue
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            Icons.Default.OpenInNew,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            "OPEN IN POLYMARKET",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = Color.White,
            letterSpacing = 0.5.sp
          )
        }

      }
    }
  }
}

@Composable
fun ToastNotificationOverlay(
  notification: ToastNotification,
  onDismiss: () -> Unit,
  onAction: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth(0.9f)
      .border(1.dp, QuantTheme.activeBorder, RoundedCornerShape(12.dp))
      .padding(4.dp),
    shape = RoundedCornerShape(12.dp),
    color = QuantTheme.surface,
    tonalElevation = 6.dp
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(0.75f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(QuantTheme.accentGreen, CircleShape)
        )
        Column {
          Text(
            notification.title,
            color = QuantTheme.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            notification.message,
            color = QuantTheme.textBody,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(
        modifier = Modifier.weight(0.25f),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "TRADE ➔",
          color = QuantTheme.accentGreen,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .clickable { onAction() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
          Icons.Default.Close,
          contentDescription = "Dismiss",
          tint = QuantTheme.textMuted,
          modifier = Modifier
            .size(16.dp)
            .clickable { onDismiss() }
        )
      }
    }
  }
}

fun formatDate(isoString: String): String {
  return try {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = parser.parse(isoString) ?: return isoString
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    formatter.format(date)
  } catch (e: Exception) {
    isoString
  }
}

@Composable
fun QuantAnalysisBreakoutView(
  result: com.example.ui.QuantAnalysisResult
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(QuantTheme.background, RoundedCornerShape(8.dp))
      .border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp))
      .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.Analytics,
          contentDescription = null,
          tint = QuantTheme.accentGreen,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          "LOCAL QUANT INDICATOR BREAKDOWN",
          color = QuantTheme.textPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
      Box(
        modifier = Modifier
          .background(
            when (result.signal) {
              "BUY" -> QuantTheme.accentGreen.copy(alpha = 0.2f)
              "SELL" -> QuantTheme.accentRed.copy(alpha = 0.2f)
              else -> QuantTheme.textMuted.copy(alpha = 0.2f)
            },
            RoundedCornerShape(6.dp)
          )
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "SIGNAL: ${result.signal}",
          color = when (result.signal) {
            "BUY" -> QuantTheme.accentGreen
            "SELL" -> QuantTheme.accentRed
            else -> QuantTheme.textMuted
          },
          fontSize = 13.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Local Sparkline chart
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(60.dp)
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        if (result.prices.size > 1) {
          val path = Path()
          result.prices.forEachIndexed { idx, p ->
            val x = (size.width / (result.prices.size - 1)) * idx
            val y = size.height - (p.toFloat() * size.height)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          drawPath(
            path = path,
            color = if (result.signal == "BUY") QuantTheme.accentGreen else QuantTheme.accentRed,
            style = Stroke(width = 2.dp.toPx())
          )
        }
      }
    }

    HorizontalDivider(color = QuantTheme.border, thickness = 0.5.dp)

    // Row 1: RSI and Bollinger Bands
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("RSI LOOKBACK (${result.rsi.toInt()})", color = QuantTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = when {
            result.rsi > 70.0 -> "${String.format(Locale.US, "%.1f", result.rsi)} (Overbought)"
            result.rsi < 30.0 -> "${String.format(Locale.US, "%.1f", result.rsi)} (Oversold)"
            else -> "${String.format(Locale.US, "%.1f", result.rsi)} (Stable)"
          },
          color = when {
            result.rsi > 70.0 -> QuantTheme.accentRed
            result.rsi < 30.0 -> QuantTheme.accentGreen
            else -> QuantTheme.textBody
          },
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Column(modifier = Modifier.weight(1.5f)) {
        Text("BOLLINGER BANDS (20, 2.0)", color = QuantTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = String.format(Locale.US, "$%.2f ➔ [$%.2f - $%.2f]", result.currentPrice, result.bbLower, result.bbUpper),
          color = QuantTheme.textBody,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Row 2: Rate of Change (ROC) and SMA Cross
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("MOMENTUM (ROC 14)", color = QuantTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        val rocColor = if (result.roc >= 0) QuantTheme.accentGreen else QuantTheme.accentRed
        Text(
          text = String.format(Locale.US, "%+.2f%%", result.roc),
          color = rocColor,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Column(modifier = Modifier.weight(1.5f)) {
        Text("SMA CROSSOVER (5d / 10d)", color = QuantTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        val smaTrend = if (result.smaShort > result.smaLong) "BULLISH (Short > Long)" else "BEARISH (Short < Long)"
        val smaColor = if (result.smaShort > result.smaLong) QuantTheme.accentGreen else QuantTheme.accentRed
        Text(
          text = smaTrend,
          color = smaColor,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Row 3: Volume Spike and Signal Reason
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("VOLUME SPIKE (2.0x)", color = QuantTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (result.volumeSpike) "SPIKE DETECTED" else "NORMAL VOLUME",
          color = if (result.volumeSpike) QuantTheme.accentGreen else QuantTheme.textBody,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Column(modifier = Modifier.weight(1.5f)) {
        Text("QUANT DETECTOR REASONING", color = QuantTheme.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = result.reason,
          color = QuantTheme.textBody,
          fontSize = 13.sp,
          maxLines = 2,
          lineHeight = 16.sp
        )
      }
    }
  }
}

@Composable
fun SimpleStepItem(number: String, title: String, desc: String, emoji: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(QuantTheme.surface, RoundedCornerShape(12.dp))
      .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .background(QuantTheme.accentGreen.copy(alpha = 0.15f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = number,
        color = QuantTheme.accentGreen,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        fontFamily = FontFamily.Monospace
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(emoji, fontSize = 16.sp)
        Text(title, fontWeight = FontWeight.Bold, color = QuantTheme.textPrimary, fontSize = 14.sp)
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(desc, color = QuantTheme.textMuted, fontSize = 12.sp, lineHeight = 16.sp)
    }
  }
}

@Composable
fun AppPromoBanner(onOpenInstallGuide: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(QuantTheme.surface)
      .border(1.dp, QuantTheme.border)
      .clickable { onOpenInstallGuide() }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .background(QuantTheme.accentGreen.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text("📲", fontSize = 18.sp)
      }
      Column {
        Text(
          "Install PolyTrader on Android!",
          color = QuantTheme.textPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Text(
          "Tap to see QR Code & 4-Step Picture Guide",
          color = QuantTheme.accentGreen,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    Button(
      onClick = onOpenInstallGuide,
      colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentGreen),
      shape = RoundedCornerShape(8.dp),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
      modifier = Modifier.height(32.dp)
    ) {
      Text(
        "GET APP",
        color = Color.Black,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

@Composable
fun InstallCompanionAppDialog(
  onDismiss: () -> Unit,
  apkDownloadUrl: String
) {
  val context = LocalContext.current

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(16.dp),
      color = QuantTheme.background,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(2.dp, QuantTheme.accentGreen)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Row with Close Icon
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("📱", fontSize = 24.sp)
            Text(
              "Android Sideload",
              color = QuantTheme.textPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(32.dp)
              .background(QuantTheme.surface, CircleShape)
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "Close",
              tint = QuantTheme.textPrimary,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Text(
          "Installing is super simple! Just scan the QR code with your phone or download the file directly.",
          color = QuantTheme.textBody,
          fontSize = 13.sp,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          lineHeight = 18.sp
        )

        // The QR code (BIG and gorgeous)
        Box(
          modifier = Modifier
            .size(200.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, QuantTheme.accentGreen, RoundedCornerShape(16.dp))
            .padding(12.dp),
          contentAlignment = Alignment.Center
        ) {
          val bitMatrix = remember(apkDownloadUrl) {
            try {
              com.google.zxing.MultiFormatWriter().encode(
                apkDownloadUrl,
                com.google.zxing.BarcodeFormat.QR_CODE,
                300,
                300
              )
            } catch (e: Throwable) {
              null
            }
          }

          if (bitMatrix != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val matrixWidth = bitMatrix.width
              val matrixHeight = bitMatrix.height
              val pixelWidth = size.width / matrixWidth
              val pixelHeight = size.height / matrixHeight

              drawRect(color = Color.White)

              for (x in 0 until matrixWidth) {
                for (y in 0 until matrixHeight) {
                  if (bitMatrix.get(x, y)) {
                    drawRect(
                      color = Color.Black,
                      topLeft = androidx.compose.ui.geometry.Offset(x * pixelWidth, y * pixelHeight),
                      size = androidx.compose.ui.geometry.Size(pixelWidth + 0.1f, pixelHeight + 0.1f)
                    )
                  }
                }
              }
            }
          } else {
            Text("QR Generation Error", color = Color.Red, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
          }
        }

        Text(
          "Scan the code above with your phone's camera!",
          color = QuantTheme.accentGreen,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        HorizontalDivider(color = QuantTheme.border)

        // Download Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              val clipManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
              val clipData = android.content.ClipData.newPlainText("PolyTrader APK Link", apkDownloadUrl)
              clipManager.setPrimaryClip(clipData)
              Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.surface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = QuantTheme.textPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy Link", color = QuantTheme.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
          }

          Button(
            onClick = {
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkDownloadUrl))
                context.startActivity(intent)
              } catch (e: Exception) {
                Toast.makeText(context, "Error: Unable to open link.", Toast.LENGTH_SHORT).show()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentBlue),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1.2f)
          ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Download APK", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }
        }

        HorizontalDivider(color = QuantTheme.border)

        // Super Simple Steps for anyone (the 4-step picture style guide)
        Text(
          "🏁 4-Step Quick Install Guide",
          color = QuantTheme.textPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.align(Alignment.Start)
        )

        SimpleStepItem(
          number = "1",
          title = "Scan & Open",
          desc = "Open your Android phone's regular Camera app and scan the QR code above.",
          emoji = "📸"
        )

        SimpleStepItem(
          number = "2",
          title = "Download Anyway",
          desc = "Tap \"Download APK\". If your phone asks \"File might be harmful?\", tap \"Download anyway\". It is 100% safe!",
          emoji = "⬇️"
        )

        SimpleStepItem(
          number = "3",
          title = "Allow in Settings",
          desc = "Open the downloaded file. If Android blocks it, tap \"Settings\" on the pop-up and turn \"Allow from this source\" ON.",
          emoji = "⚙️"
        )

        SimpleStepItem(
          number = "4",
          title = "Install & Open!",
          desc = "Tap \"Install\". If Play Protect prompts you, tap \"More details\" then \"Install anyway\". You are ready! 🎉",
          emoji = "✅"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentGreen),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            "Got It, Close Guide!",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}

@Composable
fun InlineInstallQrCard(
  apkDownloadUrl: String,
  onOpenFullGuide: () -> Unit
) {
  val context = LocalContext.current
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(2.dp, QuantTheme.accentGreen, RoundedCornerShape(16.dp)),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = QuantTheme.surface)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("📲", fontSize = 20.sp)
          Text(
            "Instant Android Sideload",
            color = QuantTheme.accentGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
        
        Text(
          "MOBILE DISCOVERY",
          color = QuantTheme.textMuted,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }

      Text(
        "Point your phone's camera at this QR code to download and install PolyTrader immediately!",
        color = QuantTheme.textPrimary,
        fontSize = 12.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        fontWeight = FontWeight.SemiBold
      )

      // The QR Code - ALWAYS visible inline in preview
      Box(
        modifier = Modifier
          .size(170.dp)
          .background(Color.White, RoundedCornerShape(12.dp))
          .border(2.dp, QuantTheme.accentGreen, RoundedCornerShape(12.dp))
          .padding(8.dp),
        contentAlignment = Alignment.Center
      ) {
        val bitMatrix = remember(apkDownloadUrl) {
          try {
            com.google.zxing.MultiFormatWriter().encode(
              apkDownloadUrl,
              com.google.zxing.BarcodeFormat.QR_CODE,
              250,
              250
            )
          } catch (e: Throwable) {
            null
          }
        }

        if (bitMatrix != null) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val pixelWidth = size.width / matrixWidth
            val pixelHeight = size.height / matrixHeight

            drawRect(color = Color.White)

            for (x in 0 until matrixWidth) {
              for (y in 0 until matrixHeight) {
                if (bitMatrix.get(x, y)) {
                  drawRect(
                    color = Color.Black,
                    topLeft = androidx.compose.ui.geometry.Offset(x * pixelWidth, y * pixelHeight),
                    size = androidx.compose.ui.geometry.Size(pixelWidth + 0.1f, pixelHeight + 0.1f)
                  )
                }
              }
            }
          }
        } else {
          Text("QR Error", color = Color.Red, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
      }

      Text(
        "🎯 Scan this code from your computer screen!",
        color = QuantTheme.accentGreen,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      // Download / Copy actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = {
            val clipManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clipData = android.content.ClipData.newPlainText("PolyTrader APK Link", apkDownloadUrl)
            clipManager.setPrimaryClip(clipData)
            android.widget.Toast.makeText(context, "Link copied!", android.widget.Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.background),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, tint = QuantTheme.textPrimary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Copy Link", color = QuantTheme.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Button(
          onClick = onOpenFullGuide,
          colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentBlue),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.weight(1.2f)
        ) {
          Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Picture Steps", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }
    }
  }
}


