package com.example.qrscanner12



    import android.content.Context
    import com.google.android.gms.safetynet.SafetyNet
    import com.google.android.gms.safetynet.SafeBrowsingThreat

    /**
     * Checks if a URL is malicious using Google Safe Browsing.
     * @param onResult returns true if safe, false if suspicious/malicious
     */
    fun checkUrlSafety(context: Context, url: String, onResult: (Boolean) -> Unit) {
        val client = SafetyNet.getClient(context)

        client.initSafeBrowsing()

        // We check for Malware and Social Engineering (Phishing)
        client.lookupUri(
            url,
            "", // API Key can be empty if using the device's local Safe Browsing
            SafeBrowsingThreat.TYPE_POTENTIALLY_HARMFUL_APPLICATION,
            SafeBrowsingThreat.TYPE_SOCIAL_ENGINEERING
        ).addOnSuccessListener { response ->
            // If the list of detected threats is empty, the link is safe
            val isSafe = response.detectedThreats.isEmpty()
            onResult(isSafe)
        }.addOnFailureListener {
            // If the API call fails (no internet, etc.), we assume it's unsafe
            // to protect the user, or you can return true depending on your policy.
            onResult(false)
        }
    }
