package com.csm.kitchenguard.presentation.screens.auth

import android.annotation.SuppressLint
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csm.kitchenguard.presentation.theme.BackgroundLight
import com.csm.kitchenguard.presentation.theme.InterFontFamily
import com.csm.kitchenguard.presentation.theme.JetBrainsMonoFamily
import com.csm.kitchenguard.presentation.theme.KitchenGuardTheme
import com.csm.kitchenguard.presentation.theme.OutlineBorder
import com.csm.kitchenguard.presentation.theme.PrimaryOrange
import com.csm.kitchenguard.presentation.theme.StatusSuccess
import com.csm.kitchenguard.presentation.theme.StrongOrange
import com.csm.kitchenguard.presentation.theme.SurfaceWhite
import com.csm.kitchenguard.presentation.theme.TextPrimary
import com.csm.kitchenguard.presentation.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens khusus LoginScreen (Figma M01)
// ─────────────────────────────────────────────────────────────────────────────
private val BadgeBgOrange    = Color(0xFFFFEDD5)  // latar badge pill
private val BadgeTextOrange  = Color(0xFFEA580C)  // teks badge pill
private val OfflineBgGreen   = Color(0xFFDCFCE7)  // latar banner offline
private val OfflineTextGreen = Color(0xFF15803D)  // teks banner offline
private val OfflineIconGreen = Color(0xFF16A34A)  // icon wifi-off

