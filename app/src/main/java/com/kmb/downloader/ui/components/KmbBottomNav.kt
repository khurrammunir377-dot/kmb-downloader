package com.kmb.downloader.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.ui.theme.DarkBorder
import com.kmb.downloader.ui.theme.DarkSurface
import com.kmb.downloader.ui.theme.TextSecondary

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val activeColor: Color
)

@Composable
fun KmbBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavItem("downloader", "Downloader", Icons.Default.Download, Color(0xFF00E5FF)),
        NavItem("whatsapp", "WhatsApp", Icons.Default.Visibility, Color(0xFF25D366)),
        NavItem("ai_studio", "AI Studio", Icons.Default.AutoFixHigh, Color(0xFF00B0FF)),
        NavItem("vip_pro", "VIP Pro", Icons.Default.WorkspacePremium, Color(0xFFFFD700))
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF040D1A).copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = BorderStroke(
            1.dp,
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.35f),
                    Color(0xFF0091EA).copy(alpha = 0.15f),
                    Color(0xFF00E5FF).copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(item.route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (isSelected) item.activeColor.copy(alpha = 0.18f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) item.activeColor else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) item.activeColor else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
