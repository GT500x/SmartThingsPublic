/**
 *  ThermoStat Setpoint Switch Mapper
 *
 *  Copyright 2018 Phillip Freeman
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "ThermoStat Setpoint Switch Mapper",
    namespace: "pfreeman1",
    author: "Phillip Freeman",
    description: "This app creates a mapping between a simulated switch and a thermostat command. The purpose is because there&#39;s no way to do this via a routine.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Which switch will trigger the thermostat command?") {
		input "switch1", "capability.switch", multiple: false
	}
    section("Select the thermostat.") {
		input "thermostat", "capability.thermostat"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(switch1, "switch.on", switchHandler)
}

def switchHandler(evt) {
	if (switch1.currentValue("switch") == "on" && thermostat.currentValue("thermostatHoldMode") == "holdOff" && evt.isStateChange()) {
    	thermostat.changeSetPoint()
    }
    switch1.off()
}

