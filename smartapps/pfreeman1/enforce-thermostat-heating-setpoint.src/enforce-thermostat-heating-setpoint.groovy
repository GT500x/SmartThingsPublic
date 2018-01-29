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
	cleanup()
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
    	if ((state.haveSetToOff || atomicState.haveRetried) && evt.value == "off") {
        	log.debug "Received event indicating that the thermostat is off. Setting back to heat."
            atomicState.haveSetToOff = false
            //runIn(10, setHeatOn)
            setHeatOn()
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
            runIn(2000, resetApp) // Putting this for now to reset the state of the app in case we experience an issue during execution.
            log.debug "TEMP CHECK: EXCEEDED SETPOINT: We are heating and we're at or over the target setpoint: Current heatingSetPoint: ${sp}, current thermostat temp ${ct}, "\
            + "(rounded down to neared 0.1): ${ct_rounded}"
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
            log.debug "TEMP CHECK: OK: Current temp ${ct},(rounded down to nearest 0.1): ${ct_rounded}, target: ${sp} (${Math.round((sp - ct_rounded) * 100) / 100f} degrees to go), thermostatOperatingState: ${cs}, thermostatMode: ${thermostat.currentState("thermostatMode").value}"
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
	
    runIn(60, refreshMode)
    runIn(120, checkHeatOn)
}

def setFanWithState() {
	if (atomicState.fanOn) {
        fanOn()
        atomicState.fanOn = false
    }
}

def refreshMode() {
	thermostat.forceRefreshMode()
}

def checkHeatOn() {
	def currentMode = thermostat.currentState("thermostatMode")?.value
	if (currentMode != "heat") {
    	if (!atomicState.haveRetried) {
        	atomicState.haveRetried = true
            atomicState.retryCount++
        	log.warn "The thermostat didn't go back to heat mode. First trying a refresh..."
        	thermostat.forceRefreshMode()
            
            sendSmsMessage("604-442-7968", "The thermostat didn't go back to heat mode - trying a refresh. Current mode: ${currentMode}" )
            runIn(30, checkHeatOn)
        }
        else if (atomicState.retryCount < 3) {
            log.error "The thermostat still didn't go back into heat mode! Trying setting to heat again."
            sendSmsMessage("604-442-7968", "The thermostat still didn't go back into heat mode! Trying setting to heat again. Current mode: ${currentMode}" )
            atomicState.retryCount++
            setHeatOn()
        } else {
            log.error "The thermostat still didn't go back into heat mode! Not retrying anymore"
            sendSmsMessage("604-442-7968", "The thermostat still didn't go back into heat mode! No more retries. Current mode: ${currentMode}" )
        	cleanup()
        }
    }
    else {
    	log.debug "Re-verified that the thermostat is in heat mode."
        if (atomicState.haveRetried) {
        	sendSmsMessage("604-442-7968", "Yay! The thermostat is back in heat mode! Current mode: ${currentMode}" )
        }
        cleanup()
    }
}

def resetApp() {
	log.warn "resetApp() was executed. What went wrong?"
    sendSmsMessage("604-442-7968", "resetApp() was executed. What went wrong?" )
    cleanup()
}

def cleanup() {
	atomicState.fanOn = null
    atomicState.haveSetToHeat = null
    atomicState.executing = null
    atomicState.haveSetToOff = null
    atomicState.haveRetried = null
    atomicState.retryCount = 0
    unschedule(resetApp)
    //assert atomicState.retryCount instanceof Integer
}