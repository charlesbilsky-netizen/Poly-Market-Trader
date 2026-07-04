package com.example.ui

/**
 * Bundled legal documents (also published in /legal of the repo and hosted
 * for the Play Store listing). Shown in-app from Settings so the privacy
 * policy is reachable without a network connection, per Play policy
 * expectations.
 */
object LegalTexts {

    const val PRIVACY_TITLE = "PRIVACY POLICY"
    const val TERMS_TITLE = "TERMS & CONDITIONS"

    val PRIVACY = """
PRIVACY POLICY
Last Updated: July 4, 2026

1. INTRODUCTION
Welcome to PolyTrader. Your privacy is important to us. This Privacy Policy explains how we handle your information when you use our mobile application ("PolyTrader" or the "App").

By using the App, you agree to the practices described in this policy.

2. INFORMATION WE DO NOT COLLECT
PolyTrader is designed to be a decentralized, client-side application. We do not operate proprietary backend servers to collect, store, or process your personal data. Specifically:
• We do not require you to create an account with us.
• We do not collect your name, email address, or contact information.
• We do not track your usage data, IP address, or device identifiers on our servers.
• We do not collect or store your private cryptographic keys.

3. INFORMATION STORED LOCALLY ON YOUR DEVICE
To function properly, PolyTrader stores certain information strictly on your local device:
• Wallet Addresses: You may input a public Polygon wallet address to view public portfolio data. This address is stored locally on your device and queried directly against public APIs.
• API Keys: If you choose to use advanced AI features (e.g., Gemini, OpenAI, xAI), you may input your own API keys. These keys are stored in your device's local app storage and are sent directly to the respective third-party service providers. We never see or have access to your API keys.
• App Settings: Watchlists, theme preferences, and research prompts are saved locally.

4. THIRD-PARTY SERVICES
PolyTrader interacts with third-party APIs to provide its services. When you use the App, your device communicates directly with these services. Please review their privacy policies:
• Polymarket (Gamma, CLOB, Data APIs): We fetch public market data and portfolio statistics. (See Polymarket's Privacy Policy)
• AI Providers: If you use the AI research tools, your prompts and relevant market data are sent to the provider you configure (Google Gemini, OpenAI, or xAI).

5. SECURITY
We take reasonable measures to protect the information stored locally on your device by using Android's standard app-sandboxed storage mechanisms. However, the security of your device is your responsibility. We recommend using a device lock and keeping your operating system up to date.

6. CHILDREN'S PRIVACY
The App is not intended for children under the age of 18. We do not knowingly collect personal information from children. If you are under 18, please do not use this App.

7. CHANGES TO THIS PRIVACY POLICY
We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page and updating the "Last Updated" date.

8. CONTACT US
If you have any questions or suggestions about our Privacy Policy, please contact us at charlesbilsky@gmail.com.
""".trimIndent()

    val TERMS = """
TERMS AND CONDITIONS
Last Updated: July 4, 2026

1. ACCEPTANCE OF TERMS
By downloading, installing, or using the PolyTrader mobile application ("the App"), you agree to be bound by these Terms and Conditions. If you do not agree to these terms, please do not use the App.

2. NATURE OF THE APP
PolyTrader is an informational, research, and analytics companion tool designed to interface with public prediction market data (specifically Polymarket).
• No Trading Execution: PolyTrader does NOT execute trades, hold funds, or manage private keys. Any "Trade" action within the App merely redirects you to the official Polymarket website via your device's web browser.
• Not Financial Advice: The information, AI-generated research, and market data provided by the App are for informational and educational purposes only. They do not constitute financial, investment, legal, or tax advice. You are solely responsible for your own trading decisions.

3. THIRD-PARTY SERVICES AND APIS
• Polymarket: We are an independent application and are not officially affiliated with, endorsed by, or sponsored by Polymarket.
• AI Services: The App allows you to input your own API keys for third-party AI services (e.g., OpenAI, Google Gemini, xAI). You are responsible for complying with the terms of service of these third-party providers and for any costs incurred through the use of your API keys.

4. USER RESPONSIBILITIES
• You must be at least 18 years old to use the App.
• You are responsible for the security of your device and any API keys or wallet addresses you input into the App.
• You agree to use the App in compliance with all applicable local, state, national, and international laws and regulations. Prediction markets and cryptocurrency may be restricted or prohibited in certain jurisdictions (including parts of the United States). It is your responsibility to ensure your use of the App and any associated platforms is legal in your jurisdiction.

5. DISCLAIMER OF WARRANTIES
THE APP IS PROVIDED ON AN "AS IS" AND "AS AVAILABLE" BASIS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. WE DO NOT WARRANT THAT THE APP WILL BE UNINTERRUPTED, ERROR-FREE, OR COMPLETELY SECURE.

6. LIMITATION OF LIABILITY
TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL THE DEVELOPER OR ITS AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES, OR ANY LOSS OF PROFITS OR REVENUES, WHETHER INCURRED DIRECTLY OR INDIRECTLY, OR ANY LOSS OF DATA, USE, GOODWILL, OR OTHER INTANGIBLE LOSSES, RESULTING FROM (A) YOUR USE OR INABILITY TO USE THE APP; (B) ANY INACCURACY OR DELAY IN MARKET DATA OR AI RESEARCH; OR (C) ANY TRADING LOSSES INCURRED ON THIRD-PARTY PLATFORMS.

7. MODIFICATIONS
We reserve the right to modify or replace these Terms and Conditions at any time. Your continued use of the App after any such changes constitutes your acceptance of the new Terms and Conditions.

8. GOVERNING LAW
These Terms shall be governed and construed in accordance with the laws of the United States, without regard to its conflict of law provisions.

9. CONTACT
If you have any questions about these Terms, please contact us at charlesbilsky@gmail.com.
""".trimIndent()
}
