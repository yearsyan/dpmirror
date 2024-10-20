package io.github.tsioam.mirror.ui.view

import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tsioam.mirror.R

@Composable
fun RemoteItemCard(item: NsdServiceInfo, onConnect: () -> Unit) {
    Box(
        modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(1.0f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(0.5f)
                ) {
                    Text(
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        fontSize = 14.sp,
                        text = item.serviceName
                    )
                }
                Text(
                    fontSize = 12.sp,
                    text = "${item.host.hostAddress}:${item.port}"
                )
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(1.0f)
            ) {
                Button(
                    onClick = onConnect
                ) {
                    Text(stringResource(R.string.mirror_connect))
                }
            }
        }
    }
}