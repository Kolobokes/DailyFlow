package com.dailyflow.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit
) {
    val icons = mapOf(
        "work" to Icons.Default.Work, "person" to Icons.Default.Person, "sports_esports" to Icons.Default.SportsEsports,
        "home" to Icons.Default.Home, "shopping_cart" to Icons.Default.ShoppingCart, "school" to Icons.Default.School,
        "favorite" to Icons.Default.Favorite, "fitness_center" to Icons.Default.FitnessCenter, "music_note" to Icons.Default.MusicNote,
        "movie" to Icons.Default.Movie, "restaurant" to Icons.Default.Restaurant, "local_cafe" to Icons.Default.LocalCafe,
        "local_bar" to Icons.Default.LocalBar, "local_dining" to Icons.Default.LocalDining, "local_drink" to Icons.Default.LocalDrink,
        "local_florist" to Icons.Default.LocalFlorist, "local_gas_station" to Icons.Default.LocalGasStation, "local_grocery_store" to Icons.Default.LocalGroceryStore,
        "local_hospital" to Icons.Default.LocalHospital, "local_laundry_service" to Icons.Default.LocalLaundryService, "local_library" to Icons.Default.LocalLibrary,
        "local_mall" to Icons.Default.LocalMall, "local_movies" to Icons.Default.LocalMovies, "local_offer" to Icons.Default.LocalOffer,
        "local_parking" to Icons.Default.LocalParking, "local_pharmacy" to Icons.Default.LocalPharmacy, "local_pizza" to Icons.Default.LocalPizza,
        "local_post_office" to Icons.Default.LocalPostOffice, "local_printshop" to Icons.Default.LocalPrintshop, "local_see" to Icons.Default.LocalSee,
        "local_shipping" to Icons.Default.LocalShipping, "local_taxi" to Icons.Default.LocalTaxi, "menu_book" to Icons.Default.MenuBook,
        "ramen_dining" to Icons.Default.RamenDining, "store" to Icons.Default.Store, "storefront" to Icons.Default.Storefront,
        "train" to Icons.Default.Train, "tram" to Icons.Default.Tram, "transfer_within_a_station" to Icons.Default.TransferWithinAStation,
        "translate" to Icons.Default.Translate, "trending_down" to Icons.Default.TrendingDown, "trending_flat" to Icons.Default.TrendingFlat,
        "trending_up" to Icons.Default.TrendingUp, "trip_origin" to Icons.Default.TripOrigin, "two_wheeler" to Icons.Default.TwoWheeler,
        "volunteer_activism" to Icons.Default.VolunteerActivism, "waves" to Icons.Default.Waves, "weekend" to Icons.Default.Weekend
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(icons.entries.toList()) { (name, icon) ->
            Icon(
                imageVector = icon,
                contentDescription = name,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onIconSelected(name) }
                    .padding(4.dp)
                    .then(
                        if (selectedIcon == name) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
