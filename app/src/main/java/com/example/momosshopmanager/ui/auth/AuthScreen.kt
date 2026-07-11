package com.example.momosshopmanager.ui.auth

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momosshopmanager.data.*
import com.example.momosshopmanager.theme.*
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(viewModel: SalesViewModel) {
    val context = LocalContext.current
    val syncingState by viewModel.syncingState.collectAsState()
    
    // Form fields state
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.OWNER) }
    var syncCode by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var ownerPasswordInput by remember { mutableStateOf("") }
    
    val databaseUrlState by viewModel.databaseUrl.collectAsState()
    var databaseUrl by remember { mutableStateOf(databaseUrlState) }
    
    var userNameTouched by remember { mutableStateOf(false) }
    var ownerPasswordInputTouched by remember { mutableStateOf(false) }
    var isOwnerPasswordRegistered by remember { mutableStateOf(false) }
    
    LaunchedEffect(syncCode) {
        if (syncCode.isNotBlank()) {
            try {
                val pwd = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    SyncManager.getOwnerPassword(syncCode)
                }
                isOwnerPasswordRegistered = pwd != null
            } catch (e: Exception) {
                isOwnerPasswordRegistered = false
            }
        }
    }
    
    LaunchedEffect(databaseUrlState) {
        databaseUrl = databaseUrlState
    }
    
    // OTP State
    var simulatedOtp by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(0) }
    
    // Visual error/loading states
    var phoneTouched by remember { mutableStateOf(false) }
    var syncCodeTouched by remember { mutableStateOf(false) }
    var databaseUrlTouched by remember { mutableStateOf(false) }
    var pinTouched by remember { mutableStateOf(false) }
    var registrationError by remember { mutableStateOf<String?>(null) }
    var localLoading by remember { mutableStateOf(false) }
    
    // OTP verification error
    var otpError by remember { mutableStateOf<String?>(null) }
    
    // Validation flags
    val isPhoneValid = phone.length == 10 && phone.all { it.isDigit() }
    val isSyncCodeValid = syncCode.isNotBlank()
    val isPinValid = pin.length == 4 && pin.all { it.isDigit() }
    val isDatabaseUrlValid = databaseUrl.trim().startsWith("https://") && databaseUrl.trim().length > 12
    val isUserNameValid = if (role == UserRole.EMPLOYEE) userName.isNotBlank() else true
    val isOwnerPasswordValid = if (role == UserRole.OWNER) ownerPasswordInput.isNotBlank() else true
    val isFormValid = isPhoneValid && isSyncCodeValid && isPinValid && isDatabaseUrlValid && isUserNameValid && isOwnerPasswordValid

    // Timer effect
    LaunchedEffect(isOtpSent, timerSeconds) {
        if (isOtpSent && timerSeconds > 0) {
            delay(1000L)
            timerSeconds -= 1
        }
    }
    
    val scrollState = rememberScrollState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Emoji Logo & App Title
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MomosOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🥟",
                    fontSize = 44.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Manjar",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Secure store device configuration",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedContent(
                targetState = isOtpSent,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState) width else -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (targetState) -width else width } + fadeOut()
                },
                label = "authFormTransition"
            ) { otpSent ->
                if (!otpSent) {
                    // --- Registration Form ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Device Registration",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MomosOrange
                            )
                            
                            // Shop Sync Code Field
                            Column {
                                Text(
                                    text = "Shop Sync Code",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = syncCode,
                                    onValueChange = { 
                                        syncCode = it
                                        syncCodeTouched = true
                                    },
                                    placeholder = { Text("e.g. MOMO-DELHI-10", color = TextMuted) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Sync,
                                            contentDescription = "Sync Icon",
                                            tint = if (isSyncCodeValid) MomosOrange else TextMuted
                                        )
                                    },
                                    isError = syncCodeTouched && !isSyncCodeValid,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MomosOrange,
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface
                                    )
                                )
                                if (syncCodeTouched && !isSyncCodeValid) {
                                    Text(
                                        text = "Sync code is required",
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            
                            // Phone Number Field
                            Column {
                                Text(
                                    text = "Phone Number",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { input ->
                                        if (input.length <= 10 && input.all { it.isDigit() }) {
                                            phone = input
                                            phoneTouched = true
                                        }
                                    },
                                    placeholder = { Text("10-digit mobile number", color = TextMuted) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Phone,
                                            contentDescription = "Phone Icon",
                                            tint = if (isPhoneValid) MomosOrange else TextMuted
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    isError = phoneTouched && !isPhoneValid,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MomosOrange,
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface
                                    )
                                )
                                if (phoneTouched && !isPhoneValid) {
                                    Text(
                                        text = "Enter a valid 10-digit phone number",
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // Employee Name Field
                            if (role == UserRole.EMPLOYEE) {
                                Column {
                                    Text(
                                        text = "Full Name",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = userName,
                                        onValueChange = { 
                                            userName = it
                                            userNameTouched = true
                                        },
                                        placeholder = { Text("e.g. Rahul Sharma", color = TextMuted) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = "Person Icon",
                                                tint = if (userName.isNotBlank()) MomosOrange else TextMuted
                                            )
                                        },
                                        isError = userNameTouched && userName.isBlank(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MomosOrange,
                                            unfocusedBorderColor = DarkSurfaceVariant,
                                            focusedContainerColor = DarkSurface,
                                            unfocusedContainerColor = DarkSurface
                                        )
                                    )
                                    if (userNameTouched && userName.isBlank()) {
                                        Text(
                                            text = "Employee Name is required",
                                            color = ErrorRed,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Owner Gated Setup Password Field
                            if (role == UserRole.OWNER) {
                                Column {
                                    val pwdLabel = if (isOwnerPasswordRegistered) "Verify Owner Password" else "Create Owner Setup Password"
                                    val pwdPlaceholder = if (isOwnerPasswordRegistered) "Enter shop setup password" else "Set new shop setup password"
                                    Text(
                                        text = pwdLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = ownerPasswordInput,
                                        onValueChange = { 
                                            ownerPasswordInput = it
                                            ownerPasswordInputTouched = true
                                        },
                                        placeholder = { Text(pwdPlaceholder, color = TextMuted) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.VpnKey,
                                                contentDescription = "Key Icon",
                                                tint = if (ownerPasswordInput.isNotBlank()) MomosOrange else TextMuted
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        visualTransformation = PasswordVisualTransformation(),
                                        isError = ownerPasswordInputTouched && ownerPasswordInput.isBlank(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MomosOrange,
                                            unfocusedBorderColor = DarkSurfaceVariant,
                                            focusedContainerColor = DarkSurface,
                                            unfocusedContainerColor = DarkSurface
                                        )
                                    )
                                    if (ownerPasswordInputTouched && ownerPasswordInput.isBlank()) {
                                        Text(
                                            text = "Password is required",
                                            color = ErrorRed,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Role Selection
                            Column {
                                Text(
                                    text = "Select Device Role",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                RoleSelector(
                                    selectedRole = role,
                                    onRoleSelected = { role = it }
                                )
                            }
                            
                            // Daily Login PIN Field
                            Column {
                                Text(
                                    text = "4-Digit Daily Login PIN",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = { input ->
                                        if (input.length <= 4 && input.all { it.isDigit() }) {
                                            pin = input
                                            pinTouched = true
                                        }
                                    },
                                    placeholder = { Text("4-digit PIN", color = TextMuted) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "PIN Icon",
                                            tint = if (isPinValid) MomosOrange else TextMuted
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    visualTransformation = PasswordVisualTransformation(),
                                    isError = pinTouched && !isPinValid,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MomosOrange,
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface
                                    )
                                )
                                if (pinTouched && !isPinValid) {
                                    Text(
                                        text = "PIN must be exactly 4 digits",
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // Security protocol banner at the bottom
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MomosOrange.copy(alpha = 0.05f))
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = "Shield",
                                    tint = MomosOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🔒 Gated Security Setup Protocol",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MomosOrangeLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            if (registrationError != null) {
                                Text(
                                    text = registrationError ?: "",
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    phoneTouched = true
                                    syncCodeTouched = true
                                    pinTouched = true
                                    if (isFormValid) {
                                        val code = (1000..9999).random().toString()
                                        simulatedOtp = code
                                        isOtpSent = true
                                        timerSeconds = 30
                                        otpCode = ""
                                        otpError = null
                                        registrationError = null
                                        
                                        // Send OTP via SMS
                                        sendOtpSms(context, phone, code)
                                    }
                                },
                                enabled = isFormValid,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MomosOrange,
                                    contentColor = DarkBackground,
                                    disabledContainerColor = DarkSurfaceVariant,
                                    disabledContentColor = TextMuted
                                )
                            ) {
                                Text(
                                    text = "Request OTP / Register",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // --- OTP Verification Screen Section ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        isOtpSent = false
                                        otpCode = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MomosOrange
                                    )
                                }
                                Text(
                                    text = "Verify OTP",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MomosOrange,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            
                            Text(
                                text = "We opened your SMS app to send the verification code to +91 $phone. Send the SMS, then enter the 4-digit code here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Custom OTP 4-digit layout
                            BasicTextField(
                                value = otpCode,
                                onValueChange = { input ->
                                    if (input.length <= 4 && input.all { it.isDigit() }) {
                                        otpCode = input
                                        otpError = null
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                decorationBox = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        (0..3).forEach { index ->
                                            val char = otpCode.getOrNull(index)?.toString() ?: ""
                                            val isFocused = otpCode.length == index
                                            
                                            val borderBrush = if (isFocused) {
                                                Brush.linearGradient(listOf(MomosOrange, MomosOrangeLight))
                                            } else {
                                                SolidColor(DarkSurfaceVariant)
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .border(
                                                        width = if (isFocused) 2.dp else 1.dp,
                                                        brush = borderBrush,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .background(DarkSurface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = char,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                            
                            if (otpError != null) {
                                Text(
                                    text = otpError ?: "",
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Resend timer text
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (timerSeconds > 0) {
                                    Text(
                                        text = "Resend OTP in 00:${timerSeconds.toString().padStart(2, '0')}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                } else {
                                    Text(
                                        text = "Didn't receive code? ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Resend OTP",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MomosOrange,
                                        modifier = Modifier.clickable {
                                            val code = (1000..9999).random().toString()
                                            simulatedOtp = code
                                            timerSeconds = 30
                                            otpCode = ""
                                            otpError = null
                                            
                                            // Send OTP via SMS
                                            sendOtpSms(context, phone, code)
                                        }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Verify Button
                            Button(
                                onClick = {
                                    if (otpCode == simulatedOtp) {
                                        localLoading = true
                                        viewModel.registerDevice(databaseUrl.trim(), syncCode, phone, role, pin, userName, ownerPasswordInput) { success, msg ->
                                            localLoading = false
                                            if (success) {
                                                Toast.makeText(context, "Registration Successful!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                otpError = msg
                                            }
                                        }
                                    } else {
                                        otpError = "Incorrect OTP. Please check the simulated banner above."
                                    }
                                },
                                enabled = otpCode.length == 4 && !syncingState && !localLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MomosOrange,
                                    contentColor = DarkBackground,
                                    disabledContainerColor = DarkSurfaceVariant,
                                    disabledContentColor = TextMuted
                                )
                            ) {
                                if (syncingState || localLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = DarkBackground,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text(
                                        text = "Verify & Register",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(120.dp)) // space for scrolling
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun RoleSelector(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val roles = UserRole.entries
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        roles.forEach { role ->
            val isSelected = role == selectedRole
            
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MomosOrange else Color.Transparent,
                animationSpec = tween(durationMillis = 300),
                label = "roleBgColor"
            )
            
            val textColor by animateColorAsState(
                targetValue = if (isSelected) DarkBackground else TextSecondary,
                animationSpec = tween(durationMillis = 300),
                label = "roleTextColor"
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .clickable { onRoleSelected(role) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = role.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

private fun sendOtpSms(context: android.content.Context, phone: String, code: String) {
    val permission = android.Manifest.permission.SEND_SMS
    val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    
    // Always show a Toast with the code so the user is never stuck if carrier fails!
    Toast.makeText(context, "OTP Verification Code: $code", Toast.LENGTH_LONG).show()
    
    if (hasPermission) {
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage("+91$phone", null, "Your Manjar verification OTP is: $code", null, null)
        } catch (e: Exception) {
            launchSmsAppFallback(context, phone, code, "Background send failed: ${e.message}")
        }
    } else {
        (context as? Activity)?.let { activity ->
            ActivityCompat.requestPermissions(activity, arrayOf(permission), 101)
        }
        launchSmsAppFallback(context, phone, code, "SMS permission requested")
    }
}

private fun launchSmsAppFallback(context: android.content.Context, phone: String, code: String, reason: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:+91$phone")
            putExtra("sms_body", "Your Manjar verification OTP is: $code")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
