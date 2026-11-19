package com.kropi.hydration.nfc

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.NfcTag

/** Zapis tagów NFC „dolej tyle i tego". Odczyt załatwia zwykła intencja (patrz QuickAddActivity). */
object NfcWriter {

    enum class Result { OK, TOO_SMALL, READ_ONLY, UNSUPPORTED, FAILED }

    enum class Hardware { MISSING, DISABLED, READY }

    fun hardware(activity: Activity): Hardware {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return Hardware.MISSING
        return if (adapter.isEnabled) Hardware.READY else Hardware.DISABLED
    }

    /**
     * Wiadomość NDEF: adres z ilością i napojem oraz rekord aplikacji (AAR), który mówi Androidowi, że tag
     * ma otworzyć Kropi — bez niego inna aplikacja obsługująca `kropi://` mogłaby go przejąć.
     */
    fun message(ml: Int, drink: Drink, packageName: String): NdefMessage = NdefMessage(
        arrayOf(
            NdefRecord.createUri(NfcTag.uri(ml, drink)),
            NdefRecord.createApplicationRecord(packageName),
        ),
    )

    /** Zapisuje [message] na tagu. Wywołuj poza wątkiem UI (callback czytnika już taki jest). */
    fun write(tag: Tag, message: NdefMessage): Result = runCatching {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            try {
                if (!ndef.isWritable) return Result.READ_ONLY
                if (ndef.maxSize < message.byteArrayLength) return Result.TOO_SMALL
                ndef.writeNdefMessage(message)
                Result.OK
            } finally {
                runCatching { ndef.close() }
            }
        } else {
            val formatable = NdefFormatable.get(tag) ?: return Result.UNSUPPORTED
            formatable.connect()
            try {
                formatable.format(message)
                Result.OK
            } finally {
                runCatching { formatable.close() }
            }
        }
    }.getOrDefault(Result.FAILED)

    /** Włącza tryb czytnika: [onTag] dostaje każdy przyłożony tag. */
    fun listen(activity: Activity, onTag: (Tag) -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.enableReaderMode(
            activity,
            { tag -> onTag(tag) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null,
        )
    }

    fun stopListening(activity: Activity) {
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
    }
}
