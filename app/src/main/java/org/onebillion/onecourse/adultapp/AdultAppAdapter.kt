package org.onebillion.onecourse.adultapp

import android.content.Intent
import android.util.Log
import android.widget.Toast
import org.onebillion.onecourse.mainui.MainActivity
import org.onebillion.onecourse.utils.*
import java.util.*

abstract class AdultAppAdapter {
    companion object {
        private const val INTENT_ACTION = "com.eidu.ob.START_UNIT"
        private const val INTENT_UNIT_ID_EXTRA = "unitId"

        @JvmStatic
        fun handleIntent(intent: Intent) {
            Log.d("MYTEST", "handleIntent Action: ${intent.action}")
            intent.takeIf { it.action == INTENT_ACTION }?.getStringExtra(INTENT_UNIT_ID_EXTRA)?.let(::runUnit)
        }
    }
}

fun runUnit(id: String) {
    Log.d("MYTEST", "runUnit: $id")
    val m = getUnitById(id)
    var configName = m.config
    OBSystemsManager.sharedManager.setCurrentUnit(id)
    //
    if (configName == null) {
        val appDir = OBConfigManager.sharedManager.currentActivityFolder
        val comps = appDir.split("/").toTypedArray()
        configName = comps[0]
    } else {
        OBConfigManager.sharedManager.updateConfigPaths(configName, false, m.lang)
    }

    try {
        if (!MainActivity.mainViewController.pushViewControllerWithNameConfig(m.target, configName, true, true, m.params)) {
            Toast.makeText(MainActivity.mainActivity, m.target + " hasn't been converted to Android yet.", Toast.LENGTH_LONG).show()
        }
    } catch (e: java.lang.Exception) {
        Toast.makeText(MainActivity.mainActivity, m.target + " error opening!", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

fun getUnitById(id: String): MlUnit = loadUnitXML().first { it.key.split('.')[0] == id }

fun loadUnitXML(): List<MlUnit> {
    val mlName = OBConfigManager.sharedManager.masterlist
    return loadUnitXML("masterlists/$mlName/units.xml")
}

fun loadUnitXML(xmlPath: String): List<MlUnit> {
    val arr: MutableList<MlUnit> = ArrayList()
    val xmlManager = OBXMLManager()
    try {
        val xl = xmlManager.parseFile(OBUtils.getInputStreamForPath(xmlPath))
        val xmlNode = xl[0]
        for (xmlLevel in xmlNode.childrenOfType("level")) {
            val xmlUnits = xmlLevel.childrenOfType("unit")
            for (n in xmlUnits) {
                val m = MlUnit.mlUnitFromXMLNode(n)
                m.key = n.attributeStringValue("id")
                m.level = xmlLevel.attributeStringValue("id").toInt()
                arr.add(m)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    Log.d("MYTEST", "loadUnitXML. Found ${arr.size} Units")
    return arr
}
