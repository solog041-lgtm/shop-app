package com.example.momosshopmanager.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momosshopmanager.data.SalesViewModel
import com.example.momosshopmanager.data.UserRole
import com.example.momosshopmanager.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(viewModel: SalesViewModel) {
    val userRole by viewModel.userRole.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    fun handleKeyPress(key: String) {
        if (isError) return

        when (key) {
            "C" -> {
                if (pinInput.isNotEmpty()) {
                    pinInput = ""
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            "⌫" -> {
                if (pinInput.isNotEmpty()) {
                    pinInput = pinInput.dropLast(1)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            else -> {
                if (pinInput.length < 4) {
                    pinInput += key
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    
                    if (pinInput.length == 4) {
                        val isCorrect = viewModel.verifyDailyPin(pinInput)
                        if (!isCorrect) {
                            scope.launch {
                                isError = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                
                                // Shake animation loop
                                shakeOffset.animateTo(15f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                                shakeOffset.animateTo(-15f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                                shakeOffset.animateTo(10f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                                shakeOffset.animateTo(-10f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                                shakeOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                                
                                delay(200)
                                pinInput = ""
                                isError = false
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MomosOrange.copy(alpha = 0.08f),
                        DarkBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top Section (Logo & Role Badge) ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(MomosOrange.copy(alpha = 0.15f), Color.Transparent),
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Lock Screen",
                            tint = MomosOrange,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "🥟 Manjar",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (userRole == UserRole.OWNER) {
                                MomosOrange.copy(alpha = 0.12f)
                            } else {
                                SuccessGreen.copy(alpha = 0.12f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (userRole == UserRole.OWNER) MomosOrange.copy(alpha = 0.3f) else SuccessGreen.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${userRole.displayName} Mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (userRole == UserRole.OWNER) MomosOrangeLight else SuccessGreenLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Middle Section (PIN Indicators & Custom Keypad) ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = "Enter your 4-digit Daily Login PIN to unlock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // PIN indicators with shake offset on failure
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pinInput.length
                        val color by animateColorAsState(
                            targetValue = when {
                                isError -> ErrorRed
                                isFilled -> MomosOrange
                                else -> Color.White.copy(alpha = 0.2f)
                            },
                            animationSpec = tween(150),
                            label = "indicatorColor"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isFilled) 1.25f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "indicatorScale"
                        )

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(if (isFilled || isError) color else Color.Transparent)
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled || isError) Color.Transparent else color,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Keypad grid
                Column(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )

                    keyRows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowKeys.forEach { key ->
                                DialerKey(
                                    text = key,
                                    icon = if (key == "⌫") Icons.Rounded.Backspace else null,
                                    onClick = { handleKeyPress(key) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Bottom Section (Switch Device / Log Out) ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp, top = 20.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ChiliRedLight
                    ),
                    border = BorderStroke(1.dp, ChiliRed.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Logout,
                        contentDescription = "Log Out",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out / Switch Device",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DialerKey(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "keyScale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isPressed) {
            MomosOrange.copy(alpha = 0.2f)
        } else {
            DarkCard.copy(alpha = 0.45f)
        },
        animationSpec = tween(100),
        label = "keyContainerColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPressed) {
            MomosOrange.copy(alpha = 0.45f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(100),
        label = "keyBorderColor"
    )

    Card(
        modifier = modifier
            .aspectRatio(1.25f)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (isPressed) MomosOrangeLight else TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = if (text == "C") {
                        if (isPressed) MomosOrangeLight else TextSecondary
                    } else {
                        if (isPressed) MomosOrangeLight else TextPrimary
                    }
                )
            }
        }
    }
}
