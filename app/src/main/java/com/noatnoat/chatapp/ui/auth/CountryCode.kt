package com.noatnoat.chatapp.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CountryCode(
    val name: String,
    val dialCode: String,
    val flag: String,
    val countryCode: String
)

val defaultCountryList = listOf(
    CountryCode("Vietnam", "+84", "🇻🇳", "VN"),
    CountryCode("United States", "+1", "🇺🇸", "US"),
    CountryCode("Japan", "+81", "🇯🇵", "JP"),
    CountryCode("South Korea", "+82", "🇰🇷", "KR"),
    CountryCode("Singapore", "+65", "🇸🇬", "SG"),
    CountryCode("United Kingdom", "+44", "🇬🇧", "GB"),
    CountryCode("Australia", "+61", "🇦🇺", "AU"),
    CountryCode("Canada", "+1", "🇨🇦", "CA"),
    CountryCode("Germany", "+49", "🇩🇪", "DE"),
    CountryCode("France", "+33", "🇫🇷", "FR"),
    CountryCode("China", "+86", "🇨🇳", "CN"),
    CountryCode("Taiwan", "+886", "🇹🇼", "TW"),
    CountryCode("Thailand", "+66", "🇹🇭", "TH"),
    CountryCode("Malaysia", "+60", "🇲🇾", "MY"),
    CountryCode("Indonesia", "+62", "🇮🇩", "ID"),
    CountryCode("Philippines", "+63", "🇵🇭", "PH"),
    CountryCode("India", "+91", "🇮🇳", "IN")
)

fun formatE164PhoneNumber(dialCode: String, rawNumber: String): String {
    val trimmed = rawNumber.trim()
    if (trimmed.startsWith("+")) return trimmed
    val cleanDigits = if (trimmed.startsWith("0")) trimmed.substring(1) else trimmed
    return "$dialCode$cleanDigits"
}

@Composable
fun PhoneInputWithCountryPicker(
    rawPhoneNumber: String,
    onRawPhoneNumberChange: (String) -> Unit,
    selectedCountry: CountryCode,
    onCountrySelected: (CountryCode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Country Selector Card Dropdown
        Box {
            OutlinedCard(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(end = 8.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedCountry.flag} ${selectedCountry.dialCode}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Country"
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                defaultCountryList.forEach { country ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = country.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${country.name} (${country.dialCode})",
                                    fontSize = 14.sp,
                                    fontWeight = if (country == selectedCountry) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onCountrySelected(country)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Phone Number Input Field
        OutlinedTextField(
            value = rawPhoneNumber,
            onValueChange = onRawPhoneNumberChange,
            label = { Text("Mobile Number") },
            placeholder = { Text("372824461") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f)
        )
    }
}
