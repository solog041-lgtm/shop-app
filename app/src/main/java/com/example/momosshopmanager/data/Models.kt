package com.example.momosshopmanager.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MenuItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val price: Double,
    val isAvailable: Boolean = true
)

@Serializable
data class SaleItem(
    val menuItem: MenuItem,
    val quantity: Int
) {
    val subtotal: Double get() = menuItem.price * quantity
}

@Serializable
data class Sale(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<SaleItem>,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val customerName: String = "",
    val createdBy: String = ""
) {
    val total: Double get() = items.sumOf { it.subtotal }
    val totalItems: Int get() = items.sumOf { it.quantity }
}

@Serializable
enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card")
}

object MenuDefaults {
    val defaultMenu = listOf(
        MenuItem(id = "steam8", name = "Steam Momo (8pc)", category = "Steamed", price = 80.0),
        MenuItem(id = "steam12", name = "Steam Momo (12pc)", category = "Steamed", price = 120.0),
        MenuItem(id = "fried8", name = "Fried Momo (8pc)", category = "Fried", price = 100.0),
        MenuItem(id = "fried12", name = "Fried Momo (12pc)", category = "Fried", price = 140.0),
        MenuItem(id = "tandoori8", name = "Tandoori Momo (8pc)", category = "Tandoori", price = 120.0),
        MenuItem(id = "tandoori12", name = "Tandoori Momo (12pc)", category = "Tandoori", price = 160.0),
        MenuItem(id = "kurkure8", name = "Kurkure Momo (8pc)", category = "Kurkure", price = 120.0),
        MenuItem(id = "kurkure12", name = "Kurkure Momo (12pc)", category = "Kurkure", price = 160.0),
        MenuItem(id = "gravy", name = "Gravy Momo", category = "Gravy", price = 130.0),
        MenuItem(id = "soup", name = "Soup Momo", category = "Soup", price = 100.0),
        MenuItem(id = "combo", name = "Momo Combo Plate", category = "Combo", price = 180.0),
        MenuItem(id = "chili", name = "Chili Momo", category = "Special", price = 130.0),
        MenuItem(id = "paneer8", name = "Paneer Momo (8pc)", category = "Special", price = 110.0),
        MenuItem(id = "chicken8", name = "Chicken Momo (8pc)", category = "Non-Veg", price = 120.0),
        MenuItem(id = "chicken12", name = "Chicken Momo (12pc)", category = "Non-Veg", price = 160.0),
    )

    val categories = listOf("Steamed", "Fried", "Tandoori", "Kurkure", "Gravy", "Soup", "Combo", "Special", "Non-Veg")
}

@Serializable
enum class UserRole(val displayName: String) {
    OWNER("Owner"),
    EMPLOYEE("Employee")
}

@Serializable
data class DeviceInfo(
    val phoneNumber: String,
    val role: UserRole,
    val deviceName: String,
    val registeredAt: Long,
    val pin: String
)

