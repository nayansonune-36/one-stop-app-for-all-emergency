package com.sentinel.app.feature.map.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.sentinel.app.feature.map.model.ShelterType
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory

object MarkerIconGenerator {

    /**
     * Creates custom vector-rendered map marker icons for emergency facilities.
     * Guaranteed zero network access — rendered programmatically on Android Canvas.
     */
    fun createShelterIcon(context: Context, type: ShelterType): Icon {
        val density = context.resources.displayMetrics.density
        val sizePx = (36 * density).toInt().coerceAtLeast(36)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fillColor = when (type) {
            ShelterType.HOSPITAL -> Color.parseColor("#E53935") // Primary Red
            ShelterType.RELIEF_CAMP -> Color.parseColor("#00BCD4") // Cyan
            ShelterType.SHELTER -> Color.parseColor("#FF9800") // Amber
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColor
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = 2.5f * density
        }

        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val radius = (sizePx / 2f) - (2f * density)

        // Draw pin background & contrast stroke
        canvas.drawCircle(cx, cy, radius, fillPaint)
        canvas.drawCircle(cx, cy, radius, strokePaint)

        // Draw distinct glyph icon per facility category
        when (type) {
            ShelterType.HOSPITAL -> {
                // Cross symbol (+)
                val barW = 2.5f * density
                val barL = 7.5f * density
                canvas.drawRect(cx - barW, cy - barL, cx + barW, cy + barL, symbolPaint)
                canvas.drawRect(cx - barL, cy - barW, cx + barL, cy + barW, symbolPaint)
            }
            ShelterType.RELIEF_CAMP -> {
                // Relief tent triangle (^)
                val path = Path().apply {
                    moveTo(cx, cy - 7f * density)
                    lineTo(cx + 7.5f * density, cy + 6.5f * density)
                    lineTo(cx - 7.5f * density, cy + 6.5f * density)
                    close()
                }
                canvas.drawPath(path, symbolPaint)
            }
            ShelterType.SHELTER -> {
                // Shelter house outline
                val path = Path().apply {
                    moveTo(cx, cy - 7.5f * density)
                    lineTo(cx + 7.5f * density, cy - 0.5f * density)
                    lineTo(cx + 5f * density, cy - 0.5f * density)
                    lineTo(cx + 5f * density, cy + 6.5f * density)
                    lineTo(cx - 5f * density, cy + 6.5f * density)
                    lineTo(cx - 5f * density, cy - 0.5f * density)
                    lineTo(cx - 7.5f * density, cy - 0.5f * density)
                    close()
                }
                canvas.drawPath(path, symbolPaint)
            }
        }

        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    /**
     * Creates a high-visibility pulsing dot icon for the user's GPS position.
     */
    fun createUserLocationIcon(context: Context): Icon {
        val density = context.resources.displayMetrics.density
        val sizePx = (30 * density).toInt().coerceAtLeast(30)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#444CAF50") // Translucent green halo
        }
        val midPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#2E7D32") // Solid green core
        }

        val cx = sizePx / 2f
        val cy = sizePx / 2f

        canvas.drawCircle(cx, cy, (sizePx / 2f) - 1f, outerPaint)
        canvas.drawCircle(cx, cy, 7f * density, midPaint)
        canvas.drawCircle(cx, cy, 5f * density, innerPaint)

        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }
}
