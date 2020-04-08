package com.airwallex.android

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.airwallex.android.model.*
import com.airwallex.android.view.*

/**
 *  Entry-point to the Airwallex Payment Flow. Create a AirwallexStarter attached to the given host Activity.
 *
 *  @param activity This Activity will receive results in `Activity#onActivityResult(int, int, Intent)`
 *  that should be passed back to this session.
 */
class AirwallexStarter constructor(
    private val activity: Activity
) {

    constructor(
        fragment: Fragment
    ) : this(fragment.requireActivity())

    interface PaymentListener {
        fun onCancelled()
    }

    /**
     * Represents a listener for PaymentShipping actions
     */
    interface PaymentShippingListener : PaymentListener {
        fun onSuccess(shipping: Shipping)
    }

    /**
     * Represents a listener for PaymentIntent actions
     */
    interface PaymentIntentListener : PaymentListener {
        fun onSuccess(paymentIntent: PaymentIntent)
        fun onFailed(error: AirwallexError)
    }

    /**
     * Represents a listener for PaymentMethod actions
     */
    interface PaymentMethodListener : PaymentListener {
        fun onSuccess(paymentMethod: PaymentMethod, cvc: String?)
    }

    private var shippingFlowListener: PaymentShippingListener? = null
    private var paymentFlowListener: PaymentIntentListener? = null
    private var paymentDetailListener: PaymentIntentListener? = null
    private var addPaymentMethodFlowListener: PaymentMethodListener? = null
    private var selectPaymentMethodFlowListener: PaymentMethodListener? = null

    /**
     * Launch the [PaymentShippingActivity] to allow the user to fill the shipping information
     *
     * @param shipping a [Shipping] used to present the shipping flow, it's optional
     * @param shippingFlowListener The callback of present the shipping flow
     */
    fun presentShippingFlow(
        shipping: Shipping? = null,
        shippingFlowListener: PaymentShippingListener
    ) {
        this.shippingFlowListener = shippingFlowListener
        PaymentShippingActivityStarter(activity)
            .startForResult(
                PaymentShippingActivityStarter.Args.Builder()
                    .setShipping(shipping)
                    .build()
            )
    }

    /**
     * Launch the [AddPaymentMethodActivity] to allow the user to add a payment method
     *
     * @param paymentIntent a [PaymentIntent] used to present the Add Payment Method flow
     * @param addPaymentMethodFlowListener The callback of present the add payment method flow
     */
    fun presentAddPaymentMethodFlow(
        paymentIntent: PaymentIntent,
        addPaymentMethodFlowListener: PaymentMethodListener
    ) {
        requireNotNull(paymentIntent.customerId, {
            "Customer id must be provided on add payment method flow"
        })
        this.addPaymentMethodFlowListener = addPaymentMethodFlowListener
        AddPaymentMethodActivityStarter(activity)
            .startForResult(
                AddPaymentMethodActivityStarter.Args.Builder()
                    .setShipping(paymentIntent.order.shipping)
                    .setCustomerId(requireNotNull(paymentIntent.customerId))
                    .setClientSecret(requireNotNull(paymentIntent.clientSecret))
                    .build()
            )
    }

    /**
     * Launch the [PaymentMethodsActivity] to allow the user to select a payment method or add a new one
     *
     * @param paymentIntent a [PaymentIntent] used to present the Select Payment Method flow
     * @param selectPaymentMethodFlowListener The callback of present the select payment method flow
     */
    fun presentSelectPaymentMethodFlow(
        paymentIntent: PaymentIntent,
        selectPaymentMethodFlowListener: PaymentMethodListener
    ) {
        requireNotNull(paymentIntent.customerId, {
            "Customer id must be provided on select payment method flow"
        })
        this.selectPaymentMethodFlowListener = selectPaymentMethodFlowListener
        PaymentMethodsActivityStarter(activity)
            .startForResult(
                PaymentMethodsActivityStarter.Args.Builder()
                    .setPaymentIntent(paymentIntent)
                    .setIncludeCheckoutFlow(false)
                    .build()
            )
    }

    /**
     * Launch the [PaymentCheckoutActivity] to allow the user to confirm [PaymentIntent] using the specified [PaymentMethod]
     *
     * @param paymentIntent a [PaymentIntent] used to present the Checkout flow
     * @param paymentMethodType a [PaymentMethodType] used to present the Checkout flow
     * @param paymentMethodId ID of [PaymentMethod]
     * @param paymentDetailListener The callback of present the select payment detail flow
     */
    fun presentPaymentDetailFlow(
        paymentIntent: PaymentIntent,
        paymentMethodType: PaymentMethodType,
        paymentMethodId: String?,
        paymentDetailListener: PaymentIntentListener
    ) {
        this.paymentDetailListener = paymentDetailListener
        val paymentMethod = when (paymentMethodType) {
            PaymentMethodType.WECHAT -> {
                PaymentMethod.Builder()
                    .setType(PaymentMethodType.WECHAT)
                    .build()
            }
            PaymentMethodType.CARD -> {
                PaymentMethod.Builder()
                    .setType(PaymentMethodType.CARD)
                    .setId(requireNotNull(paymentMethodId, {
                        "Card payment need provide the ID."
                    }))
                    .build()
            }
        }
        PaymentCheckoutActivityStarter(activity)
            .startForResult(
                PaymentCheckoutActivityStarter.Args.Builder()
                    .setPaymentIntent(paymentIntent)
                    .setPaymentMethod(paymentMethod)
                    .build()
            )
    }

    /**
     * Launch the [PaymentMethodsActivity] to allow the user to complete the entire payment flow
     *
     * @param paymentIntent a [PaymentIntent] used to present the payment flow
     * @param paymentFlowListener The callback of present entire payment flow
     */
    fun presentPaymentFlow(
        paymentIntent: PaymentIntent,
        paymentFlowListener: PaymentIntentListener
    ) {
        requireNotNull(paymentIntent.customerId, {
            "Customer id must be provided on payment flow"
        })
        this.paymentFlowListener = paymentFlowListener
        PaymentMethodsActivityStarter(activity)
            .startForResult(
                PaymentMethodsActivityStarter.Args.Builder()
                    .setPaymentIntent(paymentIntent)
                    .setIncludeCheckoutFlow(true)
                    .build()
            )
    }

    /**
     * Should be called via `Activity#onActivityResult(int, int, Intent)}}` to handle the result of all flows
     */
    fun handlePaymentResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        if (!VALID_REQUEST_CODES.contains(requestCode)) {
            return false
        }

        when (resultCode) {
            Activity.RESULT_OK -> {
                return when (requestCode) {
                    AddPaymentMethodActivityStarter.REQUEST_CODE -> {
                        val result =
                            AddPaymentMethodActivityStarter.Result.fromIntent(data) ?: return true
                        addPaymentMethodFlowListener?.onSuccess(result.paymentMethod, result.cvc)
                        true
                    }
                    PaymentShippingActivityStarter.REQUEST_CODE -> {
                        val result =
                            PaymentShippingActivityStarter.Result.fromIntent(data) ?: return true
                        shippingFlowListener?.onSuccess(result.shipping)
                        true
                    }
                    PaymentMethodsActivityStarter.REQUEST_CODE -> {
                        val result =
                            PaymentMethodsActivityStarter.Result.fromIntent(data) ?: return true
                        if (result.includeCheckoutFlow) {
                            if (result.error != null) {
                                paymentFlowListener?.onFailed(result.error)
                            } else {
                                paymentFlowListener?.onSuccess(requireNotNull(result.paymentIntent))
                            }
                        } else {
                            selectPaymentMethodFlowListener?.onSuccess(
                                requireNotNull(result.paymentMethod),
                                result.cvc
                            )
                        }
                        true
                    }
                    PaymentCheckoutActivityStarter.REQUEST_CODE -> {
                        val result =
                            PaymentCheckoutActivityStarter.Result.fromIntent(data) ?: return true
                        if (result.error != null) {
                            paymentDetailListener?.onFailed(result.error)
                        } else {
                            paymentDetailListener?.onSuccess(requireNotNull(result.paymentIntent))
                        }
                        true
                    }
                    else -> false
                }
            }
            Activity.RESULT_CANCELED -> {
                return when (requestCode) {
                    AddPaymentMethodActivityStarter.REQUEST_CODE -> {
                        addPaymentMethodFlowListener?.onCancelled()
                        true
                    }
                    PaymentShippingActivityStarter.REQUEST_CODE -> {
                        shippingFlowListener?.onCancelled()
                        true
                    }
                    PaymentMethodsActivityStarter.REQUEST_CODE -> {
                        paymentFlowListener?.onCancelled()
                        true
                    }
                    PaymentCheckoutActivityStarter.REQUEST_CODE -> {
                        paymentDetailListener?.onCancelled()
                        true
                    }
                    else -> false
                }
            }
            else -> return false
        }
    }

    /**
     * Should be called during the host `Activity`'s onDestroy to detach listeners.
     */
    fun onDestroy() {
        shippingFlowListener = null
        paymentFlowListener = null
        addPaymentMethodFlowListener = null
        selectPaymentMethodFlowListener = null
    }

    internal companion object {

        private val VALID_REQUEST_CODES = setOf(
            PaymentMethodsActivityStarter.REQUEST_CODE,
            PaymentShippingActivityStarter.REQUEST_CODE,
            AddPaymentMethodActivityStarter.REQUEST_CODE,
            PaymentCheckoutActivityStarter.REQUEST_CODE
        )
    }
}