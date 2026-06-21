package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import eu.ejdr.ejdr_frontend.generated.resources.Res
import eu.ejdr.ejdr_frontend.generated.resources.fraunces_regular
import eu.ejdr.ejdr_frontend.generated.resources.fraunces_semibold
import eu.ejdr.ejdr_frontend.generated.resources.inter_medium
import eu.ejdr.ejdr_frontend.generated.resources.inter_regular
import eu.ejdr.ejdr_frontend.generated.resources.inter_semibold
import eu.ejdr.ejdr_frontend.generated.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

@Composable
fun appDisplayFamily(): FontFamily = FontFamily(
    Font(Res.font.fraunces_regular, FontWeight.Normal),
    Font(Res.font.fraunces_semibold, FontWeight.SemiBold),
)

@Composable
fun appBodyFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
)

@Composable
fun appMonoFamily(): FontFamily = FontFamily(
    Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
)
