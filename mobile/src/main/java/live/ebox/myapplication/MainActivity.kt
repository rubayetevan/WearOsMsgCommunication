package live.ebox.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), WearableMessageListener {


    val TAG = "WearableActivity"
    private var wearableMessageManger: WearableMessageManger? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        wearableMessageManger = WearableMessageManger.getInstance(this)
        btn?.setOnClickListener {
            wearableMessageManger?.sendMessage("/P", "Hello from phone")
        }
    }

    override fun onMessageReceived(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
