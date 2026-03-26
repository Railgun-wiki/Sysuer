package com.sysu.edu.life

import dev.enro.core.NavigationKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShowUserProfile(
    val userId: String
) : NavigationKey.SupportsPush

@Parcelize
data class Activity(
    val userId: String
) : NavigationKey.SupportsPush

@Parcelize
class MainKey : NavigationKey

abstract class TabKey(val title: String) : NavigationKey

@Parcelize
class HomeKey : TabKey("home"), NavigationKey.SupportsPush

@Parcelize
class DashboardKey : TabKey("dashboard")

@Parcelize
class NotificationsKey : TabKey("notifications")

@Parcelize
class Detail(val id: String) : NavigationKey
