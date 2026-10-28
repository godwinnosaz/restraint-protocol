package com.brainiactechne.restraintprotocol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brainiactechne.restraintprotocol.ui.theme.Amber
import com.brainiactechne.restraintprotocol.ui.theme.Cream
import com.brainiactechne.restraintprotocol.ui.theme.Ink
import com.brainiactechne.restraintprotocol.ui.theme.Muted
import com.brainiactechne.restraintprotocol.ui.theme.SurfaceDark
import kotlinx.coroutines.delay

class RestrictionGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deadline = intent.getLongExtra(
            EXTRA_DEADLINE,
            getSharedPreferences("rp_commitments", MODE_PRIVATE).getLong("restriction_deadline", 0L)
        )
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Amber, background = Ink, surface = SurfaceDark,
                onSurface = Cream, onBackground = Cream
            )) {
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(deadline) {
                    while (now < deadline) {
                        delay(1_000)
                        now = System.currentTimeMillis()
                    }
                    finish()
                }
                Column(
                    modifier = Modifier.fillMaxSize().background(Ink).padding(26.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Your commitment is still active", color = Cream, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("You chose to pause access to this app for a while. Take a breath and return when your timer ends.", color = Muted, textAlign = TextAlign.Center, lineHeight = 23.sp)
                    Spacer(Modifier.height(28.dp))
                    val seconds = ((deadline - now).coerceAtLeast(0L) / 1_000L)
                    Text("%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60), color = Amber, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(22.dp))
                    Text("“I will set no wicked thing before mine eyes.”", color = Cream, fontSize = 18.sp, textAlign = TextAlign.Center, lineHeight = 26.sp)
                    Text("Psalm 101:3 · KJV", color = Amber, fontSize = 13.sp)
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = {
                            startActivity(Intent(this@RestrictionGateActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)
                    ) { Text("Return to RP", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    Text("This is an opt-in accessibility-based interruption, not an unbreakable device lock. Emergency functions remain available.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                }
            }
        }
    }

    @Deprecated("Back navigation is intentionally ignored while the selected-app timer is active.")
    override fun onBackPressed() {
        // The accessibility service will re-open this gate if a protected app is brought forward.
    }

    companion object {
        const val EXTRA_DEADLINE = "restriction_deadline"
    }
}
