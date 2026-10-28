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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private enum class RpScreen { HOME, WARNING, RESTRICTION }

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
            selectedDuration = saved.selectedDurationMinutes
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
                onDurationSelected = {
                    selectedDuration = it
                    scope.launch { repository.saveCommitment(it, deadline) }
                },
                onPreviewWarning = {
                    screen = RpScreen.WARNING
                    scope.launch { repository.recordIntervention("warning_shown", selectedDuration) }
                }
            )
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
                    "Preview the Scripture warning and timed restriction flow. This demo only restricts navigation inside RP; it does not yet block other apps.",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Text("DEMO CONSEQUENCE", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30, 60).forEach { minutes ->
                        val selected = selectedDuration == minutes
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
                            Text(if (minutes >= 60) "1h" else "${minutes}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        Text("This is a demonstration warning. RP has not detected or analysed any content.", color = Muted, textAlign = TextAlign.Center, lineHeight = 22.sp)
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
            Text("Continue to demo restriction", color = Cream)
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
        Text("Use this time to reset. Your demo restriction ends automatically.", color = Muted, textAlign = TextAlign.Center, lineHeight = 22.sp)
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
        Text("Prototype limitation: this countdown only affects this screen. It does not lock Android or other applications.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}
