package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // dp変換ヘルパー
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Float.dp(): Float = this * resources.displayMetrics.density

    // カラー定義
    private val INK = Color.parseColor("#0e0a1f")
    private val NIGHT = Color.parseColor("#15102e")
    private val TWILIGHT = Color.parseColor("#251843")
    private val DAWN = Color.parseColor("#5a2f6c")
    private val ROSE = Color.parseColor("#d96b8f")
    private val PEACH = Color.parseColor("#f7a173")
    private val SUN = Color.parseColor("#ffd166")
    private val GOLD = Color.parseColor("#e8b455")
    private val CREAM = Color.parseColor("#fff4e6")
    private val GREEN = Color.parseColor("#7be0a4")
    private val RED = Color.parseColor("#ff7785")
    private val SURF1 = Color.parseColor("#1a1a3a")
    private val SURF2 = Color.parseColor("#22204a")
    private val SURF3 = Color.parseColor("#2a2855")
    private val LINE2 = Color.parseColor("#3a3566")

    // 状態
    private var currentTab = "home"
    private var alarms = mutableListOf<AlarmData>()
    private var schedules = mutableListOf<ScheduleData>()
    private var oshiList = mutableListOf<OshiData>()
    private var weatherData: WeatherInfo? = null
    private var newsItems = mutableListOf<NewsItem>()

    // UI参照
    private lateinit var mainContainer: FrameLayout
    private lateinit var homeScreen: ScrollView
    private lateinit var oshiScreen: ScrollView
    private lateinit var todayScreen: ScrollView
    private lateinit var settingsScreen: ScrollView
    private lateinit var tabBar: LinearLayout
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var stageEmoji: TextView
    private lateinit var stageBubble: TextView
    private lateinit var stageCharName: TextView
    private lateinit var alarmListContainer: LinearLayout
    private lateinit var schedListContainer: LinearLayout
    private lateinit var weatherIcon: TextView
    private lateinit var weatherTemp: TextView
    private lateinit var weatherCond: TextView
    private lateinit var weatherLoc: TextView
    private lateinit var weatherRange: TextView
    private lateinit var newsListContainer: LinearLayout
    private lateinit var oshiListContainer: LinearLayout

    private lateinit var prefs: SharedPreferences
    private val timer = Timer()

    data class AlarmData(
        val id: String = UUID.randomUUID().toString().take(8),
        var time: String = "07:00",
        var label: String = "",
        var enabled: Boolean = true,
        var emoji: String = "🍃",
        var characterName: String = "ずんだもん"
    )

    data class ScheduleData(
        val id: String = UUID.randomUUID().toString().take(8),
        var time: String = "09:00",
        var text: String = ""
    )

    data class OshiData(
        val id: String = UUID.randomUUID().toString().take(8),
        var emoji: String = "💖",
        var name: String = "",
        var group: String = "",
        var color: Int = Color.parseColor("#ffd166"),
        var desc: String = ""
    )

    data class WeatherInfo(
        var temp: Int = 0,
        var tempMax: Int = 0,
        var tempMin: Int = 0,
        var code: Int = 0,
        var condition: String = "",
        var location: String = "東京"
    )

    data class NewsItem(var title: String = "", var link: String = "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("mezamashi", Context.MODE_PRIVATE)
        loadData()
        buildUI()
        startClock()
        lifecycleScope.launch {
            fetchWeather()
            fetchNews()
        }
    }

    // データ保存・読み込み
    private fun saveData() {
        val editor = prefs.edit()
        val alarmsJson = JSONArray()
        alarms.forEach { a ->
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("time", a.time)
            obj.put("label", a.label)
            obj.put("enabled", a.enabled)
            obj.put("emoji", a.emoji)
            obj.put("characterName", a.characterName)
            alarmsJson.put(obj)
        }
        editor.putString("alarms", alarmsJson.toString())
        val schedsJson = JSONArray()
        schedules.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("time", s.time)
            obj.put("text", s.text)
            schedsJson.put(obj)
        }
        editor.putString("schedules", schedsJson.toString())
        val oshiJson = JSONArray()
        oshiList.forEach { o ->
            val obj = JSONObject()
            obj.put("id", o.id)
            obj.put("emoji", o.emoji)
            obj.put("name", o.name)
            obj.put("group", o.group)
            obj.put("color", o.color)
            obj.put("desc", o.desc)
            oshiJson.put(obj)
        }
        editor.putString("oshi", oshiJson.toString())
        editor.apply()
    }

    private fun loadData() {
        try {
            val alarmsStr = prefs.getString("alarms", "[]") ?: "[]"
            val arr = JSONArray(alarmsStr)
            alarms.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                alarms.add(AlarmData(
                    id = obj.optString("id", UUID.randomUUID().toString().take(8)),
                    time = obj.optString("time", "07:00"),
                    label = obj.optString("label", ""),
                    enabled = obj.optBoolean("enabled", true),
                    emoji = obj.optString("emoji", "🍃"),
                    characterName = obj.optString("characterName", "ずんだもん")
                ))
            }
        } catch (_: Exception) { }
        try {
            val schedsStr = prefs.getString("schedules", "[]") ?: "[]"
            val arr = JSONArray(schedsStr)
            schedules.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                schedules.add(ScheduleData(
                    id = obj.optString("id", UUID.randomUUID().toString().take(8)),
                    time = obj.optString("time", "09:00"),
                    text = obj.optString("text", "")
                ))
            }
        } catch (_: Exception) { }
        try {
            val oshiStr = prefs.getString("oshi", "[]") ?: "[]"
            val arr = JSONArray(oshiStr)
            oshiList.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                oshiList.add(OshiData(
                    id = obj.optString("id", UUID.randomUUID().toString().take(8)),
                    emoji = obj.optString("emoji", "💖"),
                    name = obj.optString("name", ""),
                    group = obj.optString("group", ""),
                    color = obj.optInt("color", Color.parseColor("#ffd166")),
                    desc = obj.optString("desc", "")
                ))
            }
        } catch (_: Exception) { }
        if (schedules.isEmpty()) {
            schedules.add(ScheduleData(time = "09:00", text = "朝のミーティング"))
            schedules.add(ScheduleData(time = "12:30", text = "お昼ごはん"))
        }
    }

    // 角丸背景を作成
    private fun roundRect(color: Int, radius: Float, strokeColor: Int = 0, strokeWidth: Int = 0): GradientDrawable {
        val gd = GradientDrawable()
        gd.setColor(color)
        gd.cornerRadius = radius
        if (strokeColor != 0 && strokeWidth > 0) {
            gd.setStroke(strokeWidth, strokeColor)
        }
        return gd
    }

    // グラデーション背景を作成
    private fun gradientBg(colors: IntArray, radius: Float = 0f): GradientDrawable {
        val gd = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
        gd.cornerRadius = radius
        return gd
    }

    // UI構築
    private fun buildUI() {
        mainContainer = FrameLayout(this)
        mainContainer.setBackgroundColor(INK)

        // 全体のグラデーション背景
        val bgGrad = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(INK, TWILIGHT, DAWN, ROSE, PEACH)
        )
        mainContainer.background = bgGrad

        // メインコンテンツエリア
        val contentFrame = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { bottomMargin = 64.dp() }
        }

        homeScreen = buildHomeScreen()
        oshiScreen = buildOshiScreen()
        todayScreen = buildTodayScreen()
        settingsScreen = buildSettingsScreen()

        contentFrame.addView(homeScreen)
        contentFrame.addView(oshiScreen)
        contentFrame.addView(todayScreen)
        contentFrame.addView(settingsScreen)

        mainContainer.addView(contentFrame)

        // タブバー
        tabBar = buildTabBar()
        val tabParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            64.dp()
        ).apply { gravity = Gravity.BOTTOM }
        mainContainer.addView(tabBar, tabParams)

        setContentView(mainContainer)
        switchTab("home")
    }

    // ホーム画面
    private fun buildHomeScreen(): ScrollView {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
        }

        // ブランド
        root.addView(TextView(this).apply {
            text = "MEZAMASHI"
            setTextColor(GOLD)
            textSize = 11f
            letterSpacing = 0.4f
            gravity = Gravity.CENTER
            setPadding(0, 12.dp(), 0, 4.dp())
        })
        root.addView(TextView(this).apply {
            text = "朝のしらべ"
            setTextColor(CREAM)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16.dp())
        })

        // 時計
        clockText = TextView(this).apply {
            text = "00:00"
            setTextColor(CREAM)
            textSize = 64f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8.dp())
            setOnClickListener { showAlarmDialog(null) }
        }
        root.addView(clockText)

        // 日付
        dateText = TextView(this).apply {
            text = "— —"
            setTextColor(CREAM)
            alpha = 0.75f
            textSize = 12f
            letterSpacing = 0.2f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20.dp())
        }
        root.addView(dateText)

        // ステージ
        val stageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 8.dp(), 0, 24.dp())
        }

        stageBubble = TextView(this).apply {
            text = "おはよう!"
            setTextColor(INK)
            textSize = 14f
            gravity = Gravity.CENTER
            background = roundRect(CREAM, 16f.dp())
            setPadding(16.dp(), 10.dp(), 16.dp(), 10.dp())
            setOnClickListener { updateBubble() }
        }
        stageLayout.addView(stageBubble, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER; bottomMargin = 12.dp() })

        stageEmoji = TextView(this).apply {
            text = "🍃"
            textSize = 80f
            gravity = Gravity.CENTER
            setOnClickListener { updateBubble() }
        }
        stageLayout.addView(stageEmoji)

        stageCharName = TextView(this).apply {
            text = "ずんだもん"
            setTextColor(CREAM)
            alpha = 0.7f
            textSize = 11f
            letterSpacing = 0.2f
            gravity = Gravity.CENTER
            setPadding(0, 8.dp(), 0, 0)
        }
        stageLayout.addView(stageCharName)
        root.addView(stageLayout)

        // アラームセクション
        root.addView(buildSectionCard("アラーム") {
            alarmListContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            it.addView(alarmListContainer)
            it.addView(buildGhostButton("＋ アラームを追加") { showAlarmDialog(null) })
        })

        // スケジュールセクション
        root.addView(buildSectionCard("今日のスケジュール") {
            schedListContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            it.addView(schedListContainer)
            it.addView(buildGhostButton("＋ タップして予定を追加") { showScheduleDialog(null) })
        })

        scroll.addView(root)
        return scroll
    }

    // 推しタブ
    private fun buildOshiScreen(): ScrollView {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
            visibility = View.GONE
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
        }

        root.addView(TextView(this).apply {
            text = "OSHI"
            setTextColor(GOLD)
            textSize = 11f
            letterSpacing = 0.4f
            gravity = Gravity.CENTER
            setPadding(0, 12.dp(), 0, 4.dp())
        })
        root.addView(TextView(this).apply {
            text = "あなたの推し"
            setTextColor(CREAM)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20.dp())
        })

        oshiListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(oshiListContainer)

        root.addView(buildGhostButton("＋ 推しを登録") { showOshiDialog(null) }.apply {
            val lp = layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 10.dp()
            layoutParams = lp
        })

        // プライベートノート
        val noteCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundRect(SURF1, 16f.dp(), LINE2, 1.dp())
            setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 24.dp()
            layoutParams = lp
        }
        noteCard.addView(TextView(this).apply {
            text = "あなただけの推し空間"
            setTextColor(PEACH)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        })
        noteCard.addView(TextView(this).apply {
            text = "Xや Instagramのように外に向けて発信する場所ではなく、推しのことを自分のためだけに集めて整える、私的なノートです。"
            setTextColor(CREAM)
            alpha = 0.78f
            textSize = 11f
            setLineSpacing(4f.dp(), 1f)
        })
        root.addView(noteCard)

        scroll.addView(root)
        return scroll
    }

    // 今日タブ
    private fun buildTodayScreen(): ScrollView {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
            visibility = View.GONE
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
        }

        root.addView(TextView(this).apply {
            text = "TODAY"
            setTextColor(GOLD)
            textSize = 11f
            letterSpacing = 0.4f
            gravity = Gravity.CENTER
            setPadding(0, 12.dp(), 0, 4.dp())
        })
        root.addView(TextView(this).apply {
            text = "きょうのしらせ"
            setTextColor(CREAM)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20.dp())
        })

        // 天気セクション
        root.addView(buildSectionCard("天気") { section ->
            val weatherCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = gradientBg(intArrayOf(
                    Color.parseColor("#33ffd166"),
                    Color.parseColor("#18f7a173")
                ), 12f.dp())
                setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
                gravity = Gravity.CENTER_VERTICAL
            }
            weatherIcon = TextView(this).apply {
                text = "🌤"
                textSize = 40f
            }
            weatherCard.addView(weatherIcon, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = 14.dp() })

            val weatherInfo = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            weatherLoc = TextView(this).apply {
                text = "場所を取得中…"
                setTextColor(CREAM)
                alpha = 0.75f
                textSize = 11f
                letterSpacing = 0.1f
            }
            weatherInfo.addView(weatherLoc)

            val tempRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            weatherTemp = TextView(this).apply {
                text = "--°"
                setTextColor(CREAM)
                textSize = 30f
                typeface = Typeface.MONOSPACE
            }
            tempRow.addView(weatherTemp)
            weatherCond = TextView(this).apply {
                text = "取得中…"
                setTextColor(CREAM)
                alpha = 0.85f
                textSize = 13f
                setPadding(8.dp(), 0, 0, 0)
            }
            tempRow.addView(weatherCond)
            weatherInfo.addView(tempRow)

            weatherRange = TextView(this).apply {
                text = "最高 -- / 最低 --"
                setTextColor(CREAM)
                alpha = 0.7f
                textSize = 11f
            }
            weatherInfo.addView(weatherRange)
            weatherCard.addView(weatherInfo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            section.addView(weatherCard)

            // 天気更新ボタン
            section.addView(buildGhostButton("🔄 天気を更新") {
                lifecycleScope.launch { fetchWeather() }
            }.apply {
                val lp = layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = 8.dp()
                layoutParams = lp
            })
        })

        // ニュースセクション
        root.addView(buildSectionCard("ニュース") { section ->
            newsListContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            section.addView(newsListContainer)
            section.addView(buildGhostButton("🔄 ニュースを更新") {
                lifecycleScope.launch { fetchNews() }
            }.apply {
                val lp = layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = 8.dp()
                layoutParams = lp
            })
        })

        scroll.addView(root)
        return scroll
    }

    // 設定タブ
    private fun buildSettingsScreen(): ScrollView {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
            visibility = View.GONE
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
        }

        root.addView(TextView(this).apply {
            text = "SETTINGS"
            setTextColor(GOLD)
            textSize = 11f
            letterSpacing = 0.4f
            gravity = Gravity.CENTER
            setPadding(0, 12.dp(), 0, 4.dp())
        })
        root.addView(TextView(this).apply {
            text = "せってい"
            setTextColor(CREAM)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20.dp())
        })

        // 推し管理
        root.addView(buildSectionCard("推し") { section ->
            section.addView(buildPrimaryButton("＋ 推しを追加") { showOshiDialog(null) })

            section.addView(TextView(this).apply {
                text = "推しの絵文字・名前・グループを登録して、推しタブで管理できます。"
                setTextColor(CREAM)
                alpha = 0.75f
                textSize = 11f
                setLineSpacing(3f.dp(), 1f)
                background = roundRect(SURF1, 8f.dp(), LINE2, 1.dp())
                setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = 10.dp()
                layoutParams = lp
            })
        })

        // データ管理
        root.addView(buildSectionCard("データ") { section ->
            section.addView(buildGhostButton("🗑 全データをリセット") {
                AlertDialog.Builder(this)
                    .setTitle("データリセット")
                    .setMessage("全てのデータを削除しますか？")
                    .setPositiveButton("削除") { _, _ ->
                        prefs.edit().clear().apply()
                        alarms.clear()
                        schedules.clear()
                        oshiList.clear()
                        refreshAllUI()
                        Toast.makeText(this, "データをリセットしました", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
            })
        })

        // バージョン
        root.addView(TextView(this).apply {
            text = "朝のしらべ v1.0"
            setTextColor(CREAM)
            alpha = 0.4f
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, 20.dp(), 0, 20.dp())
        })

        scroll.addView(root)
        return scroll
    }

    // セクションカードビルダー
    private fun buildSectionCard(title: String, builder: (LinearLayout) -> Unit): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundRect(SURF2, 22f.dp(), LINE2, 1.dp())
            setPadding(20.dp(), 18.dp(), 20.dp(), 18.dp())
            elevation = 6f.dp()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 14.dp()
            layoutParams = lp
        }

        // タイトル
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 14.dp())
        }
        val dot = View(this).apply {
            background = roundRect(GOLD, 50f)
            val lp = LinearLayout.LayoutParams(5.dp(), 5.dp())
            lp.rightMargin = 10.dp()
            layoutParams = lp
        }
        titleRow.addView(dot)
        titleRow.addView(TextView(this).apply {
            text = title
            setTextColor(GOLD)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.15f
        })
        card.addView(titleRow)
        builder(card)
        return card
    }

    // ボタン生成
    private fun buildPrimaryButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(INK)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            background = gradientBg(intArrayOf(SUN, PEACH), 12f.dp())
            setPadding(20.dp(), 12.dp(), 20.dp(), 12.dp())
            isAllCaps = false
            elevation = 4f.dp()
            stateListAnimator = null
            setOnClickListener { onClick() }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }
    }

    private fun buildGhostButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(CREAM)
            textSize = 13f
            background = roundRect(SURF2, 12f.dp(), LINE2, 1.dp())
            setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
            isAllCaps = false
            stateListAnimator = null
            setOnClickListener { onClick() }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }
    }

    // タブバー
    private fun buildTabBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#D90e0a1f"))
            gravity = Gravity.CENTER_VERTICAL
            elevation = 10f.dp()
        }
        val tabs = listOf(
            "home" to "🏠 ホーム",
            "oshi" to "💗 推し",
            "today" to "📰 今日",
            "settings" to "⚙ 設定"
        )
        tabs.forEach { (id, label) ->
            val tab = TextView(this).apply {
                text = label
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(4.dp(), 12.dp(), 4.dp(), 12.dp())
                setTextColor(if (id == currentTab) SUN else Color.argb(120, 255, 244, 230))
                setOnClickListener { switchTab(id) }
            }
            bar.addView(tab, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
        return bar
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        homeScreen.visibility = if (tab == "home") View.VISIBLE else View.GONE
        oshiScreen.visibility = if (tab == "oshi") View.VISIBLE else View.GONE
        todayScreen.visibility = if (tab == "today") View.VISIBLE else View.GONE
        settingsScreen.visibility = if (tab == "settings") View.VISIBLE else View.GONE

        // タブバーの色を更新
        for (i in 0 until tabBar.childCount) {
            val child = tabBar.getChildAt(i) as? TextView ?: continue
            val tabs = listOf("home", "oshi", "today", "settings")
            if (i < tabs.size) {
                child.setTextColor(if (tabs[i] == tab) SUN else Color.argb(120, 255, 244, 230))
            }
        }

        if (tab == "home") refreshHomeUI()
        if (tab == "oshi") refreshOshiUI()
        if (tab == "today") refreshTodayUI()
    }

    // 時計更新
    private fun startClock() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread { updateClock() }
            }
        }, 0, 1000)
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        val h = String.format("%02d", now.get(Calendar.HOUR_OF_DAY))
        val m = String.format("%02d", now.get(Calendar.MINUTE))
        clockText.text = "$h:$m"
        val dayNames = arrayOf("日", "月", "火", "水", "木", "金", "土")
        val dow = dayNames[now.get(Calendar.DAY_OF_WEEK) - 1]
        val y = now.get(Calendar.YEAR)
        val mo = String.format("%02d", now.get(Calendar.MONTH) + 1)
        val d = String.format("%02d", now.get(Calendar.DAY_OF_MONTH))
        dateText.text = "$y.$mo.$d $dow"
    }

    private fun updateBubble() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val lines = mutableListOf<String>()
        if (h in 5..9) lines.addAll(listOf("おはよう!", "今日も一日がんばろうね", "朝ごはん食べた?"))
        else if (h in 10..11) lines.addAll(listOf("お仕事はかどってる?", "いいお天気だね"))
        else if (h in 12..13) lines.addAll(listOf("お昼の時間だよ", "おなかすいたね"))
        else if (h in 14..16) lines.addAll(listOf("午後もファイト!", "コーヒーでも飲もうか"))
        else if (h in 17..18) lines.addAll(listOf("おつかれさま!", "もうすぐ夕方だね"))
        else if (h in 19..21) lines.addAll(listOf("今日も一日おつかれさま", "ゆっくり休んでね"))
        else lines.addAll(listOf("そろそろ寝る時間…", "ぐっすりおやすみ"))

        if (weatherData != null) {
            if (weatherData!!.code in 51..67) lines.add("雨だよ、傘もった?")
            if (weatherData!!.temp >= 30) lines.add("今日は暑いね、水分補給!")
            if (weatherData!!.temp <= 5) lines.add("寒いから防寒対策ね")
        }
        oshiList.forEach { o ->
            if (o.name.isNotBlank()) lines.add("${o.name}のこと応援してるよ!")
        }
        stageBubble.text = lines.random()
    }

    // UI更新
    private fun refreshAllUI() {
        refreshHomeUI()
        refreshOshiUI()
        refreshTodayUI()
    }

    private fun refreshHomeUI() {
        renderAlarms()
        renderSchedules()
        updateBubble()
    }

    private fun refreshOshiUI() {
        renderOshiList()
    }

    private fun refreshTodayUI() {
        renderWeather()
        renderNews()
    }

    // アラーム一覧描画
    private fun renderAlarms() {
        alarmListContainer.removeAllViews()
        if (alarms.isEmpty()) {
            alarmListContainer.addView(TextView(this).apply {
                text = "アラームがまだありません"
                setTextColor(CREAM)
                alpha = 0.55f
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 20.dp(), 0, 20.dp())
            })
            return
        }
        alarms.sortBy { it.time }
        alarms.forEach { alarm ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = roundRect(SURF2, 12f.dp(), LINE2, 1.dp())
                setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
                gravity = Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 8.dp()
                layoutParams = lp
                alpha = if (alarm.enabled) 1f else 0.45f
            }
            // 絵文字
            row.addView(TextView(this).apply {
                text = alarm.emoji
                textSize = 22f
                setPadding(0, 0, 12.dp(), 0)
            })

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            info.addView(TextView(this).apply {
                text = alarm.time
                setTextColor(CREAM)
                textSize = 28f
                typeface = Typeface.MONOSPACE
            })
            info.addView(TextView(this).apply {
                text = "${alarm.label.ifBlank { "(無題)" }} ・ ${alarm.characterName}"
                setTextColor(CREAM)
                alpha = 0.65f
                textSize = 11f
            })
            row.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            // 編集ボタン
            row.addView(TextView(this).apply {
                text = "✏"
                textSize = 18f
                setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
                setOnClickListener { showAlarmDialog(alarm) }
            })
            // 削除ボタン
            row.addView(TextView(this).apply {
                text = "🗑"
                textSize = 18f
                setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
                setOnClickListener {
                    alarms.remove(alarm)
                    saveData()
                    renderAlarms()
                    Toast.makeText(this@MainActivity, "アラームを削除しました", Toast.LENGTH_SHORT).show()
                }
            })
            // ON/OFFスイッチ
            val sw = Switch(this).apply {
                isChecked = alarm.enabled
                setOnCheckedChangeListener { _, checked ->
                    alarm.enabled = checked
                    row.alpha = if (checked) 1f else 0.45f
                    saveData()
                }
            }
            row.addView(sw)
            alarmListContainer.addView(row)
        }
    }

    // スケジュール一覧描画
    private fun renderSchedules() {
        schedListContainer.removeAllViews()
        if (schedules.isEmpty()) {
            schedListContainer.addView(TextView(this).apply {
                text = "予定はまだありません"
                setTextColor(CREAM)
                alpha = 0.55f
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 20.dp(), 0, 20.dp())
            })
            return
        }
        schedules.sortBy { it.time }
        schedules.forEach { sched ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = roundRect(SURF1, 12f.dp(), LINE2, 1.dp())
                setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
                gravity = Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 8.dp()
                layoutParams = lp
            }
            row.addView(TextView(this).apply {
                text = sched.time
                setTextColor(SUN)
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setPadding(0, 0, 12.dp(), 0)
                minWidth = 52.dp()
            })
            row.addView(TextView(this).apply {
                text = sched.text
                setTextColor(CREAM)
                textSize = 14f
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this).apply {
                text = "✏"
                textSize = 14f
                setPadding(8.dp(), 8.dp(), 0, 8.dp())
                alpha = 0.5f
            })
            row.setOnClickListener { showScheduleDialog(sched) }
            schedListContainer.addView(row)
        }
    }

    // 推し一覧描画
    private fun renderOshiList() {
        oshiListContainer.removeAllViews()
        if (oshiList.isEmpty()) {
            oshiListContainer.addView(TextView(this).apply {
                text = "推しがまだ登録されていません"
                setTextColor(CREAM)
                alpha = 0.55f
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 20.dp(), 0, 20.dp())
            })
            return
        }
        oshiList.forEach { oshi ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = roundRect(SURF1, 16f.dp())
                setPadding(12.dp(), 14.dp(), 12.dp(), 14.dp())
                gravity = Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 4.dp()
                layoutParams = lp
            }
            // カラーバー
            row.addView(View(this).apply {
                background = roundRect(oshi.color, 4f.dp())
                val lp = LinearLayout.LayoutParams(4.dp(), 48.dp())
                lp.rightMargin = 12.dp()
                layoutParams = lp
            })
            // アバター
            val avatar = TextView(this).apply {
                text = oshi.emoji
                textSize = 28f
                gravity = Gravity.CENTER
                background = roundRect(SURF3, 50f.dp())
                val size = 48.dp()
                val lp = LinearLayout.LayoutParams(size, size)
                lp.rightMargin = 12.dp()
                layoutParams = lp
            }
            row.addView(avatar)

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            info.addView(TextView(this).apply {
                text = oshi.name + if (oshi.group.isNotBlank()) " ・ ${oshi.group}" else ""
                setTextColor(CREAM)
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                maxLines = 1
            })
            if (oshi.desc.isNotBlank()) {
                info.addView(TextView(this).apply {
                    text = oshi.desc
                    setTextColor(CREAM)
                    alpha = 0.65f
                    textSize = 12f
                    maxLines = 1
                })
            }
            row.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            row.setOnClickListener { showOshiDialog(oshi) }
            oshiListContainer.addView(row)
        }
    }

    // 天気描画
    private fun renderWeather() {
        val w = weatherData
        if (w == null) {
            weatherLoc.text = "天気を取得できません"
            weatherTemp.text = "--°"
            weatherCond.text = ""
            weatherRange.text = ""
            return
        }
        weatherIcon.text = weatherCodeToEmoji(w.code)
        weatherLoc.text = w.location
        weatherTemp.text = "${w.temp}°"
        weatherCond.text = w.condition
        weatherRange.text = "最高 ${w.tempMax}° / 最低 ${w.tempMin}°"
    }

    // ニュース描画
    private fun renderNews() {
        newsListContainer.removeAllViews()
        if (newsItems.isEmpty()) {
            newsListContainer.addView(TextView(this).apply {
                text = "ニュースを取得できませんでした"
                setTextColor(CREAM)
                alpha = 0.55f
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 20.dp(), 0, 20.dp())
            })
            return
        }
        newsItems.take(6).forEachIndexed { i, item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = roundRect(SURF1, 12f.dp(), LINE2, 1.dp())
                setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
                gravity = Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 6.dp()
                layoutParams = lp
            }
            // 番号
            val numBg = roundRect(SUN, 50f)
            row.addView(TextView(this).apply {
                text = "${i + 1}"
                setTextColor(INK)
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                background = numBg
                val sz = 22.dp()
                val lp = LinearLayout.LayoutParams(sz, sz)
                lp.rightMargin = 10.dp()
                layoutParams = lp
            })
            // タイトル
            row.addView(TextView(this).apply {
                text = item.title
                setTextColor(CREAM)
                textSize = 13f
                setLineSpacing(3f.dp(), 1f)
                maxLines = 3
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            newsListContainer.addView(row)
        }
    }

    // アラーム追加/編集ダイアログ
    private fun showAlarmDialog(existing: AlarmData?) {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 16.dp(), 24.dp(), 8.dp())
        }

        dialogLayout.addView(TextView(this).apply {
            text = "時刻"
            setTextColor(GOLD)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        })
        val timeInput = EditText(this).apply {
            setText(existing?.time ?: "07:00")
            setTextColor(CREAM)
            setHintTextColor(Color.argb(100, 255, 244, 230))
            hint = "例: 07:00"
            textSize = 22f
            typeface = Typeface.MONOSPACE
            background = roundRect(SURF2, 8f.dp(), LINE2, 1.dp())
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
            inputType = InputType.TYPE_CLASS_DATETIME
        }
        dialogLayout.addView(timeInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12.dp() })

        dialogLayout.addView(TextView(this).apply {
            text = "ラベル"
            setTextColor(GOLD)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        })
        val labelInput = EditText(this).apply {
            setText(existing?.label ?: "")
            setTextColor(CREAM)
            setHintTextColor(Color.argb(100, 255, 244, 230))
            hint = "例: 出勤の時間"
            textSize = 15f
            background = roundRect(SURF2, 8f.dp(), LINE2, 1.dp())
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
            maxLines = 1
        }
        dialogLayout.addView(labelInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12.dp() })

        val builder = AlertDialog.Builder(this)
            .setTitle(if (existing != null) "アラームを編集" else "新しいアラーム")
            .setView(dialogLayout)
            .setPositiveButton("保存") { _, _ ->
                val time = timeInput.text.toString().trim()
                val label = labelInput.text.toString().trim()
                if (time.isBlank()) {
                    Toast.makeText(this, "時刻を入力してください", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existing != null) {
                    existing.time = time
                    existing.label = label
                } else {
                    alarms.add(AlarmData(time = time, label = label))
                }
                saveData()
                renderAlarms()
            }
            .setNegativeButton("キャンセル", null)

        if (existing != null) {
            builder.setNeutralButton("削除") { _, _ ->
                alarms.remove(existing)
                saveData()
                renderAlarms()
                Toast.makeText(this, "アラームを削除しました", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    // スケジュール追加/編集ダイアログ
    private fun showScheduleDialog(existing: ScheduleData?) {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 16.dp(), 24.dp(), 8.dp())
        }

        dialogLayout.addView(TextView(this).apply {
            text = "時刻"
            setTextColor(GOLD)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        })
        val timeInput = EditText(this).apply {
            setText(existing?.time ?: run {
                val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1
                String.format("%02d:00", if (h > 23) 23 else h)
            })
            setTextColor(CREAM)
            setHintTextColor(Color.argb(100, 255, 244, 230))
            hint = "例: 09:00"
            textSize = 18f
            typeface = Typeface.MONOSPACE
            background = roundRect(SURF2, 8f.dp(), LINE2, 1.dp())
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
            inputType = InputType.TYPE_CLASS_DATETIME
        }
        dialogLayout.addView(timeInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12.dp() })

        dialogLayout.addView(TextView(this).apply {
            text = "予定"
            setTextColor(GOLD)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        })
        val textInput = EditText(this).apply {
            setText(existing?.text ?: "")
            setTextColor(CREAM)
            setHintTextColor(Color.argb(100, 255, 244, 230))
            hint = "例: 朝のミーティング"
            textSize = 15f
            background = roundRect(SURF2, 8f.dp(), LINE2, 1.dp())
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
            maxLines = 1
        }
        dialogLayout.addView(textInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12.dp() })

        val builder = AlertDialog.Builder(this)
            .setTitle(if (existing != null) "予定を編集" else "新しい予定")
            .setView(dialogLayout)
            .setPositiveButton("保存") { _, _ ->
                val time = timeInput.text.toString().trim()
                val text = textInput.text.toString().trim()
                if (time.isBlank() || text.isBlank()) {
                    Toast.makeText(this, "時刻と内容を入力してください", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existing != null) {
                    existing.time = time
                    existing.text = text
                } else {
                    schedules.add(ScheduleData(time = time, text = text))
                }
                saveData()
                renderSchedules()
            }
            .setNegativeButton("キャンセル", null)

        if (existing != null) {
            builder.setNeutralButton("削除") { _, _ ->
                schedules.remove(existing)
                saveData()
                renderSchedules()
                Toast.makeText(this, "予定を削除しました", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    // 推し追加/編集ダイアログ
    private fun showOshiDialog(existing: OshiData?) {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 16.dp(), 24.dp(), 8.dp())
        }

        dialogLayout.addView(TextView(this).apply {
            text = "絵文字"
            setTextColor(GOLD)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        })
        val emojiInput = EditText(this).apply {
            setText(existing?.emoji ?: "💖")
            textSize = 24f
            gravity = Gravity.CENTER
            background = roundRect(SURF2, 8f.dp(), LINE2, 1.dp())
            setPadding(14.dp(), 8.dp(), 14.dp(), 8.dp())
        }
        dialogLayout.addView(emojiInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12.dp() })

        dialogLayout.addView(TextView(this).apply {
            text = "名前"
            setTextColor(GOLD)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        })
        val nameInput = EditText(this).apply {
            setText(existing?.name ?: "")
            setTextColor(CREAM)
            setHintTextColor(Color.argb(100, 255, 244, 230))
            hint = "例: 推しの名前"
            textSize = 15f
            background = roundRect(SURF2, 8f.dp(), LINE2, 1.dp())
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
        }
        dialogLayout.addView(nameInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12.dp() })

        dialogLayout.addView(TextView(this).apply {
            text = "グループ / 所属"
            setTextColor(GOLD)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        })
        val groupInput = EditText(this).apply {
            setText(existing?.group ?: "")
            setTextColor(CREAM)
            setHintTextColor(Color.argb(100, 255, 244, 230))
            hint = "例: 〇〇プロジェクト"
            textSize = 15f
            background = roundRect(SURF2, 8f.dp(), LINE2, 1.dp())
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
        }
        dialogLayout.addView(groupInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12.dp() })

        dialogLayout.addView(TextView(this).apply {
            text = "短い説明"
            setTextColor(GOLD)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 6.dp())
        })
        val descInput = EditText(this).apply {
            setText(existing?.desc ?: "")
            setTextColor(CREAM)
            setHintTextColor(Color.argb(100, 255, 244, 230))
            hint = "例: いつも応援してる推し"
            textSize = 15f
            background = roundRect(SURF2, 8f.dp(), LINE2, 1.dp())
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
        }
        dialogLayout.addView(descInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12.dp() })

        val builder = AlertDialog.Builder(this)
            .setTitle(if (existing != null) "推しを編集" else "新しい推しを登録")
            .setView(dialogLayout)
            .setPositiveButton("保存") { _, _ ->
                val emoji = emojiInput.text.toString().trim().ifBlank { "💖" }
                val name = nameInput.text.toString().trim()
                val group = groupInput.text.toString().trim()
                val desc = descInput.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, "名前を入力してください", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existing != null) {
                    existing.emoji = emoji
                    existing.name = name
                    existing.group = group
                    existing.desc = desc
                } else {
                    oshiList.add(OshiData(emoji = emoji, name = name, group = group, desc = desc))
                }
                saveData()
                renderOshiList()
            }
            .setNegativeButton("キャンセル", null)

        if (existing != null) {
            builder.setNeutralButton("削除") { _, _ ->
                oshiList.remove(existing)
                saveData()
                renderOshiList()
                Toast.makeText(this, "推しを削除しました", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    // 天気取得
    private suspend fun fetchWeather() {
        withContext(Dispatchers.IO) {
            try {
                val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=35.6762&longitude=139.6503&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto&forecast_days=1"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()
                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val daily = json.getJSONObject("daily")
                val w = WeatherInfo(
                    temp = current.getDouble("temperature_2m").toInt(),
                    tempMax = daily.getJSONArray("temperature_2m_max").getDouble(0).toInt(),
                    tempMin = daily.getJSONArray("temperature_2m_min").getDouble(0).toInt(),
                    code = daily.getJSONArray("weather_code").getInt(0),
                    condition = weatherCodeToJP(daily.getJSONArray("weather_code").getInt(0)),
                    location = "東京"
                )
                withContext(Dispatchers.Main) {
                    weatherData = w
                    renderWeather()
                    updateBubble()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    renderWeather()
                }
            }
        }
    }

    // ニュース取得
    private suspend fun fetchNews() {
        withContext(Dispatchers.IO) {
            try {
                val rssUrl = "https://www.nhk.or.jp/rss/news/cat0.xml"
                val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=${java.net.URLEncoder.encode(rssUrl, "UTF-8")}&count=10"
                val url = URL(apiUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()
                val json = JSONObject(response)
                if (json.optString("status") == "ok") {
                    val items = json.getJSONArray("items")
                    val newsList = mutableListOf<NewsItem>()
                    for (i in 0 until minOf(items.length(), 10)) {
                        val item = items.getJSONObject(i)
                        val title = item.optString("title", "").trim()
                            .replace(Regex("<[^>]+>"), "")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                        val link = item.optString("link", "")
                        if (title.isNotBlank()) {
                            newsList.add(NewsItem(title, link))
                        }
                    }
                    withContext(Dispatchers.Main) {
                        newsItems = newsList
                        renderNews()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    renderNews()
                }
            }
        }
    }

    // 天気コード変換
    private fun weatherCodeToJP(code: Int): String {
        return when (code) {
            0 -> "快晴"; 1 -> "晴れ"; 2 -> "晴れ時々曇り"; 3 -> "曇り"
            45, 48 -> "霧"
            51, 53, 55 -> "霧雨"; 56, 57 -> "着氷性の霧雨"
            61 -> "小雨"; 63 -> "雨"; 65 -> "大雨"
            66, 67 -> "着氷性の雨"
            71 -> "小雪"; 73 -> "雪"; 75 -> "大雪"; 77 -> "霰"
            80, 81, 82 -> "にわか雨"
            85, 86 -> "にわか雪"
            95 -> "雷雨"; 96, 99 -> "雷雨と雹"
            else -> "不明"
        }
    }

    private fun weatherCodeToEmoji(code: Int): String {
        return when {
            code == 0 -> "☀️"
            code <= 2 -> "🌤"
            code == 3 -> "☁️"
            code in 45..48 -> "🌫"
            code in 51..67 -> "🌧"
            code in 71..77 -> "❄️"
            code in 80..82 -> "🌦"
            code in 85..86 -> "🌨"
            code >= 95 -> "⛈"
            else -> "🌤"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }
}