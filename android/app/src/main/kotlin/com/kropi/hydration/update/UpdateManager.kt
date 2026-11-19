package com.kropi.hydration.update

import com.kropi.hydration.R
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Aktualizacje z poziomu aplikacji (jak w Szpili): ostatnie wydanie GitHuba to kanał,
 * APK ląduje w cache, przechodzi kontrolę (SHA-256 z GitHuba, nazwa pakietu, nowszy
 * versionCode, ten sam podpis) i dopiero wtedy trafia do systemowego instalatora.
 * Wszystko blokujące — wołać z wątku w tle.
 *
 * Wymaga INTERNET (tylko do GitHuba) i REQUEST_INSTALL_PACKAGES.
 */
object UpdateManager {
    private const val PREFS = "kropi_update"
    private const val KEY_CHECKED = "checked_at"
    private const val KEY_FOUND = "found"
    private const val KEY_NOTIFIED = "notified"

    /** Tło pyta GitHuba najwyżej tak często (każdy tick workera może to wywołać). */
    const val CHECK_EVERY_MS = 6L * 60 * 60 * 1000

    private fun prefs(c: Context): SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Ostatnio sprawdzone wydanie, jeśli nowsze niż zainstalowane — do pokazania bez sieci. */
    fun hint(c: Context): String {
        val found = prefs(c).getString(KEY_FOUND, "").orEmpty()
        return if (found.isNotEmpty() && UpdateLogic.compare(found, installedVersionName(c)) > 0) found else ""
    }

    fun lastCheckedMs(c: Context): Long = prefs(c).getLong(KEY_CHECKED, 0L)

    fun installedVersionName(c: Context): String =
        runCatching { c.packageManager.getPackageInfo(c.packageName, 0).versionName }.getOrNull().orEmpty()

    private fun codeOf(pi: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()

    private fun installedVersionCode(c: Context): Long = codeOf(c.packageManager.getPackageInfo(c.packageName, 0))

    /**
     * Pyta GitHuba o ostatnie wydanie. null = nie ma użytecznego wydania (szkic,
     * pre-release, brak APK); błąd sieci albo HTTP rzuca wyjątek, żeby UI mogło
     * odróżnić „jesteś na bieżąco" od „nie udało się sprawdzić".
     */
    fun fetchLatest(): UpdateLogic.Release? {
        val con = URL(UpdateLogic.RELEASES_API).openConnection() as HttpURLConnection
        try {
            con.connectTimeout = 10_000
            con.readTimeout = 10_000
            con.setRequestProperty("Accept", "application/vnd.github+json")
            if (con.responseCode != 200) throw IOException("HTTP ${con.responseCode}")
            val buf = ByteArrayOutputStream()
            con.inputStream.use { input ->
                val chunk = ByteArray(8192)
                while (buf.size() < 512 * 1024) {
                    val n = input.read(chunk)
                    if (n <= 0) break
                    buf.write(chunk, 0, n)
                }
            }
            return UpdateLogic.parseRelease(JSONObject(buf.toString("UTF-8")))
        } finally {
            con.disconnect()
        }
    }

    /** Ręczne sprawdzenie: wydanie nowsze od zainstalowanego albo null (na bieżąco). Błąd sieci rzuca wyjątek. */
    fun checkNow(c: Context): UpdateLogic.Release? {
        val release = fetchLatest()
        prefs(c).edit().putLong(KEY_CHECKED, System.currentTimeMillis()).apply()
        val newer = release?.takeIf { UpdateLogic.compare(it.version, installedVersionName(c)) > 0 }
        prefs(c).edit().putString(KEY_FOUND, newer?.version.orEmpty()).apply()
        return newer
    }

    /**
     * Sprawdzenie w tle (najwyżej co 6 h): zapamiętuje nowszą wersję i zwraca ją
     * tylko za pierwszym razem, żeby powiadomić raz na wersję. null = nic nowego.
     */
    fun backgroundCheck(c: Context): String? {
        val p = prefs(c)
        val now = System.currentTimeMillis()
        val last = p.getLong(KEY_CHECKED, 0L)
        if (last > 0 && now >= last && now - last < CHECK_EVERY_MS) return null
        val newer = runCatching { checkNow(c) }.getOrNull() ?: return null
        if (newer.version == p.getString(KEY_NOTIFIED, "")) return null
        p.edit().putString(KEY_NOTIFIED, newer.version).apply()
        return newer.version
    }

    private fun updatesDir(c: Context): File = File(c.cacheDir, "updates").also { it.mkdirs() }

    /** Pobiera do cache/updates/kropi-update.apk (starsze pobrania znikają). */
    fun download(c: Context, release: UpdateLogic.Release, onProgress: (received: Long, total: Long) -> Unit): File {
        require(UpdateLogic.allowedUrl(release.apkUrl)) { "Not a Kropi release APK" }
        updatesDir(c).listFiles()?.forEach { it.delete() }
        val out = File(updatesDir(c), "kropi-update.apk")
        val conn = URL(release.apkUrl).openConnection() as HttpURLConnection
        try {
            conn.instanceFollowRedirects = true // GitHub przekierowuje do swojego magazynu (https → https)
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/octet-stream")
            check(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                out.outputStream().use { os ->
                    val buf = ByteArray(64 * 1024)
                    var received = 0L
                    var lastReport = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        os.write(buf, 0, n)
                        received += n
                        if (received - lastReport >= 256 * 1024) {
                            lastReport = received
                            onProgress(received, total)
                        }
                    }
                    onProgress(received, total)
                }
            }
        } finally {
            conn.disconnect()
        }
        return out
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** null, gdy APK można zainstalować; inaczej krótki powód po polsku. */
    fun verify(c: Context, apk: File, expectedSha: String): String? = try {
        val want = UpdateLogic.normalizeSha(expectedSha)
        val pm = c.packageManager
        // GET_SIGNING_CERTIFICATES jest od API 28; wcześniej podpisy siedzą w GET_SIGNATURES.
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(apk.absolutePath, flags)
        when {
            want.isNotEmpty() && want != sha256(apk) -> c.getString(R.string.update_err_checksum)
            archive == null -> c.getString(R.string.update_err_not_app)
            archive.packageName != c.packageName -> c.getString(R.string.update_err_other_app)
            codeOf(archive) <= installedVersionCode(c) -> c.getString(R.string.update_err_same_version)
            else -> {
                val installed = pm.getPackageInfo(c.packageName, flags)
                val a = signers(archive)
                if (a.isEmpty() || signers(installed) != a) c.getString(R.string.update_err_signature) else null
            }
        }
    } catch (e: Exception) {
        c.getString(R.string.update_err_verify, e.message ?: "")
    }

    private fun signers(pi: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val info = pi.signingInfo ?: return emptySet()
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION") pi.signatures
        }
        return sigs?.map { it.toByteArray().contentToString() }?.toSet().orEmpty()
    }

    fun canInstall(c: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || c.packageManager.canRequestPackageInstalls()

    /** Systemowy ekran „Instaluj nieznane aplikacje" dla Kropi. */
    fun openInstallPermission(c: Context) {
        c.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${c.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Oddaje pobrany APK systemowemu instalatorowi (sam poprosi użytkownika o potwierdzenie). */
    fun install(c: Context, apk: File) {
        val uri = FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", apk)
        c.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
