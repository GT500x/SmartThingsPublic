/**
 *  Alarm Light Controller
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
    name: "Alarm Lighting Controller",
    namespace: "pfreeman",
    author: "Phillip Freeman",
    description: "This app will turn on configured lights (and sirens optionally) when an alarm occurs and then automatically turn them off after X minutes unless they were already on.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png")


preferences {
    section("When this virtual switch is turned on...") {
		input "switch1", "capability.switch"
	}
	section("...turn on these lights...") {
		input "switches", "capability.switch", multiple: true
	}
    section("...and, optionally, turn on these sirens..."){
		input "sirens", "capability.alarm", required: false, multiple: true
	}
    //section("...turn off these lights if entering mode...") {
		//input "modes", "mode", required: true, multiple: true
	//}
    section("...for how long? After this time they will return to their previous state automatically if the virtual switch isn't turned off first (defaults to 5 min)") {
		input "resetMins", "number", title: "Number of minutes", required: false
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
	subscribe(switch1, "switch.on", switchOnHandler)
    subscribe(switch1, "switch.off", switchOffHandler)
    //subscribe(location, modeChangeHandler)
    subscribe(location, "alarmSystemStatus", alarmHandler)
}

def alarmHandler(evt) {
  log.debug "Alarm Handler value: ${evt.value}"
  log.debug "alarm state: ${location.currentState("alarmSystemStatus")?.value}"
  if (evt.value == "off" && switch1.currentState("switch").value == "on") {
  	    log.debug "Alarm was disarmed and Light trigger switch is on - turning off lights"
        switch1.off()
  }
}

//def modeChangeHandler(evt) {
//	if (modes.find{it.value == evt.value} && switch1.currentState("switch").value == "on")
//	{
//    	log.debug "Mode changed to Home and Light trigger switch is on"
//        unschedule() //unschedule the automatic turn off.
//        turnOffLights()
//	}
//}

def switchOffHandler(evt) {
    unschedule() //unschedule the automatic turn off.
    turnOffLightsAndSirens()
}

def switchOnHandler(evt) {
	log.debug "Switches: ${switches}"
	turnOnLightsAndSirens()
	runIn(resetMins ? resetMins * 60 : 300, turnOffLightsAndSirens)
}

def turnOnLightsAndSirens() {
	switches.each {
    	def curState = it.currentState("switch")
    	log.debug "${it.displayName}, status: ${curState.value}"
        if (curState.value == "off"){
			it.on()
            if (it.capabilities.find{it == "setLevel"}) {
            	it.setLevel(100)
            }
        }
    }
    
    sirens.each {
   		it.siren()
    }
}

def turnOffLightsAndSirens() {
	final delay = resetMins ? resetMins * 60 : 300
    def t0 = new Date(now() - ((delay + 30) * 1000))
    log.debug "t0: ${t0}"
	switches.each {
		def previousStates = it.statesSince("switch", t0)
        def recentOnState = previousStates.find{it.value == "on"} // This state will only exist in the recent history if we turned it on.
        if (recentOnState)
        {
        	log.debug "turning off: ${it.displayName} since it was off before"
        	it.off()
        }
    }
    
    sirens.each {
   		it.off()
    }
}