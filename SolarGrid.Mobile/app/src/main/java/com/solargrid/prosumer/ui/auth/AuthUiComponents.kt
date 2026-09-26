/*
 * File: AuthUiComponents.kt
 * Description: Shared auth screen shapes and social row
 */
package com.solargrid.prosumer.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solargrid.prosumer.R
import com.solargrid.prosumer.ui.theme.AccentGold
import com.solargrid.prosumer.ui.theme.FieldBorder
import com.solargrid.prosumer.ui.theme.FieldHint
import com.solargrid.prosumer.ui.theme.SignInButtonText

val AuthPillShape = RoundedCornerShape(50)

/** Wavy bottom edge for auth hero images. */
class WavyBottomShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.78f)
            cubicTo(
                w * 0.85f, h * 0.95f,
                w * 0.55f, h * 1.02f,
                w * 0.38f, h * 0.88f,
            )
            cubicTo(
                w * 0.22f, h * 0.74f,
                w * 0.08f, h * 0.82f,
                0f, h * 0.92f,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentGold,
    unfocusedBorderColor = FieldBorder,
    disabledBorderColor = FieldBorder,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    cursorColor = SignInButtonText,
    focusedTextColor = Color(0xFF111111),
    unfocusedTextColor = Color(0xFF111111),
)

@Composable
fun AuthOrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = FieldBorder,
        )
        Text(
            text = stringResource(R.string.or_divider),
            color = FieldHint,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = FieldBorder,
        )
    }
}

@Composable
fun AuthSocialRow(onComingSoon: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialCircleButton(
            iconRes = R.drawable.ic_google,
            contentDescription = "Google",
            onClick = onComingSoon,
        )
        Spacer(Modifier.width(20.dp))
        SocialCircleButton(
            iconRes = R.drawable.ic_microsoft,
            contentDescription = "Microsoft",
            onClick = onComingSoon,
        )
        Spacer(Modifier.width(20.dp))
        SocialCircleButton(
            iconRes = R.drawable.ic_apple,
            contentDescription = "Apple",
            onClick = onComingSoon,
        )
    }
}

@Composable
private fun SocialCircleButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .border(1.dp, FieldBorder, CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
        )
    }
}
