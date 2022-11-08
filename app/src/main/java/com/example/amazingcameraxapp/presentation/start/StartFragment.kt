package com.example.amazingcameraxapp.presentation.start

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.amazingcameraxapp.R
import kotlinx.android.synthetic.main.start_fragment.*

class StartFragment : Fragment(R.layout.start_fragment) {

    private lateinit var viewModel: StartViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(StartViewModel::class.java)

        enableButtons(allPermissionsGranted())

        btnPermissions.setOnClickListener {
            requestPermissions(PERMISSION, PERMISSION_REQUEST)
        }
        btnSimple.setOnClickListener {
            findNavController().navigate(R.id.actionSimpleCamera)
        }
        btnAnalyzer.setOnClickListener {
            findNavController().navigate(R.id.actionAnalyzerCamera)
        }
    }

    private fun enableButtons(permissionsGranted: Boolean) {

        btnSimple.isEnabled = permissionsGranted
        btnAnalyzer.isEnabled = permissionsGranted
        btnPermissions.isVisible = !permissionsGranted

        if (!permissionsGranted) {
            requestPermissions(PERMISSION, PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST && allPermissionsGranted())
            enableButtons(true)
        else
            Toast.makeText(requireContext(), "Need permissions to continue", Toast.LENGTH_SHORT)
                .show()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun allPermissionsGranted() = PERMISSION.all {
        ContextCompat.checkSelfPermission(
            requireActivity().baseContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val PERMISSION =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private const val PERMISSION_REQUEST = 10
    }

}