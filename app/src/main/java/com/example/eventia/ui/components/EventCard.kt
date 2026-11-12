package com.example.eventia.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.eventia.domain.model.Event

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = event.imageUrl ?: "https://via.placeholder.com/120x120.png?text=Eventia",
                contentDescription = event.title,
                modifier = Modifier.size(88.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                event.summary?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(event.date, event.place ?: event.venueName, event.categoryName)
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall
                )
                val cupos = event.availableSpots ?: event.capacity
                if (cupos != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Cupos: $cupos", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
