package com.velnix.tv

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun SupabaseAuthDialog(
    initialName: String,
    initialEmail: String,
    signedInEmail: String?,
    onAuthChanged: (String?) -> Unit,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail.ifBlank { signedInEmail.orEmpty() }) }
    var password by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("login") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }

    fun perform(action: suspend () -> String) {
        scope.launch {
            busy = true
            error = false
            message = null
            try { message = action() }
            catch (e: Exception) { error = true; message = e.message ?: "تعذر إكمال العملية." }
            finally { busy = false }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp), color = Color(0xFF17171B), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("حساب VELNIX", color = Color(0xFFF4FF00), fontSize = 21.sp, fontWeight = FontWeight.Black)
                if (signedInEmail != null) {
                    Text("مسجّل الدخول", color = Color(0xFF78D99A), fontWeight = FontWeight.Bold)
                    Text(signedInEmail, color = Color.White)
                    OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاسم الظاهر") })
                    Button(onClick = {
                        val cleanName = name.trim()
                        if (cleanName.isBlank()) {
                            error = true
                            message = "اكتب الاسم الظاهر أولاً."
                        } else {
                            perform {
                                val result = com.velnix.tv.auth.VelnixAuth.updateDisplayName(context, cleanName)
                                onSave(cleanName, signedInEmail)
                                result
                            }
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "جارٍ الحفظ..." else "حفظ الاسم") }
                    OutlinedButton(onClick = {
                        perform {
                            com.velnix.tv.auth.VelnixAuth.signOut(context)
                            onAuthChanged(null)
                            "تم تسجيل الخروج."
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("تسجيل الخروج") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        FilterChip(selected = mode == "login", onClick = { mode = "login"; message = null }, label = { Text("دخول") })
                        FilterChip(selected = mode == "signup", onClick = { mode = "signup"; message = null }, label = { Text("حساب جديد") })
                        FilterChip(selected = mode == "reset", onClick = { mode = "reset"; message = null }, label = { Text("استعادة") })
                    }
                    OutlinedTextField(value = email, onValueChange = { email = it.trim() }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("البريد الإلكتروني") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    if (mode != "reset") {
                        OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("كلمة المرور") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    }
                    Button(onClick = {
                        val e = email.trim()
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
                            error = true; message = "أدخل بريداً إلكترونياً صحيحاً."
                        } else if (mode != "reset" && password.length < 6) {
                            error = true; message = "كلمة المرور يجب أن تكون 6 أحرف على الأقل."
                        } else when (mode) {
                            "signup" -> perform {
                                val result = com.velnix.tv.auth.VelnixAuth.signUp(context, e, password)
                                val current = com.velnix.tv.auth.VelnixAuth.currentEmail(context)
                                if (current != null) onAuthChanged(current)
                                result.message
                            }
                            "reset" -> perform { com.velnix.tv.auth.VelnixAuth.sendPasswordReset(e) }
                            else -> perform {
                                val result = com.velnix.tv.auth.VelnixAuth.signIn(context, e, password)
                                onAuthChanged(result.email)
                                result.message
                            }
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4FF00), contentColor = Color.Black)) {
                        Text(if (busy) "جارٍ الاتصال..." else if (mode == "signup") "إنشاء الحساب" else if (mode == "reset") "إرسال رابط الاستعادة" else "تسجيل الدخول")
                    }
                }
                message?.let { Text(it, color = if (error) Color(0xFFFF7777) else Color(0xFFF4FF00), fontSize = 11.sp) }
                Divider(color = Color(0xFF34343A))
                Row(Modifier.fillMaxWidth().background(Color(0xFF24242A), RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("الخطة المجانية", color = Color(0xFFF4FF00), fontWeight = FontWeight.Bold)
                        Text("العضوية الأساسية", color = Color(0xFF9A9AA3), fontSize = 10.sp)
                    }
                    Text("نشطة", color = Color(0xFF78D99A), fontSize = 11.sp)
                }
                Text("الحسابات متصلة بـ Supabase. الاشتراكات المدفوعة والدفع الإلكتروني لم تُفعّل بعد.", color = Color(0xFF9A9AA3), fontSize = 10.sp)
                if (initialName.isNotBlank() || initialEmail.isNotBlank()) {
                    TextButton(onClick = onClear) { Text("مسح الملف المحلي", color = Color(0xFFFF7777)) }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) { Text("إغلاق") }
            }
        }
    }
}
