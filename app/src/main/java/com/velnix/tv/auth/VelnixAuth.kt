package com.velnix.tv.auth

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class AuthResult(val email: String, val message: String)

object VelnixAuth {
    private const val BASE_URL = "https://shungcjflcjwubdlhyzq.supabase.co"
    private const val API_KEY = "sb_publishable_stitJ7MdKnH573UQ5GRbYg_3XdSgEGo"
    private const val PREFS = "velnix_auth_session"
    private const val ACCESS = "access_token"
    private const val REFRESH = "refresh_token"
    private const val EMAIL = "email"
    private const val EXPIRES = "expires_at"

    private fun request(path: String, method: String, body: JSONObject? = null, bearer: String? = null): JSONObject {
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20000
            readTimeout = 20000
            setRequestProperty("apikey", API_KEY)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) connection.outputStream.use {
                it.write(body.toString().toByteArray(StandardCharsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (raw.isBlank()) JSONObject() else JSONObject(raw)
            if (code !in 200..299) {
                val message = json.optString("msg").ifBlank {
                    json.optString("message").ifBlank {
                        json.optString("error_description").ifBlank { json.optString("error").ifBlank { "HTTP $code" } }
                    }
                }
                throw IllegalStateException(message)
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    suspend fun signUp(context: Context, email: String, password: String): AuthResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val body = JSONObject().put("email", email.trim()).put("password", password)
            val result = request("/auth/v1/signup", "POST", body)
            val token = result.optString("access_token")
            if (token.isNotBlank()) saveSession(context, result, email)
            AuthResult(email.trim(), if (token.isBlank()) "تم إنشاء الحساب. تحقق من بريدك الإلكتروني لتأكيده ثم سجّل الدخول." else "تم إنشاء الحساب وتسجيل الدخول.")
        }

    suspend fun signIn(context: Context, email: String, password: String): AuthResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val body = JSONObject().put("email", email.trim()).put("password", password)
            val result = request("/auth/v1/token?grant_type=password", "POST", body)
            saveSession(context, result, email)
            AuthResult(email.trim(), "تم تسجيل الدخول بنجاح.")
        }

    suspend fun sendPasswordReset(email: String): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val body = JSONObject().put("email", email.trim())
            request("/auth/v1/recover", "POST", body)
            "إذا كان البريد مسجلاً، ستصلك رسالة استعادة كلمة المرور."
        }

    suspend fun updateDisplayName(context: Context, displayName: String): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val token = prefs.getString(ACCESS, null) ?: throw IllegalStateException("سجّل الدخول أولاً.")
            val body = JSONObject().put("data", JSONObject().put("display_name", displayName.trim()))
            request("/auth/v1/user", "PUT", body, token)
            "تم تحديث الاسم في حسابك."
        }

    suspend fun signOut(context: Context) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString(ACCESS, null)
        if (!token.isNullOrBlank()) runCatching { request("/auth/v1/logout", "POST", JSONObject(), token) }
        prefs.edit().clear().apply()
    }

    suspend fun currentEmail(context: Context): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString(ACCESS, null)
        val refresh = prefs.getString(REFRESH, null)
        if (token.isNullOrBlank()) return@withContext null
        val expiresAt = prefs.getLong(EXPIRES, 0L)
        if (expiresAt > 0L && expiresAt < System.currentTimeMillis() / 1000 + 60 && !refresh.isNullOrBlank()) {
            runCatching {
                val renewed = request("/auth/v1/token?grant_type=refresh_token", "POST", JSONObject().put("refresh_token", refresh))
                saveSession(context, renewed, prefs.getString(EMAIL, "").orEmpty())
            }.onFailure { return@withContext null }
        }
        prefs.getString(EMAIL, null)
    }

    private fun saveSession(context: Context, result: JSONObject, fallbackEmail: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val user = result.optJSONObject("user")
        val expiresIn = result.optLong("expires_in", 3600L)
        prefs.edit()
            .putString(ACCESS, result.optString("access_token"))
            .putString(REFRESH, result.optString("refresh_token"))
            .putString(EMAIL, user?.optString("email")?.takeIf { !it.isNullOrBlank() } ?: fallbackEmail.trim())
            .putLong(EXPIRES, result.optLong("expires_at", System.currentTimeMillis() / 1000 + expiresIn))
            .apply()
    }
}
