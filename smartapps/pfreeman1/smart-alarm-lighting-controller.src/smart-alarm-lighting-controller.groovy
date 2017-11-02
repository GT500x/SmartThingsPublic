/**
 *  Smart Alarm Lighting Controller
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
    name: "Smart Alarm Lighting Controller",
    namespace: "pfreeman1",
    author: "Phillip Freeman",
    description: "This app will turn on configured lights (and sirens optionally) when an alarm occurs and then automatically return them to their previous state.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


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
    section("...turn off sirens after this many minutes...") {
		input "sirensOffMins", "number", title: "Number of minutes", required: false
	}
    section("...leave the lights on for this long after the switch is turned off.") {
		input "lightsOnMins", "number", title: "Number of minutes", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(switch1, "switch.on", switchOnHandler)
    subscribe(switch1, "switch.off", switchOffHandler)
    subscribe(location, "alarmSystemStatus", alarmHandler)
}

def alarmHandler(evt) {
  log.debug "Alarm Handler value: ${evt.value}"
  log.debug "alarm state: ${location.currentState("alarmSystemStatus")?.value}"
  if (evt.value == "off" && switch1.currentState("switch").value == "on") {
  	    log.debug "Alarm was disarmed and Light trigger switch is on - starting shut down"
        switch1.off()
  }
}

def switchOffHandler(evt) {
    unschedule() //unschedule the automatic turn off.
    turnOffSirens()
    if(lightsOnMins > 0)
    {
    	runIn(lightsOnMins * 60, turnOffLights)
    }
    else
    {
    	turnOffLights()
    }
}

def switchOnHandler(evt) {
	unschedule()
	log.debug "Switches: ${switches}"
	
    def myMap = [:]
	myMap.switches = switches.collect { [ id: it.id, level: it.supportedCommands.find{it.name == "setLevel"} ? 
    	it.currentState("level")?.value as Integer : -1, value: it.currentState("switch").value] }

    log.debug "Switch information: ${myMap.switches}"
    
    state.switchMap = myMap
    
    turnOnLightsAndSirens()
    runIn(sirensOffMins ? sirensOffMins * 60 : 300, turnOffSirens)
}

def autoOff(){
	switch1.off()
}

def turnOnLightsAndSirens() {

	switches.each {
        if (it.currentState("switch").value != "on") {
        	it.on()
        }
        
        if (it.supportedCommands.find{it.name == "setLevel"} && it.currentState("level")?.value.toInteger() < 95) {
            it.setLevel(100)
        }
    }
    
    sirens?.siren()
}

def turnOffLights() {

	state.switchMap.switches.each
    {
        log.debug "Switch was previously ${it.value} and level was set to: ${it.level}"
        if(it.level != -1)
        {
            def swLevel = switches.find{ sw -> sw.id == it.id}

            if (swLevel.currentState("level")?.value.toInteger() != it.level)
            {
                log.debug "Setting level of [${swLevel.displayName}] back to ${it.level}."
                swLevel.setLevel(it.level)
            }
        }  
    
    	if(it.value == "off")
        {
        	def swOff = switches.find{ sw -> sw.id == it.id}
            log.debug "Turning off [${swOff.displayName}] because it was off before."
            swOff.off()
        }
    }

    state.clear()
}

def turnOffSirens() {
    sirens?.off()
}