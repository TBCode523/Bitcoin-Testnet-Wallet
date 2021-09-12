package tbcode.example.kotlinbitcoinwallet.send
import FeePriority
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import tbcode.example.kotlinbitcoinwallet.NumberFormatHelper
import java.net.URL

class SendViewModel : ViewModel() {

    private val feeUrl = "https://mempool.space/api/v1/fees/recommended"
    enum class FEE_RATE{LOW, MED, HIGH}
    val sats = 100000000
    lateinit var feePriority:FeePriority
    var sendAddress = ""
    var amount:Long = 0
    var fee:Long = 0
    var formattedFee: String = NumberFormatHelper.cryptoAmountFormat.format( fee/ 100_000_000.0)
    var timeLockInterval: LockTimeInterval? = null

    init{
        CoroutineScope(IO).launch {

            feePriority = try {
                generateFeePriority(feeUrl)

            } catch (e:Exception){
                FeePriority(10,5,3)
            }

            formattedFee = NumberFormatHelper.cryptoAmountFormat.format( feePriority.medFee/ 100_000_000.0)

        }
    }
      private fun generateFeePriority(feeUrl: String): FeePriority {

        val response = URL(feeUrl).readText()
        val gson = Gson()
        return gson.fromJson(response, FeePriority::class.java)
    }
    fun generateFee(bitcoinKit: BitcoinKit): Long {
        try {


            fee = bitcoinKit.fee(amount, feeRate = feePriority.medFee)

            formattedFee = NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)
        } catch (e:Exception){
            Log.d("SF-SVM","Error:$e")
            fee = 0
            formattedFee = NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)
        }
        Log.d("SF-SVM","Generated fee:$formattedFee")
    return fee
    }
    fun generateFee(bitcoinKit: BitcoinKit,feeRate: FEE_RATE): Long {
        fee = try {
            Log.d("SF-SVM", "amount: $amount")
            when (feeRate) {
                FEE_RATE.MED -> bitcoinKit.fee(amount, feeRate = feePriority.medFee)
                FEE_RATE.LOW -> bitcoinKit.fee(amount, feeRate = feePriority.lowFee)
                else -> bitcoinKit.fee(amount, feeRate = feePriority.highFee)
            }
        } catch (e:Exception){
            0
        }
        formattedFee = "${NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)} tBTC"
        Log.d("SF-SVM","Generated fee:$formattedFee")
        return fee
    }
    fun getFeeRate(feeRate: FEE_RATE):Int{
        return when(feeRate){
            FEE_RATE.LOW -> feePriority.lowFee
            FEE_RATE.HIGH -> feePriority.highFee
            else -> feePriority.medFee
        }
    }

    fun formatAmount():String{
        return NumberFormatHelper.cryptoAmountFormat.format(amount / 100_000_000.0)
    }
    fun formatFee():String{
        return  "${NumberFormatHelper.cryptoAmountFormat.format(fee / 100_000_000.0)} tBTC"
    }
    fun formatTotal():String{
        return  "${NumberFormatHelper.cryptoAmountFormat.format((fee+amount) / 100_000_000.0)} tBTC"
    }
    fun getPluginData(): MutableMap<Byte, IPluginData> {
        val pluginData = mutableMapOf<Byte, IPluginData>()
        timeLockInterval?.let {
            pluginData[HodlerPlugin.id] = HodlerData(it)
        }
        return pluginData
    }


    

}