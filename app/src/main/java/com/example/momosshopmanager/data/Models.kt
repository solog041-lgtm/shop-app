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
        MenuItem(id = "steam12", name = "Steam Momo (12pc)", category = "Steamed", price = 120.0)
    )

    val categories = listOf("Steamed")
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
    val pin: String,
    val userName: String = ""
)

@Serializable
data class Alert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String = "LOW_STOCK",
    val message: String,
    val senderName: String,
    val senderPhone: String,
    val timestamp: Long = System.currentTimeMillis()
)

