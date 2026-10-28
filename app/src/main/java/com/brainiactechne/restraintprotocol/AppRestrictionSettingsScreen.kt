package com.brainiactechne.restraintprotocol

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brainiactechne.restraintprotocol.ui.theme.Amber
import com.brainiactechne.restraintprotocol.ui.theme.Cream
import com.brainiactechne.restraintprotocol.ui.theme.Ink
import com.brainiactechne.restraintprotocol.ui.theme.Muted
import com.brainiactechne.restraintprotocol.ui.theme.Sage
import com.brainiactechne.restraintprotocol.ui.theme.SurfaceDark

private data class LaunchableApp(val packageName: String, val label: String)

@Composable
fun AppRestrictionSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("rp_settings", Context.MODE_PRIVATE)
    }
    val apps = remember(context) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) null
                else LaunchableApp(packageName, info.loadLabel(context.packageManager).toString())
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
    var selectedPackages by remember {
        mutableStateOf(preferences.getStringSet("blocked_packages", emptySet()).orEmpty().toSet())
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("App protection", color = Cream, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Text(
            "Select apps RP should interrupt while a restraint timer is active. This only watches which app comes to the foreground; it does not inspect messages, images, or videos.",
            color = Muted, lineHeight = 22.sp
        )
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Required Android permission", color = Amber, fontWeight = FontWeight.Bold)
                Text("You must manually enable Restraint Protocol under Android Accessibility settings. RP does not enable this permission by itself.", color = Muted, lineHeight = 21.sp)
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink)
                ) { Text("Open Accessibility settings") }
            }
        }
        Text("PROTECTED APPS · ${selectedPackages.size} SELECTED", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        if (apps.isEmpty()) Text("No launchable apps were returned by Android on this device.", color = Muted)
        apps.forEach { app ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = app.packageName in selectedPackages,
                    onCheckedChange = { checked ->
                        val next = if (checked) selectedPackages + app.packageName else selectedPackages - app.packageName
                        selectedPackages = next
                        preferences.edit().putStringSet("blocked_packages", next).apply()
                    }
                )
                Column(Modifier.weight(1f)) {
                    Text(app.label, color = Cream, fontWeight = FontWeight.SemiBold)
                    Text(app.packageName, color = Muted, fontSize = 11.sp)
                }
            }
        }
        Text("Safety: keep Phone, emergency calling, and other essential apps unselected. This opt-in mechanism can be disabled in Android settings and is not an unbreakable lock.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Sage) }
    }
}
