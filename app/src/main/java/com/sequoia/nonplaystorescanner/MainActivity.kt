package com.sequoia.nonplaystorescanner

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sequoia.nonplaystorescanner.databinding.ActivityMainBinding
import com.sequoia.nonplaystorescanner.databinding.ItemAppBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppAdapter
    private val packageManager by lazy { packageManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadApps()
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(mutableListOf()) { appInfo ->
            showUninstallDialog(appInfo)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadApps() {
        val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val nonPlayStoreApps = allApps.filter { app ->
            isNonPlayStoreApp(app)
        }
        adapter.updateApps(nonPlayStoreApps)
        binding.emptyView.isVisible = nonPlayStoreApps.isEmpty()
        binding.progressBar.isVisible = false
    }

    private fun isNonPlayStoreApp(appInfo: ApplicationInfo): Boolean {
        val installerPackage = packageManager.getInstallerPackageName(appInfo.packageName)
        return installerPackage != "com.android.vending"
    }

    private fun showUninstallDialog(appInfo: ApplicationInfo) {
        AlertDialog.Builder(this)
            .setTitle("Uninstall ${appInfo.loadLabel(packageManager)}?")
            .setMessage("This app was installed from ${getInstallerName(appInfo)}. Uninstall it?")
            .setPositiveButton("Uninstall") { _, _ ->
                uninstallApp(appInfo.packageName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getInstallerName(appInfo: ApplicationInfo): String {
        val installerPackage = packageManager.getInstallerPackageName(appInfo.packageName)
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
            else -> installerPackage ?: "Unknown / Side-loaded"
        }
    }

    private fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}

class AppAdapter(
    private var apps: List<ApplicationInfo>,
    private val onClick: (ApplicationInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    inner class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appInfo: ApplicationInfo) {
            binding.appIcon.setImageDrawable(appInfo.loadIcon(packageManager))
            binding.appName.text = appInfo.loadLabel(packageManager)
            binding.installerName.text = "From: ${getInstallerName(appInfo)}"
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

private val PackageManager.getInstallerPackageName: (String) -> String?
    get() = { packageManager.getInstallerPackageName(it) }