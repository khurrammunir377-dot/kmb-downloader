package com.kmb.downloader.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.kmb.downloader.data.ReferralManager
import com.kmb.downloader.data.VaultFileItem
import com.kmb.downloader.data.VaultManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipProScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val proState by ReferralManager.proState.collectAsState()
    var friendCodeInput by remember { mutableStateOf("") }
    var copiedRecently by remember { mutableStateOf(false) }

    // Secret Vault State
    var showVaultPinDialog by remember { mutableStateOf(false) }
    var showVaultBrowser by remember { mutableStateOf(false) }
    var vaultPinInput by remember { mutableStateOf("") }
    var vaultPinError by remember { mutableStateOf<String?>(null) }
    var isCreatingNewPin by remember { mutableStateOf(false) }
    var vaultFiles by remember { mutableStateOf<List<VaultFileItem>>(emptyList()) }

    fun refreshVault() {
        vaultFiles = VaultManager.getVaultFiles(context)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            var count = 0
            uris.forEach { u ->
                if (VaultManager.importUriToVault(context, u)) {
                    count++
                }
            }
            refreshVault()
            Toast.makeText(context, "$count file(s) moved to Secret Vault!", Toast.LENGTH_SHORT).show()
        }
    }

    val goldGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFFD700), Color(0xFFFF9800), Color(0xFFFF5722))
    )

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(goldGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "VIP Pro & Rewards",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Turbo Engine & Secret Vault",
                                fontSize = 11.sp,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isCreatingNewPin = !VaultManager.isPinSet(context)
                            vaultPinInput = ""
                            vaultPinError = null
                            showVaultPinDialog = true
                        }
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Secret Vault",
                            tint = Color(0xFF818CF8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
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

            // Private Secret Vault Hero Card (Fully Clickable)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isCreatingNewPin = !VaultManager.isPinSet(context)
                            vaultPinInput = ""
                            vaultPinError = null
                            showVaultPinDialog = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2C)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(24.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Private Secret Vault",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("OPEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "PIN-protected hidden storage. Keep confidential videos and photos invisible from phone Gallery.",
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

            item { Spacer(Modifier.height(6.dp)) }
        }
    }

    // Vault PIN Entry / Setup Dialog
    if (showVaultPinDialog) {
        AlertDialog(
            onDismissRequest = { showVaultPinDialog = false },
            icon = {
                Icon(
                    if (isCreatingNewPin) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isCreatingNewPin) "Set 4-Digit Secret PIN" else "Unlock Secret Vault",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isCreatingNewPin)
                            "Create a 4-digit PIN to secure your private vault."
                        else
                            "Enter your 4-digit PIN to access hidden files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = vaultPinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                vaultPinInput = it
                                vaultPinError = null
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        placeholder = { Text("••••") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = vaultPinError != null,
                        supportingText = vaultPinError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (vaultPinInput.length < 4) {
                            vaultPinError = "PIN must be at least 4 digits"
                            return@Button
                        }
                        if (isCreatingNewPin) {
                            VaultManager.setPin(context, vaultPinInput)
                            showVaultPinDialog = false
                            refreshVault()
                            showVaultBrowser = true
                            Toast.makeText(context, "PIN set successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            if (VaultManager.verifyPin(context, vaultPinInput)) {
                                showVaultPinDialog = false
                                refreshVault()
                                showVaultBrowser = true
                            } else {
                                vaultPinError = "Incorrect PIN. Try again."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isCreatingNewPin) "Save & Open" else "Unlock", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Secret Vault Fullscreen Browser Dialog
    if (showVaultBrowser) {
        Dialog(
            onDismissRequest = { showVaultBrowser = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = Color(0xFF0F121C),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Private Secret Vault", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                    Text("${vaultFiles.size} hidden items • Not in Gallery", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { showVaultBrowser = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                isCreatingNewPin = true
                                vaultPinInput = ""
                                vaultPinError = null
                                showVaultPinDialog = true
                            }) {
                                Icon(Icons.Default.Lock, contentDescription = "Change PIN", tint = Color(0xFF818CF8))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF131722))
                    )
                },
                floatingActionButton = {
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Hide Media Here", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            ) { padding ->
                if (vaultFiles.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Your Secret Vault is Empty", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Hide Media Here' below to hide sensitive videos, audios, or photos. They will be stored in an encrypted private directory invisible to Gallery and other apps.",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(vaultFiles, key = { it.file.absolutePath }) { item ->
                            VaultFileCard(
                                item = item,
                                onOpen = {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        item.file
                                    )
                                    val mimeType = if (item.isVideo) "video/*" else "image/*"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(intent, "View Hidden Media"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No app available to play this file", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onUnhide = {
                                    val ok = VaultManager.exportToGallery(context, item)
                                    if (ok) {
                                        refreshVault()
                                        Toast.makeText(context, "Restored to Gallery (Downloads/KMB Vault Restored)!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to restore file", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = {
                                    VaultManager.deleteFile(item)
                                    refreshVault()
                                    Toast.makeText(context, "Deleted from vault", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultFileCard(
    item: VaultFileItem,
    onOpen: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.lastModified) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(item.lastModified))
    }
    val sizeStr = remember(item.sizeBytes) {
        val mb = item.sizeBytes / (1024f * 1024f)
        "%.1f MB".format(mb)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.isVideo) Icons.Default.Videocam else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$sizeStr • $dateStr",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onUnhide, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Unarchive, contentDescription = "Restore to Gallery", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
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
