package view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp


@Composable
fun TextArea(text: String) {
    val scroll = rememberScrollState(0)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textHeight = with(density) {
        maxOf(
            textMeasurer.measure(
                text = text,
                maxLines = 50,
                style = TextStyle(),
            ).size.height.toDp(),
            40.dp
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.onBackground)
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        BasicTextField(
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier
                .height(textHeight)
                .verticalScroll(scroll)
                .fillMaxWidth()
                .padding(8.dp),
            value = text,
            onValueChange = {}
        )
    }
}
