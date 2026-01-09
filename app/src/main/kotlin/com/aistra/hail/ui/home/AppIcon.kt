package com.aistra.hail.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages.myUserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIcon(
    appInfo: AppInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, appInfo) {
        value = withContext(Dispatchers.IO) {
            val applicationInfo = appInfo.applicationInfo
            if (applicationInfo != null) {
                val size = context.resources.displayMetrics.widthPixels / 7 // Adjust size as needed
                val bitmap = AppIconCache.getOrLoadBitmap(context, applicationInfo, myUserId, size)
                bitmap.asImageBitmap()
            } else {
                null
            }
        }
    }

    val colorFilter = if (HailData.grayscaleIcon && appInfo.state == AppInfo.State.FROZEN) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    } else {
        null
    }

    val painter = remember(imageBitmap) {
        imageBitmap?.let { BitmapPainter(it) }
    }

    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = appInfo.name.toString(),
            modifier = modifier,
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter
        )
    } else {
        Box(modifier = modifier)
    }
}
