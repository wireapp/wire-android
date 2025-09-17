/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.settings.about.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library
import com.wire.android.model.Clickable
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY

@Composable
fun WireLibraries(
    libraries: List<Library>,
    onLibraryClick: (Library) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    header: (LazyListScope.() -> Unit)? = null
) {

    LazyColumn(
        modifier,
        verticalArrangement = Arrangement.Center,
        state = lazyListState,
        contentPadding = contentPadding
    ) {
        header?.invoke(this)
        libraryItems(
            libraries,
            onLibraryClick
        )
    }
}

inline fun LazyListScope.libraryItems(
    libraries: List<Library>,
    crossinline onLibraryClick: ((Library) -> Unit),
) {
    items(libraries) { library ->
        LibraryItem(
            library.name,
            library.owner,
            onClick = {
                onLibraryClick.invoke(library)
            }
        )
    }
}

@Composable
fun LibraryItem(
    libName: String,
    libAuthor: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        modifier = modifier
            .wrapContentHeight()
            .padding(start = dimensions().spacing8x),
        title = {
            Text(
                style = MaterialTheme.wireTypography.title02,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = libName,
            )
        },
        subtitle = {
            Text(
                modifier = Modifier.wrapContentHeight(),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                text = libAuthor
            )
        },
        clickable = Clickable(enabled = true, onClick = onClick)
    )
}

val Library.owner: String
    get() = organization?.name ?: developers.takeIf { it.isNotEmpty() }?.map { it.name }?.joinToString(", ") ?: String.EMPTY

@Preview
@Composable
fun LibraryItemPreview() {
    LibraryItem(
        libName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                " Mauris et dui a erat tempus convallis id nec nunc." +
                " Cras vehicula quis massa non sagittis.",
        libAuthor = "Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                " Mauris et dui a erat tempus convallis id nec nunc." +
                " Cras vehicula quis massa non sagittis." +
                " Mauris convallis arcu non tellus facilisis ullamcorper." +
                " Morbi massa turpis, vulputate sit amet urna eget, scelerisque efficitur neque." +
                " Nam rutrum, ante eu aliquam elementum, urna neque fermentum leo, vel commodo lectus purus et nulla." +
                " In laoreet sem viverra orci pulvinar ultricies. Fusce porta ultrices ipsum eget convallis.",
        onClick = {}
    )
}
