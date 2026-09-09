package de.lautstark.zeigmal.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The card, on screen the instant its tag is seen: the symbol if the
 * Kartensatz has one, the word in any case. The video fades in over this when
 * its first frame has rendered, and this is what remains when it has ended.
 */
@Composable
fun CardFace(
    label: String,
    symbol: File?,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, symbol) {
        value = symbol?.let { file -> withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path)?.asImageBitmap() } }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxHeight(0.6f).padding(16.dp),
                )
            }
            Text(label, color = androidx.compose.ui.graphics.Color.White, fontSize = 48.sp)
        }
    }
}
