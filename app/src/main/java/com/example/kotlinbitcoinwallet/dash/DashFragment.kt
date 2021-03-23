package com.example.kotlinbitcoinwallet.dash

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinbitcoinwallet.MainActivity
import com.example.kotlinbitcoinwallet.NumberFormatHelper
import com.example.kotlinbitcoinwallet.R
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoinkit.BitcoinKit
//import org.bitcoinj.core.SegwitAddress
//import org.bitcoinj.wallet.Wallet

class DashFragment : Fragment(){



    private lateinit var viewModel: DashViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtBalance: TextView
    private lateinit var txtNoTransaction:TextView
    private lateinit var bitcoinKit: BitcoinKit
    private lateinit var adapter: TxAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.dash_fragment, container, false)
        recyclerView = root.findViewById(R.id.dash_recyclerview)
        txtBalance = root.findViewById(R.id.tv_Balance)
        txtNoTransaction = root.findViewById(R.id.tv_NoTransaction)
        txtNoTransaction.visibility = View.GONE

        try {
         bitcoinKit =  (activity as MainActivity).viewModel.bitcoinKit
        }catch (e:Exception){
            Toast.makeText(context,"${e.message}", Toast.LENGTH_LONG).show()
        }
        return root
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        try {

            viewModel = ViewModelProvider(this).get(DashViewModel::class.java)

            viewModel.getBalance(bitcoinKit)
            viewModel.getTransactions(bitcoinKit)

            viewModel.balance.observe(viewLifecycleOwner, Observer { balance ->
                when (balance) {
                    null -> txtBalance.text = SpannableStringBuilder("0 BTC: wallet can't be found")
                    else -> txtBalance.text = SpannableStringBuilder("${NumberFormatHelper.cryptoAmountFormat.format(balance.spendable / 100_000_000.0)} BTC")
                }
            })
            viewModel.transactions.observe(viewLifecycleOwner, Observer {
                it?.let { transactions ->
                    if (adapter?.itemCount == 0) txtNoTransaction.visibility = View.VISIBLE
                    adapter.transactions = transactions
                    adapter.notifyDataSetChanged()
                }
            })

            adapter = TxAdapter(viewModel.transactions.value)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this.requireContext())

        } catch (e:Exception){
            txtBalance.text = SpannableStringBuilder("0.00 tBTC")
            txtNoTransaction.visibility = View.VISIBLE
        }


    }


}