package live.ebox.myapplication

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import live.ebox.myapplication.communication.WearableMessageListener
import live.ebox.myapplication.communication.WearableMessageManger


class MainActivity : WearableActivity(), WearableMessageListener {
    val TAG = "WearableActivity"
    private var wearableMessageManger: WearableMessageManger? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wearableMessageManger =
            WearableMessageManger.Builder(activity = this, capabilityName = "capability_phone")
                .setWearableMessageListener(this)
                .build()

        btns?.setOnClickListener {
            wearableMessageManger?.sendMessage("/P", "Hello from wear")
        }
    }

    override fun onMessageReceived(
        data: ByteArray,
        path: String,
        sourceNodeId: String,
        requestId: Int
    ) {
        Toast.makeText(this, String(data), Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        wearableMessageManger?.onPause()
    }

    override fun onResume() {
        super.onResume()
        wearableMessageManger?.onResume()
    }
}
