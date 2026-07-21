/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.util

import androidx.annotation.StringRes
import com.wire.android.ui.common.R

enum class SupportPage(
    val path: String,
    @StringRes val hardcodedUrlRes: Int
) {
    SUPPORT("", R.string.url_support),
    REPORT_MISUSE("report_misuse", R.string.url_report_misuse),
    CREATE_ACCOUNT("create_account", R.string.url_create_account_learn_more),
    DECRYPTION_FAILURE("decryption_failure", R.string.url_decryption_failure_learn_more),
    FEDERATION_SUPPORT("federation_support", R.string.url_federation_support),
    FILE_SHARING_RESTRICTED("file_sharing_restricted", R.string.url_file_sharing_restricted_learn_more),
    CONVERSATION_SEARCH("conversation_search", R.string.url_learn_about_conversation_search),
    OFFLINE_BACKENDS("offline_backends", R.string.url_message_details_offline_backends_learn_more),
    MLS("mls", R.string.url_mls_learn_more),
    REACTIONS("reactions", R.string.url_message_details_reactions_learn_more),
    READ_RECEIPTS("read_receipts", R.string.url_message_details_read_receipts_learn_more),
    CLIENT_FINGERPRINT("client_fingerprint", R.string.url_self_client_fingerprint_learn_more),
    CLIENT_VERIFICATION("client_verification", R.string.url_self_client_verification_learn_more),
    WELCOME_ANDROID("welcome_android", R.string.url_welcome_to_new_android),
    VERIFY_CONVERSATION("verify_conversation", R.string.url_why_verify_conversation),
    ADD_FAVORITES("add_favorites", R.string.url_how_to_add_favorites),
    ADD_FOLDERS("add_folders", R.string.url_how_to_add_folders),
    CREATE_CHANNEL("create_channel", R.string.url_create_channel_learn_more),
    CHANGE_EMAIL("change_email", R.string.url_change_email),
    DELETE_ACCOUNT("delete_account", R.string.url_delete_personal_account),
    E2EE("e2ee", R.string.url_system_message_learn_more_about_e2ee),
    E2EE_IDENTITY("e2ee_identity", R.string.url_e2ee_id_shield),
    ADD_APPS("add_apps", R.string.url_how_to_add_apps),
    SHARED_DRIVE("shared_drive", R.string.create_group_with_shared_drive_learn_more_url),
    CALL_QUALITY("call_quality", R.string.url_call_network_quality_learn_more),
    LEGAL_HOLD("legal_hold", R.string.url_legal_hold_learn_more),
    SEARCH("search", R.string.url_learn_about_search),
    CELLS_CONVERSATION("cells_conversation", R.string.empty_screen_learn_more_link_conversation),
    CELLS_ALL_FILES("cells_all_files", R.string.empty_screen_learn_more_link_all_files_screen)
}
