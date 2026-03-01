package org.fossify.filemanager.activities

import android.os.Bundle
import org.fossify.commons.extensions.viewBinding
import org.fossify.filemanager.databinding.CloudActivityBinding

class CloudActivity: SimpleActivity() {
    private val binding by viewBinding(CloudActivityBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

}
