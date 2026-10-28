package com.kmb.downloader.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
fun ReferralDialog(
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val proState by ReferralManager.proState.collectAsState()
    var friendCodeInput by remember { mutableStateOf("") }
    var copiedRecently by remember { mutableStateOf(false) }

    val goldGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFFD700), Color(0xFFFF9800), Color(0xFFFF5722))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "👑",
                    fontSize = 26.sp
                )
                Column {
                    Text(
                        text = "KMB Pro VIP & Referrals",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Invite friends to get 1 Month Free VIP!",
                        fontSize = 12.sp,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // VIP Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (proState.isPro) Color(0xFF2E2305) else Color(0xFF1E222B)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (proState.isPro) goldGradient else Brush.linearGradient(listOf(Color(0xFF4B5563), Color(0xFF374151)))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (proState.isPro) "👑 KMB PRO VIP ACTIVE" else "⭐ Standard Edition",
                                fontWeight = FontWeight.Bold,
                                color = if (proState.isPro) Color(0xFFFFD700) else Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                            if (proState.isPro) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${proState.daysRemaining}d left",
                                        color = Color(0xFFFFD700),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (proState.isPro) {
                                "VIP benefits active until ${proState.expiryFormatted}: Turbo speeds, ad-free, unlimited trimmer & ringtone maker!"
                            } else {
                                "Refer 10 friends to unlock 1 Month Full Pro VIP with Turbo speeds, ad-free & full studio tools."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                }

                // Your Invite Code Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131722))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "YOUR VIP INVITE CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF),
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = proState.myCode,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD700),
                            letterSpacing = 2.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("KMB Referral Code", proState.myCode))
                                    copiedRecently = true
                                    onShowMessage("Copied invite code: ${proState.myCode}")
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
                                Text(if (copiedRecently) "Copied!" else "Copy", color = Color.White, fontSize = 12.sp)
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
                                Text("Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Milestone Progress
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Milestones (${proState.referralCount}/10 Friends)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${minOf(proState.referralCount * 10, 100)}%",
                            fontSize = 12.sp,
                            color = Color(0xFFFFD700)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (proState.referralCount.toFloat() / 10f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFFD700),
                        trackColor = Color(0xFF374151)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MilestoneBadge("1: +1d", achieved = proState.referralCount >= 1)
                        MilestoneBadge("3: +7d", achieved = proState.referralCount >= 3)
                        MilestoneBadge("5: +15d", achieved = proState.referralCount >= 5)
                        MilestoneBadge("10: 1 Mo Free 👑", achieved = proState.referralCount >= 10)
                    }
                }

                // Enter Friend Code
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Have a Friend's Invite Code?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD1D5DB)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                        onShowMessage(msg)
                                        friendCodeInput = ""
                                    },
                                    onFailure = { err ->
                                        onShowMessage(err.message ?: "Redemption error")
                                    }
                                )
                            },
                            enabled = !proState.hasRedeemedFriendCode && friendCodeInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text(if (proState.hasRedeemedFriendCode) "Redeemed" else "Redeem", fontSize = 12.sp)
                        }
                    }

                    if (proState.hasRedeemedFriendCode) {
                        Text(
                            text = "✓ You already redeemed a friend code and received your bonus!",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Test / Simulation button for user verification
                OutlinedButton(
                    onClick = {
                        val msg = ReferralManager.recordReferral()
                        onShowMessage(msg)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "🧪 Simulate 1 Friend Referral (Test Milestone)",
                        fontSize = 11.sp,
                        color = Color(0xFF93C5FD)
                    )
                }
            }
        },
        containerColor = Color(0xFF1B1F2A)
    )
}

@Composable
private fun MilestoneBadge(label: String, achieved: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (achieved) Color(0xFFFFD700).copy(alpha = 0.2f) else Color(0xFF262C3A),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (achieved) Color(0xFFFFD700) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (achieved) FontWeight.Bold else FontWeight.Normal,
            color = if (achieved) Color(0xFFFFD700) else Color(0xFF9CA3AF)
        )
    }
}
