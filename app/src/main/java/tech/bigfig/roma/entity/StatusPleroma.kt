package tech.bigfig.roma.entity

import com.google.gson.annotations.SerializedName

data class StatusPleroma(
        val content: TextPlain,
        val conversation_id: String,
        val in_reply_to_account_acct: String,
        val local: Boolean,
        val spoiler_text: TextPlain
) {
    data class TextPlain(
            @SerializedName("text/plain") val text_plain: String
    )
}