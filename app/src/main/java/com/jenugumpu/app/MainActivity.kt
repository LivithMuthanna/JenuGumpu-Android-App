package com.jenugumpu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseUser
import com.jenugumpu.app.ui.Translations
import com.jenugumpu.app.ui.components.*
import kotlinx.coroutines.tasks.await
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import com.google.android.gms.common.api.ApiException
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.google.firebase.Timestamp
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manual initialization since google-services.json is missing
        val options = FirebaseOptions.Builder()
            .setProjectId("livith01project")
            .setApplicationId("1:588007310466:android:d0bc2ae0aa6f703067209b") // Guessed Android ID, usually web ID works for some things but let's be careful
            .setApiKey("AIzaSyCT4v_lDmNsHG1etC6t8fAEgIF7pCZNHBg")
            .setDatabaseUrl("https://livith01project.firebaseio.com")
            .build()

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options)
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            var darkMode by remember { mutableStateOf(false) }
            val currentUser = remember { mutableStateOf(auth.currentUser) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener {
                    currentUser.value = it.currentUser
                }
                auth.addAuthStateListener(listener)
                onDispose { auth.removeAuthStateListener(listener) }
            }

            JenuGumpuTheme(darkMode) {
                AppNavigator(
                    user = currentUser.value,
                    onThemeToggle = { darkMode = !darkMode },
                    darkMode = darkMode,
                    onGoogleLogin = { startGoogleSignIn() },
                    onLogout = { auth.signOut() }
                )
            }
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            lifecycleScope.launch {
                auth.signInWithCredential(credential).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startGoogleSignIn() {
        // webClientId is normally in google-services.json, using web appId as fallback but
        // usually it needs the actual Web Client ID from Google Cloud Console
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("588007310466-n4dds6qmp7rq4nepsc4acfss78qjsskk.apps.googleusercontent.com")
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }
}

@Composable
fun AppNavigator(
    user: FirebaseUser?,
    onThemeToggle: () -> Unit,
    darkMode: Boolean,
    onGoogleLogin: () -> Unit,
    onLogout: () -> Unit
) {
    var language by remember { mutableStateOf("en") }

    if (user == null) {
        LoginView(
            onLogin = onGoogleLogin,
            onLangToggle = { language = if (language == "en") "kn" else "en" },
            lang = language,
            onThemeToggle = onThemeToggle,
            darkMode = darkMode
        )
    } else {
        JenuGumpuApp(
            user = user,
            language = language,
            onLangToggle = { language = if (language == "en") "kn" else "en" },
            onThemeToggle = onThemeToggle,
            darkMode = darkMode,
            onLogout = onLogout
        )
    }
}

@Composable
fun LoginView(
    onLogin: () -> Unit,
    onLangToggle: () -> Unit,
    lang: String,
    onThemeToggle: () -> Unit,
    darkMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (darkMode) Color(0xFF09090B) else Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                BeeIcon(
                    modifier = Modifier.size(140.dp),
                    color = if (darkMode) Color(0xFFF59E0B) else Color(0xFF0F172A)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                Translations.get("title", lang).uppercase(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-4).sp,
                color = if (darkMode) Color.White else Color(0xFF0F172A)
            )

            Text(
                Translations.get("welcomeLogin", lang),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = if (darkMode) Color.White.copy(alpha = 0.6f) else Color.Gray,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Login, null, tint = if (darkMode) Color.White else Color(0xFF0F172A))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        Translations.get("loginWithGoogle", lang).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                Translations.get("loginDescription", lang) + " Privacy Protocol",
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        // Top right buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Gray.copy(alpha = 0.1f))
                    .clickable { onLangToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (lang == "en") "ಕನ್ನಡ" else "EN", fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Gray.copy(alpha = 0.1f))
                    .clickable { onThemeToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode, null)
            }
        }
    }
}

@Composable
fun JenuGumpuApp(
    user: FirebaseUser,
    language: String,
    onLangToggle: () -> Unit,
    onThemeToggle: () -> Unit,
    darkMode: Boolean,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf("home") }
    var activeTool by remember { mutableStateOf<String?>(null) }
    var editingRecord by remember { mutableStateOf<HarvestRecord?>(null) }
    val db = FirebaseFirestore.getInstance()
    var records by remember { mutableStateOf(listOf<HarvestRecord>()) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(user.uid) {
        db.collection("users").document(user.uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                userProfile = UserProfile(
                    uid = user.uid,
                    displayName = snapshot.getString("displayName") ?: user.displayName ?: "User",
                    displayNameKn = snapshot.getString("displayNameKn"),
                    role = snapshot.getString("role") ?: "hunter",
                    isVerifiedFarmer = snapshot.getBoolean("isVerifiedFarmer") ?: false
                )
            } else {
                // Initial profile
                val initial = UserProfile(uid = user.uid, displayName = user.displayName ?: "User", role = "hunter")
                db.collection("users").document(user.uid).set(initial)
                userProfile = initial
            }
        }

        db.collection("harvest_records")
            .whereEqualTo("userId", user.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    records = snapshot.documents.map { doc ->
                        HarvestRecord(
                            id = doc.id,
                            location = doc.getString("location") ?: "",
                            quantity = doc.getDouble("quantity") ?: 0.0,
                            moisture = doc.getDouble("moisture"),
                            botanicalSource = doc.getString("botanicalSource"),
                            batchId = doc.getString("batchId"),
                            createdAt = doc.getTimestamp("createdAt"),
                            grade = doc.getString("grade") ?: "A"
                        )
                    }
                }
            }
    }

    Scaffold(
        bottomBar = {
            if (activeTab != "add" && activeTool == null) {
                NavigationBar(
                    containerColor = if (darkMode) Color(0xFF09090B) else Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .height(100.dp)
                        .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                        .border(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                ) {
                    val items = listOf("home", "log", "add", "market", "settings")
                    val icons = listOf(Icons.Default.Home, Icons.Default.Assignment, Icons.Default.Add, Icons.Default.TrendingUp, Icons.Default.Settings)

                    items.forEachIndexed { index, item ->
                        val isAdd = item == "add"
                        val isSelected = activeTab == item

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                activeTab = item
                                activeTool = null
                            },
                            icon = {
                                if (isAdd) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .scale(if (isSelected) 1.1f else 1.0f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFF59E0B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icons[index], null, tint = Color(0xFF0F172A), modifier = Modifier.size(28.dp))
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            icons[index],
                                            null,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isSelected) Color(0xFFF59E0B) else Color.Gray
                                        )
                                        AnimatedVisibility(visible = isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF59E0B))
                                            )
                                        }
                                    }
                                }
                            },
                            label = null,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(if (darkMode) Color(0xFF09090B) else Color(0xFFF8FAFC))
        ) {
            if (activeTab != "add" && activeTool == null) {
                AppHeader(language, onLangToggle, onThemeToggle, darkMode, userProfile, onProfileClick = { activeTab = "settings" })
            }

            Box(modifier = Modifier.fillMaxSize()) {
                HoneyParticles(darkMode)
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    AnimatedContent(
                        targetState = activeTool ?: activeTab,
                        transitionSpec = {
                            if (targetState == "add") {
                                (slideInVertically { height -> height } + fadeIn()) togetherWith (slideOutVertically { height -> height } + fadeOut())
                            } else {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            }
                        }
                    ) { target ->
                        when (target) {
                            "grading" -> GradingToolView(language, darkMode, onBack = { activeTool = null })
                            "calc" -> ROIJourneyView(language, darkMode, onBack = { activeTool = null })
                            "equipment" -> ModernToolsView(language, darkMode, onBack = { activeTool = null })
                            "learn" -> AcademyView(language, darkMode, onBack = { activeTool = null })
                            "harvest" -> SustainableHarvestView(language, darkMode, onBack = { activeTool = null })
                            "home" -> HomeView(language, records, userProfile, darkMode, onSetTab = { activeTab = it }, onSetSubView = { activeTool = it })
                            "log" -> LogView(
                                language,
                                records,
                                darkMode,
                                onEdit = { record ->
                                    editingRecord = record
                                    activeTool = "edit"
                                },
                                onDelete = { record ->
                                    db.collection("harvest_records").document(record.id).delete()
                                }
                            )
                            "market" -> MarketView(language, darkMode)
                            "settings" -> SettingsView(
                                user = user,
                                profile = userProfile,
                                lang = language,
                                darkMode = darkMode,
                                onThemeToggle = onThemeToggle,
                                onUpdateProfile = { updated ->
                                    userProfile = updated
                                    db.collection("users").document(user.uid).set(updated)
                                },
                                onBack = { activeTab = "home" },
                                onLogout = onLogout
                            )
                            "profile" -> ProfileView(user, userProfile, language, darkMode, onLogout, onEdit = { activeTab = "settings" }, onBack = { activeTab = "home" })
                            "add" -> AddRecordForm(language, user.uid, darkMode, onBack = { activeTab = "home" })
                            "edit" -> editingRecord?.let { record ->
                                EditRecordForm(language, user.uid, record, darkMode, onBack = {
                                    activeTool = null
                                    editingRecord = null
                                })
                            }
                            else -> Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                FloatingAIChat(language, darkMode)
            }
        }
    }
}

