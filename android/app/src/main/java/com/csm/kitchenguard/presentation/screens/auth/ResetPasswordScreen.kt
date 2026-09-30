package com.csm.kitchenguard.presentation.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
// Design tokens khusus ResetPasswordScreen (Figma M02)
// ─────────────────────────────────────────────────────────────────────────────
private val SuccessBgGreen      = Color(0xFFDCFCE7)   // latar card notifikasi sukses
private val SuccessTextGreen    = Color(0xFF15803D)    // teks sukses
private val SuccessIconGreen    = Color(0xFF16A34A)    // icon centang
private val BadgeBgOrangeLight  = Color(0xFFFFEDD5)    // latar badge CSM SYSTEM (TopBar)
private val VerifiedBgGreen     = Color(0xFFDCFCE7)    // latar chip VERIFIED
private val VerifiedTextGreen   = Color(0xFF15803D)    // teks VERIFIED
private val IconBgOrangeLight   = Color(0xFFFFEDD5)    // latar lingkaran icon reset
private val DividerTextColor    = Color(0xFF94A3B8)    // warna teks "ATAU"
private val FooterBg            = Color(0xFFF1F5F9)    // latar footer card

// ─────────────────────────────────────────────────────────────────────────────
// ResetPasswordScreen (Figma M02)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Layar Reset Kata Sandi KitchenGuard CSM (Figma M02).
 *
 * @param onNavigateBack  Dipanggil saat user menekan tombol back atau "Kembali ke Login".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onNavigateBack: () -> Unit = {}
) {
    var email        by rememberSaveable { mutableStateOf("") }
    var emailSent    by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── 1. TOP APP BAR ─────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    text       = "Reset Kata Sandi",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint               = Color.White
                    )
                }
            },
            actions = {
                // Badge "CSM SYSTEM" di kanan TopBar
                Surface(
                    shape  = RoundedCornerShape(50.dp),
                    color  = Color.White.copy(alpha = 0.20f),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(13.dp)
                        )
                        Text(
                            text          = "CSM SYSTEM",
                            fontFamily    = InterFontFamily,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 10.sp,
                            color         = Color.White,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PrimaryOrange
            )
        )

        // ── Konten utama scrollable ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Indikator titik oranye kecil (dekorasi Figma)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PrimaryOrange)
            )

            // ── 2. CARD UTAMA ─────────────────────────────────────────────
            MainFormCard(
                email       = email,
                onEmailChange = { email = it },
                emailSent   = emailSent,
                onSendReset = {
                    if (email.isNotBlank()) emailSent = true
                },
                onNavigateBack = onNavigateBack
            )

            // ── 3. FOOTER CARD ────────────────────────────────────────────
            FooterHelpCard()

            // Baris protokol keamanan bawah
            SecurityProtocolRow()

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 2: Card Utama — Icon + Form + Sukses + Kembali
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MainFormCard(
    email         : String,
    onEmailChange : (String) -> Unit,
    emailSent     : Boolean,
    onSendReset   : () -> Unit,
    onNavigateBack: () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = OutlineBorder, shape = RoundedCornerShape(16.dp)),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Icon Reset (lingkaran oranye muda + gembok + perisai) ───────
            ResetIconComposite()

            Spacer(modifier = Modifier.height(4.dp))

            // ── Judul ────────────────────────────────────────────────────────
            Text(
                text       = "Lupa Kata Sandi Akun Kitchen?",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = TextPrimary,
                textAlign  = TextAlign.Center,
                lineHeight = 28.sp
            )

            // ── Deskripsi instruksi ─────────────────────────────────────────
            Text(
                text       = "Masukkan email atau ID karyawan yang terdaftar pada sistem PT Central Saga Mandala untuk menerima instruksi pemulihan.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize   = 13.sp,
                color      = TextSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Field Input Email ────────────────────────────────────────────
            EmailInputField(email = email, onEmailChange = onEmailChange)

            // Hint text monospace
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Security,
                    contentDescription = null,
                    tint               = DividerTextColor,
                    modifier           = Modifier.size(12.dp)
                )
                Text(
                    text       = "Format: user@csm.id atau 8 digit ID Station",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize   = 11.sp,
                    color      = DividerTextColor,
                    letterSpacing = 0.1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tombol Kirim Link Reset ──────────────────────────────────────
            Button(
                onClick  = onSendReset,
                enabled  = email.isNotBlank() && !emailSent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = PrimaryOrange,
                    contentColor           = Color.White,
                    disabledContainerColor = PrimaryOrange.copy(alpha = 0.45f),
                    disabledContentColor   = Color.White.copy(alpha = 0.7f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text          = "Kirim Link Reset",
                    fontFamily    = InterFontFamily,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 15.sp,
                    letterSpacing = 0.2.sp
                )
            }

            // ── Card Notifikasi Sukses (animasi muncul) ─────────────────────
            AnimatedVisibility(
                visible = emailSent,
                enter   = fadeIn(tween(400)) + expandVertically(tween(400))
            ) {
                SuccessNotificationCard(maskedEmail = maskEmail(email))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Divider ATAU ────────────────────────────────────────────────
            OrDivider()

            Spacer(modifier = Modifier.height(4.dp))

            // ── Outlined Button Kembali ke Login ─────────────────────────────
            OutlinedButton(
                onClick  = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape  = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = OutlineBorder
                )
            ) {
                Text(
                    text       = "❮  Kembali ke Login",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = TextPrimary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composable: Icon Reset (lingkaran bertumpuk + badge perisai)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResetIconComposite() {
    Box(contentAlignment = Alignment.BottomEnd) {
        // Lingkaran latar oranye muda
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(IconBgOrangeLight),
            contentAlignment = Alignment.Center
        ) {
            // Emoji kombinasi gembok + panah putar
            Text(text = "🔄", fontSize = 30.sp)
        }
        // Badge perisai kecil hijau di pojok kanan bawah
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SuccessIconGreen)
                .border(2.dp, SurfaceWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Security,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(14.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composable: Field Email / ID Karyawan dengan chip VERIFIED
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmailInputField(
    email        : String,
    onEmailChange: (String) -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text          = "EMAIL / ID KARYAWAN",
            fontFamily    = InterFontFamily,
            fontWeight    = FontWeight.Bold,
            fontSize      = 11.sp,
            color         = TextSecondary,
            letterSpacing = 0.8.sp
        )

        OutlinedTextField(
            value         = email,
            onValueChange = onEmailChange,
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            leadingIcon   = {
                Icon(
                    imageVector        = Icons.Default.Mail,
                    contentDescription = "Ikon Email",
                    tint               = TextSecondary
                )
            },
            trailingIcon = {
                // Chip VERIFIED hijau
                Surface(
                    shape  = RoundedCornerShape(6.dp),
                    color  = VerifiedBgGreen,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text          = "VERIFIED",
                        modifier      = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily    = InterFontFamily,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 10.sp,
                        color         = VerifiedTextGreen,
                        letterSpacing = 0.5.sp
                    )
                }
            },
            shape  = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor         = PrimaryOrange,
                unfocusedBorderColor       = OutlineBorder,
                cursorColor                = PrimaryOrange,
                focusedLeadingIconColor    = PrimaryOrange,
                unfocusedLeadingIconColor  = TextSecondary
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composable: Card Notifikasi Sukses
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuccessNotificationCard(maskedEmail: String) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .border(1.dp, StatusSuccess.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SuccessBgGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Icon centang bulat hijau
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SuccessIconGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = "Berhasil",
                    tint               = Color.White,
                    modifier           = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = "Terkirim ke Server",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = SuccessTextGreen
                )
                // Teks dengan bagian email monospace berwarna
                val annotated = buildAnnotatedString {
                    append("Link reset telah dikirim ke ")
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            color      = SuccessIconGreen
                        )
                    ) { append(maskedEmail) }
                    append(". Periksa folder masuk atau spam Anda.")
                }
                Text(
                    text       = annotated,
                    fontFamily = InterFontFamily,
                    fontSize   = 12.sp,
                    color      = SuccessTextGreen,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composable: Divider "ATAU"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OrDivider() {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(
            modifier  = Modifier.weight(1f),
            thickness = 1.dp,
            color     = OutlineBorder
        )
        Text(
            text          = "ATAU",
            fontFamily    = InterFontFamily,
            fontWeight    = FontWeight.SemiBold,
            fontSize      = 11.sp,
            color         = DividerTextColor,
            letterSpacing = 1.sp
        )
        HorizontalDivider(
            modifier  = Modifier.weight(1f),
            thickness = 1.dp,
            color     = OutlineBorder
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION 3: Footer Card — Bantuan
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FooterHelpCard() {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineBorder, RoundedCornerShape(12.dp)),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Icon headphone
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BackgroundLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.HeadsetMic,
                    contentDescription = "Bantuan",
                    tint               = TextSecondary,
                    modifier           = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = "Butuh bantuan cepat?",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = TextPrimary
                )
                Text(
                    text       = "Hubungi Head Chef atau GM Outlet via Radio CI",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 12.sp,
                    color      = TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composable: Baris protokol keamanan (footer paling bawah)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SecurityProtocolRow() {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Lock,
            contentDescription = null,
            tint               = TextSecondary.copy(alpha = 0.5f),
            modifier           = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text          = "ENFORCED ZERO-DATA EXPOSURE PROTOCOL",
            fontFamily    = JetBrainsMonoFamily,
            fontWeight    = FontWeight.Normal,
            fontSize      = 9.sp,
            color         = TextSecondary.copy(alpha = 0.55f),
            letterSpacing = 0.5.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Masking email untuk tampilan notifikasi sukses
// Contoh: marsel@csm.id → m***l@csm.id
// ─────────────────────────────────────────────────────────────────────────────

private fun maskEmail(email: String): String {
    val atIndex = email.indexOf('@')
    if (atIndex <= 0) return email
    val local  = email.substring(0, atIndex)
    val domain = email.substring(atIndex)
    return when {
        local.length <= 2 -> "${local.first()}*$domain"
        else -> "${local.first()}${"*".repeat((local.length - 2).coerceAtLeast(3))}${local.last()}$domain"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(
    name            = "ResetPasswordScreen — Figma M02",
    showBackground  = true,
    backgroundColor = 0xFFF8FAFC,
    showSystemUi    = true,
    device          = "spec:width=390dp,height=844dp,dpi=420"
)
@Composable
private fun ResetPasswordScreenPreview() {
    KitchenGuardTheme {
        ResetPasswordScreen()
    }
}
