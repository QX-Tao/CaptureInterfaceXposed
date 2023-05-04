package com.android.captureinterfacexposed.hook

import android.widget.Toast
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.type.android.BundleClass
import com.highcapable.yukihookapi.hook.type.java.UnitType
import com.tencent.automationlib.Automation

object UIHierarchyHook : YukiBaseHooker() {
    override fun onHook() {
        findClass("Activity.class").hook {
            injectMember {
                method {
                    name = "onCreate"
                    param(BundleClass)
                    returnType = UnitType
                }
                afterHook {
                    Toast.makeText(appContext,"Hook succeeded",Toast.LENGTH_SHORT).show()
                    Automation.enable(1)
                }
            }.onHooked { member ->
                loggerD(msg = "$member has hooked")
            }
        }

    }

}