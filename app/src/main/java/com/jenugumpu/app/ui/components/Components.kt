package com.jenugumpu.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun BeeIcon(modifier: Modifier = Modifier, color: Color = Color(0xFFF59E0B)) {
    val infiniteTransition = rememberInfiniteTransition(label = "bee_wing")
    val wingAngle by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wing"
    )

    Canvas(modifier = modifier.size(64.dp)) {
        val w = size.width
        val h = size.height

        // Wings
        drawPath(
            path = Path().apply {
                moveTo(w * 0.5f, h * 0.4f)
                val angle = Math.toRadians(wingAngle.toDouble()).toFloat()
                val wingX = (w * 0.1f) * Math.cos(angle.toDouble()).toFloat()
                val wingY = (h * 0.1f) * Math.sin(angle.toDouble()).toFloat()

                quadraticBezierTo(w * 0.2f + wingX, h * 0.1f + wingY, w * 0.1f, h * 0.4f)
                quadraticBezierTo(w * 0.2f, h * 0.6f, w * 0.5f, h * 0.5f)
            },
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = 2.dp.toPx())
        )
        drawPath(
            path = Path().apply {
                moveTo(w * 0.5f, h * 0.4f)
                val angle = Math.toRadians((-wingAngle).toDouble()).toFloat()
                val wingX = (w * 0.1f) * Math.cos(angle.toDouble()).toFloat()
                val wingY = (h * 0.1f) * Math.sin(angle.toDouble()).toFloat()

                quadraticBezierTo(w * 0.8f + wingX, h * 0.1f + wingY, w * 0.9f, h * 0.4f)
                quadraticBezierTo(w * 0.8f, h * 0.6f, w * 0.5f, h * 0.5f)
            },
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = 2.dp.toPx())
        )

        // Body
        drawOval(
            color = color,
            topLeft = Offset(w * 0.35f, h * 0.3f),
            size = Size(w * 0.3f, h * 0.45f)
        )

        // Stripes
        val stripeBrush = Brush.verticalGradient(
            0.3f to color,
            0.4f to Color.Black.copy(alpha = 0.5f),
            0.5f to color,
            0.6f to Color.Black.copy(alpha = 0.5f),
            0.7f to color
        )
        drawOval(
            brush = stripeBrush,
            topLeft = Offset(w * 0.35f, h * 0.3f),
            size = Size(w * 0.3f, h * 0.45f)
        )

        // Stinger
        drawPath(
            path = Path().apply {
                moveTo(w * 0.5f, h * 0.75f)
                lineTo(w * 0.45f, h * 0.85f)
                lineTo(w * 0.55f, h * 0.85f)
                close()
            },
            color = Color.Black.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun HoneyCardPremium(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    label: String,
    darkMode: Boolean = isSystemInDarkTheme(),
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(40.dp))
            .background(
                Brush.linearGradient(
                    colors = if (darkMode) listOf(Color(0xFF1E293B), Color(0xFF1E293B)) else listOf(Color.White, Color(0xFFF1F5F9))
                )
            )
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to (if (darkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)),
                        1f to Color.Transparent,
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 300f, 300f)
                    ),
                    size = size
                )
            }
            .border(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(40.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(28.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = value,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2).sp,
                            color = if (darkMode) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = " $unit",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    BeeIcon(modifier = Modifier.size(48.dp), color = Color(0xFF0F172A))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(4.dp, if (darkMode) Color.White.copy(alpha = 0.2f) else Color(0xFF0F172A).copy(alpha = 0.1f), CircleShape)
                        .background(Color(0xFFFACC15))
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 12.dp),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = Color(0xFFF59E0B),
    darkMode: Boolean = isSystemInDarkTheme(),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    val finalBgColor = if (darkMode) Color(0xFF1E293B) else Color.White
    val finalTextColor = if (darkMode) Color.White else Color(0xFF0F172A)
    val finalIconColor = if (darkMode) Color(0xFFF59E0B) else Color(0xFFFACC15)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(32.dp))
            .background(finalBgColor)
            .border(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(32.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (darkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = finalIconColor, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(
                    text = subtitle,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = finalTextColor.copy(alpha = 0.6f)
                )
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = finalTextColor,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun NarrativeInsight(
    title: String,
    text: String,
    actionText: String,
    darkMode: Boolean = isSystemInDarkTheme(),
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(if (darkMode) Color(0xFF1E293B) else Color.White)
            .border(1.dp, if (darkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(32.dp))
            .padding(32.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B))
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = "\"$text\"",
                fontSize = 20.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color = if (darkMode) Color.White else Color(0xFF0F172A),
                lineHeight = 26.sp
            )

            Row(
                modifier = Modifier
                    .clickable { onAction() }
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = if (darkMode) Color.White else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(12.dp), tint = if (darkMode) Color.White else Color(0xFF0F172A))
            }
        }
    }
}
