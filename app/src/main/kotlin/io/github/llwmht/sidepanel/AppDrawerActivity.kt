package io.github.llwmht.sidepanel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class LaunchableApp(val label: String, val pkg: String, val icon: Drawable)

/** Apps with a launcher entry, this app excluded, sorted by label. */
fun launchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(main, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { LaunchableApp(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
        .sortedBy { it.label.lowercase() }
}

/** Native app drawer: works even when the web layer does not. */
class AppDrawerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apps = launchableApps(this)
        val size = (resources.displayMetrics.density * 56).toInt()
        val grid = GridView(this).apply {
            setBackgroundColor(Color.BLACK)
            columnWidth = size * 2
            numColumns = GridView.AUTO_FIT
            verticalSpacing = size / 3
            setPadding(size / 2, size / 2, size / 2, size / 2)
            adapter = object : BaseAdapter() {
                override fun getCount() = apps.size
                override fun getItem(position: Int) = apps[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val app = apps[position]
                    return LinearLayout(this@AppDrawerActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        addView(ImageView(context).apply { setImageDrawable(app.icon) }, LinearLayout.LayoutParams(size, size))
                        addView(TextView(context).apply {
                            text = app.label
                            setTextColor(Color.WHITE)
                            gravity = Gravity.CENTER
                            maxLines = 2
                        })
                    }
                }
            }
            setOnItemClickListener { _, _, position, _ ->
                packageManager.getLaunchIntentForPackage(apps[position].pkg)?.let { startActivity(it) }
            }
        }
        val edit = Button(this).apply {
            text = "Edit from another device"
            setOnClickListener { startActivity(Intent(this@AppDrawerActivity, EditorAccessActivity::class.java)) }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            addView(edit, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        })
    }
}
