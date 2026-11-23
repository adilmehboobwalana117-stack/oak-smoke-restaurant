package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CharcoalDeep
import com.example.ui.theme.CorianderGreen
import com.example.ui.theme.CrimsonEmber
import com.example.ui.theme.EmberBorder
import com.example.ui.theme.SmokedAmber

private data class AppPreviewScreenshot(
    val title: String,
    val caption: String,
    val drawableRes: Int
)

private val OAK_AND_SMOKE_SCREENSHOTS = listOf(
    AppPreviewScreenshot(
        title = "Digital Menu & Daily Specials",
        caption = "Browse authentic Pakistani Karahi, Smoked BBQ & Platters in PKR",
        drawableRes = R.drawable.screenshot_digital_menu_1795443033571
    ),
    AppPreviewScreenshot(
        title = "COD Checkout & Live GPS Pin",
        caption = "Cash on Delivery checkout with foreground GPS pin for Riders",
        drawableRes = R.drawable.screenshot_cod_checkout_1795443032191
    ),
    AppPreviewScreenshot(
        title = "Table Booking & Loyalty",
        caption = "Reserve Charcoal Hall, Family Cabin & earn loyalty rewards",
        drawableRes = R.drawable.screenshot_table_booking_1795443033391
    ),
    AppPreviewScreenshot(
        title = "Owner Analytics & Reports",
        caption = "Live PKR profit reports, PDF/Excel export & cloud printing",
        drawableRes = R.drawable.screenshot_owner_dashboard_1795443035325
    )
)

/**
 * Reusable Privacy Policy & App Preview card shown in both Guest Customer mode and Signed-In Profile
 * so Google Play reviewers and users can always inspect the Privacy Policy and app screenshots.
 */
@Composable
fun PrivacyAndScreenshotsCard(
    onOpenPrivacyPolicy: () -> Unit,
    onOpenScreenshots: () -> Unit,
    showDeleteAccountAction: Boolean = false,
    onRequestDeleteAccount: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SmokedCard(
        modifier = modifier,
        borderColor = CorianderGreen.copy(alpha = 0.45f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Policy,
                        contentDescription = "Privacy Policy and Data Safety",
                        tint = CorianderGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Privacy Policy & App Preview",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                StatusPill(label = "PLAY COMPLIANT", color = CorianderGreen)
            }

            Text(
                text = "Read how Oak & Smoke Restaurant Hasilpur protects your data, preview our app screenshots, or manage your account deletion rights.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenPrivacyPolicy,
                    border = BorderStroke(1.dp, CorianderGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_privacy_policy_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Privacy Policy",
                        tint = CorianderGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Privacy Policy", color = CorianderGreen, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenScreenshots,
                    border = BorderStroke(1.dp, SmokedAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_app_screenshots_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = "App Screenshots",
                        tint = SmokedAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Screenshots", color = SmokedAmber, maxLines = 1)
                }
            }

            if (showDeleteAccountAction && onRequestDeleteAccount != null) {
                OutlinedButton(
                    onClick = onRequestDeleteAccount,
                    border = BorderStroke(1.dp, CrimsonEmber.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("delete_account_data_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteForever,
                        contentDescription = "Delete Account and Personal Data",
                        tint = CrimsonEmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Delete My Account & Data", color = CrimsonEmber)
                }
            }
        }
    }
}

/**
 * Complete Google Play Console–compliant Privacy Policy & Data Safety modal sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicySheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Privacy Policy & Data Safety",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Oak & Smoke Restaurant Hasilpur · Effective Nov 23, 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmokedAmber
                    )
                }
                StatusPill(label = "NO ADS", color = CorianderGreen)
            }

            HorizontalDivider(color = EmberBorder)

            PolicyHighlightRow(
                icon = Icons.Filled.VerifiedUser,
                title = "1. Account & Order Information",
                body = "Guest customers can browse the menu and dining zones without signing in. When you sign in with Google, we store your name, email, optional phone number, delivery address, COD orders, table bookings, and loyalty points securely in Google Cloud Firestore."
            )
            PolicyHighlightRow(
                icon = Icons.Filled.LocationOn,
                title = "2. Foreground-Only Delivery Location",
                body = "Location permission is requested ONLY when you tap 'Attach My Current Location' during COD checkout so our Oak & Smoke Rider can navigate to your delivery pin. We never track location in the background."
            )
            PolicyHighlightRow(
                icon = Icons.Filled.Notifications,
                title = "3. Optional Real-Time Alerts",
                body = "Notification permission is optional and used solely for order status updates, rider-ready sound alerts, table reservation confirmations, and daily specials."
            )
            PolicyHighlightRow(
                icon = Icons.Filled.Lock,
                title = "4. Encryption, Zero Data Selling & Security",
                body = "All data is encrypted in transit (HTTPS/TLS) via Google Firebase with strict role-based security rules. We never sell personal data and do not serve third-party ads."
            )
            PolicyHighlightRow(
                icon = Icons.Filled.DeleteForever,
                title = "5. Account & Data Deletion Rights",
                body = "Signed-in users can delete their profile and account directly in Profile → 'Delete My Account & Data', or email adilmehboobwalana117@gmail.com to request full removal within 7 business days."
            )

            HorizontalDivider(color = EmberBorder)

            Text(
                text = "Operator: Oak & Smoke Restaurant, Hasilpur, Pakistan\nPrivacy Contact: adilmehboobwalana117@gmail.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmokedAmber,
                    contentColor = CharcoalDeep
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_privacy_policy_btn")
            ) {
                Text("Close Privacy Policy", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Interactive horizontal showcase of Oak & Smoke's Google Play Store screenshots inside the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreenshotsGallerySheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Oak & Smoke App Screenshots",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Swipe to preview key Customer, COD, Table & Owner screens",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmokedAmber
                    )
                }
                StatusPill(label = "${OAK_AND_SMOKE_SCREENSHOTS.size} SCREENS", color = SmokedAmber)
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(OAK_AND_SMOKE_SCREENSHOTS, key = { it.title }) { shot ->
                    SmokedCard(
                        modifier = Modifier.width(220.dp),
                        borderColor = SmokedAmber.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = shot.drawableRes),
                                contentDescription = shot.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Text(
                                text = shot.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = SmokedAmber
                            )
                            Text(
                                text = shot.caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmokedAmber,
                    contentColor = CharcoalDeep
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_screenshots_gallery_btn")
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun PolicyHighlightRow(
    icon: ImageVector,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = SmokedAmber,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
