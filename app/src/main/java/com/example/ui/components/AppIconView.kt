package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppRepository

@Composable
fun AppIconView(
    packageName: String,
    label: String,
    iconStyle: String,
    repository: AppRepository,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    if (iconStyle == "none") {
        return
    }

    if (iconStyle == "custom_badge") {
        val initial = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (size.value * 0.45f).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val bitmap = remember(packageName, iconStyle) {
        val drawable = repository.getAppIcon(packageName)
        drawable?.let { drawableToBitmap(it, 96, 96) }
    }

    if (bitmap != null) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        val colorFilter = remember(iconStyle) {
            if (iconStyle == "monochrome") {
                // Desaturate and adjust contrast for clean minimalist look
                val matrix = ColorMatrix()
                matrix.setToSaturation(0f)
                ColorFilter.colorMatrix(matrix)
            } else {
                null
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(6.dp))
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                colorFilter = colorFilter,
                modifier = Modifier.size(size)
            )
        }
    } else {
        // Fallback badge if no icon drawable
        val initial = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }

    val bitmap = Bitmap.createBitmap(
        if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else width,
        if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