@Composable
fun AppHeader(lang: String, onLangToggle: () -> Unit, onThemeToggle: () -> Unit, darkMode: Boolean, userProfile: UserProfile?, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onProfileClick() }) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile?.displayName != null) {
                    Text(userProfile.displayName.take(1).uppercase(), fontWeight = FontWeight.Black, color = Color(0xFF0F172A), fontSize = 20.sp)
                } else {
                    BeeIcon(modifier = Modifier.size(24.dp), color = Color(0xFF0F172A))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    Translations.get("title", lang).uppercase(),
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = 24.sp,
                    letterSpacing = (-1).sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        Translations.get("tagline", lang).uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                    .border(
                        1.dp,
                        if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onThemeToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = if (darkMode) Color.White else Color(0xFF0F172A)
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                    .border(
                        1.dp,
                        if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onLangToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (lang == "en") "KN" else "EN",
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    color = if (darkMode) Color.White else Color(0xFF0F172A)
                )
            }
        }
    }
}

@Composable
fun HoneyParticles(darkMode: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val particles = remember { List(10) { (0..100).random() to (0..100).random() } }

    val animValues = particles.map {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween((2000..5000).random(), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "p"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
        particles.forEachIndexed { index, p ->
            val offset = Offset(
                size.width * (p.first / 100f) + (animValues[index].value * 50f),
                size.height * (p.second / 100f) + (animValues[index].value * 30f)
            )
            drawCircle(
                color = if (darkMode) Color.White else Color(0xFFF59E0B),
                radius = 80f * animValues[index].value,
                center = offset,
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
fun HomeView(lang: String, records: List<HarvestRecord>, userProfile: UserProfile?, darkMode: Boolean, onSetTab: (String) -> Unit, onSetSubView: (String) -> Unit) {
    val totalStock = records.sumOf { it.quantity }
    val locations = records.map { it.location }.distinct().size

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                val fallback = Translations.get("hunter", lang)
                val displayName = if (lang == "kn" && userProfile?.displayNameKn != null) userProfile.displayNameKn else userProfile?.displayName?.split(" ")?.firstOrNull() ?: fallback

                var showName by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { showName = true }

                AnimatedVisibility(
                    visible = showName,
                    enter = fadeIn(tween(1000)) + slideInHorizontally(tween(800))
                ) {
                    Text(
                        text = displayName.uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-1).sp,
                        color = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                Text(
                    text = if (userProfile?.role == "leader") Translations.get("collectiveHub", lang) else Translations.get("activeHunter", lang),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B),
                    letterSpacing = 2.sp
                )
            }

            HoneyCardPremium(
                title = Translations.get("currentStock", lang).uppercase(),
                value = Translations.formatNumber(String.format("%.1f", totalStock), lang),
                unit = Translations.get("unitKg", lang),
                label = "${Translations.formatNumber(locations.toString(), lang)} " + Translations.get("activeLocations", lang),
                darkMode = darkMode
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = Translations.get("marketInsight", lang).uppercase(),
                    subtitle = Translations.get("viewMarket", lang).uppercase(),
                    icon = Icons.Default.TrendingUp,
                    darkMode = darkMode,
                    onClick = { onSetTab("market") }
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = Translations.get("roiJourney", lang).uppercase(),
                    subtitle = Translations.get("profitCalc", lang).uppercase(),
                    icon = Icons.Default.Bolt,
                    darkMode = darkMode,
                    onClick = { onSetSubView("calc") }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = Translations.get("sharingHub", lang).uppercase(),
                    subtitle = Translations.get("resourceSharing", lang).uppercase(),
                    icon = Icons.Default.Handyman,
                    darkMode = darkMode,
                    onClick = { onSetSubView("equipment") }
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = Translations.get("sustainableHarvest", lang).uppercase(),
                    subtitle = Translations.get("sustainableTips", lang).uppercase(),
                    icon = Icons.Default.Eco,
                    darkMode = darkMode,
                    onClick = { onSetSubView("harvest") }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = Translations.get("gradingTool", lang).uppercase(),
                    subtitle = Translations.get("analyzing", lang).uppercase(),
                    icon = Icons.Default.Gavel,
                    darkMode = darkMode,
                    onClick = { onSetSubView("grading") }
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = Translations.get("academy", lang).uppercase(),
                    subtitle = Translations.get("researchResources", lang).uppercase(),
                    icon = Icons.Default.School,
                    darkMode = darkMode,
                    onClick = { onSetSubView("learn") }
                )
            }
        }

        item {
            NarrativeInsight(
                title = Translations.get("botName", lang).uppercase() + " " + Translations.get("insight", lang).uppercase(),
                text = Translations.get("insightTemplate", lang),
                actionText = Translations.get("askExpert", lang).uppercase(),
                darkMode = darkMode,
                onAction = { onSetSubView("grading") }
            )
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun FloatingAIChat(lang: String, darkMode: Boolean) {
    var isOpen by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var isTyping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 120.dp, end = 20.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(horizontalAlignment = Alignment.End) {
            if (isOpen) {
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .height(400.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF59E0B))
                                .padding(16.dp)
                        ) {
                            Text(
                                Translations.get("aiAssistant", lang).uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp)
                        ) {
                            items(chatHistory) { (msg, isUser) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Surface(
                                        color = if (isUser) Color(0xFFF59E0B) else (if (darkMode) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.widthIn(max = 200.dp),
                                        border = if (!isUser) BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)) else null
                                    ) {
                                        Text(
                                            msg,
                                            modifier = Modifier.padding(12.dp),
                                            fontSize = 14.sp,
                                            color = if (isUser) Color(0xFF0F172A) else (if (darkMode) Color.White else Color(0xFF0F172A))
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Ask Jenu...", fontSize = 12.sp) },
                                shape = RoundedCornerShape(24.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (message.isNotBlank()) {
                                        val userMsg = message
                                        chatHistory = chatHistory + (userMsg to true)
                                        message = ""
                                        isTyping = true
                                        scope.launch {
                                            try {
                                                val model = GenerativeModel("gemini-1.5-flash", "AIzaSyCT4v_lDmNsHG1etC6t8fAEgIF7pCZNHBg")
                                                val res = model.generateContent("You are Jenu-Bee, a friendly and wise AI assistant for a honey producers collective in the Western Ghats. Answer this: $userMsg")
                                                chatHistory = chatHistory + ((res.text ?: "...") to false)
                                            } catch (e: Exception) {
                                                chatHistory = chatHistory + ("Connection unstable. Please move to higher ground." to false)
                                            } finally {
                                                isTyping = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isTyping
                            ) {
                                Icon(Icons.Default.Send, null, tint = Color(0xFFF59E0B))
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { isOpen = !isOpen },
                containerColor = Color(0xFFF59E0B),
                shape = CircleShape
            ) {
                Icon(if (isOpen) Icons.Default.Close else Icons.Default.AutoAwesome, null, tint = Color(0xFF0F172A))
            }
        }
    }
}

@Composable
fun ModernToolsView(lang: String, darkMode: Boolean, onBack: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                    .border(
                        1.dp,
                        if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = if (darkMode) Color.White else Color(0xFF0F172A)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                Translations.get("sharingHub", lang).uppercase(),
                fontWeight = FontWeight.Black,
                color = if (darkMode) Color.White else Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val items = if (lang == "kn") listOf(
            Triple("ರೇಡಿಯಲ್ ಜೇನು ಎಕ್ಸ್‌ಟ್ರಾಕ್ಟರ್", "ಜೇನುತುಪ್ಪವನ್ನು ಪರಿಣಾಮಕಾರಿಯಾಗಿ ತೆಗೆಯಲು ವೃತ್ತಿಪರ ಕೇಂದ್ರಾಪಗಾಮಿ ಎಕ್ಸ್‌ಟ್ರಾಕ್ಟರ್.", "https://www.amazon.in/honey-extractor/s?k=honey-extractor"),
            Triple("ಎಲೆಕ್ಟ್ರಿಕ್ ಅನ್ಕ್ಯಾಪಿಂಗ್ ಚಾಕು", "ಜೇನು ಚೌಕಟ್ಟುಗಳಿಂದ ಮೇಣದ ಹೊದಿಕೆಯನ್ನು ತ್ವರಿತವಾಗಿ ತೆಗೆದುಹಾಕಲು ಬಿಸಿ ಬ್ಲೇಡ್.", "https://www.amazon.in/Electric-Uncapping-Knife/s?k=Electric+Uncapping+Knife"),
            Triple("ಡಿಜಿಟಲ್ ರಿಫ್ರ್ಯಾಕ್ಟೋಮೀಟರ್", "ಜೇನುತುಪ್ಪದ ಗುಣಮಟ್ಟ ಖಚಿತಪಡಿಸಿಕೊಳ್ಳಲು ತೇವಾಂಶದ ಪ್ರಮಾಣವನ್ನು ನಿಖರವಾಗಿ ಅಳೆಯುತ್ತದೆ.", "https://www.amazon.in/honey-refractometer/s?k=honey+refractometer"),
            Triple("ಹೀಟ್ ಗಾರ್ಡ್ ಹೊಂದಿರುವ ಸ್ಮೋಕರ್", "ಸುರಕ್ಷತಾ ರಕ್ಷಣೆಯೊಂದಿಗೆ ಸುಗ್ಗಿಯ ಸಮಯದಲ್ಲಿ ಜೇನುನೊಣಗಳನ್ನು ಶಾಂತಗೊಳಿಸಲು ಅತ್ಯಗತ್ಯ ಸಾಧನ.", "https://www.amazon.in/Bee-Smoker/s?k=Bee-Smoker"),
            Triple("ಬೀ ಬ್ರಷ್ (ಕುದುರೆ ಕೂದಲು)", "ಬಾಚಣಿಗೆಯಿಂದ ಜೇನುನೊಣಗಳನ್ನು ನಿಧಾನವಾಗಿ ತೆಗೆದುಹಾಕಲು ಮೃದುವಾದ ನೈಸರ್ಗಿಕ ನಾರುಗಳು.", "https://www.amazon.in/Bee-Brush/s?k=Bee-Brush")
        ) else listOf(
            Triple("Radial Honey Extractor", "Professional centrifugal extractor for efficient honey removal.", "https://www.amazon.in/honey-extractor/s?k=honey+extractor"),
            Triple("Electric Uncapping Knife", "Heated blade to quickly remove wax cappings from honey frames.", "https://www.amazon.in/Electric-Uncapping-Knife/s?k=Electric+Uncapping+Knife"),
            Triple("Digital Refractometer", "Accurately measures moisture content to ensure honey quality.", "https://www.amazon.in/honey-refractometer/s?k=honey+refractometer"),
            Triple("Smoker with Heat Guard", "Essential tool for calming bees during harvest with safety protection.", "https://www.amazon.in/Bee-Smoker/s?k=Bee-Smoker"),
            Triple("Bee Brush (Horsehair)", "Soft natural bristles to gently remove bees from the comb.", "https://www.amazon.in/Bee-Brush/s?k=Bee-Brush")
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items) { (name, desc, url) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(url) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                    border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, color = if (darkMode) Color.White else Color(0xFF0F172A))
                            Text(desc, fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { uriHandler.openUri(url) }) {
                            Icon(Icons.Default.OpenInNew, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AcademyView(lang: String, darkMode: Boolean, onBack: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                    .border(
                        1.dp,
                        if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = if (darkMode) Color.White else Color(0xFF0F172A)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                Translations.get("academy", lang).uppercase(),
                fontWeight = FontWeight.Black,
                color = if (darkMode) Color.White else Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val articles = if (lang == "kn") listOf(
            Triple("ಜರ್ನಲ್ ಆಫ್ ಎಪಿಬಯಾಲಜಿ ರಿಸರ್ಚ್", "ಜೇನುನೊಣಗಳ ಜೀವಶಾಸ್ತ್ರ ಮತ್ತು ಆರೋಗ್ಯದ ಕುರಿತು ಪೀರ್-ರಿವ್ಯೂಡ್ ವೈಜ್ಞಾನಿಕ ಸಂಶೋಧನೆ.", "https://www.tandfonline.com/journals/tjar20"),
            Triple("ಬೀ ವರ್ಲ್ಡ್", "ಜಾಗತಿಕ ಜೇನುಸಾಕಣೆ ಪ್ರವೃತ್ತಿಗಳು ಮತ್ತು ಒಳನೋಟಗಳ ಸಮಗ್ರ ಸಂಪನ್ಮೂಲ.", "https://www.tandfonline.com/toc/tbee20/current"),
            Triple("ಜರ್ನಲ್ ಆಫ್ ಎಪಿಬಯಾಲಜಿಕಲ್ ಸೈನ್ಸ್", "ಜೇನುಸಾಕಣೆ, ಜೀವಶಾಸ್ತ್ರ ಮತ್ತು ಪರಾಗಸ್ಪರ್ಶಕ್ಕೆ ಸಂಬಂಧಿಸಿದ ಸಂಶೋಧನೆ.", "https://sciendo.com/journal/JAS"),
            Triple("ಎಪಿಡೋಲೊಜಿ", "ಬಂಬಲ್‌ಬೀಗಳು ಮತ್ತು ಕುಟುಕದ ಜೇನುನೊಣಗಳು ಸೇರಿದಂತೆ ಜೇನುನೊಣಗಳ ಜೀವಶಾಸ್ತ್ರ.", "https://www.springer.com/journal/13592"),
            Triple("ಅಂತರಾಷ್ಟ್ರೀಯ ಅಕಾರಾಲಜಿ ಜರ್ನಲ್", "ಜೇನುನೊಣಗಳ ಆರೋಗ್ಯದ ಮೇಲೆ ಪರಿಣಾಮ ಬೀರುವ ಹುಳುಗಳ ಕುರಿತು ವಿವರವಾದ ಸಂಶೋಧನೆ.", "https://www.tandfonline.com/journals/taca20"),
            Triple("ವೈಜ್ಞಾನಿಕ ವರದಿಗಳು: ಜೇನುನೊಣಗಳ ಕುಸಿತ", "ಜೇನುನೊಣಗಳ ಸಂಖ್ಯೆ ಮತ್ತು ಪರಿಸರ ಪ್ರಭಾವದ ಕುರಿತು ಪ್ರಕೃತಿ ಸಂಶೋಧನೆ.", "https://www.nature.com/articles/srep11781"),
            Triple("PLOS ONE: ಜೇನುನೊಣಗಳ ಆರೋಗ್ಯ", "ಜೇನುನೊಣಗಳ ಪರಿಸರ ವಿಜ್ಞಾನ ಮತ್ತು ರೋಗಗಳ ಕುರಿತು ಮುಕ್ತ ಸಂಶೋಧನೆ.", "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0147283"),
            Triple("ಹಿಮಾಲಯನ್ ಜೇನುಸಾಕಣೆ ಸಂಶೋಧನೆ", "ಹಿಮಾಲಯದ ಜೇನುತುಪ್ಪ ಮತ್ತು ಸ್ಥಳೀಯ ಜೇನುನೊಣಗಳ ಕುರಿತು ಸಂಶೋಧನಾ ಪ್ರಬಂಧಗಳು.", "https://www.researchgate.net/search/publication?q=himalayan+beekeeping"),
            Triple("FAO: ಸುಸ್ಥಿರ ಜೇನುಸಾಕಣೆ", "ಸುಸ್ಥಿರ ಜೇನುಸಾಕಣೆಯ ಜಾಗತಿಕ ಮಾರ್ಗದರ್ಶನ ಮತ್ತು ನೀತಿ ದಾಖಲೆಗಳು.", "https://www.fao.org/3/i0842e/i0842e00.htm"),
            Triple("ಅಪಿಮೊಂಡಿಯಾ ಪ್ರಕಟಣೆಗಳು", "ಅಂತರಾಷ್ಟ್ರೀಯ ಜೇನುಗಾರರ ಒಕ್ಕೂಟದಿಂದ ವೈಜ್ಞಾನಿಕ ಪ್ರಕಟಣೆಗಳು.", "https://www.apimondia.org/publications.html")
        ) else listOf(
            Triple("Journal of Apicultural Research", "Peer-reviewed scientific research on bee biology and health.", "https://www.tandfonline.com/journals/tjar20"),
            Triple("Bee World", "Comprehensive resource for global beekeeping trends and insights.", "https://www.tandfonline.com/toc/tbee20/current"),
            Triple("Journal of Apicultural Science", "Research regarding apiculture, biology, and pollination.", "https://sciendo.com/journal/JAS"),
            Triple("Apidologie", "Biology of bees, including bumblebees and stingless bees.", "https://www.springer.com/journal/13592"),
            Triple("International Journal of Acarology", "Detailed research on mites affecting bee health.", "https://www.tandfonline.com/journals/taca20"),
            Triple("Scientific Reports: Bee Decline", "Nature research on bee population and ecological impact.", "https://www.nature.com/articles/srep11781"),
            Triple("PLOS ONE: Honey Bee Health", "Open access studies on honey bee ecology and diseases.", "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0147283"),
            Triple("Research on Himalayan Beekeeping", "Specialized papers on high-altitude honey and native bees.", "https://www.researchgate.net/search/publication?q=himalayan+beekeeping"),
            Triple("FAO: Sustainable Beekeeping", "Global guidance and policy documents on sustainable apiary.", "https://www.fao.org/3/i0842e/i0842e00.htm"),
            Triple("Apimondia Publications", "Scientific publications from the International Federation of Beekeepers.", "https://www.apimondia.org/publications.html")
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(articles) { (title, desc, url) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(url) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                    border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (darkMode) Color.White else Color(0xFF0F172A))
                        Text(desc, fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Translations.get("viewAll", lang).uppercase(), color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogView(
    lang: String,
    records: List<HarvestRecord>,
    darkMode: Boolean,
    onEdit: (HarvestRecord) -> Unit,
    onDelete: (HarvestRecord) -> Unit
) {
    var recordToDelete by remember { mutableStateOf<HarvestRecord?>(null) }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text(Translations.get("confirmDelete", lang), fontWeight = FontWeight.Black) },
            text = { Text(Translations.get("deleteRecordConfirm", lang)) },
            confirmButton = {
                TextButton(onClick = {
                    recordToDelete?.let { onDelete(it) }
                    recordToDelete = null
                }) {
                    Text(Translations.get("delete", lang).uppercase(), color = Color.Red, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text(Translations.get("cancel", lang).uppercase(), color = if (darkMode) Color.White else Color.Black)
                }
            },
            containerColor = if (darkMode) Color(0xFF1E293B) else Color.White,
            titleContentColor = if (darkMode) Color.White else Color(0xFF0F172A),
            textContentColor = (if (darkMode) Color.White else Color(0xFF0F172A)).copy(alpha = 0.7f)
        )
    }

    Column(modifier = Modifier.padding(top = 8.dp).fillMaxSize()) {
        Text(
            Translations.get("harvestLog", lang).uppercase(),
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1).sp,
            color = if (darkMode) Color.White else Color(0xFF0F172A)
        )
        Text(
            Translations.get("protocolTraceability", lang),
            fontSize = 10.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Translations.get("noLogs", lang), color = Color.Gray)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            itemsIndexed(records) { index, record ->
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 100L)
                    isVisible = true
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
                ) {
                    val date = record.createdAt?.toDate()?.let {
                        java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(it)
                    } ?: "New"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                            .border(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(32.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(Translations.formatNumber(date.uppercase(), lang), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                                    Text(record.location, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (darkMode) Color.White else Color(0xFF0F172A))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { onEdit(record) },
                                        modifier = Modifier.size(32.dp).background(if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = { recordToDelete = record },
                                        modifier = Modifier.size(32.dp).background(if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red.copy(alpha = 0.6f))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    if (!record.batchId.isNullOrEmpty() || !record.botanicalSource.isNullOrEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                            if (!record.batchId.isNullOrEmpty()) {
                                                Text("#${record.batchId}", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                            }
                                            if (!record.botanicalSource.isNullOrEmpty()) {
                                                Text(Translations.get(record.botanicalSource, lang).uppercase(), fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Box(modifier = Modifier.size(4.dp).background(Color(0xFF10B981), CircleShape))
                                        Text(" " + record.grade, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        if (record.moisture != null) {
                                            Text(" • " + Translations.formatNumber(record.moisture.toString(), lang) + "% " + Translations.get("moisture", lang), fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Translations.formatNumber(String.format("%.1f", record.quantity), lang), fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (darkMode) Color.White else Color(0xFF0F172A))
                                    Text(Translations.get("unitKg", lang).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
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
fun LiveIndicator(lang: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = alpha))
        )
        Spacer(Modifier.width(6.dp))
        Text(Translations.get("live", lang).uppercase(), color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun MarketView(lang: String, darkMode: Boolean) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var activeRegionForGraph by remember { mutableStateOf<Map<String, String>?>(null) }
    val points = listOf(0.8f, 0.75f, 0.6f, 0.65f, 0.4f, 0.45f, 0.3f)
    val wholesalePrices = remember { mutableStateListOf(410, 415, 412, 418, 425, 422, 420) }
    val retailPrices = remember { mutableStateListOf(550, 560, 555, 570, 585, 580, 575) }

    val wholesalePoints = remember(wholesalePrices.size) {
        val min = wholesalePrices.minOrNull() ?: 0
        val max = retailPrices.maxOrNull() ?: 1
        val range = (max - min).coerceAtLeast(1)
        wholesalePrices.map { 0.2f + 0.6f * (1.0f - (it - min).toFloat() / range) }
    }
    val retailPoints = remember(retailPrices.size) {
        val min = wholesalePrices.minOrNull() ?: 0
        val max = retailPrices.maxOrNull() ?: 1
        val range = (max - min).coerceAtLeast(1)
        retailPrices.map { 0.2f + 0.6f * (1.0f - (it - min).toFloat() / range) }
    }

    val regionalWholesalePrices = remember { mutableStateListOf<Int>() }
    val regionalRetailPrices = remember { mutableStateListOf<Int>() }

    val regionalWholesalePoints = remember(regionalWholesalePrices.size) {
        if (regionalWholesalePrices.isEmpty()) emptyList()
        else {
            val min = regionalWholesalePrices.minOrNull() ?: 0
            val max = regionalRetailPrices.maxOrNull() ?: 1
            val range = (max - min).coerceAtLeast(1)
            regionalWholesalePrices.map { 0.2f + 0.6f * (1.0f - (it - min).toFloat() / range) }
        }
    }
    val regionalRetailPoints = remember(regionalRetailPrices.size) {
        if (regionalRetailPrices.isEmpty()) emptyList()
        else {
            val min = regionalWholesalePrices.minOrNull() ?: 0
            val max = regionalRetailPrices.maxOrNull() ?: 1
            val range = (max - min).coerceAtLeast(1)
            regionalRetailPrices.map { 0.2f + 0.6f * (1.0f - (it - min).toFloat() / range) }
        }
    }

    LaunchedEffect(activeRegionForGraph) {
        if (activeRegionForGraph != null) {
            regionalWholesalePrices.clear()
            regionalRetailPrices.clear()
            val basePrice = activeRegionForGraph!!["price"]?.replace("₹", "")?.toIntOrNull() ?: 400
            val retailBase = (basePrice * 1.3).toInt()

            repeat(7) {
                regionalWholesalePrices.add(basePrice + (-10..10).random())
                regionalRetailPrices.add(retailBase + (-15..15).random())
            }

            while(activeRegionForGraph != null) {
                delay(2000)
                val lastW = regionalWholesalePrices.last()
                val nextW = (lastW + (-2..3).random()).coerceIn(basePrice - 30, basePrice + 30)
                regionalWholesalePrices[regionalWholesalePrices.size - 1] = nextW

                val lastR = regionalRetailPrices.last()
                val nextR = (lastR + (-3..4).random()).coerceIn(retailBase - 40, retailBase + 40)
                regionalRetailPrices[regionalRetailPrices.size - 1] = nextR
            }
        }
    }

    val days = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")

    LaunchedEffect(Unit) {
        while(true) {
            delay(3000)
            val lastW = wholesalePrices.last()
            val nextW = (lastW + (-1..2).random()).coerceIn(410, 440)
            wholesalePrices[wholesalePrices.size - 1] = nextW

            val lastR = retailPrices.last()
            val nextR = (lastR + (-2..3).random()).coerceIn(540, 590)
            retailPrices[retailPrices.size - 1] = nextR
        }
    }

    LazyColumn(
        modifier = Modifier.padding(top = 8.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                Translations.get("marketInsight", lang).uppercase(),
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-1).sp
            )
        }

        item {
            // Premium Dark Card for Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp))
                    .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                    .border(BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)), RoundedCornerShape(40.dp))
                    .padding(32.dp)
            ) {
                Column {
                    val displayIndex = selectedIndex ?: (wholesalePoints.size - 1)
                    val wPrice = wholesalePrices[displayIndex]
                    val rPrice = retailPrices[displayIndex]
                    val currentDayKey = days[displayIndex]
                    val isToday = selectedIndex == null || selectedIndex == wholesalePoints.size - 1

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isToday) Translations.get("priceMonitor", lang).uppercase()
                                    else Translations.get(currentDayKey, lang).uppercase() + " " + Translations.get("price", lang).uppercase(),
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                if (isToday) {
                                    Spacer(Modifier.width(8.dp))
                                    LiveIndicator(lang)
                                }
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Column {
                                    Text(Translations.get("retail", lang).uppercase(), fontSize = 8.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Black)
                                    Text("₹${Translations.formatNumber(rPrice.toString(), lang)}", color = if (darkMode) Color.White else Color(0xFF0F172A), fontSize = 32.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.width(24.dp))
                                Column {
                                    Text(Translations.get("wholesale", lang).uppercase(), fontSize = 8.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Black)
                                    Text("₹${Translations.formatNumber(wPrice.toString(), lang)}", color = (if (darkMode) Color.White else Color(0xFF0F172A)).copy(alpha = 0.6f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Translations.get("bullish", lang).uppercase(), color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Box {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            val widthStep = size.width / (wholesalePoints.size - 1)
                                            selectedIndex = (offset.x / widthStep).roundToInt().coerceIn(0, wholesalePoints.size - 1)
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val widthStep = size.width / (wholesalePoints.size - 1)
                                            selectedIndex = (offset.x / widthStep).roundToInt().coerceIn(0, wholesalePoints.size - 1)
                                        },
                                        onDrag = { change, _ ->
                                            val widthStep = size.width / (wholesalePoints.size - 1)
                                            selectedIndex = (change.position.x / widthStep).roundToInt().coerceIn(0, wholesalePoints.size - 1)
                                        },
                                        onDragEnd = { selectedIndex = null }
                                    )
                                }
                        ) {
                            val widthStep = size.width / (wholesalePoints.size - 1)

                            // Background fill (Retail)
                            val fillPathR = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, size.height)
                                retailPoints.forEachIndexed { index, p ->
                                    lineTo(index * widthStep, size.height * p)
                                }
                                lineTo(size.width, size.height)
                                close()
                            }
                            drawPath(
                                fillPathR,
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.2f), Color.Transparent)
                                )
                            )

                            // Wholesale line
                            val strokePathW = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, size.height * wholesalePoints[0])
                                wholesalePoints.forEachIndexed { index, p ->
                                    if (index > 0) lineTo(index * widthStep, size.height * p)
                                }
                            }
                            drawPath(
                                strokePathW,
                                color = Color(0xFF10B981).copy(alpha = 0.5f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                            )

                            // Retail line
                            val strokePathR = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, size.height * retailPoints[0])
                                retailPoints.forEachIndexed { index, p ->
                                    if (index > 0) lineTo(index * widthStep, size.height * p)
                                }
                            }
                            drawPath(
                                strokePathR,
                                brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFACC15))),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )

                            // Dots for points
                            wholesalePoints.forEachIndexed { index, p ->
                                val isSelected = index == selectedIndex
                                if (isSelected) {
                                    val dotRadius = 8.dp.toPx()
                                    drawCircle(
                                        color = Color.White,
                                        radius = dotRadius,
                                        center = Offset(index * widthStep, size.height * retailPoints[index])
                                    )

                                    // Vertical guide line
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.3f),
                                        start = Offset(index * widthStep, 0f),
                                        end = Offset(index * widthStep, size.height),
                                        strokeWidth = 2.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEachIndexed { index, day ->
                            val isSelected = index == selectedIndex
                            Text(
                                Translations.get(day, lang).uppercase(),
                                fontSize = 8.sp,
                                color = if (isSelected) Color(0xFFF59E0B) else (if (darkMode) Color.White.copy(alpha = 0.3f) else Color(0xFF0F172A).copy(alpha = 0.3f)),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                Translations.get("regionalNews", lang).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.Gray
            )
        }

        val indices = listOf(
            mapOf("name" to "kodaguRegion", "price" to "₹420", "trend" to "+2.4%"),
            mapOf("name" to "wayanadPlateau", "price" to "₹395", "trend" to "-0.8%"),
            mapOf("name" to "shimogaForest", "price" to "₹415", "trend" to "+1.2%"),
            mapOf("name" to "uttaraKannada", "price" to "₹405", "trend" to "+0.5%"),
            mapOf("name" to "belagavi", "price" to "₹385", "trend" to "-1.5%"),
            mapOf("name" to "chamarajanagar", "price" to "₹410", "trend" to "+3.1%"),
            mapOf("name" to "hassan", "price" to "₹402", "trend" to "+0.9%"),
            mapOf("name" to "davanagere", "price" to "₹370", "trend" to "+1.8%")
        )

        items(indices) { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeRegionForGraph = index },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(Translations.get(index["name"] ?: "", lang).uppercase(), fontWeight = FontWeight.Black, fontSize = 12.sp, color = if (darkMode) Color.White else Color(0xFF0F172A))
                        Text(Translations.get("nodeCluster", lang).uppercase(), fontSize = 8.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Translations.formatNumber(index["price"] ?: "", lang), fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (darkMode) Color.White else Color(0xFF0F172A))
                        val trend = Translations.formatNumber(index["trend"] ?: "", lang)
                        Text(trend, color = if (index["trend"]?.startsWith("+") == true) Color(0xFF10B981) else Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (activeRegionForGraph != null) {
        Dialog(onDismissRequest = { activeRegionForGraph = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                Translations.get(activeRegionForGraph!!["name"] ?: "", lang).uppercase(),
                                fontWeight = FontWeight.Black,
                                color = if (darkMode) Color.White else Color(0xFF0F172A),
                                fontSize = 16.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LiveIndicator(lang)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    Translations.get("liveMarketPrice", lang).uppercase(),
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = { activeRegionForGraph = null }) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = if (darkMode) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Translations.get("retail", lang).uppercase(), fontSize = 8.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Black)
                            val rPrice = regionalRetailPrices.lastOrNull() ?: 0
                            Text("₹${Translations.formatNumber(rPrice.toString(), lang)}", color = if (darkMode) Color.White else Color(0xFF0F172A), fontSize = 32.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Translations.get("wholesale", lang).uppercase(), fontSize = 8.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Black)
                            val wPrice = regionalWholesalePrices.lastOrNull() ?: 0
                            Text("₹${Translations.formatNumber(wPrice.toString(), lang)}", color = (if (darkMode) Color.White else Color(0xFF0F172A)).copy(alpha = 0.6f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        if (regionalWholesalePoints.isNotEmpty()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val widthStep = size.width / (regionalWholesalePoints.size - 1)

                                // Retail line
                                val strokePathR = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, size.height * regionalRetailPoints[0])
                                    regionalRetailPoints.forEachIndexed { index, p ->
                                        if (index > 0) lineTo(index * widthStep, size.height * p)
                                    }
                                }
                                drawPath(
                                    strokePathR,
                                    color = Color(0xFFF59E0B),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                )

                                // Wholesale line
                                val strokePathW = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, size.height * regionalWholesalePoints[0])
                                    regionalWholesalePoints.forEachIndexed { index, p ->
                                        if (index > 0) lineTo(index * widthStep, size.height * p)
                                    }
                                }
                                drawPath(
                                    strokePathW,
                                    color = Color(0xFF10B981).copy(alpha = 0.5f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        Translations.get("marketVolatilityNotice", lang) ?: "Real-time verification recommended for high-volume transactions.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ToolsView(lang: String, darkMode: Boolean, onSelectTool: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            Translations.get("toolkit", lang).uppercase(),
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1).sp,
            color = if (darkMode) Color.White else Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(16.dp))

        val tools = listOf(
            ToolItem(Translations.get("gradingTool", lang), "grading", Icons.Default.Science),
            ToolItem(Translations.get("roiJourney", lang), "calc", Icons.Default.Bolt),
            ToolItem(Translations.get("sharingHub", lang), "equipment", Icons.Default.Inventory),
            ToolItem(Translations.get("academy", lang), "learn", Icons.Default.School),
            ToolItem(Translations.get("sustainableHarvest", lang), "harvest", Icons.Default.Eco)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(tools) { tool ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectTool(tool.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkMode) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                tool.name,
                                fontWeight = FontWeight.Bold,
                                color = if (darkMode) Color.White else Color(0xFF0F172A)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = if (darkMode) Color.White.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

data class ToolItem(val name: String, val id: String, val icon: ImageVector)

@Composable
fun ROIJourneyView(lang: String, darkMode: Boolean, onBack: () -> Unit) {
    var qty by remember { mutableStateOf("100") }
    var purification by remember { mutableStateOf(false) }
    var certification by remember { mutableStateOf(false) }
    var packaging by remember { mutableStateOf(false) }

    val rawPrice = 300.0
    val volume = qty.toDoubleOrNull() ?: 0.0
    val rawValue = volume * rawPrice

    var currentCost = 0.0
    var currentPrice = rawPrice

    if (purification) {
        currentCost += 20.0
        currentPrice += 40.0
    }
    if (certification) {
        currentPrice += 50.0
    }
    if (packaging) {
        currentCost += 40.0
        currentPrice = rawPrice + 350.0
    }

    val finalRevenue = volume * currentPrice
    val totalCost = volume * currentCost
    val netProfit = finalRevenue - totalCost

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                        .border(
                            1.dp,
                            if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                Column {
                    Text(
                        Translations.get("roiManifest", lang).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                    Text(Translations.get("wealthSynthesis", lang), fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(Translations.get("stockInput", lang).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = Translations.formatNumber(qty, lang),
                            onValueChange = { input ->
                                // Clean up the input to keep only standard digits for calculations
                                val clean = input.filter { it.isDigit() || it in '೦'..'೯' }
                                    .map { char ->
                                        if (char in '೦'..'೯') (char.code - '೦'.code + '0'.code).toChar() else char
                                    }.joinToString("")
                                qty = clean
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = if (darkMode) Color.White else Color(0xFF0F172A)
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = if (darkMode) Color.White else Color(0xFF0F172A),
                                unfocusedTextColor = if (darkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                        Text(Translations.get("unitKg", lang), fontSize = 24.sp, fontStyle = FontStyle.Italic, color = Color(0xFFF59E0B))
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = if (darkMode) Color.Gray.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Translations.get("rawLiquidValue", lang).uppercase(), fontSize = 8.sp, color = Color.Gray)
                            Text("₹${Translations.formatNumber(String.format("%.0f", rawValue), lang)}", fontWeight = FontWeight.Black, color = if (darkMode) Color.White else Color(0xFF0F172A))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Translations.get("optimizedValue", lang).uppercase(), fontSize = 8.sp, color = Color(0xFF10B981))
                            Text("₹${Translations.formatNumber(String.format("%.0f", finalRevenue), lang)}", fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }

        item {
            Text(Translations.get("optimizationVectors", lang).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
        }

        val vectors = listOf(
            Triple("purification", Translations.get("purificationNode", lang), purification),
            Triple("certification", Translations.get("certificationNode", lang), certification),
            Triple("packaging", Translations.get("premiumPackaging", lang), packaging)
        )

        items(vectors) { (id, title, active) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when(id) {
                            "purification" -> purification = !purification
                            "certification" -> certification = !certification
                            "packaging" -> packaging = !packaging
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (active) Color(0xFFF59E0B) else (if (darkMode) Color.White.copy(alpha = 0.05f) else Color.White)
                ),
                border = if (!active) BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)) else null
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title.uppercase(), fontWeight = FontWeight.Black, color = if (active) Color(0xFF0F172A) else (if (darkMode) Color.White else Color(0xFF0F172A)))
                    if (active) Icon(Icons.Default.Check, null, tint = Color(0xFF0F172A))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Translations.get("netYieldManifest", lang).uppercase(), color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text("₹${Translations.formatNumber(String.format("%.0f", netProfit), lang)}", color = if (darkMode) Color.White else Color(0xFF0F172A), fontSize = 48.sp, fontWeight = FontWeight.Black)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(Translations.get("grossProjection", lang).uppercase(), color = Color.Gray, fontSize = 8.sp)
                            Text("₹${Translations.formatNumber(String.format("%.0f", finalRevenue), lang)}", color = if (darkMode) Color.White else Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Translations.get("operationCost", lang).uppercase(), color = Color.Gray, fontSize = 8.sp)
                            Text("₹${Translations.formatNumber(String.format("%.0f", totalCost), lang)}", color = if (darkMode) Color.White else Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsView(
    user: FirebaseUser,
    profile: UserProfile?,
    lang: String,
    darkMode: Boolean,
    onThemeToggle: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var displayName by remember { mutableStateOf(profile?.displayName ?: "") }
    var displayNameKn by remember { mutableStateOf(profile?.displayNameKn ?: "") }
    var pushEnabled by remember { mutableStateOf(true) }
    var harvestAlerts by remember { mutableStateOf(true) }
    var marketUpdates by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(Translations.get("deleteConfirmTitle", lang)) },
            text = { Text(Translations.get("deleteConfirmMessage", lang)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        user.delete().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLogout()
                            } else {
                                // Potentially re-auth needed, but for now we LOG
                                android.util.Log.e("JenuApp", "Deletion failed", task.exception)
                            }
                        }
                    }
                ) {
                    Text(Translations.get("yes", lang).uppercase(), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(Translations.get("no", lang).uppercase())
                }
            },
            containerColor = if (darkMode) Color(0xFF1E293B) else Color.White,
            titleContentColor = if (darkMode) Color.White else Color(0xFF0F172A),
            textContentColor = if (darkMode) Color.LightGray else Color.Gray
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                        .border(
                            1.dp,
                            if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    Translations.get("settings", lang).uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = if (darkMode) Color.White else Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Profile Section
        item {
            SettingHeader(Translations.get("profile", lang), lang, darkMode)
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) Color.White.copy(alpha = 0.05f) else Color.White),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(displayName.take(1).uppercase(), color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (darkMode) Color.White else Color(0xFF0F172A))
                            Text(user.email ?: "", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    JenuTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = Translations.get("displayName", lang),
                        darkMode = darkMode
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    JenuTextField(
                        value = displayNameKn,
                        onValueChange = { displayNameKn = it },
                        label = Translations.get("displayNameKn", lang),
                        darkMode = darkMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow(Translations.get("emailAddress", lang), user.email ?: "verified@example.com", user.isEmailVerified, darkMode)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            profile?.let {
                                onUpdateProfile(it.copy(displayName = displayName, displayNameKn = displayNameKn))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text(Translations.get("saveChanges", lang).uppercase(), fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    }
                }
            }
        }

        // Appearance Section
        item {
            SettingHeader(Translations.get("appearance", lang), lang, darkMode)
            SettingsToggleRow(
                icon = Icons.Default.Brightness4,
                title = Translations.get("darkMode", lang),
                subtitle = if (lang == "kn") "ಲೈಟ್ ಮತ್ತು ಡಾರ್ಕ್ ಥೀಮ್‌ಗಳ ನಡುವೆ ಬದಲಿಸಿ" else "Toggle between light and dark themes",
                checked = darkMode,
                onCheckedChange = { onThemeToggle() },
                darkMode = darkMode
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Notifications Section
        item {
            SettingHeader(Translations.get("notifications", lang), lang, darkMode)
            SettingsToggleRow(
                icon = Icons.Default.Notifications,
                title = Translations.get("pushNotifications", lang),
                subtitle = if (lang == "kn") "ನಿಮ್ಮ ಸಾಧನದಲ್ಲಿ ಎಚ್ಚರಿಕೆಗಳನ್ನು ಪಡೆಯಿರಿ" else "Receive alerts on your device",
                checked = pushEnabled,
                onCheckedChange = { pushEnabled = it },
                darkMode = darkMode
            )
            SettingsToggleRow(
                icon = Icons.Default.AddAlert,
                title = Translations.get("harvestAlerts", lang),
                subtitle = if (lang == "kn") "ನಿಮ್ಮ ಪ್ರದೇಶದಲ್ಲಿ ಕೊಯ್ಲು ಮಾಡಲು ಉತ್ತಮ ಸಮಯಗಳು" else "Best times for harvest in your region",
                checked = harvestAlerts,
                onCheckedChange = { harvestAlerts = it },
                darkMode = darkMode
            )
            SettingsToggleRow(
                icon = Icons.Default.Poll,
                title = Translations.get("marketUpdates", lang),
                subtitle = if (lang == "kn") "ದೈನಂದಿನ ಬೆಲೆ ಏರಿಳಿತಗಳು ಮತ್ತು ಪ್ರವೃತ್ತಿಗಳು" else "Daily price fluctuations and trends",
                checked = marketUpdates,
                onCheckedChange = { marketUpdates = it },
                darkMode = darkMode
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Other Settings
        item {
            SettingHeader(if (lang == "kn") "ಸಿಸ್ಟಮ್ ಮತ್ತು ಖಾತೆ" else "SYSTEM & ACCOUNT", lang, darkMode)
            SettingActionRow(Icons.Default.Delete, Translations.get("deleteAccount", lang), darkMode, Color.Red) {
                showDeleteConfirm = true
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text(if (lang == "kn") "ಲಾಗ್‌ಔಟ್ ಮಾಡಿ" else "LOGOUT SESSION", fontWeight = FontWeight.Black, color = Color.Red)
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun SettingHeader(title: String, lang: String, darkMode: Boolean) {
    Text(
        title.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = Color.Gray,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String, verified: Boolean, darkMode: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 14.sp, color = if (darkMode) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Medium)
        }
        if (verified) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (value.contains("@") || value.contains("+")) "VERIFIED" else "", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    darkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (darkMode) Color.White else Color(0xFF0F172A), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (darkMode) Color.White else Color(0xFF0F172A))
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFF59E0B),
                checkedTrackColor = Color(0xFFF59E0B).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun SettingActionRow(icon: ImageVector, title: String, darkMode: Boolean, tint: Color? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint ?: (if (darkMode) Color.White else Color(0xFF0F172A)), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            fontWeight = FontWeight.Bold,
            color = tint ?: (if (darkMode) Color.White else Color(0xFF0F172A)),
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun GradingToolView(lang: String, darkMode: Boolean, onBack: () -> Unit) {
    var botanicalSource by remember { mutableStateOf("wildflower") }
    var moisture by remember { mutableStateOf("18.0") }
    var colorChroma by remember { mutableStateOf(40f) } // 0 to 140 scale (Pfund mm)

    var response by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val botanicalOptions = listOf("wildflower", "coffeeBlossom", "forest", "rubber")
    var expanded by remember { mutableStateOf(false) }

    // Helper to get color name from Pfund scale
    fun getPfundName(mm: Float): String {
        val key = when {
            mm <= 8 -> "waterWhite"
            mm <= 17 -> "extraWhite"
            mm <= 34 -> "white"
            mm <= 50 -> "extraLightAmber"
            mm <= 85 -> "lightAmber"
            mm <= 114 -> "amber"
            else -> "darkAmber"
        }
        return Translations.get(key, lang)
    }

    // Colors for the chroma index scale
    val pfundColors = listOf(
        Color(0xFFFFFCF2), // Water White
        Color(0xFFFFF9E6), // Extra White
        Color(0xFFFFF4CC), // White
        Color(0xFFFFE680), // Extra Light Amber
        Color(0xFFFFCC33), // Light Amber
        Color(0xFFCC9900), // Amber
        Color(0xFF663300)  // Dark Amber
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                        .border(
                            1.dp,
                            if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    Translations.get("gradingTool", lang).uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = if (darkMode) Color.White else Color(0xFF0F172A)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text(
                Translations.get("botanicalSource", lang).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF8FAFC))
                    .border(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                    .clickable { expanded = true }
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        Translations.get(botanicalSource, lang).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFFF59E0B))
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(if (darkMode) Color(0xFF1E293B) else Color.White).fillMaxWidth(0.8f)
                ) {
                    botanicalOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(Translations.get(option, lang).uppercase(), fontWeight = FontWeight.Bold) },
                            onClick = {
                                botanicalSource = option
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(textColor = if (darkMode) Color.White else Color(0xFF0F172A))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                Translations.get("pfundScale", lang).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(getPfundName(colorChroma).uppercase(), fontWeight = FontWeight.Black, color = Color(0xFFF59E0B), fontSize = 12.sp)
                        Text(Translations.formatNumber(colorChroma.toInt().toString(), lang) + " mm", fontWeight = FontWeight.Bold, color = if (darkMode) Color.White else Color(0xFF0F172A))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simple color gradient representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(pfundColors))
                    )

                    Slider(
                        value = colorChroma,
                        onValueChange = { colorChroma = it },
                        valueRange = 0f..140f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF59E0B),
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            JenuTextField(
                value = moisture,
                onValueChange = { moisture = it },
                label = Translations.get("moistureLevel", lang),
                darkMode = darkMode
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        delay(1500) // Simulate processing
                        val mVal = moisture.toDoubleOrNull() ?: 0.0

                        val gradeText = when {
                            mVal < 17.0 -> if (lang == "kn") "ಎ+ (ಪ್ರೀಮಿಯಂ ರಫ್ತು)" else "A+ (Premium Export)"
                            mVal <= 18.6 -> if (lang == "kn") "ಎ (ಪ್ರಮಾಣಿತ ಗುಣಮಟ್ಟ)" else "A (Standard Quality)"
                            mVal <= 20.0 -> if (lang == "kn") "ಬಿ (ಸ್ಥಳೀಯ ದರ್ಜೆ)" else "B (Domestic Grade)"
                            else -> if (lang == "kn") "ಸಿ (ಕೈಗಾರಿಕಾ ಬಳಕೆ - ಹುದುಗುವಿಕೆಯ ಅಪಾಯ)" else "C (Industrial Use - Fermentation Risk)"
                        }

                        val clarityText = if (colorChroma < 50) {
                            if (lang == "kn") "ಹೆಚ್ಚಿನ ಸ್ಪಷ್ಟತೆ" else "High Clarity"
                        } else {
                            if (lang == "kn") "ಸಮೃದ್ಧ ವರ್ಣದ್ರವ್ಯ" else "Rich Pigmentation"
                        }

                        val source = Translations.get(botanicalSource, lang)
                        val analysisTitle = Translations.get("analysisComplete", lang).uppercase()
                        val floralLabel = Translations.get("floralAffinity", lang)
                        val opticalLabel = Translations.get("opticalDensity", lang)
                        val clarityLabel = Translations.get("clarity", lang)
                        val recommendationLabel = Translations.get("recommendation", lang)

                        val recommendationText = if (mVal < 19.0) {
                            if (lang == "kn") "ಬಾಟಲಿಂಗ್‌ಗೆ ಸೂಕ್ತವಾಗಿದೆ. ಹೆಚ್ಚಿನ ಮಾರುಕಟ್ಟೆ ಮೌಲ್ಯ." else "Perfect for bottling. High market value."
                        } else {
                            if (lang == "kn") "ತಕ್ಷಣದ ತೇವಾಂಶ ಕಡಿತ ಅಥವಾ ಕೈಗಾರಿಕಾ ಸಂಸ್ಕರಣೆ ಅಗತ್ಯವಿದೆ." else "Requires immediate moisture reduction or industrial processing."
                        }

                        response = "$analysisTitle\n\n" +
                                "${Translations.get("grade", lang)}: $gradeText\n" +
                                "$floralLabel: $source\n" +
                                "$opticalLabel: ${getPfundName(colorChroma)}\n" +
                                "$clarityLabel: $clarityText\n\n" +
                                "$recommendationLabel: $recommendationText"

                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Text(
                    if (isLoading) Translations.get("analyzing", lang).uppercase() else Translations.get("generateReport", lang).uppercase(),
                    fontWeight = FontWeight.Black,
                    color = if (darkMode) Color.White else Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                Translations.get("grade", lang).uppercase() + " REFERENCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            val honeyGradesList = listOf(
                Triple("gradeSpecial", "gradeSpecialDesc", Color(0xFFF59E0B)),
                Triple("gradePremium", "gradePremiumDesc", Color(0xFFFACC15)),
                Triple("gradeStandard", "gradeStandardDesc", Color(0xFF10B981)),
                Triple("gradeIndustrial", "gradeIndustrialDesc", Color.Gray)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(honeyGradesList) { grade ->
                    Card(
                        modifier = Modifier.width(200.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                        border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(grade.third.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WorkspacePremium, null, tint = grade.third, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(Translations.get(grade.first, lang).uppercase(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (darkMode) Color.White else Color(0xFF0F172A))
                            Spacer(Modifier.height(4.dp))
                            Text(Translations.get(grade.second, lang), fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            response?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkMode) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Insights, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("JENU-BEE INSIGHT", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            res,
                            fontSize = 14.sp,
                            color = if (darkMode) Color.White else Color(0xFF0F172A),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SustainableHarvestView(lang: String, darkMode: Boolean, onBack: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                    .border(
                        1.dp,
                        if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = if (darkMode) Color.White else Color(0xFF0F172A)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                Translations.get("sustainableHarvest", lang).uppercase(),
                fontWeight = FontWeight.Black,
                color = if (darkMode) Color.White else Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val techniques = if (lang == "kn") listOf(
            Triple("ಬಾಚಣಿಗೆ ತಿರುಗಿಸುವ ತಂತ್ರ", "ಪ್ರತಿ 3 ವರ್ಷಕ್ಕೊಮ್ಮೆ ಬಾಚಣಿಗೆಗಳನ್ನು ತಿರುಗಿಸುವುದರಿಂದ ರೋಗಕಾರಕಗಳ ಸಂಗ್ರಹವನ್ನು ತಡೆಯಬಹುದು.", "https://beekeep.club/comb-rotation/"),
            Triple("ಸೌರ ಮೇಣ ಕರಗಿಸುವಿಕೆ", "ರಾಸಾಯನಿಕಗಳಿಲ್ಲದೆ ಮೇಣವನ್ನು ಸುಸ್ಥಿರವಾಗಿ ಸಂಸ್ಕರಿಸಲು ಶುದ್ಧ ಸೌರ ಶಕ್ತಿಯನ್ನು ಬಳಸುವುದು.", "https://en.wikipedia.org/wiki/Solar_wax_melter"),
            Triple("ನೈತಿಕ ಕೊಯ್ಲು ಮಿತಿಗಳು", "ಜೇನುಗೂಡಿನ ಉಳಿವನ್ನು ಖಚಿತಪಡಿಸಿಕೊಳ್ಳಲು 60% ಕ್ಕಿಂತ ಹೆಚ್ಚು ಜೇನುತುಪ್ಪವನ್ನು ಎಂದಿಗೂ ಕೊಯ್ಲು ಮಾಡಬೇಡಿ.", "https://beekeeping.fandom.com/wiki/Harvesting_honey"),
            Triple("ಸಾವಯವ ವಾರ್ರೋವಾ ನಿಯಂತ್ರಣ", "ಹುಳುಗಳ ನಿಯಂತ್ರಣಕ್ಕಾಗಿ ಸಂಶ್ಲೇಷಿತ ಕೀಟನಾಶಕಗಳ ಬದಲಿಗೆ ನೈಸರ್ಗಿಕ ಸಾರಭೂತ ತೈಲಗಳನ್ನು ಬಳಸುವುದು.", "https://beemission.com/blogs/news/varroa-mite-control"),
            Triple("ಸ್ಥಳೀಯ ಜೇನುನೊಣ ಸಂರಕ್ಷಣೆ", "ಆಯ್ದ ಮೇವು ನೀಡುವ ಮೂಲಕ ಪಶ್ಚಿಮ ಘಟ್ಟದ ಸ್ಥಳೀಯ ಜೇನುನೊಣಗಳ ಆವಾಸಸ್ಥಾನಗಳನ್ನು ಸಂರಕ್ಷಿಸುವುದು.", "https://www.google.com/search?q=native+bee+conservation+india")
        ) else listOf(
            Triple("Comb Rotation Strategy", "Rotating combs every 3 years prevents pathogen buildup and keeps hives healthy.", "https://beekeep.club/comb-rotation/"),
            Triple("Solar Wax Melting", "Using clean solar energy to process wax sustainably without chemicals.", "https://en.wikipedia.org/wiki/Solar_wax_melter"),
            Triple("Ethical Harvesting Thresholds", "Never harvest more than 60% of surplus honey to ensure colony survival.", "https://beekeeping.fandom.com/wiki/Harvesting_honey"),
            Triple("Organic Varroa Control", "Utilizing natural essential oils instead of synthetic pesticides for mite control.", "https://beemission.com/blogs/news/varroa-mite-control"),
            Triple("Native Bee Protection", "Preserving local Western Ghats bee species habitats through selective foraging.", "https://www.google.com/search?q=native+bee+conservation+india")
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(techniques) { (title, desc, url) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(url) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xFF1E293B) else Color.White),
                    border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    ) {
                        Text(title.uppercase(), fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        Spacer(Modifier.height(8.dp))
                        Text(desc, fontSize = 14.sp, color = if (darkMode) Color.White else Color(0xFF0F172A))
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (lang == "kn") "ಪೂರ್ಣ ಮಾರ್ಗದರ್ಶಿಯನ್ನು ಪ್ರವೇಶಿಸಿ" else "ACCESS FULL GUIDE", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.OpenInNew, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileView(user: FirebaseUser, profile: UserProfile?, lang: String, darkMode: Boolean, onLogout: () -> Unit, onEdit: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                .border(
                    1.dp,
                    if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (darkMode) Color.White else Color(0xFF0F172A)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center
            ) {
                if (user.photoUrl != null) {
                    // In a real app we'd use Coil or Glide, here we just show initial or Bee if no image
                    Text(profile?.displayName?.take(1)?.uppercase() ?: "J", fontSize = 40.sp, fontWeight = FontWeight.Black)
                } else {
                    BeeIcon(modifier = Modifier.size(48.dp), color = Color(0xFF0F172A))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val nameFallback = Translations.get("hunter", lang)
            val name = if (lang == "kn" && profile?.displayNameKn != null) profile.displayNameKn else profile?.displayName ?: nameFallback
            Text(name, fontWeight = FontWeight.Black, fontSize = 32.sp, letterSpacing = (-1).sp, color = if (darkMode) Color.White else Color(0xFF0F172A))
            Text(user.email ?: "", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    if (profile?.role == "leader") Translations.get("collectiveHub", lang).uppercase() else Translations.get("activeHunter", lang).uppercase(),
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkMode) Color(0xFF1E293B) else Color.White
                ),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Text(
                    Translations.get("editProfile", lang).uppercase(),
                    color = if (darkMode) Color.White else Color(0xFF0F172A),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text(Translations.get("logout", lang).uppercase(), color = Color.Red, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EditProfileView(user: FirebaseUser, profile: UserProfile?, lang: String, darkMode: Boolean, onBack: () -> Unit) {
    var displayName by remember { mutableStateOf(profile?.displayName ?: "") }
    var displayNameKn by remember { mutableStateOf(profile?.displayNameKn ?: "") }
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                .border(
                    1.dp,
                    if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (darkMode) Color.White else Color(0xFF0F172A)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            Translations.get("editProfile", lang).uppercase(),
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            fontStyle = FontStyle.Italic,
            color = if (darkMode) Color.White else Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(Translations.get("displayName", lang)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = displayNameKn,
            onValueChange = { displayNameKn = it },
            label = { Text(Translations.get("displayNameKn", lang)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("ಕನ್ನಡದಲ್ಲಿ ಬರೆಯಿರಿ") }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                isSaving = true
                scope.launch {
                    try {
                        val db = FirebaseFirestore.getInstance()
                        val update = hashMapOf<String, Any>(
                            "displayName" to displayName,
                            "displayNameKn" to displayNameKn
                        )
                        db.collection("users").document(user.uid).update(update).await()
                        onBack()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isSaving = false
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(bottom = 16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (darkMode) Color(0xFF1E293B) else Color.White
            ),
            border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
        ) {
            Text(
                if (isSaving) "..." else Translations.get("save", lang).uppercase(),
                fontWeight = FontWeight.Black,
                color = if (darkMode) Color.White else Color(0xFF0F172A)
            )
        }
    }
}

@Composable
fun AddRecordForm(lang: String, userId: String, darkMode: Boolean, onBack: () -> Unit) {
    var location by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var moisture by remember { mutableStateOf("") }
    var batchId by remember { mutableStateOf("") }
    var botanicalSource by remember { mutableStateOf("wildflower") }
    val botanicalOptions = listOf("wildflower", "coffeeBlossom", "forest", "rubber")
    var expanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                        .border(
                            1.dp,
                            if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    Translations.get("addLog", lang).uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = if (darkMode) Color.White else Color(0xFF0F172A),
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            JenuTextField(
                value = location,
                onValueChange = { location = it },
                label = Translations.get("location", lang),
                darkMode = darkMode
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    JenuTextField(
                        value = Translations.formatNumber(quantity, lang),
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it in '೦'..'೯' || it == '.' }
                                .map { char ->
                                    if (char in '೦'..'೯') (char.code - '೦'.code + '0'.code).toChar() else char
                                }.joinToString("")
                            quantity = clean
                        },
                        label = Translations.get("quantity", lang),
                        darkMode = darkMode
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    JenuTextField(
                        value = Translations.formatNumber(moisture, lang),
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it in '೦'..'೯' || it == '.' }
                                .map { char ->
                                    if (char in '೦'..'೯') (char.code - '೦'.code + '0'.code).toChar() else char
                                }.joinToString("")
                            moisture = clean
                        },
                        label = Translations.get("moistureLevel", lang),
                        darkMode = darkMode
                    )
                }
            }
        }

        item {
            Text(
                Translations.get("botanicalSource", lang).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF8FAFC))
                    .border(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                    .clickable { expanded = true }
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        Translations.get(botanicalSource, lang).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = Color(0xFFF59E0B)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(if (darkMode) Color(0xFF1E293B) else Color.White).fillMaxWidth(0.8f)
                ) {
                    botanicalOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(Translations.get(option, lang).uppercase(), fontWeight = FontWeight.Bold) },
                            onClick = {
                                botanicalSource = option
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = if (darkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }
        }

        item {
            JenuTextField(
                value = batchId,
                onValueChange = { batchId = it },
                label = Translations.get("batchId", lang),
                darkMode = darkMode
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (location.isNotEmpty() && quantity.isNotEmpty()) {
                        isSaving = true
                        scope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val data = hashMapOf(
                                    "location" to location,
                                    "quantity" to quantity.toDoubleOrNull(),
                                    "moisture" to moisture.toDoubleOrNull(),
                                    "botanicalSource" to botanicalSource,
                                    "batchId" to batchId,
                                    "userId" to userId,
                                    "createdAt" to com.google.firebase.Timestamp.now(),
                                    "grade" to if ((moisture.toDoubleOrNull() ?: 25.0) < 20.0) "Premium" else "A"
                                )
                                db.collection("harvest_records").add(data).await()
                                onBack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkMode) Color(0xFF1E293B) else Color.White,
                    disabledContainerColor = (if (darkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Text(
                    if (isSaving) "..." else Translations.get("save", lang).uppercase(),
                    fontWeight = FontWeight.Black,
                    color = if (darkMode) Color.White else Color(0xFF0F172A),
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun EditRecordForm(lang: String, userId: String, record: HarvestRecord, darkMode: Boolean, onBack: () -> Unit) {
    var location by remember { mutableStateOf(record.location) }
    var quantity by remember { mutableStateOf(record.quantity.toString()) }
    var moisture by remember { mutableStateOf(record.moisture?.toString() ?: "") }
    var batchId by remember { mutableStateOf(record.batchId ?: "") }
    var botanicalSource by remember { mutableStateOf(record.botanicalSource ?: "wildflower") }
    val botanicalOptions = listOf("wildflower", "coffeeBlossom", "forest", "rubber")
    var expanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkMode) Color(0xFF1E293B) else Color.White)
                        .border(
                            1.dp,
                            if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    Translations.get("edit", lang).uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = if (darkMode) Color.White else Color(0xFF0F172A),
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            JenuTextField(
                value = location,
                onValueChange = { location = it },
                label = Translations.get("location", lang),
                darkMode = darkMode
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    JenuTextField(
                        value = Translations.formatNumber(quantity, lang),
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it in '೦'..'೯' || it == '.' }
                                .map { char ->
                                    if (char in '೦'..'೯') (char.code - '೦'.code + '0'.code).toChar() else char
                                }.joinToString("")
                            quantity = clean
                        },
                        label = Translations.get("quantity", lang),
                        darkMode = darkMode
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    JenuTextField(
                        value = Translations.formatNumber(moisture, lang),
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it in '೦'..'೯' || it == '.' }
                                .map { char ->
                                    if (char in '೦'..'೯') (char.code - '೦'.code + '0'.code).toChar() else char
                                }.joinToString("")
                            moisture = clean
                        },
                        label = Translations.get("moistureLevel", lang),
                        darkMode = darkMode
                    )
                }
            }
        }

        item {
            Text(
                Translations.get("botanicalSource", lang).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF8FAFC))
                    .border(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                    .clickable { expanded = true }
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        Translations.get(botanicalSource, lang).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (darkMode) Color.White else Color(0xFF0F172A)
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = Color(0xFFF59E0B)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(if (darkMode) Color(0xFF1E293B) else Color.White).fillMaxWidth(0.8f)
                ) {
                    botanicalOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(Translations.get(option, lang).uppercase(), fontWeight = FontWeight.Bold) },
                            onClick = {
                                botanicalSource = option
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = if (darkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }
        }

        item {
            JenuTextField(
                value = batchId,
                onValueChange = { batchId = it },
                label = Translations.get("batchId", lang),
                darkMode = darkMode
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (location.isNotEmpty() && quantity.isNotEmpty()) {
                        isSaving = true
                        scope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val data = hashMapOf(
                                    "location" to location,
                                    "quantity" to quantity.toDoubleOrNull(),
                                    "moisture" to moisture.toDoubleOrNull(),
                                    "botanicalSource" to botanicalSource,
                                    "batchId" to batchId,
                                    "grade" to if ((moisture.toDoubleOrNull() ?: 25.0) < 20.0) "Premium" else "A"
                                )
                                db.collection("harvest_records").document(record.id).update(data as Map<String, Any>).await()
                                onBack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkMode) Color(0xFF1E293B) else Color.White,
                    disabledContainerColor = (if (darkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            ) {
                Text(
                    if (isSaving) "..." else Translations.get("save", lang).uppercase(),
                    fontWeight = FontWeight.Black,
                    color = if (darkMode) Color.White else Color(0xFF0F172A),
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun JenuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    darkMode: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color.Gray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFF59E0B),
                unfocusedBorderColor = if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                focusedTextColor = if (darkMode) Color.White else Color(0xFF0F172A),
                unfocusedTextColor = if (darkMode) Color.White else Color(0xFF0F172A),
                cursorColor = Color(0xFFF59E0B),
                focusedContainerColor = if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF8FAFC),
                unfocusedContainerColor = if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF8FAFC)
            )
        )
    }
}

data class HarvestRecord(
    val id: String = "",
    val location: String = "",
    val quantity: Double = 0.0,
    val moisture: Double? = null,
    val botanicalSource: String? = null,
    val batchId: String? = null,
    val createdAt: com.google.firebase.Timestamp? = null,
    val grade: String = "A"
)

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val displayNameKn: String? = null,
    val role: String = "hunter",
    val isVerifiedFarmer: Boolean = false
)

@Composable
fun JenuGumpuTheme(darkMode: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkMode) {
        darkColorScheme(
            primary = Color(0xFFF59E0B),
            onPrimary = Color(0xFF0F172A),
            background = Color(0xFF09090B),
            surface = Color(0xFF1E293B),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFD97706),
            onPrimary = Color.White,
            background = Color(0xFFF8FAFC),
            surface = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
