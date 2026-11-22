package ch.lauzhand.helpinghand.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO

/** Desktop: the system file dialog. */
@Composable
actual fun rememberPhotoPicker(onPicked: (String) -> Unit): PhotoPicker {
    val latest by rememberUpdatedState(onPicked)
    return remember {
        PhotoPicker(supported = true) {
            Thread {
                val dialog = FileDialog(null as Frame?, "Photo", FileDialog.LOAD).apply {
                    setFilenameFilter { _, name -> name.lowercase().let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") } }
                    isVisible = true
                }
                val file = dialog.file?.let { File(dialog.directory, it) }
                if (file != null) encodeSmallJpeg(file)?.let(latest)
            }.start()
        }
    }
}

private fun encodeSmallJpeg(file: File): String? {
    val source = runCatching { ImageIO.read(file) }.getOrNull() ?: return null
    val scale = minOf(1.0, PHOTO_MAX_SIDE.toDouble() / maxOf(source.width, source.height))
    val w = (source.width * scale).toInt().coerceAtLeast(1)
    val h = (source.height * scale).toInt().coerceAtLeast(1)
    val rgb = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB) // JPEG has no transparency
    rgb.createGraphics().apply { drawImage(source.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null); dispose() }
    val out = ByteArrayOutputStream()
    ImageIO.write(rgb, "jpg", out)
    return Base64.getEncoder().encodeToString(out.toByteArray())
}
