package com.wire.android.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R

@Composable
fun UnderConstructionScreen(screenName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_settings),
            contentDescription = "under construction",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.Black),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(48.dp)
                .height(48.dp)
        )
        Text(
            text = "$screenName is under construction",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
    }
}

@Preview(showBackground = false)
@Composable
fun UnderConstructionScreenPreview() {
    UnderConstructionScreen("testing")
}
