package pakkupro.view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.request.*
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.skia.Image
import teksturepako.pakku.api.http.client
import teksturepako.pakku.api.platforms.GitHub.bodyIfOK

@Composable
fun IconFromUrl(url: String, modifier: Modifier = Modifier)
{
    suspend fun loadPicture(url: String): ImageBitmap?
    {
        val image = client.get(url).bodyIfOK<ByteArray>()
        return image?.let { Image.makeFromEncoded(it).toComposeImageBitmap() }
    }

    var icon by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        icon = loadPicture(url)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (icon == null)
        {
            CircularProgressIndicator()
        }
        else
        {
            Image(icon!!, "icon")
        }
    }
}