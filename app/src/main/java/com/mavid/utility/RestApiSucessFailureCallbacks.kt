package com.mavid.utility

import com.mavid.models.ModelRemoteDetails

interface RestApiSucessFailureCallbacks {
    fun onSucessFailureCallbacks(isSucess:Boolean,modelRemoteDetails: ModelRemoteDetails?)
}