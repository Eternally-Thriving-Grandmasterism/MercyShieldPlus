package org.tolc.divine

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.tolc.divine.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    external fun mercyShieldStatus(): String // JNI from Rust lib

    companion object {
        init {
            System.loadLibrary("mercy_shield_plus") // Rust lib
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Live status from Rust core
        binding.statusText.text = mercyShieldStatus()

        // Activate button one-tap
        binding.activateButton.setOnClickListener {
            // Trigger shield + haptic + glow burst
            // Call Rust verify
        }

        // Permissions mercy (network/location)
        // Request runtime
    }
}
