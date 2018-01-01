/**
 *  Smart Lighting Controller - Doors
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
    name: "Smart Lighting Controller - Doors",
    namespace: "pfreeman1",
    author: "Phillip Freeman",
    description: "This app will turn on configured lights (and sirens optionally) when an alarm occurs and then automatically return them to their previous state.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("When this contact opens...") {
		input "contacts", "capability.contactSensor", required: false
	}
	section("...turn on these lights...") {
		input "switches", "capability.switch", multiple: true
	}
    section("...set dimmable lights to this level...") {
		input "dimmerLevel", "number", title: "Dimmer level (1-100)",range: "1..100", required: true
	}
    section("...leave the lights on for this long after the switch is turned off.") {
		input "lightsOnMins", "number", title: "Number of minutes", required: false
	}
    section("Turn on between what times?") {
        input "fromTime", "time", title: "From", required: false
        input "toTime", "time", title: "To", required: false
    }
    section("On Which Days") {
        input "days", "enum", title: "Select Days of the Week", required: false, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", 
        	"Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
	unsubscribe()
    unschedule()
    state.clear()
    subscribe(location, "mode", modeChangeHandler)
    subscribe(contacts, "contact.open", contactOpenHandler)
    subscribe(contacts, "contact.closed", contactClosedHandler)
}

def modeChangeHandler (evt) {
	if (evt.isStateChange() && state) {
		log.debug "Home mode has changed from its previous value. New mode is: ${evt.value}. Removing lightsOff scheduled job."
        unsubscribe(switches)
        unschedule()
    	state.clear()
    }
}

def switchesToggleHandler (evt) {
    if( evt.value == "off" && state) {
        log.debug "Switch \"${evt.displayName}\" has been manually changed, changing hasChanged property to true for this switch."
        state.switchMap.switches.find{ it.id == evt.deviceId }.hasChanged = true
        log.debug "switchMap: ${state.switchMap}"
    }
}

def switchesDimmerHandler (evt) {
	if (!state) {
    	return
    }
    if( evt.value.toInteger() != dimmerLevel ) {
        log.debug "Switch \"${evt.displayName}\" dimmer level has been manually changed to ${evt.value}, changing hasChanged property to true for this switch."
        state.switchMap.switches.find{ it.id == evt.deviceId }.hasChanged = true
        log.debug "switchMap: ${state.switchMap}"
    }
}


def contactClosedHandler(evt) {
    unschedule(lightsOff) //unschedule any pending jobs
    if (state) {
        if(lightsOnMins > 0)
        {
        	if (state.timerRunning) {
            	log.debug "Shutoff timer is already running, resetting it to ${lightsOnMins} minutes."
            }
            
            state.timerRunning = true
            log.debug "Scheduling a job to shut the lights off in ${lightsOnMins} minutes."
            runIn(lightsOnMins * 60, lightsOff)
        }
        else
        {
            lightsOff()
        }
    }
    else {
    	log.debug "No state present - ignore this event."
    }
}

def contactOpenHandler(evt) { 
	log.debug "days: ${days}, fromTime: ${fromTimev}, toTime: ${toTime}"
	
    // Door is opened. Now check if today is one of the preset days-of-week
    def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    
    if (days) {
        def dayCheck = days.contains(day)
        if (dayCheck) {
        	if (fromTime && toTime) {
                def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
                if (between) {
                    lightsOn()
                } 
            }
            else {
            	lightsOn()
            }
        }
    }
    else {
        if (fromTime && toTime) {
            def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
            if (between) {
                lightsOn()
            } 
        }
        else {
            lightsOn()
        }
    }
}

def lightsOn() {
	unschedule()
	log.debug "Switches: ${switches}"
  	        
    if (state.timerRunning) {
        log.debug "Timer is already running so probably the door was opened and closed within short period, not recording current switch state."
		return
    }
	
	state.clear()

	def myMap = [:]
	myMap.switches = switches.collect { [ id: it.id, level: it.supportedCommands.find{it.name == "setLevel"} ? 
		it.currentState("level")?.value as Integer : -1, value: it.currentState("switch").value, hasChanged: false] }

	log.debug "Switch information: ${myMap.switches}"

	state.switchMap = myMap
   
   	unsubscribe(switches)
	switches.each {
		if (it.currentState("switch").value != "on") {
			it.on()
		}
	   
		def level = it.currentState("level")?.value.toInteger()
	   
		if (it.supportedCommands.find{it.name == "setLevel"}) {
			log.debug "Switch ${it.displayName} supports the setLevel command. It's currently set to: ${level}"
			if (it.currentState("level").value.toInteger() < dimmerLevel.toInteger()) {
                log.debug "Setting dimmerLevel for ${it.displayName} to ${dimmerLevel}"
                it.setLevel(dimmerLevel.toInteger())
            }
		}
	}
    //subscribe(switches, "switch.on", switchesToggleHandler)
	//subscribe(switches, "switch.off", switchesToggleHandler)
	//subscribe(switches, "level", switchesDimmerHandler)
    runIn(7, setSubscriptions)
}

def setSubscriptions(){
	log.debug "Adding switch state subscriptions."
	subscribe(switches, "switch.on", switchesToggleHandler)
	subscribe(switches, "switch.off", switchesToggleHandler)
	subscribe(switches, "level", switchesDimmerHandler)
}

def lightsOff() {
	//log.debug "Restoring lights back to previous state..."
    unsubscribe(switches)
    
    state.timerRunning = false
    state.switchMap.switches.findAll { !it.hasChanged }.each{
		mySw ->      
        log.debug "Switch was previously ${mySw.value} and level was set to: ${mySw.level}"
        
        if(mySw.value == "off")
        {
        	def swOff = switches.find{ sw -> sw.id == mySw.id}
            log.debug "Turning off [${swOff.displayName}] because it was off before."
            swOff.off()
        }
        else if(mySw.level != -1)
        {
            def swLevel = switches.find{ sw -> sw.id == mySw.id}

            if (swLevel.currentState("level")?.value.toInteger() != mySw.level)
            {
                log.debug "Setting level of [${swLevel.displayName}] back to ${mySw.level}."
                swLevel.setLevel(mySw.level)
            }
        }  
    }
    
    state.clear()
}