package live.ebox.myapplication

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private val CAPABILITY_1_NAME = "capability_1"
    private val CAPABILITY_2_NAME = "capability_2"

    val TAG = "WearableActivity"


    private var nodeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        // Instantiates clients without member variables, as clients are inexpensive to create and
        // won't lose their listeners. (They are cached and shared between GoogleApi instances.)
        Wearable.getDataClient(this).addListener(this)
        Wearable.getMessageClient(this).addListener(this)
        Wearable.getCapabilityClient(this)
            .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)

        requestCapability(CAPABILITY_1_NAME)

        btn?.setOnClickListener {
            nodeId?.let { nid ->
                val sendMessageTask =
                Wearable.getMessageClient(this)
                    .sendMessage(nid, "/p", "Hello from phone!".toByteArray()).addOnCompleteListener {
                        Log.d(TAG,"msg sent : ${it.isSuccessful}")
                    }
            }
        }
    }

    private fun requestCapability(capabilityName:String) {

        GlobalScope.launch {
            val capabilityInfo: CapabilityInfo = Tasks.await(
                Wearable.getCapabilityClient(this@MainActivity)
                    .getCapability(capabilityName,
                        CapabilityClient.FILTER_REACHABLE
                    )
            )
            // capabilityInfo has the reachable nodes with the capability
            updateNodeId(capabilityInfo)
        }
    }

    private fun updateNodeId(capabilityInfo: CapabilityInfo) {
        nodeId = pickBestNodeId(capabilityInfo.nodes)
        Log.d(TAG,"phone got wear node id = $nodeId")

    }

    private fun pickBestNodeId(nodes: Set<Node>): String? {
        // Find a nearby node or pick one arbitrarily
        return nodes.firstOrNull { it.isNearby }?.id ?: nodes.firstOrNull()?.id
    }


    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
        Wearable.getMessageClient(this).removeListener(this)
        Wearable.getCapabilityClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        Log.d(TAG, "onDataChanged-> $dataEventBuffer")


    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived-> data =${String(messageEvent.data)} | path =${messageEvent.path}")

        Toast.makeText(this,String(messageEvent.data),Toast.LENGTH_SHORT).show()
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged-> ${capabilityInfo}")
        updateNodeId(capabilityInfo)
    }
}
