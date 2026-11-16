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
import com.example.eventia.domain.model.Venue

/**
 * RF-13: tarjeta de lugar con nombre, dirección, imagen y número de
 * eventos próximos (imagen ausente → sustituta, como en [EventCard]).
 */
@Composable
fun VenueCard(
    venue: Venue,
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
                model = venue.imageUrl ?: "https://via.placeholder.com/120x120.png?text=Lugar",
                contentDescription = venue.name,
                modifier = Modifier.size(88.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = venue.name, style = MaterialTheme.typography.titleMedium)
                venue.address?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                venue.upcomingEventsCount?.let { count ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (count == 1) "1 evento próximo" else "$count eventos próximos",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
