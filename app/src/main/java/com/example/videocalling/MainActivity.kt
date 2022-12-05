package com.example.videocalling

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.videocalling.databinding.ActivityMainBinding
import com.permissionx.guolindev.PermissionX
import kotlin.math.log

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.enterBtn.setOnClickListener {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                ).request{allGranted,_,_ ->
                    if (allGranted){
                        Log.d(TAG, "onCreate: all permission are granted")
                        startActivity(
                            Intent(this,CallActivity::class.java)
                                .putExtra("username",binding.username.text.toString())
                        )
                    }else{
                        Log.d(TAG, "onCreate: all permission are denied")
                        Toast.makeText(this,"You should accept all permission",Toast.LENGTH_LONG).show()
                    }
                }


        }
    }
}