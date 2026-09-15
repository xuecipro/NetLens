package com.opensource.netlens.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.R
import com.opensource.netlens.data.model.Band
import com.opensource.netlens.data.model.ChannelRating
import com.opensource.netlens.data.model.WifiNetwork
import com.opensource.netlens.data.model.signalColor
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.util.SignalLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var band by remember { mutableStateOf(Band.BAND_24) }
    val ratings = remember(state.networks, band) { vm.channelRatings(band) }
    val inBand = state.networks.filter { it.band == band && it.channel > 0 }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.channel_title), fontWeight = FontWeight.Bold) })
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = band == Band.BAND_24, onClick = { band = Band.BAND_24 }, label = {
                    Text(stringResource(R.string.channel_24))
                })
                FilterChip(selected = band == Band.BAND_5, onClick = { band = Band.BAND_5 }, label = {
                    Text(stringResource(R.string.channel_5))
                })
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.channel_overlap_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            ChannelGraph(band = band, networks = inBand, ratings = ratings)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.channel_recommend),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ratings.take(8)) { r ->
                ChannelRatingRow(r, isCurrent = state.connection?.channel == r.channel && state.connection?.band == band)
            }
        }
    }
}

@Composable
fun ChannelGraph(band: Band, networks: List<WifiNetwork>, ratings: List<ChannelRating>) {
    val maxChannel = if (band == Band.BAND_24) 13 else 165
    val minChannel = if (band == Band.BAND_24) 1 else 36
    val channelsShown = ratings.map { it.channel }.sorted()

    Card {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(12.dp)
        ) {
            if (channelsShown.isEmpty()) return@Canvas
            val minC = channelsShown.min()
            val maxC = channelsShown.max()
            val span = (maxC - minC).coerceAtLeast(1).toFloat()

            // grid lines
            val grid = Color.Gray.copy(alpha = 0.25f)
            for (i in 0..4) {
                val y = size.height * i / 4f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            // draw each AP as a parabola-ish triangle around its channel
            networks.forEachIndexed { idx, net ->
                val xNorm = (net.channel - minC) / span
                val cx = xNorm * size.width
                val color = signalColor(SignalLabels.level(net.rssi)).copy(alpha = 0.55f)
                val widthPx = size.width * (if (band == Band.BAND_24) 0.18f else 0.08f)
                val h = ((net.rssi + 100) / 60f).coerceIn(0.15f, 1f) * size.height * 0.9f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - widthPx, size.height)
                    lineTo(cx, size.height - h)
                    lineTo(cx + widthPx, size.height)
                    close()
                }
                drawPath(path, color)
                drawPath(path, color.copy(alpha = 1f), style = Stroke(width = 2f))
            }

            // channel labels
            val labelColor = Color(0xFF667085)
            channelsShown.filterIndexed { i, _ -> i % (if (channelsShown.size > 16) 3 else 1) == 0 }.forEach { ch ->
                val xNorm = (ch - minC) / span
                val cx = xNorm * size.width
                drawLine(
                    labelColor.copy(alpha = 0.4f),
                    Offset(cx, size.height - 4f),
                    Offset(cx, size.height),
                    strokeWidth = 2f
                )
            }
        }
    }
}

@Composable
fun ChannelRatingRow(r: ChannelRating, isCurrent: Boolean) {
    Card {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "CH ${r.channel}" + if (isCurrent) " ●" else "",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${stringResource(R.string.channel_ap_count)}: ${r.apCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    "${r.score}",
                    fontWeight = FontWeight.Bold,
                    color = when {
                        r.score >= 70 -> Color(0xFF22C55E)
                        r.score >= 40 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                )
                Text(stringResource(R.string.channel_score), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
