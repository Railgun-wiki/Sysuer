package com.sysu.edu

import dev.enro.annotations.NavigationComponent
import dev.enro.core.controller.NavigationApplication
import dev.enro.core.controller.navigationController

@NavigationComponent
class NavApp : Application(), NavigationApplication {
    override val navigationController = navigationController()
}