// ─────────────────────────────────────────────────────────────────────────────
// LoginScreen — Entry point composable, dapat menerima AuthViewModel via DI
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Layar Login KitchenGuard CSM (Figma M01).
 *
 * @param onLoginSuccess  Dipanggil saat autentikasi berhasil; NavGraph akan
 *                        pop Login lalu push KitchenStationHubScreen.
 * @param viewModel       AuthViewModel yang harus selalu di-inject dari Navigation.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: AuthViewModel
) {
    // Observasi state dari ViewModel
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    
    var identifier      by rememberSaveable { mutableStateOf("") }
    var password        by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe      by rememberSaveable { mutableStateOf(false) }

    // Navigasi saat login berhasil
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess()
    }

    // Tampilkan error via Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissError()
        }
    }

    // ── Root layout ─────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. HEADER ──────────────────────────────────────────────────
            LoginHeader()

            Spacer(modifier = Modifier.height(32.dp))

            // ── 2. FORM CARD ───────────────────────────────────────────────
            LoginFormCard(
                identifier         = identifier,
                onIdentifierChange = { identifier = it },
                password           = password,
                onPasswordChange   = { password = it },
                passwordVisible    = passwordVisible,
                onTogglePassword   = { passwordVisible = !passwordVisible },
                rememberMe         = rememberMe,
                onRememberMeChange = { rememberMe = it },
                isLoading          = uiState.isLoading,
                onForgotPassword   = onForgotPassword,
                onLoginClick       = {
                    // Validasi input tidak boleh kosong
                    if (identifier.trim().isNotEmpty() && password.isNotEmpty()) {
                        viewModel.login(identifier.trim(), password)
                    } else {
                        // Tampilkan error jika input kosong
                        viewModel.setError("Username/email dan password harus diisi")
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 3. BANNER OFFLINE RESILIENCE ───────────────────────────────
            OfflineResilienceBanner()

            Spacer(modifier = Modifier.height(28.dp))

            // ── 4. FOOTER ──────────────────────────────────────────────────
            LoginFooter()
        }

        // Snackbar error
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 1: Header — Logo + Judul + Badge Pill + Subtitle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kartu logo putih dengan piring hijau dan emoji koki
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(
                    elevation      = 8.dp,
                    shape          = RoundedCornerShape(20.dp),
                    ambientColor   = Color.Black.copy(alpha = 0.08f),
                    spotColor      = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF66BB6A), Color(0xFF388E3C)),
                            radius = 120f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👨‍🍳", fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Judul aplikasi
        Text(
            text       = "KitchenGuard CSM",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 26.sp,
            color      = TextPrimary,
            textAlign  = TextAlign.Center
        )

        // Badge pill oranye — KITCHEN WASTE & STOCK CONTROL
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = BadgeBgOrange
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "🍴", fontSize = 11.sp)
                Text(
                    text          = "KITCHEN WASTE & STOCK CONTROL",
                    fontFamily    = InterFontFamily,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 11.sp,
                    color         = BadgeTextOrange,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Subtitle
        Text(
            text       = "Masuk untuk melanjutkan shift dan pencatatan waste.",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = 14.sp,
            color      = TextSecondary,
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 2: Form Card Login
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginFormCard(
    identifier        : String,
    onIdentifierChange: (String) -> Unit,
    password          : String,
    onPasswordChange  : (String) -> Unit,
    passwordVisible   : Boolean,
    onTogglePassword  : () -> Unit,
    rememberMe        : Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    isLoading         : Boolean,
    onForgotPassword  : () -> Unit,
    onLoginClick      : () -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor         = PrimaryOrange,
        unfocusedBorderColor       = OutlineBorder,
        focusedLabelColor          = PrimaryOrange,
        cursorColor                = PrimaryOrange,
        focusedLeadingIconColor    = PrimaryOrange,
        unfocusedLeadingIconColor  = TextSecondary,
        focusedTrailingIconColor   = PrimaryOrange,
        unfocusedTrailingIconColor = TextSecondary
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = OutlineBorder, shape = RoundedCornerShape(16.dp)),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // ── Field: Email / ID Karyawan ───────────────────────────────
            Text(
                text       = "Email / ID Karyawan",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 13.sp,
                color      = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value         = identifier,
                onValueChange = onIdentifierChange,
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                placeholder   = {
                    Text(
                        text     = "Email atau ID karyawan",
                        color    = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector        = Icons.Default.Badge,
                        contentDescription = "Ikon ID Karyawan"
                    )
                },
                shape  = RoundedCornerShape(10.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Field: Password ──────────────────────────────────────────
            Text(
                text       = "Password",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 13.sp,
                color      = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value                = password,
                onValueChange        = onPasswordChange,
                modifier             = Modifier.fillMaxWidth(),
                singleLine           = true,
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                placeholder = {
                    Text(
                        text     = "••••••••",
                        color    = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = "Ikon Password"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible)
                                "Sembunyikan password" else "Tampilkan password"
                        )
                    }
                },
                shape  = RoundedCornerShape(10.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Baris: Ingat Saya + Lupa Kata Sandi ─────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.clickable { onRememberMeChange(!rememberMe) }
                ) {
                    Checkbox(
                        checked         = rememberMe,
                        onCheckedChange = onRememberMeChange,
                        colors          = CheckboxDefaults.colors(
                            checkedColor   = PrimaryOrange,
                            uncheckedColor = OutlineBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text       = "Ingat Saya",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 13.sp,
                        color      = TextPrimary
                    )
                }

                Text(
                    text       = "Lupa Kata Sandi?",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = StrongOrange,
                    modifier   = Modifier.clickable { onForgotPassword() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Tombol Masuk ─────────────────────────────────────────────
            Button(
                onClick  = onLoginClick,
                enabled  = !isLoading && identifier.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = PrimaryOrange,
                    contentColor           = Color.White,
                    disabledContainerColor = PrimaryOrange.copy(alpha = 0.5f),
                    disabledContentColor   = Color.White.copy(alpha = 0.7f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text          = "Masuk ke KitchenGuard",
                        fontFamily    = InterFontFamily,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 15.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 3: Banner Offline Resilience
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OfflineResilienceBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = StatusSuccess.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = OfflineBgGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ikon WiFi-Off dalam kotak hijau transparan
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OfflineIconGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.WifiOff,
                    contentDescription = "Mode Offline",
                    tint               = OfflineIconGreen,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text          = "● OFFLINE RESILIENCE",
                    fontFamily    = InterFontFamily,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 11.sp,
                    color         = OfflineTextGreen,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text       = "Data shift tetap dapat diakses secara offline setelah login pertama.",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 12.sp,
                    color      = OfflineTextGreen,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 4: Footer — Node info + Legal disclaimer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginFooter() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Lock,
                contentDescription = null,
                tint               = TextSecondary.copy(alpha = 0.5f),
                modifier           = Modifier.size(12.dp)
            )
            Text(
                text          = "NODE BALI-STN-04  •  KITCHEN OPS v2.4.9",
                fontFamily    = JetBrainsMonoFamily,
                fontWeight    = FontWeight.Normal,
                fontSize      = 10.sp,
                color         = TextSecondary.copy(alpha = 0.7f),
                letterSpacing = 0.4.sp
            )
        }
        Text(
            text       = "Aplikasi internal CSM Culinary Group. Akses terbatas untuk staf terverifikasi.",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = 11.sp,
            color      = TextSecondary.copy(alpha = 0.6f),
            textAlign  = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(
    name           = "LoginScreen — Figma M01",
    showBackground = true,
    backgroundColor = 0xFFF8FAFC,
    showSystemUi   = true,
    device         = "spec:width=390dp,height=844dp,dpi=420"
)
@SuppressLint("ViewModelConstructorInComposable")
@Composable
private fun LoginScreenPreview() {
    KitchenGuardTheme {
        LoginScreen(
            onLoginSuccess = {},
            viewModel = AuthViewModel(
                object : com.csm.kitchenguard.domain.repository.AuthRepository {
                    override fun login(
                        identifier: String,
                        password: String
                    ): kotlinx.coroutines.flow.Flow<com.csm.kitchenguard.utils.result.Resource<com.csm.kitchenguard.data.local.preferences.UserSessionModel>> =
                        kotlinx.coroutines.flow.flowOf(
                            com.csm.kitchenguard.utils.result.Resource.Loading()
                        )

                    override suspend fun logout() = Unit
                }
            )
        )
    }
}
