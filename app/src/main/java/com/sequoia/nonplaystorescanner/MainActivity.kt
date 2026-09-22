package com.sequoia.nonplaystorescanner

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sequoia.nonplaystorescanner.databinding.ActivityMainBinding
import com.sequoia.nonplaystorescanner.databinding.ItemAppBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadApps()
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(packageManager, mutableListOf()) { appInfo ->
            showUninstallDialog(appInfo)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadApps() {
        val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val nonPlayStoreApps = allApps.filter { app ->
            isNonPlayStoreApp(app) && (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
        adapter.updateApps(nonPlayStoreApps)
        binding.emptyView.visibility = if (nonPlayStoreApps.isEmpty()) View.VISIBLE else View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun isNonPlayStoreApp(appInfo: ApplicationInfo): Boolean {
        val installerPackage = packageManager.getInstallerPackageName(appInfo.packageName)
        return installerPackage != "com.android.vending"
    }

    private fun showUninstallDialog(appInfo: ApplicationInfo) {
        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val installer = getInstallerName(packageManager, appInfo)
        AlertDialog.Builder(this)
            .setTitle("Uninstall $appName?")
            .setMessage("This app was installed from $installer. Uninstall it?")
            .setPositiveButton("Uninstall") { _, _ ->
                uninstallApp(appInfo.packageName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    companion object {
        fun getInstallerName(pm: PackageManager, appInfo: ApplicationInfo): String {
            val installerPackage = pm.getInstallerPackageName(appInfo.packageName)
            return when (installerPackage) {
                "com.android.vending" -> "Google Play Store"
                "com.android.packageinstaller" -> "System / Direct APK"
                "com.google.android.packageinstaller" -> "System / Direct APK"
                "com.sec.android.app.samsungapps" -> "Samsung Galaxy Store"
                "com.amazon.venezia" -> "Amazon Appstore"
                "com.huawei.appmarket" -> "Huawei AppGallery"
                "com.vivo.appstore" -> "Vivo App Store"
                "com.oppo.market" -> "OPPO App Store"
                "com.xiaomi.market" -> "Xiaomi GetApps"
                "com.oneplus.store" -> "OnePlus Store"
                "com.realme.store" -> "Realme App Market"
                "com.transsion.store" -> "Transsion App Store"
                null -> "Unknown / Side-loaded"
                else -> installerPackage
            }
        }
    }
}

class AppAdapter(
    private val pm: PackageManager,
    private var apps: List<ApplicationInfo>,
    private val onClick: (ApplicationInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    inner class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appInfo: ApplicationInfo) {
            binding.appIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
            binding.appName.text = pm.getApplicationLabel(appInfo)
            binding.installerName.text = "From: ${MainActivity.getInstallerName(pm, appInfo)}"
            binding.root.setOnClickListener { onClick(appInfo) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount() = apps.size

    fun updateApps(newApps: List<ApplicationInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}