package com.brainiactechne.restraintprotocol

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brainiactechne.restraintprotocol.data.AuthApi
import com.brainiactechne.restraintprotocol.data.LocalFirstRepository
import com.brainiactechne.restraintprotocol.ui.theme.Amber
import com.brainiactechne.restraintprotocol.ui.theme.Cream
import com.brainiactechne.restraintprotocol.ui.theme.Ink
import com.brainiactechne.restraintprotocol.ui.theme.Muted
import com.brainiactechne.restraintprotocol.ui.theme.Rose
import com.brainiactechne.restraintprotocol.ui.theme.Sage
import com.brainiactechne.restraintprotocol.ui.theme.SurfaceDark
import com.brainiactechne.restraintprotocol.ui.theme.SurfaceRaised
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RpColors = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    background = Ink,
    onBackground = Cream,
    surface = SurfaceDark,
    onSurface = Cream,
    secondary = Sage,
    error = Rose
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = RpColors) {
                RestraintApp()
            }
        }
    }
}

private enum class RpScreen { HOME, ACCOUNT, APP_SETTINGS, WARNING, RESTRICTION }

@Composable
private fun RestraintApp() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("rp_commitments", Context.MODE_PRIVATE)
    }
    val repository = remember(context) { LocalFirstRepository(context) }
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var deadline by remember {
        mutableLongStateOf(preferences.getLong("restriction_deadline", 0L))
    }
    var screen by remember {
        mutableStateOf(
            if (deadline > now) RpScreen.RESTRICTION else RpScreen.HOME
        )
    }
    var selectedDuration by remember {
        mutableIntStateOf(
            preferences.getInt("selected_duration", 15)
                .takeIf { CommitmentPolicy.isAllowedDuration(it) } ?: 15
        )
    }

    LaunchedEffect(Unit) {
        val saved = repository.loadCommitment()
        if (saved != null) {
            selectedDuration = saved.selectedDurationMinutes.takeIf { CommitmentPolicy.isAllowedDuration(it) } ?: 15
            if (saved.activeDeadlineEpochMillis > System.currentTimeMillis()) {
                deadline = saved.activeDeadlineEpochMillis
                screen = RpScreen.RESTRICTION
            } else {
                deadline = 0L
                screen = RpScreen.HOME
            }
        }
        repository.scheduleSync()
    }

    LaunchedEffect(selectedDuration) {
        preferences.edit().putInt("selected_duration", selectedDuration).apply()
    }

    LaunchedEffect(screen, deadline) {
        if (screen == RpScreen.RESTRICTION && deadline > 0L) {
            preferences.edit().putLong("restriction_deadline", deadline).apply()
        } else if (screen == RpScreen.HOME) {
            preferences.edit().remove("restriction_deadline").apply()
        }
        while (screen == RpScreen.RESTRICTION && now < deadline) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
        if (screen == RpScreen.RESTRICTION && now >= deadline) {
            screen = RpScreen.HOME
            deadline = 0L
            scope.launch {
                repository.saveCommitment(selectedDuration, 0L)
                repository.recordIntervention("restriction_completed", selectedDuration)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        when (screen) {
            RpScreen.HOME -> HomeScreen(
                selectedDuration = selectedDuration,
                onAccountClick = { screen = RpScreen.ACCOUNT },
                onAppProtectionClick = { screen = RpScreen.APP_SETTINGS },
                onDurationSelected = {
                    selectedDuration = it
                    scope.launch { repository.saveCommitment(it, deadline) }
                },
                onPreviewWarning = {
                    screen = RpScreen.WARNING
                    scope.launch { repository.recordIntervention("warning_shown", selectedDuration) }
                }
            )
            RpScreen.ACCOUNT -> AccountScreen(onBack = { screen = RpScreen.HOME })
            RpScreen.APP_SETTINGS -> AppRestrictionSettingsScreen(onBack = { screen = RpScreen.HOME })
            RpScreen.WARNING -> WarningScreen(
                onReturnToSafety = {
                    screen = RpScreen.HOME
                    scope.launch { repository.recordIntervention("user_returned_to_safety", selectedDuration) }
                },
                onContinue = {
                    deadline = System.currentTimeMillis() +
                        CommitmentPolicy.durationMillis(selectedDuration)
                    now = System.currentTimeMillis()
                    screen = RpScreen.RESTRICTION
                    scope.launch {
                        repository.saveCommitment(selectedDuration, deadline)
                        repository.recordIntervention("restriction_started", selectedDuration)
                    }
                }
            )
            RpScreen.RESTRICTION -> RestrictionScreen(
                remainingMillis = CommitmentPolicy.remainingMillis(deadline, now)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    selectedDuration: Int,
    onAccountClick: () -> Unit,
    onAppProtectionClick: () -> Unit,
    onDurationSelected: (Int) -> Unit,
    onPreviewWarning: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(SurfaceRaised, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(27.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("RESTRAINT PROTOCOL", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text("Choose who you want to become.", color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onAccountClick, shape = RoundedCornerShape(12.dp)) {
                Text("Account", fontSize = 12.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("A stronger pause.", color = Cream, fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold)
            Text(
                "A private, practical tool to help you interrupt urges and stay true to your commitments.",
                color = Muted,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Outlined.Lock, contentDescription = null, tint = Amber)
                    Text("  Practice a restraint warning", color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
                Text(
                    "Preview the Scripture warning. If you enable App protection and select apps, an active timer also interrupts those apps.",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Text("RESTRICTION DURATION", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 60, 180, 720, 1440, 4320).chunked(3).forEach { durationRow ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            durationRow.forEach { minutes ->
                                val selected = selectedDuration == minutes
                                val label = when {
                                    minutes < 60 -> "${minutes}m"
                                    minutes % 1440 == 0 -> "${minutes / 1440}d"
                                    else -> "${minutes / 60}h"
                                }
                                OutlinedButton(
                                    onClick = { onDurationSelected(minutes) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) Amber else Color.Transparent,
                                        contentColor = if (selected) Ink else Cream
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, if (selected) Amber else SurfaceRaised
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = onPreviewWarning,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)
                ) {
                    Text("Preview intervention", fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("App protection", color = Cream, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose apps RP should interrupt while a commitment timer is active. Android requires you to enable the accessibility service yourself.",
                    color = Muted, fontSize = 13.sp, lineHeight = 20.sp
                )
                OutlinedButton(onClick = onAppProtectionClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Configure protected apps")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("BUILT AROUND YOUR RECOVERY", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            FeatureRow("Pause before acting", "Interrupt the moment with a clear next step.")
            FeatureRow("Encouragement, not shame", "Scripture and practical guidance for a reset.")
            FeatureRow("Privacy first", "This prototype does not inspect or upload your content.")
        }

        Text(
            "RP is a support tool, not a substitute for professional care when compulsive behavior is causing significant distress.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}


@Composable
private fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { LocalFirstRepository(context) }
    val authPreferences = remember(context) {
        context.getSharedPreferences("rp_auth", Context.MODE_PRIVATE)
    }
    var hasToken by remember { mutableStateOf(!authPreferences.getString("access_token", null).isNullOrBlank()) }
    var register by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Account & sync", color = Cream, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "RP works offline. Sign in only when you want your commitment metadata and intervention events synced to your account.",
            color = Muted, lineHeight = 22.sp
        )
        Spacer(Modifier.height(20.dp))
        if (hasToken) {
            Text("You are signed in on this device.", color = Sage, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Pending records stay on this device until the configured server acknowledges them.", color = Muted, lineHeight = 21.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    authPreferences.edit().remove("access_token").apply()
                    hasToken = false
                    message = "Signed out. Your local data and pending sync queue were kept."
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sign out") }
        } else {
            if (register) {
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (register && username.isBlank())) {
                        message = "Complete all required fields."
                    } else {
                        busy = true
                        message = ""
                        scope.launch {
                            try {
                                val token = AuthApi.authenticate(
                                    username.takeIf { register }, email.trim(), password, register
                                )
                                authPreferences.edit().putString("access_token", token).apply()
                                hasToken = true
                                repository.scheduleSync()
                                message = "Signed in. RP will sync queued records when a network is available."
                            } catch (error: Exception) {
                                message = error.message ?: "Unable to connect. Your local data is safe."
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)
            ) {
                Text(
                    if (busy) "Connecting…" else if (register) "Create account" else "Sign in",
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = { register = !register; message = "" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (register) "Already have an account? Sign in" else "Create a new account")
            }
        }
        if (message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = if (hasToken) Sage else Rose, lineHeight = 21.sp)
        }
        Spacer(Modifier.height(18.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text(
            "Server sync requires an HTTPS API URL configured at build time. Signing in is optional; core commitments and interventions remain available offline.",
            color = Muted, fontSize = 12.sp, lineHeight = 18.sp
        )
    }
}

@Composable
private fun FeatureRow(title: String, description: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.material3.Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Sage,
            modifier = Modifier.padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Cream, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun WarningScreen(onReturnToSafety: () -> Unit, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(76.dp).background(SurfaceRaised, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Amber, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("Pause. Remember your commitment.", color = Cream, fontSize = 27.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("You opened this warning manually. RP has not detected or analysed any content.", color = Muted, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Icon(Icons.Outlined.Book, contentDescription = null, tint = Amber, modifier = Modifier.size(28.dp))
                Text("“I will set no wicked thing before mine eyes.”", color = Cream, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
                Text("Psalm 101:3 · KJV", color = Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "You made this commitment for a reason. You do not have to act on every urge. Pause, step away, and choose what supports the person you want to become.",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onReturnToSafety,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)
        ) {
            Text("Return to safety", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text("Start restriction", color = Cream)
        }
    }
}

@Composable
private fun RestrictionScreen(remainingMillis: Long) {
    val totalSeconds = (remainingMillis / 1_000L).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    Column(
        modifier = Modifier.fillMaxSize().padding(26.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(82.dp).background(SurfaceRaised, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(Icons.Outlined.Lock, contentDescription = null, tint = Amber, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Your pause is in progress", color = Cream, fontSize = 27.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("Use this time to reset. Your selected-app restriction ends automatically.", color = Muted, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))
        Text(
            if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds),
            color = Amber,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("“God is faithful… he will also provide a way out.”", color = Cream, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
                Text("1 Corinthians 10:13 · selected excerpt", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Take a breath. Change your environment. Reach out to someone you trust if you need support.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("If App protection is enabled, RP interrupts the apps you selected until this timer expires. This is not a system-wide phone lock.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}
