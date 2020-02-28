package com.airwallex.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.airwallex.android.model.ObjectBuilder
import com.airwallex.android.model.PaymentMethod
import com.airwallex.android.model.Shipping
import com.airwallex.android.view.AddPaymentMethodActivityStarter.Args
import kotlinx.android.parcel.Parcelize

internal class AddPaymentMethodActivityStarter constructor(
    activity: Activity
) : ActivityStarter<AddPaymentMethodActivity, Args>(
    activity,
    AddPaymentMethodActivity::class.java,
    REQUEST_CODE
) {

    @Parcelize
    data class Args internal constructor(
        internal val token: String?,
        internal val clientSecret: String?,
        internal val customerId: String?,
        internal val shipping: Shipping?
    ) : ActivityStarter.Args {

        class Builder : ObjectBuilder<Args> {
            private var token: String? = null
            private var clientSecret: String? = null
            private var customerId: String? = null
            private var shipping: Shipping? = null

            fun setToken(token: String?): Builder = apply {
                this.token = token
            }

            fun setClientSecret(clientSecret: String?): Builder = apply {
                this.clientSecret = clientSecret
            }

            fun setCustomerId(customerId: String?): Builder = apply {
                this.customerId = customerId
            }

            fun setShipping(shipping: Shipping?): Builder = apply {
                this.shipping = shipping
            }

            override fun build(): Args {
                return Args(
                    token = token,
                    clientSecret = clientSecret,
                    customerId = customerId,
                    shipping = shipping
                )
            }
        }

        internal companion object {
            internal fun getExtra(intent: Intent): Args {
                return requireNotNull(intent.getParcelableExtra(ActivityStarter.Args.AIRWALLEX_EXTRA))
            }
        }
    }

    @Parcelize
    data class Result internal constructor(
        val paymentMethod: PaymentMethod
    ) : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(ActivityStarter.Result.AIRWALLEX_EXTRA, this)
            return bundle
        }

        companion object {
            fun fromIntent(intent: Intent?): Result? {
                return intent?.getParcelableExtra(ActivityStarter.Result.AIRWALLEX_EXTRA)
            }
        }
    }

    companion object {
        const val REQUEST_CODE: Int = 1002
    }

}