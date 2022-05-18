package com.wire.android.util

import com.wire.kalium.logic.configuration.ServerConfig

object WireConstants {
    const val FB_STAGING_PROJECT_ID = "wiregcmpushstaging"
    const val FB_STAGING_APPLICATION_ID = "1:723990470614:android:10cf802bfcc7bb71d28e77"
    const val FB_STAGING_API_KEY = "AIzaSyAGCoJGUtDBLJJiQPLxHQRrdkbyI0wlbo8"
    const val FB_STAGING_SENDER_ID = "723990470614"


    const val FB_PROD_SENDER_ID = "782078216207"
    const val FB_PROD_API_KEY = "AIzaSyDm5X3eLROMO9tM-jc2-RVUZqaYbNByg2I"
    const val FB_PROD_PROJECT_ID = "w966768976"
    const val FB_PROD_APPLICATION_ID1 = "1:782078216207:android:d3db2443512d2055"
    const val FB_PROD_APPLICATION_ID2 = "1:782078216207:android:56b695153c71b91a217b7a"
    const val FB_PROD_APPLICATION_ID3 = "1:782078216207:android:873e181ef19a2fc0217b7a"
    const val FB_PROD_APPLICATION_ID4 = "1:782078216207:android:1f05d3922be57aa9217b7a"
    const val FB_PROD_APPLICATION_ID5 = "1:782078216207:android:c835986a50e59e1c217b7a"

    fun getSenderId(): String {
        return when (ServerConfig.DEFAULT.title.lowercase()) {
            ServerTitle.STAGING.title, ServerTitle.QA_DEMO.title,
            ServerTitle.ANTA.title, ServerTitle.BELLA.title,
            ServerTitle.CHALA.title -> {
                FB_STAGING_SENDER_ID
            }
            else -> {
                FB_PROD_SENDER_ID
            }
        }
    }

    enum class ServerTitle(val title: String) {
        STAGING("staging"),
        QA_DEMO("qa-demo"),
        ANTA("anta"),
        BELLA("bella"),
        CHALA("chala")
    }
}
