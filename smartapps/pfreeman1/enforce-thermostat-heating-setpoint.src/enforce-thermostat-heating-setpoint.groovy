/**
 *  Keep target temperature
 *
 *  Copyright 2017 Phillip Freeman
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
    name: "Enforce Thermostat Heating Setpoint",
    namespace: "pfreeman1",
    author: "Phillip Freeman",
    description: "This app is specifically for the Centralite Pearl thermostat. This thermostat has an unconfigurable differential of "
    + "1 degree. I.e. the thermostat doesn't stop heating when it reaches the setpoint, it keeps going to 0.5 degrees higher. This app "
    + "forces the thermostat to stop heating when it reaches the setpoint by temporarily setting the thermostat mode to \"off\", then back to heat.",
    category: "Health & Wellness",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select the thermostat to control") {
		input "thermostat", "capability.thermostat"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	clearAtomicState()
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(thermostat, "temperature", temperatureHandler)
	subscribe(thermostat, "thermostatOperatingState", temperatureHandler)
    subscribe(thermostat, "heatingSetpoint", thermostatSetpointChange)
    subscribe(thermostat, "thermostatMode", thermostatModeHandler)
}

def thermostatSetpointChange(evt) {
	if (evt.isStateChange() && thermostat.currentState("thermostatOperatingState").value == "heating") {
    	log.debug "Currently heating and the thermostat setpoint was just changed - triggering temp check..."
        temperatureHandler(evt)
    }
}

def thermostatModeHandler(evt) {
	if (atomicState.executing && evt.isStateChange()) {
    	if (state.haveSetToOff && evt.value == "off") {
        	log.debug "Received event indicating that the thermostat is off. Setting back to heat in 10s."
            atomicState.haveSetToOff = false
            runIn(10, setHeatOn)
        } else if (atomicState.haveSetToHeat && evt.value == "heat") {
            log.debug "Received event indicating that the thermostat is in heat mode."
			runIn(5, setFanWithState)
        }
        else {
        	log.warn "operatingStateHandler(): Unexpected scenario. evt.value = ${evt.value}, state = ${state}"
        }
    }
}

def forceRefresh()
{
	thermostat.forceRefreshTemp()
}

def temperatureHandler(evt)
{
	def sp = thermostat.currentHeatingSetpoint
    def ct = thermostat.currentTemperature
    def cs = thermostat.currentState("thermostatOperatingState").value
	if (!atomicState.executing && cs == "heating") {
    	def ct_rounded = Math.round((ct - 0.05) * 10) / 10f
        if ( ct_rounded >= sp ) {
            atomicState.executing = true
            log.debug "We are heating and we're at or over the target setpoint: Current heatingSetPoint: ${sp}, current thermostat temp ${ct}, (rounded down to neared 0.1): ${ct_rounded}"
            def currentFanMode = thermostat.currentState("thermostatFanMode")?.value
            if (currentFanMode == "fanOn") {
                atomicState.fanOn = true
            }
			
            log.debug "Setting thermostat to \"off\", to stop heating."
            thermostat.off()
            atomicState.haveSetToOff = true
            runIn(60, setHeatOn)	//Doing this just in case the thermostat doesn't send us an event telling us that it's off for some reason.
        }
        else {
            log.debug "HEATING: TEMP OK: Current temp ${ct},(rounded down to nearest 0.1): ${ct_rounded}, target: ${sp} (${Math.round((sp - ct_rounded) * 100) / 100f} degrees to go), thermostatOperatingState: ${cs}, thermostatMode: ${thermostat.currentState("thermostatMode").value}"
    	}
	}
}

def fanOn() {
	log.debug "Turning the fan back on as it was on previously."
	thermostat.fanOn()
}

def setHeatOn() {
	log.debug "Setting the thermostat back to heat mode"
    atomicState.haveSetToHeat = true
    thermostat.heat()

    runIn(120, checkHeatOn)
}

def setFanWithState() {
	if (atomicState.fanOn) {
        fanOn()
        atomicState.fanOn = false
    }
}

def checkHeatOn() {
	def currentMode = thermostat.currentState("thermostatMode")?.value
	if (currentMode != "heat") {
    	if (!atomicState.haveRetried) {
        	log.warn "The thermostat didn't go back to heat mode. First trying a refresh..."
        	thermostat.refresh()
            
            sendSmsMessage("604-442-7968", "The thermostat didn't go back to heat mode. Current mode: ${currentMode}" )
            atomicState.haveRetried = true
            atomicState.retryCount++
        }
        else if (atomicState.retryCount < 3) {
            log.error "The thermostat still didn't go back into heat mode! Trying again..."
            sendSmsMessage("604-442-7968", "The thermostat still didn't go back into heat mode! Current mode: ${currentMode}" )
            atomicState.retryCount++
            setHeatOn()
        } else {
            log.error "The thermostat still didn't go back into heat mode! Not retrying anymore"
            sendSmsMessage("604-442-7968", "The thermostat still didn't go back into heat mode! Current mode: ${currentMode}" )
        	clearAtomicState()
        }
    }
    else {
    	log.debug "Re-verified that the thermostat is in heat mode."
        clearAtomicState()
    }
}

def clearAtomicState() {
	atomicState.fanOn = null
    atomicState.haveSetToHeat = null
    atomicState.executing = null
    atomicState.haveSetToOff = null
    atomicState.haveRetried = null
    atomicState.retryCount = 0
}