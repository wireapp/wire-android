/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
import com.wire.android.testSupport.BuildConfig
import org.json.JSONObject
import java.io.File

class CredentialManager{

    companion object{
fun getSecretFieldValue(
    parentKey: String,
    fieldKey: String
): String? {

    return try {
       return getBuildConfigValue("${parentKey}_$fieldKey")
    } catch (e: Exception) {
        println("❌ Error reading secret [$parentKey/$fieldKey]: ${e.message}")
        null
    }
}

fun getBuildConfigValue(fieldName: String): String? {
    return try {
        val buildConfigClass = Class.forName("com.wire.android.testSupport.BuildConfig")
        val field = buildConfigClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.get(null)?.toString()
    } catch (e: Exception) {
        println("❌ Could not access BuildConfig field '$fieldName': ${e.message}")
        null
    }
}

    }


}
