package com.gologlu.detracktor.runtime.android.repo

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gologlu.detracktor.application.error.AppResult
import com.gologlu.detracktor.application.error.ConfigInvalidFieldError
import com.gologlu.detracktor.application.error.ConfigParseError
import com.gologlu.detracktor.application.error.AppConfigException
import com.gologlu.detracktor.application.repo.SettingsRepository
import com.gologlu.detracktor.application.types.AppSettings
import com.gologlu.detracktor.application.types.Domains
import com.gologlu.detracktor.application.types.HostCond
import com.gologlu.detracktor.application.types.Pattern
import com.gologlu.detracktor.application.types.Subdomains
import com.gologlu.detracktor.application.types.ThenBlock
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.application.types.WarningSettings
import com.gologlu.detracktor.application.types.SensitiveMergeMode
import com.gologlu.detracktor.application.types.WhenBlock
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/** Android implementation of SettingsRepository. */
class AndroidSettingsRepository(
    private val context: Context,
    private val gson: Gson = Gson(),
    private val defaultsAssetName: String = "default_settings.json",
    private val userFileName: String = "user_settings.json"
) : SettingsRepository {

    override suspend fun readUserSettings(): AppResult<AppSettings?> {
        return try {
            val file = File(context.filesDir, userFileName)
            if (!file.exists()) return AppResult.success(null)
            FileReader(file).use { reader ->
                val json = JsonParser.parseReader(reader).asJsonObject
                val settings = dtoToApp(json)
                AppResult.success(settings)
            }
        } catch (e: AppConfigException) {
            AppResult.failure(e.err)
        } catch (e: Exception) {
            AppResult.failure(ConfigParseError("Failed to read user settings: ${e.message}", e))
        }
    }

    override suspend fun writeUserSettings(settings: AppSettings): AppResult<Unit> {
        return try {
            val file = File(context.filesDir, userFileName)
            val tmp = File(context.filesDir, "$userFileName.tmp")
            FileWriter(tmp, false).use { writer ->
                val json = appToDto(settings)
                gson.toJson(json, writer)
            }
            if (!tmp.renameTo(file)) {
                // Fallback: attempt delete+rename
                file.delete()
                if (!tmp.renameTo(file)) {
                    return AppResult.failure(ConfigParseError("Failed to persist user settings (rename)."))
                }
            }
            AppResult.success(Unit)
        } catch (e: AppConfigException) {
            AppResult.failure(e.err)
        } catch (e: Exception) {
            AppResult.failure(ConfigParseError("Failed to write user settings: ${e.message}", e))
        }
    }

    override suspend fun clearUserSettings(): AppResult<Unit> {
        return try {
            val file = File(context.filesDir, userFileName)
            if (file.exists()) file.delete()
            AppResult.success(Unit)
        } catch (e: AppConfigException) {
            AppResult.failure(e.err)
        } catch (e: Exception) {
            AppResult.failure(ConfigParseError("Failed to clear user settings: ${e.message}", e))
        }
    }

    override suspend fun readDefaultSettings(): AppResult<AppSettings> {
        return try {
            context.assets.open(defaultsAssetName).bufferedReader().use { br ->
                val json = JsonParser.parseReader(br).asJsonObject
                val settings = dtoToApp(json)
                AppResult.success(settings)
            }
        } catch (e: AppConfigException) {
            AppResult.failure(e.err)
        } catch (e: Exception) {
            AppResult.failure(ConfigParseError("Failed to read default settings: ${e.message}", e))
        }
    }

    // --------- DTO mapping (JsonObject <-> AppSettings) ---------

    private fun requireInt(obj: JsonObject, name: String): Int {
        val el = obj.get(name) ?: throw AppConfigException(ConfigInvalidFieldError(name, "$name is required"))
        if (!el.isJsonPrimitive || !el.asJsonPrimitive.isNumber) {
            throw AppConfigException(ConfigInvalidFieldError(name, "$name must be an integer"))
        }
        return el.asInt
    }

    private fun getObject(obj: JsonObject, name: String): JsonObject? =
        obj.getAsJsonObject(name)

    private fun getArray(obj: JsonObject, name: String): JsonArray? =
        obj.getAsJsonArray(name)

    private fun getString(obj: JsonObject, name: String): String? =
        obj.getAsJsonPrimitive(name)?.asString

    private fun toStringList(arr: JsonArray): List<String> =
        arr.map { it.asString }

    private fun dtoToApp(root: JsonObject): AppSettings {
        val version = requireInt(root, "version")
        if (version != AppSettings.VERSION.toInt()) {
            throw AppConfigException(ConfigInvalidFieldError("version", "unsupported settings version: $version"))
        }
        val sitesArray = getArray(root, "sites") ?: throw AppConfigException(ConfigInvalidFieldError("sites", "sites array is required"))
        val sites = sitesArray.mapIndexed { idx, el -> siteFromJson(el.asJsonObject, idx) }
        return AppSettings(
            sites = sites,
            version = AppSettings.VERSION
        )
    }

    private fun siteFromJson(obj: JsonObject, index: Int): UrlRule {
        val whenObj = getObject(obj, "when") ?: throw AppConfigException(ConfigInvalidFieldError("sites[$index].when", "missing when"))
        val thenObj = getObject(obj, "then") ?: throw AppConfigException(ConfigInvalidFieldError("sites[$index].then", "missing then"))

        val hostObj = getObject(whenObj, "host") ?: throw AppConfigException(ConfigInvalidFieldError("sites[$index].when.host", "missing host"))
        val domainsEl: JsonElement = hostObj.get("domains") ?: throw AppConfigException(ConfigInvalidFieldError("sites[$index].when.host.domains", "missing domains"))
        val domains: Domains = when {
            domainsEl.isJsonPrimitive && domainsEl.asJsonPrimitive.isString -> {
                val s = domainsEl.asString
                if (s != "*") throw AppConfigException(ConfigInvalidFieldError("sites[$index].when.host.domains", "domains string must be \"*\""))
                Domains.Any
            }
            domainsEl.isJsonArray -> {
                val arr = domainsEl.asJsonArray
                val values = toStringList(arr)
                if (values.isEmpty()) throw AppConfigException(ConfigInvalidFieldError("sites[$index].when.host.domains", "domains array cannot be empty"))
                Domains.ListOf(values)
            }
            else -> throw AppConfigException(ConfigInvalidFieldError("sites[$index].when.host.domains", "invalid domains type"))
        }

        val subdomains: Subdomains? = if (hostObj.has("subdomains")) {
            val sEl = hostObj.get("subdomains")
            when {
                sEl.isJsonPrimitive && sEl.asJsonPrimitive.isString -> when (val s = sEl.asString) {
                    "*" -> Subdomains.Any
                    "" -> Subdomains.None
                    else -> throw AppConfigException(ConfigInvalidFieldError("sites[$index].when.host.subdomains", "invalid subdomains string: $s"))
                }
                sEl.isJsonArray -> {
                    val labels = toStringList(sEl.asJsonArray)
                    if (labels.isEmpty()) Subdomains.None else Subdomains.OneOf(labels)
                }
                else -> throw AppConfigException(ConfigInvalidFieldError("sites[$index].when.host.subdomains", "invalid subdomains type"))
            }
        } else null

        val schemes = getArray(whenObj, "schemes")?.let { toStringList(it).map { s -> s.lowercase() } }
        val whenBlock = WhenBlock(host = HostCond(domains = domains, subdomains = subdomains), schemes = schemes)

        val removeArr = getArray(thenObj, "remove")
        val remove = if (removeArr == null) {
            // Warn-only rule is allowed: empty removal set
            emptyList()
        } else {
            toStringList(removeArr).map { Pattern(it) } // Pattern validates with Globby
        }

        val warn = getObject(thenObj, "warn")?.let { warningFromJson(it) }
        val thenBlock = ThenBlock(remove = remove, warn = warn)

        val metadata = getObject(obj, "metadata")?.let { jsonObjectToMap(it) }
        return UrlRule(when_ = whenBlock, then = thenBlock, metadata = metadata)
    }

    private fun warningFromJson(obj: JsonObject): WarningSettings {
        val version = obj.getAsJsonPrimitive("version")?.asInt ?: WarningSettings.VERSION.toInt()
        if (version > WarningSettings.VERSION.toInt() || version < 1) {
            throw AppConfigException(ConfigInvalidFieldError("warnings.version", "unsupported warnings version: $version"))
        }
        val warnOnCreds = obj.getAsJsonPrimitive("warnOnEmbeddedCredentials")?.asBoolean
        val sens = obj.getAsJsonArray("sensitiveParams")?.let { toStringList(it) }
        val mergeMode = obj.getAsJsonPrimitive("sensitiveMerge")?.asString?.let { value ->
            when (value) {
                "UNION" -> SensitiveMergeMode.UNION
                "REPLACE" -> SensitiveMergeMode.REPLACE
                else -> null
            }
        }
        return WarningSettings(
            warnOnEmbeddedCredentials = warnOnCreds,
            sensitiveParams = sens,
            sensitiveMerge = mergeMode,
            version = WarningSettings.VERSION
        )
    }

    private fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        for ((k, v) in obj.entrySet()) {
            map[k] = jsonElementToKotlin(v)
        }
        return map
    }

    private fun jsonElementToKotlin(el: JsonElement): Any? = when {
        el.isJsonNull -> null
        el.isJsonPrimitive -> {
            val p = el.asJsonPrimitive
            when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> p.asNumber
                p.isString -> p.asString
                else -> p.asString
            }
        }
        el.isJsonArray -> el.asJsonArray.map { jsonElementToKotlin(it) }
        el.isJsonObject -> jsonObjectToMap(el.asJsonObject)
        else -> null
    }

    private fun appToDto(settings: AppSettings): JsonObject {
        val root = JsonObject()
        root.addProperty("version", settings.version.toInt())
        // no root warnings in v1 schema (moved to per-rule)

        // sites
        val sitesArr = JsonArray()
        settings.sites.forEach { site ->
            val siteObj = JsonObject()
            val whenObj = JsonObject()
            val hostObj = JsonObject()
            when (val d = site.when_.host.domains) {
                is Domains.Any -> hostObj.addProperty("domains", "*")
                is Domains.ListOf -> {
                    val arr = JsonArray(); d.values.forEach { arr.add(it) }; hostObj.add("domains", arr)
                }
            }
            site.when_.host.subdomains?.let { s ->
                when (s) {
                    Subdomains.Any -> hostObj.addProperty("subdomains", "*")
                    Subdomains.None -> hostObj.addProperty("subdomains", "")
                    is Subdomains.OneOf -> {
                        val arr = JsonArray(); s.labels.forEach { arr.add(it) }; hostObj.add("subdomains", arr)
                    }
                }
            }
            whenObj.add("host", hostObj)
            site.when_.schemes?.let { sch ->
                val arr = JsonArray(); sch.forEach { arr.add(it) }; whenObj.add("schemes", arr)
            }
            siteObj.add("when", whenObj)

            val thenObj = JsonObject()
            val remArr = JsonArray(); site.then.remove.forEach { remArr.add(it.pattern) }
            thenObj.add("remove", remArr)
            site.then.warn?.let { w ->
                val wObj = JsonObject().apply {
                    w.warnOnEmbeddedCredentials?.let { addProperty("warnOnEmbeddedCredentials", it) }
                    w.sensitiveParams?.let { sp ->
                        val arr = JsonArray(); sp.forEach { arr.add(it) }; add("sensitiveParams", arr)
                    }
                    w.sensitiveMerge?.let { addProperty("sensitiveMerge", it.name) }
                    addProperty("version", w.version.toInt())
                }
                thenObj.add("warn", wObj)
            }
            siteObj.add("then", thenObj)

            site.metadata?.let { meta ->
                val metaObj = gson.toJsonTree(meta).asJsonObject
                siteObj.add("metadata", metaObj)
            }
            sitesArr.add(siteObj)
        }
        root.add("sites", sitesArr)
        return root
    }
}


