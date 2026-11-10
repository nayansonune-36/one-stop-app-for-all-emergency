package com.sentinel.app.feature.map.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import com.sentinel.app.feature.map.model.ShelterType
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory

object MarkerIconGenerator {

    /**
     * Creates custom vector-rendered map marker icons for emergency facilities.
     * Guaranteed zero network access — rendered programmatically on Android Canvas.
     */
    fun createShelterIcon(context: Context, type: ShelterType, isApproximate: Boolean = false): Icon {
        val density = context.resources.displayMetrics.density
        val sizePx = (38 * density).toInt().coerceAtLeast(38)
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
            color = if (isApproximate) Color.parseColor("#FBBF24") else Color.WHITE
            strokeWidth = if (isApproximate) 3.5f * density else 2.5f * density
            if (isApproximate) {
                pathEffect = DashPathEffect(floatArrayOf(4f * density, 3f * density), 0f)
            }
        }

        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val radius = (sizePx / 2f) - (3f * density)

        // Draw pin background circle & stroke
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

        // Approximate badge badge dot on top-right corner
        if (isApproximate) {
            val approxBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F59E0B")
                style = Paint.Style.FILL
            }
            val approxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
            }
            val bx = cx + radius * 0.7f
            val by = cy - radius * 0.7f
            canvas.drawCircle(bx, by, 4.5f * density, approxBadgePaint)
            canvas.drawCircle(bx, by, 4.5f * density, approxStroke)
        }

        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    /**
     * Creates a cluster marker icon displaying the count of co-located facilities.
     */
    fun createClusterIcon(context: Context, count: Int, isApproximate: Boolean = false): Icon {
        val density = context.resources.displayMetrics.density
        val sizePx = (42 * density).toInt().coerceAtLeast(42)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#1E293B") // Dark slate blue
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = if (isApproximate) Color.parseColor("#FBBF24") else Color.parseColor("#38BDF8")
            strokeWidth = 3f * density
            if (isApproximate) {
                pathEffect = DashPathEffect(floatArrayOf(4f * density, 3f * density), 0f)
            }
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f * density
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val radius = (sizePx / 2f) - (3f * density)

        canvas.drawCircle(cx, cy, radius, fillPaint)
        canvas.drawCircle(cx, cy, radius, strokePaint)

        // Draw count text centered
        val countText = count.toString()
        val textBounds = Rect()
        textPaint.getTextBounds(countText, 0, countText.length, textBounds)
        val textY = cy - textBounds.exactCenterY()
        canvas.drawText(countText, cx, textY, textPaint)

        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    /**
     * Creates a high-visibility pulsing dot icon for the user's GPS position.
     * Uses a vibrant cyan/blue radar halo and bright core for contrast on dark basemaps.
     */
    fun createUserLocationIcon(context: Context): Icon {
        val density = context.resources.displayMetrics.density
        val sizePx = (36 * density).toInt().coerceAtLeast(36)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val outerHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#330284C7") // Translucent blue pulse
        }
        val haloRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            color = Color.parseColor("#6638BDF8")
        }
        val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#0284C7") // Vibrant blue core
        }

        val cx = sizePx / 2f
        val cy = sizePx / 2f

        canvas.drawCircle(cx, cy, (sizePx / 2f) - 2f, outerHaloPaint)
        canvas.drawCircle(cx, cy, (sizePx / 2f) - 2f, haloRingPaint)
        canvas.drawCircle(cx, cy, 9f * density, whiteBorderPaint)
        canvas.drawCircle(cx, cy, 7f * density, corePaint)

        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }
}
