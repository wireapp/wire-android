package com.wire.android.util.debug

import androidx.compose.runtime.staticCompositionLocalOf

//As a Beta user I don’t want to press on buttons that don’t have functions yet,
// because then I will think something is not working in the app.
//
//ACCEPTANCE CRITERIA
//
//Buttons to remove from the UI:
//Settings → App settings entry
//Settings → Backup and restore information entry
//Conversation view → + icon → Audio messages
//Conversation View → + icon → share location
//Conversation View → Aa icon
//Conversation View → :smile: icon
//Conversation View → GIF icon
//Conversation View → @ icon
//Conversation View → ping icon
//Conversation View → search icon
//User Profile → Edit icon
//long press on text → edit entry
//long press on images → edit
//long press on images → copy

// Those flags can be removed once we set all flags to true :)
object FeatureVisibilityFlags {
    const val AppSettings = false
    const val BackUpSettings = false
    const val AudioMessagesIcon = false
    const val ShareLocationIcon = false
    const val RichTextIcon = false
    const val EmojiIcon = false
    const val GifIcon = false
    const val PingIcon = false
    const val ConversationSearchIcon = false
    const val UserProfileEditIcon = false
    const val MessageEditIcon = false
}


val LocalFeatureVisibilityFlags = staticCompositionLocalOf { FeatureVisibilityFlags }
