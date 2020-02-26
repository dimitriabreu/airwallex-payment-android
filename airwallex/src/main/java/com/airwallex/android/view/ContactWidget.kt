package com.airwallex.android.view

import android.content.Context
import android.util.AttributeSet
import android.util.Patterns
import android.view.View
import android.widget.LinearLayout
import com.airwallex.android.R
import com.airwallex.android.model.Shipping
import kotlinx.android.synthetic.main.widget_contact.view.*

class ContactWidget(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    internal data class Contact(
        val lastName: String,
        val firstName: String,
        val phone: String,
        val email: String
    )

    var contactChangeCallback: (() -> Unit)? = null

    val isValidContact: Boolean
        get() {
            return atlLastName.value.isNotEmpty()
                    && atlFirstName.value.isNotEmpty()
                    && atlEmail.value.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(atlEmail.value).matches()
        }

    internal val contact: Contact
        get() {
            return Contact(
                lastName = atlLastName.value,
                firstName = atlFirstName.value,
                phone = atlPhoneNumber.value,
                email = atlEmail.value
            )
        }

    init {
        View.inflate(
            getContext(),
            R.layout.widget_contact, this
        )

        listenTextChanged()
        listenFocusChanged()
    }

    fun initializeView(shipping: Shipping) {
        with(shipping) {
            atlLastName.value = lastName ?: ""
            atlFirstName.value = firstName ?: ""
            atlPhoneNumber.value = phone ?: ""
            atlEmail.value = email ?: ""
        }
    }

    private fun listenTextChanged() {
        atlLastName.afterTextChanged { contactChangeCallback?.invoke() }
        atlFirstName.afterTextChanged { contactChangeCallback?.invoke() }
        atlEmail.afterTextChanged { contactChangeCallback?.invoke() }
    }

    private fun listenFocusChanged() {
        atlLastName.afterFocusChanged { hasFocus ->
            if (!hasFocus) {
                if (atlLastName.value.isEmpty()) {
                    atlLastName.error = resources.getString(R.string.empty_last_name)
                } else {
                    atlLastName.error = null
                }
            } else {
                atlLastName.error = null
            }
        }

        atlFirstName.afterFocusChanged { hasFocus ->
            if (!hasFocus) {
                if (atlFirstName.value.isEmpty()) {
                    atlFirstName.error = resources.getString(R.string.empty_first_name)
                } else {
                    atlFirstName.error = null
                }
            } else {
                atlFirstName.error = null
            }
        }

        atlEmail.afterFocusChanged { hasFocus ->
            if (!hasFocus) {
                when {
                    atlEmail.value.isEmpty() -> {
                        atlEmail.error = resources.getString(R.string.empty_email)
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(atlEmail.value).matches() -> {
                        atlEmail.error = resources.getString(R.string.invalid_email)
                    }
                    else -> {
                        atlEmail.error = null
                    }
                }
            } else {
                atlEmail.error = null
            }
        }
    }
}