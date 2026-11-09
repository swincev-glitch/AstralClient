package com.astral.client

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.min

/**
 * Плавающая иконка Astral. По нажатию открывает меню с функциями.
 * Иконку можно перетаскивать пальцем.
 */
class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var icon: View? = null
    private var menu: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (icon == null && Settings.canDrawOverlays(this)) showIcon()
        return START_STICKY
    }

    override fun onDestroy() {
        closeMenu()
        icon?.let { runCatching { wm.removeView(it) } }
        icon = null
        super.onDestroy()
    }

    private fun startAsForeground() {
        val channelId = "astral_overlay"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Astral Client", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Astral Client")
            .setContentText("Плавающая иконка активна")
            .setSmallIcon(android.R.drawable.star_on)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ---------- Плавающая иконка ----------

    @SuppressLint("ClickableViewAccessibility")
    private fun showIcon() {
        val size = dp(56)
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(24)
            y = dp(120)
        }

        val view = StarIconView(this)
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        wm.updateViewLayout(view, params)
                    }
                }
                MotionEvent.ACTION_UP -> if (!moved) openMenu()
            }
            true
        }

        wm.addView(view, params)
        icon = view
    }

    // ---------- Меню ----------

    private fun openMenu() {
        if (menu != null) return
        icon?.visibility = View.GONE

        val root = FrameLayout(this)
        root.setBackgroundColor(0x990B0A14.toInt())
        root.setOnClickListener { closeMenu() }

        val dm = resources.displayMetrics
        val width = min(dp(560), (dm.widthPixels * 0.9f).toInt())
        val height = min(dp(300), (dm.heightPixels * 0.9f).toInt())
        root.addView(buildPanel(), FrameLayout.LayoutParams(width, height, Gravity.CENTER))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        wm.addView(root, params)
        menu = root
    }

    private fun closeMenu() {
        menu?.let { runCatching { wm.removeView(it) } }
        menu = null
        icon?.visibility = View.VISIBLE
    }

    private fun shape(color: Int, radiusDp: Int, strokeColor: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null) setStroke(dp(1), strokeColor)
    }

    private fun buildPanel(): View {
        val cText = 0xFFF2F0FF.toInt()
        val cMuted = 0xFFA9A5CC.toInt()
        val cBorder = 0xFF2A2650.toInt()
        val cAccent = 0xFF8B7CFF.toInt()

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = shape(0xFF0F0D1F.toInt(), 20, cBorder)
            isClickable = true
            clipToOutline = true
        }

        // Левая колонка с вкладками
        val side = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF100E20.toInt())
            setPadding(dp(10), dp(14), dp(10), dp(14))
        }
        side.addView(TextView(this).apply {
            text = "ASTRAL"
            setTextColor(cText)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setPadding(dp(8), 0, 0, dp(12))
        })
        val tabsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        side.addView(tabsBox)
        panel.addView(side, LinearLayout.LayoutParams(dp(150), LinearLayout.LayoutParams.MATCH_PARENT))

        // Правая часть
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            setTextColor(cText)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(this).apply {
            text = "×"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(cMuted)
            setOnClickListener { closeMenu() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        content.addView(header)

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        content.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        content.addView(TextView(this).apply {
            text = "Скрыть иконку"
            textSize = 12f
            setTextColor(cMuted)
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { stopSelf() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40)))

        panel.addView(content, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))

        var current = "visual"

        fun render() {
            tabsBox.removeAllViews()
            for ((id, label) in Modules.categories) {
                val selected = id == current
                tabsBox.addView(TextView(this).apply {
                    text = label
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(if (selected) cText else cMuted)
                    if (selected) background = shape(cBorder, 12)
                    setPadding(dp(12), 0, dp(12), 0)
                    setOnClickListener {
                        current = id
                        render()
                    }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                    bottomMargin = dp(6)
                })
            }

            title.text = Modules.categories.first { it.first == current }.second
            list.removeAllViews()

            for (m in Modules.all.filter { it.category == current }) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                }
                fun paintRow(on: Boolean) {
                    row.background = shape(
                        if (on) 0xFF1E1A45.toInt() else 0xFF15132A.toInt(),
                        14,
                        if (on) cAccent else cBorder,
                    )
                }

                val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                texts.addView(TextView(this).apply {
                    text = m.name
                    setTextColor(cText)
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                })
                texts.addView(TextView(this).apply {
                    text = m.hint
                    setTextColor(cMuted)
                    textSize = 12f
                })
                row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                val checked = intArrayOf(android.R.attr.state_checked)
                val sw = Switch(this).apply {
                    isChecked = Modules.isOn(this@OverlayService, m.id)
                    thumbTintList = ColorStateList(arrayOf(checked, intArrayOf()), intArrayOf(cText, cMuted))
                    trackTintList = ColorStateList(arrayOf(checked, intArrayOf()), intArrayOf(cAccent, 0xFF3A3668.toInt()))
                    setOnCheckedChangeListener { _, value ->
                        Modules.set(this@OverlayService, m.id, value)
                        paintRow(value)
                    }
                }
                row.addView(sw)
                paintRow(sw.isChecked)

                list.addView(row, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = dp(8) })
            }
        }

        render()
        return panel
    }
}

/** Круглая иконка со звездой и орбитой. */
private class StarIconView(context: Context) : View(context) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B0A14.toInt() }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4FD1E8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }
    private val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B7CFF.toInt() }
    private val orbit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4FD1E8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val c = w / 2f
        val inset = ring.strokeWidth / 2f
        canvas.drawCircle(c, c, c - inset, bg)
        canvas.drawCircle(c, c, c - inset, ring)

        val r = w * 0.28f
        val k = 0.22f
        val path = Path().apply {
            moveTo(c, c - r)
            lineTo(c + r * k, c - r * k)
            lineTo(c + r, c)
            lineTo(c + r * k, c + r * k)
            lineTo(c, c + r)
            lineTo(c - r * k, c + r * k)
            lineTo(c - r, c)
            lineTo(c - r * k, c - r * k)
            close()
        }
        canvas.drawPath(path, star)

        canvas.save()
        canvas.rotate(-25f, c, c)
        canvas.drawOval(RectF(c - r * 1.5f, c - r * 0.55f, c + r * 1.5f, c + r * 0.55f), orbit)
        canvas.restore()
    }
}
