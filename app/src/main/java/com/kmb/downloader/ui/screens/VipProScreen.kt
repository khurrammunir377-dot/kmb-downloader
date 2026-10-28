package com.kmb.downloader.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.data.ReferralManager
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipProScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val proState by ReferralManager.proState.collectAsState()
    var friendCodeInput by remember { mutableStateOf("") }
    var copiedRecently by remember { mutableStateOf(false) }

    val goldGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFFD700), Color(0xFFFF9800), Color(0xFFFF5722))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👑",
                            fontSize = 22.sp
                        )
                        Text(
                            text = "KMB VIP Pro & Rewards",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // VIP Status Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (proState.isPro) Color(0xFF2E2305) else Color(0xFF1E222B)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (proState.isPro) goldGradient else Brush.linearGradient(listOf(Color(0xFF4B5563), Color(0xFF374151)))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = if (proState.isPro) Color(0xFFFFD700) else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (proState.isPro) "KMB PRO VIP ACTIVE" else "Standard Edition",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (proState.isPro) Color(0xFFFFD700) else Color(0xFFD1D5DB),
                                    fontSize = 16.sp
                                )
                            }

                            if (proState.isPro) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFD700).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${proState.daysRemaining} Days Left",
                                        color = Color(0xFFFFD700),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (proState.isPro) {
                                "VIP membership active until ${proState.expiryFormatted}. Enjoy unlimited Turbo download speeds, 100% ad-free experience, and full AI Studio tools."
                            } else {
                                "Invite 10 friends to get 1 Full Month (30 Days) of KMB Pro VIP Free! Unlock Turbo multi-thread engine and exclusive VIP badges."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                }
            }

            // Invite Code & Share Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131722))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "YOUR EXCLUSIVE REFERRAL CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF),
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = proState.myCode,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD700),
                            letterSpacing = 3.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("KMB Referral Code", proState.myCode))
                                    copiedRecently = true
                                    scope.launch { snackbarHostState.showSnackbar("Copied: ${proState.myCode}") }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    if (copiedRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (copiedRecently) "Copied!" else "Copy Code", color = Color.White, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, ReferralManager.getInviteMessage())
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share KMB Invite Code"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Invite", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Milestone Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Milestone Tracker (${proState.referralCount}/10 Friends)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${minOf(proState.referralCount * 10, 100)}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD700)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (proState.referralCount.toFloat() / 10f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color(0xFFFFD700),
                            trackColor = Color(0xFF374151)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MilestonePill("1 Friend: +1d", achieved = proState.referralCount >= 1)
                            MilestonePill("3 Friends: +7d", achieved = proState.referralCount >= 3)
                            MilestonePill("5 Friends: +15d", achieved = proState.referralCount >= 5)
                            MilestonePill("10: 1 Month Free 👑", achieved = proState.referralCount >= 10)
                        }
                    }
                }
            }

            // Enter Friend's Code Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Redeem Friend's Invite Code",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Enter a friend's code to instantly receive +3 Days of Free VIP Pro!",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = friendCodeInput,
                                onValueChange = { friendCodeInput = it.uppercase() },
                                placeholder = { Text("e.g. KMB-7A9B2", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                enabled = !proState.hasRedeemedFriendCode,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Button(
                                onClick = {
                                    val result = ReferralManager.redeemFriendCode(friendCodeInput)
                                    result.fold(
                                        onSuccess = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                            friendCodeInput = ""
                                        },
                                        onFailure = { err ->
                                            scope.launch { snackbarHostState.showSnackbar(err.message ?: "Redemption error") }
                                        }
                                    )
                                },
                                enabled = !proState.hasRedeemedFriendCode && friendCodeInput.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                            ) {
                                Text(if (proState.hasRedeemedFriendCode) "Redeemed" else "Redeem (+3d)", fontSize = 12.sp)
                            }
                        }

                        if (proState.hasRedeemedFriendCode) {
                            Text(
                                text = "✓ You already redeemed a friend's code and claimed your bonus!",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // Private Secret Vault Preview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2C))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF818CF8))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Private Secret Vault",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "PIN / Fingerprint protected hidden folder. Keeps sensitive downloads out of Gallery.",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }

            // Instant Test Simulation Button
            item {
                OutlinedButton(
                    onClick = {
                        val msg = ReferralManager.recordReferral()
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "🧪 Simulate 1 Friend Referral (Test Milestone Unlocks)",
                        fontSize = 12.sp,
                        color = Color(0xFF93C5FD)
                    )
                }
            }

            // Direct Feedback Support
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:mkmbapps@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "KMB Downloader Feedback")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                        .padding(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = KmbSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Feedback & Inquiries: mkmbapps@gmail.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = KmbSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun MilestonePill(label: String, achieved: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (achieved) Color(0xFFFFD700).copy(alpha = 0.25f) else Color(0xFF262C3A),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (achieved) Color(0xFFFFD700) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (achieved) FontWeight.Bold else FontWeight.Normal,
            color = if (achieved) Color(0xFFFFD700) else Color(0xFF9CA3AF)
        )
    }
}
