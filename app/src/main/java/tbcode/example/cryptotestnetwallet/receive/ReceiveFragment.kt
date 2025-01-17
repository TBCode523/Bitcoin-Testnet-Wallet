package tbcode.example.cryptotestnetwallet.receive
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import tbcode.example.cryptotestnetwallet.R
import tbcode.example.cryptotestnetwallet.utils.CoinKit
import tbcode.example.cryptotestnetwallet.utils.KitSyncService



class ReceiveFragment : Fragment() {
    private lateinit var viewModel: ReceiveViewModel
    private lateinit var receiveTxt: TextView
    private lateinit var qrCode: ImageView
    private lateinit var generateBtn:Button
    private lateinit var faucetBtn:Button
    private lateinit var sharedPref: SharedPreferences
    private lateinit var checkBox: CheckBox
    private lateinit var coinKit: CoinKit
    private lateinit var amountTxt:EditText
    companion object{
        const val TAG = "CT-RF"
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.receive_fragment, container, false)
        viewModel = ViewModelProvider(this)[ReceiveViewModel::class.java]
        receiveTxt = root.findViewById(R.id.tv_Receive)
        amountTxt = root.findViewById(R.id.ev_amount_rf)
        qrCode= root.findViewById(R.id.qr_code)
        generateBtn = root.findViewById(R.id.generate_btn)
        faucetBtn = root.findViewById(R.id.faucet_btn)
        sharedPref = this.requireContext().getSharedPreferences("btc-kit", Context.MODE_PRIVATE)
        if(!sharedPref.contains("warning")) sharedPref.edit().putBoolean("warning", true).apply()
        KitSyncService.isKitAvailable.observe(viewLifecycleOwner){
            if (it){
                Log.d(TAG, "kit is available: ${KitSyncService.coinKit}")
                coinKit =KitSyncService.coinKit!!
                setUpUI()
            }
            else{
                Toast.makeText(context, "Your wallet is not ready yet", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "kit not available")
                viewModel.clearFields()
            }
        }
        return root
    }

    private fun setUpUI(){
        if(sharedPref.getBoolean("warning", true)) warningDialogue()
        try {
            coinKit.let {
                if (viewModel.currentAddress.value.isNullOrEmpty() || viewModel.currentAddress.value!!.isBlank()) {
                    Log.d(TAG, "vm's addr:${viewModel.currentAddress.value}")
                    receiveClick(it)
                }
                else{
                    receiveTxt.text = viewModel.currentAddress.value
                    qrCode.setImageBitmap(viewModel.currentQRCode.value)
                }
                generateBtn.setOnClickListener {_ ->
                    Log.d(TAG,"QR-Amount: " + amountTxt.text)
                    receiveClick(it,amount = amountTxt.text.toString())
                }
                faucetBtn.setOnClickListener {
                    val faucetURL = "https://testnet-faucet.com/"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(faucetURL))
                    ContextCompat.startActivity(requireContext(), intent, null)
                }
                amountTxt.setOnEditorActionListener { _, i, _ ->
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        receiveClick(it, amount = amountTxt.text.toString())
                        true
                    }
                    else false
                }
            }
        } catch (e:Exception){
            Toast.makeText(context,e.message,Toast.LENGTH_LONG).show()
        }
    }

    private fun receiveClick(coinKit: CoinKit,amount: String = "") {
        try {
            //viewModel.generateAddress(cryptoKit)
            val generatedAddress: String = viewModel.generateAddress(coinKit)
            qrCode.setImageBitmap(null)
            Toast.makeText(this.context, "Generating QRCode", Toast.LENGTH_SHORT).show()
            if (amount.isBlank()) viewModel.generateQRCode("bitcoin:$generatedAddress")
            else viewModel.generateQRCode("bitcoin:$generatedAddress?amount=$amount")
            Log.d(TAG, "generatedAddress: $generatedAddress")
            Log.d(TAG, "currentAddress: ${viewModel.currentAddress.value}")
            Log.d(TAG, "currentQRCode: ${viewModel.currentQRCode.value}")
            receiveTxt.text = viewModel.currentAddress.value
            qrCode.setImageBitmap(viewModel.currentQRCode.value)
            Toast.makeText(
                this.context,
                "QRCode Successfully Generated: ${viewModel.currentAddress.value}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this.context, "Failed to Generate Address: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun warningDialogue(){
        val view= View.inflate(this.requireContext(), R.layout.dialog_checkbox, null)
         checkBox = view.findViewById(R.id.dialog_checkBox)
        AlertDialog.Builder(this.requireContext())
            .setTitle("\t\t\t\t\t\t\t\t\t\t\tWARNING")
            .setView(view)
            .setMessage("\nThis is a Testnet Wallet! \n\n" +
                    "Do Not Send Main-net coins (${coinKit.label.drop(1)}) to This Address!\n\n"
                    + "WE ARE NOT RESPONSIBLE FOR LOST FUNDS!!!!")
            .setPositiveButton("OK"){_ , _ ->
                if(checkBox.isChecked) sharedPref.edit().putBoolean("warning", false).apply()
            }
            .show()
    }

}