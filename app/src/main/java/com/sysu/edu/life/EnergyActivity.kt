package com.sysu.edu.life

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityWaterEletricityFeeBinding
import dev.enro.annotations.NavigationDestination
import dev.enro.core.getNavigationHandle
import dev.enro.core.navigationHandle
import dev.enro.core.push

@NavigationDestination(MainKey::class)
class EnergyActivity : AppCompatActivity() {

    //    val navigation by navigationHandle<MainKey>()

    private val navigation by navigationHandle<MainKey> {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWaterEletricityFeeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHandle =
            (this.supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment).getNavigationHandle()
        navHandle.push(HomeKey())

//        val exampleContainer by navigationContainer(
//            containerId = R.id.fragment,
//            root = { HomeKey() },
//            emptyBehavior = EmptyBehavior.CloseParent,
//        )
//            ShowUserProfile(userId = "")
//        )
//        println("EnergyActivity onCreate $navigation")
//        selectDate.push(ShowUserProfile("2023-01-01"))
//        binding.bottomNavigation.setupWithNavController(navHandle.)
    }
}
