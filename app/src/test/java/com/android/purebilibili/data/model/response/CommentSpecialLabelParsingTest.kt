package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CommentSpecialLabelParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun decodeReplyResponse_exposesDesktopSpecialLabelAndUpLikeConfig() {
        val payload = """
            {
              "code": 0,
              "data": {
                "config": {
                  "show_up_flag": true
                },
                "replies": [
                  {
                    "rpid": 1,
                    "mid": 2,
                    "ctime": 1700000000,
                    "up_action": {
                      "like": true,
                      "reply": false
                    },
                    "card_label": [
                      {
                        "text_content": "UP主觉得很赞",
                        "label_color": "#FB7299",
                        "jump_url": ""
                      }
                    ],
                    "member": {
                      "mid": "2",
                      "uname": "测试用户"
                    },
                    "content": {
                      "message": "特殊评论"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ReplyResponse>(payload)
        val data = response.data
        assertNotNull(data)

        val config = requireFieldValue(data, "config")
        assertNotNull(config)
        assertEquals(true, requireFieldValue(config, "showUpFlag"))

        val reply = data.replies?.firstOrNull()
        assertNotNull(reply)
        val upAction = requireFieldValue(reply, "upAction")
        assertNotNull(upAction)
        assertEquals(true, requireFieldValue(upAction, "like"))

        val cardLabels = requireFieldValue(reply, "cardLabels") as? List<*>
        assertNotNull(cardLabels)
        assertEquals(1, cardLabels.size)

        val firstLabel = cardLabels.firstOrNull()
        assertNotNull(firstLabel)
        assertEquals("UP主觉得很赞", requireFieldValue(firstLabel, "textContent"))
    }

    private fun requireFieldValue(instance: Any, fieldName: String): Any? {
        val field = instance.javaClass.declaredFields.firstOrNull { it.name == fieldName }
        assertNotNull(field, "Expected field '$fieldName' on ${instance.javaClass.simpleName}")
        field.isAccessible = true
        return field.get(instance)
    }
}
