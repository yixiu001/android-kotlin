package cn.yixiu.starter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.yixiu.starter.ui.*
import cn.yixiu.starter.debug.DeveloperPanel
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: StarterViewModel = viewModel()
            val account by vm.account.collectAsStateWithLifecycle()
            val busy by vm.busy.collectAsStateWithLifecycle()
            val message by vm.message.collectAsStateWithLifecycle()
            StarterTheme {
                StarterScreen(account, busy, message, vm::dispatch) {
                    DeveloperPanel(vm)
                }
            }
        }
    }
}
