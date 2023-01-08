package tbcode.example.cryptotestnetwallet

import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import io.horizontalsystems.bitcoinkit.BitcoinKit
import kotlinx.coroutines.awaitAll
import tbcode.example.cryptotestnetwallet.utils.CryptoKits
import tbcode.example.cryptotestnetwallet.utils.KitSyncService
import tbcode.example.cryptotestnetwallet.utils.kit_utils.BTCKitUtils
import java.security.SecureRandom

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var kitSyncService: KitSyncService
    private val serviceConnection = object : ServiceConnection{
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.d(TAG, "Service is connected" )
            Log.d(TAG, "Is the kit initialized? ${KitSyncService.bitcoinKit}" )
            val localBinder = p1 as KitSyncService.LocalBinder
            kitSyncService = localBinder.getBindServiceInstance()

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            TODO("Not yet implemented")
        }
    }

    companion object {
        var isActive = false
        const val TAG = "btc-activity"
    }

    init {
        //Disable dark mode here
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("btc-activity", "MainActivity onCreate is called!")
        try {
            isActive = true
            sharedPref = this.getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
            if(!sharedPref.contains(BTCKitUtils.getWalletID())){
                Log.d("btc-activity", "No wallet")
                generateWallet()
            }
            val words = sharedPref.getString(BTCKitUtils.getWalletID(),null)?.split(" ")
            Log.d("btc-service", "Kit Builder words: $words")
            KitSyncService.bitcoinKit = KitSyncService.cryptoKits.createKit(this, words!!) as BitcoinKit
            when(KitSyncService.cryptoKits){
                CryptoKits.T_BTC -> {

                }
                else -> {}
            }
            if(!isOnline()){ Log.d("btc-activity", "Not connected")
                throw Exception("No Connection Detected!")
            }
            val serviceIntent = Intent(this, KitSyncService::class.java)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Log.d("btc-activity","Starting Foreground Service")
                startForegroundService(serviceIntent)
            }
            Log.d("btc-activity","Service Component type: ${serviceIntent.component}")
        }catch (e:Exception) {
            Toast.makeText(this,"Error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.d("btc-activity","Error: ${e.message}")
        }
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_receive, R.id.nav_dash, R.id.nav_send))
        setupActionBarWithNavController(navController,appBarConfiguration)
        navView.setupWithNavController(navController)

    }

    private fun generateWallet(){
        val sb = StringBuilder()
        val entropy = ByteArray(Words.TWELVE.byteLength())
        SecureRandom().nextBytes(entropy)
        MnemonicGenerator(English.INSTANCE)
                .createMnemonic(entropy, sb::append)
        sharedPref.edit().putString(BTCKitUtils.getWalletID(), sb.toString()).apply()
        Toast.makeText(this.baseContext, "Generating Wallet", Toast.LENGTH_SHORT).show()
        seedDialog(sb.toString())

    }

    private fun seedDialog(seed:String){
        try {
            val alertDialog = AlertDialog.Builder(this)
            .setTitle("Seed Phrase Generated")
            .setMessage("Your seed phrase is: \n$seed\n" +
                    "Make sure to write it down or screenshot, and back it up somewhere!")
            .setPositiveButton("OK"){ _, _->
                Toast.makeText(this,
                    "You won't be able to send transactions until we're synced.(~2-5 min.)",
                    Toast.LENGTH_LONG).show()
            }.create()
            alertDialog.show()
        } catch (e:Exception){
            Toast.makeText(this,"Seed retrieval failed!", Toast.LENGTH_SHORT).show()
        }

    }

    private fun isOnline(): Boolean {
        val connMgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
        return networkInfo?.isConnected == true

    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }
    override fun onDestroy() {
        Log.d("btc-activity", "MainActivity onDestroy is called! $isFinishing")
        super.onDestroy()
        if (KitSyncService.isRunning) KitSyncService.stopSync()
        isActive = false
    }

}